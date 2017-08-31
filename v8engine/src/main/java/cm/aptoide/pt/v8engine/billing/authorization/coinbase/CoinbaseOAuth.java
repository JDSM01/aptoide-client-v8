package cm.aptoide.pt.v8engine.billing.authorization.coinbase;

import android.net.Uri;

import com.coinbase.api.Coinbase;
import com.coinbase.api.CoinbaseBuilder;
import com.coinbase.api.entity.OAuthCodeRequest;
import com.coinbase.api.entity.OAuthTokensResponse;
import com.coinbase.api.entity.Transaction;
import com.coinbase.api.exception.CoinbaseException;
import com.coinbase.api.exception.UnauthorizedException;

import org.joda.money.Money;

import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import cm.aptoide.pt.v8engine.billing.transaction.BitcoinTransactionService;
import cm.aptoide.pt.v8engine.billing.view.bitcoin.TransactionSimulator;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by jose_messejana on 18-08-2017.
 */

public class CoinbaseOAuth {
    public static String cbredirectUrl;
    private final double CONVERSION_RATE = 0.00024; // From August 14 2017
    static final String CLIENT_ID = "35193ae7364abb8a1a75bd97b52f437833aa802d9cb43ba8bedd16f64af09ab1";
    static final String CLIENT_SECRET = "98f9a4e6b144bf5729033fa0eb5a16986c83f48e70446fa7a178be6d7dc3cce0";
    private static final String EMAIL = "Jose.Messejana@aptoide.com";
    private final BitcoinTransactionService service;
    private String CSRFtoken = null;
    private OAuthTokensResponse tokens;
    private Coinbase coinbaseInstance = null;
    private Transaction bitCBTransaction = null;
    private TransactionSimulator bitcoinTransactionSimulator = null;
    private Map<String, OAuthTokensResponse> existing_tokens = new HashMap<>();
    private Map<String, Coinbase> existing_instance = new HashMap<>();
    private double price;

    public CoinbaseOAuth(BitcoinTransactionService service){

        this.service = service;
    }

    public String createLoginCSRFToken(){
        int result = (new Random()).nextInt();
        CSRFtoken = Integer.toString(result);
        return CSRFtoken;
    }

    public String getLoginCSRFToken(){
        if(CSRFtoken == null){
            createLoginCSRFToken();
        }
        return CSRFtoken;
    }

    public Single<Uri> beginAuth(String redirectUri){
        try {
            OAuthCodeRequest.Meta meta = null; // Comment this line to sendCoins
            String scope = "user";
            if(BitcoinTransactionService.REALTRANSACTION) {
                 meta = new OAuthCodeRequest.Meta();
                 meta.setSendLimitAmount(Money.parse("USD 1.0"));
                 meta.setSendLimitPeriod(OAuthCodeRequest.Meta.Period.DAILY);
                 scope = "user,send"; // wallet:transactions:transfer
            }
            Coinbase coinbase = (new CoinbaseBuilder()).build();
            OAuthCodeRequest request = new OAuthCodeRequest();
            request.setClientId(CLIENT_ID);
            request.setScope(scope);
            request.setRedirectUri(redirectUri);
            request.setMeta(meta);
            URI authorizationUri = coinbase.getAuthorizationUri(request);
            Uri androidUri = Uri.parse(authorizationUri.toString());
            androidUri = androidUri.buildUpon().appendQueryParameter("state", createLoginCSRFToken()).build();
            return Single.just(androidUri);
        }catch(CoinbaseException e){};
        return null;
    }

    public Single<OAuthTokensResponse> completeAuth (){
        Uri redirectUri = Uri.parse(cbredirectUrl);
        try {
            String csrfToken = redirectUri.getQueryParameter("state");
            String authCode = redirectUri.getQueryParameter("code");
            if (csrfToken != null && csrfToken.equals(getLoginCSRFToken())) {
                if (authCode == null) {
                    String ex1 = redirectUri.getQueryParameter("error_description");
                    throw new UnauthorizedException(ex1);
                } else {
                    try {
                        CSRFtoken = null;
                        Coinbase ex = (new CoinbaseBuilder()).build();
                        Uri redirectUriWithoutQuery = redirectUri.buildUpon().clearQuery().build();
                        tokens = ex.getTokens(CLIENT_ID, CLIENT_SECRET, authCode, redirectUriWithoutQuery.toString()); //javax.net.ssl.SSLHandshakeException: java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.
                        coinbaseInstance = new CoinbaseBuilder().withAccessToken(tokens.getAccessToken()).build();
                        existing_tokens.put(service.getTransaction().getPayerId(),tokens);
                        existing_instance.put(service.getTransaction().getPayerId(),coinbaseInstance);
                        return Single.just(tokens);
                    } catch (Exception var8) {
                        var8.printStackTrace();
                        throw new UnauthorizedException(var8.getMessage());
                    }
                }
            } else {
                throw new UnauthorizedException("CSRF Detected!");
            }
        } catch(UnauthorizedException e){;}
        catch(Exception e){;}
        return null;
    }

    public Single<Boolean> existsToken(){
      //  return Single.just(false);
        Coinbase cbinstance = existing_instance.get(service.getTransaction().getPayerId());
        try{
            cbinstance.getUser().getEmail();
            return Single.just(true);
        } catch(Exception e){ return Single.just(false);
        }
    }

