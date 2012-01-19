package com.skope.skope.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import com.skope.skope.R;
import com.skope.skope.application.User;
import com.skope.skope.application.User.OnThumbnailLoadListener;

public class UserProfileActivity extends BaseActivity {
	public static final int ACTION_SELECT_IMAGE = 0;
	
	protected ImageView mThumbnail;

	OnThumbnailLoadListener mThumbnailListener = new OnThumbnailLoadListener() {
		@Override
		public void onThumbnailLoaded(Bitmap thumbnail) {
			mThumbnail.setImageBitmap(thumbnail);
			mThumbnail.invalidate();
		}
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		// load up the layout
		setContentView(R.layout.user);
		
		// Set profile picture
		mThumbnail = (ImageView) findViewById(R.id.icon);
		
		// Thumbnail button action
		mThumbnail.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(Intent.ACTION_PICK, 
						android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), 
						ACTION_SELECT_IMAGE);
			}
		});
		
		// Edit button action
		Button editButton = (Button) findViewById(R.id.button_edit);
		editButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Replace normal view by edit view
				Intent intent = new Intent(UserProfileActivity.this, UserFormActivity.class);
	        	startActivity(intent);
			}
		});
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ACTION_SELECT_IMAGE)
			if (resultCode == Activity.RESULT_OK) {
				Uri selectedImage = data.getData();
				mThumbnail.setImageURI(selectedImage);
			}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		User user = getCache().getUser();
		user.createUserProfile(this);
		user.loadThumbnail(mThumbnailListener);
			
	}

}
