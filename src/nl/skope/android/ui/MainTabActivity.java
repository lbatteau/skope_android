package nl.skope.android.ui;

import nl.skope.android.application.Cache;
import nl.skope.android.application.ServiceQueue;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.User;
import nl.skope.android.util.Type;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;

import nl.skope.android.R;

public class MainTabActivity extends TabActivity {
	public final static int TAB_LIST = 0;
	public final static int TAB_MAP = 1;
	public final static int TAB_PROFILE = 2;
	
	private SkopeApplication mApplication;
	private Cache mCache;
	private ServiceQueue mServiceQueue;    
	private TabHost mTabHost;

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
        	i.setClassName("nl.skope.android",
        				   "nl.skope.android.ui.LoginActivity");
        	startActivity(i);
        	finish();
        	return;
    	}
    	
	    mTabHost = getTabHost();  // The activity TabHost
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
	    spec = mTabHost.newTabSpec("list").setIndicator(tabList)
	                  .setContent(intent);
	    mTabHost.addTab(spec);

	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, OOIListMapActivity.class);
	    spec = mTabHost.newTabSpec("map").setIndicator(tabMap)
	                  .setContent(intent);
	    mTabHost.addTab(spec);

	    intent = new Intent().setClass(this, UserProfileActivity.class);
	    spec = mTabHost.newTabSpec("profile").setIndicator(tabProfile).setContent(intent);
        mTabHost.addTab(spec);
	    
	    intent = new Intent().setClass(this, UserProfileActivity.class);
	    spec = mTabHost.newTabSpec("chat").setIndicator(chatProfile).setContent(intent);
        mTabHost.addTab(spec);
	    
        intent = new Intent().setClass(this, UserFavoritesActivity.class);
	    spec = mTabHost.newTabSpec("favorites").setIndicator(favoritesProfile).setContent(intent);
        mTabHost.addTab(spec);

		// Check if tab is specified in Intent
        int tabSpecified = 0;
        if (getIntent() != null && getIntent().getExtras() != null) {
        	tabSpecified = getIntent().getExtras().getInt("TAB");
        }
        
	    if (tabSpecified != 0) {
	    	mTabHost.setCurrentTab(tabSpecified);
	    }/* else {
	        // If user logging in for the first time
	        if (mCache.getUser().isFirstTime()) {
	        	// Direct to profile
	        	mTabHost.setCurrentTab(2);
	        } else {
	        	// Otherwise to list view
	        	mTabHost.setCurrentTab(0);
	        }
	    }*/
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
	    	mCache.setUserSignedOut(true);
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
	
	public void switchTab(int tab) {
		getTabHost().setCurrentTab(tab);
	}

}
