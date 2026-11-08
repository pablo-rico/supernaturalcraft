package org.papiricoh.supernaturalcraft.hands.reliquary;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.buildkit.Wood;

/**
 * A monastery high in the snow (v0.42), keeper of the Trumpet of Jericho: a walled court on a stone terrace cut into the slope,
 * its gate to the north. A chapel of pale stone under a steep slate roof, pilasters down its sides, its bell tower at the
 * north-west corner with an open belfry and a pyramid cap; the monks' dormitory along the east wall under a lean-to roof;
 * a well, snow on the flags, two old spruces. In the chapel's apse, past the pews, the altar where the Trumpet lies.
 * Plan space: the gate faces -z.
 */
final class MonasteryPlan {

    static final int SEED = 42_500;
    static final int CX0 = -4, CX1 = 4, CZ0 = 1, CZ1 = 14, CTOP = 6;
    static final int WALL = 12, GATE_Z = -13;

    static final String STONE = "supernaturalcraft:monastery_stone";
    static final Brush MASONRY = Palette.patches2(SEED + 1, 2, STONE, 4, "minecraft:stone_bricks", 2, "minecraft:polished_diorite", 2,
            "minecraft:white_terracotta", 1, "minecraft:cracked_stone_bricks", 1);
    static final Brush COURT = Palette.patches2(SEED + 2, 2, "minecraft:cobblestone", 3, "minecraft:stone_bricks", 3, "minecraft:snow_block", 3,
            "minecraft:gravel", 2, "minecraft:andesite", 1);
    static final Brush FLOOR = Palette.mix(SEED + 3, "minecraft:polished_andesite", 3, "minecraft:spruce_planks", 3, STONE, 2,
            "minecraft:stripped_spruce_wood[axis=y]", 1);
    static final Brush BASE = Palette.mix(SEED + 4, "minecraft:cobblestone", 3, "minecraft:stone_bricks", 3, "minecraft:mossy_cobblestone", 1,
            STONE, 2);

    private MonasteryPlan() {
    }

    static Canvas drawn() {
        Canvas c = new Canvas("reliquary_snow_monastery_north");
        terrace(c);
        enclosure(c);
        chapel(c);
        tower(c);
        dormitory(c);
        court(c);
        c.anchor(ReliquaryAnchors.ENTRY, 0, 0, -15);
        return c;
    }

    private static void terrace(Canvas c) {
        for (int x = -WALL; x <= WALL; x++) {
            for (int z = GATE_Z - 3; z <= 15; z++) {
                if (z < GATE_Z && Math.abs(x) > 3) continue;
                c.set(x, -1, z, COURT.at(x, -1, z));
                c.set(x, -2, z, BASE.at(x, -2, z));
            }
        }
    }

