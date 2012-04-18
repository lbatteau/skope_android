package nl.skope.android.ui;

import java.util.List;

import nl.skope.android.R;
import nl.skope.android.application.Cache;
import nl.skope.android.application.ObjectOfInterestList;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.User;
import nl.skope.android.application.User.OnImageLoadListener;
import nl.skope.android.http.CustomHttpClient;
import nl.skope.android.http.CustomHttpClient.RequestMethod;
import nl.skope.android.maps.SkopeMapView;
import nl.skope.android.maps.UserOverlay;
import nl.skope.android.util.Type;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MyLocationOverlay;

public class OOIDetailMapActivity extends OOIMapActivity {
	private static final String TAG = OOIDetailMapActivity.class.getSimpleName();
	
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    
    private static final int USERMENU_PHOTOS = 0;
    private static final int USERMENU_FAVORITES = 1;

    private GestureDetector mGestureDetector;
    private View.OnTouchListener mGestureListener;
    private int mOOIPosition;
    private View mNavigationHandleRight, mNavigationHandleLeft;
	private Animation mFadeInAnimation, mFadeOutAnimation;
	private SlidingDrawer mMapDrawer;
	private View mUserProfile, mUserPhotoLayout, mFavoritesLayout;
    private LayoutInflater mInflater;
	private TextView mPhotosLabel, mFavoritesLabel;
	private User mSelectedOOI;
	ObjectOfInterestList mFavoritesList;
	UserArrayAdapter mFavoritesListAdapter;
	private boolean mStopUpdatingMap = false;
	View mFavoriteIcon, mFavoriteButton;
	
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    mSelectedOOI = getIntent().getExtras().getParcelable(SkopeApplication.BUNDLEKEY_USER);

        mSelectedOOI.setCache(getCache());
	    
	    mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    
	    // Need this for passing on touch events
	    mUserProfile = findViewById(R.id.user_profile);

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
	    
	    mFavoriteButton = findViewById(R.id.favorite_button);
	    mFavoriteIcon = findViewById(R.id.favorite_icon);
    	
