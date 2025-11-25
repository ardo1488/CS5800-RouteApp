package org.example;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.input.PanKeyListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;

public class Map extends JPanel {

    public interface MapClickListener { void onMapClick(GeoPosition position); }

    private final JXMapViewer viewer;
    private boolean drawingMode = false;
    private Route currentRoute;
    private MapClickListener clickListener;

    public Map() {
        setLayout(new BorderLayout());

        // Be polite to OSM tile servers; set a UA (replace contact with yours if shipping)
        System.setProperty("http.agent", "RouteMapApp/1.0 (contact: you@example.com)");

        // HTTPS OSM TileFactory
        TileFactoryInfo httpsOSM = new TileFactoryInfo(
                0,      // min zoom (allow one extra zoom-in level)
                19,     // max zoom
                19,     // total levels
                256,    // tile size
                true,   // x/y orientation normal
                true,   // y origin at top
                "https://tile.openstreetmap.org",
                "x","y","z") {
            @Override
            public String getTileUrl(int x, int y, int zoom) {
                int osmZ = 19 - zoom; // convert JXMapViewer zoom to OSM zoom
                return String.format("%s/%d/%d/%d.png", this.baseURL, osmZ, x, y);
            }
        };

        DefaultTileFactory tf = new DefaultTileFactory(httpsOSM);
        tf.setThreadPoolSize(8);

        viewer = new JXMapViewer();
        viewer.setTileFactory(tf);
        viewer.setBackground(Color.decode("#1e1e1e"));
        viewer.setAddressLocation(new GeoPosition(37.7749, -122.4194));
        viewer.setZoom(4);

        add(viewer, BorderLayout.CENTER);

        // === Interaction ===
        // Drag to pan
        MouseInputListener mia = new PanMouseInputListener(viewer);
        viewer.addMouseListener(mia);
        viewer.addMouseMotionListener(mia);

        // Wheel to zoom – keep cursor position stable while zooming
        viewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(viewer));

        // Arrow keys to pan
        viewer.setFocusable(true);
        viewer.addKeyListener(new PanKeyListener(viewer));

