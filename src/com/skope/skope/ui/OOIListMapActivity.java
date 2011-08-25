package com.skope.skope.ui;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.ServiceQueue;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.UiQueue;
import com.skope.skope.utils.Type;

public class OOIListMapActivity extends MapActivity {

	/** Pointer to the ServiceQueue. **/
	private ServiceQueue mServiceQueue;
	/** Pointer to the Application Cache. **/
	private Cache mCache;
	/** Pointer to the UiQueue. **/
	private UiQueue mUiQueue;
	/***
	 * Used by processMessage() to temporally store a Bundle object so it can be
	 * used in the onCreateDialog() method.
	 */
	private Bundle mDialogBundle;

	OOIItemizedOverlay mItemizedOverlay;
	List<Overlay> mMapOverlays;

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
	    setContentView(R.layout.map);

	    MapView mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
	    
	    mMapOverlays = mapView.getOverlays();
	    
        MapController mapController = mapView.getController();
        Location location = mCache.getCurrentLocation();
 
        GeoPoint center = new GeoPoint((int) (location.getLatitude() * 1E6),
        							   (int) (location.getLongitude() * 1E6));
 
        mapController.setCenter(center);
        
        // Determine correct zoom level to include all oois
        ObjectOfInterest farthestOOI = mCache.getObjectOfInterestList().determineFarthestOOI();
        
        // Create point in Google Maps format, but a little bit farther away, 
        // so the icon is also visible.  
        GeoPoint farthestPoint = new GeoPoint((int) (farthestOOI.getLocation().getLatitude() * 1E6 * 1.005),
        		        				      (int) (farthestOOI.getLocation().getLongitude() * 1E6 * 1.005)) ;
        // Set zoom level by passing the longest span:
        // The difference between our location and the farthest OOI
        mapController.zoomToSpan(Math.abs(farthestPoint.getLatitudeE6() - center.getLatitudeE6()), 
        		                 Math.abs(farthestPoint.getLongitudeE6() - center.getLongitudeE6())); 
        
        populateItemizedOverlays();

        mapView.invalidate();
        
	}	/***
	 * Subscribe the Activity to the UiQueue.
	 */
	@Override
	protected void onResume() {
		mUiQueue.subscribe(mHandler);
		super.onResume();
		populateItemizedOverlays();
	}

	/***
	 * Unsubscribe the Activity from the UiQueue.
	 */
	@Override
	protected void onPause() {
		mUiQueue.unsubscribe(mHandler);
		super.onPause();
	}

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

		default:
			// Do nothing.
			break;
		}
	}

	private void populateItemizedOverlays() {
		// Clear current overlays
		mMapOverlays.clear();

		ObjectOfInterestList ooiList = getCache().getObjectOfInterestList();
		for (ObjectOfInterest ooi : ooiList) {
			// Drawable drawable =
			// this.getResources().getDrawable(R.drawable.icon);
			Drawable drawable = new BitmapDrawable(ooi.getThumbnail());
			mItemizedOverlay = new OOIItemizedOverlay(drawable, this);

			GeoPoint point = new GeoPoint((int) (ooi.getLocation()
					.getLatitude() * 1e6), (int) (ooi.getLocation()
					.getLongitude() * 1e6));
			OverlayItem overlayitem = new OverlayItem(point, ooi.getUserName(),
					ooi.getUserEmail());
			mItemizedOverlay.addOverlay(overlayitem);

			mMapOverlays.add(mItemizedOverlay);
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
