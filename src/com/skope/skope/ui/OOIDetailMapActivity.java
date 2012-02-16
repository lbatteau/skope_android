package com.skope.skope.ui;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SlidingDrawer;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.ObjectOfInterest;
import com.skope.skope.application.ObjectOfInterestList;
import com.skope.skope.application.UserPhoto;
import com.skope.skope.maps.OOIOverlay;
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
	private View mUserProfileMain, mUserProfile, mUserPhotoLayout;
    private LayoutInflater mInflater;
    private Gallery mUserPhotoGallery;    
	private UserPhotoAdapter mUserPhotoAdapter;
	private ArrayList<UserPhoto> mUserPhotoList;
	private ProgressBar mPhotosProgressBar;	 
	private TextView mPhotosLabel;
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
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
		 * The user profile is actually an expandable list view, with the
		 * first item containing the main info like name, status, profile
		 * picture etc. This item has no children, so shouldn't expand.
		 * The rest of the list consists of photos, favorites, etc.
		 */
	    mListAdapter = new UserProfileExpandableListAdapter();
	    ExpandableListView expandableList = (ExpandableListView)findViewById(R.id.expandable_list);
	    expandableList.setAdapter(mListAdapter);
	    		
	    /*
	     * Set up navigation buttons. Transparent left and right arrows 
	     * appear when the user touches the screen. On a 'fling' gesture to the
	     * left or right, the previous or next user profile is loaded.
	     */
	    
	    // Store handles to navigation buttons
	    mNavigationHandleRight = findViewById(R.id.nav_right);
	    mNavigationHandleLeft = findViewById(R.id.nav_left);
	    
	    // Make invisible
	    mNavigationHandleLeft.setVisibility(View.INVISIBLE);
		mNavigationHandleRight.setVisibility(View.INVISIBLE);
	    
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
	    view.setOnTouchListener(mGestureListener);
	    
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
	        	startActivity(i);
			}
		});
	    
	    // Photos progress bar
		mPhotosProgressBar = (ProgressBar) mUserPhotoLayout.findViewById(R.id.user_photo_progress_bar);
		// Photos label
		mPhotosLabel = (TextView) mUserPhotoLayout.findViewById(R.id.user_photo_label);
	    
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
	
	/**
	 * Retrieves the currently selected object of interest, updates the
	 * information in the view, and calls the necessary methods to update the
	 * map view.
	 */
	protected void update() {
		ObjectOfInterest selectedOOI = getCache().getObjectOfInterestList()
				.getSelectedOOI();

		selectedOOI.createUserProfile(mUserProfileMain, mInflater);
		
		// Empty current photo list
		Cache.USER_PHOTOS.clear();
		// Request new read
		Bundle bundle = new Bundle();
        bundle.putString("USERNAME", getCache().getObjectOfInterestList()
				.getSelectedOOI().getUserEmail());
        getServiceQueue().postToService(Type.READ_USER_PHOTOS, bundle);
		
        initializeMapView();
		populateItemizedOverlays();
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
		ObjectOfInterest selectedObjectOfInterest = getCache()
				.getObjectOfInterestList().getSelectedOOI();

		// Clear current overlays
		mMapOverlays = mMapView.getOverlays();

		// Clear ooi overlay
		if (mMapOverlays.size() > 1) {
			mMapOverlays.remove(1);
		}

		LayerDrawable marker = (LayerDrawable) getResources().getDrawable(
				R.drawable.marker);

		OOIOverlay ooiOverlay = new OOIOverlay(marker, this);
		ooiOverlay.addOverlay(createOverlay(selectedObjectOfInterest));
		mMapOverlays.add(1, ooiOverlay);

		MapController mapController = mMapView.getController();

		int userLatitude = (int) (getCache().getCurrentLocation().getLatitude() * 1E6);
		int userLongitude = (int) (getCache().getCurrentLocation()
				.getLongitude() * 1E6);
		int ooiLatitude = (int) (selectedObjectOfInterest.getLocation()
				.getLatitude() * 1E6);
		int ooiLongitude = (int) (selectedObjectOfInterest.getLocation()
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
}
