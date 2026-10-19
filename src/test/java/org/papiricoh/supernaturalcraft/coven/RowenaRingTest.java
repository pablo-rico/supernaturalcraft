package org.papiricoh.supernaturalcraft.coven;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRing;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where Rowena's house may stand (v0.21 P3): 24 distinct candidate chunks on the 1,200–3,200 ring (well inside the Author's
 * 8,000–12,000), the same for the same seed; the first quiet one wins; a turn for the door; flat dry ground.
 */
class RowenaRingTest {

    @Test
    void everyCandidateLiesOnTheRingAndPassesThePlacementsCheapTest() {
        Random seeds = new Random(42);
        for (int s = 0; s < 1000; s++) {
            long seed = s < 3 ? new long[]{0, -1, Long.MIN_VALUE}[s] : seeds.nextLong();
            int[][] c = RowenaRing.chunks(seed);
            assertEquals(RowenaRing.CANDIDATES, c.length);
            Set<Long> distinct = new HashSet<>();
            for (int[] k : c) {
                double d = Math.hypot(RowenaRing.middle(k[0]), RowenaRing.middle(k[1]));
                assertTrue(d >= RowenaRing.MIN && d <= RowenaRing.MAX, "seed " + seed + ": candidate at " + d + " blocks");
                assertTrue(distinct.add(((long) k[0] << 32) ^ (k[1] & 0xffffffffL)), "seed " + seed + ": two candidates share a chunk");
                // RowenaPlacement rejects chunks outside [(MIN >> 4) - 2, (MAX >> 4) + 2] chunks before looking them up.
                long d2 = (long) k[0] * k[0] + (long) k[1] * k[1];
                long lo = (RowenaRing.MIN >> 4) - 2, hi = (RowenaRing.MAX >> 4) + 2;
                assertTrue(d2 >= lo * lo && d2 <= hi * hi, "seed " + seed + ": the placement's quick test would skip " + k[0] + "," + k[1]);
            }
            int r = RowenaRing.rotation(seed);
            assertTrue(r >= 0 && r < 4, "a quarter turn: " + r);
        }
    }

    @Test
    void theSameSeedGivesTheSameCandidatesAndOthersLookElsewhere() {
        Random seeds = new Random(7);
        for (int s = 0; s < 500; s++) {
            long seed = seeds.nextLong();
            int[][] a = RowenaRing.chunks(seed), b = RowenaRing.chunks(seed);
            for (int k = 0; k < a.length; k++) assertArrayEquals(a[k], b[k]);
            assertEquals(RowenaRing.rotation(seed), RowenaRing.rotation(seed));
        }
        assertFalse(RowenaRing.chunks(1)[0][0] == RowenaRing.chunks(2)[0][0] && RowenaRing.chunks(1)[0][1] == RowenaRing.chunks(2)[0][1]);
    }

    @Test
    void theFirstQuietCandidateWinsElseTheLast() {
        int[][] c = RowenaRing.chunks(123456789L);
        for (int k = 0; k < c.length; k++) assertEquals(k, RowenaRing.indexOf(c, c[k][0], c[k][1]));
        assertEquals(-1, RowenaRing.indexOf(c, 0, 0));
        assertEquals(0, RowenaRing.choose(k -> true));
        assertEquals(7, RowenaRing.choose(k -> k >= 7));
        assertEquals(RowenaRing.CANDIDATES - 1, RowenaRing.choose(k -> false));
        Set<Integer> asked = new HashSet<>();
        RowenaRing.choose(k -> {
            asked.add(k);
            return k == 2;
        });
        assertEquals(Set.of(0, 1, 2), asked, "only candidates before the answer are sampled (each costs height lookups)");
    }

    @Test
    void flatGroundIsDryAndLevel() {
        assertTrue(RowenaRing.flat(new int[]{70, 71, 70, 73, 70}, new int[]{70, 71, 70, 73, 70}, 63));
        assertFalse(RowenaRing.flat(new int[]{70, 71, 70, 74, 70}, new int[]{70, 71, 70, 74, 70}, 63), "too steep");
        assertFalse(RowenaRing.flat(new int[]{64, 64, 64, 64, 64}, new int[]{61, 64, 64, 64, 64}, 63), "water");
        assertFalse(RowenaRing.flat(new int[]{50, 50, 50, 50, 50}, new int[]{50, 50, 50, 50, 50}, 63), "below the sea");
        assertEquals(8, RowenaRing.middle(0));
        assertEquals(-8, RowenaRing.middle(-1));
    }
}
