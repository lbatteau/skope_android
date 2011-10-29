package com.skope.skope.ui;

import android.graphics.drawable.LayerDrawable;
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
import com.skope.skope.R;
import com.skope.skope.application.ObjectOfInterest;
import com.skope.skope.maps.OOIOverlay;

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
        icon.setImageBitmap(selectedObjectOfInterest.getThumbnail());
        
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
		
		mMapView.invalidate();
	}	
	
	@Override
	protected void populateItemizedOverlays() {
		ObjectOfInterest selectedObjectOfInterest = getCache().getObjectOfInterestList().getSelectedOOI();
		
		// Clear current overlays
		mMapOverlays = mMapView.getOverlays();
	    mMapOverlays.clear();
	    LayerDrawable marker = (LayerDrawable) getResources().getDrawable(R.drawable.marker);
		OOIOverlay userOverlay = new OOIOverlay(marker, this);
		userOverlay.addOverlay(createOverlay(getCache().getUser()));
		OOIOverlay ooiOverlay = new OOIOverlay(marker, this);
		ooiOverlay.addOverlay(createOverlay(selectedObjectOfInterest));
		mMapOverlays.add(userOverlay);
		mMapOverlays.add(ooiOverlay);
		
		MapController mapController = mMapView.getController();
        
		int userLatitude = (int) (getCache().getCurrentLocation().getLatitude() * 1E6);
		int userLongitude = (int) (getCache().getCurrentLocation().getLongitude() * 1E6);
		int ooiLatitude = (int) (selectedObjectOfInterest.getLocation().getLatitude() * 1E6);
		int ooiLongitude = (int) (selectedObjectOfInterest.getLocation().getLongitude() * 1E6);
		
		mapController.zoomToSpan(Math.abs(userLatitude - ooiLatitude),
								 Math.abs(userLongitude - ooiLongitude));
		
		mapController.animateTo(new GeoPoint((userLatitude + ooiLatitude) / 2,
		        							 (userLongitude + ooiLongitude) / 2));

		
	}
	
}
