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

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "dHd6ZTBRRnpjV2pTMzh4QnN1RUJMRmc6MQ")

/***
 * Application class persists for the duration of the JRE, and is used to store
 * all the persistence classes (database + cache) and for storing the message
 * handling framework (ServiceQueue + UiQueue).
 */
public class SkopeApplication extends Application {
    /** Application wide constants **/

    // Logging
    public static int LOGLEVEL = 1;
    public static boolean WARN = LOGLEVEL > 1;
    public static boolean DEBUG = LOGLEVEL > 0;
    
    // Preferences
	public static final String PREFS_USERNAME = "USERNAME";
	public static final String PREFS_PASSWORD = "PASSWORD";
	public static final String PREFS_USERID = "USERID";
	public static final String PREFS_C2DM_REGISTRATIONID = "REGISTRATIONID";
	public static final String PREFS_C2DM_REGISTRATIONTIMESTAMP = "REGISTRATIONTIMESTAMP";
	public static final String PREFS_RANGE = "RANGE";
	public static final String PREFS_GPSENABLED = "GPSENABLED";
	public static final String PREFS_FBHASPOSTED = "FBHASPOSTED";
	public final static String BUNDLEKEY_REDIRECTACTIVITY = "REDIRECT";
	public final static String BUNDLEKEY_BACKACTIVITY = "BACK";
	public final static String BUNDLEKEY_TAB = "TAB";
	public final static String BUNDLEKEY_USERID = "USERID";
	public final static String BUNDLEKEY_CHAT_UNREAD = "UNREAD";
	public final static String BUNDLEKEY_CHAT_FROM = "FROM";
	public final static String BUNDLEKEY_CHAT_LAST = "LAST";
	public final static String BUNDLEKEY_CHAT_MARKASREAD = "MARKASREAD";
	public final static String BUNDLEKEY_USER = "USER";
	public final static String BUNDLEKEY_RESPONSE = "RESPONSE";
	public final static String NOTIFICATION_CHATMESSAGE = "CHATMESSAGE";

	/** Skope server response codes **/
	public static final int RESPONSECODE_OK = 0;
	public static final int RESPONSECODE_UPDATE = 1;
	public static final int RESPONSECODE_PAYMENTDUE = 2;	
    
    /** Lazy loaded ServiceQueue. **/
    private ServiceQueue mServiceQueue;
    /** Lazy loaded UiQueue. **/
    private UiQueue mUiQueue;
    /** Lazy loaded Cache. **/
    private Cache mCache;
    /** TODO: Add a lazy loaded database helper. **/
    // private DatabaseHelper mDb;

    /***
     * Returns the lazy loaded ServiceQueue.
     *
     * @return ServiceQueue
     */
    public final synchronized ServiceQueue getServiceQueue() {
        if (mServiceQueue == null) {
            mServiceQueue = new ServiceQueue(this);
        }
        return mServiceQueue;
    }

    /***
     * Returns the lazy loaded UiQueue.
     *
     * @return UiQueue
     */
    public final synchronized UiQueue getUiQueue() {
        if (mUiQueue == null) {
            mUiQueue = new UiQueue();
        }
        return mUiQueue;
    }

    /***
     * Returns the lazy loaded Cache.
     *
     * @return Cache
     */
    public final synchronized Cache getCache() {
        if (mCache == null) {
            mCache = new Cache(this);
        }
        return mCache;
    }

    /***
     * Returns the lazy loaded DatabaseHelper.
     *
     * @return DatabaseHelper
     */
    public final synchronized DatabaseHelper getDb() {
        // TODO: Implement a database helper.
        return null;
    }
    
    /***
     * Terminate the application, so release all resources.
     */
    @Override
    public final void onTerminate() {
        mServiceQueue = null;
        mUiQueue = null;
        mCache = null;
        //mDb = null;
        super.onTerminate();
    }
    
    @Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        super.onCreate();
    }
    
}
