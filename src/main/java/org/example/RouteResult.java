package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import java.util.List;

/**
 * Holds the result of a routing API call, including coordinates and elevation data.
 */
public class RouteResult {
    private final List<GeoPosition> points;
    private final double ascent;      // Total ascent in meters
    private final double descent;     // Total descent in meters
    private final double distance;    // Total distance in meters

    public RouteResult(List<GeoPosition> points, double ascent, double descent, double distance) {
        this.points = points;
        this.ascent = ascent;
        this.descent = descent;
        this.distance = distance;
    }

    /**
     * Create a RouteResult with just points (no elevation data)
     */
    public RouteResult(List<GeoPosition> points) {
        this(points, 0, 0, 0);
    }

    public List<GeoPosition> getPoints() {
        return points;
    }

    public double getAscent() {
        return ascent;
    }

    public double getDescent() {
        return descent;
    }

    public double getDistance() {
        return distance;
    }

    /**
     * Get total elevation gain (same as ascent, for clarity)
     */
    public double getElevationGain() {
        return ascent;
    }

    /**
     * Get total elevation loss (same as descent, for clarity)
     */
    public double getElevationLoss() {
        return descent;
    }

    public boolean hasElevationData() {
        return ascent > 0 || descent > 0;
    }

    public boolean hasPoints() {
        return points != null && !points.isEmpty();
    }

    public int getPointCount() {
        return points != null ? points.size() : 0;
    }
}