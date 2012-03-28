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

import java.util.List;

import nl.skope.android.R;
import nl.skope.android.application.ChatMessage;
import nl.skope.android.application.ObjectOfInterest;
import nl.skope.android.application.ObjectOfInterestList;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.User.OnImageLoadListener;
import nl.skope.android.util.Type;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
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
    
	private OnItemClickListener mOOISelectListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> a, View v, int position, long id) {
			// Check if selected ooi is current user
			ObjectOfInterest ooi = mChatsListAdapter.getItem(position);
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
    }

	@Override
	public void onResume() {
		super.onResume();
		int userId; 
		Bundle bundle = new Bundle();
		userId = getCache().getUser().getId();
		bundle.putInt(SkopeApplication.BUNDLEKEY_USERID, userId);
		getServiceQueue().postToService(Type.READ_USER_CHATS, bundle);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
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
            	parent.updateNrUnreadMessages(mNrUnreadMessages);
            	mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            	
            	break;
            	
    		case READ_USER_CHAT_MESSAGES_START:
    			break;
    		case READ_USER_CHAT_MESSAGES_END:
    			// Get ooi from bundle
    			ObjectOfInterest ooi = getCache().getUserChatsList().
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
    						Log.e(SkopeApplication.LOG_TAG, e.toString());
    					}
    					
    					if (lastChat != null) {
    						if (ooi != null) {
		    					// Update user's last message and notify adapter
								ooi.setLastChatMessage(lastChat.createTimeLabel() + ": " + lastChat.getMessage());
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
    
    private class ObjectOfInterestArrayAdapter extends ArrayAdapter<ObjectOfInterest> {
    	private LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	
    	OnImageLoadListener mProfilePictureListener = new OnImageLoadListener() {
			@Override
			public void onImageLoaded(Bitmap thumbnail) {
				ObjectOfInterestArrayAdapter.this.notifyDataSetChanged();
			}
		};
    	
    	public ObjectOfInterestArrayAdapter(Context context, int textViewResourceId,
    			List<ObjectOfInterest> objects) {
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
            
            ObjectOfInterest ooi = getItem(position);
            
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
}