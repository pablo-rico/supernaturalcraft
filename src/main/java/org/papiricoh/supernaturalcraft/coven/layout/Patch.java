package org.papiricoh.supernaturalcraft.coven.layout;

import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Ground;
import org.papiricoh.supernaturalcraft.buildkit.Noise;

/**
 * The levelled ground the coven's builds stand on (pure, shared by the three layouts): a patch with a ragged edge whose surface
 * block is at {@code y = -1}, a little soil and a stone floor under it, and the air above it cleared (higher in the middle than
 * at the rim, so trees around are trimmed back gently). Also the cheap "is this spot flat and dry" test of the structures.
 */
final class Patch {

    private Patch() {
    }

    /**
     * Draws the patch round the origin: mean radius {@code radius} (the edge wanders by about a sixth either way), surface from
     * {@code surface}, soil from {@code soil} two rows down to a stone row at {@code y = -4}, air cleared {@code clear} rows up
     * in the middle (three at the rim). @return the patch's ground (top at {@code y = -1})
     */
    static Ground draw(Canvas c, int seed, int radius, Brush surface, Brush soil, int clear) {
        Ground g = new Ground();
        int reach = radius + radius / 4 + 1;
        for (int x = -reach; x <= reach; x++) {
            for (int z = -reach; z <= reach; z++) {
                double r = radius * (0.85 + 0.3 * Noise.value2(x, z, 6, seed));
                double d = Math.hypot(x, z);
                if (d > r) continue;
                c.set(x, -1, z, surface.at(x, -1, z));
                c.set(x, -2, z, soil.at(x, -2, z));
                c.set(x, -3, z, soil.at(x, -3, z));
                c.set(x, -4, z, "minecraft:stone");
                int up = d < r - 3 ? clear : 3;
                for (int y = 0; y < up; y++) c.carve(x, y, z);
                g.set(x, z, -1, 1 - d / r);
            }
        }
        return g;
    }

    /** Ground under a box (surface at {@code y = -1}) where the patch's ragged edge left none. */
    static void pad(Canvas c, Ground g, org.papiricoh.supernaturalcraft.buildkit.Box b, Brush surface) {
        for (int x = b.x0(); x <= b.x1(); x++) {
            for (int z = b.z0(); z <= b.z1(); z++) {
                if (g.has(x, z)) continue;
                c.set(x, -1, z, surface.at(x, -1, z));
                c.set(x, -2, z, "minecraft:dirt");
                c.set(x, -3, z, "minecraft:dirt");
                c.set(x, -4, z, "minecraft:stone");
                for (int y = 0; y < 4; y++) c.carve(x, y, z);
                g.set(x, z, -1, 0.1);
            }
        }
    }

    /** Whether the sampled ground (tops and ocean floors at the centre and the corners) is dry land within 3 blocks of level. */
    static boolean flatAndDry(int[] tops, int[] floors, int seaLevel) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < tops.length; i++) {
            if (floors[i] < tops[i] || tops[i] <= seaLevel) return false;
            min = Math.min(min, tops[i]);
            max = Math.max(max, tops[i]);
        }
        return max - min <= 3;
    }
}
