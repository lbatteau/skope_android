package nl.skope.android.ui;

import nl.skope.android.R;
import nl.skope.android.application.Cache;
import nl.skope.android.application.ServiceQueue;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.UiQueue;
import nl.skope.android.http.CustomHttpClient;
import nl.skope.android.http.CustomHttpClient.RequestMethod;
import nl.skope.android.util.Type;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

public class MainTabActivity extends TabActivity {
	private static final String TAG = MainTabActivity.class.getSimpleName();
	public final static int TAB_LIST = 0;
	public final static int TAB_MAP = 1;
	public final static int TAB_PROFILE = 2;
	public final static int TAB_CHAT = 3;
	public final static int TAB_FAVORITES = 4;
	
	private SkopeApplication mApplication;
	private Cache mCache;
	private ServiceQueue mServiceQueue;
	private UiQueue mUiQueue;
	private TabHost mTabHost;
	
    // C2DM Intent Filter
	private IntentFilter mIntentFilter;
	
	private TextView mUnreadMessagesView;

	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.tabs);
	    
	    mApplication = (SkopeApplication) getApplication();
	    mCache = mApplication.getCache();
	    mServiceQueue = mApplication.getServiceQueue();
	    mUiQueue = mApplication.getUiQueue();
	    
		mTabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Reusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab
	    
	    LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View tabList = inflater.inflate(R.layout.tab_list, null); 
	    View tabMap = inflater.inflate(R.layout.tab_map, null);
	    View tabProfile = inflater.inflate(R.layout.tab_profile, null);
	    View chatProfile = inflater.inflate(R.layout.tab_chat, null);
	    View favoritesProfile = inflater.inflate(R.layout.tab_favorites, null);
	    
	    mUnreadMessagesView = (TextView) chatProfile.findViewById(R.id.nr_unread_messages);

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
	    
	    intent = new Intent().setClass(this, UserChatsActivity.class);
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
	    
		mIntentFilter = new IntentFilter("com.google.android.c2dm.intent.RECEIVE");
		mIntentFilter.setPriority(1);
		mIntentFilter.addCategory("nl.skope.android");
	    
	}	
	
	@Override
	protected void onResume() {
		super.onResume();
		if (checkCacheSanity()) {
			registerReceiver(mIntentReceiver, mIntentFilter);
			updateChatNotification();
		}
	}
	
	@Override
	protected void onPause() {
		try {
			unregisterReceiver(mIntentReceiver);
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
		super.onPause();
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
	
	protected boolean checkCacheSanity() {
		// Check user signed out
		if (mCache.isUserSignedOut()) {
			// User signed out, always go to login screen
			Intent i = new Intent();
			i.setClassName("nl.skope.android",
					"nl.skope.android.ui.LoginActivity");
			startActivity(i);
			finish();
			return false;
		} else {
			// Not signed out. Check if user present
			if (mCache.getUser() == null) {
				// Not present, could have been garbage collected.
				// Go back to login screen and set the auto login flag.
				Intent i = new Intent();
				i.setClassName("nl.skope.android",
						"nl.skope.android.ui.LoginActivity");
				// Add auto login flag
				Bundle bundle = new Bundle();
				bundle.putString(SkopeApplication.BUNDLEKEY_REDIRECTACTIVITY, getIntent().getClass().getName());
				i.putExtras(bundle);
				startActivity(i);
				finish();
				return false;
			}
		}
		
		return true;
	}	public void switchTab(int tab) {
		getTabHost().setCurrentTab(tab);
	}
	
	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Update chats
    		updateChatNotification();
        	
			// Get instance of Vibrator from current Context
			Vibrator v = (Vibrator) context
					.getSystemService(Context.VIBRATOR_SERVICE);

			// Vibrate for 300 milliseconds
			v.vibrate(300);

			abortBroadcast();
		}
	};
	
	public void updateChatNotification() {
		int userId = mCache.getUser().getId();
		String username = mCache.getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
		String password = mCache.getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
		String url = String.format("%s/user/%d/chat/?count&new", 
							mCache.getProperty("skope_service_url"),
							userId);
		// Send message
		new RetrieveNrUnreadMessages().execute(url, username, password);
	}
	
	protected class RetrieveNrUnreadMessages extends AsyncTask<String, Void, CustomHttpClient> {
		@Override
		protected CustomHttpClient doInBackground(String... params) {
			// Create HTTP client
			CustomHttpClient client = new CustomHttpClient(params[0]);
			client.setUseBasicAuthentication(true);
			client.setUsernamePassword(params[1], params[2]);
			
			// Send HTTP request to web service
			try {
				client.execute(RequestMethod.GET);
			} catch (Exception e) {
				// Most exceptions already handled by client
				e.printStackTrace();
			}

			return client;

		}

		@Override
		protected void onPostExecute(CustomHttpClient client) {
			// Check for server response
			switch (client.getResponseCode()) {
			case HttpStatus.SC_OK:
				// Extract chat message from response
				int nrUnreadMessages = 0;
				try {
		        	JSONObject jsonObject = new JSONObject(client.getResponse());
		        	nrUnreadMessages = Integer.parseInt(jsonObject.getString("count"));
		        } catch (JSONException e) {
					// Log exception
					Log.e(TAG, e.toString());
				}
		        
		        
				if (nrUnreadMessages > 0) {
					mUnreadMessagesView.setText(String.valueOf(nrUnreadMessages));
					mUnreadMessagesView.setVisibility(View.VISIBLE);
				} else {
					mUnreadMessagesView.setVisibility(View.GONE);
				}
				mTabHost.invalidate();		        
				
				break;
			case 0:
				// No server response
				Log.e(TAG, "Connection failed");
			case HttpStatus.SC_UNAUTHORIZED:
			case HttpStatus.SC_REQUEST_TIMEOUT:
			case HttpStatus.SC_BAD_GATEWAY:
			case HttpStatus.SC_GATEWAY_TIMEOUT:
			case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			case HttpStatus.SC_BAD_REQUEST:
				Log.e(TAG, "Failed to read chat count: "
						+ client.getErrorMessage());
			}

		}

	}	

}
