package com.skope.skope.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.ObjectOfInterest;
import com.skope.skope.application.ObjectOfInterestList;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.UserPhoto;
import com.skope.skope.application.User.OnImageLoadListener;
import com.skope.skope.http.CustomHttpClient;
import com.skope.skope.http.CustomHttpClient.RequestMethod;
import com.skope.skope.maps.OOIOverlay;
import com.skope.skope.ui.UserFavoritesActivity.ViewHolder;
import com.skope.skope.util.Type;

public class OOIDetailMapActivity extends OOIMapActivity {
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private GestureDetector mGestureDetector;
    private View.OnTouchListener mGestureListener;
    private int mOOIPosition;
    private View mNavigationHandleRight, mNavigationHandleLeft;
	private Animation mFadeInAnimation, mFadeOutAnimation;
	private SlidingDrawer mMapDrawer;
	private ExpandableListAdapter mListAdapter;
	private View mUserProfileMain, mUserProfile, mUserPhotoLayout, mFavoritesLayout;
    private LayoutInflater mInflater;
    private Gallery mUserPhotoGallery;    
	private UserPhotoAdapter mUserPhotoAdapter;
	private ArrayList<UserPhoto> mUserPhotoList;
	private ProgressBar mPhotosProgressBar, mFavoritesProgressBar;	 
	private TextView mPhotosLabel, mFavoritesLabel;
	private ObjectOfInterest mSelectedOOI;
	ObjectOfInterestList mFavoritesList;
	ObjectOfInterestArrayAdapter mFavoritesListAdapter;
	
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    mSelectedOOI = getIntent().getExtras().getParcelable("USER");
	    mSelectedOOI.setCache(getCache());
	    
	    mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    
	    // Need this for passing on touch events
	    mUserProfile = findViewById(R.id.user_profile);

	    // Open map drawer
	    mMapDrawer = (SlidingDrawer) findViewById(R.id.mapSlidingDrawer);
    	// Check if present; no drawer in landscape mode
	    if (mMapDrawer != null) {
	    	mMapDrawer.animateOpen();
	    }
	    
	    // Back button
	    View backButton = findViewById(R.id.detail_back_button);
	    backButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	    
	    /*
	     *  Favorites button
	     */
	    
	    final View favoriteButton = findViewById(R.id.favorite_button);
	    final View favoriteIcon = findViewById(R.id.favorite_icon);
    	
	    // Check for highlighting
	    if (getCache().getUserFavoritesList().contains(mSelectedOOI)) {
	    	// Selected user is a favorite of the current user. Highlight.
	    	favoriteIcon.setBackgroundResource(R.drawable.detail_button_favorite_active_selector);
	    }
	    
