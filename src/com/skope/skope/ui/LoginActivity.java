package com.skope.skope.ui;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.skope.skope.R;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.User;
import com.skope.skope.http.BMPFromURL;
import com.skope.skope.http.CustomHttpClient;
import com.skope.skope.http.CustomHttpClient.RequestMethod;

public class LoginActivity extends BaseActivity {
	String mUsername;
	String mPassword;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		final String loginUrl = getCache().getProperty("skope_service_url") + "/login/";
		
		// load up the layout
		setContentView(R.layout.login);
		
		// Set the closed beta text
		TextView text = (TextView) findViewById(R.id.text_closedbeta);
		text.setText(Html.fromHtml("This is a beta release.<br> Please visit <a href=\"http://www.skope.net\">www.skope.net</a> for more information!"));
		// Otherwise link has no response
		// http://stackoverflow.com/questions/2734270/how-do-i-make-links-in-a-textview-clickable
		text.setMovementMethod(LinkMovementMethod.getInstance());

		// Read username and password from shared preferences
		String username = getCache().getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
		String password = getCache().getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
		
		// Check if user already present
		if (getCache().getUser() != null) {
			// Already present, login automatically
			new LoginTask().execute(loginUrl, username, password);
		}
		
		// Check if username and password already present
		if (!username.equals("") && !password.equals("")) {
			// Present, fill edit fields
			EditText usernameEditText = (EditText) findViewById(R.id.txt_username);
			EditText passwordEditText = (EditText) findViewById(R.id.txt_password);
			usernameEditText.setText(username);
			passwordEditText.setText(password);
		}		

		// Login button action
		Button login = (Button) findViewById(R.id.login_button);
		login.setOnClickListener(new OnClickListener() {
			public void onClick(View viewParam) {
				// this gets the resources in the xml file and assigns it to a
				// local variable of type EditText
				EditText usernameEditText = (EditText) findViewById(R.id.txt_username);
				EditText passwordEditText = (EditText) findViewById(R.id.txt_password);

				// the getText() gets the current value of the text box
				// the toString() converts the value to String data type
				// then assigns it to a variable of type String
				mUsername = usernameEditText.getText().toString();
				mPassword = passwordEditText.getText().toString();

				new LoginTask().execute(loginUrl, mUsername, mPassword);
			}
		});

		// Signup button action
		Button signup = (Button) findViewById(R.id.signup_button);
		signup.setOnClickListener(new OnClickListener() {
			public void onClick(View viewParam) {
				// Redirect to list activity
		        Intent i = new Intent();
	        	i.setClassName("com.skope.skope",
	        				   "com.skope.skope.ui.UserSignupActivity");
	        	startActivity(i);
			}
		});
	}
	
	private class LoginTask extends AsyncTask<String, Void, CustomHttpClient> {
		private ProgressDialog dialog = new ProgressDialog(LoginActivity.this);
		
		// can use UI thread here
		protected void onPreExecute() {
			this.dialog.setMessage("Contacting server...");
			this.dialog.show();
		}
		
		protected CustomHttpClient doInBackground(String... args) {
	    	// Set up HTTP client with url as argument
	        CustomHttpClient client = new CustomHttpClient(args[0]);
	        client.setUseBasicAuthentication(true);
	        client.setUsernamePassword(args[1], args[2]);
	        try {
	        	int versionCode = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionCode;
				client.addParam("version_code", String.valueOf(versionCode));
			} catch (NameNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	         
	        // Send HTTP request to web service
	        try {
	            client.execute(RequestMethod.GET);
	        } catch (Exception e) {
	        	// Most exceptions already handled by client
	            e.printStackTrace();
	        }
	        
	    	// Check HTTP response code
	    	int httpResponseCode = client.getResponseCode();
	    	// Check for server response
	    	if (httpResponseCode == 0) {
	    		return client;
	    	} else {
	    		// Check for error
	    		if (httpResponseCode != HttpStatus.SC_OK) {
			        return client;
	    		}
	    	}

	    	// Check Skope service response code
	        JSONObject jsonResponse = null;
	        try {
	        	jsonResponse = new JSONObject(client.getResponse());
			} catch (JSONException e) {
				// Log exception
				Log.e(SkopeApplication.LOG_TAG, e.toString());
				return client;
			}
			
			// The user object is returned in the response
	        User user;
	        try {
	        	user = new User(jsonResponse);
	        	
	        	// Retrieve thumbnail
	        	BMPFromURL bmpFromURL = new BMPFromURL(user.getThumbnailURL());
				if (bmpFromURL != null) {
					Bitmap thumbnail = bmpFromURL.getBitmap();
					user.setThumbnail(thumbnail);
				}
	        } catch (JSONException e) {
				// Log exception
				Log.e(SkopeApplication.LOG_TAG, e.toString());
				return client;
	        }
	        
	        // Store the user in the cache
	        getCache().setUser(user);
	        
	        // Return server response
	        return client;
	    }

	    protected void onPostExecute(CustomHttpClient client) {
	    	this.dialog.dismiss();
	    	
	    	// Check HTTP response code
	    	int httpResponseCode = client.getResponseCode();
	    	// Check for server response
	    	if (httpResponseCode == 0) {
	    		// No server response
	    		Toast.makeText(LoginActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
	    		return;
	    	} else {
	    		// Check for error
	    		if (httpResponseCode != HttpStatus.SC_OK) {
	    			// Server returned error code
			        switch(client.getResponseCode()) {
			        case HttpStatus.SC_UNAUTHORIZED:
			        	// Login not successful, authorization required 
			        	Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
			        	break;
			        case HttpStatus.SC_REQUEST_TIMEOUT:
			        case HttpStatus.SC_BAD_GATEWAY:
			        case HttpStatus.SC_GATEWAY_TIMEOUT:
			        	// Connection timeout
			        	Toast.makeText(LoginActivity.this, "Connection failed. Please make sure you are connected to the internet.", Toast.LENGTH_SHORT).show();
			        	break;
			        case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			        	Toast.makeText(LoginActivity.this, "Sorry, the server just crashed. We're working on it.", Toast.LENGTH_SHORT).show();
			        	break;
			        case 430: // UPGRADE_REQUIRED
			        	Toast.makeText(LoginActivity.this, "Please update Skope to the latest version", Toast.LENGTH_SHORT).show();
			        	break;
			        case 431: // EMAIL NOT VERIFIED
			        	Toast.makeText(LoginActivity.this, "Please verify you email address", Toast.LENGTH_SHORT).show();
			        	break;
			        case HttpStatus.SC_PAYMENT_REQUIRED:
			        	Toast.makeText(LoginActivity.this, "You have a payment due", Toast.LENGTH_SHORT).show();
			        	break;
			        }
			        return;
	    			
	    		}
	    	}

	    	// Store credentials
	        SharedPreferences.Editor prefsEditor = getCache().getPreferences().edit();
	        prefsEditor.putString(SkopeApplication.PREFS_USERNAME, mUsername);
	        prefsEditor.putString(SkopeApplication.PREFS_PASSWORD, mPassword);
	        prefsEditor.commit();
	        
	        // Redirect to list activity
	        Intent i = new Intent();
        	i.setClassName("com.skope.skope",
        				   "com.skope.skope.ui.MainTabActivity");
        	startActivity(i);
        	finish();	
	    }
	}

	
}
