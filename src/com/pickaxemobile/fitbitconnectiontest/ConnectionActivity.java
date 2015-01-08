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
/*
 * some api links, currently Android version is very little info
 * https://wiki.fitbit.com/display/API/API+Java+Client
 * http://docs.oauth.io/#download-the-sdk
 * https://github.com/oauth-io/oauth-android/tree/6c7ad5719a0a6b67ef679ebea4354482795def27
 * https://oauth.io/docs/api-reference/client/android
 * 
 * api to request for specific JSON info
 * https://wiki.fitbit.com/display/API/V1
 * some useful info:
 * https://wiki.fitbit.com/display/API/API-Get-User-Info
 * https://wiki.fitbit.com/display/API/API-Get-Activity-Daily-Goals
 * 
 * 1).
 * create OAuth object.
 * initialize the SDK by oauth public key.
 * connect user to provider "fitbit" to log in popup, to launch activity after also need a OAuthCallback,
 * OAuthCallback is a class implements OAuthCallback Override onFinished(OAuthData) method.
 * 
 * 2).
 * in onFinished(OAuthData), OAuthData contain the following:
 * String provider;			// name of the provider
 * String state;			// state send
 * String token;			// token received
 * String secret;			// secret received (only in oauth1)
 * String status;			// status of the request (succes, error, ....)
 * String expires_in;		// if the token expires
 * String error;			// error encountered
 * JSONObject request;		// API request description ("url":"https:\/\/api.fitbit.com"})
 * 
 * 3).
 * get JSON data
 * 
 */
public class ConnectionActivity extends Activity implements OAuthCallback
{
	private TextView nameTextView;
	private TextView stepGoalTextView;
	private static String TAG = "FITBIT";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		nameTextView = (TextView)findViewById(R.id.name);
		stepGoalTextView = (TextView)findViewById(R.id.stepGoal);
	}

	//when click connect button in the layout, it will launch fitbit login page "NOT A LAYOUT, BELONG TO fitbit"
	public void connect(View v)
	{
		//create OAuth object.
		//initialize the SDK by oauth public key.
		//connect user to provider "fitbit" to log in popup, to launch activity after also need a OAuthCallback,
		//OAuthCallback is a class implements OAuthCallback Override onFinished(OAuthData) method
		final OAuth oauth = new OAuth(this);
		// Log.d(TAG, "pub key: " + MyFitbitConnectionTest.OAUTH_IO_PUBLIC_KEY);
		oauth.initialize(MyFitbitConnectionTest.OAUTH_IO_PUBLIC_KEY); //OAuth.io public key, not Fitbit's
		oauth.popup("fitbit", ConnectionActivity.this);
		//TODO seems bad to return to main activity with callback
	}

	//Called when the authorize dialog closes.
	public void onFinished(OAuthData data)
	{
		Log.d(MyFitbitConnectionTest.TAG, "in on finished, data: "+ data.toString());
		//if log in fail, display error message
		if ( ! data.status.equals("success"))
		{
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

	//get user data from JSON respond,
	//https://wiki.fitbit.com/display/API/API-Get-User-Info
	/*
	 * callback methods for OAuthRequest()
	 * 2 require methods
	 * 	void onSetHeader(String arg0, String arg1)
	 * 	void onSetURL(String arg0)
	 * 4 optional methods
	 * 	OAuthData getOAuthData()
	 * 	void onError(String message)
	 * 	void onReady()
	 * 	void setOAuthData(OAuthData oauth_data)
	 */
	private void getProfileData(OAuthData data)
	{
		data.http(data.provider.equals("fitbit") ? "/1/user/-/profile.json" : "/1.1/account/verify_credentials.json", new OAuthRequest() {
			private URL url;
			private URLConnection con;

			//This method is called once the final url is returned
			@Override
			public void onSetURL(String _url)
			{
				Log.d(MyFitbitConnectionTest.TAG, "onsetURL url:" + _url);

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

			//This method is called for each header to add to the request
			@Override
			public void onSetHeader(String header, String value)
			{
				con.addRequestProperty(header, value);
				//I try to find the full url to access json data, I might want to ask Anna for this
				//header and value are kind like api key, oauth version, and other stuff
				Log.d(MyFitbitConnectionTest.TAG, "header:" + header + ", value:" + value);
				//con.getURL().toString() is same as _url, no change
				Log.d(MyFitbitConnectionTest.TAG, "con:" + con.getURL().toString());
				//something I don't understand
				Log.d(MyFitbitConnectionTest.TAG, "con:" + con.toString());
			}

			//This method is called once url and headers are set
			//use URLConnection object to get the JSONObject
			@Override
			public void onReady()
			{
				StringBuilder total = new StringBuilder();
				String line = "";
				Log.d(MyFitbitConnectionTest.TAG, "onReady() 1 back from call");
				try
				{
					BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
					while ((line = r.readLine()) != null)
					{
						total.append(line);
					}
					//all user data
					Log.d(MyFitbitConnectionTest.TAG, "onReady() 1 total: " + total);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

				//see this page
				//https://wiki.fitbit.com/display/API/API-Get-User-Info
				//create JSONObject result with String Variable total
				//create JSONObject user by search String "user" from the JSONObject result
				//get JSONObject value by search String "displayName"
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

			//This method is called if an error occurred
			@Override
			public void onError(String message)
			{
				nameTextView.setText("error: " + message);
			}
		});
	}

	//similar to above method, for exercise data
	//https://wiki.fitbit.com/display/API/API-Get-Activity-Daily-Goals
	private void getGoalData(OAuthData data)
	{
		data.http(data.provider.equals("fitbit") ? "/1/user/-/activities/goals/daily.json" : "/1.1/account/verify_credentials.json", new OAuthRequest() {
			private URL url;
			private URLConnection con;

			@Override
			public void onSetURL(String _url)
			{
				Log.d(MyFitbitConnectionTest.TAG, "onsetURL url:" + _url);

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
				Log.d(MyFitbitConnectionTest.TAG, "onReady() 2 back from call");
				try
				{
					BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
					while ((line = r.readLine()) != null)
					{
						total.append(line);
					}
					Log.d(MyFitbitConnectionTest.TAG, "onReady() 2 total: " + total);
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
}
