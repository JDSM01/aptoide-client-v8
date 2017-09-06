package cm.aptoide.pt.v8engine.billing.view.bitcoin;

import android.os.Bundle;

import cm.aptoide.pt.v8engine.billing.Billing;
import cm.aptoide.pt.v8engine.billing.BillingAnalytics;
import cm.aptoide.pt.v8engine.billing.authorization.coinbase.CoinbaseOAuth;
import cm.aptoide.pt.v8engine.billing.view.BillingNavigator;
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
    private final BillingAnalytics analytics;
    private final BillingNavigator navigator;
    private final String sellerId;
    private final String productId;
    private CoinbaseOAuth coinbaseOAuth;

    public CoinbasePresenter(WebViewFragment view, Billing billing, BillingAnalytics analytics,
                             BillingNavigator navigator, String sellerId, String productId, CoinbaseOAuth coinbaseOAuth) {
        this.view = view;
        this.billing = billing;
        this.analytics = analytics;
        this.navigator = navigator;
        this.sellerId = sellerId;
        this.productId = productId;
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
            view.getLifecycle()
                    .filter(event -> event.equals(View.LifecycleEvent.CREATE)).observeOn(Schedulers.io())
                    .flatMapSingle(__ -> coinbaseOAuth.beginAuth(REDIRECT_URI)).observeOn(AndroidSchedulers.mainThread())
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
                   .flatMapCompletable(__ -> billing.processLocalPayment(sellerId, productId, null, coinbaseOAuth.completeAuth().getAccessToken())
                           .observeOn(AndroidSchedulers.mainThread())
                           .doOnCompleted(() -> {
                               view.hideLoading();
                               navigator.popTransactionAuthorizationView();
                           }))
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
