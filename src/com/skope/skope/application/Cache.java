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

package com.skope.skope.application;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Properties;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;


/***
 * Store application state information either permanently (in a properties
 * file), or in memory for the duration of the Application class lifecycle.
 */
public class Cache {
    /** Preferences file name. **/
    private static final String PREFS_FILE = "CACHE";

    /** [Optional] Preferences ID for process X. **/
    private static final String STATE_FIND_OBJECTS_OF_INTEREST = "STATE_FIND_OBJECTS_OF_INTEREST";

    /** [Optional] Preferences ID for process Y. **/
    private static final String STATE_LONG_TASK = "STATE_LONG_TASK";

    /** [Optional] Preferences ID for WorkerThread Queue. **/
    private static final String STATE_QUEUE = "STATE_QUEUE";

    /** [Optional] Execution state. **/
    private static final String STATE_PROCESS = "STATE_PROCESS";

    /** Cached application context. **/
    private final Context mContext;
    
    /** The custom properties for this project */
	private Properties m_properties;
	
	/** The preferences shared throughout the application */
	private SharedPreferences m_preferences;
	
    private Resources m_resources;
    
    /** The current list of objects of interest nearby **/
    private ObjectOfInterestList m_objectOfInterestList;
    
    /** The current mLocation */
    private Location mCurrentLocation;
    
    /** The current selected object of interest */
    private ObjectOfInterest mSelectedObjectOfInterest;
    
    /** The current user */
    private User user;
    
    /** Image cache using hash map on URL string */
    protected HashMap<String,Bitmap> mImageCache;

    /***
     * Constructor stores the application context.
     *
     * @param context Application context.
     */
    protected Cache(final Context context) {
        mContext = context;

		// Get system resources
        m_resources = mContext.getResources();
        
        // Get application preferences
		m_preferences = mContext.getSharedPreferences("skopePreferences", Context.MODE_WORLD_READABLE);

		// Read properties from the /assets directory
        AssetManager assetManager = m_resources.getAssets();
        try {
            InputStream inputStream = assetManager.open("skope.properties");
            m_properties = new Properties();
            m_properties.load(inputStream);
            Log.i(SkopeApplication.LOG_TAG, "properties: " + m_properties);            
        } catch (IOException e) {
            Log.e(SkopeApplication.LOG_TAG, "Failed to open skope property file");
            e.printStackTrace();
        }
        
        m_objectOfInterestList = new ObjectOfInterestList();
        
        mImageCache = new HashMap<String,Bitmap>();
    }

    /***
     * [Optional] Set the state of short task.
     *
     * @param value State value.
     */
    public final void setStateFindObjectsOfInterest(final String value) {
        setValue(mContext, STATE_FIND_OBJECTS_OF_INTEREST, value);
    }

    /***
     * [Optional] Get the state of short task.
     *
     * @return State value.
     */
    public final String getStateFindObjectsOfInterest() {
        return getValue(mContext, STATE_FIND_OBJECTS_OF_INTEREST, null);
    }

    /***
     * [Optional] Set the state of long task.
     *
     * @param value State value.
     */
    public final void setStateLongTask(final String value) {
        setValue(mContext, STATE_LONG_TASK, value);
    }

    /***
     * [Optional] Get the state of long task.
     *
     * @return State value.
     */
    public final String getStateLongTask() {
        return getValue(mContext, STATE_LONG_TASK, null);
    }

    /***
     * [Optional] Set the state of the WorkerThread Queue.
     *
     * @param value State value.
     */
    public final void setQueue(final String value) {
        setValue(mContext, STATE_QUEUE, value);
    }

    /***
     * [Optional] Get the state of the WorkerThread Queue.
     *
     * @return State value.
     */
    public final String getQueue() {
        return getValue(mContext, STATE_QUEUE, null);
    }

    /***
     * [Optional] Set the execution state of a running Long task.
     *
     * @param value Execution state.
     */
    public final void setLongProcessState(final int value) {
        setValue(mContext, STATE_PROCESS, value);
    }

    /***
     * [Optional] Get the execution state of a running Long task.
     *
     * @return Execution state.
     */
    public final int getLongProcessState() {
        return getValue(mContext, STATE_PROCESS, -1);
    }

    /***
     * Set a value in the preferences file.
     *
     * @param context Android context.
     * @param key Preferences file parameter key.
     * @param value Preference value.
     */
    private static void setValue(final Context context,
            final String key, final String value) {
        SharedPreferences.Editor editor =
            context.getSharedPreferences(PREFS_FILE, 0).edit();
        editor.putString(key, value);
        if (!editor.commit()) {
            throw new NullPointerException(
                    "MainApplication.setValue() Failed to set key[" + key
                            + "] with value[" + value + "]");
        }
    }

    /***
     * Get a value from the preferences file.
     *
     * @param context Android context.
     * @param key Preferences file parameter key.
     * @param defaultValue Preference value.
     * @return Value as a String.
     */
    private static String getValue(final Context context, final String key,
            final String defaultValue) {
        return context.getSharedPreferences(PREFS_FILE, 0).getString(key,
                defaultValue);
    }

    /***
     * Set a value in the preferences file.
     *
     * @param context Android context.
     * @param key Preferences file parameter key.
     * @param value Preference value.
     */
    private static void setValue(final Context context, final String key,
            final int value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(
                PREFS_FILE, 0).edit();
        editor.putInt(key, value);
        if (!editor.commit()) {
            throw new NullPointerException(
                    "MainApplication.setValue() Failed to set key[" + key
                            + "] with value[" + value + "]");
        }
    }

    /***
     * Get a value from the preferences file.
     *
     * @param context Android context.
     * @param key Preferences file parameter key.
     * @param defaultValue Preference value.
     * @return Value as a String.
     */
    private static int getValue(final Context context, final String key,
            final int defaultValue) {
        return context.getSharedPreferences(PREFS_FILE, 0).getInt(key,
                defaultValue);
    }
    
    public String getProperty(String name) {
		return m_properties.getProperty(name);
	}

	public Resources getResources() {
		return m_resources;
	}
	
	public SharedPreferences getPreferences() {
		return m_preferences;
	}
	
	public synchronized ObjectOfInterestList getObjectOfInterestList() {
		return m_objectOfInterestList;
	}

	public Location getCurrentLocation() {
		return mCurrentLocation;
	}

	public void setCurrentLocation(Location currentLocation) {
		this.mCurrentLocation = currentLocation;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public HashMap<String, Bitmap> getImageCache() {
		return mImageCache;
	}

}
