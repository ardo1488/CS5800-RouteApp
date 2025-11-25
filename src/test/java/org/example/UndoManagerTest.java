package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UndoManagerTest {

    /* record */

    @Test
    public void recordAddsMementoToUndoStackTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));

        UndoManager manager = new UndoManager();
        manager.record(route.createMemento());

        // Change route then undo should revert, proving something was recorded
        route.addWaypoint(new GeoPosition(1.0, 1.0));
        manager.undo(route);

        assertEquals(1, route.getPoints().size());
    }

    @Test
    public void recordWithNullDoesNotModifyStacksTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));

        UndoManager manager = new UndoManager();
        manager.record(null);

        route.addWaypoint(new GeoPosition(1.0, 1.0));
        manager.undo(route); // should do nothing

        assertEquals(2, route.getPoints().size());
    }

    /* undo */

    @Test
    public void undoRestoresPreviousRouteStateTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));

        UndoManager manager = new UndoManager();
        manager.record(route.createMemento());

        route.addWaypoint(new GeoPosition(1.0, 1.0));
        assertEquals(2, route.getPoints().size());

        manager.undo(route);

        assertEquals(1, route.getPoints().size());
    }

    @Test
    public void undoWithEmptyUndoStackDoesNothingTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));

        UndoManager manager = new UndoManager();
        manager.undo(route);

        assertEquals(1, route.getPoints().size());
    }

    /* redo */

    @Test
    public void redoReappliesUndoneStateTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));

        UndoManager manager = new UndoManager();
        manager.record(route.createMemento());

        // Change and undo
        route.addWaypoint(new GeoPosition(1.0, 1.0));
        manager.undo(route);
        assertEquals(1, route.getPoints().size());

        manager.redo(route);
        assertEquals(2, route.getPoints().size());
    }

    @Test
    public void redoWithEmptyRedoStackDoesNothingTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));

        UndoManager manager = new UndoManager();
        manager.redo(route);

        assertEquals(1, route.getPoints().size());
    }

    /* clear */

    @Test
    public void clearEmptiesBothStacksTest() {
        Route route = new Route();
        route.addWaypoint(new GeoPosition(0.0, 0.0));

        UndoManager manager = new UndoManager();
        manager.record(route.createMemento());
        route.addWaypoint(new GeoPosition(1.0, 1.0));
        manager.undo(route); // push to redo

        manager.clear();

        // After clear, undo/redo should have no effect
        int sizeBefore = route.getPoints().size();
        manager.undo(route);
        manager.redo(route);
        assertEquals(sizeBefore, route.getPoints().size());
    }

    @Test
    public void clearOnAlreadyEmptyStacksKeepsThemEmptyTest() {
        Route route = new Route();
        UndoManager manager = new UndoManager();

        manager.clear();
        manager.undo(route);
        manager.redo(route);

        assertTrue(route.isEmpty());
    }
}
