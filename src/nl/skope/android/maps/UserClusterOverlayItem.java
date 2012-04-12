package nl.skope.android.maps;

import nl.skope.android.application.ObjectOfInterestList;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class UserClusterOverlayItem extends OverlayItem {
	ObjectOfInterestList mOOIList;

	public UserClusterOverlayItem(ObjectOfInterestList oois, GeoPoint point) {
		super(point, "Cluster", String.valueOf(oois.size()));
		this.mOOIList = oois;
	}

	public ObjectOfInterestList getObjectOfInterestList() {
		return this.mOOIList;
	}
	

}
