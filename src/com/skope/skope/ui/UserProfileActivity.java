package com.skope.skope.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.skope.skope.R;
import com.skope.skope.application.User;

public class UserProfileActivity extends BaseActivity {
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		// load up the layout
		setContentView(R.layout.user);
		
		User user = getCache().getUser();
		
		TextView userNameText = (TextView) findViewById(R.id.username_text);
		userNameText.setText(user.createName());
        TextView status = (TextView) findViewById(R.id.status);
        status.setText(user.createLabelStatus());
        ImageView icon = (ImageView) findViewById(R.id.icon);
        icon.setImageBitmap(user.getThumbnail());

        // Fill user info block with items that are present
        ViewGroup userInfoBlock = (ViewGroup) findViewById(R.id.user_info_block);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        String dateOfBirth = user.createLabelDateOfBirth();
        if (!dateOfBirth.equals("")) {
            View ageView = inflater.inflate(R.layout.user_info_item, null);
            TextView ageText = (TextView) ageView.findViewById(R.id.user_info_description);
            ageText.setText(String.valueOf(dateOfBirth));
            userInfoBlock.addView(ageView);
        }
        
	}

}
