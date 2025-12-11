package org.example;

/**
 * Facade design pattern implementation for user profile management.
 *
 * This class provides a simplified interface to various subsystems including:
 * - Route preferences (distance, elevation preferences)
 * - Display settings (units, map preferences)
 * - Routing profile selection
 * - User statistics tracking
 *
 * The facade hides the complexity of interacting with multiple subsystems
 * and provides a unified API for the rest of the application.
 */
public class UserProfile {

    // Singleton instance for guest/default profile
    private static UserProfile guestInstance;

    // User identification
    private int userId;
    private String userName;

    // User preferences
    private RoutingAPI.RoutingProfile preferredRoutingProfile;
    private double preferredDistanceKm;
    private int preferredRouteVariety;  // 3, 5, or 10 points
    private boolean preferHillRoutes;
    private double maxElevationGain;  // Maximum acceptable elevation gain in meters

    // Display settings
    private boolean useMetricUnits;
    private boolean showElevation;
    private boolean autoFitRoute;

    // Statistics
    private double totalDistanceRun;
    private double totalElevationGained;
    private int totalRoutesGenerated;
    private int totalRoutesCompleted;

    /**
     * Constructor for authenticated users (with user ID from database)
     */
    public UserProfile(int userId) {
        this.userId = userId;
        initializeDefaults();
    }

    /**
     * Private constructor for guest/singleton instance
     */
    private UserProfile() {
        this.userId = -1;
        initializeDefaults();
    }

    /**
     * Initialize default values
     */
    private void initializeDefaults() {
        this.userName = "Guest";
        this.preferredRoutingProfile = RoutingAPI.RoutingProfile.FOOT_WALKING;
        this.preferredDistanceKm = 5.0;
        this.preferredRouteVariety = 5;
        this.preferHillRoutes = false;
        this.maxElevationGain = 200.0;

        this.useMetricUnits = true;
        this.showElevation = true;
        this.autoFitRoute = true;

        this.totalDistanceRun = 0.0;
        this.totalElevationGained = 0.0;
        this.totalRoutesGenerated = 0;
        this.totalRoutesCompleted = 0;
    }

    /**
     * Get the guest/default instance of UserProfile (for non-logged-in users)
     */
    public static UserProfile getInstance() {
        if (guestInstance == null) {
            guestInstance = new UserProfile();
        }
        return guestInstance;
    }

    /**
     * Get the user ID (for database operations)
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Check if this is an authenticated user (vs guest)
     */
    public boolean isAuthenticated() {
        return userId > 0;
    }

    /**
     * Load statistics from database
     */
    public void loadStatistics(double distance, double elevation, int generated, int completed) {
        this.totalDistanceRun = distance;
        this.totalElevationGained = elevation;
        this.totalRoutesGenerated = generated;
        this.totalRoutesCompleted = completed;
    }

    // ==================== Facade Methods ====================
    // These methods provide simplified access to complex operations

    /**
     * Configure routing preferences in one call
     * @param profile The routing profile (walking, cycling, driving)
     * @param distanceKm Preferred route distance
     * @param variety Route complexity (3=simple, 5=medium, 10=complex)
     */
    public void configureRoutingPreferences(RoutingAPI.RoutingProfile profile,
                                            double distanceKm, int variety) {
        this.preferredRoutingProfile = profile;
        this.preferredDistanceKm = distanceKm;
        this.preferredRouteVariety = normalizeVariety(variety);
    }

    /**
     * Configure elevation preferences
     * @param preferHills Whether to prefer hilly routes
     * @param maxGain Maximum elevation gain in meters (0 for no limit)
     */
    public void configureElevationPreferences(boolean preferHills, double maxGain) {
        this.preferHillRoutes = preferHills;
        this.maxElevationGain = maxGain > 0 ? maxGain : Double.MAX_VALUE;
    }

    /**
     * Configure display settings in one call
     * @param metric Use metric units (km, m) vs imperial (mi, ft)
     * @param showElev Show elevation data
     * @param autoFit Automatically fit map to route
     */
    public void configureDisplaySettings(boolean metric, boolean showElev, boolean autoFit) {
        this.useMetricUnits = metric;
        this.showElevation = showElev;
        this.autoFitRoute = autoFit;
    }

    /**
     * Record a completed route for statistics tracking
     * @param distanceKm Distance of the route in km
     * @param elevationGain Total elevation gain in meters
     */
    public void recordCompletedRoute(double distanceKm, double elevationGain) {
        this.totalDistanceRun += distanceKm;
        this.totalElevationGained += elevationGain;
        this.totalRoutesCompleted++;
    }

    /**
     * Record that a route was generated (for statistics)
     */
    public void recordRouteGenerated() {
        this.totalRoutesGenerated++;
    }

