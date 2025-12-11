package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Memento class for capturing and restoring Route state.
 * Stores deep copies of Point objects plus route id/name and elevation data.
 */
public class RouteMemento {
    private final List<Point> points;
    private final int id;
    private final String name;
    private final double ascent;
    private final double descent;

    public RouteMemento(List<Point> points, int id, String name, double ascent, double descent) {
        List<Point> copy = new ArrayList<>(points.size());
        for (Point p : points) {
            copy.add(new Point(p.getGeoPosition(), p.getType()));
        }
        this.points = copy;
        this.id = id;
        this.name = name;
        this.ascent = ascent;
        this.descent = descent;
    }

    /**
     * @deprecated Use constructor with elevation parameters
     */
    public RouteMemento(List<Point> points, int id, String name) {
        this(points, id, name, 0, 0);
    }

    public List<Point> getPoints() {
        List<Point> copy = new ArrayList<>(points.size());
        for (Point p : points) {
            copy.add(new Point(p.getGeoPosition(), p.getType()));
        }
        return copy;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getAscent() { return ascent; }
    public double getDescent() { return descent; }
}