package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;

import static org.junit.jupiter.api.Assertions.*;

class ChainSeatLayoutsTest {
    @Test
    void sitesAreStableDistantAndOpposite() {
        for (long seed : new long[]{0, 1, -1, 42, Long.MIN_VALUE, Long.MAX_VALUE}) {
            var demon = SeatSite.of(seed, ChainFaction.DEMON);
            var angel = SeatSite.of(seed, ChainFaction.ANGEL);
            assertEquals(demon, SeatSite.of(seed, ChainFaction.DEMON));
            for (var site : new SeatSite.Site[]{demon, angel}) {
                double radius = Math.hypot(site.x(), site.z());
                assertTrue(radius >= SeatSite.MIN_DISTANCE && radius <= SeatSite.MAX_DISTANCE);
            }
            assertTrue(Math.hypot(demon.x() - angel.x(), demon.z() - angel.z()) >= 6000);
        }
        assertNotEquals(SeatSite.of(1, ChainFaction.DEMON), SeatSite.of(2, ChainFaction.DEMON));
    }
}
