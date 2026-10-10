package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoiseTest {

    @Test
    void sameArgumentsSameValue() {
        for (int i = 0; i < 100; i++) {
            assertEquals(Noise.hash01(i, -i, i * 7, 3), Noise.hash01(i, -i, i * 7, 3));
            assertEquals(Noise.fbm2(i * 0.7, i * 1.3, 9, 3, 5), Noise.fbm2(i * 0.7, i * 1.3, 9, 3, 5));
        }
        assertNotEquals(Noise.hash01(1, 2, 3, 4), Noise.hash01(1, 2, 3, 5), "the seed changes the value");
    }

    @Test
    void valuesStayInRange() {
        for (int x = -50; x < 50; x++) {
            for (int z = -50; z < 50; z++) {
                double[] v = {Noise.hash01(x, 0, z, 1), Noise.value2(x, z, 5, 2), Noise.value3(x, z, x - z, 4, 3),
                        Noise.fbm2(x, z, 8, 4, 4), Noise.fbm3(x, 1, z, 8, 3, 5), Noise.ridged2(x, z, 6, 2, 6),
                        Noise.patch2(x, z, 4, 7), Noise.patch3(x, 2, z, 4, 8)};
                for (double d : v) assertTrue(d >= 0 && d < 1, "out of [0,1): " + d);
                int j = Noise.jitter(x, z, 9, 2);
                assertTrue(j >= -2 && j <= 2);
                int p = Noise.pick(x, 0, z, 10, 5);
                assertTrue(p >= 0 && p < 5);
            }
        }
    }

    @Test
    void whiteNoiseIsEven() {
        int[] bins = new int[10];
        for (int i = 0; i < 100_000; i++) bins[(int) (Noise.hash01(i % 317, i / 317, i % 13, 42) * 10)]++;
        for (int b : bins) assertTrue(Math.abs(b - 10_000) < 600, "uneven bin " + b);
    }

    @Test
    void patchesCoverTheirShareInClumps() {
        int under = 0, n = 0, sameAsNeighbour = 0;
        for (int x = 0; x < 200; x++) {
            for (int z = 0; z < 200; z++) {
                boolean in = Noise.patch2(x, z, 5, 77) < 0.3;
                if (in) under++;
                if (in == (Noise.patch2(x + 1, z, 5, 77) < 0.3)) sameAsNeighbour++;
                n++;
            }
        }
        double share = under / (double) n;
        assertTrue(Math.abs(share - 0.3) < 0.06, "a 0.3 threshold covers about 30 %: " + share);
        assertTrue(sameAsNeighbour / (double) n > 0.8, "clumped, not salt-and-pepper: " + sameAsNeighbour / (double) n);
    }

    @Test
    void smoothNoiseIsSmooth() {
        for (int x = 0; x < 100; x++) {
            double a = Noise.value2(x, 3, 8, 1), b = Noise.value2(x + 1, 3, 8, 1);
            assertTrue(Math.abs(a - b) < 0.3, "steps of " + Math.abs(a - b) + " between neighbours");
        }
    }
}
