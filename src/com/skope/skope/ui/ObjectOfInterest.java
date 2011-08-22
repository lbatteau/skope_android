package com.skope.skope.ui;

import java.sql.Timestamp;

import android.graphics.Bitmap;
import android.location.Location;

public class ObjectOfInterest {
	public static final int SEX_MALE = 0;
	public static final int SEX_FEMALE = 1;
	
	private String userName;
	private String userEmail;
	private int age;
	private String status;
	private int sex;
	private Bitmap thumbnail;
	private Location location;
	private Timestamp locationTimestamp;
	private float distance;
	
	/**
	 * 
	 * @return
	 */
	public String createReadableDistance() {
		int distanceReadable;
		String unit;
		
		if (this.distance > 1000) {
			distanceReadable = (int) this.distance/1000;
			unit = "km";
		} else {
			distanceReadable = (int) this.distance;
			unit = "m";
		}
		
		return "" + String.valueOf(distanceReadable) + " " + unit; 
	}
	
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public Bitmap getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(Bitmap thumbnail) {
		this.thumbnail = thumbnail;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getSex() {
		return sex;
	}

	public void setSex(int sex) {
		this.sex = sex;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}

	public Timestamp getLocationTimestamp() {
		return locationTimestamp;
	}

	public void setLocationTimestamp(Timestamp locationTimestamp) {
		this.locationTimestamp = locationTimestamp;
	}
	
}
