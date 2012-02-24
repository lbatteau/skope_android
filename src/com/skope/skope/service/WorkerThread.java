/*
 * Copyright 2010 Mark Brady
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skope.skope.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.ObjectOfInterest;
import com.skope.skope.application.ObjectOfInterestList;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.UiQueue;
import com.skope.skope.application.User;
import com.skope.skope.application.UserPhoto;
import com.skope.skope.http.BMPFromURL;
import com.skope.skope.http.CustomHttpClient;
import com.skope.skope.http.CustomHttpClient.RequestMethod;
import com.skope.skope.http.ImageUploader;
import com.skope.skope.ui.UserProfileActivity;
import com.skope.skope.util.NotificationUtils;
import com.skope.skope.util.Type;

/***
 * Used by the Service to perform long running tasks (e.g. network
 * connectivity) in a single separate thread.  This implementation uses a
 * single thread to pop items off the end of a ServiceQueue, providing a clear
 * finishing point (i.e. current task complete & queue empty) where the
 * Services own stopSelf() method can then be called to terminate the
 * background part of the Application.
 */
public class WorkerThread extends Thread {
    
	private static final String TAG = WorkerThread.class.getName();
	
	/**
     * [Optional] Execution state of the currently running long process, used by
     * the service to recover the state after the Service has been abnormally
     * terminated.
     */
    public static final String PROCESS_STATE = "PROCESS_STATE";
    /** [Optional] Size of long task increment. **/
    private static final int LONG_TASK_INCREMENT = 10;
    /** [Optional] End of long task. **/
    private static final int LONG_TASK_COMPLETE = 100;
    /***
     * [Optional] Configures how much time (in milliseconds) should be wasted
     * between UI updates - for test use only.
     */
    private static final int WASTE_TIME = 2000;
    /** [Optional] Synchronisation lock for the Thread Sleep. **/
    private final Object mWakeLock = new Object();
    /** Queue of incoming messages. **/
    private final List<Message> mWorkQueue = new ArrayList<Message>();
    /** Pointer to the Application Cache. **/
    private final Cache mCache;
    /** Pointer to the Application UiQueue. **/
    private final UiQueue mUiQueue;
    /** Pointer to the parent Service.. **/
    private LocationService mLocationService;
    /***
     * TRUE when the WorkerThread can no longer handle incoming messages,
     * because it is shutting down or dead.
     */
    private boolean stopping = false;
    
    private ObjectOfInterestList mObjectOfInterestList;

    /***
     * Constructor which stores pointers to the Application Cache, UiQueue and
     * parent Service.
     *
     * @param cache Application Cache.
     * @param uiQueue UiQueue.
     * @param myService MyService.
     */
    protected WorkerThread(final Cache cache, final UiQueue uiQueue,
            final LocationService locationService) {
    	mCache = cache;
        mUiQueue = uiQueue;
        mLocationService = locationService;
        mObjectOfInterestList = new ObjectOfInterestList();
    }

    /***
     * Add a message to the work queue.
     *
     * @param message Message containing a description of work to be done.
     */
    protected final void add(final Message message) {
        synchronized (mWorkQueue) {
            Log.i(SkopeApplication.LOG_TAG, "WorkerThread.add() "
                    + "Message type[" + Type.getType(message.what) + "]");
            mWorkQueue.add(message);
        }
        showQueue();
    }

    /***
     * Returns the current state of the WorkerThread.
     *
     * @return TRUE when the WorkerThread can no longer handle incoming
     *         messages, because it is dead or shutting down, FALSE otherwise.
     */
    public final boolean isStopping() {
        return stopping;
    }

