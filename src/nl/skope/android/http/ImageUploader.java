package nl.skope.android.http;

import java.io.FileNotFoundException;
import java.io.IOException;

import nl.skope.android.application.Cache;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.UserPhoto;
import nl.skope.android.http.CustomHttpClient.RequestMethod;

import org.apache.http.HttpStatus;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;


public class ImageUploader {
	private static final String TAG = ImageUploader.class.getSimpleName();
	
	Cache mCache;
	Context mContext;
	String mUsername, mPassword, mUserId;
	OnUploadListener mOnUploadListener;
	
	public interface OnUploadListener {
		public void onUploadStart();
		public void onUploadUpdate();
		public void onUploadComplete(boolean success, String errorMessage);
	}
	
	private class UploadRunner extends AsyncTask<Uri, Void, CustomHttpClient> {
		String mUrl, mUsername, mPassword, mField;
		
		public UploadRunner(String url, 
							String username, 
							String password,
							String field) {
			mUrl = url;
			mUsername = username;
			mPassword = password;
			mField = field;
		}
		
		@Override
		protected void onPreExecute() {
			if (mOnUploadListener != null) {
				mOnUploadListener.onUploadStart();
			}
		}
		
		@Override
		protected CustomHttpClient doInBackground(Uri... arg0) {
			// Create HTTP client
	        CustomHttpClient client = new CustomHttpClient(mUrl, mContext);
	        client.setUseBasicAuthentication(true);
	        client.setUsernamePassword(mUsername, mPassword);
	        
	        // Add image
	        client.addBitmapUri(mField, arg0[0]);
	         
	        // Send HTTP request to web service
	        try {
	            client.execute(RequestMethod.POST);
	        } catch (Exception e) {
	        	// Most exceptions already handled by client
	            e.printStackTrace();
	        }
	        
	        return client;	        
		}
		
		@Override
		protected void onPostExecute(CustomHttpClient client) {
			// Check for server response
			switch (client.getResponseCode()) {
			case HttpStatus.SC_OK:
				if (mOnUploadListener != null) {
					mOnUploadListener.onUploadComplete(true, "");
				}
				break;
			case 0:
				// No server response
				Log.e(TAG, "Connection failed");
			case HttpStatus.SC_UNAUTHORIZED:
			case HttpStatus.SC_REQUEST_TIMEOUT:
			case HttpStatus.SC_BAD_GATEWAY:
			case HttpStatus.SC_GATEWAY_TIMEOUT:
			case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			case HttpStatus.SC_BAD_REQUEST:
				Log.e(TAG, "Failed to upload image: " + client.getErrorMessage());
				if (mOnUploadListener != null) {
					mOnUploadListener.onUploadComplete(false, client.getErrorMessage());
				}
			default:
				break;
			}
		}
	}
	
	public ImageUploader(Context context, Cache cache) {
		mContext = context;
		mCache = cache;
		mUsername = mCache.getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
		mPassword = mCache.getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
		mUserId = String.valueOf(mCache.getUser().getId());
	}
	
	/**
	 * Uploads image by resource locator to server. 
	 * WARNING: Version Honeycomb and higher throws exceptions for network
	 * activity in main thread. Use asyncUpload if on main UI thread.
	 * @param mLocation 	The relative user API mLocation, e.g. profile_picture
	 * @param fieldName The form field name expected by the API
	 * @param uri 	The image uri to upload
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public String upload(String location, String fieldName, Uri uri) {
		String userId = String.valueOf(mCache.getUser().getId());
		String absoluteUrl = mCache.getProperty("skope_service_url") + "/user/" + userId + "/" + location + "/";

		// Create HTTP client
        CustomHttpClient client = new CustomHttpClient(absoluteUrl, mContext);
        client.setUseBasicAuthentication(true);
        client.setUsernamePassword(mUsername, mPassword);
        
        // Add image
        client.addBitmapUri(fieldName, uri);
         
        // Send HTTP request to web service
        try {
            client.execute(RequestMethod.POST);
        } catch (Exception e) {
        	// Most exceptions already handled by client
            e.printStackTrace();
        }

        // Check for server response
		switch (client.getResponseCode()) {
		case HttpStatus.SC_OK:
			return client.getResponse();
		case 0:
			// No server response
			Log.e(TAG, "Connection failed");
			break;
		case HttpStatus.SC_UNAUTHORIZED:
		case HttpStatus.SC_REQUEST_TIMEOUT:
		case HttpStatus.SC_BAD_GATEWAY:
		case HttpStatus.SC_GATEWAY_TIMEOUT:
		case HttpStatus.SC_INTERNAL_SERVER_ERROR:
		case HttpStatus.SC_BAD_REQUEST:
			Log.e(TAG, "Failed to upload image: " + client.getErrorMessage());
		}
		
		return "";
	}

	/**
	 * Uploads image asynchronously to server.
	 * Create an ImageUploader.OnUploadListener to register for callbacks.  
	 * @param mLocation 	The relative user API mLocation, e.g. profile_picture
	 * @param fieldName The form field name expected by the API
	 * @param uri 	The image Bitmap to upload
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void asyncUpload(String location, String fieldName, Uri uri) {
		String absoluteUrl = mCache.getProperty("skope_service_url") + "/user/" + mUserId + "/" + location + "/";
		UploadRunner runner = new UploadRunner(absoluteUrl, mUsername, mPassword, fieldName);
		runner.execute(uri);
	}
	
	public OnUploadListener getOnUploadListener() {
		return mOnUploadListener;
	}

	public void setOnUploadListener(OnUploadListener onUploadListener) {
		this.mOnUploadListener = onUploadListener;
	}

}
