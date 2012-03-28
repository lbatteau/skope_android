package nl.skope.android.ui;

import nl.skope.android.R;
import nl.skope.android.application.Cache;
import nl.skope.android.application.ChatMessage;
import nl.skope.android.application.ObjectOfInterest;
import nl.skope.android.application.ServiceQueue;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.UiQueue;
import nl.skope.android.util.Type;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainTabActivity extends TabActivity {
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
	
	private int mTotalUnreadMessages;
	private TextView mUnreadMessagesView;

	/***
	 * Handler which is subscribed to the UiQueue whenever the Activity is on
	 * screen.
	 */
	private final Handler mHandler = new Handler() {
			@Override
			public void handleMessage(final Message message) {
	
				/** TEST: Try and catch an error condition **/
				if (message.what == Type.SHOW_DIALOG.ordinal()
						&& message.obj == null) {
					Log.e(SkopeApplication.LOG_TAG, "BaseActivity.Handler."
							+ "handleMessage() ERROR");
				}
				/** TEST: Try and catch an error condition **/
	
				processMessage(message);
			}
		};

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
	    
	    mTabHost.setOnTabChangedListener(new OnTabChangeListener() {

	        @Override
	        public void onTabChanged(String tabId) {
	            if (tabId.equals("chat")) {
	            	mUiQueue.unsubscribe(mHandler);
	            } else {
	            	mUiQueue.subscribe(mHandler);
	            }

	        }
	    });
	    
		mIntentFilter = new IntentFilter("com.google.android.c2dm.intent.RECEIVE");
		mIntentFilter.setPriority(1);
		mIntentFilter.addCategory("nl.skope.android");
	    
	}	
	
	@Override
	protected void onResume() {
		super.onResume();
		if (mTabHost.getCurrentTab() != TAB_CHAT) {
			mUiQueue.subscribe(mHandler);
			int userId; 
			Bundle bundle = new Bundle();
			userId = mCache.getUser().getId();
			bundle.putInt(SkopeApplication.BUNDLEKEY_USERID, userId);
			mServiceQueue.postToService(Type.READ_USER_CHATS, bundle);
		}
		registerReceiver(mIntentReceiver, mIntentFilter);
	}
	
	@Override
	protected void onPause() {
		mUiQueue.unsubscribe(mHandler);
		unregisterReceiver(mIntentReceiver);
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
	
	/***
	 * Overridable method for handling any messages not caught by the Activities
	 * own post() method. The code pattern allows more generic messages to be
	 * handled here (show battery warning dialog, etc).
	 * 
	 * @param type
	 *            Message type.
	 * @param bundle
	 *            Optional Bundle of extra information.
	 */
	public void post(final Type type, final Bundle bundle) {
		switch (type) {
		case READ_USER_CHATS_START:
			mTotalUnreadMessages = 0;
			break;
		case READ_USER_CHATS_END:
			updateNrUnreadMessages(mTotalUnreadMessages);
			break;
		case READ_USER_CHAT_MESSAGES_START:
			break;
		case READ_USER_CHAT_MESSAGES_END:
			// Get ooi from bundle
			ObjectOfInterest ooi = mCache.getUserChatsList().
							getById(bundle.getInt(SkopeApplication.BUNDLEKEY_USERID));
			// Get messages from bundle
			String response = bundle.getString(SkopeApplication.BUNDLEKEY_RESPONSE);
			// Check for 'unread' flag
			boolean isUnreadMessages = false;
			if (bundle.containsKey(SkopeApplication.BUNDLEKEY_UNREAD)) {
				isUnreadMessages = bundle.getBoolean(SkopeApplication.BUNDLEKEY_UNREAD);
			}		
			JSONArray jsonArray = null;
			if (response == null) {
				return;
			} else {
				// Extract JSON data from response
				try {
					jsonArray = new JSONArray(response);
				} catch (JSONException e) {
					// Log exception
					Log.e(SkopeApplication.LOG_TAG, e.toString());
				}
				
				if (isUnreadMessages) {
					// Update unread messages marker
					
					// Check size
					int nrOfMessages = jsonArray.length();
					mTotalUnreadMessages += nrOfMessages;
					if (mTotalUnreadMessages == 0) {
    					// Nothing in result
    					ooi.setNrUnreadMessages(0);
    				} else {
        				ooi.setNrUnreadMessages(mTotalUnreadMessages);	      					
    				}
    				
				} else {
					// Extract the last chat message
    				ChatMessage lastChat = null;
					try {
						JSONObject jsonObject = jsonArray.getJSONObject(jsonArray.length()-1);
	    				lastChat = new ChatMessage(jsonObject);
					} catch (JSONException e) {
						Log.e(SkopeApplication.LOG_TAG, e.toString());
					}
					
					if (lastChat != null) {
						if (ooi != null) {
	    					// Update user's last message and notify adapter
							ooi.setLastChatMessage(lastChat.createTimeLabel() + ": " + lastChat.getMessage());
						}
					}
				}
			}
			break;
        case UNDETERMINED_LOCATION:
        	Toast.makeText(this, "Location currently unavailable", Toast.LENGTH_LONG).show();
        	break;
        	
	
		default:
			// Do nothing.
			break;
		}
	}
	
	/***
	 * Process an incoming message by getting the Type and optional bundle and
	 * passing it to the overridable post() method.
	 * 
	 * @param message
	 *            Message to process.
	 */
	private void processMessage(final Message message) {
		if (message.obj != null && message.obj.getClass() == Bundle.class) {
			post(Type.getType(message.what), (Bundle) message.obj);
		} else {
			post(Type.getType(message.what), null);
		}
	}

	public void switchTab(int tab) {
		getTabHost().setCurrentTab(tab);
	}
	
	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Update chats
    		Bundle bundle = new Bundle();
        	bundle.putInt(SkopeApplication.BUNDLEKEY_USERID, mCache.getUser().getId());
        	mServiceQueue.postToService(Type.READ_USER_CHATS, bundle);
        	
			// Get instance of Vibrator from current Context
			Vibrator v = (Vibrator) context
					.getSystemService(Context.VIBRATOR_SERVICE);

			// Vibrate for 300 milliseconds
			v.vibrate(300);

			abortBroadcast();
		}
	};
	
	public void updateNrUnreadMessages(int nrUnreadMessages) {
		mTotalUnreadMessages = nrUnreadMessages;
		if (nrUnreadMessages > 0) {
			mUnreadMessagesView.setText(String.valueOf(nrUnreadMessages));
			mUnreadMessagesView.setVisibility(View.VISIBLE);
		} else {
			mUnreadMessagesView.setVisibility(View.GONE);
		}
		mTabHost.invalidate();
	}

}
