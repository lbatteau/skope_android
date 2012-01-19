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
import com.skope.skope.application.Cache;
import com.skope.skope.application.ObjectOfInterest;
import com.skope.skope.application.User.OnThumbnailLoadListener;

public class ImageAdapter extends ArrayAdapter<ObjectOfInterest> {

	private static final int THUMBNAIL_HEIGHT = 70;
	private static final int THUMBNAIL_WIDTH = 70;

	private Context mContext;
	
	OnThumbnailLoadListener mThumbnailListener = new OnThumbnailLoadListener() {
		@Override
		public void onThumbnailLoaded(Bitmap thumbnail) {
			ImageAdapter.this.notifyDataSetChanged();;
		}
	};

	public ImageAdapter(Context context, int textViewResourceId,
			Cache cache, List<ObjectOfInterest> objects) {
		super(context, textViewResourceId, objects);
		mContext = context;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView = new ImageView(mContext);
		ObjectOfInterest ooi = getItem(position);

		imageView.setImageBitmap(ooi.getThumbnail());
		if (ooi.getThumbnail() == null) {
			ooi.loadThumbnail(mThumbnailListener);
		}

		int width = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, THUMBNAIL_WIDTH, mContext
						.getResources().getDisplayMetrics());
		int height = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, THUMBNAIL_HEIGHT, mContext
						.getResources().getDisplayMetrics());
		imageView.setLayoutParams(new Gallery.LayoutParams(width, height));
		imageView.setScaleType(ImageView.ScaleType.FIT_XY);
		imageView.setBackgroundResource(R.drawable.gallery_box);

		return imageView;
	}
}