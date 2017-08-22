package cm.aptoide.pt.v8engine.billing.view.bitcoin;

import android.net.Uri;
import android.os.Bundle;

import cm.aptoide.pt.v8engine.billing.Billing;
import cm.aptoide.pt.v8engine.billing.BillingAnalytics;
import cm.aptoide.pt.v8engine.billing.Product;
import cm.aptoide.pt.v8engine.billing.authorization.coinbase.CoinbaseOAuth;
import cm.aptoide.pt.v8engine.billing.transaction.BitcoinTransactionService;
import cm.aptoide.pt.v8engine.billing.view.BillingNavigator;
import cm.aptoide.pt.v8engine.billing.view.WebViewFragment;
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
    public static String redirect;
    private final WebViewFragment view;
    private final Billing billing;
    private final BillingAnalytics analytics;
    private final BillingNavigator navigator;
    private final String sellerId;
    private final String paymentMethodName;
    private final String productId;
    private final BitcoinTransactionService service;
    private CoinbaseOAuth coinbaseOAuth;

    public CoinbasePresenter(WebViewFragment view, Billing billing, BillingAnalytics analytics,
                             BillingNavigator navigator, String sellerId, String paymentMethodName, String productId
            , BitcoinTransactionService service, CoinbaseOAuth coinbaseOAuth) {
        this.view = view;
        this.billing = billing;
        this.analytics = analytics;
        this.navigator = navigator;
        this.sellerId = sellerId;
        this.paymentMethodName = paymentMethodName;
        this.productId = productId;
        this.service = service;
        this.coinbaseOAuth = coinbaseOAuth;

    }

    @Override
    public void present() {
        onViewCreateBitcoinPayment();
        handleDismissEvent();
        handleRedirectUrlEvent();
        handleBackButtonEvent();
    }

    private void onViewCreateBitcoinPayment() {
       // if(!coinbaseOAuth.existsToken()) {
            view.getLifecycle()
                    .filter(event -> event.equals(View.LifecycleEvent.CREATE)).observeOn(Schedulers.io())
                    .flatMapSingle(__ -> coinbaseOAuth.beginAuth("user", REDIRECT_URI)).observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(uri -> view.loadWebsitewithContainingRedirect(uri.toString(), REDIRECT_URI))
                    .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
                    .subscribe(__ -> {
                    }, throwable -> {
                        view.showError();
                        view.hideLoading();
                    });
      //  }
       /* else{
            view.getLifecycle()
                    .filter(event -> event.equals(View.LifecycleEvent.CREATE))
                    .doOnNext(__ -> view.setPaymentMethod(paymentMethodId)).observeOn(Schedulers.io())
                    //.doOnNext(__ -> coinbaseOAuth.sendCoins()) //uncomment for real transaction
                    .doOnNext(__ -> coinbaseOAuth.getCoinbaseUserEmail()).observeOn(AndroidSchedulers.mainThread())//simulation of Sending coins not needed on the real one
                    .doOnNext(__ -> view.showProgressBar())
                    .filter(__ -> coinbaseOAuth.isFinalStatus()) //to send coins replace with isFinalCBTransactionStatus()
                    .doOnNext(__ -> coinbaseOAuth.handleTransactionStatus(view)) //to send coins replace with handleCBTransactionStatus()
                    .doOnNext( __ -> navigator.popTransactionAuthorizationView())

                    .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
                    .subscribe(__ -> {
                    }, throwable -> {
                        view.hideLoading();
                        view.showError();
                    });
        } */
    }



    private void handleRedirectUrlEvent() {
            view.getLifecycle()
                    .filter(event -> event.equals(View.LifecycleEvent.CREATE))
                    .flatMap(created -> view.redirectUrlEvent()).observeOn(Schedulers.io())
                    .doOnNext(__ -> coinbaseOAuth.completeAuth(Uri.parse(redirect)))
                    //.doOnNext(__ -> coinbaseOAuth.sendCoins(view)) //uncomment for real transaction
                    .doOnNext(__ -> coinbaseOAuth.getCoinbaseUserEmail()).observeOn(AndroidSchedulers.mainThread())//simulation of Sending coins not needed on the real one
                    .filter(__ -> coinbaseOAuth.isFinalStatus()) //to send coins replace with isFinalCBTransactionStatus()
                    .doOnNext(__ -> coinbaseOAuth.handleTransactionStatus(view)) //to send coins replace with handleCBTransactionStatus()
                    .doOnNext( __ -> navigator.popTransactionAuthorizationView())

        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
                    .subscribe(__ -> {
                    }, throwable -> {
                        view.hideLoading();
                        view.showError();
                    });

    }

    private void handleBackButtonEvent() {
        view.getLifecycle()
                .filter(event -> event.equals(View.LifecycleEvent.CREATE))
                .flatMap(created -> view.backButtonEvent())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
                .subscribe(__ -> {
                }, throwable -> {
                    navigator.popTransactionAuthorizationView();
                    view.hideLoading();
                    view.showError();
                });
    }

    private void handleDismissEvent() {
        view.getLifecycle()
                .filter(event -> event.equals(View.LifecycleEvent.CREATE))
                .flatMap(created -> view.errorDismissedEvent())
                .doOnNext(dismiss -> navigator.popTransactionAuthorizationView())
                .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
                .subscribe();
    }

    @Override
    public void saveState(Bundle state) {

    }

    @Override
    public void restoreState(Bundle state) {

    }


}
