package cm.aptoide.pt.v8engine.billing.authorization.coinbase;

import android.net.Uri;

import com.coinbase.api.Coinbase;
import com.coinbase.api.CoinbaseBuilder;
import com.coinbase.api.entity.OAuthCodeRequest;
import com.coinbase.api.entity.OAuthTokensResponse;
import com.coinbase.api.exception.CoinbaseException;
import com.coinbase.api.exception.UnauthorizedException;

import org.joda.money.Money;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import cm.aptoide.pt.v8engine.billing.transaction.BitcoinTransactionService;
import rx.Single;

/**
 * Created by jose_messejana on 18-08-2017.
 */

public class CoinbaseOAuth {
    public static String cbredirectUrl;
    static final String CLIENT_ID = "";
    static final String CLIENT_SECRET = "";
    private final BitcoinTransactionService service;
    private String CSRFtoken = null;
    private OAuthTokensResponse tokens;
    private Map<String, OAuthTokensResponse> existing_tokens = new HashMap<>();

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
            OAuthCodeRequest.Meta meta = null;
            String scope = "user";
            if(BitcoinTransactionService.REALTRANSACTION) {
                 meta = new OAuthCodeRequest.Meta();
                 meta.setSendLimitAmount(Money.parse("USD 1.0"));
                 meta.setSendLimitPeriod(OAuthCodeRequest.Meta.Period.DAILY);
                 scope = "user,send";
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
        return tokens;
    }

//    public void tokenVerification() {
//        OAuthTokensResponse token = existing_tokens.get(service.getCurrentTransaction().getPayerId());
//        if (token != null) {
//            try {
//                Coinbase coinbase = new CoinbaseBuilder().withAccessToken(token.getAccessToken()).build();
//                coinbase.getUser();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else {
//       try {
//           if (!existing_tokens.isEmpty()) {
//               for (OAuthTokensResponse tok : existing_tokens.values()) {
//                   Coinbase coinbase = new CoinbaseBuilder().withAccessToken(tok.getAccessToken()).build();
//                   try {
//                       coinbase.revokeToken();
//                   } catch (Exception e) {
//                       e.printStackTrace();
//                   }
//               }
//           }
//       }catch(Exception e){ e.printStackTrace();}
//        }
//    }

    public static BigDecimal getConversionRateusd(){
        Coinbase coinbaseInstance = (new CoinbaseBuilder()).build();
        try {
            return coinbaseInstance.getExchangeRates().get("usd_to_btc");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BigDecimal.valueOf(0);
    }

    public static BigDecimal getConversionRateeur(){
        Coinbase coinbaseInstance = (new CoinbaseBuilder()).build();
        try {
            return coinbaseInstance.getExchangeRates().get("eur_to_btc");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BigDecimal.valueOf(0);
    }
}

