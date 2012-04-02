package nl.skope.android.c2dm;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import nl.skope.android.R;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.http.CustomHttpClient;
import nl.skope.android.http.CustomHttpClient.RequestMethod;
import nl.skope.android.ui.OOIChatActivity;
import nl.skope.android.ui.OOIDetailMapActivity.AsyncTaskListener;

import org.apache.http.HttpStatus;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.util.Log;

public class C2DMBroadcastReceiver extends BroadcastReceiver {
	public final static String C2DM_MESSAGE_ID = "id";
	public final static String C2DM_MESSAGE_SENDER = "sender";
	public final static String C2DM_MESSAGE_THUMBNAIL = "thumbnail";
	public final static String C2DM_MESSAGE_CONTENT = "content";
	public final static String C2DM_MESSAGE_USERID = "user_id";
	public final static String C2DM_MESSAGE_TIMESTAMP = "timestamp";

	SharedPreferences mPreferences;
	String mRegistrationId;

	@Override
	public void onReceive(Context context, Intent intent) {
		mPreferences = context.getSharedPreferences("skopePreferences",
				Context.MODE_WORLD_READABLE);
		if (intent.getAction().equals(
				"com.google.android.c2dm.intent.REGISTRATION")) {
			handleRegistration(context, intent);
		} else if (intent.getAction().equals(
				"com.google.android.c2dm.intent.RECEIVE")) {
			handleMessage(context, intent);
		}
	}

	private void handleRegistration(Context context, Intent intent) {
		mRegistrationId = intent.getStringExtra("registration_id");

		int userId = mPreferences.getInt(SkopeApplication.PREFS_USERID, 0);

		// Read properties from the /assets directory
		AssetManager assetManager = context.getResources().getAssets();
		Properties properties;
		try {
			InputStream inputStream = assetManager.open("skope.properties");
			properties = new Properties();
			properties.load(inputStream);
			Log.d(SkopeApplication.LOG_TAG, "properties: " + properties);
		} catch (IOException e) {
			Log.e(SkopeApplication.LOG_TAG,
					"Failed to open skope property file");
			e.printStackTrace();
			return;
		}
		
		// Construct API registration URL
		String baseUrl = properties.getProperty("skope_service_url");
		String url = String.format("%s/user/%d/c2dm/register/%s/", baseUrl,
				userId, mRegistrationId);
		String username = mPreferences.getString(
				SkopeApplication.PREFS_USERNAME, "");
		String password = mPreferences.getString(
				SkopeApplication.PREFS_PASSWORD, "");

		if (intent.getStringExtra("error") != null) {
			// Registration failed, should try again later.
		} else if (intent.getStringExtra("unregistered") != null) {
			// unregistration done, new messages from the authorized sender will
			// be rejected
		} else if (mRegistrationId != null) {
			// Compare this registration ID to the one we already have
			String currentRegistrationId = mPreferences.getString(
					SkopeApplication.PREFS_C2DM_REGISTRATIONID, null);
			if (currentRegistrationId != null
					&& !currentRegistrationId.equals(mRegistrationId)) {
				// Registration ID for this device has changed.
				// Delete the previous one.
				new UserC2DMDeleteRegistration().execute(url, username, password);
			}

			// Send the registration ID to the 3rd party site that is sending
			// the messages.
			new UserC2DMRegister().execute(url, username, password);
		}
	}

	private void handleMessage(Context context, Intent intent) {
		String content = intent.getExtras().getString(C2DM_MESSAGE_CONTENT);
		String sender = intent.getExtras().getString(C2DM_MESSAGE_SENDER);
		int userId = Integer.parseInt(intent.getExtras().getString(C2DM_MESSAGE_USERID));
		
		// Show notification
		CharSequence text = String.format("%s: %s", sender, content);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_stat_notify,
				text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		Intent notificationIntent = new Intent(context, OOIChatActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		notificationIntent.putExtras(intent.getExtras());
		notificationIntent.putExtra(SkopeApplication.BUNDLEKEY_USERID, userId);
		notificationIntent.putExtra(SkopeApplication.BUNDLEKEY_REDIRECTACTIVITY, "nl.skope.android.ui.OOIChatActivity");

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(context, sender, content, contentIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		// Send the notification
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(SkopeApplication.NOTIFICATION_CHATMESSAGE, userId, notification);

		// Get instance of Vibrator from current Context
		Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

		// Vibrate for 300 milliseconds
		v.vibrate(300);
	}
	
	protected class UserC2DMRegister extends
			AsyncTask<String, Void, CustomHttpClient> {
		AsyncTaskListener mListener;

		@Override
		protected void onPreExecute() {
			if (mListener != null) {
				mListener.onTaskStart();
			}
		}

		@Override
		protected CustomHttpClient doInBackground(String... params) {
			// Create HTTP client
			CustomHttpClient client = new CustomHttpClient(params[0]);
			client.setUseBasicAuthentication(true);
			client.setUsernamePassword(params[1], params[2]);

			// Send HTTP request to web service
			try {
				client.execute(RequestMethod.GET);
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
				// Call back OK
				if (mListener != null) {
					mListener.onTaskDone(true, "");
				}
				SharedPreferences.Editor editor = mPreferences.edit();
				editor.putString(SkopeApplication.PREFS_C2DM_REGISTRATIONID,
						mRegistrationId);
				editor.putLong(
						SkopeApplication.PREFS_C2DM_REGISTRATIONTIMESTAMP,
						new Date().getTime());
				editor.commit();

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
				Log.e(SkopeApplication.LOG_TAG, "Failed to register device: "
						+ client.getErrorMessage());
				// Call back failed
				if (mListener != null) {
					mListener.onTaskDone(false, "Failed to register device");
				}
			}

		}

		public void setListener(AsyncTaskListener listener) {
			mListener = listener;
		}

	}

	protected class UserC2DMDeleteRegistration extends
			AsyncTask<String, Void, CustomHttpClient> {
		AsyncTaskListener mListener;

		@Override
		protected void onPreExecute() {
			if (mListener != null) {
				mListener.onTaskStart();
			}
		}

		@Override
		protected CustomHttpClient doInBackground(String... params) {
			// Create HTTP client
			CustomHttpClient client = new CustomHttpClient(params[0]);
			client.setUseBasicAuthentication(true);
			client.setUsernamePassword(params[1], params[2]);

			// Send HTTP request to web service
			try {
				client.execute(RequestMethod.DELETE);
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
			case HttpStatus.SC_NO_CONTENT:
				// Call back OK
				if (mListener != null) {
					mListener.onTaskDone(true, "");
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
				Log.e(SkopeApplication.LOG_TAG, "Failed to delete registration: "
						+ client.getErrorMessage());
				// Call back failed
				if (mListener != null) {
					mListener.onTaskDone(false, "Failed to delete registration");
				}
			}

		}

		public void setListener(AsyncTaskListener listener) {
			mListener = listener;
		}

	}
}