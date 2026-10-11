package org.papiricoh.supernaturalcraft.client.legacy.table;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TerrainKind;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TerrainTile;

import static org.junit.jupiter.api.Assertions.*;

/** The war table's client sums: the map view, the compass and the hologram's paint. */
class WarTableMathTest {

    @Test
    void zoomKeepsThePointUnderTheCursor() {
        MapView v = new MapView(100, 200, 1024);
        v.frame(0, 0, 400, 300);
        double wx = v.worldX(300), wz = v.worldZ(50);
        v.zoomAt(300, 50, 512);
        assertEquals(wx, v.worldX(300), 1e-6);
        assertEquals(wz, v.worldZ(50), 1e-6);
        assertEquals(512, v.width(), 1e-9);
        v.zoomAt(0, 0, 1);
        assertEquals(MapView.MIN_WIDTH, v.width(), 1e-9);
        v.zoomAt(0, 0, 1e9);
        assertEquals(MapView.MAX_WIDTH, v.width(), 1e-9);
    }

    @Test
    void screenAndWorldRoundTrip() {
        MapView v = new MapView(-50, 70, 800);
        v.frame(10, 20, 400, 400);
        assertEquals(-50, v.worldX(210), 1e-6);
        assertEquals(70, v.worldZ(220), 1e-6);
        assertEquals(123, v.worldX(v.screenX(123)), 1e-6);
        assertEquals(-77, v.worldZ(v.screenY(-77)), 1e-6);
    }

    @Test
    void theViewNeverWandersOffTheTable() {
        MapView v = new MapView(0, 0, 400);
        v.frame(0, 0, 400, 400);
        v.pan(-100000, -100000);
        assertTrue(v.centreX() <= TerrainTile.RANGE - 200 + 1e-6);
        assertTrue(v.centreZ() <= TerrainTile.RANGE - 200 + 1e-6);
        v.zoomAt(200, 200, MapView.MAX_WIDTH);
        double[] b = v.bounds();
        assertTrue(b[0] >= -TerrainTile.RANGE - 1e-6 && b[2] <= TerrainTile.RANGE + 1e-6, "zoomed out, the view stays over the table's land");
    }

    @Test
    void compassBearings() {
        assertEquals(0, CompassMath.bearing(0, 0, 0, -10), 1e-9);
        assertEquals(90, CompassMath.bearing(0, 0, 10, 0), 1e-9);
        assertEquals(180, CompassMath.bearing(0, 0, 0, 10), 1e-9);
        assertEquals(270, CompassMath.bearing(0, 0, -10, 0), 1e-9);
        assertEquals("north_east", CompassMath.point(44));
        assertEquals("north", CompassMath.point(359));
        // Yaw 180 faces north; yaw 0 faces south; -90 faces east.
        assertEquals(0, CompassMath.heading(180), 1e-9);
        assertEquals(90, CompassMath.heading(-90), 1e-9);
        assertEquals(0, CompassMath.relative(90, -90), 1e-9);
        assertEquals(90, CompassMath.relative(90, 180), 1e-9);
        assertEquals(-90, CompassMath.relative(270, 180), 1e-9);
        assertEquals(180, CompassMath.relative(180, 180), 1e-9);
    }

    @Test
    void theHologramPaintsLandWaterAndContours() {
        byte[] d = new byte[TerrainTile.BYTES];
        int sea = 63;
        for (int j = 0; j < TerrainTile.SIDE; j++) {
            for (int i = 0; i < TerrainTile.SIDE; i++) {
                boolean water = i < 16;
                int h = water ? 40 : 64 + i * 2;
                TerrainTile.set(d, i, j, h, TerrainTile.surface(water ? TerrainKind.OCEAN : TerrainKind.PLAINS, water, j < 32));
            }
        }
        int scale = 2;
        int[] px = HoloShade.paint(d, 8, sea, scale);
        int n = TerrainTile.SIZE * scale;
        assertEquals(n * n, px.length);
        int water = px[10 * n + 4], land = px[10 * n + 100];
        assertTrue((water & 0xFF) > ((water >> 16) & 0xFF), "water is blue");
        assertTrue(((land >> 16) & 0xFF) > (land & 0xFF), "land is amber");
        assertTrue((land >>> 24) > 0 && (water >>> 24) > 0, "nothing is clear");
        // Below row 64 (sample 32) the land is only foretold: dimmer.
        int seen = px[20 * n + 100], foretold = px[(n - 4) * n + 100];
        assertTrue(HoloShade.brightness(seen) > HoloShade.brightness(foretold), "foretold land is dimmer");
        boolean contour = false;
        for (int x = 40; x < n - 1; x++) contour |= HoloShade.brightness(px[20 * n + x]) > HoloShade.brightness(px[20 * n + x + 1]) + 60;
        assertTrue(contour, "a rising slope crosses contour lines");
    }
}
