package org.papiricoh.supernaturalcraft.hell.cage;

import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.St;

import java.util.ArrayList;
import java.util.List;

/** The Cage's palette as block states, and the drawing helpers its plan shares (pure). */
final class CageKit {

    private CageKit() {
    }

    static final String NS = "supernaturalcraft:";
    static final String BARS_ID = NS + "cage_bars", POST_ID = NS + "cage_post", CHAIN_ID = NS + "cage_chain";
    static final String FRAME = NS + "cage_frame", SEAL = NS + "cage_seal", BEDROCK = NS + "abyssal_bedrock";
    static final String FLAG = NS + "abyssal_flagstone", BRICKS = NS + "abyssal_bricks", RITUAL = NS + "cage_ritual_stone";
    static final String BRAZIER = NS + "hellfire_brazier", FLAG_STAIRS = NS + "abyssal_flagstone_stairs", FLAG_SLAB = NS + "abyssal_flagstone_slab";
    static final String PILLAR_ID = NS + "enochian_pillar", CAPITAL_ID = NS + "enochian_capital", LANTERN_ID = NS + "seal_lantern";
    static final String ANCHOR_ID = NS + "chain_anchor", WATCHER_ID = NS + "fallen_watcher";

    static final String PB_BRICKS = "minecraft:polished_blackstone_bricks", CRACKED = "minecraft:cracked_polished_blackstone_bricks";
    static final String CHISELED = "minecraft:chiseled_polished_blackstone", POLISHED = "minecraft:polished_blackstone";
    static final String BLACKSTONE = "minecraft:blackstone", GILDED = "minecraft:gilded_blackstone", CRYING = "minecraft:crying_obsidian";

    /** Every mod block the plan writes (for the id checks). */
    static final String[] MOD_IDS = {BARS_ID, POST_ID, CHAIN_ID, FRAME, SEAL, BEDROCK, FLAG, BRICKS, RITUAL, BRAZIER, FLAG_STAIRS,
            FLAG_SLAB, PILLAR_ID, CAPITAL_ID, LANTERN_ID, ANCHOR_ID, WATCHER_ID};

    static {
        // How the kit should see the Cage's blocks: their shapes and light, so joins, support and the light pass come out right.
        Kinds.declareLike(BARS_ID, "minecraft:iron_bars");
        Kinds.declareLike(POST_ID, "minecraft:iron_bars");
        Kinds.declareLike(CHAIN_ID, "minecraft:chain");
        Kinds.declareLike(LANTERN_ID, "minecraft:lantern");
        Kinds.declareLike(FLAG_STAIRS, "minecraft:deepslate_tile_stairs");
        Kinds.declareLike(FLAG_SLAB, "minecraft:deepslate_tile_slab");
        Kinds.declareLike(BRAZIER, "minecraft:cauldron");
        Kinds.declareLike(ANCHOR_ID, "minecraft:lightning_rod");
        Kinds.declareLike(WATCHER_ID, "minecraft:stonecutter");
        Kinds.declareLight(LANTERN_ID, 13);
        Kinds.declareLight(BRAZIER, 15);
        Kinds.declareLight(SEAL, 7);
        Kinds.declareLight(RITUAL, 5);
        Kinds.declareLight(PILLAR_ID, 4);
        Kinds.declareLight(CAPITAL_ID, 4);
    }

    /** Makes sure the declarations above have run (the plan calls it before drawing). */
    static void init() {
    }

    // --- states -----------------------------------------------------------------------------------------------------------------

    static String bars() {
        return St.of(BARS_ID, "east", "false", "north", "false", "south", "false", "west", "false", "waterlogged", "false");
    }

    static String post() {
        return St.of(POST_ID, "east", "false", "north", "false", "south", "false", "west", "false", "waterlogged", "false");
    }

    static String chain(String axis) {
        return St.of(CHAIN_ID, "axis", axis, "waterlogged", "false");
    }

    static String pillar() {
        return St.of(PILLAR_ID, "axis", "y");
    }

    static String capital(boolean top) {
        return St.of(CAPITAL_ID, "half", top ? "top" : "bottom");
    }

    static String lantern(boolean hanging) {
        return St.of(LANTERN_ID, "hanging", String.valueOf(hanging), "waterlogged", "false");
    }

    static String anchor(Dir facing) {
        return St.of(ANCHOR_ID, "facing", facing.id());
    }

    static String watcher(Dir facing) {
        return St.of(WATCHER_ID, "facing", facing.id());
    }

    static String stairs(Dir facing, boolean top) {
        return St.stairs(FLAG_STAIRS, facing, top);
    }

    static String slab(String type) {
        return St.slab(FLAG_SLAB, type);
    }

    /** Abyssal masonry: bricks with bedrock and blackstone worked in (every 5 × 5 patch shows at least two of them). */
    static String masonry(int x, int y, int z) {
        int k = Math.floorMod(x + z + 2 * y, 3);
        double h = Noise.hash01(x, y, z, 77);
        if (k == 0) return h < 0.72 ? BRICKS : h < 0.9 ? PB_BRICKS : BEDROCK;
        if (k == 1) return h < 0.45 ? BRICKS : h < 0.8 ? BEDROCK : PB_BRICKS;
        return h < 0.5 ? BEDROCK : h < 0.75 ? BRICKS : h < 0.93 ? CRACKED : h < 0.97 ? GILDED : CRYING;
    }

