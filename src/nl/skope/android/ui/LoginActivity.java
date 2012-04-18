package nl.skope.android.ui;

import java.util.Date;

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

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends BaseActivity {
	
	private static final String TAG = LoginActivity.class.getSimpleName();

	String mUsername;
	String mPassword;
	
	private SharedPreferences mPreferences;
	private static ProgressDialog mDialog;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		mDialog = new ProgressDialog(this);
		
		mPreferences = getCache().getPreferences();

		// Read username and password from shared preferences
		mUsername = mPreferences.getString(SkopeApplication.PREFS_USERNAME, "");
		mPassword = mPreferences.getString(SkopeApplication.PREFS_PASSWORD, "");
		
		// load up the layout
		setContentView(R.layout.login);
		
		/**
		 * User
		 */
		// Check if user already present
		if (getCache().getUser() != null && !getCache().isUserSignedOut()) {
			// Present, unmark first time login
			getCache().getUser().setIsFirstTime(false);
			
			// Redirect to list activity
	        Intent i = new Intent();
        	i.setClassName("nl.skope.android",
        				   "nl.skope.android.ui.MainTabActivity");
        	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	startActivity(i);
        	finish();
        	return;
		}
		
        // Check if username and password already present
		if (!mUsername.equals("") && !mPassword.equals("")) {
			// Present, auto login if user not present
			if (getCache().getUser() == null) {
				// Auto login
				new LoginTask().execute(mUsername, mPassword);
			}
			
			// Fill in edit fields
			EditText usernameEditText = (EditText) findViewById(R.id.txt_username);
			EditText passwordEditText = (EditText) findViewById(R.id.txt_password);
			usernameEditText.setText(mUsername);
			passwordEditText.setText(mPassword);
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
			Log.e(TAG, e.toString());
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
			Log.e(TAG, e.toString());
			return 0;
        }
        
        // Store the user in the cache
        getCache().setUser(user);
        
    	// Store credentials
    	SharedPreferences.Editor editor = mPreferences.edit();
    	editor.putString(SkopeApplication.PREFS_USERNAME, mUsername);
    	editor.putString(SkopeApplication.PREFS_PASSWORD, mPassword);
    	editor.putInt(SkopeApplication.PREFS_USERID, user.getId());
    	
    	// Store preference defaults
        editor.putBoolean(SkopeApplication.PREFS_GPSENABLED, false);
    	editor.commit();

    	// Retrieve favorites
        Bundle favoritesBundle = new Bundle();
        favoritesBundle.putInt(SkopeApplication.BUNDLEKEY_USERID, user.getId());
		getServiceQueue().postToService(Type.READ_USER_FAVORITES, favoritesBundle);
		
		// Set flag
		getCache().setUserSignedOut(false);
		
		/**
		 * C2DM Registration
		 */
		String registrationId = getCache().getPreferences().getString(
				SkopeApplication.PREFS_C2DM_REGISTRATIONID, "");

		// Determine age of registration ID
		long registrationTimestamp = getCache().getPreferences().getLong(
				SkopeApplication.PREFS_C2DM_REGISTRATIONTIMESTAMP, new Date().getTime());
		long now = new Date().getTime();
		long daysDifference = (now - registrationTimestamp) / (24 * 60 * 60 * 1000);
		// TODO arbitrary expiration time
		boolean isRegistrationExpired = daysDifference > 7;
		boolean isRegistrationIdPresent = registrationId == null || !registrationId.equals("");

		if (!isRegistrationIdPresent || isRegistrationExpired) {
			// Register for C2DM
			Intent c2dmIntent = new Intent(
					"com.google.android.c2dm.intent.REGISTER");
			c2dmIntent.putExtra("app",
					PendingIntent.getBroadcast(this, 0, new Intent(), 0));
			c2dmIntent.putExtra("sender", "google@sko.pe");
			// Start registration
			startService(c2dmIntent);
		}
		
		

		// Return server response
        return client.getResponseCode();
	}
	
	private boolean handleLoginResultCode(int httpResponseCode) {
		// Check for server response
    	if (httpResponseCode == 0) {
    		// No server response
    		Toast.makeText(LoginActivity.this, getResources().getText(R.string.error_connection_failed), Toast.LENGTH_SHORT).show();
    		return false;
    	} else {
    		// Check for error
    		if (httpResponseCode != HttpStatus.SC_OK) {
    			// Server returned error code
		        switch(httpResponseCode) {
		        case HttpStatus.SC_UNAUTHORIZED:
		        	// Login not successful, authorization required 
		        	Toast.makeText(LoginActivity.this, getResources().getText(R.string.error_username_password), Toast.LENGTH_SHORT).show();
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
		        	//Toast.makeText(LoginActivity.this, "Please update Skope to the latest version", Toast.LENGTH_SHORT).show();
		        	new AlertDialog.Builder(LoginActivity.this)
			        .setTitle(getResources().getString(R.string.login_update_title))
			        .setMessage(getResources().getString(R.string.login_update_message))
			        .setPositiveButton(getResources().getString(R.string.login_update_ok), new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int whichButton) {
			            	Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://sko.pe/downloads/android-latest.apk"));
			            	startActivity(browserIntent);
			            }
			        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int whichButton) {
			                // Do nothing.
			            }
			        }).show();
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
		
		// can use UI thread here
		protected void onPreExecute() {
			mDialog.setMessage("Contacting server...");
			mDialog.show();
		}
		
		protected Integer doInBackground(String... args) {
			return login(args[0], args[1]);
	    }

	    protected void onPostExecute(Integer httpResponseCode) {
	    	mDialog.dismiss();
	    	
	    	if (handleLoginResultCode(httpResponseCode)) {
		        // Redirect to list activity
	        	Bundle bundle = new Bundle();
		        bundle.putInt(SkopeApplication.BUNDLEKEY_TAB, MainTabActivity.TAB_PROFILE);
		        Intent i = new Intent();
		        	i.setClassName("nl.skope.android",
				   "nl.skope.android.ui.MainTabActivity");
		        	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		        i.putExtras(bundle);
	        	startActivity(i);
	        	finish();
	    	}

	    }
	}
	
}
