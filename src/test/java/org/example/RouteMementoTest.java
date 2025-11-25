package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RouteMementoTest {

    private RouteMemento createSampleMemento() {
        List<Point> points = Arrays.asList(
                new Point(new GeoPosition(0.0, 0.0), Point.PointType.START),
                new Point(new GeoPosition(1.0, 1.0), Point.PointType.END)
        );
        return new RouteMemento(points, 123, "Sample");
    }

    /* getPoints */

    @Test
    public void getPointsReturnsDeepCopyOfPointsTest() {
        RouteMemento memento = createSampleMemento();
        List<Point> points = memento.getPoints();

        // Modify returned list and contained points, should not affect memento's internal state
        points.get(0).setType(Point.PointType.WAYPOINT);
        points.remove(1);

        List<Point> pointsAgain = memento.getPoints();
        assertEquals(2, pointsAgain.size());
        assertEquals(Point.PointType.START, pointsAgain.get(0).getType());
    }

    @Test
    public void getPointsSubsequentCallsReturnIndependentListsTest() {
        RouteMemento memento = createSampleMemento();
        List<Point> first = memento.getPoints();
        List<Point> second = memento.getPoints();

        assertNotSame(first, second);
        first.remove(0);
        assertEquals(2, second.size());
    }

    /* getId */

    @Test
    public void getIdReturnsConstructorValueTest() {
        RouteMemento memento = new RouteMemento(
                Arrays.asList(new Point(0.0, 0.0, Point.PointType.START)),
                99,
                "Name"
        );
        assertEquals(99, memento.getId());
    }

    @Test
    public void getIdRemainsUnchangedWhenPointsMutateTest() {
        RouteMemento memento = createSampleMemento();
        List<Point> points = memento.getPoints();
        points.clear();
        assertEquals(123, memento.getId());
    }

    /* getName */

    @Test
    public void getNameReturnsConstructorValueTest() {
        RouteMemento memento = createSampleMemento();
        assertEquals("Sample", memento.getName());
    }

    @Test
    public void getNameHandlesNullNameTest() {
        RouteMemento memento = new RouteMemento(
                Arrays.asList(new Point(0.0, 0.0, Point.PointType.START)),
                1,
                null
        );
        assertNull(memento.getName());
    }
}
