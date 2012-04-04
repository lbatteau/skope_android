package nl.skope.android.ui;

import nl.skope.android.R;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.util.Type;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

/**
 * This class represents the Skope 'options' menu.
 * 
 * It contains the following items:<br>
 *  <ul><li>GPS enabled - A check box indicating whether GPS positioning is 
 *  enabled. When the user clicks on this preference the system mLocation 
 *  settings menu ({@code ACTION_LOCATION_SOURCE_SETTINGS}) is opened.</li></ul> 
 */
public class SkopePreferenceActivity extends PreferenceActivity {
	
	SharedPreferences.OnSharedPreferenceChangeListener mListener;
	CheckBoxPreference mGPSLocalPreference;
	Preference mGPSGlobalPreference;
	SharedPreferences mPreferences;
	
	/** Used to determine whether GPS positioning is enabled */
	LocationManager mLocationManager;
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.layout.preferences);
            
            mPreferences = getSharedPreferences("skopePreferences", Context.MODE_WORLD_READABLE);

            // Setup the GPS system wide preference
            mGPSGlobalPreference = (Preference) findPreference("prefs_gps_global");
            
            // Setup the GPS local preference
            mGPSLocalPreference = (CheckBoxPreference) findPreference("prefs_gps_local");
            
            // Check if GPS is enabled
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            
            // Setup the GPS preference click listener
            mGPSGlobalPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	public boolean onPreferenceClick(Preference preference) {
            		// Redirect the user to the system mLocation settings menu
            		Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            		startActivity(intent);
            		return true;
            	}
            });
            
            // Setup the GPS local preference
            mGPSLocalPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	public boolean onPreferenceClick(Preference preference) {
            		// Store credentials
                	SharedPreferences.Editor editor = mPreferences.edit();
                	editor.putBoolean(SkopeApplication.PREFS_GPSENABLED, 
                						((CheckBoxPreference) preference).isChecked());
                	editor.commit();
            		return true;
            	}
            });
            
            mGPSLocalPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					SkopeApplication application = (SkopeApplication) getApplication();
					if ((Boolean) newValue) {
						application.getServiceQueue().postToService(Type.ENABLE_GPS, null);					
					} else {
						application.getServiceQueue().postToService(Type.DISABLE_GPS, null);					
					}
					return true;
				}
			});
            
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	
        // Read username and password from shared preferences
		if (mLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
			mGPSGlobalPreference.setSummary(R.string.prefs_gps_global_on);
	    	// Have the preference correspond with the system settings
			mGPSLocalPreference.setEnabled(true);
	        mGPSLocalPreference.setChecked(mPreferences.getBoolean(SkopeApplication.PREFS_GPSENABLED, false));
		} else {
			mGPSLocalPreference.setEnabled(false);
			mGPSGlobalPreference.setSummary(R.string.prefs_gps_global_off);
		}
	}
    
}
