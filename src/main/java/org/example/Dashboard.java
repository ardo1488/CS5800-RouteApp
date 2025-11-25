package org.example;

import javax.swing.*;
import java.awt.*;

public class Dashboard extends JPanel {

    public interface DashboardListener {
        void onDrawModeToggled(boolean enabled);
        void onClearRoute();
        void onUndo();
        void onRedo();
        void onSaveRoute();
        void onLoadRoute();
        void onZoomIn();
        void onZoomOut();
    }

    private JToggleButton drawBtn;
    private JButton clearBtn;
    private JButton undoBtn;
    private JButton redoBtn;
    private JButton saveBtn;
    private JButton loadBtn;
    private JButton zoomInBtn;
    private JButton zoomOutBtn;

    private JLabel distanceLabel;
    private JLabel elevationLabel;
    private DashboardListener listener;
    private boolean mapReady = false;

    public Dashboard() { initializeUI(); }

    public void setDashboardListener(DashboardListener listener) { this.listener = listener; }

    public void updateRouteStats(double distanceKm, int elevationMeters) {
        distanceLabel.setText(String.format("Distance: %.2f km", distanceKm));
        elevationLabel.setText(String.format("Elevation: %d m", elevationMeters));
        revalidate();
        repaint();
    }

    public void setMapReady() { setMapReady(true); }
    public void setMapReady(boolean ready) { this.mapReady = ready; }
    public boolean isMapReady() { return mapReady; }

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

        distanceLabel = new JLabel("Distance: 0.00 km");
        elevationLabel = new JLabel("Elevation: 0 m");

        add(drawBtn); add(clearBtn); add(undoBtn); add(redoBtn);
        add(saveBtn); add(loadBtn);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(zoomInBtn); add(zoomOutBtn);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(distanceLabel); add(elevationLabel);

        drawBtn.addActionListener(e -> { if (listener != null) listener.onDrawModeToggled(drawBtn.isSelected()); });
        clearBtn.addActionListener(e -> { if (listener != null) listener.onClearRoute(); });
        undoBtn.addActionListener(e -> { if (listener != null) listener.onUndo(); });
        redoBtn.addActionListener(e -> { if (listener != null) listener.onRedo(); });
        saveBtn.addActionListener(e -> { if (listener != null) listener.onSaveRoute(); });
        loadBtn.addActionListener(e -> { if (listener != null) listener.onLoadRoute(); });
        zoomInBtn.addActionListener(e -> { if (listener != null) listener.onZoomIn(); });
        zoomOutBtn.addActionListener(e -> { if (listener != null) listener.onZoomOut(); });
    }
}