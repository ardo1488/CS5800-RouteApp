package org.example;

import org.jxmapviewer.viewer.GeoPosition;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Routing API that provides road snapping functionality.
 *
 * Uses OpenRouteService API (requires API key from https://openrouteservice.org/)
 * Free tier: 2000 requests/day
 */
public class RoutingAPI {

    private static final String ORS_BASE_URL = "https://api.openrouteservice.org";
    private String apiKey;

    // Routing profile options
    public enum RoutingProfile {
        DRIVING_CAR("driving-car"),
        FOOT_WALKING("foot-walking"),
        CYCLING_REGULAR("cycling-regular");

        private final String value;
        RoutingProfile(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    private RoutingProfile currentProfile = RoutingProfile.FOOT_WALKING;

    public RoutingAPI(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Find a route between waypoints that follows actual roads/paths
     * @param waypoints List of waypoints to route between
     * @return List of points forming a route along roads, or null if routing fails
     */
    public List<GeoPosition> snapToRoads(List<GeoPosition> waypoints) {
        if (!hasApiKey() || waypoints == null || waypoints.size() < 2) {
            return null;
        }

        // Try with the current profile first
        List<GeoPosition> result = tryRouteWithProfile(waypoints, currentProfile);

        // If current profile fails and it's not foot-walking, try foot-walking as fallback
        if (result == null && currentProfile != RoutingProfile.FOOT_WALKING) {
            System.out.println("Primary profile failed, trying foot-walking as fallback...");
            result = tryRouteWithProfile(waypoints, RoutingProfile.FOOT_WALKING);
        }

        // If still null and not driving, try driving as last resort
        if (result == null && currentProfile != RoutingProfile.DRIVING_CAR) {
            System.out.println("Foot-walking failed, trying driving-car as last resort...");
            result = tryRouteWithProfile(waypoints, RoutingProfile.DRIVING_CAR);
        }

        return result;
    }

    /**
     * Try to get a route using a specific profile
     */
    private List<GeoPosition> tryRouteWithProfile(List<GeoPosition> waypoints, RoutingProfile profile) {
        try {
            // Build JSON request body - request GeoJSON format for coordinates
            StringBuilder jsonBody = new StringBuilder();
            jsonBody.append("{\"coordinates\":[");

            for (int i = 0; i < waypoints.size(); i++) {
                GeoPosition gp = waypoints.get(i);
                jsonBody.append("[").append(gp.getLongitude()).append(",").append(gp.getLatitude()).append("]");
                if (i < waypoints.size() - 1) {
                    jsonBody.append(",");
                }
            }

            jsonBody.append("],");
            jsonBody.append("\"format\":\"geojson\"");  // Request GeoJSON format!
            jsonBody.append("}");

            System.out.println("Routing request with profile " + profile.getValue() + ": " + jsonBody.toString());

            String response = makePostRequest(
                    ORS_BASE_URL + "/v2/directions/" + profile.getValue() + "/geojson",  // Use geojson endpoint
                    jsonBody.toString()
            );

            System.out.println("Response length: " + response.length());
            // Print first 500 chars for debugging
            System.out.println("Response preview: " + response.substring(0, Math.min(500, response.length())));

            // Check for error in response
            if (response.contains("\"error\"")) {
                System.err.println("API returned error response");
                // Print more of the response for debugging
                System.err.println("Error response: " + response.substring(0, Math.min(1000, response.length())));
                return null;
            }

            // Parse GeoJSON response
            List<GeoPosition> result = parseGeoJsonResponse(response);

            // Validate result
            if (result == null || result.size() < 2) {
                System.err.println("Routing failed - returned insufficient points: " + (result == null ? 0 : result.size()));
                return null;
            }

            System.out.println("Routing successful with " + profile.getValue() + ": " + result.size() + " points");
            return result;

        } catch (Exception e) {
            System.err.println("Road routing failed with " + profile.getValue() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse GeoJSON response from OpenRouteService
     * GeoJSON format: {"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"coordinates":[[lon,lat],...],"type":"LineString"},...}]}
     */
    private List<GeoPosition> parseGeoJsonResponse(String response) {
        List<GeoPosition> points = new ArrayList<>();

        try {
            System.out.println("Parsing GeoJSON response...");

            // Find the coordinates array in the geometry
            // Look for "coordinates":[[...]]
            int coordsIndex = response.indexOf("\"coordinates\"");
            if (coordsIndex == -1) {
                System.err.println("No coordinates field found in GeoJSON response");
                return points;
            }

            // Find the start of the coordinates array
            int arrayStart = response.indexOf("[[", coordsIndex);
            if (arrayStart == -1) {
                System.err.println("No coordinate array found");
                return points;
            }

            // Find the matching closing brackets - we need to find the end of the LineString coordinates
            // which is ]] followed by something that's not another [
            int depth = 0;
            int arrayEnd = arrayStart;
            boolean inArray = false;

            for (int i = arrayStart; i < response.length(); i++) {
                char c = response.charAt(i);
                if (c == '[') {
                    depth++;
                    inArray = true;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0 && inArray) {
                        arrayEnd = i + 1;
                        break;
                    }
                }
            }

            if (arrayEnd <= arrayStart) {
                System.err.println("Could not find end of coordinate array");
                return points;
            }

            // Extract the coordinates substring
            String coordsString = response.substring(arrayStart, arrayEnd);
            System.out.println("Found coordinates string, length: " + coordsString.length());
            System.out.println("Coords preview: " + coordsString.substring(0, Math.min(200, coordsString.length())));

            // Extract individual coordinate pairs: [lon,lat]
            // Pattern matches [number,number] where numbers can be negative and have decimals
            Pattern pairPattern = Pattern.compile("\\[\\s*(-?\\d+\\.?\\d*)\\s*,\\s*(-?\\d+\\.?\\d*)\\s*\\]");
            Matcher pairMatcher = pairPattern.matcher(coordsString);

            while (pairMatcher.find()) {
                try {
                    double lon = Double.parseDouble(pairMatcher.group(1));
                    double lat = Double.parseDouble(pairMatcher.group(2));
                    points.add(new GeoPosition(lat, lon));  // GeoPosition takes (lat, lon)
                } catch (NumberFormatException e) {
                    System.err.println("Failed to parse coordinate pair: " + pairMatcher.group(0));
                }
            }

            System.out.println("Parsed " + points.size() + " points from GeoJSON");

            // If we got too many points, sample them to keep rendering fast
            if (points.size() > 500) {
                List<GeoPosition> sampled = new ArrayList<>();
                int step = points.size() / 500;
                for (int i = 0; i < points.size(); i += step) {
                    sampled.add(points.get(i));
                }
                // Always include the last point
                if (!sampled.get(sampled.size() - 1).equals(points.get(points.size() - 1))) {
                    sampled.add(points.get(points.size() - 1));
                }
                System.out.println("Sampled down to " + sampled.size() + " points");
                return sampled;
            }

        } catch (Exception e) {
            System.err.println("Failed to parse GeoJSON response: " + e.getMessage());
            e.printStackTrace();
        }

        return points;
    }

    /**
     * Make a POST request to ORS API
     */
    private String makePostRequest(String urlString, String jsonBody) throws Exception {
        System.out.println("Making POST request to: " + urlString);
        System.out.println("Request body: " + jsonBody);

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Authorization", apiKey);
        conn.setRequestProperty("Accept", "application/json, application/geo+json, */*");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Read response
        int responseCode = conn.getResponseCode();
        System.out.println("Response code: " + responseCode);

        BufferedReader br;

        if (responseCode >= 200 && responseCode < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            // Read error stream
            if (conn.getErrorStream() != null) {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            } else {
                throw new Exception("API returned error " + responseCode + " with no error details");
            }
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();

        if (responseCode < 200 || responseCode >= 300) {
            System.err.println("API Error Response: " + response.toString());
            throw new Exception("API returned error " + responseCode + ": " + response.toString());
        }

        return response.toString();
    }

}