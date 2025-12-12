package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UndoManagerTest {

    private UndoManager createManager() {
        return new UndoManager();
    }

    private Route createRoute(int id, String name) {
        Route route = new Route();
        route.setId(id);
        route.setName(name);
        return route;
    }

    // -------------------------------------------------------------
    // recordMemento(...) tests
    // -------------------------------------------------------------

    @Test
    public void recordMementoStoresStateForUndoTest() {
        UndoManager manager = createManager();
        Route route = createRoute(1, "One");

        // Save initial state
        manager.recordMemento(route.createMemento());

        // Change route
        route.setId(2);
        route.setName("Two");

        // Undo should restore the saved state
        manager.undoMemento(route);

        assertEquals(1, route.getId());
        assertEquals("One", route.getName());
    }

    @Test
    public void recordMementoClearsRedoHistoryWhenNewStateRecordedTest() {
        UndoManager manager = createManager();
        Route route = createRoute(1, "One");

        // Record initial state and then change + undo to create redo history
        manager.recordMemento(route.createMemento());
        route.setId(2);
        route.setName("Two");
        manager.undoMemento(route); // redo stack now holds state (id=2, name="Two")

        // Now record a brand new state, which should clear the redo stack
        route.setId(3);
        route.setName("Three");
        manager.recordMemento(route.createMemento());

        // Change route again and try redo -> should do nothing because redo was cleared
        route.setId(4);
        route.setName("Four");
        manager.redoMemento(route);

        assertEquals(4, route.getId());
        assertEquals("Four", route.getName());
    }

    // -------------------------------------------------------------
    // undoMemento(...) tests
    // -------------------------------------------------------------

    @Test
    public void undoMementoRestoresPreviousStateAndPushesCurrentToRedoTest() {
        UndoManager manager = createManager();
        Route route = createRoute(1, "One");

        manager.recordMemento(route.createMemento()); // save state 1
        route.setId(2);
        route.setName("Two");
        manager.recordMemento(route.createMemento()); // save state 2

        // Change again without recording; this is the "current" state
        route.setId(3);
        route.setName("Three");

        manager.undoMemento(route);

        // Should restore state 2, and state 3 should now live in the redo stack
        assertEquals(2, route.getId());
        assertEquals("Two", route.getName());

        // Redo should bring us back to the state 3
        manager.redoMemento(route);
        assertEquals(3, route.getId());
        assertEquals("Three", route.getName());
    }

    @Test
    public void undoMementoDoesNothingWhenNoUndoHistoryTest() {
        UndoManager manager = createManager();
        Route route = createRoute(5, "Five");

        // No mementos recorded
        manager.undoMemento(route);

        // Route should remain unchanged
        assertEquals(5, route.getId());
        assertEquals("Five", route.getName());
    }

    // -------------------------------------------------------------
    // redoMemento(...) tests
    // -------------------------------------------------------------

    @Test
    public void redoMementoRestoresNextStateAndPushesCurrentToUndoTest() {
        UndoManager manager = createManager();
        Route route = createRoute(1, "One");

        // Save initial state, then change and undo to create redo history
        manager.recordMemento(route.createMemento());
        route.setId(2);
        route.setName("Two");
        manager.undoMemento(route); // redo stack holds state 2, route now back to state 1

        manager.redoMemento(route);

        // Redo brings us to state 2
        assertEquals(2, route.getId());
        assertEquals("Two", route.getName());

        // Now undo should bring us back to state 1 (current was pushed to undo)
        manager.undoMemento(route);
        assertEquals(1, route.getId());
        assertEquals("One", route.getName());
    }

    @Test
    public void redoMementoDoesNothingWhenNoRedoHistoryTest() {
        UndoManager manager = createManager();
        Route route = createRoute(10, "Ten");

        // No redo history yet
        manager.redoMemento(route);

        // Route remains unchanged
        assertEquals(10, route.getId());
        assertEquals("Ten", route.getName());
    }

    // -------------------------------------------------------------
    // clear() tests
    // -------------------------------------------------------------

    @Test
    public void clearEmptiesUndoAndRedoStacksSoNoFurtherUndoRedoTest() {
        UndoManager manager = createManager();
        Route route = createRoute(1, "One");

        manager.recordMemento(route.createMemento());
        route.setId(2);
        route.setName("Two");
        manager.undoMemento(route); // creates redo history

        manager.clear();

        // After clear, undo and redo should do nothing
        route.setId(3);
        route.setName("Three");
        manager.undoMemento(route);
        assertEquals(3, route.getId());
        assertEquals("Three", route.getName());

        manager.redoMemento(route);
        assertEquals(3, route.getId());
        assertEquals("Three", route.getName());
    }

    @Test
    public void clearAllowsFreshHistoryAfterClearTest() {
        UndoManager manager = createManager();
        Route route = createRoute(5, "Five");

        // First history
        manager.recordMemento(route.createMemento());
        route.setId(6);
        route.setName("Six");
        manager.undoMemento(route); // redo history exists

        manager.clear(); // start fresh

        // New history after clear
        route.setId(7);
        route.setName("Seven");
        manager.recordMemento(route.createMemento());
        route.setId(8);
        route.setName("Eight");

        manager.undoMemento(route);

        assertEquals(7, route.getId());
        assertEquals("Seven", route.getName());
    }
}
