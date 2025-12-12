package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PointTest {

    // -------------------------------------------------------------
    // Constructors tests
    // -------------------------------------------------------------

    @Test
    public void constructorWithLatLonSetsGeoPositionCorrectlyTest() {
        Point p = new Point(10.5, -20.25, Point.PointType.START);

        assertEquals(10.5, p.getLatitude(), 0.000001);
        assertEquals(-20.25, p.getLongitude(), 0.000001);
        assertEquals(Point.PointType.START, p.getType());
    }

    @Test
    public void constructorWithGeoPositionStoresExactGeoPositionReferenceTest() {
        GeoPosition gp = new GeoPosition(5.0, 6.0);
        Point p = new Point(gp, Point.PointType.WAYPOINT);

        assertSame(gp, p.getGeoPosition());
        assertEquals(5.0, p.getLatitude(), 0.000001);
        assertEquals(6.0, p.getLongitude(), 0.000001);
    }

    // -------------------------------------------------------------
    // getGeoPosition() tests
    // -------------------------------------------------------------

    @Test
    public void getGeoPositionReturnsStoredGeoPositionTest() {
        GeoPosition gp = new GeoPosition(1.0, 2.0);
        Point p = new Point(gp, Point.PointType.END);

        assertSame(gp, p.getGeoPosition());
    }

    @Test
    public void getGeoPositionReturnsNonNullForValidPointTest() {
        Point p = new Point(1.0, 2.0, Point.PointType.WAYPOINT);

        assertNotNull(p.getGeoPosition());
    }

    // -------------------------------------------------------------
    // getLatitude() tests
    // -------------------------------------------------------------

    @Test
    public void getLatitudeMatchesGeoPositionLatitudeTest() {
        Point p = new Point(12.34, 56.78, Point.PointType.START);

        assertEquals(12.34, p.getLatitude(), 0.000001);
    }

    @Test
    public void getLatitudeHandlesNegativeValuesTest() {
        Point p = new Point(-45.0, 10.0, Point.PointType.END);

        assertEquals(-45.0, p.getLatitude(), 0.000001);
    }

    // -------------------------------------------------------------
    // getLongitude() tests
    // -------------------------------------------------------------

    @Test
    public void getLongitudeMatchesGeoPositionLongitudeTest() {
        Point p = new Point(10.0, -33.33, Point.PointType.WAYPOINT);

        assertEquals(-33.33, p.getLongitude(), 0.000001);
    }

    @Test
    public void getLongitudeHandlesPositiveValuesTest() {
        Point p = new Point(10.0, 99.99, Point.PointType.START);

        assertEquals(99.99, p.getLongitude(), 0.000001);
    }

    // -------------------------------------------------------------
    // getType() tests
    // -------------------------------------------------------------

    @Test
    public void getTypeReturnsTypeProvidedInConstructorTest() {
        Point p = new Point(0.0, 0.0, Point.PointType.INTERPOLATED);

        assertEquals(Point.PointType.INTERPOLATED, p.getType());
    }

    @Test
    public void getTypeReturnsUpdatedTypeAfterSetTypeTest() {
        Point p = new Point(0.0, 0.0, Point.PointType.START);
        p.setType(Point.PointType.END);

        assertEquals(Point.PointType.END, p.getType());
    }

    // -------------------------------------------------------------
    // setType(...) tests
    // -------------------------------------------------------------

    @Test
    public void setTypeUpdatesStoredPointTypeTest() {
        Point p = new Point(0.0, 0.0, Point.PointType.WAYPOINT);

        p.setType(Point.PointType.INTERPOLATED);

        assertEquals(Point.PointType.INTERPOLATED, p.getType());
    }

    @Test
    public void setTypeAllowsSettingAnyEnumVariantTest() {
        Point p = new Point(0.0, 0.0, Point.PointType.WAYPOINT);

        p.setType(Point.PointType.START);
        assertEquals(Point.PointType.START, p.getType());

        p.setType(Point.PointType.END);
        assertEquals(Point.PointType.END, p.getType());
    }

    // -------------------------------------------------------------
    // distanceBetweenPointsUsingHaversineFormula(...) tests
    // -------------------------------------------------------------

    @Test
    public void distanceBetweenPointsReturnsZeroForSameCoordinatesTest() {
        Point a = new Point(10.0, 20.0, Point.PointType.START);
        Point b = new Point(10.0, 20.0, Point.PointType.END);

        double distance = a.distanceBetweenPointsUsingHaversineFormula(b);

        assertEquals(0.0, distance, 0.0000001);
    }

    @Test
    public void distanceBetweenPointsReturnsPositiveDistanceForDifferentPointsTest() {
        Point a = new Point(10.0, 20.0, Point.PointType.START);
        Point b = new Point(10.1, 20.1, Point.PointType.END);

        double distance = a.distanceBetweenPointsUsingHaversineFormula(b);

        assertTrue(distance > 0.0);
    }

    // -------------------------------------------------------------
    // toString() tests
    // -------------------------------------------------------------

    @Test
    public void toStringIncludesCoordinatesAndTypeTest() {
        Point p = new Point(12.3456, 78.9, Point.PointType.WAYPOINT);

        String s = p.toString();

        assertTrue(s.contains("12.345600"));
        assertTrue(s.contains("78.900000"));
        assertTrue(s.contains("WAYPOINT"));
    }

    @Test
    public void toStringProducesNonEmptyStringTest() {
        Point p = new Point(1.0, 2.0, Point.PointType.START);

        String str = p.toString();

        assertNotNull(str);
        assertFalse(str.trim().isEmpty());
    }
}
