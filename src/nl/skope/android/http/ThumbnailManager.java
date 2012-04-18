package nl.skope.android.http;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import nl.skope.android.application.Cache;
import nl.skope.android.application.UserPhoto;
import nl.skope.android.http.CustomHttpClient.FlushedInputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;


public class ThumbnailManager {
	
	private static final String TAG = ThumbnailManager.class.getSimpleName();
	
	private Cache mCache;
    private ThumbnailWorker mWorker;
    private final Object mWorkerThreadLock = new Object();
    
    
    public ThumbnailManager(Cache cache) {
    	mCache = cache;
    }
    
    public void retrieve(UserPhoto userPhoto, ImageView imageView) {
    	// Check cache
    	Bitmap bitmap = mCache.getBitmapFromCache(userPhoto.getThumbnailURL());
    	if (bitmap != null) {
    		// Bitmap found in cache
    		userPhoto.setThumbnail(new WeakReference<Bitmap>(bitmap));
    		imageView.setImageBitmap(bitmap);
    		return;
    	}
		
    	// Queue image
    	synchronized (mWorkerThreadLock) {
            if (mWorker == null || mWorker.isStopping()) {
            	mWorker = new ThumbnailWorker();
            	mWorker.add(userPhoto, imageView);
            	mWorker.start();
            } else {
            	mWorker.add(userPhoto, imageView);
            }
        }
    }
    
    public class ThumbnailWorker extends Thread {
    	private final List<UserPhoto> userPhotoQueue;
    	private final List<ImageView> imageQueue;
    	private boolean stopping = false;
    	
    	public ThumbnailWorker() {
    		userPhotoQueue = new ArrayList<UserPhoto>();    		
    		imageQueue = new ArrayList<ImageView>();
    	}
        
        protected final void add(final UserPhoto userPhoto, final ImageView imageView) {
            synchronized (userPhotoQueue) {
            	userPhotoQueue.add(userPhoto);
            }
            
            synchronized (imageQueue) {
            	imageQueue.add(imageView);
            }
            
            
        }
        
        public final void run() {
            while (userPhotoQueue.size() > 0) {
            	final UserPhoto userPhoto;
            	final ImageView imageView;
            	
            	synchronized (userPhotoQueue) {
    				userPhoto = userPhotoQueue.remove(0);
                }
            	
            	synchronized (imageQueue) {
            		imageView = imageQueue.remove(0);
                }
            	
            	URL url;
    			try {
    				url = new URL(userPhoto.getThumbnailURL());
    			} catch (MalformedURLException error) {
    				Log.e(TAG, error.toString());
    				return;
    			}
    			
    			try {
    				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    				connection.setDoInput(true);
    				connection.setUseCaches(true);
    				connection.connect();
    				FlushedInputStream input = new FlushedInputStream(connection.getInputStream());
    				final Bitmap bitmap = BitmapFactory.decodeStream(input);
    				// Set the user's profile picture
    				userPhoto.setThumbnail(new WeakReference<Bitmap>(bitmap));
    				// Cache bitmap
    				mCache.addBitmapToCache(userPhoto.getThumbnailURL(), bitmap);
    				// Set image view
    				imageView.post(new Runnable() {
    			        public void run() {
    			        	imageView.setImageBitmap(bitmap);
    			        };
    				});
    			} catch (IOException e) {
    				Log.e(TAG, e.toString());
    			}
            }
            
            stopping = true;
        }
    
        public final boolean isStopping() {
            return stopping;
        }
    }
        
}
