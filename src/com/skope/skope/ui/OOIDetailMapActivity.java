package com.skope.skope.ui;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.OverlayItem;
import com.skope.skope.R;
import com.skope.skope.application.ObjectOfInterest;

public class OOIDetailMapActivity extends OOIMapActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    // Fill out the information on the screen and update the map.
	    // TODO: Note that the methods initializing the map view are also 
	    // called by the super class, so these are called twice. 
	    // Needs refactoring.
	    update();
		
	    // Set up the gallery
		Gallery gallery = (Gallery) findViewById(R.id.gallery);
	    gallery.setAdapter(new ImageAdapter(this, getCache()));
	    gallery.setSelection(getCache().getObjectOfInterestList().getSelectedPosition());
	    
	    // When the user selects a thumbnail in the gallery, update the view
	    gallery.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView parent, View v, int position, long id) {
	            getCache().getObjectOfInterestList().setSelectedPosition(position);
	            update();
	        }
	    });
	    
	    // Also update the view when browsing through the gallery
	    gallery.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()) {
				// Only register a change of selection on release
				case MotionEvent.ACTION_UP:
					int position = ((Gallery) v).getSelectedItemPosition();
					getCache().getObjectOfInterestList().setSelectedPosition(position);
		            update();
				}
					
				return false;
			}
		});
	    
	}
	
	/**
	 * Retrieves the currently selected object of interest, updates the 
	 * information in the view, and calls the necessary methods to update
	 * the map view. 
	 */
	protected void update() {
		ObjectOfInterest selectedObjectOfInterest = getCache().getObjectOfInterestList().getSelectedOOI();
		TextView userNameText = (TextView) findViewById(R.id.username_text);
		userNameText.setText(selectedObjectOfInterest.createName());
        TextView lastUpdateText = (TextView) findViewById(R.id.last_update_text);
        ImageView icon = (ImageView) findViewById(R.id.icon);
        icon.setImageBitmap(selectedObjectOfInterest.createThumbnail(getCache().getProperty("media_url")));
        
        initializeMapView();
        populateItemizedOverlays();		
	}

	@Override
	protected void setContentView() {
		setContentView(R.layout.detail);
	}

	@Override
	protected void initializeMapView() {
		super.initializeMapView();
		
		ObjectOfInterest selectedObjectOfInterest = getCache().getObjectOfInterestList().getSelectedOOI();
		
		MapController mapController = mMapView.getController();
        
               GeoPoint selectedOOIPoint = new GeoPoint((int) (selectedObjectOfInterest.getLocation().getLatitude() * 1E6),
        							   (int) (selectedObjectOfInterest.getLocation().getLongitude() * 1E6));
 
        mapController.setCenter(selectedOOIPoint);
        
        mapController.setZoom(13);
        
        /*Location mLocation = mCache.getCurrentLocation();
        
        GeoPoint center = new GeoPoint((int) (mLocation.getLatitude() * 1E6),
        							   (int) (mLocation.getLongitude() * 1E6));
        
        // Set zoom level by passing the difference between the selected OOI and our mLocation
        mapController.zoomToSpan(Math.abs(center.getLatitudeE6() - selectedOOIPoint.getLatitudeE6()), 
        		                 Math.abs(center.getLongitudeE6() - selectedOOIPoint.getLongitudeE6()));*/
		
        mMapView.invalidate();
	}	
	
	@Override
	protected void populateItemizedOverlays() {
		ObjectOfInterest selectedObjectOfInterest = getCache().getObjectOfInterestList().getSelectedOOI();
		
		// Clear current overlays
		mMapOverlays = mMapView.getOverlays();
	    mMapOverlays.clear();
		Drawable drawable = new BitmapDrawable(selectedObjectOfInterest.createThumbnail(getCache().getProperty("media_url")));
		mItemizedOverlay = new OOIItemizedOverlay(drawable, this);
		GeoPoint point = new GeoPoint((int) (selectedObjectOfInterest.getLocation().getLatitude() * 1e6), 
				                      (int) (selectedObjectOfInterest.getLocation().getLongitude() * 1e6));
		OverlayItem overlayitem = new OverlayItem(point, selectedObjectOfInterest.getUserName(),
				"Last update: " + selectedObjectOfInterest.createLabelTimePassedSinceLastUpdate());
		mItemizedOverlay.addOverlay(overlayitem);

		mMapOverlays.add(mItemizedOverlay);
	}
	
}
