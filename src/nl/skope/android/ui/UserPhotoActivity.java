package nl.skope.android.ui;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

import nl.skope.android.application.Cache;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.UserPhoto;
import nl.skope.android.application.UserPhoto.OnImageDeleteListener;
import nl.skope.android.http.ThumbnailManager;
import nl.skope.android.util.Type;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
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

import nl.skope.android.R;

public class UserPhotoActivity extends BaseActivity {
	private static final String TAG = UserPhotoActivity.class.getName();
	
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
	    // Set up thumbnail manager
	    ThumbnailManager thumbnailManager = new ThumbnailManager(getCache());
	    // User photos adapter
	    mUserPhotoAdapter = new UserPhotoAdapter(getApplicationContext(), R.id.user_photo_grid, mUserPhotoList, thumbnailManager);
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
        bundle.putInt(SkopeApplication.BUNDLEKEY_USERID, getCache().getUser().getId());
        getServiceQueue().postToService(Type.READ_USER_PHOTOS, bundle);
	}

	private void loadPhoto(int position) {
		UserPhoto userPhoto = null;
		try {
			userPhoto = mUserPhotoAdapter.getItem(position);
		} catch (ArrayIndexOutOfBoundsException e) {
			Log.e(TAG, "Couldn't find photo for current position " + position);
			finish();
			return;
		}
		
		final ImageView userPhotoView = (ImageView) findViewById(R.id.user_photo_view);
		
		userPhotoView.setImageBitmap(new SoftReference<Bitmap>(userPhoto.getPhoto()).get());
		if (userPhoto.getPhoto() == null) {
			userPhoto.loadPhoto(new UserPhoto.OnImageLoadListener() {
				
				@Override
				public void onImageLoaded(Bitmap image) {
					ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
					progressBar.setVisibility(View.INVISIBLE);
					userPhotoView.setImageBitmap(image);
				}

				@Override
				public void onImageLoadStart() {
					ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
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
