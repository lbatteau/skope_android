package com.skope.skope.ui;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.skope.skope.R;

public class OOIDetailMapActivity extends OOIMapActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ObjectOfInterest selectedObjectOfInterest = getCache().getSelectedObjectOfInterest(); 
		TextView userNameText = (TextView) findViewById(R.id.username_text);
		userNameText.setText(selectedObjectOfInterest.getUserName());
        TextView lastUpdateText = (TextView) findViewById(R.id.last_update_text);
        ImageView icon = (ImageView) findViewById(R.id.icon);
        icon.setImageBitmap(selectedObjectOfInterest.getThumbnail());
	}

	@Override
	protected void setContentView() {
		setContentView(R.layout.detail);
	}

	@Override
	protected void initializeMapView() {
		super.initializeMapView();
		
		MapController mapController = mMapView.getController();
        
        ObjectOfInterest selectedObjectOfInterest = mCache.getSelectedObjectOfInterest();
 
        GeoPoint selectedOOIPoint = new GeoPoint((int) (selectedObjectOfInterest.getLocation().getLatitude() * 1E6),
        							   (int) (selectedObjectOfInterest.getLocation().getLongitude() * 1E6));
 
        mapController.setCenter(selectedOOIPoint);
        
        //mapController.setZoom(15);
        
        /*Location location = mCache.getCurrentLocation();
        
        GeoPoint center = new GeoPoint((int) (location.getLatitude() * 1E6),
        							   (int) (location.getLongitude() * 1E6));
        
        // Set zoom level by passing the difference between the selected OOI and our location
        mapController.zoomToSpan(Math.abs(center.getLatitudeE6() - selectedOOIPoint.getLatitudeE6()), 
        		                 Math.abs(center.getLongitudeE6() - selectedOOIPoint.getLongitudeE6()));*/
		
        mMapView.invalidate();
	}	
	
	@Override
	protected void populateItemizedOverlays() {
		// Clear current overlays
		mMapOverlays = mMapView.getOverlays();
	    mMapOverlays.clear();
		ObjectOfInterest selectedObjectOfInterest = mCache.getSelectedObjectOfInterest();
		Drawable drawable = new BitmapDrawable(selectedObjectOfInterest.getThumbnail());
		mItemizedOverlay = new OOIItemizedOverlay(drawable, this);
		GeoPoint point = new GeoPoint((int) (selectedObjectOfInterest.getLocation().getLatitude() * 1e6), 
				                      (int) (selectedObjectOfInterest.getLocation().getLongitude() * 1e6));
		OverlayItem overlayitem = new OverlayItem(point, selectedObjectOfInterest.getUserName(),
				"Last update: " + selectedObjectOfInterest.createLabelTimePassedSinceLastUpdate());
		mItemizedOverlay.addOverlay(overlayitem);

		mMapOverlays.add(mItemizedOverlay);
	}
	
}
