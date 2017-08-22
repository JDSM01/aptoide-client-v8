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

import cm.aptoide.pt.v8engine.billing.Product;
import cm.aptoide.pt.v8engine.billing.transaction.BitcoinTransactionService;
import cm.aptoide.pt.v8engine.billing.view.WebViewFragment;
import cm.aptoide.pt.v8engine.billing.view.bitcoin.TransactionSimulator;
import rx.Observable;
import rx.Single;

/**
 * Created by jose_messejana on 18-08-2017.
 */

public class CoinbaseOAuth {
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

    public Single<Uri> beginAuth(String scope, String redirectUri){

        try {
            OAuthCodeRequest.Meta meta = null; // Comment this line to sendCoins
           // OAuthCodeRequest.Meta meta = new OAuthCodeRequest.Meta(); //Uncomment this line to send coins
           // meta.setSendLimitAmount(Money.parse("USD 0.5")); //Uncomment this line to send coins
           // meta.setSendLimitPeriod(OAuthCodeRequest.Meta.Period.DAILY); //Uncomment this line to send coins
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

    public Observable<Void> completeAuth (Uri redirectUri){
        String ex1;
        try {
            String csrfToken = redirectUri.getQueryParameter("state");
            String authCode = redirectUri.getQueryParameter("code");
            if (csrfToken != null && csrfToken.equals(getLoginCSRFToken())) {
                if (authCode == null) {
                    ex1 = redirectUri.getQueryParameter("error_description");
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
        Observable.just(null);
        return null;
    }

    public Boolean existsToken(){
        if(coinbaseInstance == null){ return false;}
        else return true;
    }

    public Observable<Void> getCoinbaseUserEmail(){
        try{
            String s = coinbaseInstance.getUser().getEmail();
            bitcoinTransactionSimulator = new TransactionSimulator();
            service.addTStransaction(service.getTransaction().getProductId(),service.getTransaction().getPayerId(),bitcoinTransactionSimulator);
            bitcoinTransactionSimulator.startThread();
        } catch(Exception e ){}
        return null;
    }

    public TransactionSimulator getTransactionSimulation(){
        return bitcoinTransactionSimulator;
    }

    public boolean isFinalStatus(){
        TransactionSimulator.Estado state = bitcoinTransactionSimulator.getStatus();
        if(state.equals(TransactionSimulator.Estado.COMPLETE)
                || state.equals(TransactionSimulator.Estado.FAILED)
                || state.equals(TransactionSimulator.Estado.CANCELED)) {
            return true;
        }
        return false;
    }

    public void handleTransactionStatus(WebViewFragment view) {
        switch(bitcoinTransactionSimulator.getStatus()){
            case COMPLETE:
                view.showCompleteToast("complete");
                service.createTransactionStatusUpdate(service.getTransaction().getProductId(),
                        service.getTransaction().getPaymentMethodId(), service.getTransaction().getPayerId(),
                        cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.COMPLETED);
                break;
            case FAILED:
                view.showCompleteToast("failed");
                service.createTransactionStatusUpdate(service.getTransaction().getProductId(),
                        service.getTransaction().getPaymentMethodId(), service.getTransaction().getPayerId(),
                        cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.FAILED);
                break;
            case CANCELED: //BitCoin Transactions cannot be canceled, however the SDK has a CANCELED STATUS so it's better to handle it
                view.showCompleteToast("canceled");
                service.createTransactionStatusUpdate(service.getTransaction().getProductId(),
                        service.getTransaction().getPaymentMethodId(), service.getTransaction().getPayerId(),
                        cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.CANCELED);
                break;
            default:

        }
    }
////////////////Real Transaction///////////////////


    public void sendCoins(WebViewFragment view){
        Double p = price*CONVERSION_RATE;
        NumberFormat formatter = new DecimalFormat("#.########");
        String preco = formatter.format(p);
        try {
            if (!coinbaseInstance.getBalance().minus(p).isNegative()) {
                com.coinbase.api.entity.Transaction coinbasetransaction = new Transaction();
                coinbasetransaction.setTo(EMAIL); //mail da coinbase ou bitcoin address
                coinbasetransaction.setAmount(Money.parse("BTC " + preco));
                bitCBTransaction = coinbaseInstance.sendMoney(coinbasetransaction);
                hash = bitCBTransaction.getHash();
            }
            else {
                view.showCompleteToast("Not enough money");
            }
        }catch (Exception e) {
                e.printStackTrace();
            }
        }

    public Transaction getCBTransaction(){
        return bitCBTransaction;
    }

    public void handleCBTransactionStatus(WebViewFragment view) {
        switch(bitCBTransaction.getDetailedStatus()){
            case COMPLETED:
                view.showCompleteToast("complete");
                service.createTransactionStatusUpdate(service.getTransaction().getProductId(),
                        service.getTransaction().getPaymentMethodId(), service.getTransaction().getPayerId(),
                        cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.COMPLETED);
                break;
            case FAILED:
                view.showCompleteToast("failed");
                service.createTransactionStatusUpdate(service.getTransaction().getProductId(),
                        service.getTransaction().getPaymentMethodId(), service.getTransaction().getPayerId(),
                        cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.FAILED);
                break;
            case CANCELED: //BitCoin Transactions cannot be canceled, however the SDK has a CANCELED STATUS so it's better to handle it
                view.showCompleteToast("canceled");
                service.createTransactionStatusUpdate(service.getTransaction().getProductId(),
                        service.getTransaction().getPaymentMethodId(), service.getTransaction().getPayerId(),
                        cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.CANCELED);
                break;
            default:

        }
    }

    public boolean isFinalCBTransactionStatus(){
        Transaction.DetailedStatus state = bitCBTransaction.getDetailedStatus();
        if(state.equals(Transaction.DetailedStatus.COMPLETED)
                || state.equals(Transaction.DetailedStatus.FAILED)
                || state.equals(Transaction.DetailedStatus.CANCELED)) {
            return true;
        }
        return false;
    }

    public void setPrice(Product product){
        price = product.getPrice().getAmount();
    }

}


