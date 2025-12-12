package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RouteTest {

    private Point makePoint(double lat, double lon, Point.PointType type) {
        return new Point(lat, lon, type);
    }

    private Route createRouteWithTwoPoints() {
        Route route = new Route();
        route.addWaypoint(makePoint(10.0, 20.0, Point.PointType.WAYPOINT));
        route.addWaypoint(makePoint(10.1, 20.1, Point.PointType.WAYPOINT));
        return route;
    }

    // -------------------------------------------------------------
    // addWaypoint(Point) tests
    // -------------------------------------------------------------

    @Test
    public void addWaypointPointSetsStartAndEndTypesOnFirstPointTest() {
        Route route = new Route();
        Point p = makePoint(10.0, 20.0, Point.PointType.WAYPOINT);

        route.addWaypoint(p);

        List<Point> pts = route.getPoints();
        assertEquals(1, pts.size());
        assertEquals(Point.PointType.END, pts.get(0).getType()); // also marked END
    }

    @Test
    public void addWaypointPointPromotesPreviousEndToWaypointAndNewIsEndTest() {
        Route route = new Route();
        Point p1 = makePoint(10.0, 20.0, Point.PointType.WAYPOINT);
        Point p2 = makePoint(10.1, 20.1, Point.PointType.WAYPOINT);

        route.addWaypoint(p1);
        route.addWaypoint(p2);

        List<Point> pts = route.getPoints();
        assertEquals(2, pts.size());
        assertEquals(Point.PointType.WAYPOINT, pts.get(0).getType());
        assertEquals(Point.PointType.END, pts.get(1).getType());
    }

    // -------------------------------------------------------------
    // addWaypoint(GeoPosition) tests
    // -------------------------------------------------------------

    @Test
    public void addWaypointGeoPositionAddsPointWithMatchingCoordinatesTest() {
        Route route = new Route();
        GeoPosition gp = new GeoPosition(11.0, 22.0);

        route.addWaypoint(gp);

        List<Point> pts = route.getPoints();
        assertEquals(1, pts.size());
        GeoPosition stored = pts.get(0).getGeoPosition();
        assertEquals(gp.getLatitude(), stored.getLatitude(), 0.000001);
        assertEquals(gp.getLongitude(), stored.getLongitude(), 0.000001);
    }

    @Test
    public void addWaypointGeoPositionIgnoresNullInputTest() {
        Route route = new Route();

        route.addWaypoint((GeoPosition) null);

        assertTrue(route.getPoints().isEmpty());
    }

    // -------------------------------------------------------------
    // clear() tests
    // -------------------------------------------------------------

    @Test
    public void clearRemovesAllPointsAndResetsElevationTest() {
        Route route = createRouteWithTwoPoints();
        route.setElevation(100.0, 50.0);

        route.clear();

        assertTrue(route.getPoints().isEmpty());
        assertEquals(0.0, route.getAscentInMeters(), 0.0001);
        assertEquals(0.0, route.getDescentInMeters(), 0.0001);
    }

    @Test
    public void clearOnEmptyRouteKeepsRouteInConsistentStateTest() {
        Route route = new Route();

        route.clear();

        assertTrue(route.getPoints().isEmpty());
        assertEquals(0.0, route.getAscentInMeters(), 0.0001);
        assertEquals(0.0, route.getDescentInMeters(), 0.0001);
    }

    // -------------------------------------------------------------
    // isEmpty() tests
    // -------------------------------------------------------------

    @Test
    public void isEmptyReturnsTrueWhenNoPointsTest() {
        Route route = new Route();

        assertTrue(route.isEmpty());
    }

    @Test
    public void isEmptyReturnsFalseWhenPointsExistTest() {
        Route route = new Route();
        route.addWaypoint(makePoint(10.0, 20.0, Point.PointType.WAYPOINT));

        assertFalse(route.isEmpty());
    }

    // -------------------------------------------------------------
    // getPoints() tests
    // -------------------------------------------------------------

    @Test
    public void getPointsReturnsUnmodifiableListTest() {
        Route route = createRouteWithTwoPoints();

        List<Point> pts = route.getPoints();

        assertThrows(UnsupportedOperationException.class, () -> pts.add(makePoint(0, 0, Point.PointType.WAYPOINT)));
    }

    @Test
    public void getPointsReflectsInternalOrderOfPointsTest() {
        Route route = new Route();
        Point p1 = makePoint(1.0, 2.0, Point.PointType.WAYPOINT);
        Point p2 = makePoint(3.0, 4.0, Point.PointType.WAYPOINT);
        route.addWaypoint(p1);
        route.addWaypoint(p2);

        List<Point> pts = route.getPoints();
        assertEquals(2, pts.size());
        assertEquals(p1.getGeoPosition().getLatitude(), pts.get(0).getGeoPosition().getLatitude(), 0.000001);
        assertEquals(p2.getGeoPosition().getLatitude(), pts.get(1).getGeoPosition().getLatitude(), 0.000001);
    }

    // -------------------------------------------------------------
    // getAllPointsAsGeoPositions() tests
    // -------------------------------------------------------------

    @Test
    public void getAllPointsAsGeoPositionsReturnsListWithSameSizeTest() {
        Route route = createRouteWithTwoPoints();

        List<GeoPosition> gps = route.getAllPointsAsGeoPositions();

        assertEquals(route.getPoints().size(), gps.size());
    }

    @Test
    public void getAllPointsAsGeoPositionsCopiesGeoPositionsFromPointsTest() {
        Route route = new Route();
        GeoPosition gp1 = new GeoPosition(5.0, 6.0);
        GeoPosition gp2 = new GeoPosition(7.0, 8.0);
        route.addWaypoint(gp1);
        route.addWaypoint(gp2);

        List<GeoPosition> gps = route.getAllPointsAsGeoPositions();

        assertEquals(gp1.getLatitude(), gps.get(0).getLatitude(), 0.000001);
        assertEquals(gp2.getLatitude(), gps.get(1).getLatitude(), 0.000001);
    }

    // -------------------------------------------------------------
    // loadRouteFromGeoPositions(...) tests
    // -------------------------------------------------------------

    @Test
    public void loadRouteFromGeoPositionsClearsExistingPointsAndLoadsNewTest() {
        Route route = createRouteWithTwoPoints();

        List<GeoPosition> pts = new ArrayList<>();
        pts.add(new GeoPosition(0.0, 0.0));
        pts.add(new GeoPosition(1.0, 1.0));
        pts.add(new GeoPosition(2.0, 2.0));

        route.loadRouteFromGeoPositions(pts);

        assertEquals(3, route.getPoints().size());
    }

    @Test
    public void loadRouteFromGeoPositionsAssignsStartWaypointEndTypesTest() {
        Route route = new Route();
        List<GeoPosition> pts = new ArrayList<>();
        pts.add(new GeoPosition(0.0, 0.0));
        pts.add(new GeoPosition(1.0, 1.0));
        pts.add(new GeoPosition(2.0, 2.0));

        route.loadRouteFromGeoPositions(pts);

        List<Point> loaded = route.getPoints();
        assertEquals(Point.PointType.START, loaded.get(0).getType());
        assertEquals(Point.PointType.WAYPOINT, loaded.get(1).getType());
        assertEquals(Point.PointType.END, loaded.get(2).getType());
    }

    // -------------------------------------------------------------
    // getTotalDistance() tests
    // -------------------------------------------------------------

    @Test
    public void getTotalDistanceReturnsZeroForLessThanTwoPointsTest() {
        Route route = new Route();
        assertEquals(0.0, route.getTotalDistance(), 0.0001);

        route.addWaypoint(makePoint(10.0, 20.0, Point.PointType.WAYPOINT));
        assertEquals(0.0, route.getTotalDistance(), 0.0001);
    }

    @Test
    public void getTotalDistanceReturnsPositiveValueForSeparatedPointsTest() {
        Route route = new Route();
        route.addWaypoint(makePoint(10.0, 20.0, Point.PointType.WAYPOINT));
        route.addWaypoint(makePoint(10.1, 20.1, Point.PointType.WAYPOINT));

        double distance = route.getTotalDistance();

        assertTrue(distance > 0.0);
    }

    // -------------------------------------------------------------
    // getAscentInMeters() / setAscentInMeters(...) tests
    // -------------------------------------------------------------

    @Test
    public void setAscentInMetersStoresValueRetrievableByGetterTest() {
        Route route = new Route();
        route.setAscentInMeters(123.45);

        assertEquals(123.45, route.getAscentInMeters(), 0.0001);
    }

    @Test
    public void getAscentInMetersDefaultIsZeroTest() {
        Route route = new Route();

        assertEquals(0.0, route.getAscentInMeters(), 0.0001);
    }

    // -------------------------------------------------------------
    // getDescentInMeters() / setDescentInMeters(...) tests
    // -------------------------------------------------------------

    @Test
    public void setDescentInMetersStoresValueRetrievableByGetterTest() {
        Route route = new Route();
        route.setDescentInMeters(50.5);

        assertEquals(50.5, route.getDescentInMeters(), 0.0001);
    }

    @Test
    public void getDescentInMetersDefaultIsZeroTest() {
        Route route = new Route();

        assertEquals(0.0, route.getDescentInMeters(), 0.0001);
    }

    // -------------------------------------------------------------
    // addElevation(...) tests
    // -------------------------------------------------------------

    @Test
    public void addElevationAccumulatesAscentAndDescentTest() {
        Route route = new Route();
        route.addElevation(10.0, 5.0);
        route.addElevation(2.5, 1.5);

        assertEquals(12.5, route.getAscentInMeters(), 0.0001);
        assertEquals(6.5, route.getDescentInMeters(), 0.0001);
    }

    @Test
    public void addElevationCanHandleNegativeValuesTest() {
        Route route = new Route();
        route.setElevation(10.0, 10.0);

        route.addElevation(-2.0, -3.0);

        assertEquals(8.0, route.getAscentInMeters(), 0.0001);
        assertEquals(7.0, route.getDescentInMeters(), 0.0001);
    }

    // -------------------------------------------------------------
    // setElevation(...) tests
    // -------------------------------------------------------------

    @Test
    public void setElevationOverridesExistingElevationValuesTest() {
        Route route = new Route();
        route.addElevation(10.0, 5.0);

        route.setElevation(100.0, 50.0);

        assertEquals(100.0, route.getAscentInMeters(), 0.0001);
        assertEquals(50.0, route.getDescentInMeters(), 0.0001);
    }

    @Test
    public void setElevationAllowsZeroValuesTest() {
        Route route = new Route();
        route.setElevation(0.0, 0.0);

        assertEquals(0.0, route.getAscentInMeters(), 0.0001);
        assertEquals(0.0, route.getDescentInMeters(), 0.0001);
    }

    // -------------------------------------------------------------
    // getEstimatedElevation() tests
    // -------------------------------------------------------------

    @Test
    public void getEstimatedElevationRoundsAscentToNearestIntegerDownTest() {
        Route route = new Route();
        route.setAscentInMeters(10.4);

        assertEquals(10, route.getEstimatedElevation());
    }

    @Test
    public void getEstimatedElevationRoundsAscentToNearestIntegerUpTest() {
        Route route = new Route();
        route.setAscentInMeters(10.5);

        assertEquals(11, route.getEstimatedElevation());
    }

    // -------------------------------------------------------------
    // createMemento() tests
    // -------------------------------------------------------------

    @Test
    public void createMementoCopiesPointsAndMetadataTest() {
        Route route = new Route();
        route.setId(5);
        route.setName("Test Route");
        route.setElevation(100.0, 50.0);
        route.addWaypoint(makePoint(10.0, 20.0, Point.PointType.START));
        route.addWaypoint(makePoint(11.0, 21.0, Point.PointType.END));

        RouteMemento m = route.createMemento();

        assertEquals(5, m.getId());
        assertEquals("Test Route", m.getName());
        assertEquals(100.0, m.getAscent(), 0.0001);
        assertEquals(50.0, m.getDescent(), 0.0001);
        assertEquals(route.getPoints().size(), m.getPoints().size());
    }

    @Test
    public void createMementoIsNotAffectedByLaterRouteModificationsTest() {
        Route route = new Route();
        route.addWaypoint(makePoint(10.0, 20.0, Point.PointType.START));

        RouteMemento m = route.createMemento();
        int originalMementoPointCount = m.getPoints().size();

        // Modify route after creating memento
        route.addWaypoint(makePoint(11.0, 21.0, Point.PointType.END));

        assertEquals(originalMementoPointCount, m.getPoints().size());
    }

    // -------------------------------------------------------------
    // applyMemento(...) tests
    // -------------------------------------------------------------

    @Test
    public void applyMementoRestoresPointsAndMetadataTest() {
        Route route = new Route();
        route.setId(1);
        route.setName("Original");
        route.setElevation(10.0, 5.0);
        route.addWaypoint(makePoint(0.0, 0.0, Point.PointType.START));

        RouteMemento m = new RouteMemento(route.getPoints(), 99, "Restored", 100.0, 50.0);

        // mutate route
        route.addWaypoint(makePoint(1.0, 1.0, Point.PointType.END));
        route.setElevation(1.0, 1.0);
        route.setId(2);
        route.setName("Mutated");

        route.applyMemento(m);

        assertEquals(99, route.getId());
        assertEquals("Restored", route.getName());
        assertEquals(100.0, route.getAscentInMeters(), 0.0001);
        assertEquals(50.0, route.getDescentInMeters(), 0.0001);
        assertEquals(m.getPoints().size(), route.getPoints().size());
    }

    @Test
    public void applyMementoDoesNothingWhenNullTest() {
        Route route = new Route();
        route.setId(10);
        route.setName("Keep");
        route.setElevation(5.0, 2.0);
        route.addWaypoint(makePoint(0.0, 0.0, Point.PointType.START));

        route.applyMemento(null);

        assertEquals(10, route.getId());
        assertEquals("Keep", route.getName());
        assertEquals(5.0, route.getAscentInMeters(), 0.0001);
        assertEquals(2.0, route.getDescentInMeters(), 0.0001);
    }

    // -------------------------------------------------------------
    // getId() / setId(...) tests
    // -------------------------------------------------------------

    @Test
    public void setIdStoresIdRetrievableByGetterTest() {
        Route route = new Route();
        route.setId(123);

        assertEquals(123, route.getId());
    }

    @Test
    public void getIdDefaultIsMinusOneTest() {
        Route route = new Route();

        assertEquals(-1, route.getId());
    }

    // -------------------------------------------------------------
    // getName() / setName(...) tests
    // -------------------------------------------------------------

    @Test
    public void setNameStoresNameRetrievableByGetterTest() {
        Route route = new Route();
        route.setName("My Route");

        assertEquals("My Route", route.getName());
    }

    @Test
    public void getNameReturnsEmptyStringWhenUnderlyingNameIsNullTest() {
        Route route = new Route();
        route.setName(null);

        assertEquals("", route.getName());
    }
}
