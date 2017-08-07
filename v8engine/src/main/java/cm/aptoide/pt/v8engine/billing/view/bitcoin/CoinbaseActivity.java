package cm.aptoide.pt.v8engine.billing.view.bitcoin;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.coinbase.android.sdk.OAuth;
import com.coinbase.api.Coinbase;
import com.coinbase.api.CoinbaseBuilder;
import com.coinbase.api.entity.OAuthTokensResponse;
import com.coinbase.api.exception.CoinbaseException;

import cm.aptoide.pt.v8engine.R;
import roboguice.activity.RoboActivity;
import roboguice.util.RoboAsyncTask;


/**
 * Created by jose_messejana on 07-08-2017.
 */

public class CoinbaseActivity extends RoboActivity {
    static final String REDIRECT_URI = "aptoide://coinbase-oauth";
    static final String CLIENT_ID = "35193ae7364abb8a1a75bd97b52f437833aa802d9cb43ba8bedd16f64af09ab1";
    static final String CLIENT_SECRET = "98f9a4e6b144bf5729033fa0eb5a16986c83f48e70446fa7a178be6d7dc3cce0";

    public CoinbaseActivity(){
        Log.d("teste3","activity");
    }

   // @InjectView(R.id.text_view)
    private TextView mTextView;


    //DisplayEmailTask class


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
            mTextView.setText("There was an error fetching the user's email address: " + ex.getMessage());
        }

        @Override
        public void onSuccess(String email) {
            mTextView.setText("Success! The user's email address is: " + email);
        }
    }



    //CompleteAutorizathionTask class


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
            mTextView.setText("There was an error fetching access tokens using the auth code: " + ex.getMessage());
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals("android.intent.action.VIEW")) {
            new CompleteAuthorizationTask(intent).execute();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            OAuth.beginAuthorization(this, CLIENT_ID, "user", REDIRECT_URI, null);
        } catch (CoinbaseException ex) {
            mTextView.setText("There was an error redirecting to Coinbase: " + ex.getMessage());
        }
    }
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    } */
}
