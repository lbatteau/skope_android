package com.skope.skope.application;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

public class ObjectOfInterest extends User {
	public static final int SEX_MALE = 0;
	public static final int SEX_FEMALE = 1;
	
	private float distance;
	
	public ObjectOfInterest(Parcel in) {
		super(in);
	}

	public static final Parcelable.Creator<ObjectOfInterest> CREATOR = new Parcelable.Creator<ObjectOfInterest>() {
        public ObjectOfInterest createFromParcel(Parcel in) {
        return new ObjectOfInterest(in);
        }

        public ObjectOfInterest[] newArray(int size) {
        return new ObjectOfInterest[size];
        }
    };
    
    public ObjectOfInterest(JSONObject jsonObject) throws JSONException {
		super(jsonObject);
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