    /**
     * Check if a route meets the user's elevation preferences
     * @param elevationGain The route's total elevation gain
     * @return true if the route is acceptable
     */
    public boolean isRouteAcceptable(double elevationGain) {
        if (preferHillRoutes) {
            // If user prefers hills, accept routes with significant elevation
            return elevationGain >= 20.0;  // At least 20m of climbing
        } else {
            // Otherwise, check against max elevation preference
            return elevationGain <= maxElevationGain;
        }
    }

    /**
     * Get a formatted distance string based on user's unit preference
     * @param distanceKm Distance in kilometers
     * @return Formatted string with appropriate units
     */
    public String formatDistance(double distanceKm) {
        if (useMetricUnits) {
            if (distanceKm < 1.0) {
                return String.format("%.0f m", distanceKm * 1000);
            }
            return String.format("%.2f km", distanceKm);
        } else {
            double miles = distanceKm * 0.621371;
            return String.format("%.2f mi", miles);
        }
    }

    /**
     * Get a formatted elevation string based on user's unit preference
     * @param elevationMeters Elevation in meters
     * @return Formatted string with appropriate units
     */
    public String formatElevation(double elevationMeters) {
        if (useMetricUnits) {
            return String.format("%.0f m", elevationMeters);
        } else {
            double feet = elevationMeters * 3.28084;
            return String.format("%.0f ft", feet);
        }
    }

    /**
     * Get a summary of the user's statistics
     * @return Formatted statistics string
     */
    public String getStatisticsSummary() {
        return String.format("Total: %s run, %s climbed, %d routes completed",
                formatDistance(totalDistanceRun),
                formatElevation(totalElevationGained),
                totalRoutesCompleted);
    }

    /**
     * Reset all statistics to zero
     */
    public void resetStatistics() {
        this.totalDistanceRun = 0.0;
        this.totalElevationGained = 0.0;
        this.totalRoutesGenerated = 0;
        this.totalRoutesCompleted = 0;
    }

    /**
     * Apply this profile's settings to a RoutingAPI instance
     * @param api The RoutingAPI to configure
     */
    public void applyToRoutingAPI(RoutingAPI api) {
        api.setProfile(preferredRoutingProfile);
    }

    // ==================== Getters and Setters ====================

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public RoutingAPI.RoutingProfile getPreferredRoutingProfile() {
        return preferredRoutingProfile;
    }

    public void setPreferredRoutingProfile(RoutingAPI.RoutingProfile profile) {
        this.preferredRoutingProfile = profile;
    }

    public double getPreferredDistanceKm() {
        return preferredDistanceKm;
    }

    public void setPreferredDistanceKm(double distanceKm) {
        this.preferredDistanceKm = Math.max(0.5, Math.min(50.0, distanceKm));
    }

    public int getPreferredRouteVariety() {
        return preferredRouteVariety;
    }

    public void setPreferredRouteVariety(int variety) {
        this.preferredRouteVariety = normalizeVariety(variety);
    }

    public boolean isPreferHillRoutes() {
        return preferHillRoutes;
    }

    public void setPreferHillRoutes(boolean preferHillRoutes) {
        this.preferHillRoutes = preferHillRoutes;
    }

    public double getMaxElevationGain() {
        return maxElevationGain;
    }

    public void setMaxElevationGain(double maxElevationGain) {
        this.maxElevationGain = maxElevationGain;
    }

    public boolean isUseMetricUnits() {
        return useMetricUnits;
    }

    public void setUseMetricUnits(boolean useMetricUnits) {
        this.useMetricUnits = useMetricUnits;
    }

    public boolean isShowElevation() {
        return showElevation;
    }

    public void setShowElevation(boolean showElevation) {
        this.showElevation = showElevation;
    }

    public boolean isAutoFitRoute() {
        return autoFitRoute;
    }

    public void setAutoFitRoute(boolean autoFitRoute) {
        this.autoFitRoute = autoFitRoute;
    }

    public double getTotalDistanceRun() {
        return totalDistanceRun;
    }

    public double getTotalElevationGained() {
        return totalElevationGained;
    }

    public int getTotalRoutesGenerated() {
        return totalRoutesGenerated;
    }

    public int getTotalRoutesCompleted() {
        return totalRoutesCompleted;
    }

    // ==================== Helper Methods ====================

    /**
     * Normalize variety value to valid options (3, 5, or 10)
     */
    private int normalizeVariety(int variety) {
        if (variety <= 3) return 3;
        if (variety <= 7) return 5;
        return 10;
    }

    @Override
    public String toString() {
        return String.format("UserProfile[%s, %s, %.1fkm, hills=%b]",
                userName, preferredRoutingProfile.getValue(),
                preferredDistanceKm, preferHillRoutes);
    }
}