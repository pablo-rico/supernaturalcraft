package org.papiricoh.supernaturalcraft.hell;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.hell.worldgen.HellBiomes;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HellBiomesTest {

    @Test
    void thePitSurroundsTheOrigin() {
        Random r = new Random(1);
        for (int i = 0; i < 500; i++) {
            float t = r.nextFloat() * 2 - 1, h = r.nextFloat() * 2 - 1;
            assertEquals(HellBiomes.Kind.THE_PIT, HellBiomes.pick(r.nextInt(161) - 80, r.nextInt(161) - 80, t, h));
        }
    }

    @Test
    void thePitIsOnlyAtTheOrigin() {
        Random r = new Random(2);
        for (int i = 0; i < 500; i++) {
            int x = 300 + r.nextInt(5000), z = -2000 + r.nextInt(4000);
            assertFalse(HellBiomes.pick(x, z, r.nextFloat() * 2 - 1, r.nextFloat() * 2 - 1) == HellBiomes.Kind.THE_PIT);
        }
    }

    @Test
    void theClimateSharesOutTheOtherThree() {
        Set<HellBiomes.Kind> seen = EnumSet.noneOf(HellBiomes.Kind.class);
        for (float t = -0.8f; t <= 0.8f; t += 0.1f) {
            for (float h = -0.8f; h <= 0.8f; h += 0.1f) seen.add(HellBiomes.pick(1000, 1000, t, h));
        }
        assertEquals(EnumSet.of(HellBiomes.Kind.THE_RACK, HellBiomes.Kind.ASH_WASTES, HellBiomes.Kind.CROWLEYS_CORRIDORS), seen);
    }
}