    /** Darker, rougher masonry for what hangs under the island. */
    static String underside(int x, int y, int z) {
        double h = Noise.hash01(x, y, z, 31);
        int k = Math.floorMod(x - z + y, 3);
        if (k == 0) return h < 0.6 ? BEDROCK : h < 0.85 ? BLACKSTONE : "minecraft:basalt[axis=y]";
        return h < 0.45 ? BRICKS : h < 0.72 ? BEDROCK : h < 0.9 ? BLACKSTONE : h < 0.97 ? "minecraft:polished_basalt[axis=y]" : CRYING;
    }

    static double hash(int x, int y, int z, int salt) {
        return Noise.hash01(x, y, z, salt);
    }

    // --- spokes ------------------------------------------------------------------------------------------------------------

    /**
     * Draws in a spoke's own frame: {@code t} outward from the centre, {@code w} to the right walking outward, states written as
     * if the spoke ran east (+X) and turned with it.
     */
    record Spoke(Canvas c, int q) {

        int x(int t, int w) {
            return CageLayout.spoke(q, t, w)[0];
        }

        int z(int t, int w) {
            return CageLayout.spoke(q, t, w)[1];
        }

        void set(int t, int y, int w, String state) {
            c.set(x(t, w), y, z(t, w), St.rotate(state, q));
        }

        boolean setIfAir(int t, int y, int w, String state) {
            return c.setIfAir(x(t, w), y, z(t, w), St.rotate(state, q));
        }

        void carve(int t, int y, int w) {
            c.carve(x(t, w), y, z(t, w));
        }

        String get(int t, int y, int w) {
            return c.get(x(t, w), y, z(t, w));
        }

        boolean rock(int t, int y, int w) {
            return CageLayout.rock(x(t, w), y, z(t, w));
        }

        /** How deep in the wall's rock the cell lies (≤ 0 in the open shaft). */
        double depth(int t, int y, int w) {
            return org.papiricoh.supernaturalcraft.hell.worldgen.PitShape.at(x(t, w), y, z(t, w));
        }

        double hash(int t, int y, int w, int salt) {
            return Noise.hash01(x(t, w), y, z(t, w), salt);
        }
    }

    // --- chains -----------------------------------------------------------------------------------------------------------

    /**
     * A chain through the given points, face to face (one axis per step, so the links touch), each link turned along the way
     * it runs. Only into air: a chain never cuts through what is already there, the Pit's rock included.
     */
    static void chain(Canvas c, List<int[]> points) {
        List<int[]> cells = new ArrayList<>();
        for (int i = 0; i + 1 < points.size(); i++) {
            int[] a = points.get(i), b = points.get(i + 1);
            List<int[]> seg = path(a, b);
            if (!cells.isEmpty()) seg.removeFirst();
            cells.addAll(seg);
        }
        for (int i = 0; i < cells.size(); i++) {
            int[] p = cells.get(i), n = i + 1 < cells.size() ? cells.get(i + 1) : null, prev = i > 0 ? cells.get(i - 1) : null;
            int[] d = n != null ? new int[]{n[0] - p[0], n[1] - p[1], n[2] - p[2]} : new int[]{p[0] - prev[0], p[1] - prev[1], p[2] - prev[2]};
            String axis = d[1] != 0 ? "y" : d[0] != 0 ? "x" : "z";
            if (c.isAir(p[0], p[1], p[2]) && !CageLayout.rock(p[0], p[1], p[2])) c.set(p[0], p[1], p[2], chain(axis));
        }
    }

    static void chain(Canvas c, int[] a, int[] b) {
        chain(c, List.of(a, b));
    }

    /** A vertical chain hanging from under (x, top + 1, z) down to {@code bottom}, a lantern at its end when asked. */
    static void hang(Canvas c, int x, int top, int bottom, int z, boolean lantern) {
        for (int y = top; y >= bottom; y--) {
            if (!c.isAir(x, y, z)) return;
            c.set(x, y, z, y == bottom && lantern ? lantern(true) : chain("y"));
        }
    }

    /** The cells from a to b (inclusive), face-connected, keeping close to the straight line. */
    static List<int[]> path(int[] a, int[] b) {
        List<int[]> out = new ArrayList<>();
        int[] p = a.clone();
        out.add(p.clone());
        int[] d = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        int[] done = new int[3];
        int total = Math.abs(d[0]) + Math.abs(d[1]) + Math.abs(d[2]);
        for (int s = 0; s < total; s++) {
            // Step along the axis that lags furthest behind its share of the run.
            int best = -1;
            double lag = Double.MAX_VALUE;
            for (int k = 0; k < 3; k++) {
                if (done[k] >= Math.abs(d[k])) continue;
                double next = (done[k] + 0.5) / Math.abs(d[k]);
                if (next < lag) {
                    lag = next;
                    best = k;
                }
            }
            done[best]++;
            p[best] += Integer.signum(d[best]);
            out.add(p.clone());
        }
        return out;
    }

    /** A small hanging gibbet: a 3 × 3 iron cage of {@code height} bars, its roof at {@code top}, a lantern under its floor. */
    static void gibbet(Canvas c, int x, int top, int z, int height) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean edge = dx != 0 || dz != 0;
                c.set(x + dx, top, z + dz, dx == 0 && dz == 0 ? FRAME : edge && dx != 0 && dz != 0 ? FRAME : slab("top"));
                for (int y = top - height; y < top; y++) {
                    if (edge) c.set(x + dx, y, z + dz, dx != 0 && dz != 0 ? post() : bars());
                }
                c.set(x + dx, top - height - 1, z + dz, dx != 0 && dz != 0 ? FRAME : slab("bottom"));
            }
        }
        c.set(x, top - height - 2, z, lantern(true));
    }
}
