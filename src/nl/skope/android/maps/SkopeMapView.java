package nl.skope.android.maps;

import android.graphics.Canvas;
import android.view.MotionEvent;

import com.google.android.maps.MapView;

public class SkopeMapView extends MapView {
	private OnZoomListener mOnZoomListener;
	int oldZoomLevel=-1;
	
	public interface OnZoomListener {
		void onZoomChanged(int zoomLevel);
	}
	
	public SkopeMapView(android.content.Context context, android.util.AttributeSet attrs) {
		super(context, attrs);
	}

	 public SkopeMapView(android.content.Context context, android.util.AttributeSet attrs, int defStyle) {
		 super(context, attrs, defStyle);
	}

	public SkopeMapView(android.content.Context context, java.lang.String apiKey) {
		super(context, apiKey);
	}
	 
	public boolean onTouchEvent(MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_UP) {
			
		}
		return super.onTouchEvent(ev);
	}
	
	public void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (getZoomLevel() != oldZoomLevel) {
			if (mOnZoomListener != null) {
				mOnZoomListener.onZoomChanged(this.getZoomLevel());
			}
			oldZoomLevel = getZoomLevel();
		}
	}

	public OnZoomListener getOnZoomListener() {
		return mOnZoomListener;
	}

	public void setOnZoomListener(OnZoomListener onZoomListener) {
		this.mOnZoomListener = onZoomListener;
	}
}