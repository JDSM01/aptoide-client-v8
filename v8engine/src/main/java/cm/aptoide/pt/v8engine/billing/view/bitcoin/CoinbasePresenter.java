package cm.aptoide.pt.v8engine.billing.view.bitcoin;

import android.os.Bundle;

import cm.aptoide.pt.v8engine.billing.Billing;
import cm.aptoide.pt.v8engine.billing.BillingAnalytics;
import cm.aptoide.pt.v8engine.billing.BillingSyncScheduler;
import cm.aptoide.pt.v8engine.billing.transaction.BitcoinTransactionService;
import cm.aptoide.pt.v8engine.billing.view.BillingNavigator;
import cm.aptoide.pt.v8engine.billing.view.ProductProvider;
import cm.aptoide.pt.v8engine.billing.view.WebView;
import cm.aptoide.pt.v8engine.presenter.Presenter;
import cm.aptoide.pt.v8engine.presenter.View;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by jose_messejana on 27-07-2017.
 */

public class CoinbasePresenter implements Presenter {
    static final String REDIRECT_URI = "aptoide://coinbase-oauth";
    static final String CLIENT_ID = "35193ae7364abb8a1a75bd97b52f437833aa802d9cb43ba8bedd16f64af09ab1";
    private final WebView view;
    private final Billing billing;
    private final ProductProvider productProvider;
    private final BillingSyncScheduler syncScheduler;
    private final BillingAnalytics analytics;
    private final BillingNavigator navigator;
    private final BitcoinTransactionService service;
    private final CoinbaseActivity coinbase;

    private final int paymentMethodId;

    public CoinbasePresenter(WebView view, CoinbaseActivity coinbase, Billing billing, BillingAnalytics analytics,BillingSyncScheduler syncScheduler,
                             ProductProvider productProvider, BillingNavigator navigator, int paymentMethodId, BitcoinTransactionService service) {
        this.view = view;
        this.coinbase=coinbase;
        this.billing = billing;
        this.analytics = analytics;
        this.syncScheduler = syncScheduler;
        this.productProvider = productProvider;
        this.navigator = navigator;
        this.paymentMethodId = paymentMethodId;
        this.service = service;

    }

    @Override
    public void present() {
        //handlers
        onViewCreateBitcoinPayment();
        handleDismissEvent();
        handleRedirectUrlEvent();
    }

    private void onViewCreateBitcoinPayment() {
        view.getLifecycle()
                .filter(event -> event.equals(View.LifecycleEvent.CREATE))

              //  .doOnNext(__ -> new CoinbaseActivity())
                .doOnNext(__ -> view.loadWebsite("http://www.yourhtmlsource.com/myfirstsite/",
                       "http://www.yourhtmlsource.com/starthere/"))
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
                    .flatMap(created -> view.redirectUrlEvent())
                    .doOnNext(transaction -> {
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
}
