package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Memento class for capturing and restoring Route state.
 * Stores deep copies of Point objects plus route id/name.
 */
public class RouteMemento {
    private final List<Point> points;
    private final int id;
    private final String name;

    public RouteMemento(List<Point> points, int id, String name) {

        List<Point> copy = new ArrayList<>(points.size());
        for (Point p : points) {
            copy.add(new Point(p.getGeoPosition(), p.getType()));
        }
        this.points = copy;
        this.id = id;
        this.name = name;
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
}
