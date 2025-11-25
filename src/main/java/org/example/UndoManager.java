package org.example;

import java.util.ArrayDeque;
import java.util.Deque;

public class UndoManager {
    private final Deque<RouteMemento> undoStack = new ArrayDeque<>();
    private final Deque<RouteMemento> redoStack = new ArrayDeque<>();

    public void record(RouteMemento m) {
        if (m == null) return;
        undoStack.push(m);
        redoStack.clear();
    }

    public void undo(Route route) {
        if (route == null || undoStack.isEmpty()) return;
        RouteMemento current = route.createMemento();
        RouteMemento prev = undoStack.pop();
        redoStack.push(current);      // allow redo
        route.applyMemento(prev);
    }

    public void redo(Route route) {
        if (route == null || redoStack.isEmpty()) return;
        RouteMemento current = route.createMemento();
        RouteMemento next = redoStack.pop();
        undoStack.push(current);      // allow undo of the redo
        route.applyMemento(next);
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
