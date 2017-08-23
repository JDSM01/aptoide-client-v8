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
import java.util.Random;

import cm.aptoide.pt.v8engine.billing.BitcoinBillingService;
import cm.aptoide.pt.v8engine.billing.Product;
import cm.aptoide.pt.v8engine.billing.transaction.BitcoinTransactionService;
import cm.aptoide.pt.v8engine.billing.view.WebViewFragment;
import cm.aptoide.pt.v8engine.billing.view.bitcoin.TransactionSimulator;
import rx.Single;

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
    private Coinbase coinbaseInstance;
    private String CSRFtoken = null;
    private OAuthTokensResponse tokens;
    private Transaction bitCBTransaction = null;
    private TransactionSimulator bitcoinTransactionSimulator = null;
    private String hash;
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
            if(BitcoinBillingService.REALTRANSACTION) {
                 meta = new OAuthCodeRequest.Meta();
                 meta.setSendLimitAmount(Money.parse("USD 0.9"));
                 meta.setSendLimitPeriod(OAuthCodeRequest.Meta.Period.DAILY);
                 scope = "wallet:transactions:send"; // wallet:transactions:transfer
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

    public void completeAuth (){
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
                        Coinbase ex = (new CoinbaseBuilder()).build();
                        Uri redirectUriWithoutQuery = redirectUri.buildUpon().clearQuery().build();
                        tokens = ex.getTokens(CLIENT_ID, CLIENT_SECRET, authCode, redirectUriWithoutQuery.toString());
                        coinbaseInstance = new CoinbaseBuilder().withAccessToken(tokens.getAccessToken()).build();
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
    }

    public Single<Boolean> existsToken(){
        try{
            coinbaseInstance.getUser().getEmail();
            return Single.just(true);
        } catch(Exception e){ return Single.just(false);
        }
    }

    public Single<String> getCoinbaseUserEmail(){
        if(!BitcoinBillingService.REALTRANSACTION) {
            try {
                String s = coinbaseInstance.getUser().getEmail();
                bitcoinTransactionSimulator = new TransactionSimulator();
                service.addTStransaction(service.getTransaction().getProductId(), service.getTransaction().getPayerId(), bitcoinTransactionSimulator);
                bitcoinTransactionSimulator.startThread();
                Single.just(s);
            } catch (Exception e) {
            }
        }
            return null;
    }

    public boolean isFinalStatus(){
        if(!BitcoinBillingService.REALTRANSACTION) {
            TransactionSimulator.Estado state = bitcoinTransactionSimulator.getStatus();
            if (state.equals(TransactionSimulator.Estado.COMPLETE)
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

    public void handleTransactionStatus(WebViewFragment view) {
        cm.aptoide.pt.v8engine.billing.transaction.Transaction transaction = service.getTransaction();
        if(!BitcoinBillingService.REALTRANSACTION) {
            switch (bitcoinTransactionSimulator.getStatus()) {
                case COMPLETE:
                    view.showCompleteToast("complete");
                    service.createTransactionStatusUpdate(transaction.getSellerId(),transaction.getProductId(),
                            transaction.getPaymentMethodId(), transaction.getPayerId(),
                            cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.COMPLETED);
                    break;
                case FAILED:
                    view.showCompleteToast("failed");
                    service.createTransactionStatusUpdate(transaction.getSellerId(),transaction.getProductId(),
                            transaction.getPaymentMethodId(), transaction.getPayerId(),
                            cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.FAILED);
                    break;
                case CANCELED: //BitCoin Transactions cannot be canceled, however the SDK has a CANCELED STATUS so it's better to handle it
                    view.showCompleteToast("canceled");
                    service.createTransactionStatusUpdate(transaction.getSellerId(),transaction.getProductId(),
                            transaction.getPaymentMethodId(), transaction.getPayerId(),
                            cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.CANCELED);
                    break;
                default:
            }
        }else{
            switch(bitCBTransaction.getDetailedStatus()){
                case COMPLETED:
                    view.showCompleteToast("complete");
                    service.createTransactionStatusUpdate(transaction.getSellerId(),transaction.getProductId(),
                            transaction.getPaymentMethodId(), transaction.getPayerId(),
                            cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.COMPLETED);
                    break;
                case FAILED:
                    view.showCompleteToast("failed");
                    service.createTransactionStatusUpdate(transaction.getSellerId(),transaction.getProductId(),
                            transaction.getPaymentMethodId(), transaction.getPayerId(),
                            cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.FAILED);
                    break;
                case CANCELED: //BitCoin Transactions cannot be canceled, however the SDK has a CANCELED STATUS so it's better to handle it
                    view.showCompleteToast("canceled");
                    service.createTransactionStatusUpdate(transaction.getSellerId(),transaction.getProductId(),
                            transaction.getPaymentMethodId(), transaction.getPayerId(),
                            cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.CANCELED);
                    break;
                default:

            }
        }
    }
////////////////Real Transaction///////////////////


    public void sendCoins(WebViewFragment view){
        if(BitcoinBillingService.REALTRANSACTION) {
            Double p = price * CONVERSION_RATE;
            NumberFormat formatter = new DecimalFormat("#.########");
            String preco = formatter.format(p);
            try {
                if (!coinbaseInstance.getUser().getBalance().minus(p).isNegative()) {
                    com.coinbase.api.entity.Transaction coinbasetransaction = new Transaction();
                    coinbasetransaction.setTo(EMAIL); //mail da coinbase ou bitcoin address
                    coinbasetransaction.setAmount(Money.parse("BTC " + preco));
                    bitCBTransaction = coinbaseInstance.sendMoney(coinbasetransaction);
                    hash = bitCBTransaction.getHash();
                } else {
                    view.showCompleteToast("Not enough money");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        }

    public void setPrice(Product product){
        price = product.getPrice().getAmount();
    }

}


