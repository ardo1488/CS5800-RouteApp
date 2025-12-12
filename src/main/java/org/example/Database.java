package org.example;

import org.jxmapviewer.viewer.GeoPosition;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Singleton Database class handling both route storage and user authentication.
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
            // Routes table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS routes (" +
                            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "  name TEXT," +
                            "  distance REAL," +
                            "  elevation INTEGER)"
            );
            // Route points table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS route_points (" +
                            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "  route_id INTEGER," +
                            "  lat REAL," +
                            "  lon REAL," +
                            "  FOREIGN KEY(route_id) REFERENCES routes(id))"
            );
            // Users table with authentication info
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "  username TEXT UNIQUE NOT NULL," +
                            "  password_hash TEXT NOT NULL," +
                            "  salt TEXT NOT NULL," +
                            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
            );
            // User profiles table with preferences
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS user_profiles (" +
                            "  user_id INTEGER PRIMARY KEY," +
                            "  preferred_distance REAL DEFAULT 5.0," +
                            "  preferred_variety INTEGER DEFAULT 5," +
                            "  prefer_hills INTEGER DEFAULT 0," +
                            "  max_elevation REAL DEFAULT 200.0," +
                            "  use_metric INTEGER DEFAULT 1," +
                            "  show_elevation INTEGER DEFAULT 1," +
                            "  auto_fit_route INTEGER DEFAULT 1," +
                            "  total_distance REAL DEFAULT 0.0," +
                            "  total_elevation REAL DEFAULT 0.0," +
                            "  routes_generated INTEGER DEFAULT 0," +
                            "  routes_completed INTEGER DEFAULT 0," +
                            "  FOREIGN KEY(user_id) REFERENCES users(id))"
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


    public boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public UserProfile createNewUser(String username, String password) {
        try {
            String salt = generateRandomSaltForPasswordHashing();
            String passwordHash = hashPasswordWithSalt(password, salt);


            String insertUser = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            int userId = -1;

            try (PreparedStatement ps = connection.prepareStatement(insertUser)) {
                ps.setString(1, username.toLowerCase());
                ps.setString(2, passwordHash);
                ps.setString(3, salt);
                ps.executeUpdate();
            }

            // Get the new user ID
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    userId = rs.getInt(1);
                }
            }

            if (userId > 0) {
                // Create default profile
                String insertProfile = "INSERT INTO user_profiles (user_id) VALUES (?)";
                try (PreparedStatement ps = connection.prepareStatement(insertProfile)) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }

                // Return new UserProfile
                UserProfile profile = new UserProfile(userId);
                profile.setUserName(username);
                return profile;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public UserProfile authenticateAUser(String username, String password) {
        String sql = "SELECT id, password_hash, salt FROM users WHERE username = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username.toLowerCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    String storedHash = rs.getString("password_hash");
                    String salt = rs.getString("salt");

                    // Verify password
                    String inputHash = hashPasswordWithSalt(password, salt);
                    if (storedHash.equals(inputHash)) {
                        // Load and return user profile
                        return loadUserProfileFromDatabase(userId, username);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    private UserProfile loadUserProfileFromDatabase(int userId, String username) {
        String sql = "SELECT * FROM user_profiles WHERE user_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserProfile profile = new UserProfile(userId);
                    profile.setUserName(username);
                    profile.setPreferredDistanceKm(rs.getDouble("preferred_distance"));
                    profile.setPreferredRouteVariety(rs.getInt("preferred_variety"));
                    profile.setPreferHillRoutes(rs.getInt("prefer_hills") == 1);
                    profile.setMaxElevationGain(rs.getDouble("max_elevation"));
                    profile.setUseMetricUnits(rs.getInt("use_metric") == 1);
                    profile.setShowElevation(rs.getInt("show_elevation") == 1);
                    profile.setAutoFitRoute(rs.getInt("auto_fit_route") == 1);


                    profile.loadStatisticsFromDatabase(
                            rs.getDouble("total_distance"),
                            rs.getDouble("total_elevation"),
                            rs.getInt("routes_generated"),
                            rs.getInt("routes_completed")
                    );

                    return profile;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Return default profile if loading fails
        UserProfile profile = new UserProfile(userId);
        profile.setUserName(username);
        return profile;
    }


    public void saveUserToDatabase(UserProfile profile) {
        if (profile.getUserId() <= 0) {
            System.out.println("Cannot save profile: no user ID");
            return;
        }

        String sql = "UPDATE user_profiles SET " +
                "preferred_distance = ?, preferred_variety = ?, prefer_hills = ?, " +
                "max_elevation = ?, use_metric = ?, show_elevation = ?, auto_fit_route = ?, " +
                "total_distance = ?, total_elevation = ?, routes_generated = ?, routes_completed = ? " +
                "WHERE user_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, profile.getPreferredDistanceKm());
            ps.setInt(2, profile.getPreferredRouteVariety());
            ps.setInt(3, profile.isPreferHillRoutes() ? 1 : 0);
            ps.setDouble(4, profile.getMaxElevationGain());
            ps.setInt(5, profile.isUseMetricUnits() ? 1 : 0);
            ps.setInt(6, profile.isShowElevation() ? 1 : 0);
            ps.setInt(7, profile.isAutoFitRoute() ? 1 : 0);
            ps.setDouble(8, profile.getTotalDistanceRun());
            ps.setDouble(9, profile.getTotalElevationGained());
            ps.setInt(10, profile.getTotalRoutesGenerated());
            ps.setInt(11, profile.getTotalRoutesCompleted());
            ps.setInt(12, profile.getUserId());

            ps.executeUpdate();
            System.out.println("Profile saved for user ID: " + profile.getUserId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    private String generateRandomSaltForPasswordHashing() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }


    private String hashPasswordWithSalt(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public void close() {
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
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