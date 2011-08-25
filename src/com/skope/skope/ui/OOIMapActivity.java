package com.skope.skope.ui;

import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.ServiceQueue;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.UiQueue;
import com.skope.skope.utils.Type;

public abstract class OOIMapActivity extends MapActivity {

	/** Pointer to the ServiceQueue. **/
	private ServiceQueue mServiceQueue;
	/** Pointer to the Application Cache. **/
	protected Cache mCache;
	/** Pointer to the UiQueue. **/
	private UiQueue mUiQueue;

	/***
	 * Used by processMessage() to temporally store a Bundle object so it can be
	 * used in the onCreateDialog() method.
	 */
	private Bundle mDialogBundle;
	protected MapView mMapView;
	protected OOIItemizedOverlay mItemizedOverlay;
	protected List<Overlay> mMapOverlays;
	/***
	 * Handler which is subscribed to the UiQueue whenever the Activity is on
	 * screen.
	 */
	private final Handler mHandler = new Handler() {
			@Override
			public void handleMessage(final Message message) {
	
				/** TEST: Try and catch an error condition **/
				if (message.what == Type.SHOW_DIALOG.ordinal()
						&& message.obj == null) {
					Log.e(SkopeApplication.LOG_TAG, "BaseActivity.Handler."
							+ "handleMessage() ERROR");
				}
				/** TEST: Try and catch an error condition **/
	
				processMessage(message);
			}
		};

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    SkopeApplication application = (SkopeApplication) getApplication();
	    mServiceQueue = application.getServiceQueue();
	    mUiQueue = application.getUiQueue();
	    mCache = application.getCache();
	
	    super.onCreate(savedInstanceState);
	    
	    setContentView();
	    
	    mMapView = (MapView) findViewById(R.id.mapview);
	    initializeMapView(); 
	    
	    populateItemizedOverlays();       
	    
	}
	
	protected void setContentView() {
		setContentView(R.layout.map);
	}

	/***
	 * Subscribe the Activity to the UiQueue.
	 */
	@Override
	protected void onResume() {
		mUiQueue.subscribe(mHandler);
		super.onResume();
		initializeMapView();
		populateItemizedOverlays();
	}

	/***
	 * Unsubscribe the Activity from the UiQueue.
	 */
	@Override
	protected void onPause() {
		mUiQueue.unsubscribe(mHandler);
		super.onPause();
	}
	
	protected abstract void populateItemizedOverlays();
	
	protected void initializeMapView() {
		mMapView.setBuiltInZoomControls(true);
	}

	/***
	 * Process an incoming message by getting the Type and optional bundle and
	 * passing it to the overridable post() method.
	 * 
	 * @param message
	 *            Message to process.
	 */
	private void processMessage(final Message message) {
		if (message.obj != null && message.obj.getClass() == Bundle.class) {
			post(Type.getType(message.what), (Bundle) message.obj);
		} else {
			post(Type.getType(message.what), null);
		}
	}

	/***
	 * Overridable method for handling any messages not caught by the Activities
	 * own post() method. The code pattern allows more generic messages to be
	 * handled here (show battery warning dialog, etc).
	 * 
	 * @param type
	 *            Message type.
	 * @param bundle
	 *            Optional Bundle of extra information.
	 */
	public void post(final Type type, final Bundle bundle) {
		switch (type) {
		case FIND_OBJECTS_OF_INTEREST_FINISHED:
			populateItemizedOverlays();
			break;
	
		default:
			// Do nothing.
			break;
		}
	}

	/**
	 * Return the Application ServiceQueue.
	 * 
	 * @return ServiceQueue
	 */
	public final ServiceQueue getServiceQueue() {
		return mServiceQueue;
	}

	/**
	 * Return the Application Cache.
	 * 
	 * @return Cache
	 */
	public final Cache getCache() {
		return mCache;
	}

}