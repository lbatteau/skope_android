package com.skope.skope.application;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

public class User {
	
	/** The thumbnail URL is relative, so we need properties to create bitmap */
	private String mMediaURL;
	private DateFormat mDateFormat = DateFormat.getDateInstance();
	
	protected String mUsername;
	protected String mUserEmail;
	protected String mFirstName;
	protected String mLastName;
	protected Date mDateOfBirth;
	protected String mStatus;
	protected int mSex;
	protected String mThumbnailURL;
	protected Bitmap mThumbnail;
	protected Location mLocation;
	protected Timestamp mLocationTimestamp;
	protected String mRelationship;
	protected String mHomeTown;
	protected String mWork;
	protected String mEducation;
	protected String mInterests;
	protected boolean mIsFirstTime;
	
	protected HashMap<String, Bitmap> mImageCache;
	protected boolean mHasNoThumbnail; 
	
	public interface OnThumbnailLoadListener {
		public void onThumbnailLoaded();
	}
	
	protected class ThumbnailLoader extends AsyncTask<String, Void, Bitmap> {
		private static final int THUMBNAIL_WIDTH = 100;
		private static final int THUMBNAIL_HEIGHT = 100;
		
		OnThumbnailLoadListener mListener;

		@Override
		protected Bitmap doInBackground(String... params) {
			URL url = null;
			
			try {
				url = new URL(params[0]);
			} catch (MalformedURLException error) {
				Log.e(SkopeApplication.LOG_TAG, error.toString());
				mHasNoThumbnail = true;
				return null;
			}

			try {
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setDoInput(true);
				connection.setUseCaches(true);
				connection.connect();
				InputStream input = connection.getInputStream();
				Bitmap bitmap = BitmapFactory.decodeStream(input);
				return Bitmap.createScaledBitmap(bitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true);
			} catch (IOException e) {
				Log.e(SkopeApplication.LOG_TAG, e.toString());
				mHasNoThumbnail = true;
			}
			
			return null;
		}
		
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				// Set the user's thumbnail
				setThumbnail(bitmap);
				// Cache bitmap
				if (mImageCache != null) {
					mImageCache.put(mThumbnailURL, bitmap);
				}
				// Call back
				mListener.onThumbnailLoaded();
			}
	     }

		public void setOnThumbnailLoadListener(OnThumbnailLoadListener listener) {
			mListener = listener;
		}
	}
	
	public User(JSONObject jsonObject) throws JSONException {
		JSONObject user = jsonObject.getJSONObject("user");
		if (!user.isNull("username")) {
			this.setUserName(user.getString("username"));
		}
		if (!user.isNull("email")) {
			this.setUserEmail(user.getString("email"));
		}
		
		if (!user.isNull("first_name")) {
			this.setFirstName(user.getString("first_name"));
		}
		
		if (!user.isNull("last_name")) {
			this.setLastName(user.getString("last_name"));
		}
		
		if (!jsonObject.isNull("thumbnail_url")) {
			this.setThumbnailURL(jsonObject.getString("thumbnail_url"));
		}
		
		// Check if user has uploaded a thumbnail at all
		if (this.getThumbnailURL() == null || this.getThumbnailURL().equals("")) {
			// Nope
			this.setHasNoThumbnail(true);
		}
		

		if (!jsonObject.isNull("status_message")) {
			this.setStatus(jsonObject.getString("status_message"));
		}
		
		if (!jsonObject.isNull("relationship")) {
			this.setRelationship(jsonObject.getString("relationship"));
		}
		
		if (!jsonObject.isNull("home_town")) {
			this.setHomeTown(jsonObject.getString("home_town"));
		}
		
		if (!jsonObject.isNull("work")) {
			this.setWork(jsonObject.getString("work"));
		}
		
		if (!jsonObject.isNull("education")) {
			this.setEducation(jsonObject.getString("education"));
		}
		
		if (!jsonObject.isNull("interests")) {
			this.setInterests(jsonObject.getString("interests"));
		}
		
		if (!jsonObject.isNull("date_of_birth")) {
			try {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				this.setDateOfBirth(df.parse(jsonObject.getString("date_of_birth")));
			} catch(ParseException e) {
				Log.w(SkopeApplication.LOG_TAG, "Invalid date format: " + e);
			}
		}
		
		// Set mLocation
		// Parse mLocation in WKT (well known text) format, e.g. "POINT (52.2000000000000028 4.7999999999999998)"
		if (!jsonObject.isNull("location")) {
			String[] tokens = jsonObject.getString("location").split("[ ()]");
			Location location = new Location("SKOPE_SERVICE");
			location.setLatitude(Double.parseDouble(tokens[3]));
			location.setLongitude(Double.parseDouble(tokens[2]));
			this.setLocation(location);
		}
		
		// Set mLocation timestamp
		if (!jsonObject.isNull("location_timestamp")) {
			this.setLocationTimestamp(Timestamp.valueOf(jsonObject.getString("location_timestamp")));
		}
		
		// First time
		if (!jsonObject.isNull("is_first_time")) {
			this.setIsFirstTime(jsonObject.getBoolean("is_first_time"));
		}
	}
	
	@Override
	public boolean equals(Object user) {
		return (this.mUsername.equals(((User) user).mUsername));		
	}

	/**
	 * Loads the actual thumbnail bitmap for this user.  
	 * 
	 * When a user is retrieved the first time, it only contains an URL 
	 * pointing to it's thumbnail. When a view needs to display the thumbnail 
	 * it can call this method to load the bitmap. By passing it a handler, 
	 * the view can update itself once the thumbnail is loaded.
	 * 
	 *    - If the thumbnail is already present, the method calls the callback 
	 * 		method directly.
	 *    - If not, it checks the image cache.
	 *    - If not in cache, it uses an AsyncTask that downloads the thumbnail 
	 *      and calls the callback when finished.
	 *      
	 * Note that the system cache is used to store downloaded images.
	 *  
	 * @param listener The callback method that is called when the thumbnail
	 * 				   is successfully loaded. If no thumbnail is present, this
	 * 				   method is never called.
	 */
	public void loadThumbnail(OnThumbnailLoadListener listener) {
		if (!this.mHasNoThumbnail) {
			// Check if thumbnail already loaded for this user
			if (this.mThumbnail != null) {
				listener.onThumbnailLoaded();
			} else {
				// Not loaded, check cache
				if (mImageCache != null) {
					Bitmap bitmap = mImageCache.get(this.mThumbnailURL);
					if (bitmap != null) {
				        setThumbnail(bitmap);
				        listener.onThumbnailLoaded();
				    }
				}

				ThumbnailLoader loader = new ThumbnailLoader();
				loader.setOnThumbnailLoadListener(listener);
				loader.execute(this.getThumbnailURL());
			}
		}
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
	
	public String createLabelStatus() {
		if(mStatus == null) {
			return "";
		} else {
			return "\"" + mStatus.trim() + "\"";
		}
	}
	
	public String createLabelDateOfBirth() {
		if (mDateOfBirth == null) {
			return "";
		} else {
			return mDateFormat.format(mDateOfBirth);
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
	
	public int determineAge() {
		if (this.mDateOfBirth == null) {
			return -1; 
		}
		
		GregorianCalendar cal = new GregorianCalendar();
        int y, m, d, a;         

        y = cal.get(Calendar.YEAR);
        m = cal.get(Calendar.MONTH);
        d = cal.get(Calendar.DAY_OF_MONTH);
        cal.setTime(mDateOfBirth);
        a = y - cal.get(Calendar.YEAR);
        if ((m < cal.get(Calendar.MONTH))
                        || ((m == cal.get(Calendar.MONTH)) && (d < cal
                                        .get(Calendar.DAY_OF_MONTH)))) {
                --a;
        }
        
        return a;
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

	public Bitmap getThumbnail() {
		if (mThumbnail == null && mHasNoThumbnail == false) {
			// Not loaded, check cache
			if (mImageCache != null) {
				Bitmap bitmap = mImageCache.get(this.mThumbnailURL);
				if (bitmap != null) {
			        setThumbnail(bitmap);
			    }
			}
		}
		return mThumbnail;
	}

	public void setThumbnail(Bitmap thumbnail) {
		this.mThumbnail = thumbnail;
	}

	public Date getDateOfBirth() {
		return mDateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.mDateOfBirth = dateOfBirth;
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

	public String getThumbnailURL() {
		return mThumbnailURL;
	}

	public void setThumbnailURL(String thumbnailURL) {
		this.mThumbnailURL = thumbnailURL;
	}

	public void setHasNoThumbnail(boolean hasNoThumbnail) {
		this.mHasNoThumbnail = hasNoThumbnail;
	}

	public boolean hasNoThumbnail() {
		return this.mHasNoThumbnail;
	}

	public String getRelationship() {
		return mRelationship;
	}

	public void setRelationship(String relationship) {
		this.mRelationship = relationship;
	}
	
	public String getHomeTown() {
		return mHomeTown;
	}

	public void setHomeTown(String homeTown) {
		this.mHomeTown = homeTown;
	}
	
	public String getWork() {
		return mWork;
	}

	public void setWork(String work) {
		this.mWork = work;
	}
	
	public String getEducation() {
		return mEducation;
	}

	public void setEducation(String education) {
		this.mEducation = education;
	}
	
	public void setImageCache(HashMap<String, Bitmap> imageCache) {
		this.mImageCache = imageCache;
	}

	public String getInterests() {
		return mInterests;
	}

	public void setInterests(String interests) {
		this.mInterests = interests;
	}

	public boolean isFirstTime() {
		return mIsFirstTime;
	}

	public void setIsFirstTime(boolean isFirstTime) {
		this.mIsFirstTime = isFirstTime;
	}

}