package com.skope.skope.ui;

import java.util.ArrayList;
import java.util.List;

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
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.ObjectOfInterest;
import com.skope.skope.application.ServiceQueue;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.UiQueue;
import com.skope.skope.application.User;
import com.skope.skope.application.User.OnThumbnailLoadListener;
import com.skope.skope.maps.OOIOverlayItem;
import com.skope.skope.maps.SkopeMapView;
import com.skope.skope.util.Type;

public abstract class OOIMapActivity extends MapActivity {

	/** Pointer to the ServiceQueue. **/
	private ServiceQueue mServiceQueue;
	/** Pointer to the Application Cache. **/
	protected Cache mCache;
	/** Pointer to the UiQueue. **/
	private UiQueue mUiQueue;
	
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
					Log.e(SkopeApplication.LOG_TAG, "BaseActivity.Handler."
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
	    
	    mMapView = (SkopeMapView) findViewById(R.id.mapview);
	    
		mMyLocationOverlay = new MyLocationOverlay(this, mMapView);	
		mMyLocationOverlay.enableMyLocation();
		//mMyLocationOverlay.runOnFirstFix(new Runnable() { public void run() {
		//	mMapView.getController().animateTo(mMyLocationOverlay.getMyLocation());
		//}});
		mMapView.getOverlays().add(0, mMyLocationOverlay);
 
	    getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
	    
	    initializeMapView();		
	}
	
	protected void setContentView() {
		setContentView(R.layout.map);
	}

	/**
	 * Creates the overlay pin used to display users on the map
	 * @param user 
	 * @return
	 */
	protected OOIOverlayItem createOverlay(User user) {
		// Create the drawable containing a thumbnail 
        final LayerDrawable marker = (LayerDrawable) getResources().getDrawable(R.drawable.marker);
        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, marker.getIntrinsicWidth(), getResources().getDisplayMetrics());
	    int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, marker.getIntrinsicHeight(), getResources().getDisplayMetrics());
	    Bitmap thumbnail = user.getThumbnail();
	    if (thumbnail != null) {
	    	Drawable thumbnailDrawable = new BitmapDrawable(Bitmap.createScaledBitmap(thumbnail, width, height, true));
	    	marker.setDrawableByLayerId(R.id.marker_thumbnail, thumbnailDrawable);
	    } else {
	    	// Lazy loading
	    	marker.setDrawableByLayerId(R.id.marker_thumbnail, new BitmapDrawable());
	    	user.loadThumbnail(new OnThumbnailLoadListener() {
				
				@Override
				public void onThumbnailLoaded(Bitmap thumbnail) {
					mMapView.invalidateDrawable(marker);
					
				}
			});
	    }
		
		GeoPoint point = new GeoPoint((int) (user.getLocation()
				.getLatitude() * 1e6), (int) (user.getLocation()
				.getLongitude() * 1e6));
		
		OOIOverlayItem overlayItem = new OOIOverlayItem(point, user.getUserName(),
				"Last update: " + user.createLabelTimePassedSinceLastUpdate());
		
		overlayItem.setMarker(marker);
		
		return overlayItem;
	}
	
	/**
	 * Creates the overlay pin used to display users on the map
	 * @param user 
	 * @return
	 */
	protected OOIOverlayItem createClusterOverlay(ArrayList<ObjectOfInterest> oois) {
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
		
		String clusterSize = String.valueOf(oois.size());
        float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, getResources().getDisplayMetrics());
        float textPosX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cluster.getIntrinsicWidth()/2, getResources().getDisplayMetrics());
		float textPosY = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cluster.getIntrinsicHeight()/2 + 7, getResources().getDisplayMetrics());
		paint.setTextSize(textSize);
		canvas.drawText(clusterSize, textPosX, textPosY, paint);
		
		GeoPoint point = new GeoPoint((int) (oois.get(0).getLocation()
				.getLatitude() * 1e6), (int) (oois.get(0).getLocation()
				.getLongitude() * 1e6));
		
		OOIOverlayItem overlayItem = new OOIOverlayItem(point, "Cluster",
				String.valueOf(oois.size()));
		
		overlayItem.setIsCluster(true);
		
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
		populateItemizedOverlays();
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
        	Toast.makeText(this, "Location currently unavailable", Toast.LENGTH_LONG).show();
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