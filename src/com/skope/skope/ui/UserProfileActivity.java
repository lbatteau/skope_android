package com.skope.skope.ui;

import android.os.Bundle;

import com.skope.skope.R;
import com.skope.skope.application.User;
import com.skope.skope.util.Views;

public class UserProfileActivity extends BaseActivity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		// load up the layout
		setContentView(R.layout.user);
	}

	@Override
	protected void onResume() {
		super.onResume();
		User user = getCache().getUser();
		Views.createUserProfile(this, user);
	}

}
