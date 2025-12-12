package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {

    // ---------- reflection / setup helpers ----------

    private Database getDatabaseWithInMemoryConnection() {
        try {
            Database db = Database.getInstance();

            // Swap out the file-based connection with an in-memory connection
            Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
            setField(db, "connection", conn);

            // Recreate tables on this in-memory connection
            invokeCreateTablesIfNeeded(db);

            return db;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invokeCreateTablesIfNeeded(Database db) {
        try {
            Method m = Database.class.getDeclaredMethod("createTablesIfNeeded");
            m.setAccessible(true);
            m.invoke(db);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------
    // getInstance() tests
    // -------------------------------------------------------------

    @Test
    public void getInstanceNeverReturnsNullTest() {
        Database db = Database.getInstance();
        assertNotNull(db);
    }

    @Test
    public void getInstanceReturnsSameSingletonInstanceTest() {
        Database db1 = Database.getInstance();
        Database db2 = Database.getInstance();
        assertSame(db1, db2);
    }

    // -------------------------------------------------------------
    // saveRoute(...) tests
    // -------------------------------------------------------------

    @Test
    public void saveRouteWithPointsPersistsRouteAndPointsTest() {
        Database db = getDatabaseWithInMemoryConnection();

        List<GeoPosition> points = new ArrayList<>();
        points.add(new GeoPosition(10.0, 20.0));
        points.add(new GeoPosition(11.0, 21.0));
        points.add(new GeoPosition(12.0, 22.0));

        int routeId = db.saveRoute("My Route", 10.5, 200, points);

        assertTrue(routeId > 0, "routeId should be positive");

        List<GeoPosition> loaded = db.loadRoutePoints(routeId);
        assertEquals(3, loaded.size());
        assertEquals(10.0, loaded.get(0).getLatitude(), 0.000001);
        assertEquals(22.0, loaded.get(2).getLongitude(), 0.000001);
    }

    @Test
    public void saveRouteWithNullPointsCreatesRouteWithoutRoutePointsTest() {
        Database db = getDatabaseWithInMemoryConnection();

        int routeId = db.saveRoute("NoPoints", 3.3, 100, null);

        assertTrue(routeId > 0);

        List<GeoPosition> loaded = db.loadRoutePoints(routeId);
        assertNotNull(loaded);
        assertTrue(loaded.isEmpty());
    }

    // -------------------------------------------------------------
    // loadRoutePoints(...) tests
    // -------------------------------------------------------------

    @Test
    public void loadRoutePointsReturnsInsertedPointsInOrderTest() {
        Database db = getDatabaseWithInMemoryConnection();

        List<GeoPosition> points = new ArrayList<>();
        points.add(new GeoPosition(0.0, 0.0));
        points.add(new GeoPosition(1.0, 1.0));
        points.add(new GeoPosition(2.0, 2.0));

        int routeId = db.saveRoute("RouteA", 5.0, 50, points);

        List<GeoPosition> loaded = db.loadRoutePoints(routeId);

        assertEquals(3, loaded.size());
        assertEquals(0.0, loaded.get(0).getLatitude(), 0.000001);
        assertEquals(1.0, loaded.get(1).getLatitude(), 0.000001);
        assertEquals(2.0, loaded.get(2).getLatitude(), 0.000001);
    }

    @Test
    public void loadRoutePointsForUnknownRouteIdReturnsEmptyListTest() {
        Database db = getDatabaseWithInMemoryConnection();

        List<GeoPosition> loaded = db.loadRoutePoints(9999);

        assertNotNull(loaded);
        assertTrue(loaded.isEmpty());
    }

    // -------------------------------------------------------------
    // getAllRoutes() tests
    // -------------------------------------------------------------

    @Test
    public void getAllRoutesReturnsEmptyListWhenNoRoutesExistTest() {
        Database db = getDatabaseWithInMemoryConnection();

        List<Database.RouteSummary> summaries = db.getAllRoutes();

        assertNotNull(summaries);
        assertTrue(summaries.isEmpty());
    }

    @Test
    public void getAllRoutesReturnsRoutesInDescendingIdOrderTest() {
        Database db = getDatabaseWithInMemoryConnection();

        db.saveRoute("Route1", 3.0, 30, null);
        db.saveRoute("Route2", 4.0, 40, null);

        List<Database.RouteSummary> summaries = db.getAllRoutes();

        assertTrue(summaries.size() >= 2);
        Database.RouteSummary first = summaries.get(0);
        Database.RouteSummary second = summaries.get(1);

        assertTrue(first.getId() > second.getId(), "First route id should be greater (DESC order)");
    }

    // -------------------------------------------------------------
    // userExists(...) tests
    // -------------------------------------------------------------

    @Test
    public void userExistsReturnsTrueForExistingUserTest() {
        Database db = getDatabaseWithInMemoryConnection();

        String username = "userexists_" + System.nanoTime();
        UserProfile profile = db.createNewUser(username, "pw");
        assertNotNull(profile);

        assertTrue(db.userExists(username));
    }

    @Test
    public void userExistsIsCaseInsensitiveForUsernameTest() {
        Database db = getDatabaseWithInMemoryConnection();

        String username = "MixedCaseUser_" + System.nanoTime();
        UserProfile profile = db.createNewUser(username, "pw");
        assertNotNull(profile);

        assertTrue(db.userExists(username.toLowerCase()));
        assertTrue(db.userExists(username.toUpperCase()));
    }

    // -------------------------------------------------------------
    // createNewUser(...) tests
    // -------------------------------------------------------------

    @Test
    public void createNewUserPersistsUserAndProfileAndReturnsUserProfileTest() {
        Database db = getDatabaseWithInMemoryConnection();

        String username = "newuser_" + System.nanoTime();
        String password = "secret123";

        UserProfile profile = db.createNewUser(username, password);

        assertNotNull(profile);
        assertTrue(profile.getUserId() > 0);
        assertEquals(username, profile.getUserName());
        assertTrue(db.userExists(username));
    }

    @Test
    public void createNewUserWithDuplicateUsernameReturnsNullAndDoesNotCreateSecondUserTest() {
        Database db = getDatabaseWithInMemoryConnection();

        String username = "dupuser_" + System.nanoTime();
        String password = "secret123";

        UserProfile first = db.createNewUser(username, password);
        assertNotNull(first);

        UserProfile second = db.createNewUser(username, password);
        assertNull(second);

        assertTrue(db.userExists(username));
    }

    // -------------------------------------------------------------
    // authenticateAUser(...) tests
    // -------------------------------------------------------------

    @Test
    public void authenticateAUserReturnsProfileForValidCredentialsTest() {
        Database db = getDatabaseWithInMemoryConnection();

        String username = "authuser_" + System.nanoTime();
        String password = "authpw";

        UserProfile created = db.createNewUser(username, password);
        assertNotNull(created);

        UserProfile auth = db.authenticateAUser(username, password);

        assertNotNull(auth);
        assertEquals(created.getUserId(), auth.getUserId());
        assertEquals(username, auth.getUserName());
    }

    @Test
    public void authenticateAUserReturnsNullForInvalidPasswordTest() {
        Database db = getDatabaseWithInMemoryConnection();

        String username = "authfail_" + System.nanoTime();
        String password = "rightpw";

        UserProfile created = db.createNewUser(username, password);
        assertNotNull(created);

        UserProfile auth = db.authenticateAUser(username, "wrongpw");

        assertNull(auth);
    }

    // -------------------------------------------------------------
    // saveUserToDatabase(...) tests
    // -------------------------------------------------------------

    @Test
    public void saveUserToDatabaseUpdatesProfileRowInDatabaseTest() {
        Database db = getDatabaseWithInMemoryConnection();

        String username = "profileuser_" + System.nanoTime();
        String password = "profpass";

        UserProfile profile = db.createNewUser(username, password);
        assertNotNull(profile);

        profile.setPreferredDistanceKm(12.3);
        profile.setPreferredRouteVariety(10);
        profile.setPreferHillRoutes(true);
        profile.setMaxElevationGain(500.0);
        profile.setUseMetricUnits(false);
        profile.setShowElevation(false);
        profile.setAutoFitRoute(false);

        profile.loadStatisticsFromDatabase(
                42.0,  // distance
                100.0, // elevation
                5,     // generated
                2      // completed
        );

        db.saveUserToDatabase(profile);

        UserProfile reloaded = db.authenticateAUser(username, password);
        assertNotNull(reloaded);

        assertEquals(12.3, reloaded.getPreferredDistanceKm(), 0.0001);
        assertEquals(10, reloaded.getPreferredRouteVariety());
        assertTrue(reloaded.isPreferHillRoutes());
        assertEquals(500.0, reloaded.getMaxElevationGain(), 0.0001);
        assertFalse(reloaded.isUseMetricUnits());
        assertFalse(reloaded.isShowElevation());
        assertFalse(reloaded.isAutoFitRoute());

        assertEquals(42.0, reloaded.getTotalDistanceRun(), 0.0001);
        assertEquals(100.0, reloaded.getTotalElevationGained(), 0.0001);
        assertEquals(5, reloaded.getTotalRoutesGenerated());
        assertEquals(2, reloaded.getTotalRoutesCompleted());
    }

    @Test
    public void saveUserToDatabaseDoesNothingForGuestOrInvalidUserIdTest() {
        Database db = getDatabaseWithInMemoryConnection();

        UserProfile guest = UserProfile.getInstanceForNonLoggedInUser();

        assertDoesNotThrow(() -> db.saveUserToDatabase(guest));
        // Cannot easily assert DB side-effect, but method should safely return
    }

    // -------------------------------------------------------------
    // close() tests
    // -------------------------------------------------------------

    @Test
    public void closeClosesConnectionWithoutThrowingTest() {
        Database db = getDatabaseWithInMemoryConnection();

        assertDoesNotThrow(db::close);
    }

    @Test
    public void closeCanBeCalledMultipleTimesSafelyTest() {
        Database db = getDatabaseWithInMemoryConnection();

        assertDoesNotThrow(db::close);
        assertDoesNotThrow(db::close);
    }
}
