package nl.skope.android.maps;

import java.util.ArrayList;

import nl.skope.android.ui.OOIListMapActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class OOIOverlay extends ItemizedOverlay<OverlayItem> {
	
	private ArrayList<OOIOverlayItem> mOverlays = new ArrayList<OOIOverlayItem>();
	private Context mContext;
	
	public OOIOverlay(Drawable defaultMarker, Context context) {
		super(boundCenterBottom(defaultMarker));
		mContext = context;
		populate();
	}

	public void addOverlay(OOIOverlayItem overlay) {
		overlay.setMarker(boundCenterBottom(overlay.getMarker(0)));
	    mOverlays.add(overlay);
	    populate();
	}
	
	public void clear() {
		mOverlays.clear();
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}
	
	@Override
	protected boolean onTap(int index) {
	  OOIOverlayItem item = mOverlays.get(index);
	  if (item.isCluster()) {
		  ((OOIListMapActivity) mContext).zoomToCluster(index);
		  return true;
	  } else {
		  AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
		  dialog.setTitle(item.getTitle());
		  dialog.setMessage(item.getSnippet());
		  dialog.show();
		  return true;
	  }
	}	

}
