package nl.skope.android.ui;

import java.util.ArrayList;

import nl.skope.android.R;
import nl.skope.android.application.MapOverlayClusterer;
import nl.skope.android.application.ObjectOfInterestList;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.User;
import nl.skope.android.maps.SkopeMapView;
import nl.skope.android.maps.UserOverlay;
import nl.skope.android.maps.UserOverlayItem;
import nl.skope.android.util.Type;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Gallery;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;

public class OOIListMapActivity extends OOIMapActivity {
	private static final String TAG = OOIListMapActivity.class.getSimpleName();
	
	private ArrayList<ObjectOfInterestList> mClusters;
	private Gallery mGallery;
	private ProfilePictureAdapter mImageAdapter;
	private UserOverlay<UserOverlayItem> mOOIOverlay;
	
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
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				User user = (User) 
								((Gallery) parent).getItemAtPosition(position);
				getCache().getObjectOfInterestList().setSelectedPosition(position);
				mMapView.getController().animateTo(
						new GeoPoint((int) (user.getLocation().getLatitude() * 1E6),
	            					 (int) (user.getLocation().getLongitude() * 1E6)));
				mOOIOverlay.showBalloon(createOverlay(user));
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				mOOIOverlay.hideBalloon();
			}
		});
	    
	    LayerDrawable marker = (LayerDrawable) getResources().getDrawable(R.drawable.marker);
		mOOIOverlay = new UserOverlay<UserOverlayItem>(marker, mMapView);
		// Calculate offset (to place them on top of thumbnails) for overlay balloons
		int offset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35, getResources().getDisplayMetrics());
		mOOIOverlay.setBalloonBottomOffset(offset);
		
		mMapOverlays = mMapView.getOverlays();
		mMapOverlays.add(1, mOOIOverlay);
	    
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (sanityCheck()) {
			// Check if current location is known
			if (getCache().getCurrentLocation() != null) {
				// Location is known, center it on map
				animateMapToLocation(getCache().getCurrentLocation());
			}
		}
	}
	
	@Override 
	protected void onResume() {
		super.onResume();
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
        User farthestOOI = mCache.getObjectOfInterestList().determineFarthestOOI();
        
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
	
	protected void animateMapToLocation(Location location) {
		// Check if user location is set
		mMapView.getController().animateTo(
					new GeoPoint((int) (location.getLatitude() * 1E6),
	        					 (int) (location.getLongitude() * 1E6)));		
	}
	
	public void updateGallery(User user) {
		mGallery.setSelection(mImageAdapter.getPosition(user));		
	}
	
	public void zoomToCluster(int clusterIndex) {
		int minLat = Integer.MAX_VALUE;
	    int minLong = Integer.MAX_VALUE;
	    int maxLat = Integer.MIN_VALUE;
	    int maxLong = Integer.MIN_VALUE;

	    for (User ooi: mClusters.get(clusterIndex)) {
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
			populateItemizedOverlays();
			break;
		}
	}

	@Override
	protected void populateItemizedOverlays() {
		// Add objects of interest
		ObjectOfInterestList ooiList = getCache().getObjectOfInterestList();
		mClusters = MapOverlayClusterer.cluster(ooiList, 100, mMapView.getZoomLevel());
        
		mOOIOverlay.clear();		
		
		for (ObjectOfInterestList cluster: mClusters) {
			// Check length of cluster
			if (cluster.size() > 1) {
				// Multiple objects in cluster
				if (mMapView.getZoomLevel() == mMapView.getMaxZoomLevel()) {
					// Max zoom level, add all objects in cluster
					for (User ooi: cluster) {
						mOOIOverlay.addOverlay(createOverlay(ooi));
					}
				} else {
					// Create cluster overlay
					//ooiPin = createClusteredPin(cluster, cluster.size());
					mOOIOverlay.addOverlay(createClusterOverlay(cluster));
				}
			} else {
				// One object in cluster, create regular overlay
				mOOIOverlay.addOverlay(createOverlay(cluster.get(0)));
			}
		}
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
