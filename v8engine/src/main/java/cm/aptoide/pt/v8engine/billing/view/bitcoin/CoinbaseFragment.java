package cm.aptoide.pt.v8engine.billing.view.bitcoin;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.billing.Billing;
import cm.aptoide.pt.v8engine.billing.BillingAnalytics;
import cm.aptoide.pt.v8engine.billing.authorization.coinbase.CoinbaseOAuth;
import cm.aptoide.pt.v8engine.billing.view.BillingNavigator;
import cm.aptoide.pt.v8engine.billing.view.PaymentActivity;
import cm.aptoide.pt.v8engine.billing.view.PaymentThrowableCodeMapper;
import cm.aptoide.pt.v8engine.billing.view.PurchaseBundleMapper;
import cm.aptoide.pt.v8engine.billing.view.WebView;
import cm.aptoide.pt.v8engine.billing.view.WebViewFragment;


/**
 * Created by jose_messejana on 26-07-2017.
 */

public class CoinbaseFragment extends WebViewFragment implements WebView{

    private Billing billing;
    private BillingAnalytics billingAnalytics;
    private AptoideAccountManager accountManager;
    private CoinbaseOAuth coinbaseOAuth;

    public static Fragment create(Bundle bundle) {
        final CoinbaseFragment fragment = new CoinbaseFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        billing = ((V8Engine) getContext().getApplicationContext()).getBilling();
        billingAnalytics = ((V8Engine) getContext().getApplicationContext()).getBillingAnalytics();
        accountManager = ((V8Engine) getContext().getApplicationContext()).getAccountManager();
        coinbaseOAuth = (((V8Engine) getContext().getApplicationContext()).getCoinbaseOAuth());
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        attachPresenter(new CoinbasePresenter(this, billing, billingAnalytics,
                new BillingNavigator(new PurchaseBundleMapper(new PaymentThrowableCodeMapper()),
                        getActivityNavigator(), getFragmentNavigator(), accountManager),
                getArguments().getString(PaymentActivity.EXTRA_APPLICATION_ID),
                getArguments().getString(PaymentActivity.EXTRA_PAYMENT_METHOD_NAME),
                getArguments().getString(PaymentActivity.EXTRA_PRODUCT_ID),coinbaseOAuth), savedInstanceState);
    }
}
