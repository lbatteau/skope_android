package nl.skope.android.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import nl.skope.android.R;
import nl.skope.android.application.Cache;
import nl.skope.android.application.SkopeApplication;
import nl.skope.android.application.User;
import nl.skope.android.application.User.OnImageLoadListener;
import nl.skope.android.application.UserPhoto;
import nl.skope.android.http.CustomHttpClient;
import nl.skope.android.http.CustomHttpClient.RequestMethod;
import nl.skope.android.http.ThumbnailManager;
import nl.skope.android.service.LocationService;
import nl.skope.android.util.Type;
import nl.skope.android.util.Utility;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class UserProfileActivity extends BaseActivity {
	private static final String TAG = UserProfileActivity.class.getSimpleName();
	
	public static final int ACTION_PICK_PROFILE_PICTURE_CAMERA = 0;
	public static final int ACTION_PICK_PROFILE_PICTURE_FILE = 1;
	public static final int ACTION_THUMBNAIL_CROP = 2;
	public static final int ACTION_ADD_PHOTO_CAMERA = 3;
	public static final int ACTION_ADD_PHOTO_FILE = 4;
	
	private ImageView mProfilePictureView;
	private Button mEditButton;
	private ToggleButton mFacebookConnect;
	private View mMainProfile;
    private LayoutInflater mInflater;
    private Uri mImageUri = Uri.EMPTY;
	private ArrayList<UserPhoto> mUserPhotoList;
	private UserPhotoAdapter mUserPhotoAdapter;
	private Gallery mUserPhotoGallery;
	private ProgressDialog mDialog;
	private ProgressBar mPhotosProgressBar;
	private TextView mPhotosLabel;
	private SharedPreferences mPreferences;
	boolean mChangeProfilePictureToFB = false;
	
	private static Facebook mFacebook = new Facebook("390475844314781");
	private static AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(mFacebook);
	
	static boolean isFBRequestRunning = false;	
	

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
		
		// Shared preferences
		mPreferences = getCache().getPreferences();
		
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
				final String [] items = new String [] {/*labelCamera,*/ labelGallery};
				ArrayAdapter<String> adapter = new ArrayAdapter<String> (UserProfileActivity.this, 
														android.R.layout.select_dialog_item, items);
				AlertDialog.Builder builder  = new AlertDialog.Builder(UserProfileActivity.this);
				 
				builder.setTitle("Select Image");
				builder.setAdapter( adapter, new DialogInterface.OnClickListener() {
				public void onClick( DialogInterface dialog, int item ) {
					// Reset global image uri, so we don't use previous values by accident
					mImageUri = getOutputMediaFileUri();
					// TODO CROPPING AFTER CAMERA NOT SUPPORTED BY SDK. DISABLE FOR NOW.
					if (item == 0 && false) {  
						// Capture from camera
						Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri); // set the image file name
						startActivityForResult(Intent.createChooser(intent, "Select camera"), ACTION_PICK_PROFILE_PICTURE_CAMERA);
				    } else { 
				    	// Pick from file
				        Intent intent = new Intent();
				        intent.setType("image/*");
				        intent.setAction(Intent.ACTION_GET_CONTENT);
				        startActivityForResult(Intent.createChooser(intent, "Select gallery"), ACTION_PICK_PROFILE_PICTURE_FILE);
				    }
				}});
				builder.create().show();
			}
		});
		
		// Edit button action
		mEditButton = (Button) findViewById(R.id.button_edit);
		
		mEditButton.setOnClickListener(new OnClickListener() {
			
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
					mImageUri = getOutputMediaFileUri();
					if (item == 0) {
						// Capture from camera
						Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri); // set the image file name
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
	    // Set up thumbnail manager
	    ThumbnailManager thumbnailManager = new ThumbnailManager(getCache());
	    // User photos adapter
	    mUserPhotoAdapter = new UserPhotoAdapter(getApplicationContext(), R.id.user_photo_grid, mUserPhotoList, thumbnailManager);
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
		        i.putExtra("is_current_user", true);
	        	startActivity(i);
			}
		});
	    
		// Photos progress bar
		mPhotosProgressBar = (ProgressBar) mMainProfile.findViewById(R.id.photos_progress_bar);
		// Photos label
		mPhotosLabel = (TextView) mMainProfile.findViewById(R.id.user_photo_label);

		/*for (PackageInfo pack : getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
	        ProviderInfo[] providers = pack.providers;
	        if (providers != null) {
	            for (ProviderInfo provider : providers) {
	                Log.d(TAG, "provider: " + provider.authority);
	            }
	        }
	    }*/
		
		/*
		 * FACEBOOK
		 */
		mFacebookConnect = (ToggleButton) findViewById(R.id.facebook_connect);
		mFacebookConnect.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mFacebookConnect.isChecked()) {
					String title = getResources().getString(R.string.facebook_connect_confirm_title);
					String message = getResources().getString(R.string.facebook_connect_confirm_message);
					new AlertDialog.Builder(UserProfileActivity.this)
			        .setTitle(title)
			        .setMessage(message)
			        .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int whichButton) {
			            	updateProfileFromFacebook();
			            	mEditButton.setVisibility(View.GONE);
			            	mProfilePictureView.setClickable(false);
			            	mChangeProfilePictureToFB = true;
			            }
			        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int whichButton) {
			                mFacebookConnect.setChecked(false);
			            }
			        }).show();
				} else {
					String title = getResources().getString(R.string.facebook_disconnect_confirm_title);
					String message = getResources().getString(R.string.facebook_disconnect_confirm_message);
					new AlertDialog.Builder(UserProfileActivity.this)
			        .setTitle(title)
			        .setMessage(message)
			        .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int whichButton) {
			            	String username = getCache().getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
			    			String password = getCache().getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
			    			Object[] params = {username, password};
			    			new FBDisconnectTask().execute(params);
			    			mAsyncRunner.logout(UserProfileActivity.this, new RequestListener() {
			    				  @Override
			    				  public void onComplete(String response, Object state) {}
			    				  
			    				  @Override
			    				  public void onIOException(IOException e, Object state) {}
			    				  
			    				  @Override
			    				  public void onFileNotFoundException(FileNotFoundException e,
			    				        Object state) {}
			    				  
			    				  @Override
			    				  public void onMalformedURLException(MalformedURLException e,
			    				        Object state) {}
			    				  
			    				  @Override
			    				  public void onFacebookError(FacebookError e, Object state) {}
			    				});
			    			mEditButton.setVisibility(View.VISIBLE);
			    			mProfilePictureView.setClickable(true);
			    			mChangeProfilePictureToFB = false;
			            }
			        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int whichButton) {
			                mFacebookConnect.setChecked(true);
			            }
			        }).show();
				}
			}
		});		
 	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Bitmap image = null;
		
		if (resultCode == Activity.RESULT_OK) {
			switch(requestCode) {
			case ACTION_PICK_PROFILE_PICTURE_CAMERA:
				crop(mImageUri);
				break;
			case ACTION_PICK_PROFILE_PICTURE_FILE:
				mImageUri = data.getData();
				crop(mImageUri);
				break;
			case ACTION_THUMBNAIL_CROP:
				image = (Bitmap) data.getExtras().getParcelable("data");
				storeProfilePicture(image);
				break;
			case ACTION_ADD_PHOTO_CAMERA:
			case ACTION_ADD_PHOTO_FILE:
				Uri imageUri;
				if (data != null && data.getData() != null) {
					imageUri = data.getData();
				} else {
					imageUri = mImageUri;
				}
				
				storePhoto(imageUri);
			}
				
		} else {
			switch(requestCode) {
			case ACTION_THUMBNAIL_CROP:
			// ProfilePicture cropping failed or canceled
			try {
				// Do our own scaling
				Bitmap bitmap = Media.getBitmap(this.getContentResolver(), mImageUri);
				float height = ((float)User.PROFILE_PICTURE_WIDTH / bitmap.getWidth()) * bitmap.getHeight();
				image = Bitmap.createScaledBitmap(bitmap, User.PROFILE_PICTURE_HEIGHT, (int)height, true);
				storeProfilePicture(image);
			} catch (FileNotFoundException fnfe) {
				Log.e(TAG, fnfe.toString());
			} catch (IOException ioe) {
				Log.e(TAG, ioe.toString());
			}
			}
		}
		
        mFacebook.authorizeCallback(requestCode, resultCode, data);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    if (mImageUri != null) {
	        outState.putString("cameraImageUri", mImageUri.toString());
	    }
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
	    super.onRestoreInstanceState(savedInstanceState);
	    if (savedInstanceState.containsKey("cameraImageUri")) {
	        mImageUri = Uri.parse(savedInstanceState.getString("cameraImageUri"));
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

	protected void storePhoto(Uri uri) {
		Bundle bundle = new Bundle();
        bundle.putString(LocationService.IMAGE_UPLOAD_LOCATION, "photos");
        bundle.putString(LocationService.IMAGE_UPLOAD_NAME, "photo");
        bundle.putString(LocationService.IMAGE_UPLOAD_URI, uri.toString());
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
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (checkCacheSanity()) {
			update();

			boolean isFacebookConnect = getCache().getUser().isFacebookConnect();
			mFacebookConnect.setChecked(isFacebookConnect);
			if (isFacebookConnect) {
				mFacebook.extendAccessTokenIfNeeded(this, null);
				mProfilePictureView.setClickable(false);
				mEditButton.setVisibility(View.GONE);
				updateProfileFromFacebook();
			}
			
		}
	}

	@Override
	public void post(final Type type, final Bundle bundle) {
		switch (type) {
		case READ_USER_PHOTOS_START:
			// Hide label
			mPhotosLabel.setVisibility(View.INVISIBLE);
			
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
			for(UserPhoto userphoto: Cache.USER_PHOTOS) {
				mUserPhotoAdapter.add(userphoto);
			}
			
			// Show label if no photos present
			if (mUserPhotoAdapter.getCount() == 0) {
				mPhotosLabel.setVisibility(View.VISIBLE);
				mPhotosLabel.setText(getResources().getString(R.string.user_photos_none));
			} else {
				mPhotosLabel.setVisibility(View.INVISIBLE);
			}
			break;
		case UPLOAD_PROFILE_PICTURE_START:
			mDialog.setMessage(getResources().getString(R.string.user_profile_uploading_image));
			mDialog.show();
			break;
		case UPLOAD_PROFILE_PICTURE_END:
			mDialog.dismiss();
			String profilePictureURL = bundle.getString("profile_picture_url");
			getCache().getUser().setProfilePictureURL(profilePictureURL);
			getCache().getUser().loadProfilePicture(mProfilePictureListener);
			break;
		case UPLOAD_IMAGE_START:
			mDialog.setMessage(getResources().getString(R.string.user_profile_uploading_image));
			mDialog.show();
			break;
		case UPLOAD_IMAGE_END:
			mDialog.dismiss();
			User user = getCache().getUser();
			refreshUserPhotos(user);
		}
	}
	
	private void update() {
		User user = getCache().getUser();
		user.createUserProfile(mMainProfile, mInflater);
		
		if (user.getProfilePictureURL() != null && !user.getProfilePictureURL().equals("")) {
			user.loadProfilePicture(mProfilePictureListener);
		}
		
		refreshUserPhotos(user);
	}
	
	private void refreshUserPhotos(User user) {
		// Empty current photo list
		Cache.USER_PHOTOS.clear();
		// Request new read
		Bundle bundle = new Bundle();
        bundle.putInt(SkopeApplication.BUNDLEKEY_USERID, user.getId());
        getServiceQueue().postToService(Type.READ_USER_PHOTOS, bundle);
	}

	private void crop(Uri imageUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");
 
        intent.setData(imageUri);
        intent.putExtra("output", getOutputMediaFileUri());
        intent.putExtra("outputX", User.PROFILE_PICTURE_WIDTH);
        intent.putExtra("outputY", User.PROFILE_PICTURE_HEIGHT);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", true);
        
        startActivityForResult(Intent.createChooser(intent, "Select crop method"), ACTION_THUMBNAIL_CROP);
    }
	
	/** Create a file Uri for saving an image or video */
	private Uri getOutputMediaFileUri(){
	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String filename =  "IMG_"+ timeStamp + ".png";

        ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, filename);
		return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
	}
	
	/**
	 * 
	 */
	protected void updateProfileFromFacebook() {
		/*
         * Only call authorize if the access_token has expired.
         */
        if(!mFacebook.isSessionValid()) {
			mFacebook.authorize(UserProfileActivity.this,
				new String[] {"user_relationships", "user_hometown", "user_location",
					"user_education_history", "user_birthday", "user_work_history",
					"publish_stream"},
				new DialogListener() {
		        @Override
		        public void onComplete(Bundle values) {
		        	SharedPreferences.Editor editor = mPreferences.edit();
		            editor.putString("access_token", mFacebook.getAccessToken());
		            editor.putLong("access_expires", mFacebook.getAccessExpires());
		            editor.commit();
		        	
		        	if (!isFBRequestRunning) {
		        		mDialog.setMessage(getResources().getString(R.string.retrieving_data));
			        	mDialog.show();
			        	isFBRequestRunning = true;
			        	mAsyncRunner.request("me", new FBMeRequestListener());
		        	}
		        }

		        @Override
		        public void onFacebookError(FacebookError error) {
		        	Log.e("FB", error.toString());
		        }

		        @Override
		        public void onError(DialogError e) {
		        	Log.e("FB", e.toString());
		        }

		        @Override
		        public void onCancel() {}
		    });
		} else {
			// get information about the currently logged in user
			if (!isFBRequestRunning) {
				isFBRequestRunning = true;
				mAsyncRunner.request("me", new FBMeRequestListener());
			}
		}
	}

	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

		File picturesDir = Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES);
	              
	    File mediaStorageDir = new File(picturesDir, "Skope");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d(TAG, "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
        "IMG_"+ timeStamp + ".jpg");
    
	    return mediaFile;
	}
	
	private class ExternalProfilePicture extends AsyncTask<String, Void, Bitmap> {
		/**
		 * Determine redirect URL to compare with existing
		 * profile picture
		 */
		protected Bitmap doInBackground(String... args) {
			// Profile picture
            return Utility.getBitmapFromURL(args[0]);
		}
		
		protected void onPostExecute(Bitmap bitmap) {
			storeProfilePicture(bitmap);
		}		
        
	}
	
	private class FBDisconnectTask extends AsyncTask<Object, Void, CustomHttpClient> {
		// can use UI thread here
		protected void onPreExecute() {
			mDialog.setMessage("Contacting server...");
			mDialog.show();
		}

		protected CustomHttpClient doInBackground(Object... args) {
			int userId = getCache().getUser().getId();
			String username = (String) args[0];
			String password = (String) args[1];
			String serviceUrl = getCache().getProperty("skope_service_url") + "/user/" + userId + "/";
			
			// Set up HTTP client
	        CustomHttpClient client = new CustomHttpClient(serviceUrl, getApplicationContext());
	        client.setUseBasicAuthentication(true);
	        client.setUsernamePassword(username, password);
	        
	        // Because the Django form used for User updates is based on the 
	        // UserProfile model, it contains name fields for the related
	        // User model, and these are not prefilled with the existing values.
	        // For this reason we always have to submit them.
	        client.addParam("first_name", getCache().getUser().getFirstName());
	        client.addParam("last_name", getCache().getUser().getLastName());
	        client.addParam("is_fb_account", "");
	        
	        // Send HTTP request to web service
			try {
				client.execute(RequestMethod.PUT);
			} catch (Exception e) {
				// Most exceptions already handled by client
				e.printStackTrace();
			}
			
			// Return server response
			return client;
		}
		
		protected void onPostExecute(CustomHttpClient client) {
			mDialog.dismiss();
			
			// Check HTTP response code
			int httpResponseCode = client.getResponseCode();
			// Check for server response
			if (httpResponseCode == 0) {
				// No server response
				Toast.makeText(UserProfileActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
				return;
			} else if (httpResponseCode == HttpStatus.SC_OK) {
		        User user;
		        try {
		        	JSONObject jsonResponse = new JSONObject(client.getResponse());
		        	user = new User(jsonResponse);
		        	user.setCache(getCache());
					getCache().setUser(user);
		        } catch (JSONException e) {
					// Log exception
					Log.e(TAG, e.toString());
					Toast.makeText(UserProfileActivity.this, "Invalid content", Toast.LENGTH_SHORT).show();
					return;
				}
		        
		        // Update profile
		        user.createUserProfile(mMainProfile, mInflater);
		        
		        return;
				
			} else {
				// Server returned error code
				switch (client.getResponseCode()) {
				case HttpStatus.SC_UNAUTHORIZED:
					// Login not successful, authorization required
					Toast.makeText(UserProfileActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
					break;
				case HttpStatus.SC_REQUEST_TIMEOUT:
				case HttpStatus.SC_BAD_GATEWAY:
				case HttpStatus.SC_GATEWAY_TIMEOUT:
					// Connection timeout
					Toast.makeText(
							UserProfileActivity.this,
							getResources().getText(R.string.error_connection_failed), Toast.LENGTH_SHORT).show();
					Log.e(TAG, client.getResponse());
					break;
				case HttpStatus.SC_INTERNAL_SERVER_ERROR:
					Toast.makeText(
							UserProfileActivity.this,
							getResources().getText(R.string.error_server_error), Toast.LENGTH_SHORT).show();
					Log.e(TAG, client.getResponse());
					break;
				case HttpStatus.SC_BAD_REQUEST:
					// Validation failed, extract form errors from response
					JSONObject jsonResponse = null;
					try {
						jsonResponse = new JSONObject(client.getResponse());
					} catch (JSONException e) {
						// Log exception
						Log.e(TAG, e.toString());
						Toast.makeText(UserProfileActivity.this, "Invalid form", Toast.LENGTH_SHORT).show();
						return;
					}

					if (jsonResponse.length() > 0) {
						JSONArray fields = jsonResponse.names();
						try {
							JSONArray errorList = jsonResponse .getJSONArray(fields.getString(0));
							String error = errorList.getString(0);
							Toast.makeText(UserProfileActivity.this, error, Toast.LENGTH_LONG).show();
						} catch (JSONException e) {
							Log.e(TAG, e.toString());
						}
						break;
					}
					break;
				default:
					Toast.makeText(UserProfileActivity.this, String.valueOf(httpResponseCode), Toast.LENGTH_SHORT).show();
					break;					
				}
				return;
			}
		}
	}		

	
	private class FBConnectTask extends AsyncTask<Object, Void, CustomHttpClient> {
		// can use UI thread here
		protected void onPreExecute() {
			//mDialog.setMessage("Contacting server...");
			//mDialog.show();
		}

		protected CustomHttpClient doInBackground(Object... args) {
			User user = getCache().getUser();
			int userId = user.getId();
			String username = (String) args[0];
			String password = (String) args[1];
			String serviceUrl = getCache().getProperty("skope_service_url") + "/user/" + userId + "/";
			
			// Set up HTTP client
	        CustomHttpClient client = new CustomHttpClient(serviceUrl, getApplicationContext());
	        client.setUseBasicAuthentication(true);
	        client.setUsernamePassword(username, password);
	        
			// Add POST parameters
			JSONObject response;
			try {
				response = new JSONObject((String) args[2]);
			} catch (JSONException e1) { 
				return client;
			}
			
			// FB Connect flag
			client.addParam("is_fb_account", "on");
			
			// Determine profile picture URL 
			URL url, fbProfilePictureURL = null;
			try {
				url = new URL(("https://graph.facebook.com/me/picture?type=large&access_token=" + mFacebook.getAccessToken()));
				HttpURLConnection connection;
				connection = (HttpURLConnection) url.openConnection();
				connection.setInstanceFollowRedirects(false);
				fbProfilePictureURL = new URL(connection.getHeaderField("Location"));
				connection.disconnect();
			} catch (MalformedURLException e) {
				Log.e(TAG, e.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return client;
			}
			
			// Add profile picture to form
			client.addParam("fb_profile_picture_url", fbProfilePictureURL.toString());
				
			// Process FB response
			try {
				client.addParam("first_name", response.getString("first_name"));
			} catch (JSONException e) {}
			try {
				client.addParam("last_name", response.getString("last_name"));
			} catch (JSONException e) {}
			try {
				client.addParam("relationship_status", response.getString("relationship_status"));
			} catch (JSONException e) {}
			try {
				client.addParam("home_town", response.getJSONObject("location").getString("name"));
			} catch (JSONException e) {}
			try {
				JSONArray work = response.getJSONArray("work");
				JSONObject latest = findMostRecentWorkItem(work);
				try {
					client.addParam("work_job_title", latest.getJSONObject("position").getString("name"));
				} catch (JSONException e) {}
				try {
					client.addParam("work_company", latest.getJSONObject("employer").getString("name"));
				} catch (JSONException e) {}
			} catch (JSONException e) {}
			try {
				JSONArray education = response.getJSONArray("education");
				JSONObject latest = findMostRecentEducationItem(education);
				try {
					JSONObject concentration = latest.getJSONArray("concentration").getJSONObject(0);
					client.addParam("education_study", concentration.getString("name"));
				} catch (JSONException e) {}
				try {
					client.addParam("education_college", latest.getJSONObject("school").getString("name"));
				} catch (JSONException e) {}
			} catch (JSONException e) {}
			try {
				SimpleDateFormat formatFrom = new SimpleDateFormat("MM/dd/yyyy");
				Date birthday = formatFrom.parse(response.getString("birthday"));
				SimpleDateFormat formatTo = new SimpleDateFormat("yyyy-MM-dd");
				client.addParam("date_of_birth", formatTo.format(birthday));
			} catch (Exception e) {}
			try {
				client.addParam("gender", response.getString("gender"));
			} catch (JSONException e) {}

			// Send HTTP request to web service
			try {
				client.execute(RequestMethod.PUT);
			} catch (Exception e) {
				// Most exceptions already handled by client
				e.printStackTrace();
			}
			
			// Post to wall if first time
			// Read flag
			boolean fbHasPosted = mPreferences.getBoolean(SkopeApplication.PREFS_FBHASPOSTED, false);
			if (!fbHasPosted) {
				Bundle parameters = new Bundle();
	            parameters.putString("message", String.format("%s started using Skope", user.createName()));
	            parameters.putString("name", "Skope");
	            parameters.putString("link", "http://www.facebook.com/Skope");
	            parameters.putString("icon", "http://static.skope.net/images/ic_launcher.png");
	            parameters.putString("picture", "http://static.skope.net/images/ic_launcher.png");
	            parameters.putString("description", "Skope gives you an exciting new look at the world around you. Download the public beta for Android now!");
	            mAsyncRunner.request("me/feed", parameters, "POST", new RequestListener() {
					@Override
					public void onMalformedURLException(MalformedURLException e, Object state) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onIOException(IOException e, Object state) {
						Log.e("FB", e.toString());
						
					}
					
					@Override
					public void onFileNotFoundException(FileNotFoundException e, Object state) {
						Log.e("FB", e.toString());
						
					}
					
					@Override
					public void onFacebookError(FacebookError e, Object state) {
						Log.e("FB", e.toString());
						
					}
					
					@Override
					public void onComplete(String response, Object state) {
						SharedPreferences.Editor editor = mPreferences.edit();
				    	editor.putBoolean(SkopeApplication.PREFS_FBHASPOSTED, true);
				    	editor.commit();						
					}
				}, null);				
			}

			
			// Return server response
			return client;
		}
		
		/**
		 * Find the most recent work item in the JSONArray returned by the
		 * Facebook Graph API.
		 * @param array The list of items
		 * @return The JSONObject with the most recent start date
		 */
		private JSONObject findMostRecentWorkItem(JSONArray array) {
			JSONObject mostRecentItem = null;
			Date mostRecentDate = null;
			for (int i=0; i<array.length(); i++) {
				// Get object at index
				JSONObject item;
				try {
					item = array.getJSONObject(i);
				} catch (JSONException e) {
					continue;
				}
				
				// Extract date
				SimpleDateFormat formatFrom = new SimpleDateFormat("yyyy-MM");
				Date date = null;
				if (item.has("start_date")) {
					try {
						date = formatFrom.parse(item.getString("start_date"));
					} catch (ParseException e) {
						continue;
					} catch (JSONException e) {
						continue;
					}
				}
				
				// Compare
				if (mostRecentDate == null) {
					// First date found
					mostRecentDate = date;
					mostRecentItem = item;
				} else if (date != null){
					// Check if current date is after our most recent date 
					if (date.after(mostRecentDate)) {
						// After, replace most recent date
						mostRecentDate = date;
						mostRecentItem = item;
					}
				}
			}
			
			return mostRecentItem;
		}

		/**
		 * Find the most recent education item in the JSONArray returned by 
		 * the Facebook Graph API.
		 * @param array The list of items
		 * @return The JSONObject with the most recent start date
		 */
		private JSONObject findMostRecentEducationItem(JSONArray array) {
			JSONObject mostRecentItem = null;
			Date date, mostRecentDate = null;
			for (int i=0; i<array.length(); i++) {
				// Get object at index
				JSONObject item;
				try {
					item = array.getJSONObject(i);
				} catch (JSONException e) {
					continue;
				}
				
				// Check if data present
				if (!item.has("year")) {
					// Education containing field year is in the past
					mostRecentDate = new Date();
					mostRecentItem = item;
					continue;
				}
				
				// Extract date
				SimpleDateFormat formatFrom = new SimpleDateFormat("yyyy");
				try {
					date = formatFrom.parse(item.getJSONObject("year").getString("name"));
				} catch (ParseException e) {
					continue;
				} catch (JSONException e) {
					continue;
				}
				
				// Compare
				if (mostRecentDate == null) {
					// First date found
					mostRecentDate = date;
					mostRecentItem = item;
				} else {
					// Check if current date is after our most recent date 
					if (date.after(mostRecentDate)) {
						// After, replace most recent date
						mostRecentDate = date;
						mostRecentItem = item;
					}
				}
			}
			
			return mostRecentItem;
		}

		protected void onPostExecute(CustomHttpClient client) {
			mDialog.dismiss();
			isFBRequestRunning = false;
			boolean isFBProfilePictureChanged;
			// Check HTTP response code
			int httpResponseCode = client.getResponseCode();
			// Check for server response
			if (httpResponseCode == 0) {
				// No server response
				Toast.makeText(UserProfileActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
				return;
			} else if (httpResponseCode == HttpStatus.SC_OK) {
		        User user;
		        try {
		        	JSONObject jsonResponse = new JSONObject(client.getResponse());

		        	// Check if profile picture has changed
		        	String fbProfilePictureURL = jsonResponse.getString("fb_profile_picture_url");
		        	isFBProfilePictureChanged = 
		        		!fbProfilePictureURL.equals(getCache().getUser().getFBProfilePictureURL());

		        	user = new User(jsonResponse);
		        	// Transfer existing favorites
		        	user.setFavorites(getCache().getUser().getFavorites());
		        	user.setCache(getCache());
					getCache().setUser(user);
		        } catch (JSONException e) {
					// Log exception
					Log.e(TAG, e.toString());
					Toast.makeText(UserProfileActivity.this, "Invalid content", Toast.LENGTH_SHORT).show();
					return;
				}
		        
		        // Update profile
		        user.createUserProfile(mMainProfile, mInflater);
		        
                if (mChangeProfilePictureToFB || isFBProfilePictureChanged) {
                	new ExternalProfilePicture().execute("https://graph.facebook.com/me/picture?type=large&access_token=" + mFacebook.getAccessToken());
                }

		        return;
				
			} else {
				// Server returned error code
				switch (client.getResponseCode()) {
				case HttpStatus.SC_UNAUTHORIZED:
					// Login not successful, authorization required
					Toast.makeText(UserProfileActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
					break;
				case HttpStatus.SC_REQUEST_TIMEOUT:
				case HttpStatus.SC_BAD_GATEWAY:
				case HttpStatus.SC_GATEWAY_TIMEOUT:
					// Connection timeout
					Toast.makeText(
							UserProfileActivity.this,
							getResources().getText(R.string.error_connection_failed), Toast.LENGTH_SHORT).show();
					Log.e(TAG, client.getResponse());
					break;
				case HttpStatus.SC_INTERNAL_SERVER_ERROR:
					Toast.makeText(
							UserProfileActivity.this,
							getResources().getText(R.string.error_server_error), Toast.LENGTH_SHORT).show();
					Log.e(TAG, client.getResponse());
					break;
				case HttpStatus.SC_BAD_REQUEST:
					// Validation failed, extract form errors from response
					JSONObject jsonResponse = null;
					try {
						jsonResponse = new JSONObject(client.getResponse());
					} catch (JSONException e) {
						// Log exception
						Log.e(TAG, e.toString());
						Toast.makeText(UserProfileActivity.this, "Invalid form", Toast.LENGTH_SHORT).show();
						return;
					}

					if (jsonResponse.length() > 0) {
						JSONArray fields = jsonResponse.names();
						try {
							JSONArray errorList = jsonResponse .getJSONArray(fields.getString(0));
							String error = errorList.getString(0);
							Toast.makeText(UserProfileActivity.this, error, Toast.LENGTH_LONG).show();
						} catch (JSONException e) {
							Log.e(TAG, e.toString());
						}
						break;
					}
					break;
				default:
					Toast.makeText(UserProfileActivity.this, String.valueOf(httpResponseCode), Toast.LENGTH_SHORT).show();
					break;					
				}
				return;
			}
		}
	}
	
	private class FBMeRequestListener implements RequestListener {
		@Override
		public void onComplete(String response, Object state) {
			String username = getCache().getPreferences().getString(SkopeApplication.PREFS_USERNAME, "");
			String password = getCache().getPreferences().getString(SkopeApplication.PREFS_PASSWORD, "");
			Object[] params = {username, password, response};
			new FBConnectTask().execute(params);
			
		}

		@Override
		public void onIOException(IOException e, Object state) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onFileNotFoundException(FileNotFoundException e,
				Object state) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onMalformedURLException(MalformedURLException e,
				Object state) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onFacebookError(FacebookError e, Object state) {
			// TODO Auto-generated method stub
			
		}
		
	}	

}
