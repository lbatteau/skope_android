package nl.skope.android.ui;

import java.util.List;

import nl.skope.android.R;
import nl.skope.android.application.User;
import nl.skope.android.application.User.OnImageLoadListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

/**
 * This image adapter is used by the Gallery widget in the map overview.
 * 
 * @author Studio
 */
public class ProfilePictureAdapter extends ArrayAdapter<User> {

	private static final int THUMBNAIL_HEIGHT = 60;
	private static final int THUMBNAIL_WIDTH = 60;

	private Context mContext;
	
	OnImageLoadListener mProfilePictureListener = new OnImageLoadListener() {
		@Override
		public void onImageLoaded(Bitmap profilePicture) {
			ProfilePictureAdapter.this.notifyDataSetChanged();
		}
	};

	public ProfilePictureAdapter(Context context, int textViewResourceId,
								 List<User> objects) {
		super(context, textViewResourceId, objects);
		mContext = context;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView = new ImageView(mContext);
		User ooi = getItem(position);

		imageView.setImageBitmap(ooi.getProfilePicture());
		if (ooi.getProfilePicture() == null) {
			imageView.setImageResource(R.drawable.empty_profile_large_icon);
			ooi.loadProfilePicture(mProfilePictureListener);
		}

		int width = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, THUMBNAIL_WIDTH, mContext
						.getResources().getDisplayMetrics());
		int height = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, THUMBNAIL_HEIGHT, mContext
						.getResources().getDisplayMetrics());
		imageView.setLayoutParams(new Gallery.LayoutParams(width, height));
		imageView.setScaleType(ImageView.ScaleType.FIT_XY);
		imageView.setBackgroundResource(R.drawable.gallery_box_selectable);

		return imageView;
	}
}