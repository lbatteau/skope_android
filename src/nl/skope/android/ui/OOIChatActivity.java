package nl.skope.android.ui;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import nl.skope.android.R;
import nl.skope.android.application.ChatMessage;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.User;
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
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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

	private User mUser;
	
	// C2DM Intent Filter
	private IntentFilter mIntentFilter;

	// Layout Views
	private ListView mConversationView;
	private EditText mOutEditText;
	private Button mSendButton;

	private LayoutInflater mInflater;
	
	// Array adapter for the conversation thread
	private ChatArrayAdapter mChatArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.chat);
		
		if (getIntent() != null && getIntent().hasExtra(SkopeApplication.BUNDLEKEY_USER)) {
			// User already retrieved and passed inside intent
			mUser = getIntent().getParcelableExtra(SkopeApplication.BUNDLEKEY_USER);
			mUser.setCache(getCache());

		    update();
		    
			// Back button should go to detail
		    View backButton = findViewById(R.id.detail_back_button);
		    backButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
				}
			});		


		} else if (getIntent() != null && getIntent().hasExtra(SkopeApplication.BUNDLEKEY_USERID)) {
			// User not yet retrieved, but user ID passed
			// Post request for retrieval of user
			getServiceQueue().postToService(Type.READ_USER, getIntent().getExtras());
			
		}
		
		mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		mIntentFilter = new IntentFilter(
				"com.google.android.c2dm.intent.RECEIVE");
		mIntentFilter.setPriority(1);
		mIntentFilter.addCategory("nl.skope.android");
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mUser != null && checkCacheSanity()) {
			registerReceiver(mIntentReceiver, mIntentFilter);
			Bundle bundle = new Bundle();
			bundle.putInt(SkopeApplication.BUNDLEKEY_USERID, mUser.getId());
			bundle.putBoolean(SkopeApplication.BUNDLEKEY_MARKASREAD, true);
			getServiceQueue().postToService(Type.READ_USER_CHAT_MESSAGES, bundle);
		}
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
	
	protected void update() {
		// Set user thumbnail
		final ImageView thumbnail = (ImageView) findViewById(R.id.chat_header_thumbnail);
		thumbnail.setImageBitmap(mUser.getProfilePicture());
		// Lazy loading
		if (mUser.getProfilePicture() == null) {
			thumbnail.setImageResource(R.drawable.empty_profile_large_icon);
			mUser.loadProfilePicture(new OnImageLoadListener() {
				@Override
				public void onImageLoaded(Bitmap bitmap) {
					thumbnail.setImageBitmap(bitmap);
				}
			});
		}

		// Set user name
		final TextView label = (TextView) findViewById(R.id.chat_header_label);
		label.setText(mUser.createName());
	}

	// The action listener for the EditText widget, to listen for the return key
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// If the action is a key-up event on the return key, send the
			// message
			if (actionId == EditorInfo.IME_ACTION_DONE ||
				(actionId == EditorInfo.IME_NULL
				 && event.getAction() == KeyEvent.ACTION_UP)) {
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
			int userToId = mUser.getId();

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
		mChatArrayAdapter = new ChatArrayAdapter(this, R.layout.chat_message_to);
		
		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mChatArrayAdapter);

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
		case READ_USER_START:
			break;
		case READ_USER_END:
			mUser = bundle.getParcelable(SkopeApplication.BUNDLEKEY_USER);
			update();
			onResume();
			// Back button should go to detail
		    View backButton = findViewById(R.id.detail_back_button);
		    backButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// Redirect to list activity
			        Intent i = new Intent(OOIChatActivity.this, OOIDetailMapActivity.class);
		        	Bundle bundle = new Bundle();
			        bundle.putParcelable(SkopeApplication.BUNDLEKEY_USER, mUser);
			        i.putExtras(bundle);
					startActivity(i);
					finish();
				}
			});	
			break;
		case READ_USER_CHAT_MESSAGES_START:
			break;
		case READ_USER_CHAT_MESSAGES_END:
			// Get messages from bundle
			String response = bundle.getString(SkopeApplication.BUNDLEKEY_RESPONSE);
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
				mChatArrayAdapter.clear();
				ChatMessage lastChat = null;
				for (int i = 0; i < jsonResponse.length(); i++) {
					try {
						JSONObject jsonObject = jsonResponse.getJSONObject(i);
						ChatMessage chat = new ChatMessage(jsonObject);
						if (i == 0) {
							ChatMessage dateGroup = new ChatMessage(true, chat.getTimestamp());
							mChatArrayAdapter.add(dateGroup);
						} else if(lastChat != null && isMessageOnDifferentDay(chat, lastChat)) {
							ChatMessage dateGroup = new ChatMessage(true, chat.getTimestamp());
							mChatArrayAdapter.add(dateGroup);
						}
						mChatArrayAdapter.add(chat);
						lastChat = chat;
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

	private boolean isMessageOnDifferentDay(ChatMessage message1, ChatMessage message2) {
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(message1.getTimestamp());
		cal2.setTime(message2.getTimestamp());
		return !(cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
		         cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
	}
	
	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean isMessageAlreadyPresent = false;
			
			// Extract message ID
			int id = intent.getExtras().getInt(C2DMBroadcastReceiver.C2DM_MESSAGE_ID);
			
			// Extract sender user ID
			int userId = Integer.parseInt(intent.getExtras().getString(C2DMBroadcastReceiver.C2DM_MESSAGE_USERID));

			// Check if sender ID matches the selected user ID
			if (userId == mUser.getId()) {
				// Match
				
				// It is possible that at the moment this C2DM message is received, 
				// the chat message has already been retrieved from the server.
				// Check if already present.
				for (int i=0; i<mChatArrayAdapter.getCount(); i++) {
					if (mChatArrayAdapter.getItem(i).getId() == id) {
						// Message already present
						isMessageAlreadyPresent = true;
						break;
					}
				}
				
				// Only add message to conversation if not already present
				if (!isMessageAlreadyPresent) {
					// Extract message information from bundle
					String content = intent.getExtras().getString(C2DMBroadcastReceiver.C2DM_MESSAGE_CONTENT);
					Date timestamp = null;
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					try {
						timestamp = df.parse(intent.getExtras().getString(C2DMBroadcastReceiver.C2DM_MESSAGE_TIMESTAMP));
					} catch (ParseException e) {
						Log.e(TAG, e.toString());
					}
					
					// Create chat message
					ChatMessage chat = new ChatMessage(id, userId, timestamp, content, true);

					// Check if we need to add a date group header
					int chatArraySize = mChatArrayAdapter.getCount();
					// Check if the conversation is empty, 
					// or the last chat is on a different day
					if (chatArraySize == 0
						|| isMessageOnDifferentDay(chat, 
									mChatArrayAdapter.getItem(chatArraySize - 1))) {
						// Add header
						ChatMessage dateGroup = new ChatMessage(true, chat.getTimestamp());
						mChatArrayAdapter.add(dateGroup);
					}
					mChatArrayAdapter.add(chat);

				}

				// Get instance of Vibrator from current Context
				Vibrator v = (Vibrator) context
						.getSystemService(Context.VIBRATOR_SERVICE);

				// Vibrate for 300 milliseconds
				v.vibrate(300);

				abortBroadcast();
			}
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

				// Extract chat message from response
				ChatMessage chat = null;
		        try {
		        	JSONObject jsonObject = new JSONObject(client.getResponse());
		        	chat = new ChatMessage(jsonObject);
		        } catch (JSONException e) {
					// Log exception
					Log.e(SkopeApplication.LOG_TAG, e.toString());
				}
		        
		        if (chat != null) {
					int chatArraySize = mChatArrayAdapter.getCount();
					if (chatArraySize == 0
						|| isMessageOnDifferentDay(chat, 
									mChatArrayAdapter.getItem(chatArraySize - 1))) {
						ChatMessage dateGroup = new ChatMessage(true, chat.getTimestamp());
						mChatArrayAdapter.add(dateGroup);
					}
					mChatArrayAdapter.add(chat);
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
	
	private class ChatArrayAdapter extends ArrayAdapter<ChatMessage> {
		SimpleDateFormat mDateFormat = new SimpleDateFormat("MMM d");

		public ChatArrayAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row;
	 
			ChatMessage chat = getItem(position);
			
			if (chat.getUserFromId() == mUser.getId()) {
				// From chat
				row = mInflater.inflate(R.layout.chat_message_from, null);
			} else {
				// To chat
				row = mInflater.inflate(R.layout.chat_message_to, null);
			}
			
			if (chat.isGroupHeader()) {
				View header = row.findViewById(R.id.group_header);
				header.setVisibility(View.VISIBLE);
				View message = row.findViewById(R.id.chat_message);
				message.setVisibility(View.GONE);
				TextView dateGroup = (TextView) row.findViewById(R.id.date_group); 
				dateGroup.setText(mDateFormat.format(chat.getTimestamp()));			
			} else {
				TextView message = (TextView) row.findViewById(R.id.message);
				message.setText(chat.getMessage());
				
				TextView timestamp = (TextView) row.findViewById(R.id.timestamp);
				timestamp.setText(chat.createTimeLabel());				
			}
			
			return row;
		}
		
	}
	
}
