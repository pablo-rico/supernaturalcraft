package org.papiricoh.supernaturalcraft.apocworld.camp;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

/**
 * Michael's outposts in the Apocalypse World (v0.41), pure blueprints for the buildkit:
 * <ul>
 *   <li>{@link Kind#CAMP}: a prisoner camp. A square of iron fence on the scorched ground, its gate to the north, two
 *   watchtowers at opposite corners, two plank huts, and three iron cages in a row, each with a prisoner inside and a heavy lock on
 *   its front ({@code supernaturalcraft:camp_lock}).</li>
 *   <li>{@link Kind#CHECKPOINT}: a road block. A dirt road through a gap in rubble breastworks, a fence barrier across it with
 *   its gate open, and a roofed watch post at its side.</li>
 * </ul>
 * No block entities. Contract: {@link CampAnchors}; {@code CampPlanQualityTest} holds both to the kit's bar.
 */
public final class CampPlan {

    public enum Kind { CAMP, CHECKPOINT }

    static final int SEED = 41_200;
    static final int FENCE = 12;
    /** The cages' middles (x) and the row they stand in (their front, middle and back z). */
    static final int[] CAGE_X = {-4, 0, 4};
    static final int CAGE_FRONT = 2, CAGE_MID = 3, CAGE_BACK = 4;

    public static final String SOIL = "supernaturalcraft:scorched_soil", ASH = "supernaturalcraft:au_ash_block",
            RUBBLE = "supernaturalcraft:rubble", PLANKS = "supernaturalcraft:burned_planks", LOCK = "supernaturalcraft:camp_lock",
            STONE = "supernaturalcraft:scorched_stone", BRICKS = "supernaturalcraft:ruined_bricks";

    static final Brush YARD = Palette.patches2(SEED + 1, 3, SOIL, 6, ASH, 3, "minecraft:coarse_dirt", 2, "minecraft:gravel", 1);
    static final Brush UNDER = Palette.mix(SEED + 2, STONE, 3, "minecraft:dirt", 2);
    static final Brush ROAD = Palette.patches2(SEED + 3, 2, "minecraft:dirt_path", 4, "minecraft:coarse_dirt", 3, "minecraft:gravel", 2);
    static final Brush BREASTWORK = Palette.mix(SEED + 4, RUBBLE, 5, BRICKS, 2, STONE, 1);

    private CampPlan() {
    }

    /** Which outpost a structure's seed stands up (a little over half are camps). */
    public static Kind kindOf(long seed) {
        return Math.floorMod(seed ^ (seed >>> 31), 20L) < 11 ? Kind.CAMP : Kind.CHECKPOINT;
    }

    /** Which way it faces (0-3), from the seed. */
    public static int turnOf(long seed) {
        return (int) Math.floorMod(seed ^ (seed >>> 23), 4L);
    }

