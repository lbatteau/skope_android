package nl.skope.android.application;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;


public class ChatMessage {
	private final static String LOG_TAG = "ChatMessage";

	protected int mId;
	protected int mUserFromId;
	protected Date mTimestamp;
	protected String mMessage;
	boolean mIsIncoming = false;
	
	// Flag to add date grouping in list view
	private boolean mIsGroupHeader = false;
	
	public ChatMessage(int id, int userFromId, Date timestamp, String message, boolean isIncoming) {
		this.setId(id);
		this.setUserFromId(userFromId);
		this.setTimestamp(timestamp);
		this.setMessage(message);
		this.setIncoming(isIncoming);
	}
	
	public ChatMessage(boolean isGroupHeader, Date timestamp) {
		this.setGroupHeader(isGroupHeader);
		this.setTimestamp(timestamp);		
	}
	
	
	public ChatMessage(JSONObject jsonObject) throws JSONException {
		this.setId(jsonObject.getInt("id"));
		
		if (!jsonObject.isNull("user_from_id")) {
			this.setUserFromId(Integer.parseInt(jsonObject
								.getString("user_from_id")));
		}
		
		if (!jsonObject.isNull("message")) {
			this.setMessage(jsonObject.getString("message"));
		}
		
		if (!jsonObject.isNull("timestamp")) {
			try {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				this.mTimestamp = df.parse(jsonObject.getString("timestamp"));
			} catch(ParseException e) {
				Log.e(LOG_TAG, "Unreadable date format: " + e);
			}
		}
	}
	
	public String createTimeLabel() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("H:mm");
		return dateFormat.format(this.getTimestamp());
	}
	
	public int getUserFromId() {
		return mUserFromId;
	}

	public void setUserFromId(int userFromId) {
		this.mUserFromId = userFromId;
	}

	public Date getTimestamp() {
		return mTimestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.mTimestamp = timestamp;
	}

	public String getMessage() {
		return mMessage;
	}

	public void setMessage(String message) {
		this.mMessage = message;
	}


	public boolean isIncoming() {
		return mIsIncoming;
	}


	public void setIncoming(boolean isIncoming) {
		this.mIsIncoming = isIncoming;
	}


	public boolean isGroupHeader() {
		return mIsGroupHeader;
	}


	public void setGroupHeader(boolean isGroupHeader) {
		this.mIsGroupHeader = isGroupHeader;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		this.mId = id;
	}


}
