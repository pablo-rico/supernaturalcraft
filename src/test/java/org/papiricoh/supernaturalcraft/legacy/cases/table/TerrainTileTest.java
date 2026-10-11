package org.papiricoh.supernaturalcraft.legacy.cases.table;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The war table's terrain tiles: keys that round-trip (negative tiles too), samples that read back, tiles near the table only. */
class TerrainTileTest {

    @Test
    void keysRoundTrip() {
        for (int lod : new int[]{TerrainTile.NEAR, TerrainTile.FAR}) {
            for (int tx : new int[]{0, 1, -1, 57000, -57000}) {
                for (int tz : new int[]{0, 3, -2, 12345, -54321}) {
                    long k = TerrainTile.key(lod, tx, tz);
                    assertEquals(lod, TerrainTile.lod(k));
                    assertEquals(tx, TerrainTile.tx(k));
                    assertEquals(tz, TerrainTile.tz(k));
                }
            }
        }
    }

    @Test
    void tilesSitOnTheWorldGrid() {
        assertEquals(512, TerrainTile.span(TerrainTile.NEAR));
        assertEquals(2048, TerrainTile.span(TerrainTile.FAR));
        assertEquals(0, TerrainTile.tileOf(TerrainTile.NEAR, 0));
        assertEquals(0, TerrainTile.tileOf(TerrainTile.NEAR, 511));
        assertEquals(-1, TerrainTile.tileOf(TerrainTile.NEAR, -1));
        assertEquals(-512, TerrainTile.origin(TerrainTile.NEAR, -1));
        long[] keys = TerrainTile.covering(TerrainTile.NEAR, -10, -10, 10, 10);
        assertEquals(4, keys.length);
        assertEquals(TerrainTile.key(TerrainTile.NEAR, -1, -1), keys[0]);
        assertEquals(TerrainTile.key(TerrainTile.NEAR, 0, 0), keys[3]);
    }

    @Test
    void samplesReadBack() {
        byte[] d = new byte[TerrainTile.BYTES];
        TerrainTile.set(d, 0, 0, -40, TerrainTile.surface(TerrainKind.DEEP_OCEAN, true, false));
        TerrainTile.set(d, 64, 64, 300, TerrainTile.surface(TerrainKind.SNOW, false, true));
        assertEquals(-40, TerrainTile.height(d, 0, 0));
        assertEquals(300, TerrainTile.height(d, 64, 64));
        int s = TerrainTile.surface(d, 0, 0);
        assertEquals(TerrainKind.DEEP_OCEAN, TerrainKind.of(s & TerrainTile.KIND_MASK));
        assertTrue((s & TerrainTile.WATER) != 0);
        assertEquals(0, s & TerrainTile.REAL);
        int t = TerrainTile.surface(d, 64, 64);
        assertEquals(TerrainKind.SNOW, TerrainKind.of(t & TerrainTile.KIND_MASK));
        assertTrue((t & TerrainTile.REAL) != 0);
        assertTrue(TerrainKind.values().length <= TerrainTile.KIND_MASK + 1, "kinds fit their bits");
    }

    @Test
    void onlyTilesNearTheTableAreMapped() {
        assertTrue(TerrainTile.inRange(TerrainTile.key(TerrainTile.NEAR, 0, 0), 100, 100));
        assertTrue(TerrainTile.inRange(TerrainTile.key(TerrainTile.FAR, -1, -1), 100, 100));
        assertFalse(TerrainTile.inRange(TerrainTile.key(TerrainTile.NEAR, 10, 0), 100, 100), "5000 blocks away");
        assertFalse(TerrainTile.inRange(TerrainTile.key(7, 0, 0), 100, 100), "no such level of detail");
        assertEquals(TerrainTile.NEAR, TerrainTile.pickLod(2));
        assertEquals(TerrainTile.FAR, TerrainTile.pickLod(10));
    }
}
