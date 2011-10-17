package com.skope.skope.application;

import java.sql.Timestamp;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.skope.skope.http.BMPFromURL;

import android.graphics.Bitmap;
import android.location.Location;

public class User {

	protected String mUsername;
	protected String mUserEmail;
	protected String mFirstName;
	protected String mLastName;
	protected int mAge;
	protected String mStatus;
	protected int mSex;
	protected String mThumbnail;
	protected Location mLocation;
	protected Timestamp mLocationTimestamp;
	
	public User(JSONObject jsonObject) throws JSONException {
		this.setUserName(jsonObject.getJSONObject("user").getString("username"));
		this.setUserEmail(jsonObject.getJSONObject("user").getString("email"));
		this.setFirstName(jsonObject.getJSONObject("user").getString("first_name"));
		this.setLastName(jsonObject.getJSONObject("user").getString("last_name"));
		this.setThumbnail(jsonObject.getString("thumbnail"));
		// Set mLocation
		// Parse mLocation in WKT (well known text) format, e.g. "POINT (52.2000000000000028 4.7999999999999998)"
		String[] tokens = jsonObject.getString("location").split("[ ()]");
		Location location = new Location("SKOPE_SERVICE");
		location.setLatitude(Double.parseDouble(tokens[3]));
		location.setLongitude(Double.parseDouble(tokens[2]));
		this.setLocation(location);
		// Set mLocation timestamp
		this.setLocationTimestamp(Timestamp.valueOf(jsonObject.getString("location_timestamp")));
	}
	
	/**
	 * Creates a bitmap image from the mThumbnail URL
	 */
	public Bitmap createThumbnail(String mediaURL) {
		return new BMPFromURL(mediaURL + this.mThumbnail).getMyBitmap();
	}
	
	/**
	 * Creates a label based on the user's name:
	 * <ul><li>If the user's first and/or last name is filled "mFirstName mLastName"</li>
	 *     <li>Else "username"</li>
	 */
	public String createName() {
		if ((mFirstName != null && !"".equals(mFirstName))
			|| (mLastName != null && !"".equals(mLastName))) {
			return mFirstName + " " + mLastName;
		} else {
			return mUsername;
		}
	}

	/**
	 * Creates a description of the time passed since the last mLocation update. 
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
		return mUsername;
	}

	public void setUserName(String userName) {
		this.mUsername = userName;
	}

	public String getUserEmail() {
		return mUserEmail;
	}

	public void setUserEmail(String userEmail) {
		this.mUserEmail = userEmail;
	}

	public String getThumbnail() {
		return mThumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.mThumbnail = thumbnail;
	}

	public int getAge() {
		return mAge;
	}

	public void setAge(int age) {
		this.mAge = age;
	}

	public String getStatus() {
		return mStatus;
	}

	public void setStatus(String status) {
		this.mStatus = status;
	}

	public int getSex() {
		return mSex;
	}

	public void setSex(int sex) {
		this.mSex = sex;
	}

	public Location getLocation() {
		return mLocation;
	}

	public void setLocation(Location location) {
		this.mLocation = location;
	}

	public Timestamp getLocationTimestamp() {
		return mLocationTimestamp;
	}

	public void setLocationTimestamp(Timestamp locationTimestamp) {
		this.mLocationTimestamp = locationTimestamp;
	}

	public String getFirstName() {
		return mFirstName;
	}

	public void setFirstName(String firstName) {
		this.mFirstName = firstName;
	}

	public String getLastName() {
		return mLastName;
	}

	public void setLastName(String lastName) {
		this.mLastName = lastName;
	}

}