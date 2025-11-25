package org.example;

import org.jxmapviewer.viewer.GeoPosition;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class RouteService extends JFrame implements Dashboard.DashboardListener {
    private final Map map;
    private final Dashboard dashboard;
    private final UndoManager undoManager;
    private final Database database;
    private final RoutingAPI routingAPI;
    private Route currentRoute;
    private GeoPosition pendingPoint;
    private boolean isRouting = false;
    private JLabel statusLabel;

    // Hardcoded API key
    private static final String API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImY3NGVlNmM5NGMzYzQ2OGM5NGRhOTNhY2Q5ZWNjMDRlIiwiaCI6Im11cm11cjY0In0=";

    public RouteService() {
        super("Route Map App");

        this.map = new Map();
        this.dashboard = new Dashboard();
        this.undoManager = new UndoManager();
        this.database = Database.getInstance();
        this.routingAPI = new RoutingAPI(API_KEY);
        this.currentRoute = new Route();

        // Create status label
        statusLabel = new JLabel("Ready - routes will snap to roads");
        statusLabel.setForeground(new Color(0, 128, 0));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        setLayout(new BorderLayout());
        add(dashboard, BorderLayout.NORTH);
        add(map, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        dashboard.setDashboardListener(this);

        map.setMapClickListener(clickedPoint -> {
            if (map.isDrawingMode()) {
                handleMapClick(clickedPoint);
            }
        });

        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                try { database.close(); } catch (Exception ignored) {}
            }
        });

        dashboard.setMapReady(true);
        map.displayRoute(currentRoute);
        refreshStats();
    }

    private void setStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    private void handleMapClick(GeoPosition clickedPoint) {
        if (isRouting) {
            pendingPoint = clickedPoint;
            return;
        }

        undoManager.record(currentRoute.createMemento());

        List<GeoPosition> currentPoints = currentRoute.getAllPointsAsGeoPositions();

        // If this is the first point, just add it directly
        if (currentPoints.isEmpty()) {
            currentRoute.addWaypoint(clickedPoint);
            map.displayRoute(currentRoute);
            refreshStats();
            setStatus("Start point added. Click to add more points.", new Color(0, 128, 0));
            return;
        }

        // Route to the new point
        GeoPosition lastPoint = currentPoints.get(currentPoints.size() - 1);

        isRouting = true;
        map.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setStatus("Finding road route...", Color.BLUE);

        List<GeoPosition> routeRequest = new ArrayList<>();
        routeRequest.add(lastPoint);
        routeRequest.add(clickedPoint);

        SwingWorker<List<GeoPosition>, Void> worker = new SwingWorker<List<GeoPosition>, Void>() {
            @Override
            protected List<GeoPosition> doInBackground() {
                return routingAPI.snapToRoads(routeRequest);
            }

            @Override
            protected void done() {
                isRouting = false;
                map.setCursor(Cursor.getDefaultCursor());

                try {
                    List<GeoPosition> routedPath = get();

                    if (routedPath != null && routedPath.size() >= 2) {
                        for (int i = 1; i < routedPath.size(); i++) {
                            currentRoute.addWaypoint(routedPath.get(i));
                        }
                        setStatus("Route snapped to roads (" + routedPath.size() + " points)", new Color(0, 128, 0));
                    } else {
                        currentRoute.addWaypoint(clickedPoint);
                        setStatus("Road routing failed - using straight line", Color.RED);
                    }

                    map.displayRoute(currentRoute);
                    refreshStats();

                    if (pendingPoint != null) {
                        GeoPosition nextPoint = pendingPoint;
                        pendingPoint = null;
                        SwingUtilities.invokeLater(() -> handleMapClick(nextPoint));
                    }

                } catch (Exception ex) {
                    System.err.println("Routing error: " + ex.getMessage());
                    currentRoute.addWaypoint(clickedPoint);
                    map.displayRoute(currentRoute);
                    refreshStats();
                    setStatus("Routing error: " + ex.getMessage(), Color.RED);

                    if (pendingPoint != null) {
                        GeoPosition nextPoint = pendingPoint;
                        pendingPoint = null;
                        SwingUtilities.invokeLater(() -> handleMapClick(nextPoint));
                    }
                }
            }
        };
        worker.execute();
    }

    @Override
    public void onDrawModeToggled(boolean enabled) {
        map.setDrawingMode(enabled);
        if (enabled) {
            setStatus("Draw mode ON - click to add road-snapped waypoints", new Color(0, 128, 0));
        } else {
            setStatus("Draw mode OFF", Color.GRAY);
        }
    }

    @Override
    public void onClearRoute() {
        undoManager.record(currentRoute.createMemento());
        currentRoute.clear();
        pendingPoint = null;
        isRouting = false;
        map.displayRoute(currentRoute);
        refreshStats();
        setStatus("Route cleared", Color.GRAY);
    }

    @Override
    public void onUndo() {
        undoManager.undo(currentRoute);
        map.displayRoute(currentRoute);
        refreshStats();
    }

    @Override
    public void onRedo() {
        undoManager.redo(currentRoute);
        map.displayRoute(currentRoute);
        refreshStats();
    }

    @Override
    public void onSaveRoute() {
        if (currentRoute.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nothing to save yet.");
            return;
        }
        String name = JOptionPane.showInputDialog(this, "Route name:", "Save Route", JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        int newId = database.saveRoute(
                name.trim(),
                currentRoute.getTotalDistance(),
                currentRoute.getEstimatedElevation(),
                currentRoute.getAllPointsAsGeoPositions()
        );
        currentRoute.setName(name.trim());
        currentRoute.setId(newId);
        JOptionPane.showMessageDialog(this, "Route saved.");
    }

    @Override
    public void onLoadRoute() {
        List<Database.RouteSummary> routes = database.getAllRoutes();
        if (routes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No saved routes found.");
            return;
        }

        Database.RouteSummary choice = (Database.RouteSummary) JOptionPane.showInputDialog(
                this, "Choose a route:", "Load Route", JOptionPane.PLAIN_MESSAGE, null,
                routes.toArray(), routes.get(0)
        );
        if (choice == null) return;

        List<GeoPosition> pts = database.loadRoutePoints(choice.getId());
        if (pts == null || pts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "That route has no points.");
            return;
        }

        currentRoute = new Route();
        currentRoute.setId(choice.getId());
        currentRoute.setName(choice.getName());
        currentRoute.loadFromGeoPositions(pts);

        undoManager.clear();
        map.displayRoute(currentRoute);

        SwingUtilities.invokeLater(() -> {
            map.fitToRoute(currentRoute);
        });

        refreshStats();
        setStatus("Loaded route: " + choice.getName(), new Color(0, 128, 0));
    }

    @Override public void onZoomIn()  { map.zoomIn();  }
    @Override public void onZoomOut() { map.zoomOut(); }

    private void refreshStats() {
        dashboard.updateRouteStats(currentRoute.getTotalDistance(), currentRoute.getEstimatedElevation());
    }
}