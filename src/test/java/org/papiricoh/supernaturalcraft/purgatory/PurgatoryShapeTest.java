package org.papiricoh.supernaturalcraft.purgatory;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.purgatory.worldgen.PurgatoryBiomes;
import org.papiricoh.supernaturalcraft.purgatory.worldgen.PurgatoryShape;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Purgatory's land (v0.39), without a world: rolling forest land whose deep rock is always solid and whose sky is always open,
 * hollows sinking under the water line, cliffs rising in sudden terraces with overhangs on their faces, ravines cutting deep,
 * and the four regions following the same land.
 */
class PurgatoryShapeTest {

    /** The first air above the ground in a column (density changes sign). */
    private static int top(double hills, double cliffs, double ravine, double detail) {
        for (int y = PurgatoryShape.SKY_Y; y > 0; y--) {
            if (PurgatoryShape.density(y, hills, cliffs, ravine, detail) > 0) return y + 1;
        }
        return 0;
    }

    @Test
    void deepRockIsSolidAndTheSkyOpen() {
        Random r = new Random(1);
        for (int i = 0; i < 2000; i++) {
            double h = r.nextDouble() * 2.4 - 1.2, c = r.nextDouble() * 2 - 1, v = r.nextDouble() * 2 - 1, d = r.nextDouble() * 2 - 1;
            assertTrue(PurgatoryShape.density(PurgatoryShape.ROCK_Y, h, c, v, d) > 0, "rock at the bottom");
            assertTrue(PurgatoryShape.density(PurgatoryShape.SKY_Y, h, c, v, d) < 0, "sky at the top");
            int t = top(h, c, v, d);
            assertTrue(t > PurgatoryShape.ROCK_Y && t < PurgatoryShape.SKY_Y, "the ground lies in between: " + t);
        }
    }

    @Test
    void theLandRollsAboveTheWater() {
        int water = PurgatoryDimension.SEA_LEVEL;
        double flat = PurgatoryShape.surface(0, 0, 1);
        assertTrue(flat > water + 10, "ordinary forest stands well above the water: " + flat);
        assertTrue(PurgatoryShape.surface(0.4, 0, 1) - PurgatoryShape.surface(-0.4, 0, 1) >= 8, "the hills roll");
    }

    @Test
    void theHollowsSinkUnderTheWater() {
        double hollow = PurgatoryShape.surface(-1.1, 0, 1);
        assertTrue(hollow < PurgatoryDimension.SEA_LEVEL, "the deepest hollows hold water (the mire): " + hollow);
        assertTrue(PurgatoryShape.surface(PurgatoryShape.HOLLOW + 0.01, 0, 1) > PurgatoryDimension.SEA_LEVEL, "but only the deepest");
    }

    @Test
    void cliffsRiseInSuddenTerraces() {
        double below = PurgatoryShape.ground(0, PurgatoryShape.CLIFF_LOW - 0.05), above = PurgatoryShape.ground(0, PurgatoryShape.CLIFF_LOW + 0.05);
        assertTrue(above - below >= PurgatoryShape.CLIFF_STEP - 1, "a step of a cliff: " + (above - below));
        double high = PurgatoryShape.ground(0, PurgatoryShape.CLIFF_HIGH + 0.05);
        assertTrue(high - above >= PurgatoryShape.CLIFF_STEP - 1, "and a second terrace above it");
        // On a cliff's face the detail moves the ground in and out (overhangs); on flat land it barely does.
        double face = PurgatoryShape.CLIFF_LOW;
        int y = (int) Math.round(PurgatoryShape.ground(0, face));
        double in = PurgatoryShape.density(y, 0, face, 1, 1), out = PurgatoryShape.density(y, 0, face, 1, -1);
        assertTrue(in - out > 0.5, "the face has overhangs");
        double flatIn = PurgatoryShape.density(70, 0, -0.5, 1, 1), flatOut = PurgatoryShape.density(70, 0, -0.5, 1, -1);
        assertEquals(flatIn, flatOut, 1e-9, "flat ground is not roughened");
    }

    @Test
    void ravinesCutDeep() {
        double plain = PurgatoryShape.surface(0.2, 0, 1), gorge = PurgatoryShape.surface(0.2, 0, 0);
        assertTrue(plain - gorge >= PurgatoryShape.RAVINE_DEPTH - 1, "a gorge: " + (plain - gorge));
        assertEquals(plain, PurgatoryShape.surface(0.2, 0, PurgatoryShape.RAVINE_WIDTH), 1e-9, "and nothing outside it");
        // Its walls are steep: half the width in, it is still most of the way down.
        assertTrue(PurgatoryShape.gorge(PurgatoryShape.RAVINE_WIDTH / 2) > PurgatoryShape.RAVINE_DEPTH * 0.6);
    }

    @Test
    void theRegionsFollowTheLand() {
        assertEquals(PurgatoryBiomes.Kind.MIRE, PurgatoryBiomes.pick(-0.8, 0, 0, 0));
        assertEquals(PurgatoryBiomes.Kind.CRAGS, PurgatoryBiomes.pick(0, 0.7, 0, 0));
        assertEquals(PurgatoryBiomes.Kind.CRAGS, PurgatoryBiomes.pick(0.9, 0, 0, 0));
        assertEquals(PurgatoryBiomes.Kind.MOTHERS_WOOD, PurgatoryBiomes.pick(0, 0, 0.5f, 0.3f));
        assertEquals(PurgatoryBiomes.Kind.ENDLESS_FOREST, PurgatoryBiomes.pick(0, 0, 0, 0));
        // Every region turns up over a spread of climates and land.
        Random r = new Random(7);
        Set<PurgatoryBiomes.Kind> seen = EnumSet.noneOf(PurgatoryBiomes.Kind.class);
        int forest = 0, n = 4000;
        for (int i = 0; i < n; i++) {
            PurgatoryBiomes.Kind k = PurgatoryBiomes.pick(r.nextGaussian() * 0.45, r.nextGaussian() * 0.35, (float) (r.nextGaussian() * 0.35),
                    (float) (r.nextGaussian() * 0.35));
            seen.add(k);
            if (k == PurgatoryBiomes.Kind.ENDLESS_FOREST) forest++;
        }
        assertEquals(EnumSet.allOf(PurgatoryBiomes.Kind.class), seen);
        assertTrue(forest > n / 3, "the forest is endless: " + forest + "/" + n);
        // The mire's ground is the land's hollows: it starts a little before the land sinks under the hollow line.
        assertTrue(PurgatoryBiomes.MIRE_BELOW > PurgatoryShape.HOLLOW);
    }
}
