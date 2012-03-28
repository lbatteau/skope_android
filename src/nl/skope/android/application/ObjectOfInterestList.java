package nl.skope.android.application;

import java.util.ArrayList;

public class ObjectOfInterestList extends ArrayList<ObjectOfInterest> {

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -8026104533454334518L;
	
	/** Current position of selected object of interest */
	private int selectedPosition;
	
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
	public synchronized ObjectOfInterest find(int id) {
		for (ObjectOfInterest ooi : this) {
            if (ooi.getId() == id) {
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
		try {
			return this.get(selectedPosition);
		} catch(IndexOutOfBoundsException e) {
			// Selected position no longer available.
			return null;
		}
	}
	
	@Override
	public synchronized ObjectOfInterest get(int position) {
		return super.get(position);
	}
	
	public synchronized void update(ObjectOfInterestList list) {
		this.clear();
		this.addAll(list);
	}
	
	public synchronized boolean exists(ObjectOfInterest ooi) {
		for (ObjectOfInterest existingOOI: this) {
			if (existingOOI.getId() == ooi.getId()) {
				return true;
        	}
        }
		
		return false;
	}

	public synchronized ObjectOfInterest exists(int id) {
		for (ObjectOfInterest existingOOI: this) {
			if (id == existingOOI.getId()) {
				return existingOOI;
        	}
        }
		
		return null;
	}
	
	public synchronized ObjectOfInterest getById(int id) {
		for (ObjectOfInterest ooi: this) {
			if (ooi.getId() == id) {
				return ooi;
        	}
        }
		return null;
	}

}
