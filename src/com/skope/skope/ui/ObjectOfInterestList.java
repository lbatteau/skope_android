package com.skope.skope.ui;

import java.util.ArrayList;

public class ObjectOfInterestList extends ArrayList<ObjectOfInterest> {

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -8026104533454334518L;
	
	/**
	 * Determines the distance of the farthest object of interest.
	 * Can be used to maximize the map zoom level while including all
	 * objects of interest.
	 * @return distance
	 */
	public ObjectOfInterest determineFarthestOOI() {
		ObjectOfInterest farthestOOI = this.get(0);
		for (ObjectOfInterest ooi : this) {
            if (farthestOOI.getDistance() < ooi.getDistance()) {
            	farthestOOI = ooi;
            }
        }
		return farthestOOI;
	}
	
	/**
	 * Finds the object of interest with the given username.
	 * @param username
	 * @return The object of interest with the given username.
	 * If not found it returns null.
	 */
	public ObjectOfInterest find(String username) {
		for (ObjectOfInterest ooi : this) {
            if (ooi.getUserName().equals(username)) {
            	return ooi;
            }
        }
		return null;
	}

}
