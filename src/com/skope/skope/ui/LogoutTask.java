package com.skope.skope.ui;

import org.apache.http.HttpStatus;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.skope.skope.application.SkopeApplication;
import com.skope.skope.http.CustomHttpClient;
import com.skope.skope.http.CustomHttpClient.RequestMethod;

public class LogoutTask extends AsyncTask<Object, Void, CustomHttpClient> {
	Activity mActivity;
	
	@Override
	protected CustomHttpClient doInBackground(Object... args) {
		mActivity = (Activity) args[0];
		String logoutUrl = (String) args[1];
		String username = (String) args[2];
		String password =(String) args[3];

		// Set up HTTP client with url as argument
        CustomHttpClient client = new CustomHttpClient(logoutUrl, mActivity.getApplicationContext());
        client.setUseBasicAuthentication(true);
        client.setUsernamePassword(username, password);

        // Send HTTP request to web service
        try {
            client.execute(RequestMethod.GET);
        } catch (Exception e) {
        	// Most exceptions already handled by client
            e.printStackTrace();
        }
        
        // Return server response
        return client;
	}
	
	@Override
	protected void onPostExecute(CustomHttpClient client) {
		// Check HTTP response code
    	int httpResponseCode = client.getResponseCode();
    	// Check for server response
    	if (httpResponseCode == 0) {
    		// No server response
    		Log.e(SkopeApplication.LOG_TAG, "Connection failed during signout");
    	} else {
    		// Check for error
    		if (httpResponseCode != HttpStatus.SC_OK) {
    			// Server returned error code
		        switch(client.getResponseCode()) {
		        case HttpStatus.SC_UNAUTHORIZED:
		        	// Login not successful, authorization required 
		        	Log.e(SkopeApplication.LOG_TAG, "Authorization failed during signout");
		        	break;
		        case HttpStatus.SC_REQUEST_TIMEOUT:
		        case HttpStatus.SC_BAD_GATEWAY:
		        case HttpStatus.SC_GATEWAY_TIMEOUT:
		        	// Connection timeout
		        	Log.e(SkopeApplication.LOG_TAG, "Connection failed during signout");
		        }
    		}
    	}
		
		// Redirect to login activity
        Intent i = new Intent();
    	i.setClassName("com.skope.skope",
    				   "com.skope.skope.ui.LoginActivity");
    	mActivity.startActivity(i);
    	mActivity.finish();	
	}

}
