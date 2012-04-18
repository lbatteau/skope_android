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

import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.User;
import nl.skope.android.application.ObjectOfInterestList;
import nl.skope.android.application.User.OnImageLoadListener;
import nl.skope.android.util.Type;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import nl.skope.android.R;

/**
 * Class description goes here.
 *
 * @version "%I%, %G%"
 * @author  Lukas Batteau
 */
public class UserFavoritesActivity extends BaseActivity {	  
	private static final String TAG = UserFavoritesActivity.class.getName();
	
    private ObjectOfInterestList mFavoritesList = null;
    private ObjectOfInterestArrayAdapter mFavoritesListAdapter;
    private String mUsername;

    protected Dialog mSplashDialog;
    
    private ProgressBar mProgressBar;
    
	private OnItemClickListener mOOISelectListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> a, View v, int position, long id) {
			// Check if selected ooi is current user
			User ooi = mFavoritesListAdapter.getItem(position);
			Intent i = new Intent();
			if (ooi.getId() == getCache().getUser().getId()) {
				// Current user, redirect to profile
				Bundle bundle = new Bundle();
		        bundle.putInt("TAB", MainTabActivity.TAB_PROFILE);
		        i.putExtras(bundle);i.setClassName("nl.skope.android",
						"nl.skope.android.ui.MainTabActivity");
			} else {
				// Redirect to list activity
		        Bundle bundle = new Bundle();
		        bundle.putParcelable("USER", ooi);
		        i.putExtras(bundle);
				i.setClassName("nl.skope.android",
						"nl.skope.android.ui.OOIDetailMapActivity");
			}
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
        mFavoritesList = new ObjectOfInterestList();
        mFavoritesListAdapter = new ObjectOfInterestArrayAdapter(UserFavoritesActivity.this, R.layout.skope_view, mFavoritesList);
        ListView listView = (ListView)findViewById(R.id.list);
        listView.setAdapter(mFavoritesListAdapter); 
        listView.setOnItemClickListener(mOOISelectListener);

    }

	private void updateListFromCache() {
		mFavoritesList.clear();
    	ObjectOfInterestList cacheList = getCache().getUserFavoritesList();
    	if (cacheList != null && !cacheList.isEmpty()) {
        	// Cache contains items
        	mFavoritesList.addAll(cacheList);
		}
    	mFavoritesListAdapter.notifyDataSetChanged();
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
            case READ_USER_FAVORITES_START:
            	// If list empty show load bar
            	if (mFavoritesList.size() == 0) { 
            		mProgressBar.setVisibility(ProgressBar.VISIBLE);
            	}
                break;

            case READ_USER_FAVORITES_END:
            	updateListFromCache();
            	mProgressBar.setVisibility(ProgressBar.INVISIBLE);
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
                	holder.distanceText.setText("Distance: " + String.valueOf(ooi.createLabelDistance())
                			+ " - " + ooi.createLabelTimePassedSinceLastUpdate());
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
    
	@Override
	public void onResume() {
		super.onResume();
		if (checkCacheSanity()) {
			int userId; 
			Bundle bundle = new Bundle();
			if (getIntent() != null && getIntent().getExtras() != null) {
				userId = getIntent().getExtras().getInt(SkopeApplication.BUNDLEKEY_USERID);
			} else {
				userId = getCache().getUser().getId();
			}
			bundle.putInt(SkopeApplication.BUNDLEKEY_USERID, userId);
			getServiceQueue().postToService(Type.READ_USER_FAVORITES, bundle);
		}
	}
}