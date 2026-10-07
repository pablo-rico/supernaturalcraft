package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithHeadstones;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithHeadstones.Spot;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithSummoning;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LilithHeadstonesTest {

    @Test
    void aLooseRingAroundTheAltar() {
        for (long seed = 0; seed < 50; seed++) {
            List<Spot> spots = LilithHeadstones.ring(6, seed, seed * 0.37);
            assertTrue(spots.size() >= 5, "too many clashes for seed " + seed + ": " + spots.size());
            for (Spot s : spots) {
                double d = Math.hypot(s.dx(), s.dz());
                assertTrue(Math.abs(d - LilithHeadstones.RADIUS) <= 1.6, "off the ring: " + s);
                assertTrue(d > LilithSummoning.DISTANCE + 1.5, "too close to where she takes shape: " + s);
            }
            for (int i = 0; i < spots.size(); i++) {
                for (int j = i + 1; j < spots.size(); j++) {
                    Spot p = spots.get(i), q = spots.get(j);
                    assertTrue(Math.hypot(p.dx() - q.dx(), p.dz() - q.dz()) >= LilithHeadstones.MIN_APART, "headstones crowd: " + p + " " + q);
                }
            }
        }
    }
}