    public Single<OAuthTokensResponse> getToken(){
        return Single.just(existing_tokens.get(service.getTransaction().getPayerId()));
    }
//    public void revokeToken(){
//        if(tokens != null){
//            try {
//                coinbaseInstance.revokeToken();
//            } catch (CoinbaseException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public void createTransaction(){
        if(!BitcoinTransactionService.REALTRANSACTION) {
            try {
                 String s = coinbaseInstance.getUser().getEmail();
                TransactionSimulator bitcoinTransactionSimulatoraux = new TransactionSimulator();
                bitcoinTransactionSimulator  = bitcoinTransactionSimulatoraux;
                String payerId = service.getTransaction().getPayerId();
                String productId = service.getTransaction().getProductId();
                service.addTStransaction(productId, payerId, bitcoinTransactionSimulatoraux);
                Single.just(true).delay(TransactionSimulator.TIME_FOR_TEST_TRANSACTION, TimeUnit.MILLISECONDS)
                        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(__ ->bitcoinTransactionSimulatoraux.startThread()).subscribe();
                setTransactionToPending();
            } catch (Exception e) { e.printStackTrace();
            }
        }
        else {
            NumberFormat formatter = new DecimalFormat("#");
            formatter.setMaximumFractionDigits(8);
            if (service.getProduct() != null) {
                Double p = (service.getProduct().getPrice().getAmount() * CONVERSION_RATE);
                String preco = formatter.format(p);
                try {
                    // if (!coinbaseInstance.getUser().getBalance().minus(p).isNegative()) {
                    com.coinbase.api.entity.Transaction coinbasetransaction = new Transaction();
                    coinbasetransaction.setTo(EMAIL); //mail da coinbase ou bitcoin address
                    coinbasetransaction.setAmount(Money.parse("BTC " + preco));
                    bitCBTransaction = coinbaseInstance.sendMoney(coinbasetransaction);
                    service.addCBtransaction(service.getTransaction().getProductId(), service.getTransaction().getPayerId(), bitCBTransaction);
                    setTransactionToPending();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isFinalStatus(){
        if(!BitcoinTransactionService.REALTRANSACTION) {
            TransactionSimulator.Estado state = bitcoinTransactionSimulator.getStatus();
            if (state.equals(TransactionSimulator.Estado.COMPLETED)
                    || state.equals(TransactionSimulator.Estado.FAILED)
                    || state.equals(TransactionSimulator.Estado.CANCELED)) {
                return true;
            }
            return false;
        }else{
            Transaction.DetailedStatus state = bitCBTransaction.getDetailedStatus();
            if(state.equals(Transaction.DetailedStatus.COMPLETED)
                    || state.equals(Transaction.DetailedStatus.FAILED)
                    || state.equals(Transaction.DetailedStatus.CANCELED)) {
                return true;
            }
            return false;
            }
    }

    public void handleTransactionStatus(TransactionSimulator transactionSimulator) {
        cm.aptoide.pt.v8engine.billing.transaction.Transaction transaction = service.getTransaction();
        if(!BitcoinTransactionService.REALTRANSACTION) {
            handleSimTransaction(transaction, transactionSimulator);
        }else{
            handleRealTransaction(transaction);
        }
    }

    private void handleSimTransaction(cm.aptoide.pt.v8engine.billing.transaction.Transaction transaction, TransactionSimulator transactionSimulator){
        switch (transactionSimulator.getStatus()) {
            case COMPLETED:
               // view.showCompleteToast("complete");
                service.createTransactionStatusUpdate(transaction.getSellerId(),transaction.getProductId(),
                        transaction.getPaymentMethodId(), transaction.getPayerId(),
                        cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.COMPLETED);
                break;
            case FAILED:
           //     view.showCompleteToast("failed");
                service.createTransactionStatusUpdate(transaction.getSellerId(),transaction.getProductId(),
                        transaction.getPaymentMethodId(), transaction.getPayerId(),
                        cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.FAILED);
                break;
            case CANCELED: //BitCoin Transactions cannot be canceled, however the SDK has a CANCELED STATUS so it's better to handle it
          //      view.showCompleteToast("canceled");
                service.createTransactionStatusUpdate(transaction.getSellerId(),transaction.getProductId(),
                        transaction.getPaymentMethodId(), transaction.getPayerId(),
                        cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.CANCELED);
                break;
            default:
        }
    }
////////////////Real Transaction///////////////////

    private void handleRealTransaction(cm.aptoide.pt.v8engine.billing.transaction.Transaction transaction){
        switch(bitCBTransaction.getDetailedStatus()){
            case COMPLETED:
        //        view.showCompleteToast("complete");
                service.createTransactionStatusUpdate(transaction.getSellerId(),transaction.getProductId(),
                        transaction.getPaymentMethodId(), transaction.getPayerId(),
                        cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.COMPLETED);
                break;
            case FAILED:
       //         view.showCompleteToast("failed");
                service.createTransactionStatusUpdate(transaction.getSellerId(),transaction.getProductId(),
                        transaction.getPaymentMethodId(), transaction.getPayerId(),
                        cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.FAILED);
                break;
            case CANCELED: //BitCoin Transactions cannot be canceled, however the SDK has a CANCELED STATUS so it's better to handle it
        //        view.showCompleteToast("canceled");
                service.createTransactionStatusUpdate(transaction.getSellerId(),transaction.getProductId(),
                        transaction.getPaymentMethodId(), transaction.getPayerId(),
                        cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.CANCELED);
                break;
            default:

        }
    }

    private void setTransactionToPending(){
        cm.aptoide.pt.v8engine.billing.transaction.Transaction transaction = service.getTransaction();
        service.createTransactionStatusUpdate(transaction.getSellerId(),transaction.getProductId(),
                transaction.getPaymentMethodId(), transaction.getPayerId(),
                cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.PENDING);
    }
}


