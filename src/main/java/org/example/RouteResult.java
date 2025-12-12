package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import java.util.List;

/**
 * Holds the result of a routing API call, including coordinates and elevation data.
 */
public class RouteResult {
    private final List<GeoPosition> points;
    private final double ascentInMeters;
    private final double descentInMeters;
    private final double distanceInMeters;

    public RouteResult(List<GeoPosition> points, double ascent, double descent, double distance) {
        this.points = points;
        this.ascentInMeters = ascent;
        this.descentInMeters = descent;
        this.distanceInMeters = distance;
    }

    public List<GeoPosition> getPoints() {
        return points;
    }

    public double getAscent() {
        return ascentInMeters;
    }

    public double getDescent() {
        return descentInMeters;
    }

    public double getDistance() {
        return distanceInMeters;
    }


    public double getElevationGain() {
        return ascentInMeters;
    }


    public double getElevationLoss() {
        return descentInMeters;
    }

    public boolean hasElevationData() {
        return ascentInMeters > 0 || descentInMeters > 0;
    }

    public boolean hasPoints() {
        return points != null && !points.isEmpty();
    }

    public int getPointCount() {
        return points != null ? points.size() : 0;
    }
}