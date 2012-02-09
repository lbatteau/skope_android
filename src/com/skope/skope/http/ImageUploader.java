package com.skope.skope.http;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.http.HttpStatus;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.skope.skope.application.Cache;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.http.CustomHttpClient.RequestMethod;

public class ImageUploader {
	Cache mCache;
	String mUsername, mPassword;
	OnUploadListener mOnUploadListener;
	
	public interface OnUploadListener {
		public void onUploadStart();
		public void onUploadUpdate();
		public void onUploadComplete(boolean success, String errorMessage);
	}
	
	private class UploadRunner extends AsyncTask<Bitmap, Void, CustomHttpClient> {
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
		protected CustomHttpClient doInBackground(Bitmap... arg0) {
			// Create HTTP client
	        CustomHttpClient client = new CustomHttpClient(mUrl);
	        client.setUseBasicAuthentication(true);
	        client.setUsernamePassword(mUsername, mPassword);
	        
	        // Add image
	        client.addBitmap(mField, arg0[0]);
	         
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
				Log.e(SkopeApplication.LOG_TAG, "Connection failed");
			case HttpStatus.SC_UNAUTHORIZED:
			case HttpStatus.SC_REQUEST_TIMEOUT:
			case HttpStatus.SC_BAD_GATEWAY:
			case HttpStatus.SC_GATEWAY_TIMEOUT:
			case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			case HttpStatus.SC_BAD_REQUEST:
				Log.e(SkopeApplication.LOG_TAG, "Failed to upload image: " + client.getErrorMessage());
				if (mOnUploadListener != null) {
					mOnUploadListener.onUploadComplete(false, client.getErrorMessage());
				}
			default:
				break;
			}
		}
	}
	
	public ImageUploader(Cache cache) {
		mCache = cache;
		mUsername = mCache.getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
		mPassword = mCache.getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
	}
	
	/**
	 * Uploads image by resource locator to server. 
	 * WARNING: Version Honeycomb and higher throws exceptions for network
	 * activity in main thread. Use asyncUpload if on main UI thread.
	 * @param mLocation 	The relative user API mLocation, e.g. profile_picture
	 * @param fieldName The form field name expected by the API
	 * @param image 	The image Bitmap to upload
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void upload(String location, String fieldName, Bitmap image) {
		String absoluteUrl = mCache.getProperty("skope_service_url") + "/user/" + mUsername + "/" + location + "/";

		// Create HTTP client
        CustomHttpClient client = new CustomHttpClient(absoluteUrl);
        client.setUseBasicAuthentication(true);
        client.setUsernamePassword(mUsername, mPassword);
        
        // Add image
        client.addBitmap(fieldName, image);
         
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
			break;
		case 0:
			// No server response
			Log.e(SkopeApplication.LOG_TAG, "Connection failed");
			break;
		case HttpStatus.SC_UNAUTHORIZED:
		case HttpStatus.SC_REQUEST_TIMEOUT:
		case HttpStatus.SC_BAD_GATEWAY:
		case HttpStatus.SC_GATEWAY_TIMEOUT:
		case HttpStatus.SC_INTERNAL_SERVER_ERROR:
		case HttpStatus.SC_BAD_REQUEST:
			Log.e(SkopeApplication.LOG_TAG, "Failed to upload image: " + client.getErrorMessage());
		default:
			break;
		}
	}

	/**
	 * Uploads image asynchronously to server.
	 * Create an ImageUploader.OnUploadListener to register for callbacks.  
	 * @param mLocation 	The relative user API mLocation, e.g. profile_picture
	 * @param fieldName The form field name expected by the API
	 * @param image 	The image Bitmap to upload
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void asyncUpload(String location, String fieldName, Bitmap image) {
		String absoluteUrl = mCache.getProperty("skope_service_url") + "/user/" + mUsername + "/" + location + "/";
		UploadRunner runner = new UploadRunner(absoluteUrl, mUsername, mPassword, fieldName);
		runner.execute(image);
	}
	
	public OnUploadListener getOnUploadListener() {
		return mOnUploadListener;
	}

	public void setOnUploadListener(OnUploadListener onUploadListener) {
		this.mOnUploadListener = onUploadListener;
	}

}
