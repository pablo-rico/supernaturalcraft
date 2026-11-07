package org.papiricoh.supernaturalcraft.apocworld;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.apocworld.worldgen.AUShape;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Apocalypse World's land (v0.41), without a world: rolling scorched land whose deep rock is always solid and whose sky is
 * always open, ridges rising high along their spines, craters sinking with a rim round them, dry riverbeds cut flat and shallow,
 * and none of it ever near the sea line (there is no water).
 */
class AUShapeTest {

    private static int top(double hills, double ridges, double pits, double river, double detail) {
        for (int y = AUShape.SKY_Y; y > 0; y--) {
            if (AUShape.density(y, hills, ridges, pits, river, detail) > 0) return y + 1;
        }
        return 0;
    }

    @Test
    void deepRockIsSolidAndTheSkyOpen() {
        Random r = new Random(13);
        for (int i = 0; i < 3000; i++) {
            double h = r.nextDouble() * 2.4 - 1.2, g = r.nextDouble() * 2 - 1, p = r.nextDouble() * 2 - 1, v = r.nextDouble() * 2 - 1, d = r.nextDouble() * 2 - 1;
            assertTrue(AUShape.density(AUShape.ROCK_Y, h, g, p, v, d) > 0, "rock at the bottom");
            assertTrue(AUShape.density(AUShape.SKY_Y, h, g, p, v, d) < 0, "sky at the top");
            int t = top(h, g, p, v, d);
            assertTrue(t > AUShape.ROCK_Y && t < AUShape.SKY_Y, "the ground lies in between: " + t);
            assertTrue(t > ApocWorldDimension.SEA_LEVEL + 20, "never near the (empty) sea line: " + t);
        }
    }

    @Test
    void theLandRollsGently() {
        double flat = AUShape.surface(0, 1, -1, 1);
        assertEquals(AUShape.BASE, flat, 1e-9, "open land sits at the middle height");
        double rise = AUShape.surface(0.5, 1, -1, 1) - AUShape.surface(-0.5, 1, -1, 1);
        assertTrue(rise >= 6 && rise <= 12, "gentle hills, not mountains: " + rise);
    }

    @Test
    void ridgesRiseAlongTheirSpines() {
        double spine = AUShape.surface(0, 0, -1, 1), off = AUShape.surface(0, AUShape.RIDGE_WIDTH + 0.01, -1, 1);
        assertTrue(spine - off >= AUShape.RIDGE_HEIGHT - 1, "a ridge stands high over the land: " + (spine - off));
        double flank = AUShape.ridgeness(AUShape.RIDGE_WIDTH / 2);
        assertTrue(flank > 0 && flank < 1, "its flanks slope");
        // Craters and riverbeds never cut a ridge's spine.
        assertEquals(spine, AUShape.surface(0, 0, 1, 0), 1e-9);
    }

    @Test
    void cratersSinkWithARimRoundThem() {
        double open = AUShape.surface(0, 1, -1, 1);
        double heart = AUShape.surface(0, 1, AUShape.CRATER_FULL + 0.1, 1);
        assertTrue(open - heart >= AUShape.CRATER_DEPTH - 0.5, "a crater's heart is deep: " + (open - heart));
        double rim = AUShape.surface(0, 1, (AUShape.RIM_START + AUShape.CRATER_EDGE) / 2, 1);
        assertTrue(rim > open + 1, "a rim of thrown-up earth just outside it: " + (rim - open));
    }

    @Test
    void riverbedsAreShallowAndFlatBottomed() {
        double open = AUShape.surface(0, 1, -1, 1);
        double middle = AUShape.surface(0, 1, -1, 0), nearBank = AUShape.surface(0, 1, -1, AUShape.RIVER_WIDTH * 0.4);
        assertEquals(AUShape.RIVER_DEPTH, open - middle, 1e-6, "a riverbed is cut this deep");
        assertEquals(middle, nearBank, 0.5, "and flat across most of its width");
        assertEquals(open, AUShape.surface(0, 1, -1, AUShape.RIVER_WIDTH), 1e-9, "nothing outside it");
    }
}
