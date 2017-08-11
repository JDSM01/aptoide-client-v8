package cm.aptoide.pt.v8engine.billing.view.bitcoin;

import android.net.Uri;
import android.os.Bundle;

import com.coinbase.api.Coinbase;
import com.coinbase.api.CoinbaseBuilder;
import com.coinbase.api.entity.OAuthCodeRequest;
import com.coinbase.api.entity.OAuthTokensResponse;
import com.coinbase.api.exception.CoinbaseException;
import com.coinbase.api.exception.UnauthorizedException;

import java.net.URI;
import java.util.Random;

import cm.aptoide.pt.spotandshare.socket.Log;
import cm.aptoide.pt.v8engine.billing.Billing;
import cm.aptoide.pt.v8engine.billing.BillingAnalytics;
import cm.aptoide.pt.v8engine.billing.BillingSyncScheduler;
import cm.aptoide.pt.v8engine.billing.transaction.BitcoinTransactionService;
import cm.aptoide.pt.v8engine.billing.view.BillingNavigator;
import cm.aptoide.pt.v8engine.billing.view.WebView;
import cm.aptoide.pt.v8engine.presenter.Presenter;
import cm.aptoide.pt.v8engine.presenter.View;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by jose_messejana on 27-07-2017.
 */

public class CoinbasePresenter implements Presenter {
    static final String REDIRECT_URI = "https://en.aptoide.com//coinbase-oauth";
    static final String CLIENT_ID = "35193ae7364abb8a1a75bd97b52f437833aa802d9cb43ba8bedd16f64af09ab1";
    static final String CLIENT_SECRET = "98f9a4e6b144bf5729033fa0eb5a16986c83f48e70446fa7a178be6d7dc3cce0";
    public static String redirect;
    private final WebView view;
    private final Billing billing;
    private final BillingSyncScheduler syncScheduler;
    private final BillingAnalytics analytics;
    private final BillingNavigator navigator;
    private final BitcoinTransactionService service;
    private Coinbase coinbaseInstance;
    private Uri androidUri;
    private String CSRFtoken = null;
    private OAuthTokensResponse tokens;

    private final int paymentMethodId;

    public CoinbasePresenter(WebView view, CoinbaseActivity coinbase, Billing billing, BillingAnalytics analytics, BillingSyncScheduler syncScheduler,
                             BillingNavigator navigator, int paymentMethodId, BitcoinTransactionService service) {
        this.view = view;
        this.billing = billing;
        this.analytics = analytics;
        this.syncScheduler = syncScheduler;
        this.navigator = navigator;
        this.paymentMethodId = paymentMethodId;
        this.service = service;

    }

    @Override
    public void present() {
        onViewCreateBitcoinPayment();
        handleDismissEvent();
        handleRedirectUrlEvent();
    }

    private void onViewCreateBitcoinPayment() {
        view.getLifecycle()
                .filter(event -> event.equals(View.LifecycleEvent.CREATE))
                .doOnNext(__ -> view.loadWebsitewithContainingRedirect(beginAuth(CLIENT_ID, "user", REDIRECT_URI, null).toString(),
                       REDIRECT_URI))
                //.doOnNext(__ -> switchActivity())
                .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
                .subscribe(__ -> {
                }, throwable -> {
                    view.showError();
                    view.hideLoading();
                });
    }



    private void handleRedirectUrlEvent() {

            view.getLifecycle()
                    .filter(event -> event.equals(View.LifecycleEvent.CREATE))
                    .flatMap(created -> view.redirectUrlEvent()).observeOn(Schedulers.io())
                    .doOnNext(__ -> completeAuth(CLIENT_ID,CLIENT_SECRET,Uri.parse(redirect)))
                    .doOnNext(__ -> createCoinbaseInstance())
                    .doOnNext(__ -> getCoinbaseUserEmail()).observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(__ -> {
                                service.createTransactionCompleted(service.getTransaction().getProductId(),
                                        service.getTransaction().getPaymentMethodId(), service.getTransaction().getPayerId());
                            }
                            )
                            .doOnNext( __ -> navigator.popTransactionAuthorizationView())

                    .observeOn(AndroidSchedulers.mainThread())

        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
                    .subscribe(__ -> {
                    }, throwable -> {
                        view.hideLoading();
                        view.showError();
                    });

    }


    @Override
    public void saveState(Bundle state) {

    }

    @Override
    public void restoreState(Bundle state) {

    }

    private void handleDismissEvent() {
        view.getLifecycle()
                .filter(event -> event.equals(View.LifecycleEvent.CREATE))
                .flatMap(created -> view.errorDismissedEvent())
                .doOnNext(dismiss -> navigator.popTransactionAuthorizationView())
                .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
                .subscribe();
    }


    private Uri beginAuth(String clientId, String scope, String redirectUri, OAuthCodeRequest.Meta meta){
        try {
            Coinbase coinbase = (new CoinbaseBuilder()).build();
            OAuthCodeRequest request = new OAuthCodeRequest();
            request.setClientId(clientId);
            request.setScope(scope);
            request.setRedirectUri(redirectUri);
            request.setMeta(meta);
            URI authorizationUri = coinbase.getAuthorizationUri(request);
            androidUri = Uri.parse(authorizationUri.toString());
            androidUri = androidUri.buildUpon().appendQueryParameter("state", createLoginCSRFToken()).build();
            return androidUri;
        }catch(CoinbaseException e){};
        return null;
    }

    private String createLoginCSRFToken(){
        int result = (new Random()).nextInt();
        CSRFtoken = Integer.toString(result);
        return CSRFtoken;
    }

    private String getLoginCSRFToken(){
        if(CSRFtoken == null){
            createLoginCSRFToken();
        }
        return CSRFtoken;
    }

    private Observable<Void> completeAuth (String clientId, String clientSecret, Uri redirectUri){
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
                        tokens = ex.getTokens(clientId, clientSecret, authCode, redirectUriWithoutQuery.toString());
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


    private Observable<Void> createCoinbaseInstance(){
        coinbaseInstance = new CoinbaseBuilder().withAccessToken(tokens.getAccessToken()).build();
        return null;
    }

    private Observable<Void> getCoinbaseUserEmail(){
        try{
            String email = coinbaseInstance.getUser().getEmail();
        } catch(Exception e ){}
        return null;
    }

}
