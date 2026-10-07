package org.papiricoh.supernaturalcraft.author;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The cabin's candidate chunks: on the 8,000–12,000 ring, distinct, the same for the same seed. */
class CabinSiteTest {

    @Test
    void everyCandidateLiesOnTheRing() {
        Random seeds = new Random(42);
        for (int s = 0; s < 1000; s++) {
            long seed = s < 3 ? new long[]{0, -1, Long.MIN_VALUE}[s] : seeds.nextLong();
            int[][] c = CabinSite.chunks(seed);
            assertEquals(CabinSite.CANDIDATES, c.length);
            Set<Long> distinct = new HashSet<>();
            for (int[] k : c) {
                double d = Math.hypot(CabinSite.middle(k[0]), CabinSite.middle(k[1]));
                assertTrue(d >= CabinSite.MIN && d <= CabinSite.MAX, "seed " + seed + ": candidate at " + d + " blocks");
                assertTrue(distinct.add(((long) k[0] << 32) ^ (k[1] & 0xffffffffL)), "seed " + seed + ": two candidates share a chunk");
            }
        }
    }

    @Test
    void theSameSeedGivesTheSameCandidates() {
        Random seeds = new Random(7);
        for (int s = 0; s < 1000; s++) {
            long seed = seeds.nextLong();
            int[][] a = CabinSite.chunks(seed), b = CabinSite.chunks(seed);
            for (int k = 0; k < a.length; k++) assertArrayEquals(a[k], b[k]);
        }
    }

    @Test
    void differentSeedsLookElsewhere() {
        assertTrue(CabinSite.chunks(1)[0][0] != CabinSite.chunks(2)[0][0] || CabinSite.chunks(1)[0][1] != CabinSite.chunks(2)[0][1]);
    }

    @Test
    void candidatesAreFoundByTheirChunk() {
        int[][] c = CabinSite.chunks(123456789L);
        for (int k = 0; k < c.length; k++) assertEquals(k, CabinSite.indexOf(c, c[k][0], c[k][1]));
        assertEquals(-1, CabinSite.indexOf(c, 0, 0));
    }

    @Test
    void theFirstQuietCandidateWinsElseTheLast() {
        assertEquals(0, CabinSite.choose(k -> true));
        assertEquals(5, CabinSite.choose(k -> k >= 5));
        assertEquals(CabinSite.CANDIDATES - 1, CabinSite.choose(k -> false));
        Set<Integer> asked = new HashSet<>();
        CabinSite.choose(k -> {
            asked.add(k);
            return k == 3;
        });
        assertEquals(Set.of(0, 1, 2, 3), asked);
    }

    @Test
    void flatGroundIsDryAndLevel() {
        assertTrue(CabinSite.flat(new int[]{70, 71, 70, 72, 70}, new int[]{70, 71, 70, 72, 70}, 63));
        assertTrue(!CabinSite.flat(new int[]{70, 71, 70, 76, 70}, new int[]{70, 71, 70, 76, 70}, 63), "too steep");
        assertTrue(!CabinSite.flat(new int[]{63, 63, 63, 63, 63}, new int[]{60, 63, 63, 63, 63}, 63), "water");
        assertTrue(!CabinSite.flat(new int[]{50, 50, 50, 50, 50}, new int[]{50, 50, 50, 50, 50}, 63), "below the sea");
    }
}
