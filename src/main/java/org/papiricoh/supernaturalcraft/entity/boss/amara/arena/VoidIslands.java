package org.papiricoh.supernaturalcraft.entity.boss.amara.arena;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase V, the Void (v0.42): what is left of the town when Creation comes undone under her (pure, tested in JUnit:
 * {@code VoidIslandsTest}). The ground crumbles ring by ring from the edge inward ({@link #ring}) down to {@link #DEPTH} below
 * the floor; only {@link #ISLANDS} stay up, a heart island under the rite that never sinks and a ring of small ones the hunters
 * leap between, which she sinks one step at a time ({@link #SINK_STEPS} at most, then they go). Every coordinate is relative to
 * the rite's column, {@code dy} from the floor, like {@link FogTownLayout}.
 */
public final class VoidIslands {

    /** How deep the ground goes when it crumbles (the arena carries a faller back from below {@code ArenaRescue.FALL_DEPTH}). */
    public static final int DEPTH = 9;
    /** How many steps a small island may sink before it crumbles too. */
    public static final int SINK_STEPS = 5;
    /** How thick an island is (it sinks by moving its bottom down and its top away). */
    public static final int THICKNESS = 3;

    /** An island: its centre and radius; island 0 is the heart, which never sinks. */
    public record Island(int cx, int cz, int radius) {
        public boolean covers(int x, int z) {
            int dx = x - cx, dz = z - cz;
            return dx * dx + dz * dz <= radius * radius;
        }
    }

    public static final List<Island> ISLANDS = List.of(new Island(0, 0, 6),
            new Island(13, 0, 3), new Island(7, 11, 3), new Island(-7, 11, 3), new Island(-13, 0, 3), new Island(-7, -11, 3),
            new Island(7, -11, 3), new Island(19, 12, 2), new Island(-19, -12, 2));

    private VoidIslands() {
    }

    /** The island under column ({@code x}, {@code z}), or -1. */
    public static int islandAt(int x, int z) {
        for (int i = 0; i < ISLANDS.size(); i++) if (ISLANDS.get(i).covers(x, z)) return i;
        return -1;
    }

    /** The crumbling ring a column falls with: 0 the outermost (first); columns on an island never fall with the rings. */
    public static int ring(int x, int z, int radius) {
        double r = Math.sqrt(x * x + z * z);
        return Math.max(0, (int) Math.floor((radius - r) / 2.0));
    }

    /** How many rings an arena of {@code radius} crumbles in. */
    public static int rings(int radius) {
        return radius / 2 + 1;
    }

    /** Columns of ring {@code ring} that fall (inside the arena, off every island), as {x, z} pairs. */
    public static List<int[]> falling(int ring, int radius) {
        List<int[]> out = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;
                if (islandAt(x, z) >= 0 || ring(x, z, radius) != ring) continue;
                out.add(new int[]{x, z});
            }
        }
        return out;
    }

    /** The columns of island {@code i}, as {x, z} pairs. */
    public static List<int[]> columns(int i) {
        Island is = ISLANDS.get(i);
        List<int[]> out = new ArrayList<>();
        for (int x = is.cx - is.radius; x <= is.cx + is.radius; x++) {
            for (int z = is.cz - is.radius; z <= is.cz + is.radius; z++) if (is.covers(x, z)) out.add(new int[]{x, z});
        }
        return out;
    }

    /** Whether island {@code i} may sink another step after {@code sunk} steps (the heart never). */
    public static boolean maySink(int i, int sunk) {
        return i > 0 && sunk < SINK_STEPS;
    }
}
