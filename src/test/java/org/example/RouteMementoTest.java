package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RouteMementoTest {

    private List<Point> createSamplePoints() {
        List<Point> points = new ArrayList<>();
        points.add(new Point(10.0, 20.0, Point.PointType.START));
        points.add(new Point(11.0, 21.0, Point.PointType.WAYPOINT));
        points.add(new Point(12.0, 22.0, Point.PointType.END));
        return points;
    }

    // -------------------------------------------------------------
    // Constructors tests
    // -------------------------------------------------------------

    @Test
    public void constructorWithElevationCopiesPointsAndFieldsTest() {
        List<Point> original = createSamplePoints();

        RouteMemento memento = new RouteMemento(original, 42, "Morning Run", 150.5, 120.0);

        assertEquals(42, memento.getId());
        assertEquals("Morning Run", memento.getName());
        assertEquals(150.5, memento.getAscent(), 0.0001);
        assertEquals(120.0, memento.getDescent(), 0.0001);
        assertEquals(original.size(), memento.getPoints().size());
    }

    @Test
    public void constructorWithoutElevationInitializesAscentAndDescentZeroTest() {
        List<Point> original = createSamplePoints();

        RouteMemento memento = new RouteMemento(original, 7, "No Elevation");

        assertEquals(7, memento.getId());
        assertEquals("No Elevation", memento.getName());
        assertEquals(0.0, memento.getAscent(), 0.0001);
        assertEquals(0.0, memento.getDescent(), 0.0001);
        assertEquals(original.size(), memento.getPoints().size());
    }

    // -------------------------------------------------------------
    // getPoints() tests
    // -------------------------------------------------------------

    @Test
    public void getPointsReturnsDeepCopyNotSameInstanceTest() {
        List<Point> original = createSamplePoints();
        RouteMemento memento = new RouteMemento(original, 1, "Test", 10.0, 5.0);

        List<Point> copy = memento.getPoints();

        assertNotSame(original, copy);
        assertEquals(original.size(), copy.size());

        // individual points should be different instances
        for (int i = 0; i < original.size(); i++) {
            assertNotSame(original.get(i), copy.get(i));
            assertEquals(original.get(i).getLatitude(), copy.get(i).getLatitude(), 0.000001);
            assertEquals(original.get(i).getLongitude(), copy.get(i).getLongitude(), 0.000001);
            assertEquals(original.get(i).getType(), copy.get(i).getType());
        }
    }

    @Test
    public void getPointsModifyingReturnedListDoesNotAffectInternalPointsTest() {
        List<Point> original = createSamplePoints();
        RouteMemento memento = new RouteMemento(original, 1, "Test", 10.0, 5.0);

        List<Point> firstCopy = memento.getPoints();
        int originalSize = firstCopy.size();

        // Mutate returned list and one of its elements
        firstCopy.remove(0);
        firstCopy.get(0).setType(Point.PointType.INTERPOLATED);

        // Get points again and ensure memento still holds original data
        List<Point> secondCopy = memento.getPoints();
        assertEquals(originalSize, secondCopy.size());
        assertEquals(Point.PointType.START, secondCopy.get(0).getType());
    }

    // -------------------------------------------------------------
    // getId() tests
    // -------------------------------------------------------------

    @Test
    public void getIdReturnsProvidedIdTest() {
        RouteMemento memento = new RouteMemento(createSamplePoints(), 99, "Route", 0.0, 0.0);

        assertEquals(99, memento.getId());
    }

    @Test
    public void getIdCanReturnZeroIdTest() {
        RouteMemento memento = new RouteMemento(createSamplePoints(), 0, "Route", 0.0, 0.0);

        assertEquals(0, memento.getId());
    }

    // -------------------------------------------------------------
    // getName() tests
    // -------------------------------------------------------------

    @Test
    public void getNameReturnsProvidedNameTest() {
        RouteMemento memento = new RouteMemento(createSamplePoints(), 1, "Evening Run", 0.0, 0.0);

        assertEquals("Evening Run", memento.getName());
    }

    @Test
    public void getNameAllowsNullNameTest() {
        RouteMemento memento = new RouteMemento(createSamplePoints(), 1, null, 0.0, 0.0);

        assertNull(memento.getName());
    }

    // -------------------------------------------------------------
    // getAscent() tests
    // -------------------------------------------------------------

    @Test
    public void getAscentReturnsProvidedAscentTest() {
        RouteMemento memento = new RouteMemento(createSamplePoints(), 1, "Hill Run", 250.75, 100.0);

        assertEquals(250.75, memento.getAscent(), 0.0001);
    }

    @Test
    public void getAscentCanBeZeroTest() {
        RouteMemento memento = new RouteMemento(createSamplePoints(), 1, "Flat Run", 0.0, 10.0);

        assertEquals(0.0, memento.getAscent(), 0.0001);
    }

    // -------------------------------------------------------------
    // getDescent() tests
    // -------------------------------------------------------------

    @Test
    public void getDescentReturnsProvidedDescentTest() {
        RouteMemento memento = new RouteMemento(createSamplePoints(), 1, "Downhill", 50.0, 300.5);

        assertEquals(300.5, memento.getDescent(), 0.0001);
    }

    @Test
    public void getDescentCanBeZeroTest() {
        RouteMemento memento = new RouteMemento(createSamplePoints(), 1, "Uphill", 200.0, 0.0);

        assertEquals(0.0, memento.getDescent(), 0.0001);
    }
}