	    // Favorites button event
	    mFavoriteButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (getCache().getUser().getFavorites().contains(mSelectedOOI.getId())) {
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
							runner.setListener(new AsyncTaskListener() {
								@Override
								public void onTaskStart() {
								}
								
								@Override
								public void onTaskDone(boolean isSuccess, String message) {
									if (isSuccess) {
										getCache().getUser().getFavorites().remove(new Integer(mSelectedOOI.getId()));
										mFavoriteIcon.setBackgroundResource(R.drawable.detail_button_favorite_selector);
									} else {
										Toast.makeText(OOIDetailMapActivity.this, "Sorry, please try again", Toast.LENGTH_SHORT).show();
									}
								}
							});
							runner.execute(mSelectedOOI.getId());
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
							runner.setListener(new AsyncTaskListener() {
								@Override
								public void onTaskStart() {
								}
								
								@Override
								public void onTaskDone(boolean isSuccess, String message) {
									if (isSuccess) {
										getCache().getUser().getFavorites().add(mSelectedOOI.getId());
										mFavoriteIcon.setBackgroundResource(R.drawable.detail_button_favorite_active_selector);
									} else {
										Toast.makeText(OOIDetailMapActivity.this, "Sorry, please try again", Toast.LENGTH_SHORT).show();
									}
								}
							});
							runner.execute(mSelectedOOI.getId());
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
	     * Chat button
	     */
	    final View chatButton = findViewById(R.id.chat_button);
	    chatButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Redirect to chat activity
		        Intent i = new Intent(OOIDetailMapActivity.this, OOIChatActivity.class);
	        	Bundle bundle = new Bundle();
		        bundle.putParcelable(SkopeApplication.BUNDLEKEY_USER, mSelectedOOI);
		        i.putExtras(bundle);
				startActivity(i);	
			}
		});

	    
	    // Report button
	    View reportButton = findViewById(R.id.report_button);
	    reportButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Report
				String title = getResources().getString(R.string.user_report_title);
				String messageFormat = getResources().getString(R.string.user_report_message);
				String message = String.format(messageFormat, mSelectedOOI.createName());
				final EditText description = new EditText(OOIDetailMapActivity.this);
				description.setLines(2);
				new AlertDialog.Builder(OOIDetailMapActivity.this)
		        .setTitle(title)
		        .setMessage(message)
		        .setView(description)
		        .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int whichButton) {
		            	UserReport runner = new UserReport();
						runner.setListener(new AsyncTaskListener() {
							@Override
							public void onTaskStart() {
							}
							
							@Override
							public void onTaskDone(boolean isSuccess, String message) {
								if (isSuccess) {
									Toast.makeText(OOIDetailMapActivity.this, "Report successfully sent", Toast.LENGTH_SHORT).show();
								} else {
									Toast.makeText(OOIDetailMapActivity.this, "Sorry, please try again", Toast.LENGTH_SHORT).show();
								}
							}
						});
						runner.execute(mSelectedOOI.getId(), description.getText().toString());
		            }
		        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int whichButton) {
		                // Do nothing.
		            }
		        }).show();
				
			}
		});
	    
	    // Inner menu items
	    String[] items = new String[] { "Photos", "Favorites" };
	    ArrayAdapter<String> adapter = new UserMenuArrayAdapter(this, 0, items);
	    ListView list = (ListView) findViewById(R.id.list);
	    list.setAdapter(adapter);
	    list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				switch (position) {
				case USERMENU_PHOTOS:
					// Go to photo grid
					// Redirect to photo activity
			        Intent photoIntent = new Intent(OOIDetailMapActivity.this, UserPhotoGridActivity.class);
			        startActivity(photoIntent);
		        	break;
				case USERMENU_FAVORITES:
					// Go to favorites
					Intent favoritesIntent = new Intent(OOIDetailMapActivity.this, UserFavoritesActivity.class);
					favoritesIntent.putExtra(SkopeApplication.BUNDLEKEY_USERID, mSelectedOOI.getId());
			        startActivity(favoritesIntent);
			        break;
				}
				
			}
		});
	    		
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
	    
	    mOOIPosition = getCache().getObjectOfInterestList().getSelectedPosition();
	    
        
	}
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	Cache.USER_PHOTOS.clear();
    }

	@Override
	protected void onResume() {
		super.onResume();
		if (sanityCheck()) {
			update();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putParcelable(SkopeApplication.BUNDLEKEY_USER, mSelectedOOI);
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mSelectedOOI = savedInstanceState.getParcelable(SkopeApplication.BUNDLEKEY_USER);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.skope_menu_detail, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.signout:
	    	mServiceQueue.stopService();
	    	mCache.setUserSignedOut(true);
	    	String logoutURL = mCache.getProperty("skope_service_url") + "/logout/";
	    	String username = mCache.getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
	    	String password = mCache.getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
	    	new LogoutTask().execute(this, logoutURL, username, password);
            return true;
	    case R.id.refresh:
	    	mServiceQueue.postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
	    	return true;   	
	    case R.id.options:
	    	startActivity(new Intent(this, SkopePreferenceActivity.class));
	    default:
	        return super.onOptionsItemSelected(item);
	    }
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
        	i.setClassName("nl.skope.android",
        				   "nl.skope.android.ui.MainTabActivity");
        	startActivity(i);
        	finish();	
		}
		
	    // Check if selected user is a favorite of the current user
	    if (getCache().getUser().getFavorites().contains(mSelectedOOI.getId())) {
	    	// Favorite of the current user. Enable marker.
	    	mFavoriteIcon.setBackgroundResource(R.drawable.detail_button_favorite_active_selector);
	    } 
	    
		mSelectedOOI.createUserProfile(mUserProfile, mInflater);
		
		// Empty current photo list
		Cache.USER_PHOTOS.clear();
		// Read photos
		Bundle photosBundle = new Bundle();
        photosBundle.putInt(SkopeApplication.BUNDLEKEY_USERID, mSelectedOOI.getId());
        getServiceQueue().postToService(Type.READ_USER_PHOTOS, photosBundle);
        
        // Read favorites
        Bundle favoritesBundle = new Bundle();
        favoritesBundle.putInt(SkopeApplication.BUNDLEKEY_USERID, mSelectedOOI.getId());
		getServiceQueue().postToService(Type.READ_USER_FAVORITES, favoritesBundle);
		
        initializeMapView();
		populateItemizedOverlays();
		

	}
	
	protected void goToNext() {
		ObjectOfInterestList ooiList = getCache().getObjectOfInterestList();
		if (ooiList.size() <= mOOIPosition) {
			return;
		} else {
			ooiList.setSelectedPosition(++mOOIPosition);
			update();
		}		
	}
	
	protected void goToPrevious() {
		ObjectOfInterestList ooiList = getCache().getObjectOfInterestList();
		if (mOOIPosition == 0) {
			return;
		} else {
			ooiList.setSelectedPosition(--mOOIPosition);
			update();
		}		
	}

	@Override
	protected void setContentView() {
		setContentView(R.layout.detail);

	    // Open map drawer
	    mMapDrawer = (SlidingDrawer) findViewById(R.id.mapSlidingDrawer);
    	// Check if present; no drawer in landscape mode
	    if (mMapDrawer != null) {
	    	mMapDrawer.animateOpen();
	    }
	    
		// Overwrite map view
	    mMapView = new SkopeMapView(this, this.getString(R.string.maps_api_key));
	    mMapView.setEnabled(true);
	    mMapView.setClickable(true);
		mMyLocationOverlay = new MyLocationOverlay(this, mMapView);	
		mMyLocationOverlay.enableMyLocation();
		//mMyLocationOverlay.runOnFirstFix(new Runnable() { public void run() {
			//mMapView.getController().animateTo(mMyLocationOverlay.getMyLocation());
		//}});
		mMapView.getOverlays().add(0, mMyLocationOverlay);
	    FrameLayout mapContainer = (FrameLayout) mMapDrawer.findViewById(R.id.map_container);
	    mapContainer.addView(mMapView);
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

		UserOverlay ooiOverlay = new UserOverlay(marker, mMapView);
		ooiOverlay.addOverlay(createOverlay(mSelectedOOI));
		mMapOverlays.add(1, ooiOverlay);
		
		// Update map to span and center between user and ooi
		if (getCache().getCurrentLocation() != null) {
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

	public interface AsyncTaskListener {
		public void onTaskStart();
		public void onTaskDone(boolean isSuccess, String message);
	}
	
	protected class UserFavoritePost extends AsyncTask<Integer, Void, CustomHttpClient> {
		AsyncTaskListener mListener;
		
		@Override
		protected void onPreExecute() {
			mListener.onTaskStart();
		}

		@Override
		protected CustomHttpClient doInBackground(Integer... params) {
			int userId = mCache.getUser().getId();
			String username = mCache.getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
			String password = mCache.getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
			String serviceUrl = String.format("%s/user/%d/favorites/%d/", mCache.getProperty("skope_service_url"), userId, params[0]);
			
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
				mListener.onTaskDone(true, "");
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
				Log.e(TAG, "Failed to add favorite: " + client.getErrorMessage());
				// Call back failed
				mListener.onTaskDone(false, "Failed to add favorite");
			}
			
	   }
		
		public void setListener(AsyncTaskListener listener) {
			mListener = listener;
		}

	}
	
	protected class UserFavoriteDelete extends AsyncTask<Integer, Void, CustomHttpClient> {
		AsyncTaskListener mListener;
		
		@Override
		protected void onPreExecute() {
			if (mListener != null) {
				mListener.onTaskStart();
			}
		}

		@Override
		protected CustomHttpClient doInBackground(Integer... params) {
			int userId = mCache.getUser().getId();
			String username = mCache.getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
			String password = mCache.getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
			String serviceUrl = String.format("%s/user/%d/favorites/%d/", mCache.getProperty("skope_service_url"), userId, params[0]);

			
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
					mListener.onTaskDone(true, "");
				}
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
				Log.e(TAG, "Failed to delete favorite: " + client.getErrorMessage());
				// Call back failed
				if (mListener != null) {
					mListener.onTaskDone(false, "Failed to delete favorite");
				}
			}
			
	   }
		
		public void setListener(AsyncTaskListener listener) {
			mListener = listener;
		}

	}
	
	protected class UserReport extends AsyncTask<Object, Void, CustomHttpClient> {
		AsyncTaskListener mListener;
		
		@Override
		protected void onPreExecute() {
			mListener.onTaskStart();
		}

		@Override
		protected CustomHttpClient doInBackground(Object... params) {
			int userId = mCache.getUser().getId();
			String username = mCache.getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
			String password = mCache.getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
			String serviceUrl = String.format("%s/user/%d/report/%d/", mCache.getProperty("skope_service_url"), userId, params[0]);
			
			// Create HTTP client
	        CustomHttpClient client = new CustomHttpClient(serviceUrl, OOIDetailMapActivity.this);
	        client.setUseBasicAuthentication(true);
	        client.setUsernamePassword(username, password);
	        client.addParam("message", (String) params[1]);
	        
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
				mListener.onTaskDone(true, "");
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
				Log.e(TAG, "Failed to report user: " + client.getErrorMessage());
				// Call back failed
				mListener.onTaskDone(false, "Failed to report user");
			}
			
	   }
		
		public void setListener(AsyncTaskListener listener) {
			mListener = listener;
		}

	}
	
    public static class ViewHolder {
		public TextView nameText;
		public TextView distanceText;
		public TextView lastUpdateText;
		public ImageView icon;

	}
    
	private class UserMenuArrayAdapter extends ArrayAdapter<String> {
		String[] mItems;
		int[] mIcons = new int[] { R.drawable.expand_icon_photos, R.drawable.expand_icon_favorites };

		public UserMenuArrayAdapter(Context context, int textViewResourceId,
				String[] objects) {
			super(context, textViewResourceId, objects);
			mItems = objects;
		}
		
		@Override
        public View getView(int position, View convertView, final ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
                convertView = (View) mInflater.inflate(R.layout.simple_item, null);
                holder = new ViewHolder();
                holder.nameText = (TextView) convertView.findViewById(R.id.name_text);
                convertView.setTag(holder);
            } else {
            	holder = (ViewHolder) convertView.getTag();
            }             
            
            holder.nameText.setText(mItems[position]);
            holder.nameText.setCompoundDrawablePadding(5);
            holder.nameText.setCompoundDrawablesWithIntrinsicBounds(mIcons[position], 0, 0, 0);
            
            return convertView;
        }
		
	}
	
    private class UserArrayAdapter extends ArrayAdapter<User> {
    	private LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	
    	OnImageLoadListener mProfilePictureListener = new OnImageLoadListener() {
			@Override
			public void onImageLoaded(Bitmap thumbnail) {
				UserArrayAdapter.this.notifyDataSetChanged();
			}
		};
    	
    	public UserArrayAdapter(Context context, int textViewResourceId,
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
            
            User user = getItem(position);
            
            if (user != null) {
                if (holder.nameText != null) {
                	holder.nameText.setText(user.createName());                            
                }
                
                if (holder.distanceText != null) {
                	// For favorites don't display distance, not really useful
                	holder.distanceText.setText("");
                }
                
                if (holder.icon != null) {
                	holder.icon.setImageBitmap(user.getProfilePicture()); // even when null, otherwise previous values remain
            		// Lazy loading
                	if (user.getProfilePicture() == null) {
                		user.loadProfilePicture(mProfilePictureListener);
                	}
                }
            }
            
            return convertView;
        }
    }	

}
