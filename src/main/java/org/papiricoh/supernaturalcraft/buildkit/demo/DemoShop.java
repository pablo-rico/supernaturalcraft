package org.papiricoh.supernaturalcraft.buildkit.demo;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Clutter;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Furniture;
import org.papiricoh.supernaturalcraft.buildkit.Ground;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Openings;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.Paths;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.Scatter;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Terrain;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.buildkit.Wood;

import java.util.List;

/**
 * A timber-framed shop on a street corner (pure demo): a brick and stone ground floor with a glazed bay under an awning, a
 * jettied upper storey of dark oak framing with braces and plaster, a cross-gabled roof of deepslate tiles with spruce barge
 * boards, a chimney, a cobbled street with lamp posts, a market stall and crates.
 */
public final class DemoShop {

    static final int SEED = 2203;
    static final Box LOWER = new Box(-5, 0, -4, 5, 0, 4), UPPER = new Box(-5, 0, -5, 5, 0, 5);
    static final int UPPER_FLOOR = 4, WALL_TOP = 8;

    private DemoShop() {
    }

    public static Canvas build() {
        Canvas c = new Canvas("kit_shop");
        c.anchor("entry", 3, 0, 12);
        Ground g = Terrain.island(c, Terrain.island(0, 2, 20, -1, 1, 11, SEED));
        Terrain.level(c, g, new Box(-12, 0, -9, 12, 0, 13), -1, Palette.patches2(SEED + 1, 4, "minecraft:grass_block", 10,
                "minecraft:coarse_dirt", 1), Brush.of("minecraft:dirt"), 3);
        c.zone("shop", new Box(-8, -2, -8, 8, 18, 9));
        c.inZone("shop", () -> shop(c));
        street(c, g);
        c.anchor("door", 3, 0, 6);
        Scatter.prune(c);
        return c.finish();
    }

    static final Brush BRICK = Palette.patches(SEED + 10, 2.5, "minecraft:bricks", 6, "minecraft:mud_bricks", 1, "minecraft:granite", 1);
    static final Brush PLASTER = Palette.patches(SEED + 11, 2.5, "minecraft:calcite", 5, "minecraft:white_concrete_powder", 2,
            "minecraft:smooth_sandstone", 1);

