package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PointTest {

    /* getGeoPosition */

    @Test
    public void getGeoPositionReturnsSameCoordinatesTest() {
        Point point = new Point(10.0, 20.0, Point.PointType.START);
        GeoPosition geoPosition = point.getGeoPosition();

        assertEquals(10.0, geoPosition.getLatitude(), 1e-9);
        assertEquals(20.0, geoPosition.getLongitude(), 1e-9);
    }

    @Test
    public void getGeoPositionIsNotNullTest() {
        Point point = new Point(0.0, 0.0, Point.PointType.START);
        assertNotNull(point.getGeoPosition());
    }

    /* getLatitude */

    @Test
    public void getLatitudeReturnsCorrectLatitudeTest() {
        Point point = new Point(12.345678, 45.0, Point.PointType.START);
        assertEquals(12.345678, point.getLatitude(), 1e-9);
    }

    @Test
    public void getLatitudeSupportsNegativeValuesTest() {
        Point point = new Point(-33.865143, 151.209900, Point.PointType.START);
        assertEquals(-33.865143, point.getLatitude(), 1e-9);
    }

    /* getLongitude */

    @Test
    public void getLongitudeReturnsCorrectLongitudeTest() {
        Point point = new Point(10.0, 123.456789, Point.PointType.START);
        assertEquals(123.456789, point.getLongitude(), 1e-9);
    }

    @Test
    public void getLongitudeSupportsNegativeValuesTest() {
        Point point = new Point(51.5074, -0.1278, Point.PointType.START);
        assertEquals(-0.1278, point.getLongitude(), 1e-9);
    }

    /* getType */

    @Test
    public void getTypeReturnsInitialTypeTest() {
        Point point = new Point(0.0, 0.0, Point.PointType.WAYPOINT);
        assertEquals(Point.PointType.WAYPOINT, point.getType());
    }

    @Test
    public void getTypeReflectsUpdatedTypeTest() {
        Point point = new Point(0.0, 0.0, Point.PointType.START);
        point.setType(Point.PointType.END);
        assertEquals(Point.PointType.END, point.getType());
    }

    /* setType */

    @Test
    public void setTypeChangesTypeTest() {
        Point point = new Point(0.0, 0.0, Point.PointType.START);
        point.setType(Point.PointType.WAYPOINT);
        assertEquals(Point.PointType.WAYPOINT, point.getType());
    }

    @Test
    public void setTypeAllowsSettingInterpolatedTypeTest() {
        Point point = new Point(0.0, 0.0, Point.PointType.WAYPOINT);
        point.setType(Point.PointType.INTERPOLATED);
        assertEquals(Point.PointType.INTERPOLATED, point.getType());
    }

    /* distanceTo */

    @Test
    public void distanceToZeroDistanceForSamePointTest() {
        Point p1 = new Point(0.0, 0.0, Point.PointType.START);
        Point p2 = new Point(0.0, 0.0, Point.PointType.END);
        assertEquals(0.0, p1.distanceTo(p2), 1e-6);
    }

    @Test
    public void distanceToCorrectApproxDistanceBetweenPointsTest() {
        // Distance between (0,0) and (0,1) ≈ 111.19 km with R=6371
        Point p1 = new Point(0.0, 0.0, Point.PointType.START);
        Point p2 = new Point(0.0, 1.0, Point.PointType.END);

        double distance = p1.distanceTo(p2);
        assertTrue(distance > 110.0 && distance < 112.0,
                "Expected distance around 111 km but was " + distance);
    }

    /* toString */

    @Test
    public void toStringContainsLatitudeLongitudeAndTypeTest() {
        Point point = new Point(10.123456, 20.654321, Point.PointType.START);
        String s = point.toString();

        assertTrue(s.contains("10.123456"));
        assertTrue(s.contains("20.654321"));
        assertTrue(s.contains("START"));
    }

    @Test
    public void toStringUsesExpectedFormatTest() {
        Point point = new Point(1.0, 2.0, Point.PointType.END);
        String s = point.toString();

        assertTrue(s.startsWith("Point("));
        assertTrue(s.endsWith(")"));
    }
}
