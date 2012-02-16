package com.skope.skope.ui;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.skope.skope.R;
import com.skope.skope.application.Cache;
import com.skope.skope.application.UserPhoto;

public class UserPhotoActivity extends BaseActivity {
	
	private ArrayList<UserPhoto> mUserPhotoList;
	private UserPhotoAdapter mUserPhotoAdapter;
	private Gallery mUserPhotoGallery;
	private int mCurrentPosition;
	
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
	    
	    mUserPhotoGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int position, long arg3) {
				mCurrentPosition = position;
				loadPhoto(position);
				
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
		});	
	    
		// Load photos
	    mUserPhotoAdapter.clear();
		for(UserPhoto userphoto: Cache.USER_PHOTOS) {
			mUserPhotoAdapter.add(userphoto);
		}

		// Extract the current selected photo from the bundle
		mCurrentPosition = getIntent().getExtras().getInt("position");
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mUserPhotoGallery.setSelection(mCurrentPosition);
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
	
}
