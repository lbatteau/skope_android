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

package nl.skope.android.application;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Handler;
import android.util.Log;

import nl.skope.android.R;
import nl.skope.android.ui.OOIListMapActivity;


/***
 * Store application state information either permanently (in a properties
 * file), or in memory for the duration of the Application class lifecycle.
 */
public class Cache {
	private static final String TAG = Cache.class.getSimpleName();
	
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

    /** Image cache settings */
    private static final int HARD_CACHE_CAPACITY = 40;
    private static final int DELAY_BEFORE_PURGE = 60 * 1000; // in milliseconds

    /** Cached application context. **/
    private final Context mContext;
    
    /** The custom properties for this project */
	private Properties mProperties;
	
	/** The preferences shared throughout the application */
	private SharedPreferences mPreferences;
	
    private Resources mResources;
    
    /** The current list of objects of interest nearby **/
    private ObjectOfInterestList mObjectOfInterestList;
    
    /** The list of user favorites **/
    private ObjectOfInterestList mUserFavoritesList, mUserChatsList;
    
    /** The current mLocation */
    private Location mCurrentLocation;
    private boolean mIsLocationProviderAvailable;
    
    /** The current selected object of interest */
    private User mSelectedUser;
    
    /** The current user */
    private User user;
    
    private boolean mUserSignedOut = true;
    
    // Hard cache, with a fixed maximum capacity and a life duration
    private final HashMap<String, Bitmap> sHardBitmapCache =
        new LinkedHashMap<String, Bitmap>(HARD_CACHE_CAPACITY / 2, 0.75f, true) {
        private static final long serialVersionUID = -7190622541619388252L;
        @Override
        protected boolean removeEldestEntry(LinkedHashMap.Entry<String, Bitmap> eldest) {
            if (size() > HARD_CACHE_CAPACITY) {
                // Entries push-out of hard reference cache are transferred to soft reference cache
                sSoftBitmapCache.put(eldest.getKey(), new SoftReference<Bitmap>(eldest.getValue()));
                return true;
            } else {
                return false;
            }
        }
    };

    // Soft cache for bitmap kicked out of hard cache
    private final static ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache =
        new ConcurrentHashMap<String, SoftReference<Bitmap>>(HARD_CACHE_CAPACITY / 2);

    private final Handler purgeHandler = new Handler();

    private final Runnable purger = new Runnable() {
        public void run() {
            clearCache();
        }
    };
    
    /** Queue for uploading of images */
    private static final List<Bitmap> mImageUploadQueue = new ArrayList<Bitmap>();
    
    /** Lookup list corresponding to User.relationship choices */
    public static String[] RELATIONSHIP_CHOICES;
    
    /** Lookup list corresponding to User.gender choices */
    public static String[] GENDER_CHOICES;
    
    public static final ArrayList<UserPhoto> USER_PHOTOS = new ArrayList<UserPhoto>();
    
    /***
     * Constructor stores the application context.
     *
     * @param context Application context.
     */
    protected Cache(final Context context) {
        mContext = context;

		// Get system resources
        mResources = mContext.getResources();
        
        // Fill lookup lists
        RELATIONSHIP_CHOICES = mResources.getStringArray(R.array.user_relationship_choices);
        GENDER_CHOICES = mResources.getStringArray(R.array.user_gender_choices);
        
        // Get application preferences
		mPreferences = mContext.getSharedPreferences("skopePreferences", Context.MODE_WORLD_READABLE);

		// Read properties from the /assets directory
        AssetManager assetManager = mResources.getAssets();
        try {
            InputStream inputStream = assetManager.open("skope.properties");
            mProperties = new Properties();
            mProperties.load(inputStream);
            Log.i(TAG, "properties: " + mProperties);            
        } catch (IOException e) {
            Log.e(TAG, "Failed to open skope property file");
            e.printStackTrace();
        }
        
        mObjectOfInterestList = new ObjectOfInterestList();
        mUserFavoritesList = new ObjectOfInterestList();
        mUserChatsList = new ObjectOfInterestList();
        
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

    /**
     * @param url The URL of the image that will be retrieved from the cache.
     * @return The cached bitmap or null if it was not found.
     */
    public Bitmap getBitmapFromCache(String url) {
        // First try the hard reference cache
        synchronized (sHardBitmapCache) {
            final Bitmap bitmap = sHardBitmapCache.get(url);
            if (bitmap != null) {
                // Bitmap found in hard cache
                // Move element to first position, so that it is removed last
                sHardBitmapCache.remove(url);
                sHardBitmapCache.put(url, bitmap);
                return bitmap;
            }
        }

        // Then try the soft reference cache
        SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(url);
        if (bitmapReference != null) {
            final Bitmap bitmap = bitmapReference.get();
            if (bitmap != null) {
                // Bitmap found in soft cache
                return bitmap;
            } else {
                // Soft reference has been Garbage Collected
                sSoftBitmapCache.remove(url);
            }
        }

        return null;
    }
    
    /**
     * @param url The URL of the image that will be retrieved from the cache.
     * @param bitmap The bitmap to add to the cache
     */
    public void addBitmapToCache(String url, Bitmap bitmap) {
    	// Add bitmap to cache
        if (bitmap != null) {
            synchronized (sHardBitmapCache) {
                sHardBitmapCache.put(url, bitmap);
            }
        }

    }
    

    /**
     * Clears the image cache used internally to improve performance. Note that for memory
     * efficiency reasons, the cache will automatically be cleared after a certain inactivity delay.
     */
    public void clearCache() {
        sHardBitmapCache.clear();
        sSoftBitmapCache.clear();
    }

    public void resetPurgeTimer() {
        purgeHandler.removeCallbacks(purger);
        purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
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
		return mProperties.getProperty(name);
	}

	public Resources getResources() {
		return mResources;
	}
	
	public SharedPreferences getPreferences() {
		return mPreferences;
	}
	
	public synchronized ObjectOfInterestList getObjectOfInterestList() {
		return mObjectOfInterestList;
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

	public List<Bitmap> getImageUploadQueue() {
		return mImageUploadQueue;
	}
	
	public ContentResolver getContentResolver() {
		return mContext.getContentResolver();
	}

	public ObjectOfInterestList getUserFavoritesList() {
		return mUserFavoritesList;
	}

	public ObjectOfInterestList getUserChatsList() {
		return mUserChatsList;
	}

	public boolean isUserSignedOut() {
		return mUserSignedOut;
	}

	public void setUserSignedOut(boolean userSignedOut) {
		this.mUserSignedOut = userSignedOut;
	}

	public boolean isLocationProviderAvailable() {
		return mIsLocationProviderAvailable;
	}

	public void setLocationProviderAvailable(boolean isLocationProviderAvailable) {
		this.mIsLocationProviderAvailable = isLocationProviderAvailable;
	}
	
}
