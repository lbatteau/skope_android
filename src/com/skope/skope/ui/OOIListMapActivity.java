package com.skope.skope.ui;

import java.util.ArrayList;

import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.skope.skope.R;
import com.skope.skope.application.MapOverlayClusterer;
import com.skope.skope.application.ObjectOfInterest;
import com.skope.skope.application.ObjectOfInterestList;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.maps.OOIOverlay;
import com.skope.skope.maps.SkopeMapView;
import com.skope.skope.utils.Type;

public class OOIListMapActivity extends OOIMapActivity {
	ArrayList<ArrayList<ObjectOfInterest>> mClusters;
	
	@Override
	protected void initializeMapView() {
		super.initializeMapView();
		
		MapController mapController = mMapView.getController();
		Location location = mCache.getCurrentLocation();
		
		if (location == null) {
			// Location currently unavailable
			mMapView.invalidate();
			return;
		}
 
        GeoPoint center = new GeoPoint((int) (location.getLatitude() * 1E6),
        							   (int) (location.getLongitude() * 1E6));
 
        mapController.setCenter(center);
        
        // Determine correct zoom level to include all oois
        ObjectOfInterest farthestOOI = mCache.getObjectOfInterestList().determineFarthestOOI();
        
        // Create point in Google Maps format, but a little bit farther away, 
        // because the upper part of the map is behind tabs.  
        GeoPoint farthestPoint = new GeoPoint((int) (farthestOOI.getLocation().getLatitude() * 1E6 * 1.005),
        		        				      (int) (farthestOOI.getLocation().getLongitude() * 1E6 * 1.005)) ;
        // Set zoom level by passing the longest span:
        // The difference between our mLocation and the farthest OOI
        mapController.zoomToSpan(Math.abs(farthestPoint.getLatitudeE6() - center.getLatitudeE6()), 
        		                 Math.abs(farthestPoint.getLongitudeE6() - center.getLongitudeE6()));
        
        // Add listener to update overlays when zooming in or out
        mMapView.setOnZoomListener(new SkopeMapView.OnZoomListener() {
			
			@Override
			public void onZoomChanged(int zoomLevel) {
				populateItemizedOverlays();
				mMapView.invalidate();				
			}
		});
        
        mMapView.invalidate();
	}
	
	public void zoomToCluster(int clusterIndex) {
		int minLat = Integer.MAX_VALUE;
	    int minLong = Integer.MAX_VALUE;
	    int maxLat = Integer.MIN_VALUE;
	    int maxLong = Integer.MIN_VALUE;

	    for (ObjectOfInterest ooi: mClusters.get(clusterIndex)) {
	        minLat = (int) Math.min(ooi.getLocation().getLatitude() * 1e6, minLat);
	        minLong = (int) Math.min(ooi.getLocation().getLongitude() * 1e6, minLong);
	        maxLat = (int) Math.max(ooi.getLocation().getLatitude() * 1e6, maxLat);
	        maxLong = (int) Math.max(ooi.getLocation().getLongitude() * 1e6, maxLong);
	    }
	    
		MapController mapController = mMapView.getController();
		
		mapController.animateTo(new GeoPoint((maxLat + minLat) / 2,
											 (maxLong + minLong) / 2));

		mapController.zoomToSpan(Math.abs(maxLat - minLat), 
				 Math.abs(maxLong - minLong));

	}
	
	@Override
	protected void populateItemizedOverlays() {
		mMapOverlays = mMapView.getOverlays();
	    
		// Clear current overlay
		mMapOverlays.clear();
        
        // Add current user
		LayerDrawable marker = (LayerDrawable) getResources().getDrawable(R.drawable.marker);
		OOIOverlay userOverlay = new OOIOverlay(marker, this);
		userOverlay.addOverlay(createOverlay(getCache().getUser()));
        mMapOverlays.add(userOverlay);
        
        // Add objects of interest
		ObjectOfInterestList ooiList = getCache().getObjectOfInterestList();
		mClusters = MapOverlayClusterer.cluster(ooiList, 20, mMapView.getZoomLevel());
        
		//itemizedOverlay.addOverlay(overlayitem);
		OOIOverlay ooiOverlay = new OOIOverlay(marker, this);
		for (ArrayList<ObjectOfInterest> cluster: mClusters) {
			// Check length of cluster
			if (cluster.size() > 1) {
				// Multiple objects in cluster, create cluster overlay
				//ooiPin = createClusteredPin(cluster, cluster.size());
				ooiOverlay.addOverlay(createClusterOverlay(cluster));
			} else {
				// One object in cluster, create regular overlay
				ooiOverlay.addOverlay(createOverlay(cluster.get(0)));	
			}
		}
		mMapOverlays.add(ooiOverlay);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.signout:
	    	getCache().setUser(null);
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
