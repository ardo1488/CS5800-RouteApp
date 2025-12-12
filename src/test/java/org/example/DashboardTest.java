package org.example;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardTest {

    // ---------- reflection helpers ----------

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name, Class<T> type) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return (T) f.get(target);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(new NoSuchFieldException(name));
    }

    private static boolean getBooleanField(Object target, String name) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f.getBoolean(target);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(new NoSuchFieldException(name));
    }

    private static void setBooleanField(Object target, String name, boolean value) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                f.setBoolean(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(new NoSuchFieldException(name));
    }

    private Dashboard createDashboard() {
        return new Dashboard();
    }

    // ---------- test listener ----------

    private static class TestDashboardListener implements Dashboard.DashboardListener {
        boolean drawToggledCalled;
        boolean drawEnabledArg;
        boolean clearCalled;
        boolean undoCalled;
        boolean redoCalled;
        boolean saveCalled;
        boolean loadCalled;
        boolean zoomInCalled;
        boolean zoomOutCalled;
        boolean generateCalled;
        boolean loginLogoutCalled;

        @Override
        public void onDrawModeToggled(boolean enabled) {
            drawToggledCalled = true;
            drawEnabledArg = enabled;
        }

        @Override
        public void onClearRoute() { clearCalled = true; }

        @Override
        public void onUndo() { undoCalled = true; }

        @Override
        public void onRedo() { redoCalled = true; }

        @Override
        public void onSaveRoute() { saveCalled = true; }

        @Override
        public void onLoadRoute() { loadCalled = true; }

        @Override
        public void onZoomIn() { zoomInCalled = true; }

        @Override
        public void onZoomOut() { zoomOutCalled = true; }

        @Override
        public void onGenerateRoute() { generateCalled = true; }

        @Override
        public void onLoginLogout() { loginLogoutCalled = true; }
    }

    // -------------------------------------------------------------
    // Constructor tests
    // -------------------------------------------------------------

    @Test
    public void constructorInitializesButtonsAndLabelsNonNullTest() {
        Dashboard d = createDashboard();

        JToggleButton drawBtn = getField(d, "drawBtn", JToggleButton.class);
        JButton clearBtn = getField(d, "clearBtn", JButton.class);
        JButton undoBtn = getField(d, "undoBtn", JButton.class);
        JButton redoBtn = getField(d, "redoBtn", JButton.class);
        JButton saveBtn = getField(d, "saveBtn", JButton.class);
        JButton loadBtn = getField(d, "loadBtn", JButton.class);
        JButton zoomInBtn = getField(d, "zoomInBtn", JButton.class);
        JButton zoomOutBtn = getField(d, "zoomOutBtn", JButton.class);
        JButton generateBtn = getField(d, "generateBtn", JButton.class);
        JButton loginBtn = getField(d, "loginBtn", JButton.class);

        JLabel distanceLabel = getField(d, "distanceLabel", JLabel.class);
        JLabel elevationLabel = getField(d, "elevationLabel", JLabel.class);
        JLabel userLabel = getField(d, "userLabel", JLabel.class);

        assertNotNull(drawBtn);
        assertNotNull(clearBtn);
        assertNotNull(undoBtn);
        assertNotNull(redoBtn);
        assertNotNull(saveBtn);
        assertNotNull(loadBtn);
        assertNotNull(zoomInBtn);
        assertNotNull(zoomOutBtn);
        assertNotNull(generateBtn);
        assertNotNull(loginBtn);
        assertNotNull(distanceLabel);
        assertNotNull(elevationLabel);
        assertNotNull(userLabel);
    }

    @Test
    public void constructorSetsInitialLabelTextsAndLoginButtonTextTest() {
        Dashboard d = createDashboard();

        JLabel distanceLabel = getField(d, "distanceLabel", JLabel.class);
        JLabel elevationLabel = getField(d, "elevationLabel", JLabel.class);
        JLabel userLabel = getField(d, "userLabel", JLabel.class);
        JButton loginBtn = getField(d, "loginBtn", JButton.class);

        assertEquals("Distance: 0.00 km", distanceLabel.getText());
        assertEquals("Elevation: 0 m", elevationLabel.getText());
        assertEquals("Guest", userLabel.getText());
        assertEquals("Login", loginBtn.getText());
    }

    // -------------------------------------------------------------
    // setDashboardListener(...) tests
    // -------------------------------------------------------------

    @Test
    public void setDashboardListenerStoresListenerReferenceTest() {
        Dashboard d = createDashboard();
        TestDashboardListener listener = new TestDashboardListener();

        d.setDashboardListener(listener);

        Dashboard.DashboardListener stored = getField(d, "listener", Dashboard.DashboardListener.class);
        assertSame(listener, stored);
    }

    @Test
    public void setDashboardListenerWiresButtonsToListenerCallbacksTest() {
        Dashboard d = createDashboard();
        TestDashboardListener listener = new TestDashboardListener();
        d.setDashboardListener(listener);

        JToggleButton drawBtn = getField(d, "drawBtn", JToggleButton.class);
        JButton clearBtn = getField(d, "clearBtn", JButton.class);
        JButton undoBtn = getField(d, "undoBtn", JButton.class);
        JButton redoBtn = getField(d, "redoBtn", JButton.class);
        JButton saveBtn = getField(d, "saveBtn", JButton.class);
        JButton loadBtn = getField(d, "loadBtn", JButton.class);
        JButton zoomInBtn = getField(d, "zoomInBtn", JButton.class);
        JButton zoomOutBtn = getField(d, "zoomOutBtn", JButton.class);
        JButton generateBtn = getField(d, "generateBtn", JButton.class);
        JButton loginBtn = getField(d, "loginBtn", JButton.class);

        drawBtn.doClick();
        clearBtn.doClick();
        undoBtn.doClick();
        redoBtn.doClick();
        saveBtn.doClick();
        loadBtn.doClick();
        zoomInBtn.doClick();
        zoomOutBtn.doClick();
        generateBtn.doClick();
        loginBtn.doClick();

        assertTrue(listener.drawToggledCalled);
        assertTrue(listener.clearCalled);
        assertTrue(listener.undoCalled);
        assertTrue(listener.redoCalled);
        assertTrue(listener.saveCalled);
        assertTrue(listener.loadCalled);
        assertTrue(listener.zoomInCalled);
        assertTrue(listener.zoomOutCalled);
        assertTrue(listener.generateCalled);
        assertTrue(listener.loginLogoutCalled);
    }

    // -------------------------------------------------------------
    // updateRouteStatsDisplay(...) tests
    // -------------------------------------------------------------

    @Test
    public void updateRouteStatsDisplayUpdatesDistanceAndElevationTextTest() {
        Dashboard d = createDashboard();

        d.updateRouteStatsDisplay(12.3456, 123.0, 50.0);

        JLabel distanceLabel = getField(d, "distanceLabel", JLabel.class);
        JLabel elevationLabel = getField(d, "elevationLabel", JLabel.class);

        assertEquals("Distance: 12.35 km", distanceLabel.getText());
        assertEquals("Elevation: 123 m", elevationLabel.getText());
    }

    @Test
    public void updateRouteStatsDisplaySetsElevationLabelColorByAscentThresholdsTest() {
        Dashboard d = createDashboard();
        JLabel elevationLabel = getField(d, "elevationLabel", JLabel.class);

        // low ascent -> green
        d.updateRouteStatsDisplay(1.0, 30.0, 0.0);
        Color lowColor = elevationLabel.getForeground();
        assertEquals(new Color(34, 139, 34), lowColor);

        // high ascent -> dark red
        d.updateRouteStatsDisplay(1.0, 150.0, 0.0);
        Color highColor = elevationLabel.getForeground();
        assertEquals(new Color(178, 34, 34), highColor);
    }

    // -------------------------------------------------------------
    // setMapReady(...) / isMapReady() tests
    // -------------------------------------------------------------

    @Test
    public void setMapReadyTrueMarksDashboardAsReadyTest() {
        Dashboard d = createDashboard();
        d.setMapReady(true);

        assertTrue(d.isMapReady());
        assertTrue(getBooleanField(d, "mapReady"));
    }

    @Test
    public void setMapReadyFalseMarksDashboardAsNotReadyTest() {
        Dashboard d = createDashboard();
        d.setMapReady(true);
        d.setMapReady(false);

        assertFalse(d.isMapReady());
        assertFalse(getBooleanField(d, "mapReady"));
    }

    // -------------------------------------------------------------
    // setDrawButtonSelected(...) tests
    // -------------------------------------------------------------

    @Test
    public void setDrawButtonSelectedTrueSelectsToggleButtonTest() {
        Dashboard d = createDashboard();
        JToggleButton drawBtn = getField(d, "drawBtn", JToggleButton.class);

        d.setDrawButtonSelected(true);

        assertTrue(drawBtn.isSelected());
    }

    @Test
    public void setDrawButtonSelectedFalseDeselectsToggleButtonTest() {
        Dashboard d = createDashboard();
        JToggleButton drawBtn = getField(d, "drawBtn", JToggleButton.class);

        drawBtn.setSelected(true);
        d.setDrawButtonSelected(false);

        assertFalse(drawBtn.isSelected());
    }

    // -------------------------------------------------------------
    // setGenerateButtonEnabled(...) tests
    // -------------------------------------------------------------

    @Test
    public void setGenerateButtonEnabledFalseDisablesGenerateButtonTest() {
        Dashboard d = createDashboard();
        JButton generateBtn = getField(d, "generateBtn", JButton.class);

        d.setGenerateButtonEnabled(false);

        assertFalse(generateBtn.isEnabled());
    }

    @Test
    public void setGenerateButtonEnabledTrueEnablesGenerateButtonTest() {
        Dashboard d = createDashboard();
        JButton generateBtn = getField(d, "generateBtn", JButton.class);

        generateBtn.setEnabled(false);
        d.setGenerateButtonEnabled(true);

        assertTrue(generateBtn.isEnabled());
    }

    // -------------------------------------------------------------
    // setMapReady() overload tests
    // -------------------------------------------------------------

    @Test
    public void setMapReadyNoArgMethodSetsReadyTrueTest() {
        Dashboard d = createDashboard();

        d.setMapReady();

        assertTrue(d.isMapReady());
    }

    @Test
    public void setMapReadyNoArgMethodOverridesPreviousFalseStateTest() {
        Dashboard d = createDashboard();
        d.setMapReady(false);

        d.setMapReady();

        assertTrue(d.isMapReady());
    }

    // -------------------------------------------------------------
    // updateUserDisplayBasedOnAuthenticationState(...) tests
    // -------------------------------------------------------------

    @Test
    public void updateUserDisplayBasedOnAuthenticationStateLoggedInUpdatesUserLabelAndLoginButtonToLogoutTest() {
        Dashboard d = createDashboard();

        d.updateUserDisplayBasedOnAuthenticationState("Alice", true);

        JLabel userLabel = getField(d, "userLabel", JLabel.class);
        JButton loginBtn = getField(d, "loginBtn", JButton.class);

        assertEquals("Alice", userLabel.getText());
        assertEquals("Logout", loginBtn.getText());

        assertEquals(new Color(0, 100, 0), userLabel.getForeground());
        assertEquals(new Color(178, 34, 34), loginBtn.getForeground());
    }

    @Test
    public void updateUserDisplayBasedOnAuthenticationStateLoggedOutShowsGuestAndLoginStylingTest() {
        Dashboard d = createDashboard();

        d.updateUserDisplayBasedOnAuthenticationState("Bob", true);
        d.updateUserDisplayBasedOnAuthenticationState(null, false);

        JLabel userLabel = getField(d, "userLabel", JLabel.class);
        JButton loginBtn = getField(d, "loginBtn", JButton.class);

        assertEquals("Guest", userLabel.getText());
        assertEquals("Login", loginBtn.getText());
        assertEquals(Color.GRAY, userLabel.getForeground());
        assertEquals(new Color(70, 130, 180), loginBtn.getForeground());
    }
}
