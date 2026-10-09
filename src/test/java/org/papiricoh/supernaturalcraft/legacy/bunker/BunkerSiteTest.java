package org.papiricoh.supernaturalcraft.legacy.bunker;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where the bunker may stand: on its ring, deterministic per seed, distinct candidates, quiet ground first. */
class BunkerSiteTest {

    @Test
    void candidatesLieOnTheRing() {
        for (long seed : new long[]{0, 1, -42, 123456789L, Long.MAX_VALUE}) {
            int[][] c = BunkerSite.chunks(seed, BunkerSite.MIN, BunkerSite.MAX);
            assertEquals(BunkerSite.CANDIDATES, c.length);
            Set<String> distinct = new HashSet<>();
            for (int[] k : c) {
                double d = Math.hypot(BunkerSite.middle(k[0]), BunkerSite.middle(k[1]));
                assertTrue(d >= BunkerSite.MIN && d <= BunkerSite.MAX, "candidate at " + d + " blocks");
                distinct.add(k[0] + "," + k[1]);
            }
            assertEquals(BunkerSite.CANDIDATES, distinct.size(), "distinct candidates");
        }
    }

    @Test
    void sameSeedSameCandidatesOtherRingOtherPlaces() {
        assertArrayEquals(BunkerSite.chunks(77, 3000, 5000), BunkerSite.chunks(77, 3000, 5000));
        int[][] near = BunkerSite.chunks(77, 1000, 1500);
        for (int[] k : near) assertTrue(Math.hypot(BunkerSite.middle(k[0]), BunkerSite.middle(k[1])) <= 1500, "a configured ring is obeyed");
    }

    @Test
    void notTheAuthorsCandidates() {
        int[][] mine = BunkerSite.chunks(5, 8000, 12000);
        int[][] his = org.papiricoh.supernaturalcraft.author.CabinSite.chunks(5, 8000, 12000);
        int same = 0;
        for (int[] k : mine) if (org.papiricoh.supernaturalcraft.author.CabinSite.indexOf(his, k[0], k[1]) >= 0) same++;
        assertTrue(same <= 1, "its own salt: " + same + " shared candidates");
    }

    @Test
    void theFirstQuietCandidateElseTheLast() {
        assertEquals(3, BunkerSite.choose(k -> k == 3 || k == 9));
        assertEquals(BunkerSite.CANDIDATES - 1, BunkerSite.choose(k -> false));
    }

    @Test
    void quietGroundIsDryAndGentle() {
        assertTrue(BunkerSite.quiet(new int[]{70, 71, 72, 70, 74}, new int[]{70, 71, 72, 70, 74}, 63));
        assertFalse(BunkerSite.quiet(new int[]{70, 71, 72, 70, 80}, new int[]{70, 71, 72, 70, 80}, 63), "too steep");
        assertFalse(BunkerSite.quiet(new int[]{70, 71, 72, 70, 70}, new int[]{70, 71, 60, 70, 70}, 63), "water");
        assertFalse(BunkerSite.quiet(new int[]{63, 63, 63, 63, 63}, new int[]{63, 63, 63, 63, 63}, 63), "sea level");
        assertTrue(BunkerSite.validRing(3000, 5000));
        assertFalse(BunkerSite.validRing(5000, 3000));
    }

    @Test
    void theHutTurnsWithTheSeed() {
        Set<Integer> turns = new HashSet<>();
        for (long s = 0; s < 40; s++) {
            int t = BunkerSite.rotation(s);
            assertTrue(t >= 0 && t < 4);
            turns.add(t);
        }
        assertEquals(4, turns.size(), "every way round happens");
    }
}
