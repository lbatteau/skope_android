package nl.skope.android.maps;

import java.util.ArrayList;
import java.util.List;

import nl.skope.android.R;
import nl.skope.android.application.User;
import nl.skope.android.ui.BalloonOverlayView;
import nl.skope.android.ui.OOIDetailMapActivity;
import nl.skope.android.ui.OOIListMapActivity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class UserOverlay<Item> extends ItemizedOverlay<OverlayItem> {
	
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private MapView mMapView;
	private BalloonOverlayView mBalloonView;
	private View mClickRegion;
	private int mViewOffset;
	final MapController mMapController;	
	
	public UserOverlay(Drawable defaultMarker, MapView mapView) {
		super(boundCenter(defaultMarker));
		this.mMapView = mapView;
		this.mViewOffset = 0;
		this.mMapController = mapView.getController();
		populate();
	}
	
	public int indexOf(User user) {
		for (int i=0; i<mOverlays.size(); i++) {
			OverlayItem item = mOverlays.get(i);
			if (item instanceof UserOverlayItem) {
				if (((UserOverlayItem) item).getObjectOfInterest().getId() == user.getId()) {
					return i;
				}
			} else if (item instanceof UserClusterOverlayItem) {
				for (User clusteredOOI: ((UserClusterOverlayItem) item).getObjectOfInterestList()) {
					if (clusteredOOI.getId() == user.getId()) {
						return i;
					}
				}
			}
		}
		
		return -1;
	}

	public void setBalloonBottomOffset(int pixels) {
		mViewOffset = pixels;
	}
	
	protected boolean onBalloonTap(OverlayItem item) {
		if (item instanceof UserOverlayItem) {
			// Redirect to list activity
	        Intent i = new Intent(mMapView.getContext(), OOIDetailMapActivity.class);
        	Bundle bundle = new Bundle();
	        bundle.putParcelable("USER", ((UserOverlayItem) item).getObjectOfInterest());
	        i.putExtras(bundle);
	        ((OOIListMapActivity)mMapView.getContext()).startActivity(i);
	        return true;
		}
		return false;
	}

	public void addOverlay(OverlayItem overlay) {
		overlay.setMarker(boundCenterBottom(overlay.getMarker(0)));
		mOverlays.add(overlay);
	    populate();
	}
	
	public void clear() {
		mOverlays.clear();
		// Workaround for another issue with this class:
		// http://groups.google.com/group/android-developers/browse_thread/thread/38b11314e34714c3
		setLastFocusedIndex(-1);
		populate();
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}
	
	protected final boolean onTap(int index) {
		// Don't do anything on detail screen
		if (mMapView.getContext() instanceof OOIDetailMapActivity) {
			return true;
		}
		
		OverlayItem item = createItem(index);
		
		if (item instanceof UserClusterOverlayItem) {
			((OOIListMapActivity) mMapView.getContext()).zoomToCluster(index);
			return true;
		} else  {
			UserOverlayItem ooiItem = (UserOverlayItem) item;
			((OOIListMapActivity) mMapView.getContext()).updateGallery(ooiItem.getObjectOfInterest());
		}
		
		return showBalloon(item);
	}
	
	public final boolean showBalloon(OverlayItem item) {
		boolean isRecycled;
		GeoPoint point;
		
		// No balloons for clusters right now
		if (item instanceof UserClusterOverlayItem) {
			hideBalloon();
			return true;
		}
		
		point = item.getPoint();
		
		if (mBalloonView == null) {
			mBalloonView = new BalloonOverlayView(mMapView.getContext(), mViewOffset);
			mClickRegion = (View) mBalloonView.findViewById(R.id.balloon_inner_layout);
			isRecycled = false;
		} else {
			isRecycled = true;
		}
	
		mBalloonView.setVisibility(View.GONE);
		
		List<Overlay> mapOverlays = mMapView.getOverlays();
		if (mapOverlays.size() > 1) {
			hideOtherBalloons(mapOverlays);
		}
		
		mBalloonView.setData(item);
		
		MapView.LayoutParams params = new MapView.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, point,
				MapView.LayoutParams.BOTTOM_CENTER);
		params.mode = MapView.LayoutParams.MODE_MAP;
		
		setBalloonTouchListener(item);
		
		mBalloonView.setVisibility(View.VISIBLE);

		if (isRecycled) {
			mBalloonView.setLayoutParams(params);
		} else {
			mMapView.addView(mBalloonView, params);
		}
		
		mMapController.animateTo(point);
		
		return true;
	}
	
	public void resetBalloons() {
		List<Overlay> mapOverlays = mMapView.getOverlays();
		if (mapOverlays.size() > 1) {
			hideOtherBalloons(mapOverlays);
		}
	}
	
	public void hideBalloon() {
		if (mBalloonView != null) {
			mBalloonView.setVisibility(View.GONE);
		}
	}

	private void hideOtherBalloons(List<Overlay> overlays) {
		
		for (Overlay overlay : overlays) {
			if (overlay instanceof UserOverlay<?> && overlay != this) {
				((UserOverlay<?>) overlay).hideBalloon();
			}
		}
		
	}
	
	private void setBalloonTouchListener(final OverlayItem item) {
		
		try {
			mClickRegion.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					
					View l =  ((View) v.getParent()).findViewById(R.id.balloon_main_layout);
					Drawable d = l.getBackground();
					
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						int[] states = {android.R.attr.state_pressed};
						if (d.setState(states)) {
							d.invalidateSelf();
						}
						return true;
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						int newStates[] = {};
						if (d.setState(newStates)) {
							d.invalidateSelf();
						}
						// call overridden method
						onBalloonTap(item);
						return true;
					} else {
						return false;
					}
					
				}
			});
			
		} catch (SecurityException e) {
			Log.e("BalloonItemizedOverlay", "setBalloonTouchListener reflection SecurityException");
			return;
		}

	}
	
	//	@Override
//	protected boolean onTap(int index) {
//	  UserOverlayItem item = mOverlays.get(index);
//	  if (item.isCluster()) {
//		  ((OOIListMapActivity) mContext).zoomToCluster(index);
//		  return true;
//	  } else {
//		  AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
//		  dialog.setTitle(item.getTitle());
//		  dialog.setMessage(item.getSnippet());
//		  dialog.set
//		  dialog.show();
//		  return true;
//	  }
//	}	

}
