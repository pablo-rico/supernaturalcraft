package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TreesTest {

    interface Grow {
        void grow(Canvas c, int x, int y, int z, int size, int seed);
    }

    static final Map<String, Grow> SPECIES = Map.of(
            "oak", Trees::oak,
            "cherry", Trees::cherry,
            "birch", Trees::birch,
            "spruce", Trees::spruce,
            "dead", (c, x, y, z, s, seed) -> Trees.deadOak(c, x, y, z, s, false, seed),
            "struck", (c, x, y, z, s, seed) -> Trees.deadOak(c, x, y, z, s, true, seed),
            "willow", Trees::willow,
            "bush", (c, x, y, z, s, seed) -> Trees.bush(c, x, y, z, "minecraft:azalea_leaves", 1 + s * 0.3, seed));

    /** A tree on a lawn: grass at y = -1, the trunk from (0, 0, 0). */
    static Canvas grown(Grow g, int size, int seed) {
        Canvas c = new Canvas("tree");
        c.fill(new Box(-14, -3, -14, 14, -1, 14), "minecraft:grass_block");
        g.grow(c, 0, 0, 0, size, seed);
        return c.finish();
    }

    @Test
    void leavesArePersistentAndNearWood() {
        for (Map.Entry<String, Grow> e : SPECIES.entrySet()) {
            for (int size = 0; size <= 3; size++) {
                for (int seed = 1; seed <= 4; seed++) {
                    Canvas c = grown(e.getValue(), size, seed * 31);
                    List<long[]> logs = new ArrayList<>();
                    for (Map.Entry<Long, String> m : c.map().entrySet()) if (Kinds.log(m.getValue())) logs.add(new long[]{m.getKey()});
                    List<String> bad = new ArrayList<>();
                    for (Map.Entry<Long, String> m : c.map().entrySet()) {
                        String s = m.getValue();
                        if (!Kinds.leaves(s)) continue;
                        if (!"true".equals(St.get(s, "persistent"))) bad.add("not persistent " + s);
                        int x = Canvas.kx(m.getKey()), y = Canvas.ky(m.getKey()), z = Canvas.kz(m.getKey());
                        int best = Integer.MAX_VALUE;
                        for (long[] l : logs) {
                            long k = l[0];
                            best = Math.min(best, Math.max(Math.abs(Canvas.kx(k) - x), Math.max(Math.abs(Canvas.ky(k) - y), Math.abs(Canvas.kz(k) - z))));
                        }
                        if (best > 6) bad.add(x + "," + y + "," + z + " is " + best + " from wood");
                    }
                    assertTrue(bad.isEmpty(), e.getKey() + " size " + size + " seed " + seed + ": " + bad.subList(0, Math.min(8, bad.size())));
                }
            }
        }
    }

    @Test
    void treesStandOnTheirGround() {
        for (Map.Entry<String, Grow> e : SPECIES.entrySet()) {
            for (int size = 0; size <= 3; size++) {
                Canvas c = grown(e.getValue(), size, 77 + size);
                assertTrue(Kinds.log(c.get(0, 0, 0)), e.getKey() + ": a trunk at its foot, got " + c.get(0, 0, 0));
                String under = c.get(0, -1, 0);
                assertTrue(Kinds.soil(under), e.getKey() + " stands on " + under);
                // No wood hangs in the air below the trunk's foot and nothing overwrote the lawn but roots.
                for (Map.Entry<Long, String> m : c.map().entrySet()) {
                    int y = Canvas.ky(m.getKey());
                    if (y < -2) assertTrue(St.path(m.getValue()).equals("grass_block"), "something dug below the soil: " + m.getValue());
                }
                LayoutQuality.assertSupported(c);
            }
        }
    }

    @Test
    void treesAreNotLollipops() {
        // A storybook oak has a crown wider than it is tall-ish and several branches out of the trunk.
        Canvas c = grown(Trees::oak, 2, 9);
        int minX = 0, maxX = 0, branchLogs = 0;
        for (Map.Entry<Long, String> m : c.map().entrySet()) {
            int x = Canvas.kx(m.getKey());
            if (Kinds.leaves(m.getValue())) {
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
            }
            String axis = St.get(m.getValue(), "axis");
            if (Kinds.log(m.getValue()) && axis != null && !axis.equals("y") && Canvas.ky(m.getKey()) > 1) branchLogs++;
        }
        assertTrue(maxX - minX >= 9, "a broad crown: " + (maxX - minX + 1));
        assertTrue(branchLogs >= 4, "sideways branch logs: " + branchLogs);
    }
}
