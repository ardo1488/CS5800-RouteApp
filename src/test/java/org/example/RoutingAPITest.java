package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RoutingAPITest {

    // -------------------------------------------------------------
    // Constructor tests
    // -------------------------------------------------------------

    @Test
    public void constructorSetsApiKeyAndHasApiKeyTrueForNonEmptyKeyTest() {
        RoutingAPI api = new RoutingAPI("my-key");

        assertTrue(api.hasApiKey());
    }

    @Test
    public void constructorSetsDefaultProfileFootWalkingTest() {
        RoutingAPI api = new RoutingAPI("my-key");

        assertEquals(RoutingAPI.RoutingProfile.FOOT_WALKING, api.getProfile());
    }

    // -------------------------------------------------------------
    // hasApiKey() tests
    // -------------------------------------------------------------

    @Test
    public void hasApiKeyReturnsTrueWhenNonEmptyKeyProvidedTest() {
        RoutingAPI api = new RoutingAPI("abc123");

        assertTrue(api.hasApiKey());
    }

    @Test
    public void hasApiKeyReturnsFalseWhenNullOrBlankKeyTest() {
        RoutingAPI apiNull = new RoutingAPI(null);
        RoutingAPI apiBlank = new RoutingAPI("   ");

        assertFalse(apiNull.hasApiKey());
        assertFalse(apiBlank.hasApiKey());
    }

    // -------------------------------------------------------------
    // setProfile(...) tests
    // -------------------------------------------------------------

    @Test
    public void setProfileUpdatesCurrentProfileTest() {
        RoutingAPI api = new RoutingAPI("key");
        api.setProfile(RoutingAPI.RoutingProfile.CYCLING_REGULAR);

        assertEquals(RoutingAPI.RoutingProfile.CYCLING_REGULAR, api.getProfile());
    }

    @Test
    public void setProfileAllowsNullProfileAndStoresItTest() {
        RoutingAPI api = new RoutingAPI("key");
        api.setProfile(null);

        assertNull(api.getProfile());
    }

    // -------------------------------------------------------------
    // getProfile() tests
    // -------------------------------------------------------------

    @Test
    public void getProfileReturnsDefaultFootWalkingWhenNotChangedTest() {
        RoutingAPI api = new RoutingAPI("key");

        assertEquals(RoutingAPI.RoutingProfile.FOOT_WALKING, api.getProfile());
    }

    @Test
    public void getProfileReflectsMostRecentlySetProfileTest() {
        RoutingAPI api = new RoutingAPI("key");
        api.setProfile(RoutingAPI.RoutingProfile.DRIVING_CAR);

        assertEquals(RoutingAPI.RoutingProfile.DRIVING_CAR, api.getProfile());
    }

    // -------------------------------------------------------------
    // generateRoundTripWithAPI(startPoint, distanceKm) tests
    // -------------------------------------------------------------

    @Test
    public void generateRoundTripWithAPIDistanceOnlyReturnsNullWhenNoApiKeyTest() {
        RoutingAPI api = new RoutingAPI(null);
        GeoPosition start = new GeoPosition(10.0, 20.0);

        RouteResult result = api.generateRoundTripWithAPI(start, 5.0);

        assertNull(result);
    }

    @Test
    public void generateRoundTripWithAPIDistanceOnlyReturnsNullWhenDistanceNonPositiveTest() {
        RoutingAPI api = new RoutingAPI("");
        GeoPosition start = new GeoPosition(10.0, 20.0);

        RouteResult resultZero = api.generateRoundTripWithAPI(start, 0.0);
        RouteResult resultNegative = api.generateRoundTripWithAPI(start, -1.0);

        assertNull(resultZero);
        assertNull(resultNegative);
    }

    // -------------------------------------------------------------
    // generateRoundTripWithAPI(startPoint, distanceKm, points, seed) tests
    // -------------------------------------------------------------

    @Test
    public void generateRoundTripWithAPIFullSignatureReturnsNullWhenStartPointNullTest() {
        RoutingAPI api = new RoutingAPI("   "); // blank -> no API key

        RouteResult result = api.generateRoundTripWithAPI(null, 5.0, 5, 42);

        assertNull(result);
    }

    @Test
    public void generateRoundTripWithAPIFullSignatureReturnsNullWhenNoApiKeyTest() {
        RoutingAPI api = new RoutingAPI(null);
        GeoPosition start = new GeoPosition(0.0, 0.0);

        RouteResult result = api.generateRoundTripWithAPI(start, 10.0, 5, null);

        assertNull(result);
    }

    // -------------------------------------------------------------
    // snapToRoadsWithTwoPoints(...) tests
    // -------------------------------------------------------------

    @Test
    public void snapToRoadsWithTwoPointsReturnsNullWhenLessThanTwoWaypointsTest() {
        RoutingAPI api = new RoutingAPI("key");
        List<GeoPosition> waypoints = new ArrayList<>();
        waypoints.add(new GeoPosition(1.0, 2.0));

        RouteResult result = api.snapToRoadsWithTwoPoints(waypoints);

        assertNull(result);
    }

    @Test
    public void snapToRoadsWithTwoPointsReturnsNullWhenNoApiKeyTest() {
        RoutingAPI api = new RoutingAPI(null);
        List<GeoPosition> waypoints = new ArrayList<>();
        waypoints.add(new GeoPosition(1.0, 2.0));
        waypoints.add(new GeoPosition(3.0, 4.0));

        RouteResult result = api.snapToRoadsWithTwoPoints(waypoints);

        assertNull(result);
    }

    // -------------------------------------------------------------
    // buildJsonRequestForRoundTrip(...) tests
    // -------------------------------------------------------------

    @Test
    public void buildJsonRequestForRoundTripIncludesCoordinatesDistanceAndPointsTest() {
        RoutingAPI api = new RoutingAPI("key");
        GeoPosition start = new GeoPosition(11.5, -122.3);
        int distanceMeters = 5000;
        int points = 7;

        String json = api.buildJsonRequestForRoundTrip(start, distanceMeters, points, null).toString();

        assertTrue(json.contains("\"coordinates\":[["));
        assertTrue(json.contains(String.valueOf(start.getLongitude())));
        assertTrue(json.contains(String.valueOf(start.getLatitude())));
        assertTrue(json.contains("\"length\":" + distanceMeters));
        assertTrue(json.contains("\"points\":" + points));
        assertFalse(json.contains("\"seed\""));
    }

    @Test
    public void buildJsonRequestForRoundTripIncludesSeedWhenProvidedTest() {
        RoutingAPI api = new RoutingAPI("key");
        GeoPosition start = new GeoPosition(40.0, -70.0);

        String json = api.buildJsonRequestForRoundTrip(start, 10000, 5, 12345).toString();

        assertTrue(json.contains("\"seed\":12345"));
    }

    // -------------------------------------------------------------
    // buildJasonRequestForRouteWithProfile(...) tests
    // -------------------------------------------------------------

    @Test
    public void buildJasonRequestForRouteWithProfileSingleWaypointFormatsCoordinatesTest() {
        RoutingAPI api = new RoutingAPI("key");
        List<GeoPosition> waypoints = new ArrayList<>();
        GeoPosition gp = new GeoPosition(10.0, 20.0);
        waypoints.add(gp);

        String json = api.buildJasonRequestForRouteWithProfile(waypoints).toString();

        String expectedCoord = "[" + gp.getLongitude() + "," + gp.getLatitude() + "]";
        assertTrue(json.contains(expectedCoord));
        assertTrue(json.contains("\"elevation\":true"));
        assertTrue(json.contains("\"format\":\"geojson\""));
    }

    @Test
    public void buildJasonRequestForRouteWithProfileMultipleWaypointsCommaSeparatedTest() {
        RoutingAPI api = new RoutingAPI("key");
        List<GeoPosition> waypoints = new ArrayList<>();
        waypoints.add(new GeoPosition(1.0, 2.0));
        waypoints.add(new GeoPosition(3.0, 4.0));

        String json = api.buildJasonRequestForRouteWithProfile(waypoints).toString();

        assertTrue(json.contains("[" + waypoints.get(0).getLongitude() + "," + waypoints.get(0).getLatitude() + "]"));
        assertTrue(json.contains("[" + waypoints.get(1).getLongitude() + "," + waypoints.get(1).getLatitude() + "]"));
        assertTrue(json.contains("],[")); // comma between coordinate pairs
    }

    // -------------------------------------------------------------
    // checkIfResponseHasError(...) tests
    // -------------------------------------------------------------

    @Test
    public void checkIfResponseHasErrorDoesNothingWhenNoErrorFieldPresentTest() {
        RoutingAPI api = new RoutingAPI("key");
        String response = "{\"summary\":{\"distance\":1000}}";

        // Should not throw
        api.checkIfResponseHasError(response);
    }

    @Test
    public void checkIfResponseHasErrorHandlesErrorFieldWithoutThrowingTest() {
        RoutingAPI api = new RoutingAPI("key");
        String response = "{\"error\":{\"code\":400,\"message\":\"Bad request\"}}";

        // Method only logs; just ensure it does not throw
        api.checkIfResponseHasError(response);
    }

    // -------------------------------------------------------------
    // convertKMtoMeters(...) tests
    // -------------------------------------------------------------

    @Test
    public void convertKMtoMetersConvertsWholeKilometersCorrectlyTest() {
        RoutingAPI api = new RoutingAPI("key");

        assertEquals(5000, api.convertKMtoMeters(5.0));
        assertEquals(1000, api.convertKMtoMeters(1.0));
    }

    @Test
    public void convertKMtoMetersHandlesZeroAndFractionalValuesTest() {
        RoutingAPI api = new RoutingAPI("key");

        assertEquals(0, api.convertKMtoMeters(0.0));
        assertEquals(1, api.convertKMtoMeters(0.001)); // 0.001 km = 1 m
    }

    // -------------------------------------------------------------
    // makeSureAtLeast3Points(...) tests
    // -------------------------------------------------------------

    @Test
    public void makeSureAtLeast3PointsReturnsThreeWhenInputLessThanThreeTest() {
        RoutingAPI api = new RoutingAPI("key");

        assertEquals(3, api.makeSureAtLeast3Points(0));
        assertEquals(3, api.makeSureAtLeast3Points(2));
    }

    @Test
    public void makeSureAtLeast3PointsReturnsSameWhenInputThreeOrMoreTest() {
        RoutingAPI api = new RoutingAPI("key");

        assertEquals(3, api.makeSureAtLeast3Points(3));
        assertEquals(5, api.makeSureAtLeast3Points(5));
        assertEquals(10, api.makeSureAtLeast3Points(10));
    }
}
