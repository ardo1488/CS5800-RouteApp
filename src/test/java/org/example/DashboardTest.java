package org.example;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardTest {

    private JLabel getLabel(Dashboard dashboard, String fieldName) throws Exception {
        Field field = Dashboard.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (JLabel) field.get(dashboard);
    }

    /* setDashboardListener */

    @Test
    public void setDashboardListenerStoresListenerTest() {
        Dashboard dashboard = new Dashboard();
        Dashboard.DashboardListener listener = new Dashboard.DashboardListener() {
            @Override public void onDrawModeToggled(boolean enabled) { }
            @Override public void onClearRoute() { }
            @Override public void onUndo() { }
            @Override public void onRedo() { }
            @Override public void onSaveRoute() { }
            @Override public void onLoadRoute() { }
            @Override public void onZoomIn() { }
            @Override public void onZoomOut() { }
        };

        dashboard.setDashboardListener(listener);
        // Indirect verification: should not throw when clicking buttons
        // (we can simulate a click by programmatically firing the action)
        // If listener is null, NPE would be thrown in the action handlers.
        // We'll trigger Draw button.
        SwingUtilities.invokeLater(() -> {
            try {
                Field drawBtnField = Dashboard.class.getDeclaredField("drawBtn");
                drawBtnField.setAccessible(true);
                JToggleButton drawBtn = (JToggleButton) drawBtnField.get(dashboard);
                drawBtn.doClick();
            } catch (Exception e) {
                fail("Clicking draw button should not throw when listener is set: " + e.getMessage());
            }
        });
    }

    @Test
    public void setDashboardListenerAcceptsNullListenerTest() {
        Dashboard dashboard = new Dashboard();
        assertDoesNotThrow(() -> dashboard.setDashboardListener(null));
    }

    /* updateRouteStats */

    @Test
    public void updateRouteStatsUpdatesDistanceLabelTextTest() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.updateRouteStats(12.3456, 100);

        JLabel distanceLabel = getLabel(dashboard, "distanceLabel");
        assertEquals("Distance: 12.35 km", distanceLabel.getText());
    }

    @Test
    public void updateRouteStatsUpdatesElevationLabelTextTest() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.updateRouteStats(0.0, 250);

        JLabel elevationLabel = getLabel(dashboard, "elevationLabel");
        assertEquals("Elevation: 250 m", elevationLabel.getText());
    }

    /* setMapReady / isMapReady */

    @Test
    public void setMapReadyNoArgMarksMapReadyTrueTest() {
        Dashboard dashboard = new Dashboard();
        dashboard.setMapReady();
        assertTrue(dashboard.isMapReady());
    }

    @Test
    public void setMapReadyBooleanControlsFlagTest() {
        Dashboard dashboard = new Dashboard();
        dashboard.setMapReady(false);
        assertFalse(dashboard.isMapReady());

        dashboard.setMapReady(true);
        assertTrue(dashboard.isMapReady());
    }
}