    public static int[] rotate(int x, int z, int q) {
        return switch (Math.floorMod(q, 4)) {
            case 1 -> new int[]{-z, x};
            case 2 -> new int[]{-x, -z};
            case 3 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    public static LayoutPlan plan(Kind kind, int q) {
        return canvas(kind, q).toPlan();
    }

    public static Canvas canvas(Kind kind, int q) {
        Canvas north = kind == Kind.CAMP ? camp() : checkpoint();
        Canvas c = new Canvas("au_" + kind.name().toLowerCase(java.util.Locale.ROOT));
        int r = Math.max(Math.max(-CampAnchors.MIN_X, CampAnchors.MAX_X), Math.max(-CampAnchors.MIN_Z, CampAnchors.MAX_Z)) + 2;
        c.zone("outpost", new Box(-r, CampAnchors.BOTTOM_Y, -r, r, CampAnchors.TOP_Y, r));
        c.inZone("outpost", () -> c.stamp(north, 0, 0, 0, Math.floorMod(q, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    private static void ground(Canvas c, boolean road) {
        int g = CampAnchors.GROUND_Y;
        for (int x = CampAnchors.MIN_X; x <= CampAnchors.MAX_X; x++) {
            for (int z = CampAnchors.MIN_Z; z <= CampAnchors.MAX_Z; z++) {
                c.set(x, g, z, road && Math.abs(x) <= 2 ? ROAD.at(x, g, z) : YARD.at(x, g, z));
                c.set(x, g - 1, z, UNDER.at(x, g - 1, z));
            }
        }
    }

    // --- the prisoner camp ---------------------------------------------------------------------------------------------------

    static Canvas camp() {
        Canvas c = new Canvas("au_camp_north");
        ground(c, false);
        // The fence, gapped at the gate, with log posts either side of it and at the corners.
        for (int i = -FENCE; i <= FENCE; i++) {
            for (int[] p : new int[][]{{i, -FENCE}, {i, FENCE}, {-FENCE, i}, {FENCE, i}}) {
                if (p[1] == -FENCE && Math.abs(p[0]) <= 2) continue;
                boolean post = Math.abs(p[0]) == FENCE && Math.abs(p[1]) == FENCE || p[1] == -FENCE && Math.abs(p[0]) == 3;
                for (int y = 0; y <= 1; y++) c.set(p[0], y, p[1], post ? St.axis("minecraft:dark_oak_log", "y") : St.bars());
                if (post) c.set(p[0], 2, p[1], St.axis("minecraft:dark_oak_log", "y"));
            }
        }
        tower(c, -10, -10);
        tower(c, 10, 10);
        hut(c, 4, -9, 8, -6, Dir.SOUTH);
        hut(c, -9, 6, -5, 9, Dir.NORTH);
        for (int n = 0; n < CampAnchors.CAGES; n++) cage(c, CAGE_X[n]);
        // Lights by the cages and the gate.
        Lighting.lampPost(c, -7, 0, -2, 3, "minecraft:polished_blackstone", "minecraft:spruce_fence", Dir.EAST, false);
        Lighting.lampPost(c, 7, 0, -2, 3, "minecraft:polished_blackstone", "minecraft:spruce_fence", Dir.WEST, false);
        Lighting.lampPost(c, -5, 0, -11, 2, "minecraft:polished_blackstone", "minecraft:spruce_fence", null, false);
        // Supply crates by the first hut, ash and rubble about the yard.
        for (int[] k : new int[][]{{9, -4}, {10, -4}, {9, -3}}) c.set(k[0], 0, k[1], St.axis("minecraft:stripped_spruce_wood", "y"));
        c.set(9, 1, -4, St.axis("minecraft:stripped_spruce_wood", "y"));
        for (int x = -FENCE + 1; x < FENCE; x++) {
            for (int z = -FENCE + 1; z < FENCE; z++) {
                if (c.has(x, 0, z) || Math.abs(x) <= 2 || z >= CAGE_FRONT - 1 && z <= CAGE_BACK + 1) continue;
                if (Noise.chance(x, 0, z, SEED + 9, 0.025)) c.set(x, 0, z, RUBBLE);
            }
        }
        c.anchor(CampAnchors.ENTRY, 0, 0, CampAnchors.MIN_Z + 1);
        int[][] guards = {{-3, 0, -14}, {3, 0, -14}, {-8, 0, 1}, {8, 0, 2}, {0, 0, 7}};
        for (int n = 0; n < guards.length; n++) c.anchor(CampAnchors.guard(n), guards[n][0], guards[n][1], guards[n][2]);
        for (int n = 0; n < CampAnchors.CAGES; n++) {
            c.anchor(CampAnchors.prisoner(n), CAGE_X[n], 0, CAGE_MID);
            c.anchor(CampAnchors.lock(n), CAGE_X[n], 0, CAGE_FRONT);
        }
        return c;
    }

    /** A watchtower: four log legs, a plank platform at 5 with a rail, a lantern up there. */
    private static void tower(Canvas c, int cx, int cz) {
        for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
                for (int y = 0; y <= 5; y++) c.set(cx + dx, y, cz + dz, St.axis("minecraft:dark_oak_log", "y"));
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
                    c.set(cx + dx, 6, cz + dz, St.fence("minecraft:dark_oak_fence"));
                    continue;
                }
                c.set(cx + dx, 5, cz + dz, PLANKS);
                if (dx != 0 || dz != 0) c.set(cx + dx, 6, cz + dz, St.fence("minecraft:dark_oak_fence"));
            }
        }
        c.set(cx, 6, cz, St.lantern(false));
    }

    /** A plank hut (inclusive corners), its door gap in the {@code door} wall's middle, a flat roof, a lantern inside. */
    private static void hut(Canvas c, int x0, int z0, int x1, int z1, Dir door) {
        int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                for (int y = 0; y <= 2; y++) {
                    if (!edge) c.carve(x, y, z);
                    else c.set(x, y, z, (x == x0 || x == x1) && (z == z0 || z == z1) ? St.axis("supernaturalcraft:burned_log", "y") : PLANKS);
                }
                c.set(x, 3, z, PLANKS);
            }
        }
        int dx = door == Dir.SOUTH || door == Dir.NORTH ? mx : door == Dir.EAST ? x1 : x0;
        int dz = door == Dir.SOUTH ? z1 : door == Dir.NORTH ? z0 : mz;
        for (int y = 0; y <= 1; y++) c.carve(dx, y, dz);
        c.set(door == Dir.SOUTH ? x0 + 1 : x1 - 1, 0, door == Dir.SOUTH ? z0 + 1 : z1 - 1, St.lantern(false));
        // A window either side.
        c.set(x0, 1, mz, St.pane("minecraft:black_stained_glass_pane"));
        c.set(x1, 1, mz, St.pane("minecraft:black_stained_glass_pane"));
    }

    /** An iron cage in the row, its lock in the front (north) face, a slab roof. */
    private static void cage(Canvas c, int cx) {
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = CAGE_FRONT; z <= CAGE_BACK; z++) {
                boolean edge = x != cx || z != CAGE_MID;
                for (int y = 0; y <= 1; y++) {
                    if (!edge) c.carve(x, y, z);
                    else c.set(x, y, z, x == cx && z == CAGE_FRONT && y == 0 ? LOCK : St.bars());
                }
                c.set(x, 2, z, St.slabBottom("minecraft:dark_oak_slab"));
            }
        }
    }

    // --- the checkpoint ------------------------------------------------------------------------------------------------------

    static Canvas checkpoint() {
        Canvas c = new Canvas("au_checkpoint_north");
        ground(c, true);
        // Breastworks of rubble either side of the road, an L each.
        for (int s : new int[]{-1, 1}) {
            for (int x = 4; x <= 8; x++) {
                c.set(s * x, 0, -4, BREASTWORK.at(s * x, 0, -4));
                if (x % 2 == 0) c.set(s * x, 1, -4, BREASTWORK.at(s * x, 1, -4));
            }
            for (int z = -3; z <= 4; z++) c.set(s * 8, 0, z, BREASTWORK.at(s * 8, 0, z));
        }
        // The barrier across the road, its gate standing open.
        for (int x = -3; x <= 3; x++) {
            if (x == 0) c.set(x, 0, 0, St.gate("minecraft:spruce_fence_gate", Dir.NORTH, true, false));
            else c.set(x, 0, 0, Math.abs(x) == 3 ? St.axis("minecraft:dark_oak_log", "y") : St.fence("minecraft:spruce_fence"));
        }
        c.set(-3, 1, 0, St.axis("minecraft:dark_oak_log", "y"));
        c.set(3, 1, 0, St.axis("minecraft:dark_oak_log", "y"));
        // The watch post: four log legs, a slab roof, a lantern hanging under it.
        for (int dx : new int[]{10, 12}) {
            for (int dz : new int[]{-2, 0}) {
                for (int y = 0; y <= 2; y++) c.set(dx, y, dz, St.axis("supernaturalcraft:burned_log", "y"));
            }
        }
        for (int x = 10; x <= 12; x++) for (int z = -2; z <= 0; z++) c.set(x, 3, z, St.slabBottom("minecraft:dark_oak_slab"));
        c.set(11, 2, -1, St.lantern(true));
        Lighting.lampPost(c, -4, 0, -7, 3, "minecraft:polished_blackstone", "minecraft:spruce_fence", Dir.EAST, false);
        Lighting.lampPost(c, 4, 0, 6, 3, "minecraft:polished_blackstone", "minecraft:spruce_fence", Dir.WEST, false);
        // A burned-out car pushed off the road.
        for (int z = 6; z <= 9; z++) {
            for (int x = -7; x <= -6; x++) c.set(x, 0, z, "supernaturalcraft:wrecked_metal");
            if (z == 7 || z == 8) c.set(-7, 1, z, "supernaturalcraft:wrecked_metal");
        }
        c.anchor(CampAnchors.ENTRY, 0, 0, CampAnchors.MIN_Z + 2);
        int[][] guards = {{-3, 0, -2}, {3, 0, -2}, {11, 0, -1}, {0, 0, 4}};
        for (int n = 0; n < guards.length; n++) c.anchor(CampAnchors.guard(n), guards[n][0], guards[n][1], guards[n][2]);
        return c;
    }
}
