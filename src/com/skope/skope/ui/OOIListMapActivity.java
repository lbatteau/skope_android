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
        // The difference between our location and the farthest OOI
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
			Drawable drawable = new BitmapDrawable(ooi.getThumbnail());
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
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.skope_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.quit:
	    	getServiceQueue().stopService();
            this.finish();
            return true;
	    case R.id.refresh:
	    	getServiceQueue().postToService(Type.FIND_OBJECTS_OF_INTEREST, null);
	    	return true;   	
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
}
