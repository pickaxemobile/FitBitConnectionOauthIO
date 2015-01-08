package com.pickaxemobile.fitbitconnectiontest;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import io.oauth.OAuth;
import io.oauth.OAuthCallback;
import io.oauth.OAuthData;
import io.oauth.OAuthRequest;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class ConnectionActivity extends Activity implements OAuthCallback
{
    private TextView nameTextView;
    private TextView stepGoalTextView;
    private static String TAG = "FITBIT";

    OAuthData data1 = null;
    private SharedPreferences pref;
    String myProvider, myState, myToken, mySecret, myStatus, myExpires_in, myError, myRequest;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        nameTextView = (TextView)findViewById(R.id.name);
        stepGoalTextView = (TextView) findViewById(R.id.stepGoal);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        myProvider = "";
        myState = "";
        myToken = "";
        mySecret = "";
        myStatus = "";
        myExpires_in = "";
        myError = "";
        myRequest = "";

        loadOAuthData();
    }

    public void connect(View v)
    {
        if(myProvider.equalsIgnoreCase(""))
        {
            final OAuth oauth = new OAuth(this);
            oauth.initialize(MyFitbitConnectionTest.OAUTH_IO_PUBLIC_KEY);
            oauth.popup("fitbit", ConnectionActivity.this);
        }
        else
        {
            data1.provider = myProvider;
            data1.state = myState;
            data1.token = myToken;
            data1.secret = mySecret;
            data1.status = myStatus;
            data1.expires_in = myExpires_in;
            data1.error = myError;
            try
            {
                data1.request = new JSONObject(myRequest);
            }
            catch (JSONException e)
            {
                Log.e(MyFitbitConnectionTest.TAG, "json error: " + e.getLocalizedMessage());
            }
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
            getProfileData(data1);
            getGoalData(data1);
        }
    }

    public void onFinished(OAuthData data)
    {
        if ( ! data.status.equals("success"))
        {
            nameTextView.setTextColor(Color.parseColor("#FF0000"));
            nameTextView.setText("error, " + data.error);
        }

        myProvider = data.provider;
        myState = data.state;
        myToken = data.token;
        mySecret = data.secret;
        myStatus = data.status;
        myExpires_in = data.expires_in;
        myError = data.error;
        myRequest = data.request.toString();

        saveOAuthData();

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        getProfileData(data);
        getGoalData(data);
    }

    private void getProfileData(OAuthData data)
    {
        data.http(data.provider.equals("fitbit") ? "/1/user/-/profile.json" : "/1.1/account/verify_credentials.json", new OAuthRequest()
        {
            private URL url;
            private URLConnection con;

            @Override
            public void onSetURL(String _url)
            {
                try
                {
                    url = new URL(_url);
                    con = url.openConnection();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSetHeader(String header, String value)
            {
                con.addRequestProperty(header, value);
            }

            @Override
            public void onReady()
            {
                StringBuilder total = new StringBuilder();
                String line = "";
                try
                {
                    BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    while ((line = r.readLine()) != null)
                    {
                        total.append(line);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                try
                {
                    JSONObject result = new JSONObject(total.toString());
                    JSONObject user = result.getJSONObject("user");
                    nameTextView.setText(user.getString("displayName"));
                }
                catch (JSONException e)
                {
                    Log.e(MyFitbitConnectionTest.TAG, "json error: " + e.getLocalizedMessage());
                }
            }

            @Override
            public void onError(String message)
            {
                nameTextView.setText("error: " + message);
            }
        });
    }

    private void getGoalData(OAuthData data)
    {
        data.http(data.provider.equals("fitbit") ? "/1/user/-/activities/goals/daily.json" : "/1.1/account/verify_credentials.json", new OAuthRequest()
        {
            private URL url;
            private URLConnection con;

            @Override
            public void onSetURL(String _url)
            {
                try
                {
                    url = new URL(_url);
                    con = url.openConnection();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSetHeader(String header, String value)
            {
                con.addRequestProperty(header, value);
            }

            @Override
            public void onReady()
            {
                StringBuilder total = new StringBuilder();
                String line = "";
                try
                {
                    BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    while ((line = r.readLine()) != null)
                    {
                        total.append(line);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                try
                {
                    JSONObject result = new JSONObject(total.toString());
                    JSONObject goals = result.getJSONObject("goals");
                    stepGoalTextView.setText(goals.getString("steps"));

                }
                catch (JSONException e)
                {
                    Log.e(MyFitbitConnectionTest.TAG, "json error: " + e.getLocalizedMessage());
                }
            }

            @Override
            public void onError(String message)
            {
                nameTextView.setText("error: " + message);
            }
        });
    }

    public void saveOAuthData()
    {
        SharedPreferences.Editor edit = pref.edit();
        edit.putString("myProvider", myProvider);
        edit.putString("myState", myState);
        edit.putString("myToken", myToken);
        edit.putString("mySecret", mySecret);
        edit.putString("myStatus", myStatus);
        edit.putString("myExpires_in", myExpires_in);
        edit.putString("myError", myError);
        edit.putString("myRequest", myRequest);
        edit.apply();
    }

    public void loadOAuthData()
    {
        myProvider = pref.getString("myProvider", "");
        myState = pref.getString("myState", "");
        myToken = pref.getString("myToken", "");
        mySecret = pref.getString("mySecret", "");
        myStatus = pref.getString("myStatus", "");
        myExpires_in = pref.getString("myExpires_in", null);
        myError = pref.getString("myError", null);
        myRequest = pref.getString("myRequest", "");
    }
}
