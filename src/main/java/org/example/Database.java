package org.example;

import org.jxmapviewer.viewer.GeoPosition;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DB API stays the same for the rest of the app:
 * - saveRoute(name, distance, elevation, List<GeoPosition>)
 * - loadRoutePoints(routeId) -> List<GeoPosition>
 *
 * Internally we still persist lat/lon; Route wraps/unwraps to Point.
 */
public class Database {

    private static Database instance;
    private Connection connection;

    private Database() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:routes.db");
            createTablesIfNeeded();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Database getInstance() {
        if (instance == null) instance = new Database();
        return instance;
    }

    private void createTablesIfNeeded() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS routes (" +
                            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "  name TEXT," +
                            "  distance REAL," +
                            "  elevation INTEGER)"
            );
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS route_points (" +
                            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "  route_id INTEGER," +
                            "  lat REAL," +
                            "  lon REAL," +
                            "  FOREIGN KEY(route_id) REFERENCES routes(id))"
            );
        }
    }

    private static class LatLonColumns {
        final String latCol, lonCol;
        LatLonColumns(String latCol, String lonCol) { this.latCol = latCol; this.lonCol = lonCol; }
    }

    private java.util.Map<String, Boolean> getColumns(String table) {
        java.util.Map<String, Boolean> cols = new java.util.HashMap<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) cols.put(name.toLowerCase(Locale.ROOT), true);
            }
        } catch (SQLException ignored) {}
        return cols;
    }

    private LatLonColumns detectLatLonColumns() {
        java.util.Map<String, Boolean> cols = getColumns("route_points");
        if (cols.containsKey("lat") && cols.containsKey("lon")) return new LatLonColumns("lat", "lon");
        if (cols.containsKey("latitude") && cols.containsKey("longitude")) return new LatLonColumns("latitude", "longitude");
        if (cols.containsKey("y") && cols.containsKey("x")) return new LatLonColumns("y", "x");
        return new LatLonColumns("lat", "lon");
    }

    public int saveRoute(String name, double distance, int elevation, List<GeoPosition> points) {
        int routeId = -1;
        try {
            String insertRoute = "INSERT INTO routes (name, distance, elevation) VALUES (?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertRoute)) {
                ps.setString(1, name);
                ps.setDouble(2, distance);
                ps.setInt(3, elevation);
                ps.executeUpdate();
            }
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) routeId = rs.getInt(1);
            }

            if (routeId > 0 && points != null && !points.isEmpty()) {
                LatLonColumns cols = detectLatLonColumns();
                String insertPoint = "INSERT INTO route_points (route_id, " + cols.latCol + ", " + cols.lonCol + ") VALUES (?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(insertPoint)) {
                    for (GeoPosition p : points) {
                        ps.setInt(1, routeId);
                        ps.setDouble(2, p.getLatitude());
                        ps.setDouble(3, p.getLongitude());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return routeId;
    }

    public List<GeoPosition> loadRoutePoints(int routeId) {
        List<GeoPosition> pts = new ArrayList<>();
        LatLonColumns cols = detectLatLonColumns();
        String sql = "SELECT " + cols.latCol + " AS latVal, " + cols.lonCol + " AS lonVal " +
                "FROM route_points WHERE route_id = ? ORDER BY id ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, routeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pts.add(new GeoPosition(rs.getDouble("latVal"), rs.getDouble("lonVal")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pts;
    }

    public void close() {
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
    }

    public java.util.List<RouteSummary> getAllRoutes() {
        java.util.List<RouteSummary> list = new java.util.ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, distance, elevation FROM routes ORDER BY id DESC")) {
            while (rs.next()) {
                list.add(new RouteSummary(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("distance"),
                        rs.getInt("elevation")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static class RouteSummary {
        private final int id;
        private final String name;
        private final double distance;
        private final int elevation;

        public RouteSummary(int id, String name, double distance, int elevation) {
            this.id = id; this.name = name; this.distance = distance; this.elevation = elevation;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public double getDistance() { return distance; }
        public int getElevation() { return elevation; }

        @Override public String toString() { return String.format("%s (%.2f km, %d m)", name, distance, elevation); }
    }
}
