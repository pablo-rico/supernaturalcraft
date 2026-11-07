package org.papiricoh.supernaturalcraft.apocworld;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.apocworld.worldgen.AUBiomes;
import org.papiricoh.supernaturalcraft.apocworld.worldgen.AUShape;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Apocalypse World's five regions (v0.41) follow its land: ridges on the spines, riverbeds in the channels, craters in the pits. */
class AUBiomesTest {

    @Test
    void eachRegionWhereItsLandIs() {
        assertEquals(AUBiomes.Kind.BROKEN_RIDGES, AUBiomes.pick(0, -1, 1, 0));
        assertEquals(AUBiomes.Kind.DRY_RIVERBED, AUBiomes.pick(1, -1, 0, 0));
        assertEquals(AUBiomes.Kind.CRATERED_FIELDS, AUBiomes.pick(1, AUShape.CRATER_EDGE, 1, 0));
        assertEquals(AUBiomes.Kind.BURNED_FOREST, AUBiomes.pick(1, -1, 1, 0.5));
        assertEquals(AUBiomes.Kind.ASH_PLAINS, AUBiomes.pick(1, -1, 1, -0.5));
        // A ridge over a crater is still a ridge (the land stands over the pit).
        assertEquals(AUBiomes.Kind.BROKEN_RIDGES, AUBiomes.pick(0, 0.9, 0, 0.5));
    }

    @Test
    void everyRegionTurnsUpAcrossTheNoise() {
        Set<AUBiomes.Kind> seen = EnumSet.noneOf(AUBiomes.Kind.class);
        Random r = new Random(7);
        int plains = 0, n = 20000;
        for (int i = 0; i < n; i++) {
            AUBiomes.Kind k = AUBiomes.pick(r.nextGaussian() * 0.35, r.nextGaussian() * 0.35, r.nextGaussian() * 0.35, r.nextGaussian() * 0.35);
            seen.add(k);
            if (k == AUBiomes.Kind.ASH_PLAINS) plains++;
        }
        assertEquals(EnumSet.allOf(AUBiomes.Kind.class), seen);
        assertTrue(plains > n / 6, "the ash plains are the common ground: " + plains);
    }
}
