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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.v8engine.billing.transaction.BitcoinTransactionService;
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

    public OAuthTokensResponse completeAuth (){
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
                        existing_tokens.put(service.getCurrentTransaction().getPayerId(),tokens);
                        return tokens;
                    } catch (Exception var8) {
                        var8.printStackTrace();
                        throw new UnauthorizedException(var8.getMessage());
                    }
                }
            } else {
                throw new UnauthorizedException("CSRF Detected!");
            }
        } catch(UnauthorizedException e){e.printStackTrace();}
        catch(Exception e){e.printStackTrace();}
        Logger.e("teste3","not here");
        return tokens;
    }

    public Single<Boolean> existsToken(){
      //  return Single.just(false);
        OAuthTokensResponse token = existing_tokens.get(service.getCurrentTransaction().getPayerId());
        try{
            Coinbase coinbase = new CoinbaseBuilder().withAccessToken(token.getAccessToken()).build();
            coinbase.getUser();
            return Single.just(true);
        } catch(Exception e){ return Single.just(false);
        }
    }

    public Single<OAuthTokensResponse> getToken(){
        return Single.just(existing_tokens.get(service.getCurrentTransaction().getPayerId()));
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

}


