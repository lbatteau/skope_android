package com.skope.skope.application;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

public class ObjectOfInterest extends User {
	public static final int SEX_MALE = 0;
	public static final int SEX_FEMALE = 1;
	
	private float distance;
	
	public ObjectOfInterest(JSONObject jsonObject, Cache cache) throws JSONException {
		super(jsonObject, cache);
	}
	
	/**
	 * 
	 * @return
	 */
	public String createLabelDistance() {
		int distanceReadable;
		String unit;
		
		if (this.distance > 1000) {
			distanceReadable = (int) this.distance/1000;
			unit = "km";
		} else {
			distanceReadable = (int) this.distance;
			unit = "m";
		}
		
		return "" + String.valueOf(distanceReadable) + unit;
	}
	
	public void setDistanceToLocation(Location location) {
		this.distance = location.distanceTo(this.mLocation);
	}
	
	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}
	
}
