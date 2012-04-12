package nl.skope.android.application;

import java.util.ArrayList;
import java.util.Hashtable;

import android.location.Location;
import android.util.Log;

import com.google.android.maps.Overlay;

/**
 * Clustering for a list of users
 * Based on article http://www.appelsiini.net/2008/11/introduction-to-marker-clustering-with-google-maps
 * @author Lukas Batteau
 */
public class MapOverlayClusterer {
	public final static int OFFSET = 268435456; // half of the earth circumference in pixels at zoom level 21
	public final static float RADIUS = 85445659.4471f; // OFFSET / Pi
	
	private static int longitudeToX(double longitude) {
		return (int) Math.round(OFFSET + RADIUS * longitude * Math.PI / 180);    
	}
	
	private static int latitudeToY(double latitude) {
		 return (int) Math.round(OFFSET - RADIUS * 
	                Math.log((1 + Math.sin(latitude * Math.PI / 180)) / 
	                (1 - Math.sin(latitude * Math.PI / 180))) / 2);		
	}
	
	private static int pixelDistance(Location locationA, Location locationB, int zoom) {
		double xA = longitudeToX(locationA.getLongitude());
		double yA = latitudeToY(locationA.getLatitude());
		double xB = longitudeToX(locationB.getLongitude());
		double yB = latitudeToY(locationB.getLatitude());
		
		return ((int)Math.sqrt(Math.pow((xA-xB), 2) + Math.pow((yA-yB),2))) >> (21 - zoom);
	}
	
	/**
	 * Create a clustered list from the given object of interest list, 
	 * based on minimal distance in pixels and the current zoom level.
	 * @param ooiList The current list of objects of interest
	 * @param distance The minimal distance in pixels between objects
	 * @param zoom The current map zoom level (0-21)
	 * @return A clustered list. Clusters may contain just one item.
	 */
	public static ArrayList<ObjectOfInterestList> cluster(ObjectOfInterestList ooiList, int minimalDistance, int zoom) {
		// The clustered list
		ArrayList<ObjectOfInterestList>  clusters = new ArrayList<ObjectOfInterestList>();
		
		// Loop through the object of interest list
		for (User user: ooiList) {
			boolean isClustered = false;
			// Check if there is a cluster within the minimal distance
			for (ArrayList<User> cluster: clusters) {
				// We determine the distance to the first item
				User clusterOOI = cluster.get(0);
				int distance = pixelDistance(user.getLocation(), clusterOOI.getLocation(), zoom);
				// Check distance
				if (minimalDistance > distance) {
					// Distance is below minimum, cluster this object
					cluster.add(user);
					isClustered = true;
					break;
				}
			}
			
			// Check if clustered
			if (!isClustered) {
				// Add the current object as a new cluster.
				ObjectOfInterestList cluster = new ObjectOfInterestList();
				cluster.add(user);
				clusters.add(cluster);
			}
		}
		
		return clusters;
	}

}
