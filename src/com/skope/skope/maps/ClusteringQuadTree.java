package com.skope.skope.maps;

import org.w3c.dom.Node;

import com.skope.skope.application.ObjectOfInterest;
import com.skope.skope.application.ObjectOfInterestList;

public class ClusteringQuadTree {
	private Cluster root;
	
	private class Cluster {
		float x, y;
		ObjectOfInterestList oois;
		Cluster northwest, northeast, southwest, southeast;
		
		Cluster(float x, float y, ObjectOfInterestList oois) {
            this.x = x;
            this.y = y;
            this.oois = oois;
        }
	}

    public void insert(float x, float y, ObjectOfInterest ooi) {
        root = insert(root, x, y, ooi);
    }

    private Cluster insert(Cluster h, float x, float y, ObjectOfInterest ooi) {
        if (h == null) return new Cluster(x, y, new ObjectOfInterestList(ooi));
        // else within distance, append ooi, update x,y to center
        else if (x < h.x && y < h.y) h.southwest = insert(h.southwest, x, y, ooi);
        else if (x < h.x && y >= h.y) h.northwest = insert(h.northwest, x, y, ooi);
        else if (x >= h.x && y < h.y) h.southeast = insert(h.southeast, x, y, ooi);
        else if (x >= h.x && y >= h.y) h.northeast = insert(h.northeast, x, y, ooi);
        return h;
    }

}
