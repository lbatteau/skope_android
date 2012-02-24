package com.skope.skope.ui;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.SkopeApplication;
import com.skope.skope.application.UserPhoto;
import com.skope.skope.application.UserPhoto.OnImageDeleteListener;
import com.skope.skope.util.Type;

public class UserPhotoActivity extends BaseActivity {
	
	private ArrayList<UserPhoto> mUserPhotoList;
	private UserPhotoAdapter mUserPhotoAdapter;
	private Gallery mUserPhotoGallery;
	private int mCurrentPosition;
	private boolean mIsCurrentUser;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		// load the layout
		setContentView(R.layout.user_photo);
		
		
	    // Initial empty list of user photos
	    mUserPhotoList = new ArrayList<UserPhoto>();
	    // User photos adapter
	    mUserPhotoAdapter = new UserPhotoAdapter(getApplicationContext(), R.id.user_photo_grid, mUserPhotoList);
	    // User photos gallery 
	    mUserPhotoGallery = (Gallery) findViewById(R.id.user_photo_gallery);
	    
	    mUserPhotoGallery.setAdapter(mUserPhotoAdapter);
	    
	    mUserPhotoGallery.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				mCurrentPosition = position;
				loadPhoto(position);
				
			}
		});	
	    
		// Load photos
	    mUserPhotoAdapter.clear();
		for(UserPhoto userphoto: Cache.USER_PHOTOS) {
			mUserPhotoAdapter.add(userphoto);
		}

		// Extract the current selected photo from the bundle
		mCurrentPosition = getIntent().getExtras().getInt("position");
		mIsCurrentUser = getIntent().getExtras().getBoolean("is_current_user");
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mUserPhotoGallery.setSelection(mCurrentPosition);
		loadPhoto(mCurrentPosition);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putInt("position", mCurrentPosition);
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mCurrentPosition = savedInstanceState.getInt("position");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    if (mIsCurrentUser) {
		    inflater.inflate(R.menu.user_photo_menu, menu);
	    }
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.delete:
	    	final ProgressDialog dialog = new ProgressDialog(this);
	    	dialog.setMessage("Contacting server...");
	    	
	    	UserPhoto userPhoto = mUserPhotoAdapter.getItem(mCurrentPosition);
	    	userPhoto.delete(new OnImageDeleteListener() {
				
				@Override
				public void onImageDeleted(boolean isSuccess, String message) {
					dialog.dismiss();
					if (isSuccess) {
						if (mCurrentPosition == mUserPhotoAdapter.getCount() - 1) {
							mCurrentPosition--;
						}
						refreshUserPhotos();
					} else {
						Toast.makeText(UserPhotoActivity.this, message, Toast.LENGTH_LONG).show();
					}
				}
				
				@Override
				public void onImageDeleteStart() {
					dialog.show();					
				}
			});
	    	
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	private void refreshUserPhotos() {
		// Empty current photo list
		Cache.USER_PHOTOS.clear();
		// Request new read
		Bundle bundle = new Bundle();
        bundle.putString("USERNAME", getCache().getPreferences().getString(SkopeApplication.PREFS_USERNAME, ""));
        getServiceQueue().postToService(Type.READ_USER_PHOTOS, bundle);
	}

	private void loadPhoto(int position) {
		UserPhoto userPhoto = mUserPhotoAdapter.getItem(position);
		final ImageView userPhotoView = (ImageView) findViewById(R.id.user_photo_view);
		final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
		
		userPhotoView.setImageBitmap(new SoftReference<Bitmap>(userPhoto.getPhoto()).get());
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
	}

	@Override
	public void post(final Type type, final Bundle bundle) {
		switch (type) {
		case READ_USER_PHOTOS_END:
			// Copy user photo list from cache
			mUserPhotoAdapter.clear();
			for(UserPhoto userphoto: Cache.USER_PHOTOS) {
				mUserPhotoAdapter.add(userphoto);
			}
			
			// Go back if no more left
			if (mUserPhotoAdapter.getCount() == 0) {
				finish();
			}
			
			mUserPhotoGallery.setSelection(mCurrentPosition);
			loadPhoto(mCurrentPosition);

			break;
		}
	}
}