        // Click callback for adding waypoints (supports both single and double clicks)
        viewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (clickListener == null) return;
                // Trigger on every click (including double-clicks) for smooth route drawing
                clickListener.onMapClick(viewer.convertPointToGeoPosition(e.getPoint()));
            }
        });

        // Overlay painter draws current route
        viewer.setOverlayPainter(new Painter<JXMapViewer>() {
            @Override
            public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
                if (currentRoute == null) return;
                List<GeoPosition> pts = currentRoute.getAllPointsAsGeoPositions();
                if (pts == null || pts.isEmpty()) return;

                Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Stroke old = g.getStroke();
                Color oldColor = g.getColor();

                int zoom = map.getZoom();
                Rectangle viewport = map.getViewportBounds();

                // Draw the route lines in blue
                if (pts.size() >= 2) {
                    g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(new Color(30, 144, 255)); // Dodger blue color

                    for (int i = 1; i < pts.size(); i++) {
                        Point2D p1 = map.getTileFactory().geoToPixel(pts.get(i - 1), zoom);
                        Point2D p2 = map.getTileFactory().geoToPixel(pts.get(i), zoom);
                        int x1 = (int) (p1.getX() - viewport.getX());
                        int y1 = (int) (p1.getY() - viewport.getY());
                        int x2 = (int) (p2.getX() - viewport.getX());
                        int y2 = (int) (p2.getY() - viewport.getY());
                        g.drawLine(x1, y1, x2, y2);
                    }
                }

                // Draw start point marker (green circle)
                if (pts.size() >= 1) {
                    Point2D startPt = map.getTileFactory().geoToPixel(pts.get(0), zoom);
                    int sx = (int) (startPt.getX() - viewport.getX());
                    int sy = (int) (startPt.getY() - viewport.getY());

                    // Draw outer circle (white border)
                    g.setColor(Color.WHITE);
                    g.fillOval(sx - 8, sy - 8, 16, 16);

                    // Draw inner circle (green)
                    g.setColor(new Color(34, 139, 34)); // Forest green
                    g.fillOval(sx - 6, sy - 6, 12, 12);
                }

                // Draw end point marker (red circle) - only if route has more than 1 point
                if (pts.size() >= 2) {
                    Point2D endPt = map.getTileFactory().geoToPixel(pts.get(pts.size() - 1), zoom);
                    int ex = (int) (endPt.getX() - viewport.getX());
                    int ey = (int) (endPt.getY() - viewport.getY());

                    // Draw outer circle (white border)
                    g.setColor(Color.WHITE);
                    g.fillOval(ex - 8, ey - 8, 16, 16);

                    // Draw inner circle (red)
                    g.setColor(new Color(220, 20, 60)); // Crimson red
                    g.fillOval(ex - 6, ey - 6, 12, 12);
                }

                g.setStroke(old);
                g.setColor(oldColor);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
            }
        });
    }

    /* ---------------- Public API ---------------- */

    public void setMapClickListener(MapClickListener l) { this.clickListener = l; }
    public void setDrawingMode(boolean enable) { this.drawingMode = enable; }
    public boolean isDrawingMode() { return drawingMode; }

    public void displayRoute(Route route) {
        this.currentRoute = route;
        viewer.repaint();
    }

    /**
     * Set the cursor for the map panel
     */
    @Override
    public void setCursor(Cursor cursor) {
        super.setCursor(cursor);
        viewer.setCursor(cursor);
    }

    public void fitToRoute(Route route) {
        if (route == null || route.isEmpty()) return;

        List<GeoPosition> positions = route.getAllPointsAsGeoPositions();
        if (positions.isEmpty()) return;

        // Calculate bounds
        double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;

        for (GeoPosition gp : positions) {
            minLat = Math.min(minLat, gp.getLatitude());
            maxLat = Math.max(maxLat, gp.getLatitude());
            minLon = Math.min(minLon, gp.getLongitude());
            maxLon = Math.max(maxLon, gp.getLongitude());
        }

        // Calculate center
        double centerLat = (minLat + maxLat) / 2.0;
        double centerLon = (minLon + maxLon) / 2.0;

        // Calculate the span of the route
        double latSpan = maxLat - minLat;
        double lonSpan = maxLon - minLon;

        // Add padding factor to ensure route isn't right at the edge
        double paddingFactor = 1.5; // 50% padding around the route
        latSpan *= paddingFactor;
        lonSpan *= paddingFactor;

        // Get viewer dimensions
        int viewportWidth = viewer.getWidth();
        int viewportHeight = viewer.getHeight();

        // If the viewer has no size yet, we can only center without zoom-fitting
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            viewer.setAddressLocation(new GeoPosition(centerLat, centerLon));
            viewer.repaint();
            return;
        }

        TileFactoryInfo info = viewer.getTileFactory().getInfo();
        int minZoom = info.getMinimumZoomLevel();
        int maxZoom = info.getMaximumZoomLevel();

        // JXMapViewer zoom levels: smaller number = more zoomed in
        // We'll choose the most zoomed-in level where the route still fits.
        int bestZoom = maxZoom; // default to most zoomed-out

        for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
            // World pixel width at this zoom (using maxZoom as top level index for our scheme)
            int mapSize = 256 * (1 << (maxZoom - zoom));
            double pixelsPerDegreeLat = mapSize / 180.0;
            double pixelsPerDegreeLon = mapSize / 360.0;

            // Calculate how many pixels the route would span
            double routePixelHeight = latSpan * pixelsPerDegreeLat;
            double routePixelWidth = lonSpan * pixelsPerDegreeLon * Math.cos(Math.toRadians(centerLat));

            // Check if route fits in viewport at this zoom
            if (routePixelWidth <= viewportWidth && routePixelHeight <= viewportHeight) {
                bestZoom = zoom;
                break; // this is the most zoomed-in that still fits
            }
        }

        // Set the center and zoom
        viewer.setAddressLocation(new GeoPosition(centerLat, centerLon));
        viewer.setZoom(bestZoom);
        viewer.repaint();
    }

    public void zoomIn() {
        TileFactoryInfo info = viewer.getTileFactory().getInfo();
        int minZoom = info.getMinimumZoomLevel();
        int current = viewer.getZoom();
        viewer.setZoom(Math.max(minZoom, current - 1));
    }

    public void zoomOut() {
        TileFactoryInfo info = viewer.getTileFactory().getInfo();
        int maxZoom = info.getMaximumZoomLevel();
        int current = viewer.getZoom();
        viewer.setZoom(Math.min(maxZoom, current + 1));
    }
}
