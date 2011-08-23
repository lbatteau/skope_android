package com.skope.skope.ui;

import java.sql.Timestamp;
import java.util.Date;

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
	
	/**
	 * Creates a description of the time passed since the last location update. 
	 * @return The time passed since the last update. The method adjusts the 
	 * time unit to keep things readable:
	 *          <ul><li>If less than 10 seconds "Just now"</li>
	 *              <li>If more than 10 and less than 60 seconds "x second(s)"</li>
	 *              <li>If more than 60 seconds and less than 60 minutes "x minute(s)"</li>
	 *              <li>If more than 60 minutes and less than 24 hours "x hour(s)"</li>
	 *              <li>If more than 24 hours "x day(s)"</li></ul>
	 */
	public String createLabelTimePassedSinceLastUpdate() {
		Date today = new Date();
    	
		// Determine the time delta between now and the last update.
    	long delta = (today.getTime() - this.getLocationTimestamp().getTime())/1000;
    	String label = "";
    	String unit = "";
    	
    	// Determine unit
    	
    	// Just now? Server time could be slightly in the future 
    	if (delta <= 10) {
    		label = "Just now";
    	} else {
    		// Construct readable time delta, 
    		// e.g. 24 seconds ago, or 5 days ago
    		
    		// Less than sixty seconds?
        	if (delta < 60) {
        		unit = "second";
        		if (delta != 1) {
        			unit += "s";
        		}
        	}
            
        	// More than sixty seconds?
        	if (delta > 60) {
        		// Change unit to minutes
        		delta = delta/60;
        		unit = "minute";
        		if (delta > 1) {
        			unit += "s";
        		}
        		
        		// More than sixty minutes?
            	if (delta > 60) {
            		// Change unit to hours
            		delta = delta/60;
            		unit = "hour";
            		if (delta > 1) {
            			unit += "s";
            		}
            		
            		// More than twenty four hours?
                	if (delta > 24) {
                		// Change unit to days
                		delta = delta/24;
                		unit = "day";
                		if (delta > 1) {
                			unit += "s";
                		}
                	}
            	}
        	}
        	
        	label = String.valueOf(delta) + " " + unit + " ago";
    	}
    	
    	return label;
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
