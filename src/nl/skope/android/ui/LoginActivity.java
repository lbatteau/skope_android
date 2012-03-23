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

import android.app.PendingIntent;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends BaseActivity {
	public final static String INTENT_AUTOLOGIN = "AUTOLOGIN";
	
	String mUsername;
	String mPassword;
	
	private SharedPreferences mPreferences;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		mPreferences = getCache().getPreferences();

		// Read username and password from shared preferences
		String username = mPreferences.getString(SkopeApplication.PREFS_USERNAME, "");
		String password = mPreferences.getString(SkopeApplication.PREFS_PASSWORD, "");
		
		// Check for intent parameters
		if (getIntent() != null && getIntent().getExtras() != null) {
			// Check for auto login
        	boolean isAutoLogin = getIntent().getExtras().getBoolean(INTENT_AUTOLOGIN);
        	if (isAutoLogin) {
        		// Auto login: Check for username and password
        		if (!username.equals("") && !password.equals("")) {
        			// Username and password present
        			new LoginTask().execute(username, password);
        		}
        	}
        }		
	    
		// load up the layout
		setContentView(R.layout.login);
		
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

				new LoginTask().execute(mUsername, mPassword);
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
	
	private int login(String username, String password) {
		final String loginUrl = getCache().getProperty("skope_service_url") + "/login/";
		
		// Set up HTTP client with url as argument
        CustomHttpClient client = new CustomHttpClient(loginUrl);
        client.setUseBasicAuthentication(true);
        client.setUsernamePassword(username, password);
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
    		return httpResponseCode;
    	} else {
    		// Check for error
    		if (httpResponseCode != HttpStatus.SC_OK) {
		        return httpResponseCode;
    		}
    	}

    	// Check Skope service response code
        JSONObject jsonResponse = null;
        try {
        	jsonResponse = new JSONObject(client.getResponse());
		} catch (JSONException e) {
			// Log exception
			Log.e(SkopeApplication.LOG_TAG, e.toString());
			return 0;
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
			return 0;
        }
        
        // Store the user in the cache
        getCache().setUser(user);
        
    	// Store credentials
    	SharedPreferences.Editor editor = mPreferences.edit();
    	editor.putString(SkopeApplication.PREFS_USERNAME, mUsername);
    	editor.putString(SkopeApplication.PREFS_PASSWORD, mPassword);
    	editor.putInt(SkopeApplication.PREFS_USERID, user.getId());
    	editor.commit();
        
        // Retrieve favorites
        Bundle favoritesBundle = new Bundle();
        favoritesBundle.putInt("USER_ID", user.getId());
		getServiceQueue().postToService(Type.READ_USER_FAVORITES, favoritesBundle);
		
		// Set flag
		getCache().setUserSignedOut(false);

		// Return server response
        return client.getResponseCode();
	}
	
	private boolean handleLoginResultCode(int httpResponseCode) {
		// Check for server response
    	if (httpResponseCode == 0) {
    		// No server response
    		Toast.makeText(LoginActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
    		return false;
    	} else {
    		// Check for error
    		if (httpResponseCode != HttpStatus.SC_OK) {
    			// Server returned error code
		        switch(httpResponseCode) {
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
		        return false;
    		}
    	}
    	
    	return true;
	}
	
	private class LoginTask extends AsyncTask<String, Void, Integer> {
		private ProgressDialog dialog = new ProgressDialog(LoginActivity.this);
		
		// can use UI thread here
		protected void onPreExecute() {
			this.dialog.setMessage("Contacting server...");
			this.dialog.show();
		}
		
		protected Integer doInBackground(String... args) {
			return login(args[0], args[1]);
	    }

	    protected void onPostExecute(Integer httpResponseCode) {
	    	this.dialog.dismiss();
	    	
	    	if (handleLoginResultCode(httpResponseCode)) {
		        // Redirect to list activity
	        	Bundle bundle = new Bundle();
		        bundle.putInt("TAB", MainTabActivity.TAB_PROFILE);
		        Intent i = new Intent();
	        	i.setClassName("nl.skope.android",
	        				   "nl.skope.android.ui.GatewayActivity");
		        i.putExtras(bundle);
	        	startActivity(i);
	        	finish();
	    	}

	    }
	}
	
}
