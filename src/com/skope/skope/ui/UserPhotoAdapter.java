package com.skope.skope.ui;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import com.skope.skope.R;
import com.skope.skope.application.UserPhoto;
import com.skope.skope.application.UserPhoto.OnImageLoadListener;

/**
 * This image adapter is used by the grid view in the detail page
 * 
 * @author Studio
 */
public class UserPhotoAdapter extends ArrayAdapter<UserPhoto> {

	protected static final int THUMBNAIL_HEIGHT = 60;
	protected static final int THUMBNAIL_WIDTH = 60;

	protected Context mContext;
	
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
								 List<UserPhoto> objects) {
		super(context, textViewResourceId, objects);
		mContext = context;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView = new ImageView(mContext);
		
		UserPhoto userPhoto = getItem(position);

		int width = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, THUMBNAIL_WIDTH, mContext
						.getResources().getDisplayMetrics());
		int height = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, THUMBNAIL_HEIGHT, mContext
						.getResources().getDisplayMetrics());
		imageView.setLayoutParams(new Gallery.LayoutParams(width, height));
		imageView.setScaleType(ImageView.ScaleType.FIT_XY);
		imageView.setBackgroundResource(R.drawable.gallery_box);

		imageView.setImageBitmap(userPhoto.getThumbnail());
		if (userPhoto.getThumbnail() == null) {
			imageView.setImageResource(R.drawable.empty_photo_large_icon);
			userPhoto.loadThumbnail(mImageLoadListener);
		}

		return imageView;
	}
}