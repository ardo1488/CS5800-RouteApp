package org.example;

import org.jxmapviewer.viewer.GeoPosition;


public class Point {
    private GeoPosition geoPosition;
    private PointType type;

    public enum PointType {
        START,      // First point in route
        WAYPOINT,   // Intermediate point
        END,        // Last point in route
        INTERPOLATED // Auto-generated point between waypoints
    }

    public Point(double latitude, double longitude, PointType type) {
        this.geoPosition = new GeoPosition(latitude, longitude);
        this.type = type;
    }

    public Point(GeoPosition geoPosition, PointType type) {
        this.geoPosition = geoPosition;
        this.type = type;
    }

    public GeoPosition getGeoPosition() {
        return geoPosition;
    }

    public double getLatitude() {
        return geoPosition.getLatitude();
    }

    public double getLongitude() {
        return geoPosition.getLongitude();
    }

    public PointType getType() {
        return type;
    }

    public void setType(PointType type) {
        this.type = type;
    }


    public double distanceBetweenPointsUsingHaversineFormula(Point other) {
        double R = 6371; // Earth's radius in km
        double lat1 = Math.toRadians(this.getLatitude());
        double lat2 = Math.toRadians(other.getLatitude());
        double dLat = Math.toRadians(other.getLatitude() - this.getLatitude());
        double dLon = Math.toRadians(other.getLongitude() - this.getLongitude());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    @Override
    public String toString() {
        return String.format("Point(%.6f, %.6f, %s)",
                getLatitude(), getLongitude(), type);
    }
}