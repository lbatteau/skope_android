package com.skope.skope.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.ServiceQueue;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.UiQueue;
import com.skope.skope.ui.SkopeListActivity;
import com.skope.skope.utils.Type;

public class LocationService extends Service implements LocationListener  {
	
	private static final String TAG = LocationService.class.getName();
	
	// Constants time and distance
	private static final int ONE_MINUTE = 60000;
	private static final int TWO_MINUTES = ONE_MINUTE * 2;
    private static final int ONE_METER = 1;
	private static final int TEN_METERS = ONE_METER * 10;

	// Constants for bundle keys
	public static final String LATITUDE = "LAT";
	public static final String LONGITUDE = "LONG";
	public static final String PROVIDER = "PROVIDER";

	// Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.location_service_started;
	
	private NotificationManager m_notificationManager;
	
    private LocationManager m_locationManager;
    private Location m_currentLocation;
    
    /** Performs all long running tasks in a separate thread. **/
    private WorkerThread mWorkerThread;
    /** Synchronisation lock for the WorkerThread. **/
    private final Object mWorkerThreadLock = new Object();
    /** Pointer to the Application Cache. **/
    private Cache mCache;
    /** Pointer to the Application UiQueue. **/
    private UiQueue mUiQueue;
    /** Pointer to the Application ServiceQueue. **/
    private ServiceQueue mServiceQueue;
    
    /** Handler for receiving all messages from the ServiceQueue. **/
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message message) {
            Message messageCopy = new Message();
            messageCopy.copyFrom(message);
            processMessage(messageCopy);
        }
    };


    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
    	public LocationService getService() {
            return LocationService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return m_localBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder m_localBinder = new LocalBinder();

	@Override
	public void onCreate() {
        SkopeApplication skopeApplication = (SkopeApplication) getApplication();
        Log.i(SkopeApplication.LOG_TAG, "MyService.onCreate() " + "myApplication["
                + skopeApplication + "]");
        mCache = skopeApplication.getCache();
        mUiQueue = skopeApplication.getUiQueue();
        mServiceQueue = skopeApplication.getServiceQueue();
        
        /**
         * Resister with the ServiceQueue that the Service is now ready to
         * handle incoming messages.
         */
        mServiceQueue.registerServiceHandler(mHandler);

        /** Recreate an unresolved execution state. **/
        int state = mCache.getLongProcessState();
        if (state != -1) {
            Bundle bundle = new Bundle();
            bundle.putInt(WorkerThread.PROCESS_STATE, state);
            mServiceQueue.postToService(Type.DO_LONG_TASK, bundle);
        }

        // Get the location manager
		m_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		// Get the notification manager
		m_notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		// Determine location. Start with fine.
		m_currentLocation = m_locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (m_currentLocation == null) {
			// Fall back to coarse location.
			m_currentLocation = m_locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		
		// Store current location in cache
		mCache.setCurrentLocation(m_currentLocation);

		// Register service as location event listener
		m_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
		m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, ONE_MINUTE, 0, this);
		
		showNotification();
	}
	
    /***
     * Reacts to any incoming message by passing it to the WorkerThread,
     * creating a new one if necessary.
     *
     * @param message
     *            Message from UI.
     */
    private void processMessage(final Message message) {
        synchronized (mWorkerThreadLock) {
            if (mWorkerThread == null || mWorkerThread.isStopping()) {
                mWorkerThread = new WorkerThread(mCache, mUiQueue, this);
                mWorkerThread.add(message);
                mWorkerThread.start();
            } else {
                mWorkerThread.add(message);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
    	m_notificationManager.cancel(NOTIFICATION);
    	
    	m_locationManager.removeUpdates(this);

    	Log.i(SkopeApplication.LOG_TAG, "LocationService.MyBinder.onDestroy()");
        mServiceQueue.registerServiceHandler(null);
        super.onDestroy();
    }

	@Override
	public void onLocationChanged(Location location) {
		if (isBetterLocation(location, m_currentLocation)) {
			mCache.setCurrentLocation(location);
			Bundle bundle = new Bundle();
            bundle.putDouble(LATITUDE, location.getLatitude());
            bundle.putDouble(LONGITUDE, location.getLongitude());
            bundle.putString(PROVIDER, location.getProvider());
	        mServiceQueue.postToService(Type.FIND_OBJECTS_OF_INTEREST, bundle);
	        mUiQueue.postToUi(Type.LOCATION_CHANGED, null, true);

		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.i(TAG, "Enabled provider" + provider);

	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.i(TAG, "Disabled provider" + provider);
	}

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.location_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());
        
        notification.flags = Notification.FLAG_ONGOING_EVENT;

        // The PendingIntent to launch our activity if the user selects this notification
        Intent intent = new Intent(this, SkopeListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.location_service_label),
                       text, contentIntent);

        // Send the notification.
        m_notificationManager.notify(NOTIFICATION, notification);
    }
    
	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
	    boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}	

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	
}
