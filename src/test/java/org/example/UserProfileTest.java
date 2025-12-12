package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserProfileTest {

    // Helper to create a standard authenticated user
    private UserProfile createUser(int id) {
        return new UserProfile(id);
    }

    // ---- Constructor tests ----

    @Test
    public void constructorWithUserIdInitializesDefaultsTest() {
        UserProfile profile = new UserProfile(42);

        assertEquals(42, profile.getUserId());
        assertEquals("Guest", profile.getUserName());
        assertEquals(RoutingAPI.RoutingProfile.FOOT_WALKING, profile.getPreferredRoutingProfile());
        assertEquals(5.0, profile.getPreferredDistanceKm(), 0.0001);
        assertEquals(5, profile.getPreferredRouteVariety());
    }

    @Test
    public void constructorWithUserIdSetsAuthenticatedUserTest() {
        UserProfile profile = new UserProfile(1);

        assertTrue(profile.isAuthenticatedUser());
        assertFalse(UserProfile.getInstanceForNonLoggedInUser().isAuthenticatedUser());
    }

    // ---- getInstanceForNonLoggedInUser tests ----

    @Test
    public void getInstanceForNonLoggedInUserReturnsSingletonTest() {
        UserProfile guest1 = UserProfile.getInstanceForNonLoggedInUser();
        UserProfile guest2 = UserProfile.getInstanceForNonLoggedInUser();

        assertSame(guest1, guest2);
    }

    @Test
    public void getInstanceForNonLoggedInUserHasGuestDefaultsTest() {
        UserProfile guest = UserProfile.getInstanceForNonLoggedInUser();

        assertEquals(-1, guest.getUserId());
        assertEquals("Guest", guest.getUserName());
        assertEquals(RoutingAPI.RoutingProfile.FOOT_WALKING, guest.getPreferredRoutingProfile());
    }

    // ---- getUserId tests ----

    @Test
    public void getUserIdReturnsConstructorValueTest() {
        UserProfile profile = createUser(123);

        assertEquals(123, profile.getUserId());
    }

    @Test
    public void getUserIdReturnsNegativeForGuestTest() {
        UserProfile guest = UserProfile.getInstanceForNonLoggedInUser();

        assertEquals(-1, guest.getUserId());
    }

    // ---- isAuthenticatedUser tests ----

    @Test
    public void isAuthenticatedUserTrueForPositiveIdTest() {
        UserProfile profile = createUser(10);

        assertTrue(profile.isAuthenticatedUser());
    }

    @Test
    public void isAuthenticatedUserFalseForNonPositiveIdTest() {
        UserProfile profileZero = createUser(0);
        UserProfile guest = UserProfile.getInstanceForNonLoggedInUser();

        assertFalse(profileZero.isAuthenticatedUser());
        assertFalse(guest.isAuthenticatedUser());
    }

    // ---- loadStatisticsFromDatabase tests ----

    @Test
    public void loadStatisticsFromDatabaseUpdatesTotalsTest() {
        UserProfile profile = createUser(1);
        profile.loadStatisticsFromDatabase(42.5, 300.0, 10, 7);

        assertEquals(42.5, profile.getTotalDistanceRun(), 0.0001);
        assertEquals(300.0, profile.getTotalElevationGained(), 0.0001);
        assertEquals(10, profile.getTotalRoutesGenerated());
        assertEquals(7, profile.getTotalRoutesCompleted());
    }

    @Test
    public void loadStatisticsFromDatabaseAcceptsZeroValuesTest() {
        UserProfile profile = createUser(1);
        profile.loadStatisticsFromDatabase(0.0, 0.0, 0, 0);

        assertEquals(0.0, profile.getTotalDistanceRun(), 0.0001);
        assertEquals(0.0, profile.getTotalElevationGained(), 0.0001);
        assertEquals(0, profile.getTotalRoutesGenerated());
        assertEquals(0, profile.getTotalRoutesCompleted());
    }

    // ---- recordRouteGenerated tests ----

    @Test
    public void recordRouteGeneratedIncrementsCountOnceTest() {
        UserProfile profile = createUser(1);
        int initial = profile.getTotalRoutesGenerated();

        profile.recordRouteGenerated();

        assertEquals(initial + 1, profile.getTotalRoutesGenerated());
    }

    @Test
    public void recordRouteGeneratedIncrementsCountMultipleTimesTest() {
        UserProfile profile = createUser(1);
        int initial = profile.getTotalRoutesGenerated();

        profile.recordRouteGenerated();
        profile.recordRouteGenerated();
        profile.recordRouteGenerated();

        assertEquals(initial + 3, profile.getTotalRoutesGenerated());
    }

    // ---- formatDistanceForUnitPreference tests ----

    @Test
    public void formatDistanceForUnitPreferenceMetricFormattingTest() {
        UserProfile profile = createUser(1);
        profile.setUseMetricUnits(true);

        String formattedShort = profile.formatDistanceForUnitPreference(0.5); // 500 m
        String formattedLong = profile.formatDistanceForUnitPreference(5.0);  // 5.00 km

        assertEquals("500 m", formattedShort);
        assertEquals("5.00 km", formattedLong);
    }

    @Test
    public void formatDistanceForUnitPreferenceImperialFormattingTest() {
        UserProfile profile = createUser(1);
        profile.setUseMetricUnits(false);

        String formatted = profile.formatDistanceForUnitPreference(1.0); // ~0.62 mi

        assertTrue(formatted.endsWith(" mi"));
        assertTrue(formatted.startsWith("0.62"));
    }

    // ---- formatElevationForUnitPreference tests ----

    @Test
    public void formatElevationForUnitPreferenceMetricFormattingTest() {
        UserProfile profile = createUser(1);
        profile.setUseMetricUnits(true);

        String formatted = profile.formatElevationForUnitPreference(123.4);

        assertEquals("123 m", formatted);
    }

    @Test
    public void formatElevationForUnitPreferenceImperialFormattingTest() {
        UserProfile profile = createUser(1);
        profile.setUseMetricUnits(false);

        String formatted = profile.formatElevationForUnitPreference(100.0); // ~328 ft

        assertTrue(formatted.endsWith(" ft"));
        assertTrue(formatted.startsWith("328"));
    }

    // ---- applyUserSettingsToRoutingAPI tests ----

    @Test
    public void applyUserSettingsToRoutingAPISetsProfileTest() {
        UserProfile profile = createUser(1);
        profile.setPreferredRoutingProfile(RoutingAPI.RoutingProfile.CYCLING_REGULAR);
        RoutingAPI api = new RoutingAPI("dummy-api-key");

        profile.applyUserSettingsToRoutingAPI(api);

        assertEquals(RoutingAPI.RoutingProfile.CYCLING_REGULAR, api.getProfile());
    }

    @Test
    public void applyUserSettingsToRoutingAPIPreservesSelectedProfileTest() {
        UserProfile profile = createUser(1);
        profile.setPreferredRoutingProfile(RoutingAPI.RoutingProfile.DRIVING_CAR);
        RoutingAPI api = new RoutingAPI("dummy-api-key");

        profile.applyUserSettingsToRoutingAPI(api);

        assertEquals(RoutingAPI.RoutingProfile.DRIVING_CAR, api.getProfile());
    }

    // ---- getUserName tests ----

    @Test
    public void getUserNameDefaultIsGuestTest() {
        UserProfile profile = createUser(1);

        assertEquals("Guest", profile.getUserName());
    }

    @Test
    public void getUserNameReflectsUpdatedNameTest() {
        UserProfile profile = createUser(1);
        profile.setUserName("Alice");

        assertEquals("Alice", profile.getUserName());
    }

    // ---- setUserName tests ----

    @Test
    public void setUserNameUpdatesNameFieldTest() {
        UserProfile profile = createUser(1);
        profile.setUserName("Bob");

        assertEquals("Bob", profile.getUserName());
    }

    @Test
    public void setUserNameAllowsNullNameTest() {
        UserProfile profile = createUser(1);
        profile.setUserName(null);

        assertNull(profile.getUserName());
    }

    // ---- getPreferredRoutingProfile tests ----

    @Test
    public void getPreferredRoutingProfileDefaultValueTest() {
        UserProfile profile = createUser(1);

        assertEquals(RoutingAPI.RoutingProfile.FOOT_WALKING, profile.getPreferredRoutingProfile());
    }

    @Test
    public void getPreferredRoutingProfileReflectsSetValueTest() {
        UserProfile profile = createUser(1);
        profile.setPreferredRoutingProfile(RoutingAPI.RoutingProfile.DRIVING_CAR);

        assertEquals(RoutingAPI.RoutingProfile.DRIVING_CAR, profile.getPreferredRoutingProfile());
    }

    // ---- setPreferredRoutingProfile tests ----

    @Test
    public void setPreferredRoutingProfileChangesProfileTest() {
        UserProfile profile = createUser(1);
        profile.setPreferredRoutingProfile(RoutingAPI.RoutingProfile.CYCLING_REGULAR);

        assertEquals(RoutingAPI.RoutingProfile.CYCLING_REGULAR, profile.getPreferredRoutingProfile());
    }

    @Test
    public void setPreferredRoutingProfileAcceptsNullProfileTest() {
        UserProfile profile = createUser(1);
        profile.setPreferredRoutingProfile(null);

        assertNull(profile.getPreferredRoutingProfile());
    }

    // ---- getPreferredDistanceKm tests ----

    @Test
    public void getPreferredDistanceKmDefaultValueTest() {
        UserProfile profile = createUser(1);

        assertEquals(5.0, profile.getPreferredDistanceKm(), 0.0001);
    }

    @Test
    public void getPreferredDistanceKmReflectsSetValueTest() {
        UserProfile profile = createUser(1);
        profile.setPreferredDistanceKm(10.0);

        assertEquals(10.0, profile.getPreferredDistanceKm(), 0.0001);
    }

    // ---- setPreferredDistanceKm tests ----

    @Test
    public void setPreferredDistanceKmClampsBelowMinimumTest() {
        UserProfile profile = createUser(1);
        profile.setPreferredDistanceKm(0.1);

        assertEquals(0.5, profile.getPreferredDistanceKm(), 0.0001);
    }

    @Test
    public void setPreferredDistanceKmClampsAboveMaximumTest() {
        UserProfile profile = createUser(1);
        profile.setPreferredDistanceKm(100.0);

        assertEquals(50.0, profile.getPreferredDistanceKm(), 0.0001);
    }

    // ---- getPreferredRouteVariety tests ----

    @Test
    public void getPreferredRouteVarietyDefaultValueTest() {
        UserProfile profile = createUser(1);

        assertEquals(5, profile.getPreferredRouteVariety());
    }

    @Test
    public void getPreferredRouteVarietyReflectsNormalizedValueTest() {
        UserProfile profile = createUser(1);
        profile.setPreferredRouteVariety(4); // between 3 and 7, should normalize to 5

        assertEquals(5, profile.getPreferredRouteVariety());
    }

    // ---- setPreferredRouteVariety tests ----

    @Test
    public void setPreferredRouteVarietyNormalizesLowValueTest() {
        UserProfile profile = createUser(1);
        profile.setPreferredRouteVariety(2);

        assertEquals(3, profile.getPreferredRouteVariety());
    }

    @Test
    public void setPreferredRouteVarietyNormalizesHighValueTest() {
        UserProfile profile = createUser(1);
        profile.setPreferredRouteVariety(9);

        assertEquals(10, profile.getPreferredRouteVariety());
    }

    // ---- isPreferHillRoutes tests ----

    @Test
    public void isPreferHillRoutesDefaultIsFalseTest() {
        UserProfile profile = createUser(1);

        assertFalse(profile.isPreferHillRoutes());
    }

    @Test
    public void isPreferHillRoutesReflectsSetValueTest() {
        UserProfile profile = createUser(1);
        profile.setPreferHillRoutes(true);

        assertTrue(profile.isPreferHillRoutes());
    }

    // ---- setPreferHillRoutes tests ----

    @Test
    public void setPreferHillRoutesEnablesPreferenceTest() {
        UserProfile profile = createUser(1);
        profile.setPreferHillRoutes(true);

        assertTrue(profile.isPreferHillRoutes());
    }

    @Test
    public void setPreferHillRoutesDisablesPreferenceTest() {
        UserProfile profile = createUser(1);
        profile.setPreferHillRoutes(true);
        profile.setPreferHillRoutes(false);

        assertFalse(profile.isPreferHillRoutes());
    }

    // ---- getMaxElevationGain tests ----

    @Test
    public void getMaxElevationGainDefaultValueTest() {
        UserProfile profile = createUser(1);

        assertEquals(200.0, profile.getMaxElevationGain(), 0.0001);
    }

    @Test
    public void getMaxElevationGainReflectsSetValueTest() {
        UserProfile profile = createUser(1);
        profile.setMaxElevationGain(500.0);

        assertEquals(500.0, profile.getMaxElevationGain(), 0.0001);
    }

    // ---- setMaxElevationGain tests ----

    @Test
    public void setMaxElevationGainUpdatesValueTest() {
        UserProfile profile = createUser(1);
        profile.setMaxElevationGain(1000.0);

        assertEquals(1000.0, profile.getMaxElevationGain(), 0.0001);
    }

    @Test
    public void setMaxElevationGainAcceptsZeroTest() {
        UserProfile profile = createUser(1);
        profile.setMaxElevationGain(0.0);

        assertEquals(0.0, profile.getMaxElevationGain(), 0.0001);
    }

    // ---- isUseMetricUnits tests ----

    @Test
    public void isUseMetricUnitsDefaultIsTrueTest() {
        UserProfile profile = createUser(1);

        assertTrue(profile.isUseMetricUnits());
    }

    @Test
    public void isUseMetricUnitsReflectsSetValueTest() {
        UserProfile profile = createUser(1);
        profile.setUseMetricUnits(false);

        assertFalse(profile.isUseMetricUnits());
    }

    // ---- setUseMetricUnits tests ----

    @Test
    public void setUseMetricUnitsSwitchesToImperialTest() {
        UserProfile profile = createUser(1);
        profile.setUseMetricUnits(false);

        assertFalse(profile.isUseMetricUnits());
    }

    @Test
    public void setUseMetricUnitsSwitchesBackToMetricTest() {
        UserProfile profile = createUser(1);
        profile.setUseMetricUnits(false);
        profile.setUseMetricUnits(true);

        assertTrue(profile.isUseMetricUnits());
    }

    // ---- isShowElevation tests ----

    @Test
    public void isShowElevationDefaultIsTrueTest() {
        UserProfile profile = createUser(1);

        assertTrue(profile.isShowElevation());
    }

    @Test
    public void isShowElevationReflectsSetValueTest() {
        UserProfile profile = createUser(1);
        profile.setShowElevation(false);

        assertFalse(profile.isShowElevation());
    }

    // ---- setShowElevation tests ----

    @Test
    public void setShowElevationDisablesElevationTest() {
        UserProfile profile = createUser(1);
        profile.setShowElevation(false);

        assertFalse(profile.isShowElevation());
    }

    @Test
    public void setShowElevationEnablesElevationTest() {
        UserProfile profile = createUser(1);
        profile.setShowElevation(false);
        profile.setShowElevation(true);

        assertTrue(profile.isShowElevation());
    }

    // ---- isAutoFitRoute tests ----

    @Test
    public void isAutoFitRouteDefaultIsTrueTest() {
        UserProfile profile = createUser(1);

        assertTrue(profile.isAutoFitRoute());
    }

    @Test
    public void isAutoFitRouteReflectsSetValueTest() {
        UserProfile profile = createUser(1);
        profile.setAutoFitRoute(false);

        assertFalse(profile.isAutoFitRoute());
    }

    // ---- setAutoFitRoute tests ----

    @Test
    public void setAutoFitRouteDisablesAutoFitTest() {
        UserProfile profile = createUser(1);
        profile.setAutoFitRoute(false);

        assertFalse(profile.isAutoFitRoute());
    }

    @Test
    public void setAutoFitRouteEnablesAutoFitTest() {
        UserProfile profile = createUser(1);
        profile.setAutoFitRoute(false);
        profile.setAutoFitRoute(true);

        assertTrue(profile.isAutoFitRoute());
    }

    // ---- getTotalDistanceRun tests ----

    @Test
    public void getTotalDistanceRunDefaultIsZeroTest() {
        UserProfile profile = createUser(1);

        assertEquals(0.0, profile.getTotalDistanceRun(), 0.0001);
    }

    @Test
    public void getTotalDistanceRunReflectsLoadedStatsTest() {
        UserProfile profile = createUser(1);
        profile.loadStatisticsFromDatabase(12.34, 50.0, 3, 2);

        assertEquals(12.34, profile.getTotalDistanceRun(), 0.0001);
    }

    // ---- getTotalElevationGained tests ----

    @Test
    public void getTotalElevationGainedDefaultIsZeroTest() {
        UserProfile profile = createUser(1);

        assertEquals(0.0, profile.getTotalElevationGained(), 0.0001);
    }

    @Test
    public void getTotalElevationGainedReflectsLoadedStatsTest() {
        UserProfile profile = createUser(1);
        profile.loadStatisticsFromDatabase(10.0, 250.0, 3, 2);

        assertEquals(250.0, profile.getTotalElevationGained(), 0.0001);
    }

    // ---- getTotalRoutesGenerated tests ----

    @Test
    public void getTotalRoutesGeneratedDefaultIsZeroTest() {
        UserProfile profile = createUser(1);

        assertEquals(0, profile.getTotalRoutesGenerated());
    }

    @Test
    public void getTotalRoutesGeneratedReflectsGeneratedRoutesTest() {
        UserProfile profile = createUser(1);
        profile.recordRouteGenerated();
        profile.recordRouteGenerated();

        assertEquals(2, profile.getTotalRoutesGenerated());
    }

    // ---- getTotalRoutesCompleted tests ----

    @Test
    public void getTotalRoutesCompletedDefaultIsZeroTest() {
        UserProfile profile = createUser(1);

        assertEquals(0, profile.getTotalRoutesCompleted());
    }

    @Test
    public void getTotalRoutesCompletedReflectsLoadedStatsTest() {
        UserProfile profile = createUser(1);
        profile.loadStatisticsFromDatabase(10.0, 100.0, 5, 4);

        assertEquals(4, profile.getTotalRoutesCompleted());
    }

    // ---- toString tests ----

    @Test
    public void toStringIncludesUserNameAndProfileTest() {
        UserProfile profile = createUser(1);
        profile.setUserName("TestUser");
        profile.setPreferredRoutingProfile(RoutingAPI.RoutingProfile.FOOT_WALKING);

        String s = profile.toString();

        assertTrue(s.contains("TestUser"));
        assertTrue(s.contains(RoutingAPI.RoutingProfile.FOOT_WALKING.getValue()));
    }

    @Test
    public void toStringReflectsHillPreferenceFlagTest() {
        UserProfile profile = createUser(1);
        profile.setPreferHillRoutes(true);

        String s = profile.toString();
        assertTrue(s.contains("hills=true"));
    }
}
