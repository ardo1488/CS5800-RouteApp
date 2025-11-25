package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {

    private Connection getConnection(Database db) throws Exception {
        Field field = Database.class.getDeclaredField("connection");
        field.setAccessible(true);
        return (Connection) field.get(db);
    }

    private void clearDatabaseTables() throws Exception {
        Database db = Database.getInstance();
        Connection conn = getConnection(db);
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM route_points");
            st.executeUpdate("DELETE FROM routes");
        }
    }

    private Database createNewDatabaseInstance() throws Exception {
        Constructor<Database> ctor = Database.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    @BeforeEach
    public void beforeEachClearTables() throws Exception {
        clearDatabaseTables();
    }

    /* getInstance */

    @Test
    public void getInstanceReturnsSingletonInstanceTest() {
        Database db1 = Database.getInstance();
        Database db2 = Database.getInstance();
        assertSame(db1, db2);
    }

    @Test
    public void getInstanceProvidesOpenConnectionTest() throws Exception {
        Database db = Database.getInstance();
        Connection conn = getConnection(db);
        assertNotNull(conn);
        assertFalse(conn.isClosed());
    }

    /* saveRoute */

    @Test
    public void saveRoutePersistsRouteAndPointsTest() {
        Database db = Database.getInstance();
        List<GeoPosition> points = java.util.Arrays.asList(
                new GeoPosition(10.0, 20.0),
                new GeoPosition(11.0, 21.0)
        );

        int id = db.saveRoute("Route A", 123.45, 200, points);
        assertTrue(id > 0, "Route id should be positive");

        List<GeoPosition> loadedPoints = db.loadRoutePoints(id);
        assertEquals(2, loadedPoints.size());
        assertEquals(10.0, loadedPoints.get(0).getLatitude(), 1e-9);
        assertEquals(20.0, loadedPoints.get(0).getLongitude(), 1e-9);
    }

    @Test
    public void saveRouteWithoutPointsStoresRouteOnlyTest() throws Exception {
        Database db = Database.getInstance();

        int id = db.saveRoute("No Points Route", 0.0, 0, java.util.Collections.emptyList());
        assertTrue(id > 0);

        List<GeoPosition> loadedPoints = db.loadRoutePoints(id);
        assertNotNull(loadedPoints);
        assertTrue(loadedPoints.isEmpty(), "There should be no points for route without points");

        // Verify route exists in routes table
        Connection conn = getConnection(db);
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM routes WHERE id = " + id)) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    /* loadRoutePoints */

    @Test
    public void loadRoutePointsReturnsEmptyListForNonExistingRouteTest() {
        Database db = Database.getInstance();
        List<GeoPosition> pts = db.loadRoutePoints(999999);
        assertNotNull(pts);
        assertTrue(pts.isEmpty());
    }

    @Test
    public void loadRoutePointsReturnsCorrectCoordinatesTest() {
        Database db = Database.getInstance();
        List<GeoPosition> points = java.util.Arrays.asList(
                new GeoPosition(0.0, 0.0),
                new GeoPosition(1.0, 1.0)
        );
        int id = db.saveRoute("Coords Route", 0.0, 0, points);

        List<GeoPosition> loaded = db.loadRoutePoints(id);
        assertEquals(2, loaded.size());
        assertEquals(0.0, loaded.get(0).getLatitude(), 1e-9);
        assertEquals(0.0, loaded.get(0).getLongitude(), 1e-9);
        assertEquals(1.0, loaded.get(1).getLatitude(), 1e-9);
        assertEquals(1.0, loaded.get(1).getLongitude(), 1e-9);
    }

    /* getAllRoutes */

    @Test
    public void getAllRoutesReturnsSavedRoutesTest() {
        Database db = Database.getInstance();

        int id1 = db.saveRoute("Route1", 10.0, 100,
                java.util.Collections.singletonList(new GeoPosition(0.0, 0.0)));
        int id2 = db.saveRoute("Route2", 20.0, 200,
                java.util.Collections.singletonList(new GeoPosition(1.0, 1.0)));

        List<Database.RouteSummary> routes = db.getAllRoutes();
        assertTrue(routes.size() >= 2);

        boolean found1 = routes.stream().anyMatch(r -> r.getId() == id1);
        boolean found2 = routes.stream().anyMatch(r -> r.getId() == id2);
        assertTrue(found1);
        assertTrue(found2);
    }

    @Test
    public void getAllRoutesOrderedByIdDescendingTest() {
        Database db = Database.getInstance();

        int id1 = db.saveRoute("First", 10.0, 0,
                java.util.Collections.singletonList(new GeoPosition(0.0, 0.0)));
        int id2 = db.saveRoute("Second", 20.0, 0,
                java.util.Collections.singletonList(new GeoPosition(1.0, 1.0)));

        List<Database.RouteSummary> routes = db.getAllRoutes();
        assertTrue(routes.size() >= 2);

        // First element should have id >= second element
        Database.RouteSummary first = routes.get(0);
        Database.RouteSummary second = routes.get(1);
        assertTrue(first.getId() >= second.getId());
    }

    /* close */

    @Test
    public void closeClosesConnectionForNewInstanceTest() throws Exception {
        Database db = createNewDatabaseInstance();
        Connection conn = getConnection(db);
        assertFalse(conn.isClosed());

        db.close();
        assertTrue(conn.isClosed(), "Connection should be closed after close()");
    }

    @Test
    public void closeCanBeCalledMultipleTimesWithoutExceptionTest() throws Exception {
        Database db = createNewDatabaseInstance();
        db.close();
        assertDoesNotThrow(db::close);
    }
}
