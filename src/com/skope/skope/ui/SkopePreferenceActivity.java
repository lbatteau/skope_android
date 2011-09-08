package com.skope.skope.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.skope.skope.R;

/**
 * This class represents the Skope 'options' menu.
 * 
 * It contains the following items:<br>
 *  <ul><li>GPS enabled - A check box indicating whether GPS positioning is 
 *  enabled. When the user clicks on this preference the system location 
 *  settings menu ({@code ACTION_LOCATION_SOURCE_SETTINGS}) is opened.</li></ul> 
 */
public class SkopePreferenceActivity extends PreferenceActivity {
	
	SharedPreferences.OnSharedPreferenceChangeListener mListener;
	CheckBoxPreference mGPSPreference;
	
	/** Used to determine whether GPS positioning is enabled */
	LocationManager mLocationManager;
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.menu.preferences);

            // Setup the GPS preference
            mGPSPreference = (CheckBoxPreference) findPreference("prefs_gps");
            
            // Check if GPS is enabled
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            
            // Setup the GPS preference click listener
            mGPSPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	public boolean onPreferenceClick(Preference preference) {
            		// Redirect the user to the system location settings menu
            		Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            		startActivity(intent);
            		return true;
            	}
            });
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	
    	// Have the preference correspond with the system settings
        mGPSPreference.setChecked(mLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER));
	}
    
}