    /***
     * Main run method, where all the queued messages are executed.
     */
    public final void run() {
        setName("WorkerThread");
        while (mWorkQueue.size() > 0) {
            Type type;
            Bundle bundle = null;
            synchronized (mWorkQueue) {
                Message message = mWorkQueue.remove(0);
                Log.i(SkopeApplication.LOG_TAG, "WorkerThread.run() "
                        + "Message type[" + Type.getType(message.what) + "]");
                type = Type.getType(message.what);
                if (message.obj != null
                        && message.obj.getClass() == Bundle.class) {
                    bundle = (Bundle) message.obj;
                }
            }
            showQueue();

            switch (type) {
            case FIND_OBJECTS_OF_INTEREST:
                findObjectsOfInterest(bundle);
                break;
                
            case UPLOAD_PROFILE_PICTURE:
            	uploadProfilePicture(bundle);
            	break;
                
            case UPLOAD_IMAGE:
            	uploadImage(bundle);
            	break;

            case READ_USER_PHOTOS:
            	readUserPhotos(bundle);
            	break;

            case DO_LONG_TASK:
                doLongTask(bundle);
                break;

            default:
                // Do nothing.
                break;
            }
        }

        mUiQueue.postToUi(Type.FIND_OBJECTS_OF_INTEREST_FINISHED, null, true);
        mCache.setStateFindObjectsOfInterest("");

        stopping = true;
        mLocationService.stopSelf();
    }

