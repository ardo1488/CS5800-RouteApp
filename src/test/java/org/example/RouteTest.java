package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RouteTest {

    /* addWaypoint(Point) */

    @Test
    public void addWaypointWithPointUpdatesTypesCorrectlyTest() {
        Route route = new Route();
        Point p1 = new Point(0.0, 0.0, Point.PointType.WAYPOINT);
        Point p2 = new Point(1.0, 1.0, Point.PointType.WAYPOINT);

        route.addWaypoint(p1);
        route.addWaypoint(p2);

        List<Point> points = route.getPoints();
        assertEquals(Point.PointType.WAYPOINT, points.get(0).getType());
        assertEquals(Point.PointType.END, points.get(1).getType());
    }

    @Test
    public void addWaypointWithMultiplePointsRetagsPreviousEndToWaypointTest() {
        Route route = new Route();
        Point p1 = new Point(0.0, 0.0, Point.PointType.START);
        Point p2 = new Point(1.0, 1.0, Point.PointType.END);
        Point p3 = new Point(2.0, 2.0, Point.PointType.WAYPOINT);

        route.addWaypoint(p1);
        route.addWaypoint(p2);
        route.addWaypoint(p3);

        List<Point> points = route.getPoints();
        assertEquals(Point.PointType.WAYPOINT, points.get(1).getType(), "Previous END should become WAYPOINT");
        assertEquals(Point.PointType.END, points.get(2).getType(), "Last point should be END");
    }

    /* addWaypoint(GeoPosition) */

    @Test
    public void addWaypointWithGeoPositionCreatesPointInRouteTest() {
        Route route = new Route();
        GeoPosition gp = new GeoPosition(10.0, 20.0);

        route.addWaypoint(gp);

        assertEquals(1, route.getPoints().size());
        Point p = route.getPoints().get(0);
        assertEquals(10.0, p.getLatitude(), 1e-9);
        assertEquals(20.0, p.getLongitude(), 1e-9);
    }

    @Test
    public void addWaypointWithNullGeoPositionDoesNothingTest() {
        Route route = new Route();
        route.addWaypoint((GeoPosition) null);
        assertTrue(route.isEmpty());
    }

    /* clear */

    @Test
    public void clearRemovesAllPointsTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));
        route.addWaypoint(new GeoPosition(1.0, 1.0));

        route.clear();

        assertTrue(route.isEmpty());
        assertEquals(0, route.getPoints().size());
    }

    @Test
    public void clearOnEmptyRouteKeepsItEmptyTest() {
        Route route = new Route();
        route.clear();
        assertTrue(route.isEmpty());
    }

    /* isEmpty */

    @Test
    public void isEmptyReturnsTrueForNewRouteTest() {
        Route route = new Route();
        assertTrue(route.isEmpty());
    }

    @Test
    public void isEmptyReturnsFalseAfterAddingPointTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));
        assertFalse(route.isEmpty());
    }

    /* getPoints */

    @Test
    public void getPointsReturnsUnmodifiableListTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));

        List<Point> points = route.getPoints();
        assertThrows(UnsupportedOperationException.class, () -> points.add(
                new Point(1.0, 1.0, Point.PointType.WAYPOINT)
        ));
    }

    @Test
    public void getPointsReflectsInternalPointsOrderTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));
        route.addWaypoint(new GeoPosition(1.0, 1.0));
        route.addWaypoint(new GeoPosition(2.0, 2.0));

        List<Point> points = route.getPoints();
        assertEquals(3, points.size());
        assertEquals(0.0, points.get(0).getLatitude(), 1e-9);
        assertEquals(2.0, points.get(2).getLatitude(), 1e-9);
    }

    /* getAllPointsAsGeoPositions */

    @Test
    public void getAllPointsAsGeoPositionsReturnsSameSizeAsPointsTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));
        route.addWaypoint(new GeoPosition(1.0, 1.0));

        List<GeoPosition> positions = route.getAllPointsAsGeoPositions();
        assertEquals(route.getPoints().size(), positions.size());
    }

    @Test
    public void getAllPointsAsGeoPositionsContainsCorrectCoordinatesTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(10.0, 20.0));
        route.addWaypoint(new GeoPosition(30.0, 40.0));

        List<GeoPosition> positions = route.getAllPointsAsGeoPositions();
        assertEquals(10.0, positions.get(0).getLatitude(), 1e-9);
        assertEquals(20.0, positions.get(0).getLongitude(), 1e-9);
        assertEquals(30.0, positions.get(1).getLatitude(), 1e-9);
        assertEquals(40.0, positions.get(1).getLongitude(), 1e-9);
    }

    /* loadFromGeoPositions */

    @Test
    public void loadFromGeoPositionsSetsCorrectPointTypesTest() {
        Route route = new Route();
        List<GeoPosition> pts = Arrays.asList(
                new GeoPosition(0.0, 0.0),
                new GeoPosition(1.0, 1.0),
                new GeoPosition(2.0, 2.0)
        );

        route.loadFromGeoPositions(pts);

        List<Point> points = route.getPoints();
        assertEquals(Point.PointType.START, points.get(0).getType());
        assertEquals(Point.PointType.WAYPOINT, points.get(1).getType());
        assertEquals(Point.PointType.END, points.get(2).getType());
    }

    @Test
    public void loadFromGeoPositionsWithNullOrEmptyLeavesRouteEmptyTest() {
        Route route = new Route();
        route.loadFromGeoPositions(null);
        assertTrue(route.isEmpty());

        route.loadFromGeoPositions(Arrays.asList());
        assertTrue(route.isEmpty());
    }

    /* getTotalDistance */

    @Test
    public void getTotalDistanceZeroForSinglePointTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));

        assertEquals(0.0, route.getTotalDistance(), 1e-6);
    }

    @Test
    public void getTotalDistanceSumsSegmentDistancesTest() {
        Route route = new Route();
        // (0,0) -> (0,1) -> (0,2) ≈ 2 * 111 km
        route.addWaypoint(new GeoPosition(0.0, 0.0));
        route.addWaypoint(new GeoPosition(0.0, 1.0));
        route.addWaypoint(new GeoPosition(0.0, 2.0));

        double total = route.getTotalDistance();
        assertTrue(total > 220 && total < 225, "Total distance should be around 222 km but was " + total);
    }

    /* getEstimatedElevation */

    @Test
    public void getEstimatedElevationReturnsZeroTest() {
        Route route = new Route();
        assertEquals(0, route.getEstimatedElevation());
    }

    @Test
    public void getEstimatedElevationStableAcrossCallsTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));
        route.addWaypoint(new GeoPosition(1.0, 1.0));

        assertEquals(0, route.getEstimatedElevation());
        assertEquals(0, route.getEstimatedElevation());
    }

    /* createMemento */

    @Test
    public void createMementoCapturesCurrentPointsAndMetadataTest() {
        Route route = new Route();
        route.setId(42);
        route.setName("Test Route");
        route.addWaypoint(new GeoPosition(0.0, 0.0));
        route.addWaypoint(new GeoPosition(1.0, 1.0));

        RouteMemento memento = route.createMemento();

        assertEquals(42, memento.getId());
        assertEquals("Test Route", memento.getName());
        assertEquals(2, memento.getPoints().size());
    }

    @Test
    public void createMementoCreatesDeepCopyOfPointsTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));
        RouteMemento memento = route.createMemento();

        // Mutate route after memento creation
        route.addWaypoint(new GeoPosition(1.0, 1.0));

        assertEquals(1, memento.getPoints().size(), "Memento should not be affected by later changes");
    }

    /* applyMemento */

    @Test
    public void applyMementoRestoresRouteStateTest() {
        Route route = new Route();
        route.setId(1);
        route.setName("Original");
        route.addWaypoint(new GeoPosition(0.0, 0.0));

        RouteMemento memento = route.createMemento();

        // Change route
        route.setId(2);
        route.setName("Modified");
        route.addWaypoint(new GeoPosition(1.0, 1.0));

        route.applyMemento(memento);

        assertEquals(1, route.getId());
        assertEquals("Original", route.getName());
        assertEquals(1, route.getPoints().size());
    }

    @Test
    public void applyMementoWithNullDoesNothingTest() {
        Route route = new Route();
        route.setId(5);
        route.setName("Name");
        route.addWaypoint(new GeoPosition(0.0, 0.0));

        route.applyMemento(null);

        assertEquals(5, route.getId());
        assertEquals("Name", route.getName());
        assertEquals(1, route.getPoints().size());
    }

    /* getId */

    @Test
    public void getIdDefaultIsMinusOneTest() {
        Route route = new Route();
        assertEquals(-1, route.getId());
    }

    @Test
    public void getIdReturnsValueSetBySetIdTest() {
        Route route = new Route();
        route.setId(99);
        assertEquals(99, route.getId());
    }

    /* setId */

    @Test
    public void setIdUpdatesIdTest() {
        Route route = new Route();
        route.setId(10);
        assertEquals(10, route.getId());
    }

    @Test
    public void setIdOverwritesPreviousIdTest() {
        Route route = new Route();
        route.setId(1);
        route.setId(2);
        assertEquals(2, route.getId());
    }

    /* getName */

    @Test
    public void getNameDefaultIsEmptyStringTest() {
        Route route = new Route();
        assertEquals("", route.getName());
    }

    @Test
    public void getNameReturnsNonNullEvenWhenSetToNullTest() {
        Route route = new Route();
        route.setName(null);
        assertNotNull(route.getName());
        assertEquals("", route.getName());
    }

    /* setName */

    @Test
    public void setNameUpdatesNameTest() {
        Route route = new Route();
        route.setName("My Route");
        assertEquals("My Route", route.getName());
    }

    @Test
    public void setNameAllowsNullValueTest() {
        Route route = new Route();
        route.setName("Something");
        route.setName(null);
        assertEquals("", route.getName());
    }
}
