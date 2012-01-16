package com.skope.skope.util;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.skope.skope.R;
import com.skope.skope.application.User;
import com.skope.skope.application.User.OnThumbnailLoadListener;

public class Views {
	/**
	 * This method fills out the profile for the given User.
	 * 
	 * @param activity The activity's content must include a layout as 
	 * 				   defined in layout/user_profile.xml.
	 * @param user     The user who's profile should be filled out.
	 */
	public static void createUserProfile(Activity activity, User user) {
		TextView userNameText = (TextView) activity.findViewById(R.id.username_text);
		userNameText.setText(user.createName());
		TextView status = (TextView) activity.findViewById(R.id.status);
		status.setText(user.createLabelStatus());
		final ImageView icon = (ImageView) activity.findViewById(R.id.icon);
		icon.setImageBitmap(user.getThumbnail());
		// Lazy loading
		if (user.getThumbnail() == null) {
			user.loadThumbnail(new OnThumbnailLoadListener() {

				@Override
				public void onThumbnailLoaded() {
					icon.invalidate();
				}
			});
		}
		
		// Fill user info block with items that are present
		ViewGroup userInfoBlock = (ViewGroup) activity.findViewById(R.id.user_info_block);
		// But first clear...
		userInfoBlock.removeAllViews();
		
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		String dateOfBirth = user.createLabelDateOfBirth();
		TextView userInfoItem;
		if (dateOfBirth != null && !dateOfBirth.equals("")) {
			userInfoItem = (TextView) inflater.inflate(
					R.layout.user_info_item, null);
			userInfoItem.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.details_profile_icon_dateofbirth, 0, 0, 0);
			userInfoItem.setText(String.valueOf(dateOfBirth));
			userInfoBlock.addView(userInfoItem);
		}

		String relationship = user.getRelationship();
		if (relationship != null && !relationship.equals("")) {
			userInfoItem = (TextView) inflater.inflate(
					R.layout.user_info_item, null);
			userInfoItem.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.details_profile_icon_relationship, 0, 0, 0);
			userInfoItem.setText(user.getRelationship());
			userInfoBlock.addView(userInfoItem);
		}

		String homeTown = user.getHomeTown(); 
		if (homeTown != null && !homeTown.equals("")) {
			userInfoItem = (TextView) inflater.inflate(
					R.layout.user_info_item, null);
			userInfoItem.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.details_profile_icon_hometown, 0, 0, 0);
			userInfoItem.setText(homeTown);
			userInfoBlock.addView(userInfoItem);
		}

		String work = user.getWork(); 
		if (work != null && !work.equals("")) {
			userInfoItem = (TextView) inflater.inflate(
					R.layout.user_info_item, null);
			userInfoItem.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.details_profile_icon_job, 0, 0, 0);
			userInfoItem.setText(work);
			userInfoBlock.addView(userInfoItem);
		}

		String education = user.getEducation(); 
		if (education != null && !education.equals("")) {
			userInfoItem = (TextView) inflater.inflate(
					R.layout.user_info_item, null);
			userInfoItem.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.details_profile_icon_college, 0, 0, 0);
			userInfoItem.setText(education);
			userInfoBlock.addView(userInfoItem);
		}
	}

}
