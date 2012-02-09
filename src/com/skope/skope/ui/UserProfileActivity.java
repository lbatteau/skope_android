package com.skope.skope.ui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TableLayout.LayoutParams;

import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.User;
import com.skope.skope.application.UserPhoto;
import com.skope.skope.application.User.OnImageLoadListener;
import com.skope.skope.service.LocationService;
import com.skope.skope.util.Type;

public class UserProfileActivity extends BaseActivity {
	private static final String TAG = LocationService.class.getName();
	
	public static final int ACTION_PICK_THUMBNAIL_CAMERA = 0;
	public static final int ACTION_PICK_THUMBNAIL_FILE = 1;
	public static final int ACTION_THUMBNAIL_CROP = 2;
	public static final int ACTION_ADD_PHOTO_CAMERA = 3;
	public static final int ACTION_ADD_PHOTO_FILE = 4;
	
	private ImageView mProfilePictureView;
	private View mMainProfile;
    private LayoutInflater mInflater;
    private Uri mImageUri = Uri.EMPTY;
	private ArrayList<UserPhoto> mUserPhotoList;
	private UserPhotoAdapter mUserPhotoAdapter;
	private GridView mUserPhotoGrid;
	

	 OnImageLoadListener mProfilePictureListener = new OnImageLoadListener() {
		@Override
		public void onImageLoaded(Bitmap profilePicture) {
			mProfilePictureView.setImageBitmap(profilePicture);
			mProfilePictureView.invalidate();
		}
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		// load the layout
		setContentView(R.layout.user);
		
		/// Add user profile
		mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mMainProfile = mInflater.inflate(R.layout.user_profile, null);
		addContentView(mMainProfile, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		// Profile picture ImageView
		mProfilePictureView = (ImageView) findViewById(R.id.icon);
		
		// Profile picture button action
		mProfilePictureView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String labelCamera = getResources().getString(R.string.take_with_camera);
				String labelGallery = getResources().getString(R.string.select_from_gallery);
				final String [] items = new String [] {labelCamera, labelGallery};
				ArrayAdapter<String> adapter = new ArrayAdapter<String> (UserProfileActivity.this, 
														android.R.layout.select_dialog_item, items);
				AlertDialog.Builder builder  = new AlertDialog.Builder(UserProfileActivity.this);
				 
				builder.setTitle("Select Image");
				builder.setAdapter( adapter, new DialogInterface.OnClickListener() {
				public void onClick( DialogInterface dialog, int item ) {
					// Reset global image uri, so we don't use previous values by accident
					mImageUri = Uri.EMPTY;
					if (item == 0) {
						// Capture from camera
						Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						startActivityForResult(Intent.createChooser(intent, "Select camera"), ACTION_PICK_THUMBNAIL_CAMERA);
				    } else { 
				    	// Pick from file
				        Intent intent = new Intent();
				        intent.setType("image/*");
				        intent.setAction(Intent.ACTION_GET_CONTENT);
				        startActivityForResult(Intent.createChooser(intent, "Select gallery"), ACTION_PICK_THUMBNAIL_FILE);
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
		
	    // Initial empty list of user photos
	    mUserPhotoList = new ArrayList<UserPhoto>();
	    // User photos adapter
	    mUserPhotoAdapter = new UserPhotoAdapter(this, R.id.user_photo_grid, mUserPhotoList);
	    // User photos gallery 
	    mUserPhotoGrid = (GridView) mMainProfile.findViewById(R.id.user_photo_grid);
	    mUserPhotoGrid.setAdapter(mUserPhotoAdapter);
	    
	    mUserPhotoGrid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long id) {
				UserPhoto userPhoto = mUserPhotoAdapter.getItem(position);
				View userPhotoLayout = mInflater.inflate(R.layout.user_photo, null);
				final ImageView userPhotoView = (ImageView) userPhotoLayout.findViewById(R.id.user_photo_view);
				userPhotoView.setImageBitmap(new SoftReference<Bitmap>(userPhoto.getPhoto()).get());
				final ProgressBar progressBar = (ProgressBar) userPhotoLayout.findViewById(R.id.progress);
				
				if (userPhoto.getPhoto() == null) {
					userPhoto.loadPhoto(new UserPhoto.OnImageLoadListener() {
						
						@Override
						public void onImageLoaded(Bitmap image) {
							progressBar.setVisibility(View.INVISIBLE);
							userPhotoView.setImageBitmap(image);
						}

						@Override
						public void onImageLoadStart() {
							progressBar.setVisibility(View.VISIBLE);
						}
					},
					getWindowManager().getDefaultDisplay().getWidth(),
					getWindowManager().getDefaultDisplay().getHeight());
				}
				
				AlertDialog.Builder builder = new AlertDialog.Builder(UserProfileActivity.this);
				builder.setView(userPhotoLayout)
					   .setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						})
						.create()
						.show();
			}
		});	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Bitmap image = null;
		
		if (resultCode == Activity.RESULT_OK) {
			switch(requestCode) {
			case ACTION_PICK_THUMBNAIL_CAMERA:
				mImageUri = data.getData();
				crop(mImageUri);
				break;
			case ACTION_PICK_THUMBNAIL_FILE:
				mImageUri = data.getData();
				crop(mImageUri);
				break;
			case ACTION_THUMBNAIL_CROP:
				image = data.getExtras().getParcelable("data");
				storeProfilePicture(image);
			}
		} else {
			switch(requestCode) {
			case ACTION_THUMBNAIL_CROP:
			// ProfilePicture cropping failed
			try {
				// Do our own scaling
				Bitmap bitmap = Media.getBitmap(this.getContentResolver(), mImageUri);
				float height = ((float)User.PROFILE_PICTURE_WIDTH / bitmap.getWidth()) * bitmap.getHeight();
				image = Bitmap.createScaledBitmap(bitmap, User.PROFILE_PICTURE_HEIGHT, (int)height, true);
			} catch (FileNotFoundException fnfe) {
				Log.e(SkopeApplication.LOG_TAG, fnfe.toString());
			} catch (IOException ioe) {
				Log.e(SkopeApplication.LOG_TAG, ioe.toString());
			}
			}
		}
			
	}
	
