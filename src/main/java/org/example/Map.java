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

    public interface MapClickListener {
        void onMapClick(GeoPosition position);
    }

    private final JXMapViewer viewer;
    private boolean drawingMode = false;
    private Route currentRoute;
    private MapClickListener clickListener;

    private static final Color ROUTE_LINE_COLOR = new Color(30, 144, 255);
    private static final Color START_MARKER_COLOR = new Color(34, 139, 34);
    private static final Color END_MARKER_COLOR = new Color(220, 20, 60);
    private static final float ROUTE_LINE_WIDTH = 3f;
    private static final int MARKER_OUTER_RADIUS = 8;
    private static final int MARKER_INNER_RADIUS = 6;

    public Map() {
        setLayout(new BorderLayout());

        setHttpUserAgent();
        TileFactoryInfo tileFactoryInfo = createOpenStreetMapTileFactoryInfo();
        DefaultTileFactory tileFactory = createTileFactory(tileFactoryInfo);

        viewer = new JXMapViewer();
        configureViewer(tileFactory);

        add(viewer, BorderLayout.CENTER);

        setupPanningWithMouse();
        setupZoomingWithMouseWheel();
        setupPanningWithKeyboard();
        setupClickListenerForWaypoints();
        setupRouteOverlayPainter();
    }


    private void setHttpUserAgent() {
        System.setProperty("http.agent", "RouteMapApp/1.0 (contact: you@example.com)");
    }

    private TileFactoryInfo createOpenStreetMapTileFactoryInfo() {
        return new TileFactoryInfo(
                0,      // min zoom
                19,     // max zoom
                19,     // total levels
                256,    // tile size
                true,   // x/y orientation normal
                true,   // y origin at top
                "https://tile.openstreetmap.org",
                "x", "y", "z") {
            @Override
            public String getTileUrl(int x, int y, int zoom) {
                int osmZ = 19 - zoom;
                return String.format("%s/%d/%d/%d.png", this.baseURL, osmZ, x, y);
            }
        };
    }

    private DefaultTileFactory createTileFactory(TileFactoryInfo info) {
        DefaultTileFactory tf = new DefaultTileFactory(info);
        tf.setThreadPoolSize(8);
        return tf;
    }

    private void configureViewer(DefaultTileFactory tileFactory) {
        viewer.setTileFactory(tileFactory);
        viewer.setBackground(Color.decode("#1e1e1e"));
        viewer.setAddressLocation(new GeoPosition(37.7749, -122.4194));
        viewer.setZoom(4);
    }

    private void setupPanningWithMouse() {
        MouseInputListener panListener = new PanMouseInputListener(viewer);
        viewer.addMouseListener(panListener);
        viewer.addMouseMotionListener(panListener);
    }

    private void setupZoomingWithMouseWheel() {
        viewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(viewer));
    }

    private void setupPanningWithKeyboard() {
        viewer.setFocusable(true);
        viewer.addKeyListener(new PanKeyListener(viewer));
    }

    private void setupClickListenerForWaypoints() {
        viewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (clickListener == null) return;
                clickListener.onMapClick(viewer.convertPointToGeoPosition(e.getPoint()));
            }
        });
    }

    private void setupRouteOverlayPainter() {
        viewer.setOverlayPainter(new Painter<JXMapViewer>() {
            @Override
            public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
                paintRouteOverlay(g, map);
            }
        });
    }



    private void paintRouteOverlay(Graphics2D g, JXMapViewer map) {
        if (currentRoute == null) return;

        List<GeoPosition> points = currentRoute.getAllPointsAsGeoPositions();
        if (points == null || points.isEmpty()) return;

        saveGraphicsState(g);
        enableAntiAliasing(g);

        int zoom = map.getZoom();
        Rectangle viewport = map.getViewportBounds();

        drawRouteLines(g, map, points, zoom, viewport);
        drawStartMarker(g, map, points, zoom, viewport);
        drawEndMarker(g, map, points, zoom, viewport);

        restoreGraphicsState(g);
    }

    private void saveGraphicsState(Graphics2D g) {
        g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.getStroke();
        g.getColor();
    }

    private void enableAntiAliasing(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private void restoreGraphicsState(Graphics2D g) {
        // Graphics state is restored when the paint method returns
    }

    private void drawRouteLines(Graphics2D g, JXMapViewer map, List<GeoPosition> points, int zoom, Rectangle viewport) {
        if (points.size() < 2) return;

        g.setStroke(new BasicStroke(ROUTE_LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(ROUTE_LINE_COLOR);

        for (int i = 1; i < points.size(); i++) {
            drawLineBetweenPoints(g, map, points.get(i - 1), points.get(i), zoom, viewport);
        }
    }

    private void drawLineBetweenPoints(Graphics2D g, JXMapViewer map, GeoPosition from, GeoPosition to, int zoom, Rectangle viewport) {
        Point2D p1 = map.getTileFactory().geoToPixel(from, zoom);
        Point2D p2 = map.getTileFactory().geoToPixel(to, zoom);

        int x1 = (int) (p1.getX() - viewport.getX());
        int y1 = (int) (p1.getY() - viewport.getY());
        int x2 = (int) (p2.getX() - viewport.getX());
        int y2 = (int) (p2.getY() - viewport.getY());

        g.drawLine(x1, y1, x2, y2);
    }

    private void drawStartMarker(Graphics2D g, JXMapViewer map, List<GeoPosition> points, int zoom, Rectangle viewport) {
        if (points.isEmpty()) return;

        GeoPosition startPosition = points.get(0);
        int[] screenCoords = convertGeoPositionToScreenCoordinates(map, startPosition, zoom, viewport);
        drawMarkerAtScreenCoordinates(g, screenCoords, START_MARKER_COLOR);
    }

    private void drawEndMarker(Graphics2D g, JXMapViewer map, List<GeoPosition> points, int zoom, Rectangle viewport) {
        if (points.size() < 2) return;

        GeoPosition endPosition = points.get(points.size() - 1);
        int[] screenCoords = convertGeoPositionToScreenCoordinates(map, endPosition, zoom, viewport);
        drawMarkerAtScreenCoordinates(g, screenCoords, END_MARKER_COLOR);
    }

    private int[] convertGeoPositionToScreenCoordinates(JXMapViewer map, GeoPosition position, int zoom, Rectangle viewport) {
        Point2D pixelPoint = map.getTileFactory().geoToPixel(position, zoom);
        int x = (int) (pixelPoint.getX() - viewport.getX());
        int y = (int) (pixelPoint.getY() - viewport.getY());
        return new int[]{x, y};
    }

    private void drawMarkerAtScreenCoordinates(Graphics2D g, int[] screenCoords, Color innerColor) {
        drawOuterMarkerCircle(g, screenCoords[0], screenCoords[1]);
        drawInnerMarkerCircle(g, screenCoords[0], screenCoords[1], innerColor);
    }

    private void drawOuterMarkerCircle(Graphics2D g, int centerX, int centerY) {
        g.setColor(Color.WHITE);
        g.fillOval(centerX - MARKER_OUTER_RADIUS, centerY - MARKER_OUTER_RADIUS,
                MARKER_OUTER_RADIUS * 2, MARKER_OUTER_RADIUS * 2);
    }

    private void drawInnerMarkerCircle(Graphics2D g, int centerX, int centerY, Color color) {
        g.setColor(color);
        g.fillOval(centerX - MARKER_INNER_RADIUS, centerY - MARKER_INNER_RADIUS,
                MARKER_INNER_RADIUS * 2, MARKER_INNER_RADIUS * 2);
    }


    public void setMapClickListener(MapClickListener l) {
        this.clickListener = l;
    }

    public void setDrawingMode(boolean enable) {
        this.drawingMode = enable;
    }

    public boolean isDrawingMode() {
        return drawingMode;
    }

    public void displayRoute(Route route) {
        this.currentRoute = route;
        viewer.repaint();
    }

    @Override
    public void setCursor(Cursor cursor) {
        super.setCursor(cursor);
        viewer.setCursor(cursor);
    }

    public void fitToRoute(Route route) {
        if (route == null || route.isEmpty()) return;

        List<GeoPosition> positions = route.getAllPointsAsGeoPositions();
        if (positions.isEmpty()) return;

        RouteBounds bounds = calculateRouteBounds(positions);
        GeoPosition center = calculateBoundsCenter(bounds);
        RouteSpan span = calculateRouteSpanWithPadding(bounds);

        if (!isViewerSizeValid()) {
            centerMapWithoutZoom(center);
            return;
        }

        int bestZoom = calculateBestZoomLevel(span, center);
        setMapCenterAndZoom(center, bestZoom);
    }


    private static class RouteBounds {
        double minLat, maxLat, minLon, maxLon;

        RouteBounds() {
            minLat = Double.POSITIVE_INFINITY;
            maxLat = Double.NEGATIVE_INFINITY;
            minLon = Double.POSITIVE_INFINITY;
            maxLon = Double.NEGATIVE_INFINITY;
        }
    }

    private static class RouteSpan {
        double latSpan, lonSpan;

        RouteSpan(double latSpan, double lonSpan) {
            this.latSpan = latSpan;
            this.lonSpan = lonSpan;
        }
    }

    private RouteBounds calculateRouteBounds(List<GeoPosition> positions) {
        RouteBounds bounds = new RouteBounds();

        for (GeoPosition gp : positions) {
            bounds.minLat = Math.min(bounds.minLat, gp.getLatitude());
            bounds.maxLat = Math.max(bounds.maxLat, gp.getLatitude());
            bounds.minLon = Math.min(bounds.minLon, gp.getLongitude());
            bounds.maxLon = Math.max(bounds.maxLon, gp.getLongitude());
        }

        return bounds;
    }

    private GeoPosition calculateBoundsCenter(RouteBounds bounds) {
        double centerLat = (bounds.minLat + bounds.maxLat) / 2.0;
        double centerLon = (bounds.minLon + bounds.maxLon) / 2.0;
        return new GeoPosition(centerLat, centerLon);
    }

    private RouteSpan calculateRouteSpanWithPadding(RouteBounds bounds) {
        double paddingFactor = 1.5;
        double latSpan = (bounds.maxLat - bounds.minLat) * paddingFactor;
        double lonSpan = (bounds.maxLon - bounds.minLon) * paddingFactor;
        return new RouteSpan(latSpan, lonSpan);
    }

    private boolean isViewerSizeValid() {
        return viewer.getWidth() > 0 && viewer.getHeight() > 0;
    }

    private void centerMapWithoutZoom(GeoPosition center) {
        viewer.setAddressLocation(center);
        viewer.repaint();
    }

    private int calculateBestZoomLevel(RouteSpan span, GeoPosition center) {
        TileFactoryInfo info = viewer.getTileFactory().getInfo();
        int minZoom = info.getMinimumZoomLevel();
        int maxZoom = info.getMaximumZoomLevel();

        int viewportWidth = viewer.getWidth();
        int viewportHeight = viewer.getHeight();

        for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
            if (doesRouteSpanFitAtZoomLevel(span, center, zoom, maxZoom, viewportWidth, viewportHeight)) {
                return zoom;
            }
        }

        return maxZoom;
    }

    private boolean doesRouteSpanFitAtZoomLevel(RouteSpan span, GeoPosition center, int zoom, int maxZoom,
                                                int viewportWidth, int viewportHeight) {
        int mapSize = 256 * (1 << (maxZoom - zoom));
        double pixelsPerDegreeLat = mapSize / 180.0;
        double pixelsPerDegreeLon = mapSize / 360.0;

        double routePixelHeight = span.latSpan * pixelsPerDegreeLat;
        double routePixelWidth = span.lonSpan * pixelsPerDegreeLon * Math.cos(Math.toRadians(center.getLatitude()));

        return routePixelWidth <= viewportWidth && routePixelHeight <= viewportHeight;
    }

    private void setMapCenterAndZoom(GeoPosition center, int zoom) {
        viewer.setAddressLocation(center);
        viewer.setZoom(zoom);
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