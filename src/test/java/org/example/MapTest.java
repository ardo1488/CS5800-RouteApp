package org.example;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MapTest {

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

    private Map createMap() {
        return new Map();
    }

    // -------------------------------------------------------------
    // Constructor tests
    // -------------------------------------------------------------

    @Test
    public void constructorInitializesViewerAndDefaultsTest() {
        Map map = createMap();

        JXMapViewer viewer = getField(map, "viewer", JXMapViewer.class);
        assertNotNull(viewer);

        Route currentRoute = getField(map, "currentRoute", Route.class);
        assertNull(currentRoute);

        assertFalse(getBooleanField(map, "drawingMode"));
        assertNull(getField(map, "clickListener", Map.MapClickListener.class));
    }

    @Test
    public void constructorSetsInitialCenterAndZoomOnViewerTest() {
        Map map = createMap();

        JXMapViewer viewer = getField(map, "viewer", JXMapViewer.class);
        GeoPosition center = viewer.getAddressLocation();

        assertEquals(37.7749, center.getLatitude(), 0.001);
        assertEquals(-122.4194, center.getLongitude(), 0.001);
        assertEquals(4, viewer.getZoom());
    }

    // -------------------------------------------------------------
    // setMapClickListener(...) tests
    // -------------------------------------------------------------

    @Test
    public void setMapClickListenerStoresListenerReferenceTest() {
        Map map = createMap();
        Map.MapClickListener listener = pos -> {};

        map.setMapClickListener(listener);

        Map.MapClickListener stored = getField(map, "clickListener", Map.MapClickListener.class);
        assertSame(listener, stored);
    }

    @Test
    public void setMapClickListenerCanBeSetToNullTest() {
        Map map = createMap();
        Map.MapClickListener listener = pos -> {};
        map.setMapClickListener(listener);

        map.setMapClickListener(null);

        Map.MapClickListener stored = getField(map, "clickListener", Map.MapClickListener.class);
        assertNull(stored);
    }

    // -------------------------------------------------------------
    // setDrawingMode(...) / isDrawingMode() tests
    // -------------------------------------------------------------

    @Test
    public void setDrawingModeTrueSetsFlagAndIsDrawingModeReturnsTrueTest() {
        Map map = createMap();

        map.setDrawingMode(true);

        assertTrue(map.isDrawingMode());
        assertTrue(getBooleanField(map, "drawingMode"));
    }

    @Test
    public void setDrawingModeFalseSetsFlagAndIsDrawingModeReturnsFalseTest() {
        Map map = createMap();
        map.setDrawingMode(true);

        map.setDrawingMode(false);

        assertFalse(map.isDrawingMode());
        assertFalse(getBooleanField(map, "drawingMode"));
    }

    // -------------------------------------------------------------
    // displayRoute(...) tests
    // -------------------------------------------------------------

    @Test
    public void displayRouteStoresRouteReferenceAndRepaintsTest() {
        Map map = createMap();
        Route route = new Route();
        route.addWaypoint(new GeoPosition(10.0, 20.0));

        map.displayRoute(route);

        Route stored = getField(map, "currentRoute", Route.class);
        assertSame(route, stored);
    }

    @Test
    public void displayRouteAllowsNullAndClearsCurrentRouteReferenceTest() {
        Map map = createMap();
        Route route = new Route();
        route.addWaypoint(new GeoPosition(10.0, 20.0));
        map.displayRoute(route);

        map.displayRoute(null);

        Route stored = getField(map, "currentRoute", Route.class);
        assertNull(stored);
    }

    // -------------------------------------------------------------
    // setCursor(...) override tests
    // -------------------------------------------------------------

    @Test
    public void setCursorUpdatesBothPanelAndViewerCursorTest() {
        Map map = createMap();
        JXMapViewer viewer = getField(map, "viewer", JXMapViewer.class);

        Cursor hand = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        map.setCursor(hand);

        assertEquals(hand, map.getCursor());
        assertEquals(hand, viewer.getCursor());
    }

    @Test
    public void setCursorAcceptsDefaultCursorWithoutExceptionTest() {
        Map map = createMap();
        JXMapViewer viewer = getField(map, "viewer", JXMapViewer.class);

        Cursor defaultCursor = Cursor.getDefaultCursor();
        map.setCursor(defaultCursor);

        assertEquals(defaultCursor, map.getCursor());
        assertEquals(defaultCursor, viewer.getCursor());
    }

    // -------------------------------------------------------------
    // fitToRoute(...) tests
    // -------------------------------------------------------------

    @Test
    public void fitToRouteWithNullOrEmptyRouteLeavesViewerCenterUnchangedTest() {
        Map map = createMap();
        JXMapViewer viewer = getField(map, "viewer", JXMapViewer.class);

        GeoPosition beforeCenter = viewer.getAddressLocation();
        int beforeZoom = viewer.getZoom();

        // null route
        map.fitToRoute(null);
        GeoPosition centerAfterNull = viewer.getAddressLocation();
        int zoomAfterNull = viewer.getZoom();

        assertEquals(beforeCenter.getLatitude(), centerAfterNull.getLatitude(), 0.0001);
        assertEquals(beforeCenter.getLongitude(), centerAfterNull.getLongitude(), 0.0001);
        assertEquals(beforeZoom, zoomAfterNull);

        // empty route
        Route empty = new Route();
        map.fitToRoute(empty);
        GeoPosition centerAfterEmpty = viewer.getAddressLocation();
        int zoomAfterEmpty = viewer.getZoom();

        assertEquals(beforeCenter.getLatitude(), centerAfterEmpty.getLatitude(), 0.0001);
        assertEquals(beforeCenter.getLongitude(), centerAfterEmpty.getLongitude(), 0.0001);
        assertEquals(beforeZoom, zoomAfterEmpty);
    }

    @Test
    public void fitToRouteWithValidRouteRecentersMapWithinBoundsTest() {
        Map map = createMap();
        JXMapViewer viewer = getField(map, "viewer", JXMapViewer.class);

        // give the viewer a non-zero size so zoom calculation can run
        viewer.setSize(800, 600);

        Route route = new Route();
        route.addWaypoint(new GeoPosition(10.0, 20.0));
        route.addWaypoint(new GeoPosition(20.0, 30.0));

        map.fitToRoute(route);

        GeoPosition center = viewer.getAddressLocation();
        List<GeoPosition> pts = route.getAllPointsAsGeoPositions();

        double minLat = Math.min(pts.get(0).getLatitude(), pts.get(1).getLatitude());
        double maxLat = Math.max(pts.get(0).getLatitude(), pts.get(1).getLatitude());
        double minLon = Math.min(pts.get(0).getLongitude(), pts.get(1).getLongitude());
        double maxLon = Math.max(pts.get(0).getLongitude(), pts.get(1).getLongitude());

        assertTrue(center.getLatitude() >= minLat && center.getLatitude() <= maxLat);
        assertTrue(center.getLongitude() >= minLon && center.getLongitude() <= maxLon);

        int zoom = viewer.getZoom();
        int minZoom = viewer.getTileFactory().getInfo().getMinimumZoomLevel();
        int maxZoom = viewer.getTileFactory().getInfo().getMaximumZoomLevel();
        assertTrue(zoom >= minZoom && zoom <= maxZoom);
    }

    // -------------------------------------------------------------
    // zoomIn() tests
    // -------------------------------------------------------------

    @Test
    public void zoomInDoesNotGoBelowMinimumZoomLevelTest() {
        Map map = createMap();
        JXMapViewer viewer = getField(map, "viewer", JXMapViewer.class);

        int minZoom = viewer.getTileFactory().getInfo().getMinimumZoomLevel();
        viewer.setZoom(minZoom);

        map.zoomIn();

        assertEquals(minZoom, viewer.getZoom());
    }

    @Test
    public void zoomInReducesZoomWhenAboveMinimumTest() {
        Map map = createMap();
        JXMapViewer viewer = getField(map, "viewer", JXMapViewer.class);

        int minZoom = viewer.getTileFactory().getInfo().getMinimumZoomLevel();
        viewer.setZoom(minZoom + 2);
        int before = viewer.getZoom();

        map.zoomIn();

        assertEquals(before - 1, viewer.getZoom());
    }

    // -------------------------------------------------------------
    // zoomOut() tests
    // -------------------------------------------------------------

    @Test
    public void zoomOutDoesNotExceedMaximumZoomLevelTest() {
        Map map = createMap();
        JXMapViewer viewer = getField(map, "viewer", JXMapViewer.class);

        int maxZoom = viewer.getTileFactory().getInfo().getMaximumZoomLevel();
        viewer.setZoom(maxZoom);

        map.zoomOut();

        assertEquals(maxZoom, viewer.getZoom());
    }

    @Test
    public void zoomOutIncreasesZoomWhenBelowMaximumTest() {
        Map map = createMap();
        JXMapViewer viewer = getField(map, "viewer", JXMapViewer.class);

        int maxZoom = viewer.getTileFactory().getInfo().getMaximumZoomLevel();
        viewer.setZoom(maxZoom - 2);
        int before = viewer.getZoom();

        map.zoomOut();

        assertEquals(before + 1, viewer.getZoom());
    }
}
