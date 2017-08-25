package cm.aptoide.pt.v8engine.billing.view.bitcoin;

import android.os.Bundle;

import cm.aptoide.pt.v8engine.billing.Billing;
import cm.aptoide.pt.v8engine.billing.BillingAnalytics;
import cm.aptoide.pt.v8engine.billing.BillingSyncScheduler;
import cm.aptoide.pt.v8engine.billing.authorization.coinbase.CoinbaseOAuth;
import cm.aptoide.pt.v8engine.billing.view.BillingNavigator;
import cm.aptoide.pt.v8engine.billing.view.ProductProvider;
import cm.aptoide.pt.v8engine.billing.view.WebViewFragment;
import cm.aptoide.pt.v8engine.presenter.Presenter;
import cm.aptoide.pt.v8engine.presenter.View;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by jose_messejana on 27-07-2017.
 */

public class CoinbasePresenter implements Presenter {
    static final String REDIRECT_URI = "https://en.aptoide.com//coinbase-oauth";
    private final WebViewFragment view;
    private final Billing billing;
    private final ProductProvider productProvider;
    private final BillingSyncScheduler syncScheduler;
    private final BillingAnalytics analytics;
    private final BillingNavigator navigator;
    private CoinbaseOAuth coinbaseOAuth;
    private int paymentMethodId;
    private boolean tokenexists = false;

    public CoinbasePresenter(WebViewFragment view, Billing billing, BillingAnalytics analytics, BillingSyncScheduler syncScheduler,
                             ProductProvider productProvider, BillingNavigator navigator, int paymentMethodId, CoinbaseOAuth coinbaseOAuth) {
        this.view = view;
        this.billing = billing;
        this.analytics = analytics;
        this.syncScheduler = syncScheduler;
        this.productProvider = productProvider;
        this.navigator = navigator;
        this.paymentMethodId = paymentMethodId;
        this.coinbaseOAuth = coinbaseOAuth;

    }

    @Override
    public void present() {
        onViewCreateBitcoinPayment();
        handleDismissEvent();
        handleRedirectUrlEvent();
        handleBackButtonEvent();
    }
//"user,wallet:transactions:send", replace with user for real transaction wallet:transactions:transfer
    private void onViewCreateBitcoinPayment() {
            view.getLifecycle()
                    .filter(event -> event.equals(View.LifecycleEvent.CREATE))
                    .doOnNext(__ -> view.setPaymentMethod(paymentMethodId)).observeOn(Schedulers.io())
                    .flatMapSingle(exists -> coinbaseOAuth.existsToken())
                    .flatMapSingle(exists -> {
                        if (exists){ tokenexists = true; /*return Single.just(REDIRECT_URI);*/ }
                        else{ tokenexists = false; }return coinbaseOAuth.beginAuth(REDIRECT_URI); }).observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(uri -> view.loadWebsitewithContainingRedirect(uri.toString(), REDIRECT_URI))
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
                    .flatMapSingle(__ -> productProvider.getProduct())
                    .doOnNext(product -> coinbaseOAuth.setPrice(product))
                    .doOnNext(__ -> {
                        if(!tokenexists){ coinbaseOAuth.completeAuth();}
                    })
                    .doOnNext(__ -> coinbaseOAuth.sendCoins(view))
                    .doOnNext(__ -> coinbaseOAuth.getCoinbaseUserEmail()).observeOn(AndroidSchedulers.mainThread())
                    .filter(__ -> coinbaseOAuth.isFinalStatus())
                    .doOnNext(__ -> coinbaseOAuth.handleTransactionStatus(view))
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


}