    private static void enclosure(Canvas c) {
        Box outline = new Box(-WALL, 0, GATE_Z, WALL, 0, 15);
        int k = 0;
        for (Walls.Run r : Walls.around(outline)) {
            Walls.body(c, r, 0, 3, MASONRY);
            for (int i = 0; i < r.length(); i++) {
                if (Math.floorMod(i, 2) == 0) c.set(r.x(i), 4, r.z(i), St.wall("minecraft:stone_brick_wall"));
            }
            if (r.out() != Dir.NORTH) Walls.pillars(c, r, 0, 3, 6, Brush.of("minecraft:stone_bricks"), Family.STONE_BRICKS, "minecraft:chiseled_stone_bricks", true);
            k++;
        }
        // The gate, its arch.
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) c.carve(x, y, GATE_Z);
            c.set(x, 4, GATE_Z, "minecraft:chiseled_stone_bricks");
        }
        c.set(-1, 3, GATE_Z, St.stairs("minecraft:stone_brick_stairs", Dir.WEST, true));
        c.set(0, 3, GATE_Z, STONE);
        c.set(1, 3, GATE_Z, St.stairs("minecraft:stone_brick_stairs", Dir.EAST, true));
        Lighting.lampPost(c, -3, 0, GATE_Z - 2, 2, "minecraft:stone_bricks", "minecraft:spruce_fence", null, false);
        Lighting.lampPost(c, 3, 0, GATE_Z - 2, 2, "minecraft:stone_bricks", "minecraft:spruce_fence", null, false);
    }

    private static void chapel(Canvas c) {
        Box foot = new Box(CX0, 0, CZ0, CX1, 0, CZ1);
        for (int x = CX0; x <= CX1; x++) {
            for (int z = CZ0; z <= CZ1; z++) {
                boolean edge = x == CX0 || x == CX1 || z == CZ0 || z == CZ1;
                c.set(x, -1, z, edge ? "minecraft:stone_bricks" : FLOOR.at(x, -1, z));
                if (!edge) for (int y = 0; y <= CTOP + 6; y++) c.carve(x, y, z);
            }
        }
        for (Walls.Run r : Walls.around(foot)) {
            Walls.body(c, r, 0, CTOP, MASONRY);
            Walls.plinth(c, r, 0, 1, Family.STONE_BRICKS, r.out() != Dir.NORTH);
            Walls.beam(c, r, CTOP, Wood.SPRUCE, true);
            if (r.out() == Dir.EAST || r.out() == Dir.WEST) {
                Walls.pillars(c, r, 1, CTOP - 1, 3, Brush.of(STONE), Family.STONE_BRICKS, "minecraft:chiseled_stone_bricks", false);
            }
        }
        Walls.cornerPosts(c, foot, 0, CTOP, "minecraft:spruce_log[axis=y]");
        // Windows down the sides, a round one over the door.
        for (int z : new int[]{4, 7, 10}) {
            for (int x : new int[]{CX0, CX1}) for (int y = 2; y <= 4; y++) c.set(x, y, z, St.pane("minecraft:light_blue_stained_glass_pane"));
        }
        for (int[] p : new int[][]{{0, 5}, {-1, 4}, {1, 4}}) c.set(p[0], p[1], CZ0, St.pane("minecraft:yellow_stained_glass_pane"));
        // The door.
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) c.carve(x, y, CZ0);
        c.set(-1, 3, CZ0, "minecraft:chiseled_stone_bricks");
        c.set(1, 3, CZ0, "minecraft:chiseled_stone_bricks");
        c.set(0, 3, CZ0, St.axis("minecraft:stripped_spruce_log", "x"));
        // The slate roof.
        Roofs.Style slate = new Roofs.Style(Family.DEEPSLATE_TILES, Wood.SPRUCE.family(), MASONRY, 1, Roofs.Pitch.STEEP, 0, true,
                "minecraft:spruce_trapdoor", true);
        Roofs.gable(c, foot, CTOP, false, slate);
        // Inside: pews, the apse's step, the altar, sconces.
        for (int z = 3; z <= 8; z += 2) {
            for (int x = -3; x <= 3; x++) {
                if (x == 0) continue;
                c.set(x, 0, z, St.stairs("minecraft:spruce_stairs", Dir.NORTH, false));
            }
        }
        for (int x = -3; x <= 3; x++) c.set(x, -1, 11, "minecraft:polished_diorite");
        c.carve(0, 0, 12);
        c.anchor(ReliquaryAnchors.ALTAR, 0, 0, 12);
        c.anchor(ReliquaryAnchors.KNEEL, 0, 0, 10);
        for (int s : new int[]{-1, 1}) {
            c.set(s * 2, 0, 13, "minecraft:chiseled_stone_bricks");
            c.set(s * 2, 1, 13, St.candle("white", 4, true));
            for (int z : new int[]{3, 6, 9}) Lighting.sconce(c, s * 3, 2, z, s < 0 ? Dir.EAST : Dir.WEST, "minecraft:spruce_trapdoor", false);
        }
        c.set(0, -1, 13, "minecraft:gold_block");
        c.carve(3, 0, 13);
        c.anchor(ReliquaryAnchors.chest(1), 3, 0, 13);
    }

    /** The bell tower at the chapel's north-west corner: solid below, an open belfry, a pyramid cap. */
    private static void tower(Canvas c) {
        Box foot = new Box(-8, 0, 1, -6, 0, 3);
        for (int x = -8; x <= -6; x++) {
            for (int z = 1; z <= 3; z++) {
                boolean post = (x == -8 || x == -6) && (z == 1 || z == 3);
                for (int y = 0; y <= 10; y++) {
                    if (y >= 8 && !post && !(x == -7 && z == 2)) {
                        if (y == 8) c.set(x, y, z, "minecraft:stone_bricks");
                        else c.carve(x, y, z);
                        continue;
                    }
                    if (y >= 9 && x == -7 && z == 2) {
                        c.carve(x, y, z);
                        continue;
                    }
                    c.set(x, y, z, post ? St.axis("minecraft:spruce_log", "y") : MASONRY.at(x, y, z));
                }
            }
        }
        c.set(-7, 9, 2, St.lantern(false));
        c.set(-7, 10, 1, St.fence("minecraft:spruce_fence"));
        c.set(-7, 10, 3, St.fence("minecraft:spruce_fence"));
        Roofs.Style cap = new Roofs.Style(Family.DEEPSLATE_TILES, Wood.SPRUCE.family(), MASONRY, 1, Roofs.Pitch.STEEP, 0, false, null, false);
        Roofs.hip(c, foot, 10, cap);
    }

    /** The monks' dormitory along the east wall under a lean-to roof. */
    private static void dormitory(Canvas c) {
        int x0 = 6, x1 = WALL - 1, z0 = -9, z1 = 6;
        Box foot = new Box(x0, 0, z0, x1, 0, z1);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                c.set(x, -1, z, edge ? "minecraft:stone_bricks" : FLOOR.at(x, -1, z));
                for (int y = 0; y <= 4; y++) {
                    if (!edge) c.carve(x, y, z);
                    else if (y <= 3) c.set(x, y, z, MASONRY.at(x, y, z));
                }
            }
        }
        Walls.cornerPosts(c, foot, 0, 3, "minecraft:spruce_log[axis=y]");
        Walls.pillars(c, new Walls.Run(x0, z0, x0, z1, Dir.WEST), 0, 3, 4, Brush.of("minecraft:spruce_log[axis=y]"), Family.STONE_BRICKS, null, false);
        for (int y = 0; y <= 2; y++) c.carve(x0, y, -2);
        for (int z : new int[]{-6, 2}) for (int y = 1; y <= 2; y++) c.set(x0, y, z, St.pane("minecraft:glass_pane"));
        Roofs.Style lean = new Roofs.Style(Wood.SPRUCE.family(), Wood.SPRUCE.family(), MASONRY, 1, Roofs.Pitch.HALF, 0, false, null, false);
        Roofs.shed(c, foot, 3, Dir.EAST, lean);
        // Cots along the back wall, a desk, lamps.
        for (int z = z0 + 1; z <= z1 - 1; z += 3) {
            c.set(x1 - 1, 0, z, St.slabBottom("minecraft:spruce_slab"));
            c.set(x1 - 1, 0, z + 1, St.slabBottom("minecraft:spruce_slab"));
        }
        c.set(x0 + 1, 0, 4, St.slabTop("minecraft:spruce_slab"));
        c.set(x0 + 1, 1, 4, St.lantern(false));
        c.set(x0 + 1, 0, -7, St.slabTop("minecraft:spruce_slab"));
        c.set(x0 + 1, 1, -7, St.lantern(false));
        c.set(x0 + 2, 0, 1, St.slabTop("minecraft:spruce_slab"));
        c.set(x0 + 2, 1, 1, St.lantern(false));
        c.carve(x1 - 1, 0, -8);
        c.anchor(ReliquaryAnchors.chest(0), x1 - 1, 0, -8);
    }

    /** The court: a well, two spruces, snow drifted on the flags. */
    private static void court(Canvas c) {
        for (int x = -7; x <= -5; x++) {
            for (int z = -7; z <= -5; z++) {
                if (x == -6 && z == -6) {
                    c.set(x, -1, z, St.water());
                    c.set(x, -2, z, "minecraft:stone_bricks");
                } else {
                    c.set(x, 0, z, St.wall("minecraft:cobblestone_wall"));
                    c.set(x, -1, z, "minecraft:stone_bricks");
                }
            }
        }
        Trees.spruce(c, -9, 0, 10, 1, SEED + 20);
        Trees.spruce(c, 9, 0, 11, 0, SEED + 21);
        for (int x = -WALL + 1; x < WALL; x++) {
            for (int z = GATE_Z + 1; z < 15; z++) {
                if (c.has(x, 0, z) || !St.path(c.world(x, -1, z)).equals("snow_block") && !Noise.chance(x, 0, z, SEED + 22, 0.06)) continue;
                if (Kindsish.solidTop(c.world(x, -1, z))) c.set(x, 0, z, St.snow(1));
            }
        }
    }

    /** Whether a planned block takes a snow layer on top (a plain full block). */
    static final class Kindsish {
        static boolean solidTop(String s) {
            String p = St.path(s);
            return !St.isAir(s) && !p.contains("slab") && !p.contains("stairs") && !p.contains("wall") && !p.contains("fence") && !p.equals("water")
                    && !p.contains("pane") && !p.contains("leaves") && !p.contains("snow") && !p.contains("_log");
        }
    }
}
