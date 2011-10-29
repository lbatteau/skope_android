package com.skope.skope.maps;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class OOIOverlayItem extends OverlayItem {
	protected boolean mIsCluster;

	public OOIOverlayItem(GeoPoint point, String title, String snippet) {
		super(point, title, snippet);
		mIsCluster = false;
	}

	public boolean isCluster() {
		return mIsCluster;
	}

	public void setIsCluster(boolean isCluster) {
		this.mIsCluster = isCluster;
	}
	

}
