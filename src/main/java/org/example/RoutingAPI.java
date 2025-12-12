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
 * Routing API that provides road snapping and round-trip route generation functionality.
 *
 * OpenRouteService API
 */
public class RoutingAPI {

    private static final String ORS_BASE_URL = "https://api.openrouteservice.org";
    private final String apiKey;

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

    public void setProfile(RoutingProfile profile) {
        this.currentProfile = profile;
    }

    public RoutingProfile getProfile() {
        return currentProfile;
    }


    public RouteResult generateRoundTripWithAPI(GeoPosition startPoint, double distanceKm) {
        return generateRoundTripWithAPI(startPoint, distanceKm, 5, null);
    }


    public RouteResult generateRoundTripWithAPI(GeoPosition startPoint, double distanceKm, int points, Integer seed) {
        if (!hasApiKey() || startPoint == null || distanceKm <= 0) {
            return null;
        }

        int distanceMeters = convertKMtoMeters(distanceKm);

        points = makeSureAtLeast3Points(points);

        // Try with foot-walking first (most likely to succeed for running routes)
        RouteResult result = generateRoundTripWithProfileAndElevation(startPoint, distanceMeters, points, seed, RoutingProfile.FOOT_WALKING);

        // If foot-walking fails, try cycling as fallback
        if (result == null) {
            System.out.println("Foot-walking round trip failed, trying cycling...");
            result = generateRoundTripWithProfileAndElevation(startPoint, distanceMeters, points, seed, RoutingProfile.CYCLING_REGULAR);
        }

        return result;
    }