    static void shop(Canvas c) {
        // Floors.
        for (int x = LOWER.x0(); x <= LOWER.x1(); x++) {
            for (int z = LOWER.z0(); z <= LOWER.z1(); z++) {
                boolean wall = x == LOWER.x0() || x == LOWER.x1() || z == LOWER.z0() || z == LOWER.z1();
                c.set(x, -1, z, wall ? "minecraft:stone_bricks" : Palette.checker(1, Brush.of("minecraft:polished_andesite"),
                        Brush.of("minecraft:spruce_planks")).at(x, -1, z));
                c.set(x, -2, z, "minecraft:cobblestone");
                for (int y = 0; y < UPPER_FLOOR; y++) if (!wall) c.carve(x, y, z);
            }
        }
        // Ground floor: stone plinth, brick between stone quoins.
        for (Walls.Run r : Walls.around(LOWER)) {
            Walls.body(c, r, 0, 0, Brush.of("minecraft:stone_bricks"));
            Walls.body(c, r, 1, UPPER_FLOOR - 1, BRICK);
            Walls.plinth(c, r, 0, 0, Family.STONE_BRICKS, true);
        }
        Walls.cornerPosts(c, LOWER, 0, UPPER_FLOOR - 1, "minecraft:polished_andesite");
        for (int x : new int[]{LOWER.x0(), LOWER.x1()}) {
            for (int z : new int[]{LOWER.z0(), LOWER.z1()}) for (int y = 1; y < UPPER_FLOOR; y += 2) c.set(x, y, z, "minecraft:stone_bricks");
        }
        // The jettied upper storey: joists and a bressummer out over the street front and the back.
        Walls.jetty(c, new Walls.Run(-5, 4, 5, 4, Dir.SOUTH), UPPER_FLOOR, Wood.DARK_OAK);
        Walls.jetty(c, new Walls.Run(5, -4, -5, -4, Dir.NORTH), UPPER_FLOOR, Wood.DARK_OAK);
        for (int x = UPPER.x0(); x <= UPPER.x1(); x++) {
            for (int z = UPPER.z0(); z <= UPPER.z1(); z++) {
                boolean edge = x == UPPER.x0() || x == UPPER.x1() || z == UPPER.z0() || z == UPPER.z1();
                if (!edge) c.set(x, UPPER_FLOOR, z, Math.floorMod(z, 3) == 0 ? St.axis("minecraft:stripped_dark_oak_log", "x") : "minecraft:spruce_planks");
                if (!edge) for (int y = UPPER_FLOOR + 1; y <= WALL_TOP; y++) c.carve(x, y, z);
            }
        }
        for (Walls.Run r : Walls.around(UPPER)) {
            if (r.out().axis().equals("x")) Walls.beam(c, r, UPPER_FLOOR, Wood.DARK_OAK, false);
            Walls.timberFrame(c, r, UPPER_FLOOR + 1, WALL_TOP, Wood.DARK_OAK, PLASTER, 3, -99, true, "minecraft:dark_oak_stairs");
        }

        // Front: a glazed bay with an awning, the door with a canopy and lamps.
        Walls.Run front = new Walls.Run(-5, 4, 5, 4, Dir.SOUTH);
        for (int x = -4; x <= 0; x++) {
            for (int y = 0; y <= 2; y++) c.carve(x, y, 4);
            c.set(x, -1, 5, "minecraft:stone_bricks");
            c.set(x, 0, 5, x == -4 || x == 0 ? "minecraft:stripped_dark_oak_wood[axis=y]" : St.slab("minecraft:dark_oak_slab", "double"));
            for (int y = 1; y <= 2; y++) c.set(x, y, 5, x == -4 || x == 0 ? St.log("minecraft:stripped_dark_oak_log") : St.pane("minecraft:glass_pane"));
            c.set(x, 3, 5, Wood.DARK_OAK.stairs(Dir.NORTH, true));
            c.set(x, 0, 6, St.trapdoorAgainst("minecraft:dark_oak_trapdoor", Dir.NORTH));
        }
        for (int x = -5; x <= 1; x++) c.set(x, 3, 6, St.trapdoor("minecraft:spruce_trapdoor", Dir.SOUTH, true, false));
        for (int x = -4; x <= 0; x++) c.set(x, 0, 3, St.slabTop("minecraft:dark_oak_slab"));
        Openings.door(c, front, 8, 0, 1, new Openings.DoorStyle("minecraft:dark_oak_door", Brush.of("minecraft:stripped_dark_oak_log[axis=y]"),
                null, Family.STONE_BRICKS, null, "minecraft:dark_oak_trapdoor"));
        // A hanging sign on a bracket over the door.
        c.set(3, 3, 5, St.fence("minecraft:dark_oak_fence"));
        c.set(3, 2, 5, St.of("dark_oak_hanging_sign", "attached", "false", "rotation", "4", "waterlogged", "false"));
        c.decor(org.papiricoh.supernaturalcraft.buildkit.Decor.sign(3, 2, 5, "", "Supply", "& Sundries"));

        // Windows: shop sides and back below, framed casements above.
        Openings.WindowStyle low = new Openings.WindowStyle("minecraft:glass_pane", Brush.of("minecraft:stone_bricks"), Family.STONE_BRICKS,
                null, null, null, false, null);
        Openings.WindowStyle up = new Openings.WindowStyle("minecraft:glass_pane", null, Wood.DARK_OAK.family(), null,
                "minecraft:spruce_trapdoor", null, false, null);
        Walls.Run east = new Walls.Run(5, -4, 5, 4, Dir.EAST), west = new Walls.Run(-5, 4, -5, -4, Dir.WEST);
        Walls.Run back = new Walls.Run(5, -4, -5, -4, Dir.NORTH);
        Openings.window(c, east, 3, 1, 3, 2, low);
        Openings.window(c, west, 3, 1, 3, 2, low);
        Openings.window(c, back, 2, 1, 2, 2, low);
        Walls.Run upFront = new Walls.Run(-5, 5, 5, 5, Dir.SOUTH), upBack = new Walls.Run(5, -5, -5, -5, Dir.NORTH);
        Walls.Run upEast = new Walls.Run(5, -5, 5, 5, Dir.EAST), upWest = new Walls.Run(-5, 5, -5, -5, Dir.WEST);
        Openings.window(c, upFront, 1, 6, 2, 2, up.withFlowerBox("minecraft:moss_block"));
        Openings.window(c, upFront, 7, 6, 2, 2, up.withFlowerBox("minecraft:moss_block"));
        Openings.window(c, upBack, 4, 6, 2, 2, up);
        Openings.window(c, upEast, 4, 6, 3, 2, up);
        Openings.window(c, upWest, 4, 6, 3, 2, up);

        // Inside: a counter, shelves, crates, lamps; upstairs a workroom.
        Furniture.bar(c, -3, 0, -1, Dir.EAST, 5, Dir.SOUTH, "minecraft:stripped_spruce_wood[axis=y]", "minecraft:spruce_stairs", null);
        Furniture.bookshelves(c, -4, -3, Dir.EAST, 6, 0, 1, Dir.SOUTH, true, St.log("minecraft:stripped_dark_oak_log"), 3);
        for (int z = -2; z <= 2; z += 2) Furniture.cabinet(c, 4, 0, z, Dir.WEST, "minecraft:spruce_planks", null, true);
        Lighting.hanging(c, -1, UPPER_FLOOR, 1, 1, false);
        Lighting.hanging(c, 2, UPPER_FLOOR, -1, 1, false);
        Furniture.flight(c, 4, 0, 3, Dir.NORTH, 4, 1, "minecraft:dark_oak_stairs", true);
        c.set(4, UPPER_FLOOR, -1, "minecraft:spruce_planks");
        Lighting.hanging(c, -3, WALL_TOP + 3, 3, 4, false);
        Lighting.hanging(c, 3, WALL_TOP + 3, 3, 4, false);
        c.set(3, UPPER_FLOOR + 1, 1, St.fence("minecraft:dark_oak_fence"));
        c.set(3, UPPER_FLOOR + 1, 2, St.fence("minecraft:dark_oak_fence"));
        Furniture.desk(c, -2, UPPER_FLOOR + 1, -3, Dir.NORTH, "minecraft:dark_oak_slab", "minecraft:dark_oak_stairs", false);
        Furniture.bed(c, -3, UPPER_FLOOR + 1, 2, Dir.WEST, "blue", "minecraft:chiseled_bookshelf[facing=east,slot_0_occupied=true,slot_1_occupied=false,slot_2_occupied=true,slot_3_occupied=false,slot_4_occupied=false,slot_5_occupied=true]", true);
        Lighting.hanging(c, 0, WALL_TOP + 4, 0, 4, false);
        Clutter.room(c, new Box(-4, 0, -3, 4, 3, 3), Clutter.Kind.WORKSHOP, 0.5, 0.15, true, new Box(-4, 0, 0, 4, 1, 1), SEED + 20);

        // The roof: a gable to the street crossed by one to the sides.
        Roofs.Style roof = new Roofs.Style(Family.DEEPSLATE_TILES, Wood.SPRUCE.family(),
                Palette.halfTimber(PLASTER, "minecraft:stripped_dark_oak_log[axis=y]", 2),
                1, Roofs.Pitch.STEEP, 0, true, "minecraft:spruce_trapdoor", true);
        new Roofs.Plan().gable(UPPER, WALL_TOP, false, roof).gable(new Box(-5, 0, -4, 5, 0, 4), WALL_TOP, true, roof).draw(c);
        Box roofBox = new Box(-7, WALL_TOP - 1, -7, 7, 18, 7);
        Palette.swapFamily(c, roofBox, Family.DEEPSLATE_TILES, Family.COBBLED_DEEPSLATE, 0.14, 2.5, SEED + 32);
        Palette.swapFamily(c, roofBox, Family.DEEPSLATE_TILES, Family.DEEPSLATE_BRICKS, 0.12, 2, SEED + 33);
        Roofs.chimney(c, -3, -3, UPPER_FLOOR + 1, Palette.patches(SEED + 31, 2, "minecraft:bricks", 4, "minecraft:mud_bricks", 1),
                Family.STONE_BRICKS, true);
        c.set(0, WALL_TOP + 2, 5, St.pane("minecraft:glass_pane"));
        c.set(0, WALL_TOP + 3, 5, St.pane("minecraft:glass_pane"));
    }

