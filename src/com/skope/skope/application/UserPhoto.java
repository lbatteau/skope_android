package com.skope.skope.application;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.skope.skope.http.CustomHttpClient.FlushedInputStream;

public class UserPhoto {
	private final static String LOG_TAG = "UserPhoto";
	
	private Cache mCache;
	
	protected int id;
	protected String mPhotoURL;
	protected String mThumbnailURL;
	protected Bitmap mPhoto;
	protected Bitmap mThumbnail;
	protected Location mLocation;
	protected Timestamp mLocationTimestamp;
	
	public UserPhoto(JSONObject jsonObject, Cache cache) throws JSONException {
		mCache = cache;
		
		this.setId(jsonObject.getInt("id"));
		
		if (!jsonObject.isNull("photo_url")) {
			this.setPhotoURL(jsonObject.getString("photo_url"));
		}
		
		if (!jsonObject.isNull("thumbnail_url")) {
			this.setThumbnailURL(jsonObject.getString("thumbnail_url"));
		}
		
		// Set mLocation
		// Parse mLocation in WKT (well known text) format, e.g. "POINT (52.2000000000000028 4.7999999999999998)"
		if (!jsonObject.isNull("location")) {
			String[] tokens = jsonObject.getString("location").split("[ ()]");
			Location location = new Location("API");
			location.setLatitude(Double.parseDouble(tokens[3]));
			location.setLongitude(Double.parseDouble(tokens[2]));
			this.setLocation(location);
		}
		
		// Set mLocation timestamp
		if (!jsonObject.isNull("location_timestamp")) {
			this.setLocationTimestamp(Timestamp.valueOf(jsonObject.getString("location_timestamp")));
		}
		
	}

	public void loadThumbnail(OnImageLoadListener listener) {
		UserThumbnailLoader loader = new UserThumbnailLoader();
		loader.setOnImageLoadListener(listener);
		loader.execute(getThumbnailURL());
	}
	
	public void loadPhoto(OnImageLoadListener listener) {
		UserPhotoLoader loader = new UserPhotoLoader();
		loader.setOnImageLoadListener(listener);
		loader.execute(this.getPhotoURL());
	}
	
	public void loadPhoto(OnImageLoadListener listener, int targetWidth, int targetHeight) {
		UserPhotoLoader loader = new UserPhotoLoader();
		loader.enableRescaling(targetWidth, targetHeight);
		loader.setOnImageLoadListener(listener);
		loader.execute(this.getPhotoURL());
	}
	
	public String getPhotoURL() {
		return mPhotoURL;
	}

	public void setPhotoURL(String photoURL) {
		this.mPhotoURL = photoURL;
	}

	public String getThumbnailURL() {
		return mThumbnailURL;
	}

	public void setThumbnailURL(String thumbnailURL) {
		this.mThumbnailURL = thumbnailURL;
	}

	public Bitmap getPhoto() {
		if (mPhoto == null) {
			// Not loaded, check cache
			Bitmap bitmap = mCache.getBitmapFromCache(mPhotoURL);
			if (bitmap != null) {
				mPhoto = bitmap;
			}
		}

		return mPhoto;
	}

	public void setPhoto(Bitmap photo) {
		this.mPhoto = photo;
	}

	public Bitmap getThumbnail() {
		if (mThumbnail == null) {
			// Not loaded, check cache
			Bitmap bitmap = mCache.getBitmapFromCache(mThumbnailURL);
			if (bitmap != null) {
				mThumbnail = bitmap;
			}
		}

		return mThumbnail;
	}

	public void setThumbnail(Bitmap thumbnail) {
		this.mThumbnail = thumbnail;
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

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}	

	public interface OnImageLoadListener {
		public void onImageLoadStart();
		public void onImageLoaded(Bitmap image);
	}
	
	protected class UserThumbnailLoader extends AsyncTask<String, Void, Bitmap> {
		
		OnImageLoadListener mListener;

		@Override
		protected Bitmap doInBackground(String... params) {
			URL url = null;
			
			try {
				url = new URL(params[0]);
			} catch (MalformedURLException error) {
				Log.e(SkopeApplication.LOG_TAG, error.toString());
				return null;
			}
			
			mListener.onImageLoadStart();

			try {
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setDoInput(true);
				connection.setUseCaches(true);
				connection.connect();
				FlushedInputStream input = new FlushedInputStream(connection.getInputStream());
				return BitmapFactory.decodeStream(input);
			} catch (IOException e) {
				Log.e(SkopeApplication.LOG_TAG, e.toString());
			}
			
			return null;
		}
		
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				// Set the user's profile picture
				setThumbnail(bitmap);
				// Cache bitmap
				mCache.addBitmapToCache(mThumbnailURL, bitmap);
				// Call back
				mListener.onImageLoaded(bitmap);
			}
	     }

		public void setOnImageLoadListener(OnImageLoadListener listener) {
			mListener = listener;
		}
	}
	
	protected class UserPhotoLoader extends UserThumbnailLoader {
		boolean doRescaling;
		private int mWidth, mHeight;
		
		public void enableRescaling(int width, int height) {
			doRescaling = true;
			mWidth = width;
			mHeight = height;
		}
		
		@Override
		protected Bitmap doInBackground(String... params) {
			URL url = null;
			
			try {
				url = new URL(params[0]);
			} catch (MalformedURLException error) {
				Log.e(SkopeApplication.LOG_TAG, error.toString());
				return null;
			}
			
			mListener.onImageLoadStart();

			HttpURLConnection connection;
			Options options = new BitmapFactory.Options();
			FlushedInputStream input;
			
			try {
				connection = (HttpURLConnection) url.openConnection();
				connection.connect();
				try {
					if (doRescaling) {
						// First retrieve image bounds
						options.inJustDecodeBounds = true;
						input = new FlushedInputStream(connection.getInputStream());
						BitmapFactory.decodeStream(input, null, options);
						
						Boolean scaleByHeight = Math.abs(options.outHeight - mHeight) >= Math.abs(options.outWidth - mWidth);

						if (options.outHeight * options.outWidth * 2 >= 200*100*2){
						    // Load, scaling to smallest power of 2 that'll get it <= desired dimensions
						    double sampleSize = scaleByHeight
						    ? options.outHeight / mHeight
						    : options.outWidth / mWidth;
						    options.inSampleSize =
						        (int)Math.pow(2d, Math.floor(
						        Math.log(sampleSize)/Math.log(2d)));
						}
						connection.disconnect();
						connection = (HttpURLConnection) url.openConnection();
						connection.connect();
					}
					
					// Do the actual decoding
					input = new FlushedInputStream(connection.getInputStream());
					options.inJustDecodeBounds = false;
					return BitmapFactory.decodeStream(input, null, options);

				} finally {
					connection.disconnect();
				}
			} catch (IOException e1) {
				// Error setting up URL connection
				Log.e(LOG_TAG, e1.toString());
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				// Set the user's profile picture
				setPhoto(bitmap);
				// Cache bitmap
				mCache.addBitmapToCache(mPhotoURL, bitmap);
			}
			// Call back
			mListener.onImageLoaded(bitmap);
	   }
	}	
}
