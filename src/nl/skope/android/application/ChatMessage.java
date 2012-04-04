package nl.skope.android.application;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


public class ChatMessage implements Parcelable {
	private final static String TAG = "ChatMessage";

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
				Log.e(TAG, "Unreadable date format: " + e);
			}
		}
	}
	
	public ChatMessage(Parcel in) {
		if (this.mIsGroupHeader) {
			this.mId = in.readInt();
		}
		String timestamp = in.readString();
		if (!timestamp.equals("")) {
			this.mTimestamp = new Date(Long.parseLong(timestamp));
		}
		this.mMessage = in.readString();
		this.mIsIncoming = Boolean.parseBoolean(in.readString());
		this.mIsGroupHeader = Boolean.parseBoolean(in.readString());
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if (!this.mIsGroupHeader) {
			dest.writeInt(this.mId);
		}
		dest.writeString(this.mTimestamp != null ? String.valueOf(this.mTimestamp.getTime()) : "");
		dest.writeString(this.mMessage != null ? this.mMessage : "");
		dest.writeString(String.valueOf(this.mIsIncoming));
		dest.writeString(String.valueOf(this.mIsGroupHeader));
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	public static final Parcelable.Creator<ChatMessage> CREATOR = new Parcelable.Creator<ChatMessage>() {
        public ChatMessage createFromParcel(Parcel in) {
        return new ChatMessage(in);
        }

        public ChatMessage[] newArray(int size) {
        return new ChatMessage[size];
        }
    };
    
	public String createTimeLabel() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("H:mm");
		return dateFormat.format(this.getTimestamp());
	}
	
	public String createDateLabel() {
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
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
