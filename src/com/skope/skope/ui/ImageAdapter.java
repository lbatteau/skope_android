package com.skope.skope.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;

import com.skope.skope.R;
import com.skope.skope.application.Cache;

public class ImageAdapter extends BaseAdapter {
    int mGalleryItemBackground;
    private Context mContext;
    private Cache mCache;

    public ImageAdapter(Context context, Cache cache) {
        mContext = context;
        mCache = cache;
        
        TypedArray attr = mContext.obtainStyledAttributes(R.styleable.Gallery);
        mGalleryItemBackground = attr.getResourceId(
                R.styleable.Gallery_android_galleryItemBackground, 0);
        attr.recycle();
    }

    public int getCount() {
        return mCache.getObjectOfInterestList().size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView = new ImageView(mContext);
        	
        imageView.setImageBitmap(mCache.getObjectOfInterestList().get(position).getThumbnail());
        imageView.setLayoutParams(new Gallery.LayoutParams(100, 100));
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setBackgroundResource(R.drawable.gallery_box);

        return imageView;
    }
}