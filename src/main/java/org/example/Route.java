package org.example;

import org.jxmapviewer.viewer.GeoPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Route now stores rich Point objects (with type + GeoPosition) internally,
 * but keeps compatibility helpers so existing code keeps working.
 */

public class Route {

    private int id = -1;
    private String name = "";
    private final List<Point> points = new ArrayList<>();

    private double ascentInMeters = 0;
    private double descentInMeters = 0;


    public void addWaypoint(Point p) {
        if (p == null) return;

        if (points.isEmpty()) {
            p.setType(Point.PointType.START);
        } else {

            Point last = points.get(points.size() - 1);
            if (last.getType() == Point.PointType.END || last.getType() == Point.PointType.START) {
                last.setType(Point.PointType.WAYPOINT);
            }
        }
        points.add(p);

        points.get(points.size() - 1).setType(Point.PointType.END);
    }

    public void addWaypoint(GeoPosition gp) {
        if (gp == null) return;
        addWaypoint(new Point(gp, Point.PointType.WAYPOINT));
    }

    public void clear() {
        points.clear();
        ascentInMeters = 0;
        descentInMeters = 0;
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public List<Point> getPoints() {
        return Collections.unmodifiableList(points);
    }

    public List<GeoPosition> getAllPointsAsGeoPositions() {
        List<GeoPosition> list = new ArrayList<>(points.size());
        for (Point p : points) list.add(p.getGeoPosition());
        return list;
    }

    public void loadRouteFromGeoPositions(List<GeoPosition> pts) {
        points.clear();
        if (pts == null || pts.isEmpty()) return;
        for (int i = 0; i < pts.size(); i++) {
            Point.PointType t;
            if (i == 0) t = Point.PointType.START;
            else if (i == pts.size() - 1) t = Point.PointType.END;
            else t = Point.PointType.WAYPOINT;
            points.add(new Point(pts.get(i), t));
        }
    }


    public double getTotalDistance() {
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            total += points.get(i - 1).distanceBetweenPointsUsingHaversineFormula(points.get(i));
        }
        return total;
    }


    public double getAscentInMeters() {
        return ascentInMeters;
    }


    public void setAscentInMeters(double ascentInMeters) {
        this.ascentInMeters = ascentInMeters;
    }


    public double getDescentInMeters() {
        return descentInMeters;
    }


    public void setDescentInMeters(double descentInMeters) {
        this.descentInMeters = descentInMeters;
    }


    public void addElevation(double segmentAscent, double segmentDescent) {
        this.ascentInMeters += segmentAscent;
        this.descentInMeters += segmentDescent;
    }


    public void setElevation(double ascent, double descent) {
        this.ascentInMeters = ascent;
        this.descentInMeters = descent;
    }


    public int getEstimatedElevation() {

        return (int) Math.round(ascentInMeters);
    }



    public RouteMemento createMemento() {
        return new RouteMemento(copyPoints(points), id, name, ascentInMeters, descentInMeters);
    }

    public void applyMemento(RouteMemento m) {
        if (m == null) return;
        points.clear();
        points.addAll(copyPoints(m.getPoints()));
        this.id = m.getId();
        this.name = m.getName();
        this.ascentInMeters = m.getAscent();
        this.descentInMeters = m.getDescent();
    }

    private static List<Point> copyPoints(List<Point> src) {
        List<Point> copy = new ArrayList<>(src.size());
        for (Point p : src) {
            copy.add(new Point(p.getGeoPosition(), p.getType()));
        }
        return copy;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name == null ? "" : name; }
    public void setName(String name) { this.name = name; }
}