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

    /* ---------------- Core ops ---------------- */

    /** Add a waypoint as our rich Point type. */
    public void addWaypoint(Point p) {
        if (p == null) return;
        // If first point, tag START; if we had one before, retag last as WAYPOINT
        if (points.isEmpty()) {
            p.setType(Point.PointType.START);
        } else {
            // ensure the previous "END" becomes a WAYPOINT if user is adding in draw mode
            Point last = points.get(points.size() - 1);
            if (last.getType() == Point.PointType.END || last.getType() == Point.PointType.START) {
                last.setType(Point.PointType.WAYPOINT);
            }
        }
        points.add(p);
        // Mark the last point as END for nicer semantics
        if (points.size() >= 1) {
            points.get(points.size() - 1).setType(Point.PointType.END);
        }
    }

    /** Back-compat: Add with GeoPosition (used by Map click → RouteMapApp). */
    public void addWaypoint(GeoPosition gp) {
        if (gp == null) return;
        addWaypoint(new Point(gp, Point.PointType.WAYPOINT));
    }

    public void clear() {
        points.clear();
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    /** Internal points (rich). Use for memento, not for drawing directly. */
    public List<Point> getPoints() {
        return Collections.unmodifiableList(points);
    }

    /** Back-compat: expose plain GeoPositions for Map drawing & DB save. */
    public List<GeoPosition> getAllPointsAsGeoPositions() {
        List<GeoPosition> list = new ArrayList<>(points.size());
        for (Point p : points) list.add(p.getGeoPosition());
        return list;
    }

    /** Back-compat: load a route from plain GeoPositions (DB load flow). */
    public void loadFromGeoPositions(List<GeoPosition> pts) {
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

    /* ---------------- Stats ---------------- */

    public double getTotalDistance() {
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            total += points.get(i - 1).distanceTo(points.get(i));
        }
        return total;
    }

    public int getEstimatedElevation() {
        // placeholder; wire a real elevation service if desired
        return 0;
    }

    /* ---------------- Memento (undo/redo) ---------------- */

    public RouteMemento createMemento() {
        return new RouteMemento(copyPoints(points), id, name);
    }

    public void applyMemento(RouteMemento m) {
        if (m == null) return;
        points.clear();
        points.addAll(copyPoints(m.getPoints()));
        this.id = m.getId();
        this.name = m.getName();
    }

    private static List<Point> copyPoints(List<Point> src) {
        List<Point> copy = new ArrayList<>(src.size());
        for (Point p : src) {
            copy.add(new Point(p.getGeoPosition(), p.getType()));
        }
        return copy;
    }

    /* ---------------- Id/Name ---------------- */

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name == null ? "" : name; }
    public void setName(String name) { this.name = name; }
}
