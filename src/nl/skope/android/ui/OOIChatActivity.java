package nl.skope.android.ui;

import java.util.ArrayList;
import java.util.Date;

import nl.skope.android.R;
import nl.skope.android.application.ObjectOfInterest;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.User.OnImageLoadListener;
import nl.skope.android.c2dm.C2DMBroadcastReceiver;
import nl.skope.android.http.CustomHttpClient;
import nl.skope.android.http.CustomHttpClient.RequestMethod;
import nl.skope.android.ui.OOIDetailMapActivity.AsyncTaskListener;
import nl.skope.android.util.Type;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class OOIChatActivity extends BaseActivity {
	private static final String TAG = "OOIChatActivity";
	private static final boolean D = true;

	public static final int MESSAGE_READ = 1;
	public static final int MESSAGE_WRITE = 2;

	private ObjectOfInterest mCurrentOOI;

	// C2DM Intent Filter
	private IntentFilter mIntentFilter;

	// Layout Views
	private ListView mConversationView;
	private EditText mOutEditText;
	private Button mSendButton;

	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCurrentOOI = getIntent().getExtras().getParcelable("USER");
		mCurrentOOI.setCache(getCache());

		setContentView(R.layout.chat);

	    // Back button
	    View backButton = findViewById(R.id.detail_back_button);
	    backButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	    
		// Set user thumbnail
		final ImageView thumbnail = (ImageView) findViewById(R.id.chat_header_thumbnail);
		thumbnail.setImageBitmap(mCurrentOOI.getProfilePicture());
		// Lazy loading
		if (mCurrentOOI.getProfilePicture() == null) {
			thumbnail.setImageResource(R.drawable.empty_profile_large_icon);
			mCurrentOOI.loadProfilePicture(new OnImageLoadListener() {
				@Override
				public void onImageLoaded(Bitmap bitmap) {
					thumbnail.setImageBitmap(bitmap);
				}
			});
		}

		// Set user name
		final TextView label = (TextView) findViewById(R.id.chat_header_label);
		label.setText(mCurrentOOI.createName());

		mIntentFilter = new IntentFilter(
				"com.google.android.c2dm.intent.RECEIVE");
		mIntentFilter.setPriority(1);
		mIntentFilter.addCategory("nl.skope.android");
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mIntentReceiver, mIntentFilter);
		Bundle bundle = new Bundle();
		bundle.putInt("USER_ID", mCurrentOOI.getId());
		getServiceQueue().postToService(Type.READ_USER_CHAT_MESSAGES, bundle);
	}

	@Override
	public void onStart() {
		super.onStart();
		setupChat();
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mIntentReceiver);
	}

	// The action listener for the EditText widget, to listen for the return key
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// If the action is a key-up event on the return key, send the
			// message
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_UP) {
				String message = view.getText().toString();
				sendMessage(message);
			}
			if (D)
				Log.i(TAG, "END onEditorAction");
			return true;
		}
	};

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	private void sendMessage(String message) {
		// Check that there's actually something to send
		if (message.length() > 0) {
			// Bundle present, extract mId
	    	int userId = getCache().getUser().getId();
			int userToId = mCurrentOOI.getId();

			String username = getCache().getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
			String password = getCache().getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
			String url = String.format("%s/user/%d/chat/%d/", 
								getCache().getProperty("skope_service_url"),
								userId ,
								userToId);
			// Send message
			new ChatPost().execute(url, username, password, message);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
			mOutEditText.setText(mOutStringBuffer);
		}
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");

		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.chat_message);
		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);

		// Initialize the compose field with a listener for the return key
		mOutEditText = (EditText) findViewById(R.id.edit_text_out);
		mOutEditText.setOnEditorActionListener(mWriteListener);

		// Initialize the send button with a listener that for click events
		mSendButton = (Button) findViewById(R.id.button_send);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				TextView view = (TextView) findViewById(R.id.edit_text_out);
				String message = view.getText().toString();
				sendMessage(message);
			}
		});

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public void post(final Type type, final Bundle bundle) {
		switch (type) {
		case READ_USER_CHAT_MESSAGES_START:
			break;
		case READ_USER_CHAT_MESSAGES_END:
			// Get messages from bundle
			String response = bundle.getString("response");
			JSONArray jsonResponse = null;
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

				// Copy the JSON list of messages to our adapter
				for (int i = 0; i < jsonResponse.length(); i++) {
					try {
						JSONObject jsonObject = jsonResponse.getJSONObject(i);

						// Extract data
						int userFromId = Integer.parseInt(jsonObject
								.getString("user_from_id"));

						// Add to list
						if (userFromId == mCurrentOOI.getId()) {
							mConversationArrayAdapter.add(mCurrentOOI
									.getFirstName()
									+ ": "
									+ jsonObject.getString("message"));
						} else {
							mConversationArrayAdapter.add("Me: "
									+ jsonObject.getString("message"));
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			break;
		default:
			super.post(type, bundle);
		}
	}

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String content = intent.getExtras().getString(
					C2DMBroadcastReceiver.C2DM_MESSAGE_CONTENT);
			String sender = intent.getExtras().getString(
					C2DMBroadcastReceiver.C2DM_MESSAGE_SENDER);
			mConversationArrayAdapter.add(sender + ": " + content);

			// Get instance of Vibrator from current Context
			Vibrator v = (Vibrator) context
					.getSystemService(Context.VIBRATOR_SERVICE);

			// Vibrate for 300 milliseconds
			v.vibrate(300);

			abortBroadcast();
		}
	};

	protected class ChatPost extends AsyncTask<String, Void, CustomHttpClient> {
		AsyncTaskListener mListener;
		String mMessage;

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
			mMessage = params[3];
			
			client.addParam("message", mMessage);

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
				// Call back OK
				if (mListener != null) {
					mListener.onTaskDone(true, "");
				}
				mConversationArrayAdapter.add("Me: " + mMessage);
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
				Log.e(SkopeApplication.LOG_TAG, "Failed to post chat: "
						+ client.getErrorMessage());
				// Call back failed
				if (mListener != null) {
					mListener.onTaskDone(false, "Failed to post chat");
				}
			}

		}

		public void setListener(AsyncTaskListener listener) {
			mListener = listener;
		}

	}

}