    static void street(Canvas c, Ground g) {
        Paths.Style cobble = new Paths.Style(5,
                Palette.patches2(SEED + 40, 2, "minecraft:cobblestone", 4, "minecraft:stone", 2, "minecraft:andesite", 2,
                        "minecraft:mossy_cobblestone", 1, "minecraft:gravel", 1),
                Palette.patches2(SEED + 41, 1.5, "minecraft:gravel", 3, "minecraft:coarse_dirt", 2, "minecraft:cobblestone", 2,
                        "minecraft:moss_block", 1),
                null, Brush.of("minecraft:polished_andesite"), "minecraft:cobblestone_wall", 10, "minecraft:stone_bricks", null);
        Paths.path(c, g, List.of(new int[]{-20, 10}, new int[]{-8, 9}, new int[]{4, 10}, new int[]{20, 8}), cobble, SEED + 42);
        Paths.path(c, g, List.of(new int[]{3, 7}, new int[]{3, 9}), new Paths.Style(2, Brush.of("minecraft:stone_bricks"),
                Brush.of("minecraft:cobblestone"), null, null, null, 0, null, null), SEED + 43);

        // A market stall across the street.
        int sx = -6, sz = 13;
        if (g.has(sx, sz)) {
            int t = g.top(sx, sz);
            for (int dx = 0; dx <= 3; dx++) {
                for (int dz = 0; dz <= 2; dz++) {
                    int x = sx + dx, z = sz + dz;
                    boolean post = (dx == 0 || dx == 3) && (dz == 0 || dz == 2);
                    if (post) for (int y = t + 1; y <= t + 3; y++) c.set(x, y, z, St.fence("minecraft:spruce_fence"));
                    c.set(x, t + 4, z, St.carpet(dx % 2 == 0 ? "red" : "white"));
                    c.set(x, t + 4 - 1, z, post ? St.fence("minecraft:spruce_fence") : St.trapdoor("minecraft:spruce_trapdoor", Dir.NORTH, true, false));
                }
                c.set(sx + dx, t + 1, sz, St.slabTop("minecraft:spruce_slab"));
            }
            c.set(sx + 1, t + 2, sz, "minecraft:melon");
            c.set(sx + 2, t + 2, sz, St.of("carved_pumpkin", "facing", "north"));
            c.set(sx + 1, t + 1, sz + 2, St.barrel(Dir.UP, true));
            c.set(sx + 2, t + 1, sz + 2, St.axis("minecraft:hay_block", "x"));
        }
        // Crates by the shop's side, a tree and planting.
        int[][] crates = {{7, -1}, {7, 0}, {8, -1}};
        for (int[] k : crates) if (g.has(k[0], k[1])) c.set(k[0], g.top(k[0], k[1]) + 1, k[1], St.barrel(Dir.UP, false));
        c.set(7, g.top(7, -1) + 2, -1, St.barrel(Dir.UP, false));
        if (g.has(12, -4)) Trees.oak(c, 12, g.top(12, -4) + 1, -4, 1, SEED + 50);
        if (g.has(-12, -3)) Trees.birch(c, -12, g.top(-12, -3) + 1, -3, 1, SEED + 51);
        if (g.has(-9, 2)) Trees.bush(c, -9, g.top(-9, 2) + 1, 2, "minecraft:azalea_leaves", 1.3, SEED + 52);
        Scatter.flora(c, g, null, 0.4, 1, SEED + 53, Scatter.MEADOW);
    }
}
