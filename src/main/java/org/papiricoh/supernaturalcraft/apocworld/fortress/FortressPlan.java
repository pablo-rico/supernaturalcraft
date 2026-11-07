package org.papiricoh.supernaturalcraft.apocworld.fortress;

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
 * Michael's fortress in the Apocalypse World (v0.41), a pure blueprint for the buildkit: a dark citadel of black basalt with gold
 * seams on a raised plinth, seen from far off across the burned land. A square curtain wall with four tall corner towers (gold
 * crowns burning on top), a gatehouse to the north over the approach road, an open court where the sky can reach (the fight's
 * arena), and at its south end the general's throne on a dais under the keep, whose black spire rises far above the walls. No
 * block entities (the court is the arena). Contract: {@link FortressAnchors}; {@code FortressPlanQualityTest} holds it to the
 * kit's bar.
 */
public final class FortressPlan {

    static final int SEED = 41_300;
    /** The curtain wall (its inner and outer faces), its top; the corner towers' half-width and top; the keep and its spire. */
    static final int WALL_IN = 27, WALL_OUT = 28, WALL_TOP = 11, TOWER_HALF = 3, TOWER_TOP = 18;
    static final int KEEP_X = 10, KEEP_Z0 = 21, KEEP_Z1 = 34, KEEP_TOP = 26, SPIRE_X = 3, SPIRE_Z0 = 25, SPIRE_Z1 = 31, SPIRE_TOP = 40;
    /** The dais (inclusive) and the throne on it. */
    static final int DAIS_X = 7, DAIS_Z0 = 13, DAIS_Z1 = 20;

    public static final String CITADEL = "supernaturalcraft:citadel_stone", SCORCHED = "supernaturalcraft:scorched_stone",
            RUBBLE = "supernaturalcraft:rubble";

    static final Brush WALL = Palette.mix(SEED + 1, CITADEL, 8, "minecraft:polished_blackstone_bricks", 3,
            "minecraft:cracked_polished_blackstone_bricks", 2, "minecraft:blackstone", 1);
    static final Brush FLOOR = Palette.mix(SEED + 2, "minecraft:polished_blackstone_bricks", 6, CITADEL, 3, "minecraft:polished_basalt[axis=y]", 2,
            "minecraft:cracked_polished_blackstone_bricks", 2, "minecraft:polished_blackstone", 1);
    static final Brush ROAD = Palette.mix(SEED + 3, "minecraft:polished_blackstone_bricks", 5, "minecraft:cracked_polished_blackstone_bricks", 3,
            SCORCHED, 2, RUBBLE, 1);
    static final Brush FOOTING = Palette.mix(SEED + 4, SCORCHED, 4, "minecraft:blackstone", 3, "minecraft:basalt[axis=y]", 2);
    static final Brush APRON = Palette.patches2(SEED + 5, 3, SCORCHED, 6, RUBBLE, 2, "minecraft:coarse_dirt", 2, "minecraft:gravel", 1);

    private FortressPlan() {
    }

