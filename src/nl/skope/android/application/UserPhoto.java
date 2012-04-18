package nl.skope.android.application;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;

import nl.skope.android.http.CustomHttpClient;
import nl.skope.android.http.CustomHttpClient.FlushedInputStream;
import nl.skope.android.http.CustomHttpClient.RequestMethod;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;


public class UserPhoto {
	private static final String TAG = UserPhoto.class.getSimpleName();
	public static final int USER_PHOTO_MAX_PIXELS = 640000;

	
	private Cache mCache;
	
	protected String mUsername;
	protected int mId;
	protected String mPhotoURL;
	protected String mThumbnailURL;
	protected WeakReference<Bitmap> mPhoto;
	protected WeakReference<Bitmap> mThumbnail;
	protected Location mLocation;
	protected Timestamp mLocationTimestamp;
	
	public UserPhoto() {
		// Empty shell is used in user profile as 'add photo' button
	}
	
	public UserPhoto(JSONObject jsonObject, Cache cache) throws JSONException {
		mCache = cache;
		
		JSONObject user = jsonObject.getJSONObject("user");
		if (!user.isNull("username")) {
			this.setUsername(user.getString("username"));
		}
				
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
		mCache.resetPurgeTimer();
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
	
	public void delete(OnImageDeleteListener listener) {
		UserPhotoDeleter deleter = new UserPhotoDeleter();
		deleter.setOnImageDeleteListener(listener);
		deleter.execute();
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
				mPhoto = new WeakReference<Bitmap>(bitmap);
			}
		}

		if (mPhoto != null) {
			return mPhoto.get();
		} else {
			return null;
		}		
	}

	public void setPhoto(WeakReference<Bitmap> photo) {
		this.mPhoto = photo;
	}

	public Bitmap getThumbnail() {
		if (mThumbnail == null) {
			// Not loaded, check cache
			Bitmap bitmap = mCache.getBitmapFromCache(mThumbnailURL);
			if (bitmap != null) {
				mCache.resetPurgeTimer();
				mThumbnail = new WeakReference<Bitmap>(bitmap);
			}
		}

		if (mThumbnail != null) {
			return mThumbnail.get();
		} else {
			return null;
		}
	}

	public void setThumbnail(WeakReference<Bitmap> thumbnail) {
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
		return mId;
	}

	public void setId(int id) {
		this.mId = id;
	}	

	public interface OnImageLoadListener {
		public void onImageLoadStart();
		public void onImageLoaded(Bitmap image);
	}
	
	public interface OnImageDeleteListener {
		public void onImageDeleteStart();
		public void onImageDeleted(boolean isSuccess, String message);
	}
	
	protected class UserThumbnailLoader extends AsyncTask<String, Void, Bitmap> {
		
		OnImageLoadListener mListener;
		
		@Override
		protected void onPreExecute() {
			mListener.onImageLoadStart();			
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			URL url = null;
			
			try {
				url = new URL(params[0]);
			} catch (MalformedURLException error) {
				Log.e(TAG, error.toString());
				return null;
			}
			
			try {
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setDoInput(true);
				connection.setUseCaches(true);
				connection.connect();
				FlushedInputStream input = new FlushedInputStream(connection.getInputStream());
				return BitmapFactory.decodeStream(input);
			} catch (IOException e) {
				Log.e(TAG, e.toString());
			}
			
			return null;
		}
		
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				// Set the user's profile picture
				setThumbnail(new WeakReference<Bitmap>(bitmap));
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
				Log.e(TAG, error.toString());
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
				Log.e(TAG, e1.toString());
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				// Set the user's profile picture
				setPhoto(new WeakReference<Bitmap>(bitmap));
				// Cache bitmap
				//mCache.addBitmapToCache(mPhotoURL, bitmap);
			}
			// Call back
			mListener.onImageLoaded(bitmap);
	   }
	}
	
	protected class UserPhotoDeleter extends AsyncTask<Void, Void, CustomHttpClient> {
		
		OnImageDeleteListener mListener;
		
		@Override
		protected void onPreExecute() {
			mListener.onImageDeleteStart();			
		}

		@Override
		protected CustomHttpClient doInBackground(Void... params) {
			int userId = mCache.getUser().getId();
			String username = mCache.getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
			String password = mCache.getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
			String absoluteUrl = mCache.getProperty("skope_service_url") + "/user/" + userId + "/photos/" + UserPhoto.this.mId + "/";
			
			// Create HTTP client
	        CustomHttpClient client = new CustomHttpClient(absoluteUrl);
	        client.setUseBasicAuthentication(true);
	        client.setUsernamePassword(username, password);
	        
	        // Send HTTP request to web service
	        try {
	            client.execute(RequestMethod.DELETE);
	        } catch (Exception e) {
	        	// Most exceptions already handled by client
	            e.printStackTrace();
	        }

	        return client;
		}
		
		protected void onPostExecute(CustomHttpClient client) {
			// Check HTTP response code
	    	int httpResponseCode = client.getResponseCode();
	    	// Check for server response
	    	if (httpResponseCode == 0) {
	    		// No server response
	    		mListener.onImageDeleted(false, "Connection failed");
	    		return;
	    	} else {
	    		// Check for error
	    		if (httpResponseCode != HttpStatus.SC_NO_CONTENT) {
	    			// Server returned error code
			        switch(client.getResponseCode()) {
			        case HttpStatus.SC_UNAUTHORIZED:
			        	// Login not successful, authorization required 
			        	mListener.onImageDeleted(false, "Unauthorized");
			        	break;
			        case HttpStatus.SC_REQUEST_TIMEOUT:
			        case HttpStatus.SC_BAD_GATEWAY:
			        case HttpStatus.SC_GATEWAY_TIMEOUT:
			        	// Connection timeout
			        	mListener.onImageDeleted(false, "Gateway timeout");
			        	break;
			        case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			        	mListener.onImageDeleted(false, "Server error");
			        	break;
			        }
			        return;
	    		}
	    	}
	    	
	    	mListener.onImageDeleted(true, "");
			
	     }

		public void setOnImageDeleteListener(OnImageDeleteListener listener) {
			mListener = listener;
		}
	}

	public String getUsername() {
		return mUsername;
	}

	public void setUsername(String username) {
		this.mUsername = username;
	}
	
	
}
