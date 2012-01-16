package com.skope.skope.application;

import java.util.ArrayList;

public class ObjectOfInterestList extends ArrayList<ObjectOfInterest> {

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -8026104533454334518L;
	
	/** Current position of selected object of interest */
	private int selectedPosition;
	
	public ObjectOfInterestList() {
		super();
	}
	
	public ObjectOfInterestList(ObjectOfInterest ooi) {
		super();
		this.add(ooi);
	}
	
	public ObjectOfInterestList() {
		super();
	}
	
	public ObjectOfInterestList(ObjectOfInterest ooi) {
		super();
		this.add(ooi);
	}
	
	/**
	 * Determines the distance of the farthest object of interest.
	 * Can be used to maximize the map zoom level while including all
	 * objects of interest.
	 * @return distance
	 */
	public synchronized ObjectOfInterest determineFarthestOOI() {
		if (this.size() == 0) {
			return null;
		} else {
			ObjectOfInterest farthestOOI = this.get(0);
			for (ObjectOfInterest ooi : this) {
	            if (farthestOOI.getDistance() < ooi.getDistance()) {
	            	farthestOOI = ooi;
	            }
	        }
			return farthestOOI;
		}
	}
	
	/**
	 * Finds the object of interest with the given username.
	 * @param username
	 * @return The object of interest with the given username.
	 * If not found it returns null.
	 */
	public synchronized ObjectOfInterest find(String username) {
		for (ObjectOfInterest ooi : this) {
            if (ooi.getUserName().equals(username)) {
            	return ooi;
            }
        }
		return null;
	}

	public synchronized int getSelectedPosition() {
		return selectedPosition;
	}

	public synchronized void setSelectedPosition(int selectedPosition) {
		this.selectedPosition = selectedPosition;
	}

	public synchronized ObjectOfInterest getSelectedOOI() {
		return this.get(selectedPosition);
	}
	
	@Override
	public synchronized ObjectOfInterest get(int position) {
		return super.get(position);
	}
	
	public synchronized void update(ObjectOfInterestList list) {
		this.clear();
		this.addAll(list);
	}

}