	protected void storeProfilePicture(Bitmap image) {
		getCache().getUser().setProfilePicture(image);
		mProfilePictureView.setImageBitmap(image);

		Bundle bundle = new Bundle();
        bundle.putString(LocationService.IMAGE_UPLOAD_LOCATION, "profile_picture");
        bundle.putString(LocationService.IMAGE_UPLOAD_NAME, "profile_picture");
        bundle.putParcelable(LocationService.IMAGE_UPLOAD_BITMAP, image);
        getServiceQueue().postToService(Type.UPLOAD_IMAGE, bundle);
	}

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	Cache.USER_PHOTOS.clear();
    }

	@Override
	protected void onResume() {
		super.onResume();
		
		User user = getCache().getUser();
		user.createUserProfile(mMainProfile, mInflater);
		user.loadProfilePicture(mProfilePictureListener);
			
		// Empty current photo list
		Cache.USER_PHOTOS.clear();
		// Request new read
		Bundle bundle = new Bundle();
        bundle.putString("USERNAME", user.getUserEmail());
        getServiceQueue().postToService(Type.READ_USER_PHOTOS, bundle);
		
	}

	@Override
	public void post(final Type type, final Bundle bundle) {
		switch (type) {
		case READ_USER_PHOTOS_END:
			// Copy user photo list from cache
			mUserPhotoAdapter.clear();
			for (UserPhoto userPhoto: Cache.USER_PHOTOS) {
				mUserPhotoAdapter.add(userPhoto);
			}
			break;
		}
	}

	private void crop(Uri imageUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");
 
        intent.setData(imageUri);
        intent.putExtra("outputX", User.PROFILE_PICTURE_WIDTH);
        intent.putExtra("outputY", User.PROFILE_PICTURE_HEIGHT);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", true);
        
        startActivityForResult(Intent.createChooser(intent, "Select crop method"), ACTION_THUMBNAIL_CROP);
    }
}
