package org.example;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RouteServiceTest {

    // ---------- reflection helpers ----------

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name, Class<T> type) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean getBooleanField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.getBoolean(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RouteService createService() {
        return new RouteService();
    }

    // -------------------------------------------------------------
    // Constructor tests
    // -------------------------------------------------------------

    @Test
    public void constructorInitializesCoreComponentsNonNullTest() {
        RouteService service = createService();
        try {
            assertNotNull(getField(service, "map", Map.class));
            assertNotNull(getField(service, "dashboard", Dashboard.class));
            assertNotNull(getField(service, "undoManager", UndoManager.class));
            assertNotNull(getField(service, "database", Database.class));
            assertNotNull(getField(service, "routingAPI", RoutingAPI.class));
            assertNotNull(getField(service, "authContext", AuthContext.class));
            assertNotNull(getField(service, "userProfile", UserProfile.class));
            assertNotNull(getField(service, "currentRoute", Route.class));
            assertNotNull(getField(service, "statusLabel", JLabel.class));
        } finally {
            service.dispose();
        }
    }

    @Test
    public void constructorAppliesUserProfileSettingsToRoutingApiTest() {
        RouteService service = createService();
        try {
            RoutingAPI api = getField(service, "routingAPI", RoutingAPI.class);
            UserProfile profile = getField(service, "userProfile", UserProfile.class);

            assertEquals(profile.getPreferredRoutingProfile(), api.getProfile());
        } finally {
            service.dispose();
        }
    }

    // -------------------------------------------------------------
    // onDrawModeToggled(...) tests
    // -------------------------------------------------------------

    @Test
    public void onDrawModeToggledEnablingSetsDrawingModeAndExitsGenerateModeTest() {
        RouteService service = createService();
        try {
            Map map = getField(service, "map", Map.class);

            // simulate generate mode being active
            setField(service, "isGenerateMode", true);
            setField(service, "generateStartPoint", new GeoPosition(1.0, 2.0));

            service.onDrawModeToggled(true);

            assertTrue(map.isDrawingMode());
            assertFalse(getBooleanField(service, "isGenerateMode"));
            assertNull(getField(service, "generateStartPoint", GeoPosition.class));
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onDrawModeToggledDisablingTurnsOffDrawingModeTest() {
        RouteService service = createService();
        try {
            Map map = getField(service, "map", Map.class);
            map.setDrawingMode(true);

            service.onDrawModeToggled(false);

            assertFalse(map.isDrawingMode());
        } finally {
            service.dispose();
        }
    }

    // -------------------------------------------------------------
    // onClearRoute() tests
    // -------------------------------------------------------------

    @Test
    public void onClearRouteClearsRouteAndResetsFlagsTest() {
        RouteService service = createService();
        try {
            Route route = getField(service, "currentRoute", Route.class);

            route.addWaypoint(new GeoPosition(0.0, 0.0));
            assertFalse(route.isEmpty());

            setField(service, "pendingPoint", new GeoPosition(1.0, 1.0));
            setField(service, "isRouting", true);
            setField(service, "isGenerateMode", true);
            setField(service, "generateStartPoint", new GeoPosition(2.0, 2.0));

            service.onClearRoute();

            assertTrue(route.isEmpty());
            assertNull(getField(service, "pendingPoint", GeoPosition.class));
            assertFalse(getBooleanField(service, "isRouting"));
            assertFalse(getBooleanField(service, "isGenerateMode"));
            assertNull(getField(service, "generateStartPoint", GeoPosition.class));
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onClearRouteRecordsMementoAllowingUndoToRestoreTest() {
        RouteService service = createService();
        try {
            Route route = getField(service, "currentRoute", Route.class);

            route.addWaypoint(new GeoPosition(0.0, 0.0));
            assertFalse(route.isEmpty());

            service.onClearRoute();
            assertTrue(route.isEmpty());

            service.onUndo();
            assertFalse(route.isEmpty());
        } finally {
            service.dispose();
        }
    }

    // -------------------------------------------------------------
    // onUndo() tests
    // -------------------------------------------------------------

    @Test
    public void onUndoRestoresPreviousRouteStateWhenHistoryExistsTest() {
        RouteService service = createService();
        try {
            Route route = getField(service, "currentRoute", Route.class);
            UndoManager undoManager = getField(service, "undoManager", UndoManager.class);

            route.clear();
            route.addWaypoint(new GeoPosition(0.0, 0.0));
            undoManager.recordMemento(route.createMemento());  // snapshot with 1 point

            route.addWaypoint(new GeoPosition(1.0, 1.0)); // now 2 points
            assertEquals(2, route.getAllPointsAsGeoPositions().size());

            service.onUndo();

            assertEquals(1, route.getAllPointsAsGeoPositions().size());
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onUndoWithNoHistoryLeavesRouteUnchangedTest() {
        RouteService service = createService();
        try {
            Route route = getField(service, "currentRoute", Route.class);
            route.clear();
            route.addWaypoint(new GeoPosition(0.0, 0.0));

            int before = route.getAllPointsAsGeoPositions().size();
            service.onUndo();
            int after = route.getAllPointsAsGeoPositions().size();

            assertEquals(before, after);
        } finally {
            service.dispose();
        }
    }

    // -------------------------------------------------------------
    // onRedo() tests
    // -------------------------------------------------------------

    @Test
    public void onRedoAfterUndoRestoresLaterStateTest() {
        RouteService service = createService();
        try {
            Route route = getField(service, "currentRoute", Route.class);
            UndoManager undoManager = getField(service, "undoManager", UndoManager.class);

            route.clear();
            route.addWaypoint(new GeoPosition(0.0, 0.0));
            undoManager.recordMemento(route.createMemento()); // state with 1 point

            route.addWaypoint(new GeoPosition(1.0, 1.0)); // state with 2 points

            service.onUndo(); // back to 1 point, redo has 2-point state
            assertEquals(1, route.getAllPointsAsGeoPositions().size());

            service.onRedo(); // should go back to 2 points
            assertEquals(2, route.getAllPointsAsGeoPositions().size());
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onRedoWithNoRedoHistoryLeavesRouteUnchangedTest() {
        RouteService service = createService();
        try {
            Route route = getField(service, "currentRoute", Route.class);
            route.clear();
            route.addWaypoint(new GeoPosition(0.0, 0.0));

            int before = route.getAllPointsAsGeoPositions().size();
            service.onRedo();
            int after = route.getAllPointsAsGeoPositions().size();

            assertEquals(before, after);
        } finally {
            service.dispose();
        }
    }

    // -------------------------------------------------------------
    // onSaveRoute() tests  (only empty-route branch – no dialogs)
    // -------------------------------------------------------------

    @Test
    public void onSaveRouteWithEmptyRouteDoesNotChangeRouteIdTest() {
        RouteService service = createService();
        try {
            Route route = getField(service, "currentRoute", Route.class);
            route.clear();
            int beforeId = route.getId();

            service.onSaveRoute();

            assertEquals(beforeId, route.getId());
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onSaveRouteWithEmptyRouteDoesNotPersistNewRouteTest() {
        RouteService service = createService();
        try {
            Database db = getField(service, "database", Database.class);
            Route route = getField(service, "currentRoute", Route.class);
            route.clear();

            List<Database.RouteSummary> before = db.getAllRoutes();
            service.onSaveRoute();
            List<Database.RouteSummary> after = db.getAllRoutes();

            assertEquals(before.size(), after.size());
        } finally {
            service.dispose();
        }
    }

    // -------------------------------------------------------------
    // onLoadRoute() tests  (just ensure no exception)
    // -------------------------------------------------------------

    @Test
    public void onLoadRouteDoesNotThrowWhenInvokedTest() {
        RouteService service = createService();
        try {
            service.onLoadRoute();
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onLoadRouteKeepsRouteObjectNonNullTest() {
        RouteService service = createService();
        try {
            Route beforeRoute = getField(service, "currentRoute", Route.class);

            service.onLoadRoute();

            Route afterRoute = getField(service, "currentRoute", Route.class);
            assertNotNull(afterRoute);
            assertSame(beforeRoute.getClass(), afterRoute.getClass());
        } finally {
            service.dispose();
        }
    }

    // -------------------------------------------------------------
    // onGenerateRoute() tests
    // -------------------------------------------------------------

    @Test
    public void onGenerateRouteEntersGenerateModeWhenIdleTest() {
        RouteService service = createService();
        try {
            setField(service, "isRouting", false);
            setField(service, "isGenerateMode", false);

            service.onGenerateRoute();

            assertTrue(getBooleanField(service, "isGenerateMode"));
            assertNull(getField(service, "generateStartPoint", GeoPosition.class));
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onGenerateRouteCalledAgainExitsGenerateModeTest() {
        RouteService service = createService();
        try {
            setField(service, "isRouting", false);
            setField(service, "isGenerateMode", false);

            service.onGenerateRoute(); // enter generate mode
            assertTrue(getBooleanField(service, "isGenerateMode"));

            service.onGenerateRoute(); // exit generate mode
            assertFalse(getBooleanField(service, "isGenerateMode"));
        } finally {
            service.dispose();
        }
    }

    // -------------------------------------------------------------
    // onZoomIn() / onZoomOut() tests
    // -------------------------------------------------------------

    @Test
    public void onZoomInAdjustsViewerZoomTowardsMinTest() {
        RouteService service = createService();
        try {
            Map map = getField(service, "map", Map.class);
            JXMapViewer viewer = getField(map, "viewer", JXMapViewer.class);

            int before = viewer.getZoom();
            service.onZoomIn();
            int after = viewer.getZoom();

            assertTrue(after <= before);
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onZoomOutAdjustsViewerZoomTowardsMaxTest() {
        RouteService service = createService();
        try {
            Map map = getField(service, "map", Map.class);
            JXMapViewer viewer = getField(map, "viewer", JXMapViewer.class);

            int before = viewer.getZoom();
            service.onZoomOut();
            int after = viewer.getZoom();

            assertTrue(after >= before);
        } finally {
            service.dispose();
        }
    }

    // -------------------------------------------------------------
    // onLoginLogout() tests (no custom AuthState impl)
    // -------------------------------------------------------------

    @Test
    public void onLoginLogoutDoesNotThrowWhenInvokedTest() {
        RouteService service = createService();
        try {
            service.onLoginLogout();
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onLoginLogoutLeavesAuthContextNonNullTest() {
        RouteService service = createService();
        try {
            AuthContext ctxBefore = getField(service, "authContext", AuthContext.class);

            service.onLoginLogout();

            AuthContext ctxAfter = getField(service, "authContext", AuthContext.class);
            assertNotNull(ctxAfter);
            assertSame(ctxBefore, ctxAfter);
        } finally {
            service.dispose();
        }
    }

    // -------------------------------------------------------------
    // Auth callbacks: onStateChanged / onLoginSuccess / onLoginFailure /
    //                 onLogout / onRegistrationSuccess / onRegistrationFailure
    // -------------------------------------------------------------

    @Test
    public void onStateChangedAcceptsNullStatesWithoutThrowingTest() {
        RouteService service = createService();
        try {
            service.onStateChanged(null, null);
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onStateChangedDoesNotModifyUserProfileReferenceTest() {
        RouteService service = createService();
        try {
            UserProfile before = getField(service, "userProfile", UserProfile.class);
            service.onStateChanged(null, null);
            UserProfile after = getField(service, "userProfile", UserProfile.class);

            assertSame(before, after);
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onLoginSuccessSwitchesToUserProfileAndUpdatesRoutingApiTest() {
        RouteService service = createService();
        try {
            UserProfile user = new UserProfile(10);
            user.setPreferredRoutingProfile(RoutingAPI.RoutingProfile.CYCLING_REGULAR);

            service.onLoginSuccess(user);

            UserProfile currentProfile = getField(service, "userProfile", UserProfile.class);
            RoutingAPI api = getField(service, "routingAPI", RoutingAPI.class);

            assertSame(user, currentProfile);
            assertEquals(user.getPreferredRoutingProfile(), api.getProfile());
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onLoginSuccessSetsPositiveStatusMessageTest() {
        RouteService service = createService();
        try {
            UserProfile user = new UserProfile(10);
            user.setUserName("Tester");

            service.onLoginSuccess(user);

            JLabel label = getField(service, "statusLabel", JLabel.class);
            assertTrue(label.getText().contains("Welcome, Tester"));
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onLoginFailureSetsErrorStatusWithReasonTest() {
        RouteService service = createService();
        try {
            service.onLoginFailure("Bad password");

            JLabel label = getField(service, "statusLabel", JLabel.class);
            assertTrue(label.getText().contains("Login failed"));
            assertTrue(label.getText().contains("Bad password"));
            assertEquals(Color.RED, label.getForeground());
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onLogoutSwitchesBackToGuestProfileTest() {
        RouteService service = createService();
        try {
            UserProfile user = new UserProfile(10);
            service.onLoginSuccess(user); // switch to user

            service.onLogout();

            UserProfile profile = getField(service, "userProfile", UserProfile.class);
            assertFalse(profile.isAuthenticatedUser());
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onLogoutSetsNeutralStatusMessageTest() {
        RouteService service = createService();
        try {
            service.onLogout();

            JLabel label = getField(service, "statusLabel", JLabel.class);
            assertTrue(label.getText().contains("Logged out"));
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onRegistrationSuccessSwitchesToUserProfileAndGreetsTest() {
        RouteService service = createService();
        try {
            UserProfile user = new UserProfile(20);
            user.setUserName("NewUser");

            service.onRegistrationSuccess(user);

            UserProfile profile = getField(service, "userProfile", UserProfile.class);
            JLabel label = getField(service, "statusLabel", JLabel.class);

            assertSame(user, profile);
            assertTrue(label.getText().contains("Account created"));
            assertTrue(label.getText().contains("NewUser"));
        } finally {
            service.dispose();
        }
    }

    @Test
    public void onRegistrationFailureSetsErrorStatusWithReasonTest() {
        RouteService service = createService();
        try {
            service.onRegistrationFailure("Username taken");

            JLabel label = getField(service, "statusLabel", JLabel.class);
            assertTrue(label.getText().contains("Registration failed"));
            assertTrue(label.getText().contains("Username taken"));
            assertEquals(Color.RED, label.getForeground());
        } finally {
            service.dispose();
        }
    }
}
