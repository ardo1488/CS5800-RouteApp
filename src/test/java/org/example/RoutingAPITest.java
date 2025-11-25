package org.example;

import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class RoutingAPITest {

    /* hasApiKey */

    @Test
    public void hasApiKeyReturnsTrueForNonEmptyKeyTest() {
        RoutingAPI api = new RoutingAPI("some-api-key");
        assertTrue(api.hasApiKey());
    }

    @Test
    public void hasApiKeyReturnsFalseForNullOrBlankKeyTest() {
        RoutingAPI apiNull = new RoutingAPI(null);
        assertFalse(apiNull.hasApiKey());

        RoutingAPI apiBlank = new RoutingAPI("   ");
        assertFalse(apiBlank.hasApiKey());
    }

    /* snapToRoads */

    @Test
    public void snapToRoadsReturnsNullWhenNoApiKeyTest() {
        RoutingAPI api = new RoutingAPI(null);
        assertNull(api.snapToRoads(
                java.util.Arrays.asList(new GeoPosition(0.0, 0.0), new GeoPosition(1.0, 1.0))
        ));
    }

    @Test
    public void snapToRoadsReturnsNullWhenWaypointsListTooSmallTest() {
        RoutingAPI api = new RoutingAPI("some-api-key");

        assertNull(api.snapToRoads(null));
        assertNull(api.snapToRoads(Collections.singletonList(new GeoPosition(0.0, 0.0))));
    }
}
