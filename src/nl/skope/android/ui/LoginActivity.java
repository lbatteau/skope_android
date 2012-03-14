package nl.skope.android.ui;

import nl.skope.android.R;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.User;
import nl.skope.android.http.CustomHttpClient;
import nl.skope.android.http.CustomHttpClient.RequestMethod;
import nl.skope.android.util.Type;
import nl.skope.android.util.Utility;

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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends BaseActivity {
	String mUsername;
	String mPassword;
	
	private SharedPreferences mPreferences;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		final String loginUrl = getCache().getProperty("skope_service_url") + "/login/";
		
		mPreferences = getCache().getPreferences();
	    
		// load up the layout
		setContentView(R.layout.login);
		
		// Read username and password from shared preferences
		String username = getCache().getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
		String password = getCache().getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
		
		// Check if user already present
		if (getCache().getUser() != null && !getCache().isUserSignedOut()) {
			// Present, unmark first time login
			getCache().getUser().setIsFirstTime(false);
			
			// Redirect to list activity
	        Intent i = new Intent();
        	i.setClassName("nl.skope.android",
        				   "nl.skope.android.ui.MainTabActivity");
        	startActivity(i);
        	finish();
        	return;
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
				mUsername = usernameEditText.getText().toString().toLowerCase();
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
	        	i.setClassName("nl.skope.android",
	        				   "nl.skope.android.ui.UserSignupActivity");
	        	startActivity(i);
			}
		});
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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
	        CustomHttpClient client = new CustomHttpClient(args[0], getApplicationContext());
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
	        	user.setCache(getCache());
	        	
	        	// Retrieve profile picture
	        	if (user.getProfilePictureURL() != null) {
					Bitmap profilePicture = Utility.getBitmapFromURL(user.getProfilePictureURL());
					if (profilePicture != null) {
						user.setProfilePicture(profilePicture);
					}
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
			        	Toast.makeText(LoginActivity.this, getResources().getText(R.string.error_connection_failed), Toast.LENGTH_SHORT).show();
			        	break;
			        case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			        	Toast.makeText(LoginActivity.this, getResources().getText(R.string.error_server_error), Toast.LENGTH_SHORT).show();
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
	    	SharedPreferences.Editor editor = mPreferences.edit();
	    	editor.putString(SkopeApplication.PREFS_USERNAME, mUsername);
	    	editor.putString(SkopeApplication.PREFS_PASSWORD, mPassword);
	    	editor.commit();
	        
	        // Retrieve favorites
	        Bundle favoritesBundle = new Bundle();
	        favoritesBundle.putInt("USER_ID", getCache().getUser().getId());
			getServiceQueue().postToService(Type.READ_USER_FAVORITES, favoritesBundle);
			
			// Set flag
			getCache().setUserSignedOut(false);
	        
	        // Redirect to list activity
        	Bundle bundle = new Bundle();
	        bundle.putInt("TAB", MainTabActivity.TAB_PROFILE);
	        Intent i = new Intent();
        	i.setClassName("nl.skope.android",
        				   "nl.skope.android.ui.MainTabActivity");
	        i.putExtras(bundle);
        	startActivity(i);
        	finish();
	    }
	}
	
}
