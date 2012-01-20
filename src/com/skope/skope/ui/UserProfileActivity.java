package com.skope.skope.ui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.skope.skope.R;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.User;
import com.skope.skope.application.User.OnThumbnailLoadListener;
import com.skope.skope.util.Type;

public class UserProfileActivity extends BaseActivity {
	public static final int ACTION_PICK_CAMERA = 0;
	public static final int ACTION_PICK_FILE = 1;
	public static final int ACTION_CROP = 2;
	
	private ImageView mThumbnailView;
	private Uri mThumbnailUri;

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
				final String [] items        = new String [] {"Take with camera", "Select from gallery"};
				ArrayAdapter<String> adapter = new ArrayAdapter<String> (UserProfileActivity.this, android.R.layout.select_dialog_item, items);
				AlertDialog.Builder builder  = new AlertDialog.Builder(UserProfileActivity.this);
				 
				builder.setTitle("Select Image");
				builder.setAdapter( adapter, new DialogInterface.OnClickListener() {
				public void onClick( DialogInterface dialog, int item ) {
					if (item == 0) {
						//create new Intent
						Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						startActivityForResult(Intent.createChooser(intent, "Select camera"), ACTION_PICK_CAMERA);
				    } else { //pick from file
				       Intent intent = new Intent();
				       intent.setType("image/*");
				       intent.setAction(Intent.ACTION_GET_CONTENT);
				       startActivityForResult(Intent.createChooser(intent, "Select gallery"), ACTION_PICK_FILE);
				    }
				}});
				builder.create().show();
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
		if (resultCode == Activity.RESULT_OK) {
			mThumbnailUri = data.getData();
			Bitmap thumbnail = null;
			switch(requestCode) {
			case ACTION_PICK_CAMERA:
			case ACTION_PICK_FILE:
				crop(mThumbnailUri);
				break;
			case ACTION_CROP:
				Bundle extras = data.getExtras();
				 
                if (extras != null) {
                    thumbnail = extras.getParcelable("data");
 
                    getCache().getUser().setThumbnail(thumbnail);
    				mThumbnailView.setImageBitmap(thumbnail);
    				Bundle bundle = new Bundle();
    	            bundle.putString("NAME", "thumbnail");
    	            getCache().getImageUploadQueue().add(thumbnail);
    		        getServiceQueue().postToService(Type.UPLOAD_IMAGE, bundle);
                }
			}
			
			/*if (thumbnail != null) {
				getCache().getUser().setThumbnail(thumbnail);
				mThumbnailView.setImageBitmap(thumbnail);
				Bundle bundle = new Bundle();
	            bundle.putString("NAME", "thumbnail");
	            getCache().getImageUploadQueue().add(thumbnail);
		        getServiceQueue().postToService(Type.UPLOAD_IMAGE, bundle);
			}*/
			
			
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		User user = getCache().getUser();
		user.createUserProfile(this);
		user.loadThumbnail(mThumbnailListener);
			
	}

	private void crop(Uri imageUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");
 
        intent.setData(imageUri);
        intent.putExtra("outputX", 200);
        intent.putExtra("outputY", 200);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", true);
        startActivityForResult(Intent.createChooser(intent, "Select crop method"), ACTION_CROP);
    }
}
