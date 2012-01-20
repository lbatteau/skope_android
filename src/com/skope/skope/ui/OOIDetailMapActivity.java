package com.skope.skope.ui;

import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Gallery;
import android.widget.SlidingDrawer;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.skope.skope.R;
import com.skope.skope.application.ObjectOfInterest;
import com.skope.skope.maps.OOIOverlay;
import com.skope.skope.util.Type;

public class OOIDetailMapActivity extends OOIMapActivity {
	private ObjectOfInterest mSelectedOOI;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    // Fill out the information on the screen and update the map.
	    // TODO: Note that the methods initializing the map view are also 
	    // called by the super class, so these are called twice. 
	    // Needs refactoring.
	    update();
	    
	    // Open map drawer
	    SlidingDrawer mapDrawer = (SlidingDrawer) findViewById(R.id.mapSlidingDrawer);
    	// Check if present; no drawer in landscape mode
	    if (mapDrawer != null) {
		    mapDrawer.open();
	    }
	    
	    // Back button
	    View backButton = findViewById(R.id.detail_back_button);
	    backButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	    
	    // Store selected OOI
	    mSelectedOOI = getCache().getObjectOfInterestList().getSelectedOOI();
	    
	    
	}

	@Override
	protected void onResume() {
		super.onResume();
		update();
	}

	/**
	 * Retrieves the currently selected object of interest, updates the
	 * information in the view, and calls the necessary methods to update the
	 * map view.
	 */
	protected void update() {
		ObjectOfInterest selectedOOI = getCache().getObjectOfInterestList()
				.getSelectedOOI();

		selectedOOI.createUserProfile(this);
		
		TextView status = (TextView) findViewById(R.id.status);
		status.setText(selectedOOI.createLabelStatus());

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
		ObjectOfInterest selectedObjectOfInterest = getCache()
				.getObjectOfInterestList().getSelectedOOI();

		// Clear current overlays
		mMapOverlays = mMapView.getOverlays();

		// Clear ooi overlay
		if (mMapOverlays.size() > 1) {
			mMapOverlays.remove(1);
		}

		LayerDrawable marker = (LayerDrawable) getResources().getDrawable(
				R.drawable.marker);

		OOIOverlay ooiOverlay = new OOIOverlay(marker, this);
		ooiOverlay.addOverlay(createOverlay(selectedObjectOfInterest));
		mMapOverlays.add(1, ooiOverlay);

		MapController mapController = mMapView.getController();

		int userLatitude = (int) (getCache().getCurrentLocation().getLatitude() * 1E6);
		int userLongitude = (int) (getCache().getCurrentLocation()
				.getLongitude() * 1E6);
		int ooiLatitude = (int) (selectedObjectOfInterest.getLocation()
				.getLatitude() * 1E6);
		int ooiLongitude = (int) (selectedObjectOfInterest.getLocation()
				.getLongitude() * 1E6);

		mapController.animateTo(new GeoPoint((userLatitude + ooiLatitude) / 2,
				(userLongitude + ooiLongitude) / 2));

		mapController.zoomToSpan(Math.abs(userLatitude - ooiLatitude),
				Math.abs(userLongitude - ooiLongitude));

	}

}
