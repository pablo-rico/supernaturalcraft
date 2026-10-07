package org.papiricoh.supernaturalcraft.hell;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.hell.cage.CageLayout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CageLayoutTest {

    @Test
    void everythingStaysWithinReach() {
        for (BlockPos a : CageLayout.anchors()) assertTrue(Math.max(Math.abs(a.getX()), Math.abs(a.getZ())) + 2 <= CageLayout.REACH);
        assertTrue(CageLayout.GATE_END + CageLayout.GATE_HALF + 1 <= CageLayout.REACH);
        assertTrue(CageLayout.REACH <= 7 * 16, "the structure's pieces must lie within 8 chunks of its start");
    }

    @Test
    void theIrisOpensRingByRing() {
        assertEquals(49, CageLayout.irisCells().size());
        for (BlockPos p : CageLayout.irisCells()) {
            assertFalse(CageLayout.irisOpen(p.getX(), p.getZ(), 0), "a closed iris has no gap");
            assertTrue(CageLayout.irisOpen(p.getX(), p.getZ(), CageLayout.IRIS_STEPS), "an open iris has no plate");
            assertTrue(CageLayout.inOctagon(p.getX(), p.getZ()));
        }
        int open1 = 0, open2 = 0;
        for (BlockPos p : CageLayout.irisCells()) {
            if (CageLayout.irisOpen(p.getX(), p.getZ(), 1)) open1++;
            if (CageLayout.irisOpen(p.getX(), p.getZ(), 2)) open2++;
        }
        assertEquals(9, open1);
        assertEquals(25, open2);
    }

    @Test
    void sixtySixSeals() {
        int n = CageLayout.sealCount();
        assertTrue(n >= 60 && n <= 72, "the ring holds " + n + " seals");
    }

    @Test
    void theOctagonHasItsEightCorners() {
        assertEquals(8, CageLayout.vertices().size());
        for (int[] v : CageLayout.vertices()) {
            assertTrue(CageLayout.onOctagonEdge(v[0], v[1]), "corner not on the edge: " + v[0] + "," + v[1]);
        }
    }

    @Test
    void thePlinthsAreWhereTheCircleWantsThem() throws IOException {
        JsonArray rows = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/data/supernaturalcraft/supernaturalcraft/ritual_pattern/cage_circle.json")))
                .getAsJsonObject().getAsJsonArray("pattern");
        int ax = -1, az = -1;
        Set<BlockPos> stones = new HashSet<>();
        for (int z = 0; z < rows.size(); z++) {
            String row = rows.get(z).getAsString();
            for (int x = 0; x < row.length(); x++) {
                if (row.charAt(x) == 'A') {
                    ax = x;
                    az = z;
                }
            }
        }
        for (int z = 0; z < rows.size(); z++) {
            String row = rows.get(z).getAsString();
            for (int x = 0; x < row.length(); x++) {
                if (row.charAt(x) == 'R') stones.add(new BlockPos(x - ax, CageLayout.ISLAND_Y + 1, z - az));
            }
        }
        assertEquals(new HashSet<>(CageLayout.plinths()), stones);
    }

    @Test
    void theIslandIsASlabOverANarrowCore() {
        assertEquals(CageLayout.ISLAND_RADIUS, CageLayout.islandRadius(CageLayout.ISLAND_Y, 0), 1e-9);
        assertTrue(CageLayout.islandRadius(CageLayout.ISLAND_Y - CageLayout.SLAB, 0) <= CageLayout.CORE_RADIUS);
        assertTrue(CageLayout.islandRadius(CageLayout.ISLAND_BOTTOM, 0) < 1);
    }
}
