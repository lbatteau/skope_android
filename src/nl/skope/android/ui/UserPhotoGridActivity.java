package nl.skope.android.ui;

import java.util.ArrayList;

import nl.skope.android.application.Cache;
import nl.skope.android.application.UserPhoto;
import nl.skope.android.http.ThumbnailManager;

import android.content.Intent;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import nl.skope.android.R;

public class UserPhotoGridActivity extends BaseActivity {
	
	private GridView mUserPhotoGrid;    
	private UserPhotoAdapter mUserPhotoAdapter;
	private ArrayList<UserPhoto> mUserPhotoList;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		// Load layout
		setContentView(R.layout.user_photo_grid);
		
	    // Back button
	    View backButton = findViewById(R.id.detail_back_button);
	    backButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	    
	    // Initial empty list of user photos
	    mUserPhotoList = new ArrayList<UserPhoto>();
	    // Set up thumbnail manager
	    ThumbnailManager thumbnailManager = new ThumbnailManager(getCache());
	    // User photos adapter
	    mUserPhotoAdapter = new UserPhotoAdapter(this, R.id.user_photo_grid, mUserPhotoList, thumbnailManager);
	    // User photos gallery 
	    mUserPhotoGrid = (GridView) findViewById(R.id.user_photo_grid);
	    mUserPhotoGrid.setAdapter(mUserPhotoAdapter);
	    
	    mUserPhotoGrid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long id) {
				// Redirect to photo activity
		        Intent i = new Intent(UserPhotoGridActivity.this, UserPhotoActivity.class);
		        i.putExtra("position", position);
		        i.putExtra("is_current_user", false);
	        	startActivity(i);
			}
		});
	    
	    // Copy user photo list from cache
		mUserPhotoAdapter.clear();
		for(UserPhoto userphoto: Cache.USER_PHOTOS) {
			mUserPhotoAdapter.add(userphoto);
		}	    
	}
	

}
