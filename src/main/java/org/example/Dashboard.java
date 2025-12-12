package org.example;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Dashboard extends JPanel {

    /**
     * Custom rounded border with configurable color and corner radius
     */
    private static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;
        private final Insets insets;

        public RoundedBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
            this.insets = new Insets(thickness + 2, thickness + 6, thickness + 2, thickness + 6);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.draw(new RoundRectangle2D.Double(
                    x + thickness / 2.0, y + thickness / 2.0,
                    width - thickness, height - thickness,
                    radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return insets;
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = this.insets.left;
            insets.top = this.insets.top;
            insets.right = this.insets.right;
            insets.bottom = this.insets.bottom;
            return insets;
        }
    }

    public interface DashboardListener {
        void onDrawModeToggled(boolean enabled);
        void onClearRoute();
        void onUndo();
        void onRedo();
        void onSaveRoute();
        void onLoadRoute();
        void onZoomIn();
        void onZoomOut();
        void onGenerateRoute();
        void onLoginLogout();
    }

    private JToggleButton drawBtn;
    private JButton clearBtn;
    private JButton undoBtn;
    private JButton redoBtn;
    private JButton saveBtn;
    private JButton loadBtn;
    private JButton zoomInBtn;
    private JButton zoomOutBtn;
    private JButton generateBtn;
    private JButton loginBtn;

    private JLabel distanceLabel;
    private JLabel elevationLabel;
    private JLabel userLabel;
    private DashboardListener listener;
    private boolean mapReady = false;

    public Dashboard() { initializeUI(); }

    public void setDashboardListener(DashboardListener listener) { this.listener = listener; }

    /**
     * Update route statistics display
     * @param distanceKm Total distance in kilometers
     * @param ascentMeters Total elevation gain in meters
     * @param descentMeters Total elevation loss in meters
     */
    public void updateRouteStatsDisplay(double distanceKm, double ascentMeters, double descentMeters) {
        distanceLabel.setText(String.format("Distance: %.2f km", distanceKm));

        elevationLabel.setText(String.format("Elevation: %.0f m", ascentMeters));


        if (ascentMeters > 100) {
            elevationLabel.setForeground(new Color(178, 34, 34));  // Dark red for hilly
        } else if (ascentMeters > 50) {
            elevationLabel.setForeground(new Color(255, 140, 0));  // Orange for moderate
        } else {
            elevationLabel.setForeground(new Color(34, 139, 34));  // Green for flat
        }

        revalidate();
        repaint();
    }

    public void setMapReady() { setMapReady(true); }
    public void setMapReady(boolean ready) { this.mapReady = ready; }
    public boolean isMapReady() { return mapReady; }


    public void setDrawButtonSelected(boolean selected) {
        drawBtn.setSelected(selected);
    }


    public void setGenerateButtonEnabled(boolean enabled) {
        generateBtn.setEnabled(enabled);
    }

    private void initializeUI() {
        setLayout(new FlowLayout(FlowLayout.LEFT));

        drawBtn = new JToggleButton("Draw");
        clearBtn = new JButton("Clear");
        undoBtn = new JButton("Undo");
        redoBtn = new JButton("Redo");
        saveBtn = new JButton("Save");
        loadBtn = new JButton("Load");
        zoomInBtn = new JButton("+");
        zoomOutBtn = new JButton("-");
        generateBtn = new JButton("Generate Run");
        loginBtn = new JButton("Login");


        generateBtn.setForeground(new Color(46, 139, 87));
        generateBtn.setFont(generateBtn.getFont().deriveFont(Font.BOLD));
        generateBtn.setBorder(new RoundedBorder(new Color(46, 139, 87), 2, 10));
        generateBtn.setFocusPainted(false);
        generateBtn.setContentAreaFilled(false);

        loginBtn.setForeground(new Color(70, 130, 180));
        loginBtn.setBorder(new RoundedBorder(new Color(70, 130, 180), 2, 10));
        loginBtn.setFocusPainted(false);
        loginBtn.setContentAreaFilled(false);

        distanceLabel = new JLabel("Distance: 0.00 km");
        elevationLabel = new JLabel("Elevation: 0 m");
        userLabel = new JLabel("Guest");
        userLabel.setForeground(Color.GRAY);

        add(drawBtn); add(clearBtn); add(undoBtn); add(redoBtn);
        add(saveBtn); add(loadBtn);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(generateBtn);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(zoomInBtn); add(zoomOutBtn);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(distanceLabel);
        add(elevationLabel);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(userLabel);
        add(loginBtn);

        drawBtn.addActionListener(e -> { if (listener != null) listener.onDrawModeToggled(drawBtn.isSelected()); });
        clearBtn.addActionListener(e -> { if (listener != null) listener.onClearRoute(); });
        undoBtn.addActionListener(e -> { if (listener != null) listener.onUndo(); });
        redoBtn.addActionListener(e -> { if (listener != null) listener.onRedo(); });
        saveBtn.addActionListener(e -> { if (listener != null) listener.onSaveRoute(); });
        loadBtn.addActionListener(e -> { if (listener != null) listener.onLoadRoute(); });
        zoomInBtn.addActionListener(e -> { if (listener != null) listener.onZoomIn(); });
        zoomOutBtn.addActionListener(e -> { if (listener != null) listener.onZoomOut(); });
        generateBtn.addActionListener(e -> { if (listener != null) listener.onGenerateRoute(); });
        loginBtn.addActionListener(e -> { if (listener != null) listener.onLoginLogout(); });
    }


    public void updateUserDisplayBasedOnAuthenticationState(String username, boolean isLoggedIn) {
        if (isLoggedIn && username != null) {
            userLabel.setText(username);
            userLabel.setForeground(new Color(0, 100, 0)); // Dark green for logged in
            loginBtn.setText("Logout");
            loginBtn.setForeground(new Color(178, 34, 34)); // Dark red for logout
            loginBtn.setBorder(new RoundedBorder(new Color(178, 34, 34), 2, 10));
        } else {
            userLabel.setText("Guest");
            userLabel.setForeground(Color.GRAY);
            loginBtn.setText("Login");
            loginBtn.setForeground(new Color(70, 130, 180)); // Steel blue
            loginBtn.setBorder(new RoundedBorder(new Color(70, 130, 180), 2, 10));
        }
        revalidate();
        repaint();
    }
}