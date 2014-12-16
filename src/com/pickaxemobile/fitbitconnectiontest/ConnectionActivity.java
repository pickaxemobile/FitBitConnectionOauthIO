package com.pickaxemobile.fitbitconnectiontest;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
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
        oauth.initialize(MyFitbitConnectionTest.OAUTH_IO_PUBLIC_KEY); //OAuth.io public key, not Fitbit's
        oauth.popup("fitbit", ConnectionActivity.this); //TODO seems bad to return to main activity with callback
    }

    public void onFinished(OAuthData data) {
        Log.d(MyFitbitConnectionTest.TAG, "in on finished, data: "+ data.toString());
        if ( ! data.status.equals("success")) {
            nameTextView.setTextColor(Color.parseColor("#FF0000"));
            nameTextView.setText("error, " + data.error);
        }
        Log.d(MyFitbitConnectionTest.TAG, "data provider: " + data.provider + " data http" + data.request.toString());
        new GetProfileServerTask().execute(data);


    }

    private class GetProfileServerTask extends AsyncTask<OAuthData, Void, String> {

        protected String doInBackground(OAuthData... data) {
            String resultString = profileHTTPGET(data.toString());
            return resultString;
        }

        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(String result) {
            //TODO parse out result
            /*try{
                JSONObject petObj = new JSONObject(result);
                //	public Pet(String name, String story, int type, int id){

                // JSONObject petObj = jObject.getJSONObject("id");
                int id = petObj.getInt("id");
                String name = petObj.getString("name");



            } catch (JSONException e){
                Log.e(TAG, "exception: " + e.getMessage());
            }*/
            String resultString = "";

        }
    }

    private void doProfileQuery(OAuthData data){
        // this is from example, don't want to use this http library
        data.http(data.provider.equals("fitbit") ? "/1/user/-/profile.json" : "/1.1/account/verify_credentials.json", new OAuthRequest() {
            private URL url;
            private URLConnection con;

            @Override
            public void onSetURL(String _url) {
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
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line);
                    }
                    Log.d("FITBIT", "query result: " + total);
                    JSONObject result = new JSONObject(total.toString());
                    JSONObject user = result.getJSONObject("user");
                    nameTextView.setText(user.getString("displayName"));
                } catch (Exception e) { e.printStackTrace(); }
            }

            @Override
            public void onError(String message) {
                nameTextView.setText("error: " + message);
            }
        });
    }

    public String profileHTTPGET(String URL) {
        // this is the http library (apache) we want to use
        Log.d(TAG,"get profile data,  url: " + URL);
        HttpEntity resEntity;
        String responseString = "";

        try {

            HttpUriRequest request = new HttpGet(URL);

            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(request);

            resEntity = response.getEntity();
            if (resEntity != null) {
                responseString = EntityUtils.toString(resEntity);
                //	Log.i(TAG,"get response: " + responseString);
            } else {
                Log.d(MyFitbitConnectionTest.TAG, "resEntity is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        return responseString;
    }
}