    public static int[] rotate(int x, int z, int q) {
        return switch (Math.floorMod(q, 4)) {
            case 1 -> new int[]{-z, x};
            case 2 -> new int[]{-x, -z};
            case 3 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    public static LayoutPlan plan(int q) {
        return canvas(q).toPlan();
    }

    public static Canvas canvas(int q) {
        Canvas north = drawn();
        Canvas c = new Canvas("au_fortress");
        int r = Math.max(Math.max(-FortressAnchors.MIN_X, FortressAnchors.MAX_X), Math.max(-FortressAnchors.MIN_Z, FortressAnchors.MAX_Z)) + 4;
        c.zone("fortress", new Box(-r, FortressAnchors.BOTTOM_Y, -r, r, FortressAnchors.TOP_Y, r));
        c.inZone("fortress", () -> c.stamp(north, 0, 0, 0, Math.floorMod(q, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    static Canvas drawn() {
        Canvas c = new Canvas("au_fortress_north");
        ground(c);
        walls(c);
        towers(c);
        gatehouse(c);
        court(c);
        keep(c);
        points(c);
        return c;
    }

    // --- the plinth and the ground round it ----------------------------------------------------------------------------------

    private static void ground(Canvas c) {
        int g = FortressAnchors.GROUND_Y;
        for (int x = FortressAnchors.MIN_X; x <= FortressAnchors.MAX_X; x++) {
            for (int z = FortressAnchors.MIN_Z; z <= FortressAnchors.MAX_Z; z++) {
                boolean inside = Math.abs(x) < WALL_IN && Math.abs(z) < WALL_IN;
                boolean road = Math.abs(x) <= 3 && z < -WALL_OUT;
                c.set(x, g, z, inside ? FLOOR.at(x, g, z) : road ? ROAD.at(x, g, z) : APRON.at(x, g, z));
                c.set(x, g - 1, z, FOOTING.at(x, g - 1, z));
            }
        }
        // Lamp posts down the approach road, and rubble heaped at its sides.
        for (int z = FortressAnchors.MIN_Z + 4; z < -WALL_OUT - 3; z += 6) {
            Lighting.lampPost(c, -5, 0, z, 3, "minecraft:polished_blackstone", "minecraft:polished_blackstone_wall", Dir.EAST, false);
            Lighting.lampPost(c, 5, 0, z, 3, "minecraft:polished_blackstone", "minecraft:polished_blackstone_wall", Dir.WEST, false);
        }
        for (int z = FortressAnchors.MIN_Z + 2; z < -WALL_OUT - 1; z++) {
            for (int x : new int[]{-8, 8}) {
                if (Noise.chance(x, 0, z, SEED + 6, 0.35)) c.set(x, 0, z, RUBBLE);
            }
        }
    }

    // --- the curtain wall ----------------------------------------------------------------------------------------------------

    private static boolean onWall(int x, int z) {
        int ax = Math.abs(x), az = Math.abs(z);
        return (ax >= WALL_IN && ax <= WALL_OUT && az <= WALL_OUT) || (az >= WALL_IN && az <= WALL_OUT && ax <= WALL_OUT);
    }

    private static void walls(Canvas c) {
        for (int x = -WALL_OUT; x <= WALL_OUT; x++) {
            for (int z = -WALL_OUT; z <= WALL_OUT; z++) {
                if (!onWall(x, z)) continue;
                for (int y = 0; y <= WALL_TOP; y++) {
                    // A gold seam every fourth course.
                    c.set(x, y, z, y % 4 == 3 && (Math.abs(x) == WALL_OUT || Math.abs(z) == WALL_OUT) ? "minecraft:gilded_blackstone" : WALL.at(x, y, z));
                }
                boolean outer = Math.abs(x) == WALL_OUT || Math.abs(z) == WALL_OUT;
                if (outer && Math.floorMod(x + z, 2) == 0) c.set(x, WALL_TOP + 1, z, "minecraft:polished_blackstone_bricks");
            }
        }
    }

    private static void towers(Canvas c) {
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                int cx = sx * WALL_OUT, cz = sz * WALL_OUT;
                for (int x = cx - TOWER_HALF; x <= cx + TOWER_HALF; x++) {
                    for (int z = cz - TOWER_HALF; z <= cz + TOWER_HALF; z++) {
                        for (int y = 0; y <= TOWER_TOP; y++) c.set(x, y, z, WALL.at(x, y, z));
                        boolean rim = Math.abs(x - cx) == TOWER_HALF || Math.abs(z - cz) == TOWER_HALF;
                        if (rim && Math.floorMod(x + z, 2) == 0) c.set(x, TOWER_TOP + 1, z, "minecraft:polished_blackstone_bricks");
                    }
                }
                // Arrow slits down each face, and the gold crown burning on top.
                for (int y = 3; y <= TOWER_TOP - 3; y += 5) {
                    c.set(cx, y, cz - TOWER_HALF, "minecraft:gilded_blackstone");
                    c.set(cx, y, cz + TOWER_HALF, "minecraft:gilded_blackstone");
                    c.set(cx - TOWER_HALF, y, cz, "minecraft:gilded_blackstone");
                    c.set(cx + TOWER_HALF, y, cz, "minecraft:gilded_blackstone");
                }
                c.set(cx, TOWER_TOP + 1, cz, "minecraft:gold_block");
                c.set(cx, TOWER_TOP + 2, cz, "minecraft:shroomlight");
            }
        }
    }

    private static void gatehouse(Canvas c) {
        // Two square gate towers flanking the opening.
        for (int sx : new int[]{-1, 1}) {
            int cx = sx * 6;
            for (int x = cx - 2; x <= cx + 2; x++) {
                for (int z = -WALL_OUT - 2; z <= -WALL_IN + 1; z++) {
                    for (int y = 0; y <= 15; y++) c.set(x, y, z, WALL.at(x, y, z));
                    if (Math.floorMod(x + z, 2) == 0) c.set(x, 16, z, "minecraft:polished_blackstone_bricks");
                }
            }
            c.set(cx, 16, -WALL_OUT, "minecraft:gold_block");
            c.set(cx, 17, -WALL_OUT, "minecraft:shroomlight");
        }
        // The opening through the wall: three wide each side of the middle, six high, a gold lintel over it.
        for (int x = -3; x <= 3; x++) {
            for (int z = -WALL_OUT; z <= -WALL_IN; z++) {
                for (int y = 0; y <= 5; y++) c.carve(x, y, z);
                c.set(x, 6, z, "minecraft:gold_block");
            }
        }
    }

    // --- the court -----------------------------------------------------------------------------------------------------------

    private static void court(Canvas c) {
        // Six black columns down the court, gold-capped.
        for (int sx : new int[]{-1, 1}) {
            for (int z : new int[]{-14, 0, 14}) {
                int x0 = sx * 18;
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        int x = x0 + sx * dx, zz = z + dz;
                        for (int y = 0; y <= 9; y++) c.set(x, y, zz, y == 0 ? "minecraft:chiseled_polished_blackstone" : CITADEL);
                        c.set(x, 10, zz, "minecraft:gold_block");
                    }
                }
            }
        }
        // Low lights round the court's edge (open sky above: the afternoon lights it anyway).
        for (int z = -22; z <= 22; z += 11) {
            for (int x : new int[]{-24, 24}) {
                c.set(x, 0, z, "minecraft:polished_blackstone");
                c.set(x, 1, z, St.lantern(false));
            }
        }
        // Rubble and scorch where the court's stones have broken.
        for (int x = -25; x <= 25; x++) {
            for (int z = -25; z <= 25; z++) {
                if (Math.abs(x) <= 9 && z >= -26 && z <= 22) continue;
                if (Math.abs(x) >= 11 && Math.abs(x) <= 20) continue;
                if (Noise.chance(x, 0, z, SEED + 7, 0.02)) c.set(x, 0, z, RUBBLE);
            }
        }
        // The dais, its steps, the throne.
        for (int x = -DAIS_X; x <= DAIS_X; x++) {
            for (int z = DAIS_Z0; z <= DAIS_Z1; z++) {
                c.set(x, 0, z, z == DAIS_Z0 ? St.stairs("minecraft:polished_blackstone_brick_stairs", Dir.SOUTH, false)
                        : (Math.abs(x) == DAIS_X || z == DAIS_Z1) ? "minecraft:gilded_blackstone" : CITADEL);
            }
        }
        c.set(0, 1, 19, St.stairs("minecraft:polished_blackstone_stairs", Dir.SOUTH, false));
        c.set(-1, 1, 19, St.slabBottom("minecraft:polished_blackstone_slab"));
        c.set(1, 1, 19, St.slabBottom("minecraft:polished_blackstone_slab"));
        for (int y = 1; y <= 4; y++) c.set(0, y, 20, y == 4 ? "minecraft:gold_block" : CITADEL);
        c.set(-1, 3, 20, "minecraft:gold_block");
        c.set(1, 3, 20, "minecraft:gold_block");
    }

    // --- the keep and its spire ----------------------------------------------------------------------------------------------

    private static void keep(Canvas c) {
        for (int x = -KEEP_X; x <= KEEP_X; x++) {
            for (int z = KEEP_Z0; z <= KEEP_Z1; z++) {
                // A stepped top: the outer ring lower than the middle.
                int ring = Math.min(KEEP_X - Math.abs(x), Math.min(z - KEEP_Z0, KEEP_Z1 - z));
                int top = KEEP_TOP - 4 + Math.min(4, ring);
                for (int y = 0; y <= top; y++) c.set(x, y, z, y % 6 == 5 && ring == 0 ? "minecraft:gilded_blackstone" : WALL.at(x, y, z));
                if (ring == 0 && Math.floorMod(x + z, 2) == 0) c.set(x, top + 1, z, "minecraft:polished_blackstone_bricks");
            }
        }
        for (int x = -SPIRE_X; x <= SPIRE_X; x++) {
            for (int z = SPIRE_Z0; z <= SPIRE_Z1; z++) {
                int ring = Math.min(SPIRE_X - Math.abs(x), Math.min(z - SPIRE_Z0, SPIRE_Z1 - z));
                int top = SPIRE_TOP - 6 + 3 * ring;
                for (int y = KEEP_TOP + 1; y <= top; y++) c.set(x, y, z, CITADEL);
            }
        }
        int mz = (SPIRE_Z0 + SPIRE_Z1) / 2;
        for (int y = SPIRE_TOP + 4; y <= SPIRE_TOP + 5; y++) c.set(0, y, mz, "minecraft:gold_block");
        c.set(0, SPIRE_TOP + 6, mz, "minecraft:shroomlight");
        // The alcove behind the throne, where the rift opens: lit from its sides.
        for (int x = -2; x <= 2; x++) {
            for (int z = KEEP_Z0; z <= KEEP_Z0 + 1; z++) {
                for (int y = 0; y <= 5; y++) c.carve(x, y, z);
            }
        }
        for (int x = -2; x <= 2; x++) c.set(x, -1, KEEP_Z0, CITADEL);
        for (int x = -2; x <= 2; x++) c.set(x, -1, KEEP_Z0 + 1, CITADEL);
        c.set(-3, 3, KEEP_Z0 + 1, "minecraft:shroomlight");
        c.set(3, 3, KEEP_Z0 + 1, "minecraft:shroomlight");
        c.set(0, 6, KEEP_Z0 + 1, "minecraft:shroomlight");
    }

    // --- the points ----------------------------------------------------------------------------------------------------------

    private static void points(Canvas c) {
        c.anchor(FortressAnchors.ENTRY, 0, 0, FortressAnchors.MIN_Z + 6);
        c.anchor(FortressAnchors.THRONE, 0, 1, 17);
        c.anchor(FortressAnchors.ARENA_CENTRE, 0, 0, 0);
        c.anchor(FortressAnchors.RIFT, 0, 0, KEEP_Z0 + 1);
        c.anchor(FortressAnchors.gateMin(FortressAnchors.GATE), -3, 0, -WALL_OUT);
        c.anchor(FortressAnchors.gateMax(FortressAnchors.GATE), 3, 5, -WALL_IN);
        int[][] spawns = {{-13, 0, -10}, {13, 0, -10}, {-13, 0, 6}, {13, 0, 6}};
        for (int n = 0; n < FortressAnchors.MIN_SPAWNS; n++) c.anchor(FortressAnchors.spawn(n), spawns[n][0], spawns[n][1], spawns[n][2]);
    }
}
