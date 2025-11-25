package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class RouteServiceTest {

    private Map getMap(RouteService service) throws Exception {
        Field field = RouteService.class.getDeclaredField("map");
        field.setAccessible(true);
        return (Map) field.get(service);
    }

    private Route getCurrentRoute(RouteService service) throws Exception {
        Field field = RouteService.class.getDeclaredField("currentRoute");
        field.setAccessible(true);
        return (Route) field.get(service);
    }

    private UndoManager getUndoManager(RouteService service) throws Exception {
        Field field = RouteService.class.getDeclaredField("undoManager");
        field.setAccessible(true);
        return (UndoManager) field.get(service);
    }

    private JLabel getStatusLabel(RouteService service) throws Exception {
        Field field = RouteService.class.getDeclaredField("statusLabel");
        field.setAccessible(true);
        return (JLabel) field.get(service);
    }

    private GeoPosition getSamplePosition() {
        return new GeoPosition(10.0, 20.0);
    }

    /* constructor (RouteService) */

    @Test
    public void constructorInitializesCoreComponentsTest() throws Exception {
        RouteService service = new RouteService();

        assertNotNull(getMap(service));
        assertNotNull(getCurrentRoute(service));
        assertNotNull(getUndoManager(service));
        assertNotNull(getStatusLabel(service));

        service.dispose();
    }

    @Test
    public void constructorInitialStatusMessageIndicatesReadyStateTest() throws Exception {
        RouteService service = new RouteService();
        JLabel statusLabel = getStatusLabel(service);

        assertNotNull(statusLabel.getText());
        assertTrue(statusLabel.getText().toLowerCase().contains("ready"));

        service.dispose();
    }

    /* onDrawModeToggled */

    @Test
    public void onDrawModeToggledTrueEnablesDrawingAndUpdatesStatusTest() throws Exception {
        RouteService service = new RouteService();
        Map map = getMap(service);
        JLabel statusLabel = getStatusLabel(service);

        service.onDrawModeToggled(true);

        assertTrue(map.isDrawingMode());
        assertTrue(statusLabel.getText().contains("Draw mode ON"));

        service.dispose();
    }

    @Test
    public void onDrawModeToggledFalseDisablesDrawingAndUpdatesStatusTest() throws Exception {
        RouteService service = new RouteService();
        Map map = getMap(service);
        JLabel statusLabel = getStatusLabel(service);

        service.onDrawModeToggled(true);
        service.onDrawModeToggled(false);

        assertFalse(map.isDrawingMode());
        assertTrue(statusLabel.getText().contains("Draw mode OFF"));

        service.dispose();
    }

    /* onClearRoute */

    @Test
    public void onClearRouteClearsCurrentRouteAndPendingFlagsTest() throws Exception {
        RouteService service = new RouteService();
        Route route = getCurrentRoute(service);

        // Add a point to make route non-empty
        route.addWaypoint(getSamplePosition());

        service.onClearRoute();

        assertTrue(route.isEmpty());
        JLabel statusLabel = getStatusLabel(service);
        assertTrue(statusLabel.getText().contains("Route cleared"));

        service.dispose();
    }

    @Test
    public void onClearRouteRecordsUndoStateAllowingUndoTest() throws Exception {
        RouteService service = new RouteService();
        Route route = getCurrentRoute(service);

        route.addWaypoint(getSamplePosition());
        int originalSize = route.getPoints().size();
        assertTrue(originalSize > 0);

        service.onClearRoute();
        assertTrue(route.isEmpty());

        // Undo should restore previous route
        service.onUndo();
        assertEquals(originalSize, route.getPoints().size());

        service.dispose();
    }

    /* onUndo */

    @Test
    public void onUndoRevertsToPreviousRouteStateWhenAvailableTest() throws Exception {
        RouteService service = new RouteService();
        Route route = getCurrentRoute(service);
        UndoManager undoManager = getUndoManager(service);

        // initial state with one point
        route.addWaypoint(new GeoPosition(0.0, 0.0));
        undoManager.record(route.createMemento());

        // modified state
        route.addWaypoint(new GeoPosition(1.0, 1.0));
        assertEquals(2, route.getPoints().size());

        service.onUndo();
        assertEquals(1, route.getPoints().size());

        service.dispose();
    }

    @Test
    public void onUndoWithNoUndoStateDoesNothingTest() throws Exception {
        RouteService service = new RouteService();
        Route route = getCurrentRoute(service);

        // No mementos recorded
        route.addWaypoint(getSamplePosition());
        int sizeBefore = route.getPoints().size();

        service.onUndo();
        int sizeAfter = route.getPoints().size();

        assertEquals(sizeBefore, sizeAfter);

        service.dispose();
    }

    /* onRedo */

    @Test
    public void onRedoReappliesUndoneStateWhenAvailableTest() throws Exception {
        RouteService service = new RouteService();
        Route route = getCurrentRoute(service);
        UndoManager undoManager = getUndoManager(service);

        // original state
        route.addWaypoint(new GeoPosition(0.0, 0.0));
        undoManager.record(route.createMemento());

        // new state
        route.addWaypoint(new GeoPosition(1.0, 1.0));
        assertEquals(2, route.getPoints().size());

        // undo then redo
        service.onUndo();
        assertEquals(1, route.getPoints().size());

        service.onRedo();
        assertEquals(2, route.getPoints().size());

        service.dispose();
    }

    @Test
    public void onRedoWithNoRedoStateDoesNothingTest() throws Exception {
        RouteService service = new RouteService();
        Route route = getCurrentRoute(service);

        // no redo state prepared
        route.addWaypoint(getSamplePosition());
        int sizeBefore = route.getPoints().size();

        service.onRedo();
        int sizeAfter = route.getPoints().size();

        assertEquals(sizeBefore, sizeAfter);

        service.dispose();
    }

    /* onSaveRoute */

    @Disabled("Requires interactive JOptionPane dialogs; not suitable for automated unit tests")
    @Test
    public void onSaveRouteWithEmptyRouteShowsMessageDialogTest() throws Exception {
        RouteService service = new RouteService();
        Route route = getCurrentRoute(service);
        route.clear();

        assertDoesNotThrow(service::onSaveRoute);

        service.dispose();
    }

    @Disabled("Requires mocking JOptionPane.showInputDialog for route name input")
    @Test
    public void onSaveRoutePersistsRouteAndUpdatesIdAndNameTest() throws Exception {
        RouteService service = new RouteService();
        Route route = getCurrentRoute(service);
        route.loadFromGeoPositions(java.util.Arrays.asList(
                new GeoPosition(0.0, 0.0),
                new GeoPosition(1.0, 1.0)
        ));

        // In a real test you would mock JOptionPane.showInputDialog to return "My Route"
        // and then assert that route.getId() > 0 and route.getName().equals("My Route").

        service.dispose();
    }

    /* onLoadRoute */

    @Disabled("Requires interactive JOptionPane selection; not suitable for automated unit tests")
    @Test
    public void onLoadRouteWithNoSavedRoutesShowsNoRoutesMessageTest() throws Exception {
        // In a full integration test, ensure DB is empty and call onLoadRoute(),
        // verifying that a message dialog about 'No saved routes' is shown.
        RouteService service = new RouteService();
        assertDoesNotThrow(service::onLoadRoute);
        service.dispose();
    }

    @Disabled("Requires mocking JOptionPane route selection list")
    @Test
    public void onLoadRouteLoadsSelectedRouteIntoCurrentRouteTest() throws Exception {
        // Would involve:
        // 1. Saving a route via Database directly.
        // 2. Mocking JOptionPane.showInputDialog to return that route's summary.
        // 3. Calling onLoadRoute() and verifying currentRoute matches saved route.
    }

    /* onZoomIn */

    private int getViewerZoom(Map map) throws Exception {
        Field viewerField = Map.class.getDeclaredField("viewer");
        viewerField.setAccessible(true);
        Object viewer = viewerField.get(map);
        return (int) viewer.getClass().getMethod("getZoom").invoke(viewer);
    }

    @Test
    public void onZoomInDelegatesToMapZoomInTest() throws Exception {
        RouteService service = new RouteService();
        Map map = getMap(service);

        int before = getViewerZoom(map);
        service.onZoomIn();
        int after = getViewerZoom(map);

        assertTrue(after <= before);
        assertTrue(after >= 0);

        service.dispose();
    }

    @Test
    public void onZoomInDoesNotGoBelowMinimumZoomTest() throws Exception {
        RouteService service = new RouteService();
        Map map = getMap(service);

        // Repeated zoom in should not go below 0
        for (int i = 0; i < 50; i++) {
            service.onZoomIn();
        }

        int zoom = getViewerZoom(map);
        assertEquals(1, zoom);

        service.dispose();
    }

    /* onZoomOut */

    @Test
    public void onZoomOutDelegatesToMapZoomOutTest() throws Exception {
        RouteService service = new RouteService();
        Map map = getMap(service);

        int before = getViewerZoom(map);
        service.onZoomOut();
        int after = getViewerZoom(map);

        assertTrue(after >= before);
        assertTrue(after <= 19);

        service.dispose();
    }

    @Test
    public void onZoomOutDoesNotExceedMaximumZoomTest() throws Exception {
        RouteService service = new RouteService();
        Map map = getMap(service);

        for (int i = 0; i < 50; i++) {
            service.onZoomOut();
        }

        int zoom = getViewerZoom(map);
        assertEquals(19, zoom);

        service.dispose();
    }
}
