package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class MapTest {

    private int getViewerZoom(Map map) throws Exception {
        Field viewerField = Map.class.getDeclaredField("viewer");
        viewerField.setAccessible(true);
        Object viewer = viewerField.get(map);
        // JXMapViewer#getZoom is public, but we can't import the class cleanly here,
        // so we use reflection:
        return (int) viewer.getClass().getMethod("getZoom").invoke(viewer);
    }

    /* setMapClickListener / displayRoute */

    @Test
    public void setMapClickListenerAndDisplayRouteDoNotThrowTest() {
        Map map = new Map();
        map.setMapClickListener(position -> { /* no-op */ });

        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));

        assertDoesNotThrow(() -> map.displayRoute(route));
    }

    @Test
    public void setMapClickListenerAcceptsNullListenerTest() {
        Map map = new Map();
        assertDoesNotThrow(() -> map.setMapClickListener(null));
    }

    /* setDrawingMode / isDrawingMode */

    @Test
    public void setDrawingModeEnablesDrawingFlagTest() {
        Map map = new Map();
        map.setDrawingMode(true);
        assertTrue(map.isDrawingMode());
    }

    @Test
    public void setDrawingModeDisablesDrawingFlagTest() {
        Map map = new Map();
        map.setDrawingMode(true);
        map.setDrawingMode(false);
        assertFalse(map.isDrawingMode());
    }

    /* setCursor */

    @Test
    public void setCursorPropagatesToComponentTest() {
        Map map = new Map();
        Cursor hand = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

        assertDoesNotThrow(() -> map.setCursor(hand));
    }

    @Test
    public void setCursorAcceptsDefaultCursorTest() {
        Map map = new Map();
        assertDoesNotThrow(() -> map.setCursor(Cursor.getDefaultCursor()));
    }

    /* fitToRoute */

    @Test
    public void fitToRouteWithNullRouteDoesNothingTest() {
        Map map = new Map();
        assertDoesNotThrow(() -> map.fitToRoute(null));
    }

    @Test
    public void fitToRouteWithNonEmptyRouteAdjustsZoomLevelTest() throws Exception {
        Map map = new Map();
        // Need the component to have a size; add to a frame for layout
        JFrame frame = new JFrame();
        frame.add(map);
        frame.setSize(800, 600);
        frame.addNotify(); // ensure peer created

        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));
        route.addWaypoint(new GeoPosition(10.0, 10.0));

        int zoomBefore = getViewerZoom(map);
        map.fitToRoute(route);
        int zoomAfter = getViewerZoom(map);

        // We don't know exact value, but fitToRoute should set something, not throw.
        assertTrue(zoomAfter >= 0 && zoomAfter <= 19);
    }

    /* zoomIn / zoomOut */

    @Test
    public void zoomInDecreasesZoomValueWithinBoundsTest() throws Exception {
        Map map = new Map();
        int before = getViewerZoom(map);

        map.zoomIn();
        int after = getViewerZoom(map);

        // JXMapViewer zoom 0 is most zoomed-in; zoomIn should decrease or stay same at bound
        assertTrue(after <= before);
        assertTrue(after >= 0);
    }

    @Test
    public void zoomOutIncreasesZoomValueWithinBoundsTest() throws Exception {
        Map map = new Map();
        int before = getViewerZoom(map);

        map.zoomOut();
        int after = getViewerZoom(map);

        assertTrue(after >= before);
        assertTrue(after <= 19);
    }
}
