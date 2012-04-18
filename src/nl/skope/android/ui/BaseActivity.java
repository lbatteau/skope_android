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

package nl.skope.android.ui;

import java.security.InvalidParameterException;

import nl.skope.android.application.Cache;
import nl.skope.android.application.ServiceQueue;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.UiQueue;
import nl.skope.android.util.Type;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/***
 * BaseClass to be extended by all Activities that use the framework. This class
 * caches Application objects (ServiceQueue, UiQueue and Cache) and handles
 * incoming messages from UiQueue. The code pattern for receiving messages
 * involves overriding the post() method, but calling super() if your activity
 * does not implement the given message. The same goes with onCreateDialog(),
 * onResume() and onPause() which can also be overridden in your Activity.
 */
public class BaseActivity extends Activity {
	
	private static final String TAG = BaseActivity.class.getSimpleName();

	/** Pointer to the ServiceQueue. **/
	private ServiceQueue mServiceQueue;
	/** Pointer to the Application Cache. **/
	private Cache cache;
	/** Pointer to the UiQueue. **/
	private UiQueue mUiQueue;
	/***
	 * Used by processMessage() to temporally store a Bundle object so it can be
	 * used in the onCreateDialog() method.
	 */
	private Bundle mDialogBundle;

	/***
	 * Create the BaseActivity and cache Application objects: ServiceQueue,
	 * UiQueue and Cache.
	 * 
	 * @param savedInstanceState
	 *            Unused state object.
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		SkopeApplication application = (SkopeApplication) getApplication();
		mServiceQueue = application.getServiceQueue();
		mUiQueue = application.getUiQueue();
		cache = application.getCache();
		super.onCreate(savedInstanceState);
	}
	
	protected boolean checkCacheSanity() {
		// Check user signed out
		if (cache.isUserSignedOut()) {
			// User signed out, always go to login screen
			Intent i = new Intent();
			i.setClassName("nl.skope.android",
					"nl.skope.android.ui.LoginActivity");
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			finish();
			return false;
		} else {
			// Not signed out. Check if user present
			if (cache.getUser() == null) {
				// Not present, could have been garbage collected.
				// Go back to login screen and set the auto login flag.
				Intent i = new Intent();
				i.setClassName("nl.skope.android",
						"nl.skope.android.ui.LoginActivity");
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				// Add auto login flag
				//TODO Redirect doesn't work, target Activity needs onNewIntent method implemented
				//Bundle bundle = new Bundle();
				//bundle.putString(SkopeApplication.BUNDLEKEY_REDIRECTACTIVITY, getIntent().getClass().getName());
				//i.putExtras(bundle);
				startActivity(i);
				finish();
				return false;
			}
		}
		
		return true;
	}

	/***
	 * Subscribe the Activity to the UiQueue.
	 */
	@Override
	protected void onResume() {
		mUiQueue.subscribe(mHandler);
		super.onResume();
	}

	/***
	 * Unsubscribe the Activity from the UiQueue.
	 */
	@Override
	protected void onPause() {
		mUiQueue.unsubscribe(mHandler);
		super.onPause();
	}

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
				Log.e(TAG, "BaseActivity.Handler."
						+ "handleMessage() ERROR");
			}
			/** TEST: Try and catch an error condition **/

			processMessage(message);
		}
	};

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
		case SHOW_DIALOG:
			mDialogBundle = bundle;
			showDialog(Type.DIALOG_STATUS.ordinal());
			break;

		default:
			// Do nothing.
			break;
		}
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		switch (Type.getType(id)) {
		case DIALOG_STATUS:
			String text = "Cached dialog bundle is NULL";
			if (mDialogBundle != null) {
				text = mDialogBundle.getString("TEXT");
				mDialogBundle = null;
			}

			return new AlertDialog.Builder(this).setMessage(text).create();

		default:
			throw new InvalidParameterException("BaseActivity."
					+ "onCreateDialog() Unknown dialog type["
					+ Type.getType(id) + "]");
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
		return cache;
	}
}
