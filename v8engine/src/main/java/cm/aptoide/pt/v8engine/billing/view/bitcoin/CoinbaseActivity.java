package cm.aptoide.pt.v8engine.billing.view.bitcoin;

import android.content.Intent;
import android.os.Bundle;

import com.coinbase.android.sdk.OAuth;
import com.coinbase.api.Coinbase;
import com.coinbase.api.CoinbaseBuilder;
import com.coinbase.api.entity.OAuthTokensResponse;
import com.coinbase.api.exception.CoinbaseException;

import cm.aptoide.pt.v8engine.R;
import roboguice.activity.RoboActivity;
import roboguice.util.RoboAsyncTask;
/**
 * Created by jose_messejana on 10-08-2017.
 */

public class CoinbaseActivity extends RoboActivity {
    private int paymentId;
    static final String REDIRECT_URI = "https://en.aptoide.com//coinbase-oauth";
    static final String CLIENT_ID = "35193ae7364abb8a1a75bd97b52f437833aa802d9cb43ba8bedd16f64af09ab1";
    static final String CLIENT_SECRET = "98f9a4e6b144bf5729033fa0eb5a16986c83f48e70446fa7a178be6d7dc3cce0";

    @Override
    protected void onNewIntent(final Intent intent) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals("android.intent.action.VIEW")) {
            new CompleteAuthorizationTask(intent).execute();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // this.paymentId = paymentId;
        setContentView(R.layout.activity_main);
        try {
            OAuth.beginAuthorization(this, CLIENT_ID, "user", REDIRECT_URI, null);
        } catch (CoinbaseException ex) {
        }
    }

    public class CompleteAuthorizationTask extends RoboAsyncTask<OAuthTokensResponse> {
        private Intent mIntent;

        public CompleteAuthorizationTask(Intent intent) {
            super(CoinbaseActivity.this);
            mIntent = intent;
        }

        @Override
        public OAuthTokensResponse call() throws Exception {
            return OAuth.completeAuthorization(CoinbaseActivity.this, CLIENT_ID, CLIENT_SECRET, mIntent.getData());
        }

        @Override
        public void onSuccess(OAuthTokensResponse tokens) {
            new DisplayEmailTask(tokens).execute();
        }

        @Override
        public void onException(Exception ex) {
        }
    }

    public class DisplayEmailTask extends RoboAsyncTask<String> {
        private OAuthTokensResponse mTokens;

        public DisplayEmailTask(OAuthTokensResponse tokens) {
            super(CoinbaseActivity.this);
            mTokens = tokens;
        }

        public String call() throws Exception {
            Coinbase coinbase = new CoinbaseBuilder().withAccessToken(mTokens.getAccessToken()).build();
            return coinbase.getUser().getEmail();
        }

        @Override
        public void onException(Exception ex) {
        }

        @Override
        public void onSuccess(String email) {
        }
    }



}
