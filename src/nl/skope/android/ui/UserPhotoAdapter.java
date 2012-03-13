package nl.skope.android.ui;

import java.util.List;

import nl.skope.android.application.UserPhoto;
import nl.skope.android.application.UserPhoto.OnImageLoadListener;
import nl.skope.android.http.ThumbnailManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import nl.skope.android.R;

/**
 * This image adapter is used by the grid view in the detail page
 * 
 * @author Studio
 */
public class UserPhotoAdapter extends ArrayAdapter<UserPhoto> {

	private int mThumbnailHeight = 60;
	private int mThumbnailWidth = 60;

	protected Context mContext;
	protected ThumbnailManager mThumbnailManager;
	
	OnImageLoadListener mImageLoadListener = new OnImageLoadListener() {
		
		@Override
		public void onImageLoaded(Bitmap image) {
			notifyDataSetChanged();			
		}

		@Override
		public void onImageLoadStart() {
			// TODO Auto-generated method stub
			
		}
	};

	public UserPhotoAdapter(Context context, int textViewResourceId,
								 List<UserPhoto> objects, 
								 ThumbnailManager thumbnailManager) {
		super(context, textViewResourceId, objects);
		mContext = context;
		mThumbnailManager = thumbnailManager;
	}
	
	public UserPhotoAdapter(Context context, int textViewResourceId,
			 List<UserPhoto> objects, 
			 ThumbnailManager thumbnailManager, 
			 int width, int height) {
		this(context, textViewResourceId, objects, thumbnailManager);
		mThumbnailWidth = width;
		mThumbnailHeight = height;		
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView = new ImageView(mContext);
		
		UserPhoto userPhoto = getItem(position);

		int width = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, mThumbnailWidth, mContext
						.getResources().getDisplayMetrics());
		int height = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, mThumbnailHeight, mContext
						.getResources().getDisplayMetrics());
		if (parent instanceof Gallery) {
			imageView.setLayoutParams(new Gallery.LayoutParams(width, height));
		} else {
			imageView.setLayoutParams(new AbsListView.LayoutParams(width, height));			
		}
		imageView.setScaleType(ImageView.ScaleType.FIT_XY);
		imageView.setBackgroundResource(R.drawable.gallery_box);

		imageView.setImageBitmap(userPhoto.getThumbnail());
		if (userPhoto.getThumbnail() == null) {
			imageView.setImageResource(R.drawable.empty_photo_large_icon);
			mThumbnailManager.retrieve(userPhoto, imageView);
		}

		return imageView;
	}
}