    /***
     * Calls the Skope mLocation service for the given mLocation. 
     *
     * @param bundle
     *            Bundle contains the current mLocation as a pair of
     *            latitude/longitude doubles. Since the message could 
     *            be posted from the UI, the bundle might be empty.
     *            In that case use the current mLocation stored in the
     *            parent service. 
     */
    private void findObjectsOfInterest(final Bundle bundle) {
    	Location currentLocation;
    	
        mCache.setStateFindObjectsOfInterest("Searching objects of interest nearby");
        mUiQueue.postToUi(Type.FIND_OBJECTS_OF_INTEREST_START, null, true);

    	JSONArray jsonResponse = new JSONArray();
		
		// Check if bundle is present
    	if (bundle == null) {
    		// Bundle not present
    		currentLocation = mCache.getCurrentLocation();
    		
    		// If the current mLocation is not known, post message
    		if (currentLocation == null) {
    	        mCache.setStateFindObjectsOfInterest("Finished");
    	        mUiQueue.postToUi(Type.UNDETERMINED_LOCATION, null, true);
    	        return;
    		}
    	} else {
    		// Bundle present, extract mLocation information
    		double latitude = bundle.getDouble(LocationService.LATITUDE);
    		double longitude = bundle.getDouble(LocationService.LONGITUDE);
    		String provider = bundle.getString(LocationService.PROVIDER);
    		currentLocation = new Location(provider);
    		currentLocation.setLatitude(latitude);
    		currentLocation.setLongitude(longitude);
    	}
		
		String username = mCache.getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
		String password = mCache.getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
		String serviceUrl = mCache.getProperty("skope_service_url") + "/skope/";
		
		// Set up HTTP client
        CustomHttpClient client = new CustomHttpClient(serviceUrl, mLocationService.getApplicationContext());
        client.setUseBasicAuthentication(true);
        client.setUsernamePassword(username, password);
        
        client.addParam("lat", String.valueOf(currentLocation.getLatitude()));
        client.addParam("lng", String.valueOf(currentLocation.getLongitude()));
        client.addParam("status_message", mCache.getUser().getStatus());
         
        // Send HTTP request to web service
        try {
            client.execute(RequestMethod.GET);
        } catch (Exception e) {
        	// Most exceptions already handled by client
            e.printStackTrace();
        }
        
        String response = client.getResponse();
        
        if (response == null) {
        	return;
        } else {
        	// Extract JSON data from response
	        try {
	        	jsonResponse = new JSONArray(response);
			} catch (JSONException e) {
				// Log exception
				Log.e(SkopeApplication.LOG_TAG, e.toString());
			}
			
			// Copy the JSON list of objects to our OOI list
			mObjectOfInterestList.clear();
			for (int i=0; i < jsonResponse.length(); i++) {
				try {
					JSONObject jsonObject = jsonResponse.getJSONObject(i);
					
					// Create new object of interest
					ObjectOfInterest objectOfInterest = new ObjectOfInterest(jsonObject, mCache);
					
					// If current user, skip
					if (!objectOfInterest.getUserName().equals(username)) {
						// Set distance
						objectOfInterest.setDistanceToLocation(currentLocation);
						// Add to list
						mObjectOfInterestList.add(objectOfInterest);
					}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}				
        }
        
        mCache.getObjectOfInterestList().update(mObjectOfInterestList);
       
        mCache.setStateFindObjectsOfInterest("Finished");
        mUiQueue.postToUi(Type.FIND_OBJECTS_OF_INTEREST_FINISHED, null, true);

        /*if (bundle != null) {
            Bundle outBundle = new Bundle();
            outBundle.putString("TEXT",
                    "Searching objects of interest finished. Called from ["
                            + bundle.getString("TEXT") + "]");
            mUiQueue.postToUi(Type.SHOW_DIALOG, outBundle, false);
        }*/
    }

    /***
     * Uploads an image to the server
     * 
     * The target filename is [UUID].png
     *
     * @param bundle
     *            Bundle should contain:
     *            	"LOCATION": The relative API mLocation
     *            	"FIELDNAME": The form field name, e.g. "profile_picture"
     *            	"URI": The image URI to upload
     */
    private void uploadImage(final Bundle bundle) {
        mUiQueue.postToUi(Type.UPLOAD_IMAGE_START, null, true);

        ImageUploader imageUploader = new ImageUploader(mLocationService.getApplicationContext(), mCache);

        String location = bundle.getString(LocationService.IMAGE_UPLOAD_LOCATION);
		String name = bundle.getString(LocationService.IMAGE_UPLOAD_NAME);
		String uri = bundle.getString(LocationService.IMAGE_UPLOAD_URI);
		
		try {
			imageUploader.upload(location, name, Uri.parse(uri));
		} catch (Exception e) {
			Bundle outBundle = new Bundle();
			outBundle.putString("TEXT", 
					mLocationService.getResources().getString(R.string.error_image_upload_not_found)
							+ ": " + e.toString());
            mUiQueue.postToUi(Type.SHOW_DIALOG, outBundle, false);
		}
		
        mUiQueue.postToUi(Type.UPLOAD_IMAGE_END, null, true);

    }
    
    /***
     * Uploads an image to the server
     * 
     * The target filename is [UUID].png
     *
     * @param bundle
     *            Bundle should contain:
     *            	"LOCATION": The relative API mLocation
     *            	"FIELDNAME": The form field name, e.g. "profile_picture"
     *            	"URI": The image URI to upload
     */
    private void uploadProfilePicture(final Bundle bundle) {
        mUiQueue.postToUi(Type.UPLOAD_PROFILE_PICTURE_START, null, true);

        ImageUploader imageUploader = new ImageUploader(mLocationService.getApplicationContext(), mCache);

        String location = bundle.getString(LocationService.IMAGE_UPLOAD_LOCATION);
		String name = bundle.getString(LocationService.IMAGE_UPLOAD_NAME);
		String uri = bundle.getString(LocationService.IMAGE_UPLOAD_URI);
		
		String response;
		try {
			response = imageUploader.upload(location, name, Uri.parse(uri));
		} catch (Exception e) {
			Bundle outBundle = new Bundle();
			outBundle.putString("TEXT", 
					mLocationService.getResources().getString(R.string.error_image_upload_not_found)
							+ ": " + e.toString());
            mUiQueue.postToUi(Type.SHOW_DIALOG, outBundle, false);
            return;
		}
		
    	// Server returns new profile picture URL
        JSONObject jsonResponse = null;
        String profilePictureURL = "";
        
        try {
        	jsonResponse = new JSONObject(response);
        	profilePictureURL = jsonResponse.getString("profile_picture_url");
		} catch (JSONException e) {
			Bundle outBundle = new Bundle();
			outBundle.putString("TEXT", 
					mLocationService.getResources().getString(R.string.error_profile_picture_result)
							+ ": " + e.toString());
            mUiQueue.postToUi(Type.SHOW_DIALOG, outBundle, false);
			return;
		}
		
		// Post back new photo and thumbnail url
		Bundle outBundle = new Bundle();
		outBundle.putString("profile_picture_url", profilePictureURL);
        mUiQueue.postToUi(Type.UPLOAD_PROFILE_PICTURE_END, outBundle, true);
    }
    
    private void readUserPhotos(final Bundle bundle) {
    	String userPhotosUsername;
    	JSONArray jsonResponse = new JSONArray();
    	
    	mUiQueue.postToUi(Type.READ_USER_PHOTOS_START, null, true);

    	// Check if bundle is present
    	if (bundle == null) {
    		return;
    	}
		
		// Bundle present, extract mUsername
		userPhotosUsername = bundle.getString("USERNAME");

		String username = mCache.getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
		String password = mCache.getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
		String serviceUrl = mCache.getProperty("skope_service_url") + "/user/" + userPhotosUsername + "/photos/";
		
		// Set up HTTP client
        CustomHttpClient client = new CustomHttpClient(serviceUrl, mLocationService.getApplicationContext());
        client.setUseBasicAuthentication(true);
        client.setUsernamePassword(username, password);
        
        // Send HTTP request to web service
        try {
            client.execute(RequestMethod.GET);
        } catch (Exception e) {
        	// Most exceptions already handled by client
            e.printStackTrace();
        }
        
        String response = client.getResponse();
        
        if (response == null) {
        	return;
        } else {
        	// Extract JSON data from response
	        try {
	        	jsonResponse = new JSONArray(response);
			} catch (JSONException e) {
				// Log exception
				Log.e(SkopeApplication.LOG_TAG, e.toString());
			}
			
			// Copy the JSON list of objects to our OOI list
			Cache.USER_PHOTOS.clear();
			for (int i=0; i < jsonResponse.length(); i++) {
				try {
					JSONObject jsonObject = jsonResponse.getJSONObject(i);
					
					// Create new object of interest
					UserPhoto userPhoto = new UserPhoto(jsonObject, mCache);
					// Add to list
					Cache.USER_PHOTOS.add(userPhoto);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}				
        }
        
        mUiQueue.postToUi(Type.READ_USER_PHOTOS_END, null, true);
    }

    /***
     * [Optional] Example task which takes time to complete and repeatedly
     * updates the UI.
     *
     * @param bundle Bundle of extra information.
     */
    private void doLongTask(final Bundle bundle) {
        mCache.setStateLongTask("Loading long task");
        mUiQueue.postToUi(Type.UPDATE_LONG_TASK, null, true);
        wasteTime(WASTE_TIME);

        int i = 0;
        if (bundle != null) {
            i = bundle.getInt(PROCESS_STATE);
        } else {
            i = 0;
        }

        for (; i <= LONG_TASK_COMPLETE; i += LONG_TASK_INCREMENT) {
            mCache.setStateLongTask("Long task " + i + "% complete");
            mUiQueue.postToUi(Type.UPDATE_LONG_TASK, null, true);
            NotificationUtils.notifyUserOfProgress(mLocationService
                    .getApplicationContext(), i);
            wasteTime(WASTE_TIME);
            mCache.setLongProcessState(i);
        }
        /** Clear Long Process state. **/
        mCache.setLongProcessState(-1);

        mCache.setStateLongTask("Long task done");
        mUiQueue.postToUi(Type.UPDATE_LONG_TASK, null, true);
        NotificationUtils.notifyUserOfProgress(mLocationService
                .getApplicationContext(), -1);
    }

    /***
     * [Optional] Example task which sends the current state of the queue to the
     * UI.
     */
    private void showQueue() {
        StringBuffer stringBuffer = new StringBuffer();
        for (Message message : mWorkQueue) {
            stringBuffer.append("Message type[");
            stringBuffer.append(Type.getType(message.what));
            stringBuffer.append("]\n");
        }
        mCache.setQueue(stringBuffer.toString());
        mUiQueue.postToUi(Type.UPDATE_QUEUE, null, true);
    }

    /***
     * [Optional] Slow down the running task - for test use only.
     *
     * @param time
     *            Amount of time to waste.
     */
    private void wasteTime(final long time) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < startTime + time) {
            synchronized (mWakeLock) {
                try {
                    mWakeLock.wait(startTime + WASTE_TIME
                            - System.currentTimeMillis());
                } catch (InterruptedException e) {
                    // Do nothing.
                }
            }
        }
    }
}
