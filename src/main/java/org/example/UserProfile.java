package org.example;


//  Facade design pattern implementation for user profile management.

public class UserProfile {

    // Singleton
    private static UserProfile guestInstance;


    private final int userId;
    private String userName;

    // User preferences
    private RoutingAPI.RoutingProfile preferredRoutingProfile;
    private double preferredDistanceKm;
    private int preferredRouteVariety;  // 3, 5, or 10 points
    private boolean preferHillRoutes;
    private double maxElevationGain;

    private boolean useMetricUnits;
    private boolean showElevation;
    private boolean autoFitRoute;


    private double totalDistanceRun;
    private double totalElevationGained;
    private int totalRoutesGenerated;
    private int totalRoutesCompleted;


    public UserProfile(int userId) {
        this.userId = userId;
        initializeDefaults();
    }


    private UserProfile() {
        this.userId = -1;
        initializeDefaults();
    }


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


    public static UserProfile getInstanceForNonLoggedInUser() {
        if (guestInstance == null) {
            guestInstance = new UserProfile();
        }
        return guestInstance;
    }


    public int getUserId() {
        return userId;
    }


    public boolean isAuthenticatedUser() {
        return userId > 0;
    }


    public void loadStatisticsFromDatabase(double distance, double elevation, int generated, int completed) {
        this.totalDistanceRun = distance;
        this.totalElevationGained = elevation;
        this.totalRoutesGenerated = generated;
        this.totalRoutesCompleted = completed;
    }


    public void recordRouteGenerated() {
        this.totalRoutesGenerated++;
    }


    public String formatDistanceForUnitPreference(double distanceKm) {
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


    public String formatElevationForUnitPreference(double elevationMeters) {
        if (useMetricUnits) {
            return String.format("%.0f m", elevationMeters);
        } else {
            double feet = elevationMeters * 3.28084;
            return String.format("%.0f ft", feet);
        }
    }



    public void applyUserSettingsToRoutingAPI(RoutingAPI api) {
        api.setProfile(preferredRoutingProfile);
    }


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
        this.preferredRouteVariety = normalizeVarietyToValidOption(variety);
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


    private int normalizeVarietyToValidOption(int variety) {
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