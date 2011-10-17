package com.skope.skope.ui;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.skope.skope.R;
import com.skope.skope.application.ObjectOfInterest;
import com.skope.skope.application.ObjectOfInterestList;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.utils.Type;

public class OOIListMapActivity extends OOIMapActivity {

	@Override
	protected void initializeMapView() {
		super.initializeMapView();
		
		MapController mapController = mMapView.getController();
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
        // The difference between our mLocation and the farthest OOI
        mapController.zoomToSpan(Math.abs(farthestPoint.getLatitudeE6() - center.getLatitudeE6()), 
        		                 Math.abs(farthestPoint.getLongitudeE6() - center.getLongitudeE6()));
		
        mMapView.invalidate();
	}	
	
	@Override
	protected void populateItemizedOverlays() {
		mMapOverlays = mMapView.getOverlays();
	    
		// Clear current overlays
        mMapOverlays.clear();

		ObjectOfInterestList ooiList = getCache().getObjectOfInterestList();
		for (ObjectOfInterest ooi : ooiList) {
			// Drawable drawable =
			// this.getResources().getDrawable(R.drawable.icon);
			Drawable drawable = new BitmapDrawable(ooi.createThumbnail(getCache().getProperty("media_url")));
			mItemizedOverlay = new OOIItemizedOverlay(drawable, this);

			GeoPoint point = new GeoPoint((int) (ooi.getLocation()
					.getLatitude() * 1e6), (int) (ooi.getLocation()
					.getLongitude() * 1e6));
			OverlayItem overlayitem = new OverlayItem(point, ooi.getUserName(),
					"Last update: " + ooi.createLabelTimePassedSinceLastUpdate());
			mItemizedOverlay.addOverlay(overlayitem);

			mMapOverlays.add(mItemizedOverlay);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.signout:
	    	String logoutURL = getCache().getProperty("skope_logout_url");
	    	String username = getCache().getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
	    	String password = getCache().getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
	    	new LogoutTask().execute(this, logoutURL, username, password);
	    	getServiceQueue().stopService();
            return true;
	    case R.id.refresh:
	    	getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
	    	return true;   	
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
}
