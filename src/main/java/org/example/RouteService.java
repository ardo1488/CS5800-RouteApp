package org.example;

import org.jxmapviewer.viewer.GeoPosition;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class RouteService extends JFrame implements Dashboard.DashboardListener, AuthContext.AuthStateListener {
    private final Map map;
    private final Dashboard dashboard;
    private final UndoManager undoManager;
    private final Database database;
    private final RoutingAPI routingAPI;
    private final AuthContext authContext;
    private UserProfile userProfile;
    private Route currentRoute;
    private GeoPosition pendingPoint;
    private boolean isRouting = false;
    private JLabel statusLabel;

    // Route generation state
    private boolean isGenerateMode = false;
    private GeoPosition generateStartPoint = null;

    // Hardcoded API key
    private static final String API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImY3NGVlNmM5NGMzYzQ2OGM5NGRhOTNhY2Q5ZWNjMDRlIiwiaCI6Im11cm11cjY0In0=";

    public RouteService() {
        super("Route Map App");

        this.map = new Map();
        this.dashboard = new Dashboard();
        this.undoManager = new UndoManager();
        this.database = Database.getInstance();
        this.routingAPI = new RoutingAPI(API_KEY);
        this.authContext = AuthContext.getInstance();
        this.userProfile = UserProfile.getInstanceForNonLoggedInUser();
        this.currentRoute = new Route();

        authContext.addListener(this);
        userProfile.applyUserSettingsToRoutingAPI(routingAPI);

        initializeUserInterface();
        setupMapClickListener();
        setupWindowCloseListener();

        dashboard.setMapReady(true);
        map.displayRoute(currentRoute);
        refreshStats();
    }



    private void initializeUserInterface() {
        statusLabel = createStatusLabel();

        setLayout(new BorderLayout());
        add(dashboard, BorderLayout.NORTH);
        add(map, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        dashboard.setDashboardListener(this);

        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private JLabel createStatusLabel() {
        JLabel label = new JLabel("Ready - routes will snap to roads");
        label.setForeground(new Color(0, 128, 0));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        return label;
    }

    private void setupMapClickListener() {
        map.setMapClickListener(clickedPoint -> {
            if (isGenerateMode) {
                handleGenerateStartPointClick(clickedPoint);
            } else if (map.isDrawingMode()) {
                handleMapClick(clickedPoint);
            }
        });
    }

    private void setupWindowCloseListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    database.close();
                } catch (Exception ignored) {
                }
            }
        });
    }



    private void setStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    private void setStatusSuccess(String message) {
        setStatus(message, new Color(0, 128, 0));
    }

    private void setStatusError(String message) {
        setStatus(message, Color.RED);
    }

    private void setStatusInfo(String message) {
        setStatus(message, Color.BLUE);
    }

    private void setStatusNeutral(String message) {
        setStatus(message, Color.GRAY);
    }



    private void handleMapClick(GeoPosition clickedPoint) {
        if (isRouting) {
            pendingPoint = clickedPoint;
            return;
        }

        undoManager.recordMemento(currentRoute.createMemento());

        List<GeoPosition> currentPoints = currentRoute.getAllPointsAsGeoPositions();

        if (currentPoints.isEmpty()) {
            addFirstWaypointToRoute(clickedPoint);
        } else {
            routeFromLastPointToClickedPoint(clickedPoint, currentPoints);
        }
    }

    private void addFirstWaypointToRoute(GeoPosition clickedPoint) {
        currentRoute.addWaypoint(clickedPoint);
        updateMapAndRefreshStats();
        setStatusSuccess("Start point added. Click to add more points.");
    }

    private void routeFromLastPointToClickedPoint(GeoPosition clickedPoint, List<GeoPosition> currentPoints) {
        GeoPosition lastPoint = currentPoints.get(currentPoints.size() - 1);

        setRoutingInProgress(true);
        setStatusInfo("Finding road route...");

        List<GeoPosition> routeRequest = createTwoPointRouteRequest(lastPoint, clickedPoint);
        executeRoutingApiCall(clickedPoint, routeRequest);
    }

    private List<GeoPosition> createTwoPointRouteRequest(GeoPosition fromPoint, GeoPosition toPoint) {
        List<GeoPosition> routeRequest = new ArrayList<>();
        routeRequest.add(fromPoint);
        routeRequest.add(toPoint);
        return routeRequest;
    }

    private void setRoutingInProgress(boolean inProgress) {
        isRouting = inProgress;
        if (inProgress) {
            map.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            map.setCursor(Cursor.getDefaultCursor());
        }
    }

    private void executeRoutingApiCall(GeoPosition clickedPoint, List<GeoPosition> routeRequest) {
        SwingWorker<RouteResult, Void> worker = new SwingWorker<RouteResult, Void>() {
            @Override
            protected RouteResult doInBackground() {
                return routingAPI.snapToRoadsWithTwoPoints(routeRequest);
            }

            @Override
            protected void done() {
                setRoutingInProgress(false);
                try {
                    RouteResult routeResult = get();
                    handleRoutingApiResult(routeResult, clickedPoint);
                } catch (Exception ex) {
                    handleRoutingApiException(ex, clickedPoint);
                }
                processAnyPendingPoint();
            }
        };
        worker.execute();
    }

    private void handleRoutingApiResult(RouteResult routeResult, GeoPosition clickedPoint) {
        if (routeResult != null && routeResult.getPointCount() >= 2) {
            addRoutedPathToCurrentRoute(routeResult);
            setStatusSuccess("Route snapped to roads (" + routeResult.getPointCount() + " points)");
        } else {
            addStraightLineAsFallback(clickedPoint);
            setStatusError("Road routing failed - using straight line");
        }
        updateMapAndRefreshStats();
    }

    private void addRoutedPathToCurrentRoute(RouteResult routeResult) {
        List<GeoPosition> routedPath = routeResult.getPoints();
        for (int i = 1; i < routedPath.size(); i++) {
            currentRoute.addWaypoint(routedPath.get(i));
        }
        currentRoute.addElevation(routeResult.getAscent(), routeResult.getDescent());
    }

    private void addStraightLineAsFallback(GeoPosition clickedPoint) {
        currentRoute.addWaypoint(clickedPoint);
    }

    private void handleRoutingApiException(Exception ex, GeoPosition clickedPoint) {
        System.err.println("Routing error: " + ex.getMessage());
        addStraightLineAsFallback(clickedPoint);
        updateMapAndRefreshStats();
        setStatusError("Routing error: " + ex.getMessage());
    }

    private void processAnyPendingPoint() {
        if (pendingPoint != null) {
            GeoPosition nextPoint = pendingPoint;
            pendingPoint = null;
            SwingUtilities.invokeLater(() -> handleMapClick(nextPoint));
        }
    }



    private void handleGenerateStartPointClick(GeoPosition clickedPoint) {
        generateStartPoint = clickedPoint;
        isGenerateMode = false;
        map.setCursor(Cursor.getDefaultCursor());
        promptForDistanceAndGenerate();
    }



    private void promptForDistanceAndGenerate() {
        if (generateStartPoint == null) {
            setStatusError("No starting point selected");
            return;
        }

        JPanel panel = createRouteGenerationDialogPanel();
        int result = JOptionPane.showConfirmDialog(this, panel, "Generate Running Route",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            extractInputAndStartGeneration(panel);
        } else {
            setStatusNeutral("Route generation cancelled");
        }

        generateStartPoint = null;
    }

    private JPanel createRouteGenerationDialogPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addDistanceSpinnerToPanel(panel, gbc);
        addVarietyComboToPanel(panel, gbc);
        addCoordinatesLabelToPanel(panel, gbc);

        return panel;
    }

    private void addDistanceSpinnerToPanel(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Desired distance (km):"), gbc);

        JSpinner distanceSpinner = new JSpinner(new SpinnerNumberModel(
                userProfile.getPreferredDistanceKm(), 0.5, 50.0, 0.5));
        distanceSpinner.setEditor(new JSpinner.NumberEditor(distanceSpinner, "0.0"));
        distanceSpinner.setName("distanceSpinner");
        gbc.gridx = 1;
        panel.add(distanceSpinner, gbc);
    }

    private void addVarietyComboToPanel(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Route variety:"), gbc);

        String[] varietyOptions = {"Simple (3 points)", "Medium (5 points)", "Complex (10 points)"};
        JComboBox<String> varietyCombo = new JComboBox<>(varietyOptions);
        varietyCombo.setSelectedIndex(convertVarietyToIndex(userProfile.getPreferredRouteVariety()));
        varietyCombo.setName("varietyCombo");
        gbc.gridx = 1;
        panel.add(varietyCombo, gbc);
    }

    private void addCoordinatesLabelToPanel(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JLabel coordLabel = new JLabel(String.format("Start: %.5f, %.5f",
                generateStartPoint.getLatitude(), generateStartPoint.getLongitude()));
        coordLabel.setForeground(Color.GRAY);
        panel.add(coordLabel, gbc);
    }

    private int convertVarietyToIndex(int varietyPoints) {
        if (varietyPoints == 3) return 0;
        if (varietyPoints == 5) return 1;
        return 2;
    }

    private int convertIndexToVarietyPoints(int index) {
        if (index == 0) return 3;
        if (index == 1) return 5;
        return 10;
    }

    private void extractInputAndStartGeneration(JPanel dialogPanel) {
        JSpinner distanceSpinner = findComponentByName(dialogPanel, "distanceSpinner", JSpinner.class);
        JComboBox<?> varietyCombo = findComponentByName(dialogPanel, "varietyCombo", JComboBox.class);

        double distanceKm = (Double) distanceSpinner.getValue();
        int points = convertIndexToVarietyPoints(varietyCombo.getSelectedIndex());

        userProfile.setPreferredDistanceKm(distanceKm);
        userProfile.setPreferredRouteVariety(points);

        generateRoundTripRoute(generateStartPoint, distanceKm, points);
    }

    @SuppressWarnings("unchecked")
    private <T> T findComponentByName(JPanel panel, String name, Class<T> type) {
        for (Component comp : panel.getComponents()) {
            if (name.equals(comp.getName()) && type.isInstance(comp)) {
                return (T) comp;
            }
        }
        throw new IllegalStateException("Component not found: " + name);
    }



    private void generateRoundTripRoute(GeoPosition startPoint, double distanceKm, int points) {
        if (isRouting) {
            setStatus("Please wait for current operation to complete", Color.ORANGE);
            return;
        }

        setGenerationInProgress(true, distanceKm);
        Integer seed = (int) (System.currentTimeMillis() % 100000);
        executeGenerationApiCall(startPoint, distanceKm, points, seed);
    }

    private void setGenerationInProgress(boolean inProgress, double distanceKm) {
        isRouting = inProgress;
        if (inProgress) {
            map.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            dashboard.setGenerateButtonEnabled(false);
            setStatusInfo(String.format("Generating %.1f km running route...", distanceKm));
        } else {
            map.setCursor(Cursor.getDefaultCursor());
            dashboard.setGenerateButtonEnabled(true);
        }
    }

    private void executeGenerationApiCall(GeoPosition startPoint, double distanceKm, int points, Integer seed) {
        SwingWorker<RouteResult, Void> worker = new SwingWorker<RouteResult, Void>() {
            @Override
            protected RouteResult doInBackground() {
                return routingAPI.generateRoundTripWithAPI(startPoint, distanceKm, points, seed);
            }

            @Override
            protected void done() {
                setGenerationInProgress(false, 0);
                try {
                    RouteResult routeResult = get();
                    handleGenerationResult(routeResult);
                } catch (Exception ex) {
                    handleGenerationException(ex);
                }
            }
        };
        worker.execute();
    }

    private void handleGenerationResult(RouteResult routeResult) {
        if (routeResult != null && routeResult.getPointCount() >= 3) {
            applyGeneratedRoute(routeResult);
            displayGenerationSuccessMessage(routeResult);
        } else {
            setStatusError("Failed to generate route - try a different location or distance");
        }
    }

    private void applyGeneratedRoute(RouteResult routeResult) {
        undoManager.recordMemento(currentRoute.createMemento());

        currentRoute = new Route();
        currentRoute.loadRouteFromGeoPositions(routeResult.getPoints());
        currentRoute.setElevation(routeResult.getAscent(), routeResult.getDescent());

        userProfile.recordRouteGenerated();

        System.out.println("DEBUG: RouteResult ascent=" + routeResult.getAscent() +
                ", descent=" + routeResult.getDescent());
        System.out.println("DEBUG: currentRoute ascent=" + currentRoute.getAscentInMeters() +
                ", descent=" + currentRoute.getDescentInMeters());

        map.displayRoute(currentRoute);

        if (userProfile.isAutoFitRoute()) {
            SwingUtilities.invokeLater(() -> map.fitToRoute(currentRoute));
        }

        refreshStats();
    }

    private void displayGenerationSuccessMessage(RouteResult routeResult) {
        double actualDistance = currentRoute.getTotalDistance();
        setStatusSuccess(String.format("Generated %s (↑%s)",
                userProfile.formatDistanceForUnitPreference(actualDistance),
                userProfile.formatElevationForUnitPreference(routeResult.getAscent())));
    }

    private void handleGenerationException(Exception ex) {
        System.err.println("Route generation error: " + ex.getMessage());
        ex.printStackTrace();
        setStatusError("Route generation error: " + ex.getMessage());
    }



    @Override
    public void onDrawModeToggled(boolean enabled) {
        if (enabled && isGenerateMode) {
            isGenerateMode = false;
            generateStartPoint = null;
        }

        map.setDrawingMode(enabled);
        if (enabled) {
            setStatusSuccess("Draw mode ON - click to add road-snapped waypoints");
        } else {
            setStatusNeutral("Draw mode OFF");
        }
    }

    @Override
    public void onClearRoute() {
        undoManager.recordMemento(currentRoute.createMemento());
        resetAllRouteState();
        updateMapAndRefreshStats();
        setStatusNeutral("Route cleared");
    }

    private void resetAllRouteState() {
        currentRoute.clear();
        pendingPoint = null;
        isRouting = false;
        isGenerateMode = false;
        generateStartPoint = null;
    }

    @Override
    public void onUndo() {
        undoManager.undoMemento(currentRoute);
        updateMapAndRefreshStats();
    }

    @Override
    public void onRedo() {
        undoManager.redoMemento(currentRoute);
        updateMapAndRefreshStats();
    }

    @Override
    public void onSaveRoute() {
        if (currentRoute.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nothing to save yet.");
            return;
        }

        String name = promptForRouteName();
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        saveRouteToDatabase(name.trim());
        JOptionPane.showMessageDialog(this, "Route saved.");
    }

    private String promptForRouteName() {
        return JOptionPane.showInputDialog(this, "Route name:", "Save Route", JOptionPane.QUESTION_MESSAGE);
    }

    private void saveRouteToDatabase(String name) {
        int newId = database.saveRoute(
                name,
                currentRoute.getTotalDistance(),
                currentRoute.getEstimatedElevation(),
                currentRoute.getAllPointsAsGeoPositions()
        );
        currentRoute.setName(name);
        currentRoute.setId(newId);
    }

    @Override
    public void onLoadRoute() {
        List<Database.RouteSummary> routes = database.getAllRoutes();
        if (routes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No saved routes found.");
            return;
        }

        Database.RouteSummary choice = promptForRouteSelection(routes);
        if (choice == null) {
            return;
        }

        loadRouteFromDatabase(choice);
    }

    private Database.RouteSummary promptForRouteSelection(List<Database.RouteSummary> routes) {
        return (Database.RouteSummary) JOptionPane.showInputDialog(
                this, "Choose a route:", "Load Route", JOptionPane.PLAIN_MESSAGE, null,
                routes.toArray(), routes.get(0)
        );
    }

    private void loadRouteFromDatabase(Database.RouteSummary choice) {
        List<GeoPosition> pts = database.loadRoutePoints(choice.getId());
        if (pts == null || pts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "That route has no points.");
            return;
        }

        currentRoute = new Route();
        currentRoute.setId(choice.getId());
        currentRoute.setName(choice.getName());
        currentRoute.loadRouteFromGeoPositions(pts);

        undoManager.clear();
        updateMapAndRefreshStats();

        SwingUtilities.invokeLater(() -> map.fitToRoute(currentRoute));

        setStatusSuccess("Loaded route: " + choice.getName());
    }

    @Override
    public void onGenerateRoute() {
        if (isRouting) {
            setStatus("Please wait for current operation to complete", Color.ORANGE);
            return;
        }

        if (isGenerateMode) {
            exitGenerateMode();
        } else {
            enterGenerateMode();
        }
    }

    private void exitGenerateMode() {
        isGenerateMode = false;
        generateStartPoint = null;
        map.setCursor(Cursor.getDefaultCursor());
        map.setDrawingMode(false);
        dashboard.setDrawButtonSelected(false);
        setStatusNeutral("Route generation cancelled");
    }

    private void enterGenerateMode() {
        isGenerateMode = true;
        generateStartPoint = null;

        map.setDrawingMode(false);
        dashboard.setDrawButtonSelected(false);
        map.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        setStatus("Click on the map to select your starting point for the run", new Color(0, 100, 200));
    }

    @Override
    public void onZoomIn() {
        map.zoomIn();
    }

    @Override
    public void onZoomOut() {
        map.zoomOut();
    }

    @Override
    public void onLoginLogout() {
        if (authContext.isAuthenticated()) {
            confirmAndLogout();
        } else {
            showLoginDialog();
        }
    }

    private void confirmAndLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            authContext.logout();
        }
    }

    private void showLoginDialog() {
        LoginDialog loginDialog = new LoginDialog(this);
        loginDialog.showDialogAndLoginStatus();
    }


    @Override
    public void onStateChanged(AuthState oldState, AuthState newState) {
        System.out.println("Auth state changed: " + oldState.getStateName() + " -> " + newState.getStateName());
    }

    @Override
    public void onLoginSuccess(UserProfile user) {
        switchToUserProfile(user);
        dashboard.updateUserDisplayBasedOnAuthenticationState(user.getUserName(), true);
        setStatusSuccess("Welcome, " + user.getUserName() + "!");
    }

    @Override
    public void onLoginFailure(String reason) {
        setStatusError("Login failed: " + reason);
    }

    @Override
    public void onLogout() {
        switchToGuestProfile();
        dashboard.updateUserDisplayBasedOnAuthenticationState(null, false);
        setStatusNeutral("Logged out");
    }

    @Override
    public void onRegistrationSuccess(UserProfile user) {
        switchToUserProfile(user);
        dashboard.updateUserDisplayBasedOnAuthenticationState(user.getUserName(), true);
        setStatusSuccess("Account created! Welcome, " + user.getUserName() + "!");
    }

    @Override
    public void onRegistrationFailure(String reason) {
        setStatusError("Registration failed: " + reason);
    }

    private void switchToUserProfile(UserProfile user) {
        this.userProfile = user;
        userProfile.applyUserSettingsToRoutingAPI(routingAPI);
    }

    private void switchToGuestProfile() {
        this.userProfile = UserProfile.getInstanceForNonLoggedInUser();
        userProfile.applyUserSettingsToRoutingAPI(routingAPI);
    }


    private void updateMapAndRefreshStats() {
        map.displayRoute(currentRoute);
        refreshStats();
    }

    private void refreshStats() {
        dashboard.updateRouteStatsDisplay(currentRoute.getTotalDistance(),
                currentRoute.getAscentInMeters(), currentRoute.getDescentInMeters());
    }
}