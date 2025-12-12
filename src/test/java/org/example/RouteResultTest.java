package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RouteResultTest {

    private List<GeoPosition> createSamplePoints() {
        return Arrays.asList(
                new GeoPosition(10.0, 20.0),
                new GeoPosition(11.0, 21.0),
                new GeoPosition(12.0, 22.0)
        );
    }

    // -------------------------------------------------------------
    // Constructor / getPoints() tests
    // -------------------------------------------------------------

    @Test
    public void getPointsReturnsSameListReferencePassedIntoConstructorTest() {
        List<GeoPosition> points = createSamplePoints();
        RouteResult result = new RouteResult(points, 100.0, 80.0, 5000.0);

        assertSame(points, result.getPoints());
        assertEquals(3, result.getPoints().size());
    }

    @Test
    public void getPointsMayBeNullWhenConstructedWithNullListTest() {
        RouteResult result = new RouteResult(null, 0.0, 0.0, 0.0);

        assertNull(result.getPoints());
    }

    // -------------------------------------------------------------
    // getAscent() tests
    // -------------------------------------------------------------

    @Test
    public void getAscentReturnsValueProvidedToConstructorTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 123.45, 50.0, 3000.0);

        assertEquals(123.45, result.getAscent(), 0.0001);
    }

    @Test
    public void getAscentCanReturnZeroWhenNoAscentTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 0.0, 10.0, 1000.0);

        assertEquals(0.0, result.getAscent(), 0.0001);
    }

    // -------------------------------------------------------------
    // getDescent() tests
    // -------------------------------------------------------------

    @Test
    public void getDescentReturnsValueProvidedToConstructorTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 50.0, 200.5, 3000.0);

        assertEquals(200.5, result.getDescent(), 0.0001);
    }

    @Test
    public void getDescentCanReturnZeroWhenNoDescentTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 20.0, 0.0, 1000.0);

        assertEquals(0.0, result.getDescent(), 0.0001);
    }

    // -------------------------------------------------------------
    // getDistance() tests
    // -------------------------------------------------------------

    @Test
    public void getDistanceReturnsValueProvidedToConstructorTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 10.0, 5.0, 4321.0);

        assertEquals(4321.0, result.getDistance(), 0.0001);
    }

    @Test
    public void getDistanceAllowsZeroDistanceTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 10.0, 5.0, 0.0);

        assertEquals(0.0, result.getDistance(), 0.0001);
    }

    // -------------------------------------------------------------
    // getElevationGain() tests
    // -------------------------------------------------------------

    @Test
    public void getElevationGainMatchesAscentValueTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 150.0, 75.0, 2500.0);

        assertEquals(150.0, result.getElevationGain(), 0.0001);
        assertEquals(result.getAscent(), result.getElevationGain(), 0.0001);
    }

    @Test
    public void getElevationGainCanBeZeroTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 0.0, 50.0, 2000.0);

        assertEquals(0.0, result.getElevationGain(), 0.0001);
    }

    // -------------------------------------------------------------
    // getElevationLoss() tests
    // -------------------------------------------------------------

    @Test
    public void getElevationLossMatchesDescentValueTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 80.0, 120.0, 2500.0);

        assertEquals(120.0, result.getElevationLoss(), 0.0001);
        assertEquals(result.getDescent(), result.getElevationLoss(), 0.0001);
    }

    @Test
    public void getElevationLossCanBeZeroTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 50.0, 0.0, 2000.0);

        assertEquals(0.0, result.getElevationLoss(), 0.0001);
    }

    // -------------------------------------------------------------
    // hasElevationData() tests
    // -------------------------------------------------------------

    @Test
    public void hasElevationDataTrueWhenAscentPositiveTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 10.0, 0.0, 1000.0);

        assertTrue(result.hasElevationData());
    }

    @Test
    public void hasElevationDataTrueWhenDescentPositiveTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 0.0, 20.0, 1000.0);

        assertTrue(result.hasElevationData());
    }

    @Test
    public void hasElevationDataFalseWhenBothAscentAndDescentZeroTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 0.0, 0.0, 1000.0);

        assertFalse(result.hasElevationData());
    }

    @Test
    public void hasElevationDataTrueWhenBothAscentAndDescentPositiveTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 5.0, 7.0, 1000.0);

        assertTrue(result.hasElevationData());
    }

    // -------------------------------------------------------------
    // hasPoints() tests
    // -------------------------------------------------------------

    @Test
    public void hasPointsTrueWhenListNonEmptyTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 0.0, 0.0, 0.0);

        assertTrue(result.hasPoints());
    }

    @Test
    public void hasPointsFalseWhenListNullOrEmptyTest() {
        RouteResult resultNull = new RouteResult(null, 0.0, 0.0, 0.0);
        RouteResult resultEmpty = new RouteResult(Collections.emptyList(), 0.0, 0.0, 0.0);

        assertFalse(resultNull.hasPoints());
        assertFalse(resultEmpty.hasPoints());
    }

    // -------------------------------------------------------------
    // getPointCount() tests
    // -------------------------------------------------------------

    @Test
    public void getPointCountReturnsSizeOfPointsListWhenNonNullTest() {
        RouteResult result = new RouteResult(createSamplePoints(), 0.0, 0.0, 0.0);

        assertEquals(3, result.getPointCount());
    }

    @Test
    public void getPointCountReturnsZeroWhenPointsNullOrEmptyTest() {
        RouteResult resultNull = new RouteResult(null, 0.0, 0.0, 0.0);
        RouteResult resultEmpty = new RouteResult(Collections.emptyList(), 0.0, 0.0, 0.0);

        assertEquals(0, resultNull.getPointCount());
        assertEquals(0, resultEmpty.getPointCount());
    }
}