    private RouteResult generateRoundTripWithProfileAndElevation(GeoPosition startPoint, int distanceMeters,
                                                                 int points, Integer seed, RoutingProfile profile) {
        try {

            StringBuilder jsonBody = buildJsonRequestForRoundTrip(startPoint, distanceMeters, points, seed);

            System.out.println("Round trip request with profile " + profile.getValue() + ": " + jsonBody.toString());

            String response = makePostRequest(
                    ORS_BASE_URL + "/v2/directions/" + profile.getValue() + "/geojson",
                    jsonBody.toString()
            );

            System.out.println("Response length: " + response.length());
            System.out.println("Response preview: " + response.substring(0, Math.min(500, response.length())));

            // Check for error in response
            checkIfResponseHasError(response);

            // Parse GeoJSON response with elevation
            RouteResult result = parseGeoJsonResponseWithElevation(response);

            // Validate result
            if (result == null || result.getPointCount() < 3) {
                System.err.println("Round trip generation failed - returned insufficient points: " +
                        (result == null ? 0 : result.getPointCount()));
                return null;
            }

            System.out.println("Round trip generation successful with " + profile.getValue() +
                    ": " + result.getPointCount() + " points, ascent: " + result.getAscent() +
                    "m, descent: " + result.getDescent() + "m");
            return result;

        } catch (Exception e) {
            System.err.println("Round trip generation failed with " + profile.getValue() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    public RouteResult snapToRoadsWithTwoPoints(List<GeoPosition> waypoints) {
        if (!hasApiKey() || waypoints == null || waypoints.size() < 2) {
            return null;
        }

        // Try with the current profile first
        RouteResult result = tryRouteWithProfile(waypoints, currentProfile);

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


    private RouteResult tryRouteWithProfile(List<GeoPosition> waypoints, RoutingProfile profile) {
        try {
            StringBuilder jsonBody = buildJasonRequestForRouteWithProfile(waypoints);

            System.out.println("Routing request with profile " + profile.getValue() + ": " + jsonBody.toString());

            String response = makePostRequest(
                    ORS_BASE_URL + "/v2/directions/" + profile.getValue() + "/geojson",
                    jsonBody.toString()
            );

            System.out.println("Response length: " + response.length());
            System.out.println("Response preview: " + response.substring(0, Math.min(500, response.length())));


            checkIfResponseHasError(response);

            RouteResult result = parseGeoJsonResponseWithElevation(response);

            // Validate result
            if (result == null || result.getPointCount() < 2) {
                System.err.println("Routing failed - returned insufficient points: " +
                        (result == null ? 0 : result.getPointCount()));
                return null;
            }

            System.out.println("Routing successful with " + profile.getValue() +
                    ": " + result.getPointCount() + " points, ascent: " + result.getAscent() +
                    "m, descent: " + result.getDescent() + "m");
            return result;

        } catch (Exception e) {
            System.err.println("Road routing failed with " + profile.getValue() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    private RouteResult parseGeoJsonResponseWithElevation(String response) {
        List<GeoPosition> points = new ArrayList<>();
        double ascent = 0;
        double descent = 0;
        double distance = 0;

        try {
            System.out.println("Parsing GeoJSON response with elevation...");


            System.out.println("Full response (first 2000 chars): " +
                    response.substring(0, Math.min(2000, response.length())));

            // Extract ascent and descent from properties.summary
            // The response contains: "summary":{"distance":X,"ascent":Y,"descent":Z}
            // Look for "ascent":value and "descent":value (handles both int and float)
            Pattern ascentPattern = Pattern.compile("\"ascent\"\\s*:\\s*(-?\\d+\\.?\\d*(?:[eE][+-]?\\d+)?)");
            Pattern descentPattern = Pattern.compile("\"descent\"\\s*:\\s*(-?\\d+\\.?\\d*(?:[eE][+-]?\\d+)?)");
            Pattern distancePattern = Pattern.compile("\"distance\"\\s*:\\s*(-?\\d+\\.?\\d*(?:[eE][+-]?\\d+)?)");

            Matcher ascentMatcher = ascentPattern.matcher(response);
            if (ascentMatcher.find()) {
                ascent = Double.parseDouble(ascentMatcher.group(1));
                System.out.println("Found ascent: " + ascent);
            } else {
                System.out.println("WARNING: No ascent value found in response");
            }

            Matcher descentMatcher = descentPattern.matcher(response);
            if (descentMatcher.find()) {
                descent = Double.parseDouble(descentMatcher.group(1));
                System.out.println("Found descent: " + descent);
            } else {
                System.out.println("WARNING: No descent value found in response");
            }

            Matcher distanceMatcher = distancePattern.matcher(response);
            if (distanceMatcher.find()) {
                distance = Double.parseDouble(distanceMatcher.group(1));
                System.out.println("Found distance: " + distance);
            }

            // Find the coordinates array in the geometry
            int coordsIndex = response.indexOf("\"coordinates\"");
            if (coordsIndex == -1) {
                System.err.println("No coordinates field found in GeoJSON response");
                return new RouteResult(points, ascent, descent, distance);
            }

            // Find the start of the coordinates array
            int arrayStart = response.indexOf("[[", coordsIndex);
            if (arrayStart == -1) {
                System.err.println("No coordinate array found");
                return new RouteResult(points, ascent, descent, distance);
            }

            // Find the matching closing brackets
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
                return new RouteResult(points, ascent, descent, distance);
            }

            // Extract the coordinates substring
            String coordsString = response.substring(arrayStart, arrayEnd);
            System.out.println("Found coordinates string, length: " + coordsString.length());

            // Extract individual coordinate pairs/triples: [lon,lat] or [lon,lat,elevation]
            // Pattern matches [number,number] or [number,number,number]
            Pattern pairPattern = Pattern.compile("\\[\\s*(-?\\d+\\.?\\d*)\\s*,\\s*(-?\\d+\\.?\\d*)(?:\\s*,\\s*(-?\\d+\\.?\\d*))?\\s*\\]");
            Matcher pairMatcher = pairPattern.matcher(coordsString);

            while (pairMatcher.find()) {
                try {
                    double lon = Double.parseDouble(pairMatcher.group(1));
                    double lat = Double.parseDouble(pairMatcher.group(2));
                    // Elevation is in group 3 if present (we don't store it per-point currently)
                    points.add(new GeoPosition(lat, lon));
                } catch (NumberFormatException e) {
                    System.err.println("Failed to parse coordinate: " + pairMatcher.group(0));
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
                return new RouteResult(sampled, ascent, descent, distance);
            }

        } catch (Exception e) {
            System.err.println("Failed to parse GeoJSON response: " + e.getMessage());
            e.printStackTrace();
        }

        return new RouteResult(points, ascent, descent, distance);
    }


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
        conn.setConnectTimeout(30000);  // 30 second connect timeout
        conn.setReadTimeout(30000);     // 30 second read timeout

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

    StringBuilder buildJsonRequestForRoundTrip(GeoPosition startPoint, int distanceMeters, int points, Integer seed) {
        StringBuilder jsonBody = new StringBuilder();
        jsonBody.append("{\"coordinates\":[[");
        jsonBody.append(startPoint.getLongitude()).append(",").append(startPoint.getLatitude());
        jsonBody.append("]],");
        jsonBody.append("\"elevation\":true,");
        jsonBody.append("\"options\":{\"round_trip\":{");
        jsonBody.append("\"length\":").append(distanceMeters).append(",");
        jsonBody.append("\"points\":").append(points);
        if (seed != null) {
            jsonBody.append(",\"seed\":").append(seed);
        }
        jsonBody.append("}},");
        jsonBody.append("\"format\":\"geojson\"");
        jsonBody.append("}");

        return jsonBody;
    }

    StringBuilder buildJasonRequestForRouteWithProfile(List<GeoPosition> waypoints){
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
        jsonBody.append("\"elevation\":true,");  // Request elevation data
        jsonBody.append("\"format\":\"geojson\"");
        jsonBody.append("}");

        return jsonBody;
    }

    void checkIfResponseHasError(String response) {
        if (response.contains("\"error\"")) {
            System.err.println("API returned error response");
            System.err.println("Error response: " + response.substring(0, Math.min(1000, response.length())));
        }
    }

    int convertKMtoMeters(double km) {
        return (int)(km * 1000);
    }

    int makeSureAtLeast3Points(int points) {
        return Math.max(points, 3);
    }

}