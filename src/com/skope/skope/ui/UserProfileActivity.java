package com.skope.skope.ui;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.http.HttpStatus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.skope.skope.R;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.User;
import com.skope.skope.application.User.OnThumbnailLoadListener;
import com.skope.skope.http.CustomHttpClient;
import com.skope.skope.http.CustomHttpClient.RequestMethod;
import com.skope.skope.util.Type;

public class UserProfileActivity extends BaseActivity {
	public static final int ACTION_SELECT_IMAGE = 0;
	
	protected ImageView mThumbnailView;

	OnThumbnailLoadListener mThumbnailListener = new OnThumbnailLoadListener() {
		@Override
		public void onThumbnailLoaded(Bitmap thumbnail) {
			mThumbnailView.setImageBitmap(thumbnail);
			mThumbnailView.invalidate();
		}
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		// load up the layout
		setContentView(R.layout.user);
		
		// Set profile picture
		mThumbnailView = (ImageView) findViewById(R.id.icon);
		
		// Thumbnail button action
		mThumbnailView.setOnClickListener(new OnClickListener() {
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
				Bitmap thumbnail = null;
				try {
					Bitmap bitmap = Media.getBitmap(this.getContentResolver(), selectedImage);
					float height = (100f / bitmap.getWidth()) * bitmap.getHeight();
					thumbnail = Bitmap.createScaledBitmap(bitmap, 100, (int)height, true);
				} catch (FileNotFoundException e) {
					Log.e(SkopeApplication.LOG_TAG, e.toString());
				} catch (IOException e) {
					Log.e(SkopeApplication.LOG_TAG, e.toString());
				}

				if (thumbnail != null) {
					getCache().getUser().setThumbnail(thumbnail);
					mThumbnailView.setImageBitmap(thumbnail);
					Bundle bundle = new Bundle();
		            bundle.putString("NAME", "thumbnail");
		            getCache().getImageUploadQueue().add(thumbnail);
			        getServiceQueue().postToService(Type.UPLOAD_IMAGE, bundle);
				}
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
