/*
 * SkopeActivity
 * 
 * Version information
 *
 * Date
 * 
 * Copyright notice
 */
package nl.skope.android.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nl.skope.android.R;
import nl.skope.android.application.ChatMessage;
import nl.skope.android.application.ObjectOfInterestList;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.User;
import nl.skope.android.application.User.OnImageLoadListener;
import nl.skope.android.util.Type;
import nl.skope.android.util.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Class description goes here.
 *
 * @version "%I%, %G%"
 * @author  Lukas Batteau
 */
public class UserChatsActivity extends BaseActivity {	  
	private static final String TAG = UserChatsActivity.class.getName();
	
    private ObjectOfInterestList mChatsList = null;
    private ObjectOfInterestArrayAdapter mChatsListAdapter;
    private String mUsername;
    private int mNrUnreadMessages;

    protected Dialog mSplashDialog;
    
    private ProgressBar mProgressBar;
    
    // C2DM Intent Filter
	private IntentFilter mIntentFilter;
	
	private OnItemClickListener mOOISelectListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> a, View v, int position, long id) {
			// Check if selected ooi is current user
			User ooi = mChatsListAdapter.getItem(position);
			Intent i = new Intent();
			// Redirect to detail chat activity
	        Bundle bundle = new Bundle();
	        bundle.putParcelable(SkopeApplication.BUNDLEKEY_USER, ooi);
	        i.putExtras(bundle);
			i.setClassName("nl.skope.android","nl.skope.android.ui.OOIChatActivity");
			startActivity(i);
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	// Set the main layout
    	if (getIntent() != null && getIntent().getExtras() != null) {
    		setContentView(R.layout.ooi_favorites);        	

    		// Back button
    	    View backButton = findViewById(R.id.detail_back_button);
    	    backButton.setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				finish();
    			}
    		});
    	    
		} else {
			setContentView(R.layout.user_favorites);
		}
    	
    	// Hide the progress bar
    	mProgressBar = (ProgressBar) findViewById(R.id.titleProgressBar);
    	mProgressBar.setVisibility(ProgressBar.INVISIBLE);
    	
    	// Set up the list adapter
        mChatsList = new ObjectOfInterestList();
        mChatsListAdapter = new ObjectOfInterestArrayAdapter(UserChatsActivity.this, R.layout.skope_view, mChatsList);
        ListView listView = (ListView)findViewById(R.id.list);
        listView.setAdapter(mChatsListAdapter); 
        listView.setOnItemClickListener(mOOISelectListener);
        
        mIntentFilter = new IntentFilter("com.google.android.c2dm.intent.RECEIVE");
		mIntentFilter.setPriority(2); // Make sure it overrides receiver in MainTabActivity
		mIntentFilter.addCategory("nl.skope.android");
    }

	@Override
	public void onResume() {
		super.onResume();
		if (checkCacheSanity()) {
			requestChatUsersUpdate();
			registerReceiver(mIntentReceiver, mIntentFilter);			
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
	public void onSaveInstanceState(Bundle savedInstanceState) {
		ArrayList<User> list = new ArrayList<User>();
		list.addAll(mChatsList);
		savedInstanceState.putParcelableArrayList("chat_users", list);
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		ArrayList<User> list = new ArrayList<User>();
		list = savedInstanceState.getParcelableArrayList("chat_users");
		for (User user: list) {
			mChatsList.add((User) user);
		}
	}

	private void requestChatUsersUpdate() {
		int userId; 
		Bundle bundle = new Bundle();
		userId = getCache().getUser().getId();
		bundle.putInt(SkopeApplication.BUNDLEKEY_USERID, userId);
		getServiceQueue().postToService(Type.READ_USER_CHATS, bundle);		
	}
	
	private void updateListFromCache() {
		mChatsList.clear();
    	ObjectOfInterestList cacheList = getCache().getUserChatsList();
    	if (cacheList != null && !cacheList.isEmpty()) {
        	// Cache contains items
        	mChatsList.addAll(cacheList);
		}
    	mChatsListAdapter.notifyDataSetChanged();
    }
    
    /*
    
    /***
     * Override the post method to receive incoming messages from the Service.
     *
     * @param type Message type.
     * @param bundle Optional Bundle of extra information, NULL otherwise.
     */
    @Override
    public final void post(final Type type, final Bundle bundle) {
    	switch (type) {
            case READ_USER_CHATS_START:
            	// If list empty show load bar
            	if (mChatsList.size() == 0) { 
            		mProgressBar.setVisibility(ProgressBar.VISIBLE);
            	}
            	mNrUnreadMessages = 0;
                break;

            case READ_USER_CHATS_END:
            	updateListFromCache();
            	MainTabActivity parent = (MainTabActivity) getParent();
            	parent.updateChatNotification();
            	mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            	
                // Now retrieve all last messages to display in the list
            	for (User ooi: mChatsList) {
            		Bundle messagesBundle = new Bundle();
                	messagesBundle.putInt(SkopeApplication.BUNDLEKEY_USERID, ooi.getId());
                	//messagesBundle.putBoolean(SkopeApplication.BUNDLEKEY_CHAT_FROM, true);
                	messagesBundle.putBoolean(SkopeApplication.BUNDLEKEY_CHAT_LAST, true);
                	getServiceQueue().postToService(Type.READ_USER_CHAT_MESSAGES, messagesBundle);
            	}
            	
            	// Retrieve number of unread messages
            	for (User ooi: mChatsList) {
            		Bundle messagesBundle = new Bundle();
                	messagesBundle.putInt(SkopeApplication.BUNDLEKEY_USERID, ooi.getId());
                	messagesBundle.putBoolean(SkopeApplication.BUNDLEKEY_CHAT_UNREAD, true);
                	messagesBundle.putBoolean(SkopeApplication.BUNDLEKEY_CHAT_FROM, true);
                	getServiceQueue().postToService(Type.READ_USER_CHAT_MESSAGES, messagesBundle);
            	}
                
            	break;
            	
    		case READ_USER_CHAT_MESSAGES_START:
    			break;
    		case READ_USER_CHAT_MESSAGES_END:
    			// Get ooi from bundle
    			User ooi = getCache().getUserChatsList().
    							getById(bundle.getInt(SkopeApplication.BUNDLEKEY_USERID));
				// Get messages from bundle
    			String response = bundle.getString(SkopeApplication.BUNDLEKEY_RESPONSE);
    			// Check for 'unread' flag
    			boolean isUnreadMessages = false;
    			if (bundle.containsKey(SkopeApplication.BUNDLEKEY_CHAT_UNREAD)) {
    				isUnreadMessages = bundle.getBoolean(SkopeApplication.BUNDLEKEY_CHAT_UNREAD);
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
    					Log.e(TAG, e.toString());
    				}
    				
    				if (isUnreadMessages) {
    					// Update unread messages marker
						
    					// Check size
    					int nrUnreadMessages = jsonArray.length();
        				if (nrUnreadMessages == 0) {
        					// Nothing in result
        					ooi.setNrUnreadMessages(0);
        				} else {
            				ooi.setNrUnreadMessages(nrUnreadMessages);	      					
        				}
        				
        				mNrUnreadMessages = nrUnreadMessages;
        				
    				} else {
    					// Extract the last chat message
        				ChatMessage lastChat = null;
    					try {
    						JSONObject jsonObject = jsonArray.getJSONObject(jsonArray.length()-1);
    	    				lastChat = new ChatMessage(jsonObject);
    					} catch (JSONException e) {
    						Log.e(TAG, e.toString());
    					}
    					
    					if (lastChat != null) {
    						if (ooi != null) {
		    					// Update user's last message and notify adapter
    							if (Utility.isDateSameDay(lastChat.getTimestamp(), new Date())) {
    								ooi.setLastChatMessage(lastChat.createTimeLabel() + ": " + lastChat.getMessage());
    							} else if (Utility.isYesterday(lastChat.getTimestamp())){
    								ooi.setLastChatMessage(getResources().getString(R.string.yesterday) + 
    										": " + lastChat.getMessage());
    							} else {
    								ooi.setLastChatMessage(lastChat.createDateLabel() + ": " + lastChat.getMessage());
    							}
								
    						}
    					}
					}
					mChatsListAdapter.notifyDataSetChanged();

    			}
    			break;
    		case UNDETERMINED_LOCATION:
            	Toast.makeText(this, "Location currently unavailable", Toast.LENGTH_LONG).show();
            	break;
            	
            case LOCATION_CHANGED:
            	//Toast.makeText(this, "Location updated", Toast.LENGTH_LONG).show();
            	break;

            default:
                /** Let the BaseActivity handle other message types. */
                super.post(type, bundle);
                break;
        }
    }
    
    public static class ViewHolder {
		public TextView nameText;
		public TextView distanceText;
		public TextView lastUpdateText;
		public TextView nrUnreadMessages;
		public ImageView icon;

	}
    
    private class ObjectOfInterestArrayAdapter extends ArrayAdapter<User> {
    	private LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	
    	OnImageLoadListener mProfilePictureListener = new OnImageLoadListener() {
			@Override
			public void onImageLoaded(Bitmap thumbnail) {
				ObjectOfInterestArrayAdapter.this.notifyDataSetChanged();
			}
		};
    	
    	public ObjectOfInterestArrayAdapter(Context context, int textViewResourceId,
    			List<User> objects) {
    		super(context, textViewResourceId, objects);
    	}

		@Override
        public View getView(int position, View convertView, final ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
                convertView = (View) mInflater.inflate(R.layout.skope_item, null);
                holder = new ViewHolder();
                holder.nameText = (TextView) convertView.findViewById(R.id.name_text);
                holder.distanceText = (TextView) convertView.findViewById(R.id.distance_text);
                holder.nrUnreadMessages = (TextView) convertView.findViewById(R.id.nr_unread_messages);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                convertView.setTag(holder);
            } else {
            	holder = (ViewHolder) convertView.getTag();
            }             
            
            User ooi = getItem(position);
            
            if (ooi != null) {
                if (holder.nameText != null) {
                	holder.nameText.setText(ooi.createName());                            
                }
                
                if (holder.distanceText != null) {
                	holder.distanceText.setText(ooi.getLastChatMessage());
                }
                
                if (holder.nrUnreadMessages != null) {
                	if (ooi.getNrUnreadMessages() > 0) {
                		holder.nrUnreadMessages.setText(String.valueOf(ooi.getNrUnreadMessages()));
                		holder.nrUnreadMessages.setVisibility(View.VISIBLE);
                	} else {
                		holder.nrUnreadMessages.setVisibility(View.GONE);
                	}
                }
                
                if (holder.icon != null) {
                	holder.icon.setImageBitmap(ooi.getProfilePicture()); // even when null, otherwise previous values remain
            		// Lazy loading
                	if (ooi.getProfilePicture() == null) {
                		ooi.loadProfilePicture(mProfilePictureListener);
                	}
                }
            }
            
            return convertView;
        }
    }
    
	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Update chats
			requestChatUsersUpdate();
        	
			// Get instance of Vibrator from current Context
			Vibrator v = (Vibrator) context
					.getSystemService(Context.VIBRATOR_SERVICE);

			// Vibrate for 300 milliseconds
			v.vibrate(300);

			abortBroadcast();
		}
	};
	
    
}