	    // Favorites button event
	    favoriteButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (getCache().getUserFavoritesList().contains(mSelectedOOI)) {
					// Delete favorite
					String title = getResources().getString(R.string.user_favorite_confirm_remove_title);
					String messageFormat = getResources().getString(R.string.user_favorite_confirm_remove_message);
					String message = String.format(messageFormat, mSelectedOOI.createName());
					new AlertDialog.Builder(OOIDetailMapActivity.this)
			        .setTitle(title)
			        .setMessage(message)
			        .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int whichButton) {
			            	UserFavoriteDelete runner = new UserFavoriteDelete();
							runner.setListener(new OnFavoriteUpdateListener() {
								@Override
								public void onFavoriteUpdateStart() {
								}
								
								@Override
								public void onFavoriteUpdateDone(boolean isSuccess, String message) {
									if (isSuccess) {
										getCache().getUserFavoritesList().remove(mSelectedOOI);
										favoriteIcon.setBackgroundResource(R.drawable.detail_button_favorite_selector);
									} else {
										Toast.makeText(OOIDetailMapActivity.this, "Sorry, please try again", Toast.LENGTH_SHORT).show();
									}
								}
							});
							runner.execute(mSelectedOOI.getUserName());
			            }
			        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int whichButton) {
			                // Do nothing.
			            }
			        }).show();
				} else {
					// Add favorite
					String title = getResources().getString(R.string.user_favorite_confirm_add_title);
					String messageFormat = getResources().getString(R.string.user_favorite_confirm_add_message);
					String message = String.format(messageFormat, mSelectedOOI.createName());
					new AlertDialog.Builder(OOIDetailMapActivity.this)
			        .setTitle(title)
			        .setMessage(message)
			        .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int whichButton) {
			            	UserFavoritePost runner = new UserFavoritePost();
							runner.setListener(new OnFavoriteUpdateListener() {
								@Override
								public void onFavoriteUpdateStart() {
								}
								
								@Override
								public void onFavoriteUpdateDone(boolean isSuccess, String message) {
									if (isSuccess) {
										getCache().getUserFavoritesList().add(mSelectedOOI);
										favoriteIcon.setBackgroundResource(R.drawable.detail_button_favorite_active_selector);
									} else {
										Toast.makeText(OOIDetailMapActivity.this, "Sorry, please try again", Toast.LENGTH_SHORT).show();
									}
								}
							});
							runner.execute(mSelectedOOI.getUserName());
			            }
			        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int whichButton) {
			                // Do nothing.
			            }
			        }).show();
				}
				
			}
		});
	    
	    /*
		 * The user profile is actually an expandable list view, with the
		 * first item containing the main info like name, status, profile
		 * picture etc. This item has no children, so shouldn't expand.
		 * The rest of the list consists of photos, mFavorites, etc.
		 */
	    mListAdapter = new UserProfileExpandableListAdapter();
	    ExpandableListView expandableList = (ExpandableListView)findViewById(R.id.expandable_list);
	    expandableList.setAdapter(mListAdapter);
	    		
	    /*
	     * Set up navigation buttons. Transparent left and right arrows 
	     * appear when the user touches the screen. On a 'fling' gesture to the
	     * left or right, the previous or next user profile is loaded.
	     *
	     */ 	    
	       
	    // Store handles to navigation buttons
	    mNavigationHandleRight = findViewById(R.id.nav_right);
	    mNavigationHandleLeft = findViewById(R.id.nav_left);
	    
	    // Make invisible
	    mNavigationHandleLeft.setVisibility(View.INVISIBLE);
		mNavigationHandleRight.setVisibility(View.INVISIBLE);
		
		/*LMB 6-3-2012: DISABLED SWIPE LEFT/RIGHT BECAUSE IT INTERFERES WITH GALLERY INTERACTION
	    
	    // Navigation button fade animations
    	mFadeInAnimation = AnimationUtils.loadAnimation(OOIDetailMapActivity.this, R.anim.fade_in);
    	mFadeOutAnimation = AnimationUtils.loadAnimation(OOIDetailMapActivity.this, R.anim.fade_out);
    	
    	// Configure visibility of navigation buttons before and after fade in
    	mFadeInAnimation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				mNavigationHandleLeft.setVisibility(View.VISIBLE);
				mNavigationHandleRight.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				// Do nothing
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				mNavigationHandleLeft.setVisibility(View.VISIBLE);
				mNavigationHandleRight.setVisibility(View.VISIBLE);
			}
		});
    	
    	// Configure visibility of navigation buttons before and after fade out
    	mFadeOutAnimation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				mNavigationHandleLeft.setVisibility(View.VISIBLE);
				mNavigationHandleRight.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				// Do nothing
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				mNavigationHandleLeft.setVisibility(View.INVISIBLE);
				mNavigationHandleRight.setVisibility(View.INVISIBLE);
			}
		});
    	
    	mGestureDetector = new GestureDetector(this, new MyGestureDetector());
	    mGestureListener = new OnTouchListener() {
	    	// Hide/show navigation buttons before/after fades
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()) {
				case MotionEvent.ACTION_UP:
					mNavigationHandleRight.startAnimation(mFadeOutAnimation);
					mNavigationHandleLeft.startAnimation(mFadeOutAnimation);				    
				}
				
				// When interacting with map, don't listen for anything else
				if (mMapDrawer.dispatchTouchEvent(event)) {
					return false;
				}
				
				// Since the gesture listener might consume the event, we  
				// dispatch the touch event to the profile frame before 
				// passing it to the gesture listener
				mUserProfile.dispatchTouchEvent(event);
				
				return mGestureDetector.onTouchEvent(event);
			}
		};
		
	    View view = findViewById(R.id.overlay);
	    view.setOnTouchListener(mGestureListener);*/
	    
	    mUserProfileMain = mInflater.inflate(R.layout.ooi_profile_main, null);
	    
	    mOOIPosition = getCache().getObjectOfInterestList().getSelectedPosition();
	    
	    // Initial empty list of user photos
	    mUserPhotoList = new ArrayList<UserPhoto>();
	    // User photos adapter
	    mUserPhotoAdapter = new UserPhotoAdapter(this, R.id.user_photo_grid, mUserPhotoList);
	    // User photos gallery 
	    mUserPhotoLayout = mInflater.inflate(R.layout.user_photo_gallery, null);
	    mUserPhotoGallery = (Gallery) mUserPhotoLayout.findViewById(R.id.user_photo_gallery);
	    mUserPhotoGallery.setAdapter(mUserPhotoAdapter);
	    
	    mUserPhotoGallery.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long id) {
				// Redirect to photo activity
		        Intent i = new Intent(OOIDetailMapActivity.this, UserPhotoActivity.class);
		        i.putExtra("position", position);
		        i.putExtra("is_current_user", false);
	        	startActivity(i);
			}
		});
	    
	    // Photos progress bar
		mPhotosProgressBar = (ProgressBar) mUserPhotoLayout.findViewById(R.id.user_photo_progress_bar);
		// Photos label
		mPhotosLabel = (TextView) mUserPhotoLayout.findViewById(R.id.user_photo_label);
		
		// User favorites layout
		mFavoritesLayout = mInflater.inflate(R.layout.user_favorites, null);
    	// Favorites progress bar
		mFavoritesProgressBar = (ProgressBar) mFavoritesLayout.findViewById(R.id.titleProgressBar);
		mFavoritesProgressBar.setVisibility(ProgressBar.INVISIBLE);

		// List of user favorites
        mFavoritesList = new ObjectOfInterestList();
        mFavoritesListAdapter = new ObjectOfInterestArrayAdapter(OOIDetailMapActivity.this, R.layout.skope_view, mFavoritesList);
        ListView listView = (ListView) mFavoritesLayout.findViewById(R.id.list);
	    listView.setAdapter(mFavoritesListAdapter); 
        listView.setOnItemClickListener(mOOISelectListener);
	}
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	Cache.USER_PHOTOS.clear();
    }

	@Override
	protected void onResume() {
		super.onResume();
		update();
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putParcelable("USER", mSelectedOOI);
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mSelectedOOI = savedInstanceState.getParcelable("USER");
	}

	/**
	 * Retrieves the currently selected object of interest, updates the
	 * information in the view, and calls the necessary methods to update the
	 * map view.
	 */
	protected void update() {
		// Check if object still present
		if (mSelectedOOI == null) {
			// No longer present, return to list view
			// Redirect to list activity
	        Intent i = new Intent();
        	i.setClassName("com.skope.skope",
        				   "com.skope.skope.ui.MainTabActivity");
        	startActivity(i);
        	finish();	
		}

		mSelectedOOI.createUserProfile(mUserProfileMain, mInflater);
		
		// Empty current photo list
		Cache.USER_PHOTOS.clear();
		// Read photos
		Bundle photosBundle = new Bundle();
        photosBundle.putString("USERNAME", mSelectedOOI.getUserEmail());
        getServiceQueue().postToService(Type.READ_USER_PHOTOS, photosBundle);
        
        // Read favorites
        Bundle favoritesBundle = new Bundle();
        favoritesBundle.putString("USERNAME", mSelectedOOI.getUserName());
		getServiceQueue().postToService(Type.READ_USER_FAVORITES, favoritesBundle);
		
        initializeMapView();
		populateItemizedOverlays();
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
	
	protected void goToNext() {
		ObjectOfInterestList ooiList = getCache().getObjectOfInterestList();
		if (ooiList.size() <= mOOIPosition) {
			return;
		} else {
			mUserPhotoAdapter.clear();
			ooiList.setSelectedPosition(++mOOIPosition);
			update();
		}		
	}
	
	protected void goToPrevious() {
		ObjectOfInterestList ooiList = getCache().getObjectOfInterestList();
		if (mOOIPosition == 0) {
			return;
		} else {
			mUserPhotoAdapter.clear();
			ooiList.setSelectedPosition(--mOOIPosition);
			update();
		}		
	}

	@Override
	protected void setContentView() {
		setContentView(R.layout.detail);
	}

	@Override
	protected void initializeMapView() {
		super.initializeMapView();

		mMapView.invalidate();
	}

	@Override
	protected void populateItemizedOverlays() {
		// Clear current overlays
		mMapOverlays = mMapView.getOverlays();

		// Clear ooi overlay
		if (mMapOverlays.size() > 1) {
			mMapOverlays.remove(1);
		}

		LayerDrawable marker = (LayerDrawable) getResources().getDrawable(
				R.drawable.marker);

		OOIOverlay ooiOverlay = new OOIOverlay(marker, this);
		ooiOverlay.addOverlay(createOverlay(mSelectedOOI));
		mMapOverlays.add(1, ooiOverlay);

		MapController mapController = mMapView.getController();

		int userLatitude = (int) (getCache().getCurrentLocation().getLatitude() * 1E6);
		int userLongitude = (int) (getCache().getCurrentLocation()
				.getLongitude() * 1E6);
		int ooiLatitude = (int) (mSelectedOOI.getLocation()
				.getLatitude() * 1E6);
		int ooiLongitude = (int) (mSelectedOOI.getLocation()
				.getLongitude() * 1E6);

		mapController.animateTo(new GeoPoint((userLatitude + ooiLatitude) / 2,
				(userLongitude + ooiLongitude) / 2));

		mapController.zoomToSpan(Math.abs(userLatitude - ooiLatitude),
				Math.abs(userLongitude - ooiLongitude));

	}

	@Override
	public void post(final Type type, final Bundle bundle) {
		switch (type) {
		case READ_USER_PHOTOS_START:
			// Hide label
			mPhotosLabel.setVisibility(View.INVISIBLE);
			
			// Display progress bar when gallery is empty
			if (mUserPhotoAdapter.isEmpty()) {
				mPhotosProgressBar.setVisibility(View.VISIBLE);
			}
			break;
		case READ_USER_PHOTOS_END:
			// Hide progress bar
			mPhotosProgressBar.setVisibility(View.INVISIBLE);
			// Copy user photo list from cache
			mUserPhotoAdapter.clear();
			for(UserPhoto userphoto: Cache.USER_PHOTOS) {
				mUserPhotoAdapter.add(userphoto);
			}
			
			// Show label if no photos present
			if (mUserPhotoAdapter.getCount() == 0) {
				mPhotosLabel.setVisibility(View.VISIBLE);
				mPhotosLabel.setText(getResources().getString(R.string.user_photos_none));
			} else {
				mPhotosLabel.setVisibility(View.INVISIBLE);
			}
			
			break;
        case READ_USER_FAVORITES_START:
        	// If list empty show load bar
        	if (mFavoritesList.size() == 0) { 
        		mFavoritesProgressBar.setVisibility(ProgressBar.VISIBLE);
        	}
            break;

        case READ_USER_FAVORITES_END:
        	updateListFromCache();
        	mFavoritesProgressBar.setVisibility(ProgressBar.INVISIBLE);
        	break;
        	
			
        default:
			super.post(type, bundle);
		}
	}
	
	class MyGestureDetector extends SimpleOnGestureListener {

		@Override
		public boolean onDown(MotionEvent e) {
			mNavigationHandleRight.startAnimation(mFadeInAnimation);
			mNavigationHandleLeft.startAnimation(mFadeInAnimation);
		    return true;
		}
		
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
	    		    return false;
                }
                // right to left swipe
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    goToNext();
                }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    goToPrevious();
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }
	};

    /**
     * A simple adapter which maintains an ArrayList of mPhoto resource Ids. 
     * Each mPhoto is displayed as an image. This adapter supports clearing the
     * list of photos and adding a new mPhoto.
     *
     */
    public class UserProfileExpandableListAdapter extends BaseExpandableListAdapter {
        public Object getChild(int groupPosition, int childPosition) {
            switch(groupPosition) {
            case 1:
            	return "Test1";
            case 2:
            	return "Test2";
            default:
            	return null;
            }
        }
        
        @Override
        public boolean areAllItemsEnabled() {
        	return false;
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) {
        	switch(groupPosition) {
            case 1:
            	return 1;
            case 2:
            	return 1;
            default:
            	return 0;
            }
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
        	switch(groupPosition) {
            case 1:
            	return mUserPhotoLayout;
            case 2:
            	return mFavoritesLayout;
            default:
            	TextView textView = new TextView(OOIDetailMapActivity.this);
                textView.setText(getChild(groupPosition, childPosition).toString());
                return textView;
            }
        	
        	
        }

		@Override
		public Object getGroup(int groupPosition) {
			return null;
		}

        public int getGroupCount() {
            return 3;
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public View createGroupView(String label, boolean isExpanded, int iconDrawable) {
            // Layout parameters for the ExpandableListView
            AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            
            View view = mInflater.inflate(R.layout.expanded_list_group, null);
            view.setLayoutParams(lp);
        	
            // Set label
        	TextView labelView = (TextView) view.findViewById(R.id.label);
        	labelView.setCompoundDrawablePadding(10);
        	labelView.setCompoundDrawablesWithIntrinsicBounds(iconDrawable, 0, 0, 0);
        	labelView.setText(label);
        	
        	// Indicator
        	ImageView indicator = (ImageView) view.findViewById(R.id.indicator);
        	if (isExpanded) {
        		indicator.setImageResource(R.drawable.nav_arrow_down);
        	} else {
        		indicator.setImageResource(R.drawable.nav_arrow_right);
        	}
        	
            return view;
        }
        
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
        	switch(groupPosition) {
            case 0: {
            	View view = mUserProfileMain;
            	view.setBackgroundDrawable(null);
            	return view;
            } case 1: {
            	return createGroupView("Photos", isExpanded, R.drawable.expand_icon_photos);
            } case 2:
            	return createGroupView("Favorites", isExpanded, R.drawable.expand_icon_favorites);
            }
        	return null;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

        public boolean hasStableIds() {
            return true;
        }

    }
    
	public interface OnFavoriteUpdateListener {
		public void onFavoriteUpdateStart();
		public void onFavoriteUpdateDone(boolean isSuccess, String message);
	}
	
	protected class UserFavoritePost extends AsyncTask<String, Void, CustomHttpClient> {
		OnFavoriteUpdateListener mListener;
		
		@Override
		protected void onPreExecute() {
			mListener.onFavoriteUpdateStart();
		}

		@Override
		protected CustomHttpClient doInBackground(String... params) {
			String username = mCache.getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
			String password = mCache.getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
			String serviceUrl = mCache.getProperty("skope_service_url") + "/user/" + username + "/favorites/" + params[0] + "/";

			
			// Create HTTP client
	        CustomHttpClient client = new CustomHttpClient(serviceUrl, OOIDetailMapActivity.this);
	        client.setUseBasicAuthentication(true);
	        client.setUsernamePassword(username, password);
	        
	        // Send HTTP request to web service
	        try {
	            client.execute(RequestMethod.POST);
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
				// Call back OK
				mListener.onFavoriteUpdateDone(true, "");
				break;
			case 0:
				// No server response
				Log.e(SkopeApplication.LOG_TAG, "Connection failed");
			case HttpStatus.SC_UNAUTHORIZED:
			case HttpStatus.SC_REQUEST_TIMEOUT:
			case HttpStatus.SC_BAD_GATEWAY:
			case HttpStatus.SC_GATEWAY_TIMEOUT:
			case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			case HttpStatus.SC_BAD_REQUEST:
				Log.e(SkopeApplication.LOG_TAG, "Failed to add favorite: " + client.getErrorMessage());
				// Call back failed
				mListener.onFavoriteUpdateDone(false, "Failed to add favorite");
			}
			
	   }
		
		public void setListener(OnFavoriteUpdateListener listener) {
			mListener = listener;
		}

	}
	
	protected class UserFavoriteDelete extends AsyncTask<String, Void, CustomHttpClient> {
		OnFavoriteUpdateListener mListener;
		
		@Override
		protected void onPreExecute() {
			if (mListener != null) {
				mListener.onFavoriteUpdateStart();
			}
		}

		@Override
		protected CustomHttpClient doInBackground(String... params) {
			String username = mCache.getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
			String password = mCache.getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
			String serviceUrl = mCache.getProperty("skope_service_url") + "/user/" + username + "/favorites/" + params[0] + "/";

			
			// Create HTTP client
	        CustomHttpClient client = new CustomHttpClient(serviceUrl, OOIDetailMapActivity.this);
	        client.setUseBasicAuthentication(true);
	        client.setUsernamePassword(username, password);
	        
	        // Send HTTP request to web service
	        try {
	            client.execute(RequestMethod.DELETE);
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
			case HttpStatus.SC_NO_CONTENT:
				// Call back OK
				if (mListener != null) {
					mListener.onFavoriteUpdateDone(true, "");
				}
				break;
			case 0:
				// No server response
				Log.e(SkopeApplication.LOG_TAG, "Connection failed");
			case HttpStatus.SC_UNAUTHORIZED:
			case HttpStatus.SC_REQUEST_TIMEOUT:
			case HttpStatus.SC_BAD_GATEWAY:
			case HttpStatus.SC_GATEWAY_TIMEOUT:
			case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			case HttpStatus.SC_BAD_REQUEST:
				Log.e(SkopeApplication.LOG_TAG, "Failed to delete favorite: " + client.getErrorMessage());
				// Call back failed
				if (mListener != null) {
					mListener.onFavoriteUpdateDone(false, "Failed to delete favorite");
				}
			}
			
	   }
		
		public void setListener(OnFavoriteUpdateListener listener) {
			mListener = listener;
		}

	}
	
    public static class ViewHolder {
		public TextView nameText;
		public TextView distanceText;
		public TextView lastUpdateText;
		public ImageView icon;

	}
    
	private OnItemClickListener mOOISelectListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> a, View v, int position, long id) {
			// Check if selected ooi is current user
			ObjectOfInterest ooi = mFavoritesListAdapter.getItem(position);
			Intent i = new Intent();
			if (ooi.getUserName().equals(getCache().getUser().getUserName())) {
				// Current user, redirect to profile
				Bundle bundle = new Bundle();
		        bundle.putInt("TAB", MainTabActivity.TAB_PROFILE);
		        i.putExtras(bundle);i.setClassName("com.skope.skope",
						"com.skope.skope.ui.MainTabActivity");
			} else {
				// Redirect to list activity
		        Bundle bundle = new Bundle();
		        bundle.putParcelable("USER", ooi);
		        i.putExtras(bundle);
				i.setClassName("com.skope.skope",
						"com.skope.skope.ui.OOIDetailMapActivity");
			}
			startActivity(i);
		}
	};	

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
                
                //if (holder.distanceText != null) {
                //	holder.distanceText.setText("Distance: " + String.valueOf(ooi.createLabelDistance())
                //			+ " - " + ooi.createLabelTimePassedSinceLastUpdate());
                //}
                
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
