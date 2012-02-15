package com.skope.skope.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ListIterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout.LayoutParams;

import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.User;
import com.skope.skope.application.User.OnImageLoadListener;
import com.skope.skope.application.UserPhoto;
import com.skope.skope.service.LocationService;
import com.skope.skope.util.Type;

public class UserProfileActivity extends BaseActivity {
	private static final String TAG = UserProfileActivity.class.getName();
	
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
	private Gallery mUserPhotoGallery;
	private ProgressDialog mDialog;
	private ProgressBar mPhotosProgressBar;
	

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
		
		// Progress dialog
		mDialog = new ProgressDialog(this);
		
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
		
		// Add photo action
		Button addPhotoButton = (Button) findViewById(R.id.button_add_photo);
		addPhotoButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String labelCamera = getResources().getString(R.string.take_with_camera);
				String labelGallery = getResources().getString(R.string.select_from_gallery);
				final String [] items = new String [] {labelCamera, labelGallery};
				ArrayAdapter<String> adapter = new ArrayAdapter<String> (UserProfileActivity.this, 
														android.R.layout.select_dialog_item, items);
				AlertDialog.Builder builder  = new AlertDialog.Builder(UserProfileActivity.this);
				 
				builder.setTitle("Select Image");
				builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
				public void onClick( DialogInterface dialog, int item ) {
					// Reset global image uri, so we don't use previous values by accident
					mImageUri = Uri.EMPTY;
					if (item == 0) {
						// Capture from camera
						Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						startActivityForResult(Intent.createChooser(intent, "Select camera"), ACTION_ADD_PHOTO_CAMERA);
				    } else { 
				    	// Pick from file
				        Intent intent = new Intent();
				        intent.setType("image/*");
				        intent.setAction(Intent.ACTION_GET_CONTENT);
				        startActivityForResult(Intent.createChooser(intent, "Select gallery"), ACTION_ADD_PHOTO_FILE);
				    }
				}});
				builder.create().show();
			}
		});
		
	    // Initial empty list of user photos
	    mUserPhotoList = new ArrayList<UserPhoto>();
	    // User photos adapter
	    mUserPhotoAdapter = new UserPhotoAdapter(getApplicationContext(), R.id.user_photo_grid, mUserPhotoList);
	    // User photos gallery 
	    mUserPhotoGallery = (Gallery) mMainProfile.findViewById(R.id.user_photo_gallery);
	    
	    mUserPhotoGallery.setAdapter(mUserPhotoAdapter);
	    
	    mUserPhotoGallery.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long id) {
				// Redirect to photo activity
		        Intent i = new Intent(UserProfileActivity.this, UserPhotoActivity.class);
		        i.putExtra("position", position);
	        	startActivity(i);
			}
		});
	    
		// Photos progress bar
		mPhotosProgressBar = (ProgressBar) mMainProfile.findViewById(R.id.photos_progress_bar);
 	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		WeakReference<Bitmap> image = null;
		
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
				image = new WeakReference<Bitmap>((Bitmap) data.getExtras().getParcelable("data"));
				storeProfilePicture(image.get());
				break;
			case ACTION_ADD_PHOTO_CAMERA:
			case ACTION_ADD_PHOTO_FILE:
				try {
					image = new WeakReference<Bitmap>(Media.getBitmap(this.getContentResolver(), data.getData()));
					// Scale down
					int halfFactor = 1;
					int resolution = image.get().getWidth() * image.get().getHeight();
					while (resolution / Math.pow(2, halfFactor) > UserPhoto.USER_PHOTO_MAX_PIXELS) {
						halfFactor++;
					}
					int newHeight = image.get().getHeight() / halfFactor;
					int newWidth = image.get().getWidth() / halfFactor;
					WeakReference<Bitmap> scaledBitmap = new WeakReference<Bitmap>(Bitmap.createScaledBitmap(image.get(), newWidth, newHeight, true));
					image = null;
					storePhoto(scaledBitmap.get());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
				
		} else {
			switch(requestCode) {
			case ACTION_THUMBNAIL_CROP:
			// ProfilePicture cropping failed
			try {
				// Do our own scaling
				WeakReference<Bitmap> bitmap = new WeakReference<Bitmap>(Media.getBitmap(this.getContentResolver(), mImageUri));
				float height = ((float)User.PROFILE_PICTURE_WIDTH / bitmap.get().getWidth()) * bitmap.get().getHeight();
				image = new WeakReference<Bitmap>(Bitmap.createScaledBitmap(bitmap.get(), User.PROFILE_PICTURE_HEIGHT, (int)height, true));
				storeProfilePicture(image.get());
			} catch (FileNotFoundException fnfe) {
				Log.e(SkopeApplication.LOG_TAG, fnfe.toString());
			} catch (IOException ioe) {
				Log.e(SkopeApplication.LOG_TAG, ioe.toString());
			}
			}
		}
			
	}
	
	protected Uri saveBitmapToInternalStorage(Bitmap bitmap) {
		// Write bitmap to file
		try {
			FileOutputStream fos = openFileOutput("uploadtmp", Context.MODE_PRIVATE);
			bitmap.compress(CompressFormat.JPEG, 100, fos);
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.toString());
		}
		
		// Send upload message
		File uploadtmp = getFileStreamPath("uploadtmp");
		return Uri.fromFile(uploadtmp);
	}
	
	protected void storeProfilePicture(Bitmap bitmap) {
		getCache().getUser().setProfilePicture(bitmap);
		// Prevents current profile picture from being reloaded
		// before the new one is finished uploading
		getCache().getUser().setProfilePictureURL("");

		Uri bitmapUri = saveBitmapToInternalStorage(bitmap);
		Bundle bundle = new Bundle();
        bundle.putString(LocationService.IMAGE_UPLOAD_LOCATION, "profile_picture");
        bundle.putString(LocationService.IMAGE_UPLOAD_NAME, "profile_picture");
        bundle.putString(LocationService.IMAGE_UPLOAD_URI, bitmapUri.toString());
        getServiceQueue().postToService(Type.UPLOAD_PROFILE_PICTURE, bundle);
	}
	
	protected void storePhoto(Bitmap bitmap) {
		Uri bitmapUri = saveBitmapToInternalStorage(bitmap);
		Bundle bundle = new Bundle();
        bundle.putString(LocationService.IMAGE_UPLOAD_LOCATION, "photos");
        bundle.putString(LocationService.IMAGE_UPLOAD_NAME, "photo");
        bundle.putString(LocationService.IMAGE_UPLOAD_URI, bitmapUri.toString());
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
		
		refreshUserPhotos(user);
	}

	@Override
	public void post(final Type type, final Bundle bundle) {
		switch (type) {
		case READ_USER_PHOTOS_START:
			// Display progress bar when gallery is empty
			if (mUserPhotoAdapter.isEmpty()) {
				mPhotosProgressBar.setVisibility(View.VISIBLE);
			}
			break;
		case READ_USER_PHOTOS_END:
			// Hide progress bar
			mPhotosProgressBar.setVisibility(View.INVISIBLE);
			// Copy user photo list from cache
			mUserPhotoAdapter.clear();
			// Reverse
			ListIterator<UserPhoto> userPhotosIterator = 
					Cache.USER_PHOTOS.listIterator(Cache.USER_PHOTOS.size());

			while(userPhotosIterator.hasPrevious()) {
				mUserPhotoAdapter.add(userPhotosIterator.previous());
			}
			
			mUserPhotoAdapter.notifyDataSetChanged();
			break;
		case UPLOAD_PROFILE_PICTURE_START:
			mDialog.setMessage(getResources().getString(R.string.user_profile_uploading_image));
			mDialog.show();
			break;
		case UPLOAD_PROFILE_PICTURE_END:
			mDialog.hide();
			String profilePictureURL = bundle.getString("profile_picture_url");
			getCache().getUser().setProfilePictureURL(profilePictureURL);
			mProfilePictureView.setImageBitmap(getCache().getUser().getProfilePicture());
			break;
		case UPLOAD_IMAGE_START:
			mDialog.setMessage(getResources().getString(R.string.user_profile_uploading_image));
			mDialog.show();
			break;
		case UPLOAD_IMAGE_END:
			mDialog.hide();
			User user = getCache().getUser();
			refreshUserPhotos(user);
		}
	}
	
	private void refreshUserPhotos(User user) {
		// Empty current photo list
		Cache.USER_PHOTOS.clear();
		// Request new read
		Bundle bundle = new Bundle();
        bundle.putString("USERNAME", user.getUserEmail());
        getServiceQueue().postToService(Type.READ_USER_PHOTOS, bundle);
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
