package nl.skope.android.ui;

import java.util.ArrayList;

import nl.skope.android.application.MapOverlayClusterer;
import nl.skope.android.application.ObjectOfInterest;
import nl.skope.android.application.ObjectOfInterestList;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.maps.OOIOverlay;
import nl.skope.android.maps.SkopeMapView;
import nl.skope.android.util.Type;

import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import nl.skope.android.R;

public class OOIListMapActivity extends OOIMapActivity {
	private ArrayList<ArrayList<ObjectOfInterest>> mClusters;
	private Gallery mGallery;
	private ProfilePictureAdapter mImageAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	    // Set up the gallery
		mGallery = (Gallery) findViewById(R.id.gallery);
		mGallery.setUnselectedAlpha(0.5f);
		mGallery.setSpacing(5);
		mImageAdapter = new ProfilePictureAdapter(this, R.id.gallery, getCache().getObjectOfInterestList());
	    mGallery.setAdapter(mImageAdapter);
	    
	    
	    // When the user selects a mThumbnail in the gallery, update the view
	    mGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView parent, View view,
					int position, long id) {
				getCache().getObjectOfInterestList().setSelectedPosition(position);
				ObjectOfInterest ooi = getCache().getObjectOfInterestList().getSelectedOOI();
				mMapView.getController().animateTo(
						new GeoPoint((int) (ooi.getLocation().getLatitude() * 1E6),
	            					 (int) (ooi.getLocation().getLongitude() * 1E6)));
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
								
			}
		});		
	
	}
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
        
        if (farthestOOI != null) {
            // Create point in Google Maps format, but a little bit farther away, 
            // because the upper part of the map is behind tabs.  
            GeoPoint farthestPoint = new GeoPoint((int) (farthestOOI.getLocation().getLatitude() * 1E6 * 1.005),
            		        				      (int) (farthestOOI.getLocation().getLongitude() * 1E6 * 1.005)) ;
            // Set zoom level by passing the longest span:
            // The difference between our mLocation and the farthest OOI
            mapController.zoomToSpan(Math.abs(farthestPoint.getLatitudeE6() - center.getLatitudeE6()), 
            		                 Math.abs(farthestPoint.getLongitudeE6() - center.getLongitudeE6()));
        }

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
	public void post(final Type type, final Bundle bundle) {
		super.post(type, bundle);

		switch (type) {
		case FIND_OBJECTS_OF_INTEREST_FINISHED:
			super.post(type, bundle);
			mImageAdapter.notifyDataSetChanged();
			break;
		}
	}

	@Override
	protected void populateItemizedOverlays() {
		mMapOverlays = mMapView.getOverlays();
	    
		// Clear ooi overlay
		if (mMapOverlays.size() > 1) {
			mMapOverlays.remove(1);
		}
        
		// Add objects of interest
		LayerDrawable marker = (LayerDrawable) getResources().getDrawable(R.drawable.marker);
		ObjectOfInterestList ooiList = getCache().getObjectOfInterestList();
		mClusters = MapOverlayClusterer.cluster(ooiList, 100, mMapView.getZoomLevel());
        
		//itemizedOverlay.addOverlay(overlayitem);
		OOIOverlay ooiOverlay = new OOIOverlay(marker, this);
		for (ArrayList<ObjectOfInterest> cluster: mClusters) {
			// Check length of cluster
			if (cluster.size() > 1) {
				// Multiple objects in cluster
				if (mMapView.getZoomLevel() == mMapView.getMaxZoomLevel()) {
					// Max zoom level, add all objects in cluster
					for (ObjectOfInterest ooi: cluster) {
						ooiOverlay.addOverlay(createOverlay(ooi));
					}
				} else {
					// Create cluster overlay
					//ooiPin = createClusteredPin(cluster, cluster.size());
					ooiOverlay.addOverlay(createClusterOverlay(cluster));
				}
			} else {
				// One object in cluster, create regular overlay
				ooiOverlay.addOverlay(createOverlay(cluster.get(0)));	
			}
		}
		mMapOverlays.add(1, ooiOverlay);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.signout:
	    	getCache().setUser(null);
	    	String logoutURL = getCache().getProperty("skope_service_url") + "/logout/";
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
