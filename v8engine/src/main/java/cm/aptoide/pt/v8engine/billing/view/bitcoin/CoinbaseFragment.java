package cm.aptoide.pt.v8engine.billing.view.bitcoin;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.billing.Billing;
import cm.aptoide.pt.v8engine.billing.BillingAnalytics;
import cm.aptoide.pt.v8engine.billing.BillingSyncScheduler;
import cm.aptoide.pt.v8engine.billing.transaction.BitcoinTransactionService;
import cm.aptoide.pt.v8engine.billing.view.BillingNavigator;
import cm.aptoide.pt.v8engine.billing.view.PaymentThrowableCodeMapper;
import cm.aptoide.pt.v8engine.billing.view.PurchaseBundleMapper;
import cm.aptoide.pt.v8engine.billing.view.WebView;
import cm.aptoide.pt.v8engine.billing.view.WebViewFragment;


/**
 * Created by jose_messejana on 26-07-2017.
 */

public class CoinbaseFragment extends WebViewFragment implements WebView{

    private static final String EXTRA_PAYMENT_METHOD_ID =
            "cm.aptoide.pt.v8engine.billing.view.extra.PAYMENT_METHOD_ID";

    private Billing billing;
    private BillingSyncScheduler billingSyncScheduler;
    private BillingAnalytics billingAnalytics;
    private BitcoinTransactionService service;
    private int paymentMethodId;
    private CoinbaseActivity coinbase;
    private AptoideAccountManager accountManager;

    public static Fragment create(Bundle bundle, int paymentMethodId) {
        final CoinbaseFragment fragment = new CoinbaseFragment();
        bundle.putInt(EXTRA_PAYMENT_METHOD_ID, paymentMethodId);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        billing = ((V8Engine) getContext().getApplicationContext()).getBilling();
        billingAnalytics = ((V8Engine) getContext().getApplicationContext()).getBillingAnalytics();
        paymentMethodId = getArguments().getInt(EXTRA_PAYMENT_METHOD_ID);
        service = ((V8Engine) getContext().getApplicationContext()).getBitTransactionService();
        accountManager = ((V8Engine) getContext().getApplicationContext()).getAccountManager();
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        attachPresenter(new CoinbasePresenter(this, coinbase, billing, billingAnalytics,billingSyncScheduler,
                new BillingNavigator(new PurchaseBundleMapper(new PaymentThrowableCodeMapper()),
                        getActivityNavigator(), getFragmentNavigator(), accountManager), paymentMethodId, service), savedInstanceState);
    }
}
