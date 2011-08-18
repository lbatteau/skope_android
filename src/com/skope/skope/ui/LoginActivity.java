package com.skope.skope.ui;

import org.apache.http.HttpStatus;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.skope.skope.R;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.http.CustomHttpClient;
import com.skope.skope.http.CustomHttpClient.RequestMethod;

public class LoginActivity extends BaseActivity {
	String mUsername;
	String mPassword;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// load up the layout
		setContentView(R.layout.login);
		
		// Read username and password from shared preferences
		String username = getCache().getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
		String password = getCache().getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
		
		// Check if username and password already present
		if (!username.equals("") && !password.equals("")) {
			// Present, fill edit fields
			EditText usernameEditText = (EditText) findViewById(R.id.txt_username);
			EditText passwordEditText = (EditText) findViewById(R.id.txt_password);
			usernameEditText.setText(username);
			passwordEditText.setText(password);
		}		

		// get the button resource in the xml file and assign it to a local
		// variable of type Button
		Button launch = (Button) findViewById(R.id.login_button);

		// this is the action listener
		launch.setOnClickListener(new OnClickListener() {
			String username, password;

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

				String loginUrl = getCache().getProperty("skope_login_url");
				
				new LoginTask().execute(loginUrl, mUsername, mPassword);
			}
		}

		); // end of launch.setOnclickListener
	}
	
	private class LoginTask extends AsyncTask<String, Void, Integer> {
		private ProgressDialog dialog = new ProgressDialog(LoginActivity.this);
		
		// can use UI thread here
		protected void onPreExecute() {
			this.dialog.setMessage("Contacting server...");
			this.dialog.show();
		}
		
		protected Integer doInBackground(String... args) {
	    	// Set up HTTP client
	        CustomHttpClient client = new CustomHttpClient(LoginActivity.this, args[0]);
	        client.setUseBasicAuthentication(true);
	        client.setUsernamePassword(args[1], args[2]);
	         
	        // Send HTTP request to web service
	        try {
	            client.execute(RequestMethod.GET);
	        } catch (Exception e) {
	        	// Most exceptions already handled by client
	            e.printStackTrace();
	        }
	        
	        // Return server response
	        return client.getResponseCode();
	    }

	    protected void onPostExecute(Integer result) {
	    	this.dialog.dismiss();
	    	
	    	// Check response code
	        switch(result) {
	        case HttpStatus.SC_UNAUTHORIZED:
	        	// Login not successful, authorization required 
	        	Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
	        	return;
	        case HttpStatus.SC_REQUEST_TIMEOUT:
	        	Toast.makeText(LoginActivity.this, "Connection failed. Please make sure you are connected to the internet.", Toast.LENGTH_SHORT).show();
	        	return;
	        }
	        
	        // Login successful, store credentials
	        SharedPreferences.Editor prefsEditor = getCache().getPreferences().edit();
	        prefsEditor.putString(SkopeApplication.PREFS_USERNAME, mUsername);
	        prefsEditor.putString(SkopeApplication.PREFS_PASSWORD, mPassword);
	        prefsEditor.commit();
	        
	        // Redirect to list activity
	        Intent i = new Intent();
        	i.setClassName("com.skope.skope",
        				   "com.skope.skope.ui.SkopeListActivity");
        	startActivity(i);
        	finish();	
	    }
	}

	
}
