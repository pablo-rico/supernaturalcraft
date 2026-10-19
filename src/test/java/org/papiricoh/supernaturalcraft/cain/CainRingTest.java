package org.papiricoh.supernaturalcraft.cain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where Cain's farm may stand (v0.21 P4): 24 distinct candidate chunks on the 1,800-4,000 ring, farther out than Rowena's
 * and drawn with a salt of its own, the same for the same seed, each passing the placement's quick test; a turn for the
 * farmhouse; the datapack's ring is this one.
 */
class CainRingTest {

    private static long key(int[] k) {
        return ((long) k[0] << 32) ^ (k[1] & 0xffffffffL);
    }

    @Test
    void everyCandidateLiesOnHisRingAndPassesThePlacementsQuickTest() {
        Random seeds = new Random(4242);
        for (int s = 0; s < 1000; s++) {
            long seed = s < 3 ? new long[]{0, -1, Long.MIN_VALUE}[s] : seeds.nextLong();
            int[][] c = CainRing.chunks(seed);
            assertEquals(RowenaRing.CANDIDATES, c.length);
            Set<Long> distinct = new HashSet<>();
            for (int[] k : c) {
                double d = Math.hypot(RowenaRing.middle(k[0]), RowenaRing.middle(k[1]));
                assertTrue(d >= CainRing.MIN && d <= CainRing.MAX, "seed " + seed + ": candidate at " + d + " blocks");
                assertTrue(distinct.add(key(k)), "seed " + seed + ": two candidates share a chunk");
                // CainPlacement rejects chunks outside [(MIN >> 4) - 2, (MAX >> 4) + 2] before looking them up.
                long d2 = (long) k[0] * k[0] + (long) k[1] * k[1];
                long lo = (CainRing.MIN >> 4) - 2, hi = (CainRing.MAX >> 4) + 2;
                assertTrue(d2 >= lo * lo && d2 <= hi * hi, "seed " + seed + ": the placement's quick test would skip " + k[0] + "," + k[1]);
            }
            int r = CainRing.rotation(seed);
            assertTrue(r >= 0 && r < 4, "a quarter turn: " + r);
        }
    }

    @Test
    void theSameSeedGivesTheSameFarmAndNotRowenasCandidates() {
        Random seeds = new Random(99);
        for (int s = 0; s < 500; s++) {
            long seed = seeds.nextLong();
            int[][] a = CainRing.chunks(seed), b = CainRing.chunks(seed);
            for (int k = 0; k < a.length; k++) assertArrayEquals(a[k], b[k]);
            assertEquals(CainRing.rotation(seed), CainRing.rotation(seed));
            // His own salt: not Rowena's list of candidates.
            int[][] hers = RowenaRing.chunks(seed);
            int same = 0;
            for (int k = 0; k < a.length; k++) if (a[k][0] == hers[k][0] && a[k][1] == hers[k][1]) same++;
            assertTrue(same < 3, "seed " + seed + ": Cain's candidates follow Rowena's");
        }
        assertFalse(CainRing.chunks(1)[0][0] == CainRing.chunks(2)[0][0] && CainRing.chunks(1)[0][1] == CainRing.chunks(2)[0][1]);
        assertTrue(CainRing.MIN > RowenaRing.MIN && CainRing.MAX > RowenaRing.MAX, "his ring lies farther out than hers");
    }

    @Test
    void theDatapackPlacesTheFarmOnThisRing() throws IOException {
        Path set = Path.of("src/generated/resources/data/supernaturalcraft/worldgen/structure_set/cain_farm.json");
        assertTrue(Files.exists(set), "no generated structure set: run runData");
        JsonObject p = JsonParser.parseString(Files.readString(set)).getAsJsonObject().getAsJsonObject("placement");
        assertEquals("supernaturalcraft:cain", p.get("type").getAsString());
        assertEquals(CainRing.MIN, p.get("min_distance").getAsInt());
        assertEquals(CainRing.MAX, p.get("max_distance").getAsInt());
    }
}
