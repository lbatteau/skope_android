package nl.skope.android.maps;

import nl.skope.android.application.User;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class UserOverlayItem extends OverlayItem {
	User mUser;

	public UserOverlayItem(User user, GeoPoint point) {
		super(point, user.createName(), user.createLabelStatus());
		this.mUser = user;
	}

	public User getObjectOfInterest() {
		return mUser;
	}
	

}
