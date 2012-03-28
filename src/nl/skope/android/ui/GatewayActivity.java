package nl.skope.android.ui;

import java.util.Date;

import nl.skope.android.application.Cache;
import nl.skope.android.application.SkopeApplication;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

public class GatewayActivity extends BaseActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Cache cache = getCache();
		
		/**
		 * Check user status
		 */
		
		// Check user signed out
		if (cache.isUserSignedOut()) {
			// User signed out, always go to login screen
			Intent i = new Intent();
			i.setClassName("nl.skope.android",
					"nl.skope.android.ui.LoginActivity");
			startActivity(i);
			finish();
			return;
		} else {
			// Not signed out. Check if user present
			if (cache.getUser() == null) {
				// Not present, could have been garbage collected.
				// Go back to login screen and set the auto login flag.
				Intent i = new Intent();
				i.setClassName("nl.skope.android",
						"nl.skope.android.ui.LoginActivity");
				// Add auto login flag
				Bundle bundle = new Bundle();
				bundle.putBoolean(LoginActivity.INTENT_AUTOLOGIN, true);
				startActivity(i);
				finish();
				return;
			}
		}
		
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
		
		/**
		 * Everything seems OK, proceed
		 */
		
		Intent i = new Intent();
		// Check if redirect present in intent
		if (getIntent() != null 
			&& getIntent().hasExtra(SkopeApplication.BUNDLEKEY_REDIRECTACTIVITY)) {
			// Redirect present, set target activity
			String redirectActivity = getIntent().getExtras().getString(SkopeApplication.BUNDLEKEY_REDIRECTACTIVITY);
			i.setClassName("nl.skope.android", redirectActivity);
			i.putExtras(getIntent().getExtras());
		} else {
			// No redirect, target activity is home
			i.setClassName("nl.skope.android",
			   "nl.skope.android.ui.MainTabActivity");
			if (getIntent() != null && getIntent().getExtras() != null) {
				i.putExtras(getIntent().getExtras());
			}
		}
    	startActivity(i);
    	finish();	

	}
}
