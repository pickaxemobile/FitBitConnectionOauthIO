package com.pickaxemobile.fitbitconnectiontest;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import io.oauth.OAuth;
import io.oauth.OAuthCallback;
import io.oauth.OAuthData;
import io.oauth.OAuthRequest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class ConnectionActivity extends Activity implements OAuthCallback {
    private TextView nameTextView;
    private TextView stepGoalTextView;
    private static String TAG = "FITBIT";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        nameTextView = (TextView)findViewById(R.id.name);
        stepGoalTextView = (TextView)findViewById(R.id.stepGoal);


    }


    public void connect(View v){
        final OAuth oauth = new OAuth(this);
       // Log.d(TAG, "pub key: " + MyFitbitConnectionTest.OAUTH_IO_PUBLIC_KEY);
        oauth.initialize(MyFitbitConnectionTest.OAUTH_IO_PUBLIC_KEY); //OAuth.io public key, not Fitbit's
        oauth.popup("fitbit", ConnectionActivity.this);
        //TODO seems bad to return to main activity with callback
    }

    public void onFinished(OAuthData data) {
        Log.d(MyFitbitConnectionTest.TAG, "in on finished, data: "+ data.toString());
        if ( ! data.status.equals("success")) {
            nameTextView.setTextColor(Color.parseColor("#FF0000"));
            nameTextView.setText("error, " + data.error);
        }
        Log.d(MyFitbitConnectionTest.TAG, "data provider: " + data.provider + " data http" + data.request.toString());
            // You can access the tokens through data.token and data.secret


            // Let's skip the NetworkOnMainThreadException for the purpose of this sample.

        //TODO definitely have to make this async
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

            // To make an authenticated request, you can implement OAuthRequest with your preferred way.
            // Here, we use an URLConnection (HttpURLConnection) but you can use any library.
        getProfileData(data);
        getGoalData(data);
    }

    private void getProfileData(OAuthData data){
        data.http(data.provider.equals("fitbit") ? "/1/user/-/profile.json" : "/1.1/account/verify_credentials.json", new OAuthRequest() {
            private URL url;
            private URLConnection con;

            @Override
            public void onSetURL(String _url) {
                Log.d(MyFitbitConnectionTest.TAG, "onsetURL url:" + _url);

                try {
                    url = new URL(_url);
                    con = url.openConnection();
                } catch (Exception e) { e.printStackTrace(); }
            }

            @Override
            public void onSetHeader(String header, String value) {
                con.addRequestProperty(header, value);
            }

            @Override
            public void onReady() {
                StringBuilder total = new StringBuilder();
                String line = "";
                Log.d(MyFitbitConnectionTest.TAG, "onReady() back from call");
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    while ((line = r.readLine()) != null) {
                        total.append(line);
                    }
                    Log.d(MyFitbitConnectionTest.TAG, "onReady() total: " + total);
                } catch (Exception e) { e.printStackTrace(); }

                try {
                    JSONObject result = new JSONObject(total.toString());
                    JSONObject user = result.getJSONObject("user");
                    nameTextView.setText(user.getString("displayName"));

                } catch (JSONException e){
                    Log.e(MyFitbitConnectionTest.TAG, "json error: " + e.getLocalizedMessage());
                }
            }

            @Override
            public void onError(String message) {
                nameTextView.setText("error: " + message);
            }
        });
    }

    private void getGoalData(OAuthData data){
        data.http(data.provider.equals("fitbit") ? "/1/user/-/activities/goals/daily.json" : "/1.1/account/verify_credentials.json", new OAuthRequest() {
            private URL url;
            private URLConnection con;

            @Override
            public void onSetURL(String _url) {
                Log.d(MyFitbitConnectionTest.TAG, "onsetURL url:" + _url);

                try {
                    url = new URL(_url);
                    con = url.openConnection();
                } catch (Exception e) { e.printStackTrace(); }
            }

            @Override
            public void onSetHeader(String header, String value) {
                con.addRequestProperty(header, value);
            }

            @Override
            public void onReady() {
                StringBuilder total = new StringBuilder();
                String line = "";
                Log.d(MyFitbitConnectionTest.TAG, "onReady() back from call");
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    while ((line = r.readLine()) != null) {
                        total.append(line);
                    }
                    Log.d(MyFitbitConnectionTest.TAG, "onReady() total: " + total);
                } catch (Exception e) { e.printStackTrace(); }

                try {
                    JSONObject result = new JSONObject(total.toString());
                    JSONObject goals = result.getJSONObject("goals");
                    stepGoalTextView.setText(goals.getString("steps"));

                } catch (JSONException e){
                    Log.e(MyFitbitConnectionTest.TAG, "json error: " + e.getLocalizedMessage());
                }
            }

            @Override
            public void onError(String message) {
                nameTextView.setText("error: " + message);
            }
        });
    }



    }
