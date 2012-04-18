package nl.skope.android.ui;

import java.util.List;

import nl.skope.android.R;
import nl.skope.android.application.Cache;
import nl.skope.android.application.ObjectOfInterestList;
import nl.skope.android.application.ServiceQueue;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.UiQueue;
import nl.skope.android.application.User;
import nl.skope.android.application.User.OnImageLoadListener;
import nl.skope.android.maps.SkopeMapView;
import nl.skope.android.maps.UserClusterOverlayItem;
import nl.skope.android.maps.UserOverlayItem;
import nl.skope.android.service.LocationService;
import nl.skope.android.util.Type;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

public abstract class OOIMapActivity extends MapActivity {
	
	private static final String TAG = OOIMapActivity.class.getSimpleName();

	/** Pointer to the ServiceQueue. **/
	protected ServiceQueue mServiceQueue;
	/** Pointer to the Application Cache. **/
	protected Cache mCache;
	/** Pointer to the UiQueue. **/
	protected UiQueue mUiQueue;
	
	protected MyLocationOverlay mMyLocationOverlay;
	/***
	 * Used by processMessage() to temporally store a Bundle object so it can be
	 * used in the onCreateDialog() method.
	 */
	private Bundle mDialogBundle;
	protected SkopeMapView mMapView;
	protected List<Overlay> mMapOverlays;
	
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
					Log.e(TAG, "BaseActivity.Handler."
							+ "handleMessage() ERROR");
				}
				/** TEST: Try and catch an error condition **/
	
				processMessage(message);
			}
		};

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    SkopeApplication application = (SkopeApplication) getApplication();
	    mServiceQueue = application.getServiceQueue();
	    mUiQueue = application.getUiQueue();
	    mCache = application.getCache();
	
	    super.onCreate(savedInstanceState);
	    
	    setContentView();

	    initializeMapView();
	}
	
	protected void setContentView() {
		setContentView(R.layout.map_gallery);

		mMapView = (SkopeMapView) findViewById(R.id.mapview);
	    
		mMyLocationOverlay = new MyLocationOverlay(this, mMapView);	
		mMyLocationOverlay.enableMyLocation();
		//mMyLocationOverlay.runOnFirstFix(new Runnable() { public void run() {
			//mMapView.getController().animateTo(mMyLocationOverlay.getMyLocation());
		//}});
		mMapView.getOverlays().add(0, mMyLocationOverlay);
 
	}

	/**
	 * Creates the overlay pin used to display users on the map
	 * @param user 
	 * @return
	 */
	protected UserOverlayItem createOverlay(User user) {
		// Create the drawable containing a profile picture 
        final LayerDrawable marker = (LayerDrawable) getResources().getDrawable(R.drawable.marker);
        final int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
	    final int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());

		GeoPoint point = new GeoPoint((int) (user.getLocation()
				.getLatitude() * 1e6), (int) (user.getLocation()
				.getLongitude() * 1e6));
		
		final UserOverlayItem overlayItem = new UserOverlayItem(user, point);
		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
		overlayItem.setMarker(marker);
		
	    Bitmap profilePicture = user.getProfilePicture();
	    if (profilePicture != null) {
	    	Drawable profilePictureDrawable = new BitmapDrawable(Bitmap.createScaledBitmap(profilePicture, width, height, false));
	    	marker.setDrawableByLayerId(R.id.marker_thumbnail, profilePictureDrawable);
	    } else {
	    	// Lazy loading
	    	//marker.setDrawableByLayerId(R.id.marker_thumbnail, new BitmapDrawable());
	    	user.loadProfilePicture(new OnImageLoadListener() {
				
				@Override
				public void onImageLoaded(Bitmap profilePicture) {
					Drawable profilePictureDrawable = new BitmapDrawable(Bitmap.createScaledBitmap(profilePicture, width, height, false));
					//profilePictureDrawable.setBounds(0, 0, width, height);
					marker.setDrawableByLayerId(R.id.marker_thumbnail, profilePictureDrawable);
					overlayItem.setMarker(marker);
				}
			});
	    }
		
		return overlayItem;
	}
	
	/**
	 * Creates the overlay pin used to display users on the map
	 * @param user 
	 * @return
	 */
	protected UserClusterOverlayItem createClusterOverlay(ObjectOfInterestList users) {
	    // Get the overlay item
	    Drawable cluster = getResources().getDrawable(R.drawable.cluster);
		
	    int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cluster.getIntrinsicWidth(), getResources().getDisplayMetrics());
	    int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cluster.getIntrinsicHeight(), getResources().getDisplayMetrics());
		
		cluster.setBounds(0, 0, width, height);

		// Create a mutable bitmap
        Bitmap bmOverlay = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);

        Canvas canvas = new Canvas(bmOverlay);
        // Draw cluster circle
		cluster.draw(canvas);
		
		Paint paint = new Paint();
		paint.setFlags(Paint.ANTI_ALIAS_FLAG|Paint.FAKE_BOLD_TEXT_FLAG);
		paint.setTextAlign(Align.CENTER);
		// Add text
		paint.setColor(Color.BLACK);
		
		String clusterSize = String.valueOf(users.size());
        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, getResources().getDisplayMetrics());
        float textPosX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cluster.getIntrinsicWidth()/2, getResources().getDisplayMetrics());
		float textPosY = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cluster.getIntrinsicHeight()/2 + 7, getResources().getDisplayMetrics());
		paint.setTextSize(textSize);
		canvas.drawText(clusterSize, textPosX, textPosY, paint);
		
		// Center cluster
		int minLat = Integer.MAX_VALUE;
	    int minLong = Integer.MAX_VALUE;
	    int maxLat = Integer.MIN_VALUE;
	    int maxLong = Integer.MIN_VALUE;

	    for (User user: users) {
	        minLat = (int) Math.min(user.getLocation().getLatitude() * 1e6, minLat);
	        minLong = (int) Math.min(user.getLocation().getLongitude() * 1e6, minLong);
	        maxLat = (int) Math.max(user.getLocation().getLatitude() * 1e6, maxLat);
	        maxLong = (int) Math.max(user.getLocation().getLongitude() * 1e6, maxLong);
	    }
		
		GeoPoint point = new GeoPoint((maxLat + minLat) / 2, (maxLong + minLong) / 2);
		
		UserClusterOverlayItem overlayItem = new UserClusterOverlayItem(users, point);
		
		overlayItem.setMarker(new BitmapDrawable(bmOverlay));
		
		return overlayItem;
	}
	
	/***
	 * Subscribe the Activity to the UiQueue.
	 */
	@Override
	protected void onResume() {
		mUiQueue.subscribe(mHandler);
		super.onResume();
		mMyLocationOverlay.enableMyLocation();
		getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
	}

	/***
	 * Unsubscribe the Activity from the UiQueue.
	 */
	@Override
	protected void onPause() {
		mUiQueue.unsubscribe(mHandler);
		mMyLocationOverlay.disableMyLocation();
		super.onPause();
	}
	
	protected abstract void populateItemizedOverlays();
	
	protected void initializeMapView() {
		mMapView.setBuiltInZoomControls(true);
	}

	/**
	 * Checks if vital information concerning the currently 
	 * logged in user is present in the cache:
	 * <ul>
	 * <li>Whether the user object is present at all (cache could have been 
	 * recreated after activity restart)</li>
	 * <li>Whether the user is signed in</li>
	 * </ul>
	 * @return true if user information is present. If not, false, and an 
	 * intent for LoginActivity is started. The current activity is finished.
	 */
	protected boolean sanityCheck() {
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
		case FIND_OBJECTS_OF_INTEREST_FINISHED:
			populateItemizedOverlays();
			break;
        case UNDETERMINED_LOCATION:
        	String message;
        	if (getCache().isLocationProviderAvailable()) {
        		message = getResources().getString(R.string.no_location_message);
        	} else {
        		message = getResources().getString(R.string.no_location_message_disabled);
        	}
        	
        	new AlertDialog.Builder(this)
	        .setTitle(getResources().getString(R.string.no_location_title))
	        .setMessage(message)
	        .setPositiveButton(getResources().getString(R.string.no_location_ok), new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	// Redirect the user to the system mLocation settings menu
            		Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            		startActivity(intent);
            		dialog.dismiss();
	            }
	        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	                // Do nothing.
	            }
	        }).show();
        	break;
        	
	
		default:
			// Do nothing.
			break;
		}
	}

	/**
	 * Return the Application ServiceQueue.
	 * 
	 * @return ServiceQueue
	 */
	public final ServiceQueue getServiceQueue() {
		return mServiceQueue;
	}

	/**
	 * Return the Application Cache.
	 * 
	 * @return Cache
	 */
	public final Cache getCache() {
		return mCache;
	}

}