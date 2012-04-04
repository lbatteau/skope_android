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

package nl.skope.android.util;

/***
 * Defines all the message types used in the framework.
 */
public enum Type {

	/***
	 * UI to Service messages.
	 */
	READ_USER, FIND_OBJECTS_OF_INTEREST, LOCATION_CHANGED, UPDATE_NOTIFICATION, DO_LONG_TASK, UPLOAD_PROFILE_PICTURE, UPLOAD_IMAGE, READ_USER_PHOTOS, READ_USER_FAVORITES, READ_USER_CHATS, READ_USER_CHAT_MESSAGES, ENABLE_GPS, DISABLE_GPS,

	/***
	 * Service to UI messages.
	 */
	READ_USER_START, READ_USER_END, FIND_OBJECTS_OF_INTEREST_START, FIND_OBJECTS_OF_INTEREST_FINISHED, UNDETERMINED_LOCATION, UPDATE_LONG_TASK, UPDATE_QUEUE, SHOW_DIALOG, UPLOAD_PROFILE_PICTURE_START, UPLOAD_PROFILE_PICTURE_END, UPLOAD_IMAGE_START, UPLOAD_IMAGE_END, READ_USER_PHOTOS_START, READ_USER_PHOTOS_END, READ_USER_FAVORITES_START, READ_USER_FAVORITES_END, READ_USER_CHATS_START, READ_USER_CHATS_END, READ_USER_CHAT_MESSAGES_START, READ_USER_CHAT_MESSAGES_END,

	/***
	 * UI Dialogs.
	 */
	DIALOG_STATUS,

	/***
	 * Do not handle this message.
	 */
	UNKNOWN;

	/***
	 * Get the Type from a given Integer value.
	 * 
	 * @param input
	 *            Integer.ordinal value of the UiEvent
	 * @return Relevant Type or UNKNOWN if the Integer is not known.
	 */
	public static Type getType(final int input) {
		if (input < 0 || input > UNKNOWN.ordinal()) {
			return UNKNOWN;
		} else {
			return Type.values()[input];
		}
	}
}
