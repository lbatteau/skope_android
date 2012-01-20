package com.skope.skope.ui;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;

import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.ServiceQueue;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.User;
import com.skope.skope.util.Type;

public class MainTabActivity extends TabActivity {
	private SkopeApplication mApplication;
	private Cache mCache;
	private ServiceQueue mServiceQueue;    

	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.tabs);
	    
	    mApplication = (SkopeApplication) getApplication();
	    mCache = mApplication.getCache();
	    mServiceQueue = mApplication.getServiceQueue();
	    
    	User user = mCache.getUser();
    	
    	// Cache empty?
    	if (user == null) {
    		// Redirect to login screen
    		Intent i = new Intent();
        	i.setClassName("com.skope.skope",
        				   "com.skope.skope.ui.LoginActivity");
        	startActivity(i);
        	finish();
        	return;
    	}
    	
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Reusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View tabList = inflater.inflate(R.layout.tab_list, null); 
	    View tabMap = inflater.inflate(R.layout.tab_map, null);
	    View tabProfile = inflater.inflate(R.layout.tab_profile, null);
	    View chatProfile = inflater.inflate(R.layout.tab_chat, null);
	    View favoritesProfile = inflater.inflate(R.layout.tab_favorites, null);
	    
	    // Initialize a TabSpec for each tab and add it to the TabHost

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, OOIListActivity.class);
	    spec = tabHost.newTabSpec("list").setIndicator(tabList)
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, OOIListMapActivity.class);
	    spec = tabHost.newTabSpec("map").setIndicator(tabMap)
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    intent = new Intent().setClass(this, UserProfileActivity.class);
	    spec = tabHost.newTabSpec("profile").setIndicator(tabProfile).setContent(intent);
        tabHost.addTab(spec);
	    
	    intent = new Intent().setClass(this, UserProfileActivity.class);
	    spec = tabHost.newTabSpec("chat").setIndicator(chatProfile).setContent(intent);
        tabHost.addTab(spec);
	    
	    spec = tabHost.newTabSpec("favorites").setIndicator(favoritesProfile).setContent(intent);
        tabHost.addTab(spec);
	    
        // If user logging in for the first time...
        if (user.isFirstTime()) {
        	// ...direct to profile
        	tabHost.setCurrentTab(2);
        } else {
        	// ...otherwise to list view
        	tabHost.setCurrentTab(0);
        }
	    
	}	
	
	/***
	 * Subscribe the Activity to the UiQueue.
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.skope_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.signout:
	    	mApplication.getServiceQueue().stopService();
	    	mCache.setUser(null);
	    	String logoutURL = mCache.getProperty("skope_service_url") + "/logout/";
	    	String username = mCache.getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
	    	String password = mCache.getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
	    	new LogoutTask().execute(this, logoutURL, username, password);
            return true;
	    case R.id.refresh:
	    	mApplication.getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
	    	return true;   	
	    case R.id.options:
	    	startActivity(new Intent(this, SkopePreferenceActivity.class));
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	

}
