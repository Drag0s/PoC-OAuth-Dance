package com.dragosfotescu.myraco.OAuth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dragosfotescu.myraco.MainActivity;
import com.dragosfotescu.myraco.RacoUtils;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;

/**
 * Created by dragos on 9/06/15.
 */
public class PrepareRequestTokenActivity extends Activity {

    final String TAG = getClass().getName();

    private OAuthConsumer consumer;
    private OAuthProvider provider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            this.consumer = new CommonsHttpOAuthConsumer(Constants.CONSUMER_KEY, Constants.CONSUMER_SECRET);
            this.provider = new CommonsHttpOAuthProvider(Constants.REQUEST_URL,Constants.ACCESS_URL,Constants.AUTHORIZE_URL);
        } catch (Exception e) {
            Log.e(TAG, "Error creating consumer / provider",e);
        }

        Log.i(TAG, "Starting task to retrieve request token.");
        new OAuthRequestTokenTask(this,consumer,provider).execute();
    }

    /**
     * Called when the OAuthRequestTokenTask finishes (user has authorized the request token).
     * The callback URL will be intercepted here.
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Uri uri = intent.getData();
        if (uri != null && uri.getScheme().equals(Constants.OAUTH_CALLBACK_SCHEME)) {
            Log.i(TAG, "Callback received : " + uri);
            Log.i(TAG, "Retrieving Access Token");
            new RetrieveAccessTokenTask(this,consumer,provider,prefs).execute(uri);
            finish();
        }
    }

    public class RetrieveAccessTokenTask extends AsyncTask<Uri, Void, Void> {

        private Context context;
        private OAuthProvider provider;
        private OAuthConsumer consumer;
        private SharedPreferences prefs;

        public RetrieveAccessTokenTask(Context context, OAuthConsumer consumer,OAuthProvider provider, SharedPreferences prefs) {
            this.context = context;
            this.consumer = consumer;
            this.provider = provider;
            this.prefs=prefs;
        }


        /**
         * Retrieve the oauth_verifier, and store the oauth and oauth_token_secret
         * for future API calls.
         */
        @Override
        protected Void doInBackground(Uri...params) {
            final Uri uri = params[0];
            final String oauth_verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);

            try {
                provider.retrieveAccessToken(consumer, oauth_verifier);

                final SharedPreferences.Editor edit = prefs.edit();
                edit.putString(OAuth.OAUTH_TOKEN, consumer.getToken());
                edit.putString(OAuth.OAUTH_TOKEN_SECRET, consumer.getTokenSecret());
                edit.commit();

                String token = prefs.getString(OAuth.OAUTH_TOKEN, "");
                String secret = prefs.getString(OAuth.OAUTH_TOKEN_SECRET, "");

                consumer.setTokenWithSecret(token, secret);
                context.startActivity(new Intent(context,MainActivity.class));

//                executeAfterAccessTokenRetrieval();

                Log.i(TAG, "OAuth - Access Token Retrieved");

            } catch (Exception e) {
                Log.e(TAG, "OAuth - Access Token Retrieval Error", e);
            }
            try {
                String nom = RacoUtils.getNom(prefs);
                Log.i("GET NOM", nom);
                final SharedPreferences.Editor edit = prefs.edit();
                edit.putString("name", nom);
                edit.commit();
            } catch (Exception ex) {
                Log.e("GET NOM", "GetName error", ex);
            }

            return null;
        }


    }

}
