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
 * A storybook cottage on a small sky island (pure demo): white plaster over a stone plinth between spruce posts, a steep dark
 * oak roof with spruce barge boards, a cross-gabled front wing, two dormers, a stone chimney with smoke, a porch, a furnished
 * ground floor and attic, a path with lamp posts through a front garden with a picket fence, and custom trees.
 */
public final class DemoCottage {

    static final int SEED = 1201;
    /** The main block's walls and the front wing's. */
    static final Box MAIN = new Box(-6, 0, -5, 6, 0, 5), WING = new Box(1, 0, 5, 5, 0, 9);
    static final int WALL_TOP = 4;

    private DemoCottage() {
    }

    public static Canvas build() {
        Canvas c = new Canvas("kit_cottage");
        c.anchor("entry", -3, 0, 9);
        Ground g = Terrain.island(c, Terrain.island(0, 3, 24, -1, 2, 14, SEED));
        Brush grass = Palette.patches2(SEED + 1, 5, "minecraft:grass_block", 12, "minecraft:moss_block", 1);
        Brush soil = Palette.patches(SEED + 2, 3, "minecraft:dirt", 5, "minecraft:coarse_dirt", 1);
        Terrain.level(c, g, new Box(-10, 0, -8, 10, 0, 12), -1, grass, soil, 3);

        c.zone("house", new Box(-9, -2, -8, 8, 16, 11));
        c.inZone("house", () -> house(c));
        garden(c, g);
        Scatter.prune(c);
        return c.finish();
    }

    // --- the house -----------------------------------------------------------------------------------------------------------

    static final Brush PLASTER = Palette.patches(SEED + 10, 2.5, "minecraft:calcite", 6, "minecraft:white_concrete_powder", 2,
            "minecraft:diorite", 1, "minecraft:white_terracotta", 1);
    static final Brush STONE = Palette.weathered(Palette.STONE_BRICK_WEAR, Palette.Field.nearGround(-1, 3), 0.3, SEED + 11);
    static final Brush BOARDS = Palette.halfTimber(Palette.patches(SEED + 12, 2, "minecraft:calcite", 4, "minecraft:white_concrete_powder", 1),
            "minecraft:stripped_dark_oak_log[axis=y]", 2);
    static final String POST = St.log("minecraft:spruce_log");

    static void house(Canvas c) {
        Box[] parts = {MAIN, WING};
        // Floors and foundations.
        for (Box b : parts) {
            for (int x = b.x0(); x <= b.x1(); x++) {
                for (int z = b.z0(); z <= b.z1(); z++) {
                    boolean wall = x == b.x0() || x == b.x1() || z == b.z0() || z == b.z1();
                    c.set(x, -1, z, wall ? "minecraft:stone_bricks" : Palette.boards(true, Brush.of("minecraft:oak_planks"),
                            Brush.of("minecraft:spruce_planks"), 5).at(x, -1, z));
                    c.set(x, -2, z, "minecraft:cobblestone");
                    for (int y = 0; y <= 3; y++) if (!wall) c.carve(x, y, z);
                }
            }
        }
        // Walls: a stone plinth row with a skirting, plaster between spruce posts, a log plate on top.
        for (Box b : parts) {
            for (Walls.Run r : Walls.around(b)) {
                boolean shared = b == WING && r.out() == Dir.NORTH;
                if (shared) continue;
                Walls.body(c, r, 0, 0, STONE);
                Walls.body(c, r, 1, 3, PLASTER);
                Walls.beam(c, r, WALL_TOP, Wood.SPRUCE, true);
            }
        }
        for (Box b : parts) Walls.cornerPosts(c, b, 0, WALL_TOP, POST);
        // The skirting, round both outlines (the shared wall has none).
        for (Box b : parts) {
            for (Walls.Run r : Walls.around(b)) {
                if (b == WING && r.out() == Dir.NORTH || b == MAIN && r.out() == Dir.SOUTH) continue;
                Walls.plinth(c, r, 0, 0, Family.COBBLESTONE, true);
            }
        }
        Walls.Run south = new Walls.Run(-6, 5, 0, 5, Dir.SOUTH);
        Walls.plinth(c, south, 0, 0, Family.COBBLESTONE, false);
        // The wing opens into the main room.
        for (int x = 2; x <= 4; x++) for (int y = 0; y <= 3; y++) c.carve(x, y, 5);
        c.set(2, 3, 5, Wood.SPRUCE.stairs(Dir.WEST, true));
        c.set(4, 3, 5, Wood.SPRUCE.stairs(Dir.EAST, true));
        c.set(3, 3, 5, St.axis("minecraft:stripped_spruce_log", "x"));
        for (int y = 0; y <= WALL_TOP; y++) {
            c.set(1, y, 5, POST);
            c.set(5, y, 5, POST);
        }
        // Gable belts: a ledge of spruce under each main gable.
        Walls.belt(c, new Walls.Run(-6, -5, -6, 5, Dir.WEST), WALL_TOP, Wood.SPRUCE.family());
        Walls.belt(c, new Walls.Run(6, -5, 6, 5, Dir.EAST), WALL_TOP, Wood.SPRUCE.family());

        // Openings.
        Openings.WindowStyle win = new Openings.WindowStyle("minecraft:glass_pane", null, Wood.SPRUCE.family(), null,
                "minecraft:dark_oak_trapdoor", null, false, null);
        Openings.WindowStyle box = win.withFlowerBox("minecraft:rooted_dirt");
        Openings.WindowStyle big = new Openings.WindowStyle("minecraft:glass_pane", null, Wood.SPRUCE.family(), Wood.SPRUCE.family(),
                null, "minecraft:rooted_dirt", false, "minecraft:stripped_spruce_log[axis=y]");
        Openings.window(c, south, 1, 1, 1, 2, box);
        Openings.window(c, south, 5, 1, 1, 2, win);
        Walls.Run wingSouth = new Walls.Run(1, 9, 5, 9, Dir.SOUTH);
        Openings.window(c, wingSouth, 1, 1, 3, 2, big);
        Walls.Run east = new Walls.Run(6, -5, 6, 5, Dir.EAST);
        Openings.window(c, east, 2, 1, 2, 2, win);
        Openings.window(c, east, 7, 1, 1, 2, win);
        Walls.Run wingEast = new Walls.Run(5, 5, 5, 9, Dir.EAST);
        Openings.window(c, wingEast, 2, 1, 1, 2, win);
        Walls.Run north = new Walls.Run(6, -5, -6, -5, Dir.NORTH);
        Openings.window(c, north, 2, 1, 2, 2, win);
        Openings.window(c, north, 9, 1, 2, 2, win);
        Walls.Run west = new Walls.Run(-6, 5, -6, -5, Dir.WEST);
        Openings.window(c, west, 1, 1, 1, 2, win);
        Openings.window(c, west, 9, 1, 1, 2, win);
        Openings.DoorStyle door = new Openings.DoorStyle("minecraft:spruce_door", Brush.of(POST), null, null, null, null);
        Openings.door(c, south, 3, 0, 1, door);
        Openings.door(c, north, 6, 0, 1, new Openings.DoorStyle("minecraft:spruce_door", Brush.of(POST), null, Family.COBBLESTONE,
                Wood.SPRUCE.family(), "minecraft:spruce_trapdoor"));

        // The porch along the main front, west of the wing.
        Openings.porch(c, new Walls.Run(-6, 5, 0, 5, Dir.SOUTH), 0, 6, 2, -1, 2, 3, 3,
                new Openings.PorchStyle(Brush.of("minecraft:spruce_planks"), POST, "minecraft:spruce_fence", 3,
                        Wood.DARK_OAK.family(), Family.COBBLESTONE, Wood.SPRUCE));

        // Inside: the attic floor with beams, the stair up, the rooms.
        for (int x = MAIN.x0() + 1; x < MAIN.x1(); x++) {
            for (int z = MAIN.z0() + 1; z < MAIN.z1(); z++) {
                c.set(x, WALL_TOP, z, Math.floorMod(x, 3) == 0 ? St.axis("minecraft:stripped_spruce_log", "z")
                        : Palette.boards(false, Brush.of("minecraft:spruce_planks"), Brush.of("minecraft:dark_oak_planks"), 4).at(x, WALL_TOP, z));
            }
        }
        // (the stair up is drawn after the roof)

        // Living room: hearth, sofa, rug over a hidden light, shelves.
        Furniture.fireplace(c, -5, 0, 0, Dir.EAST, Family.COBBLESTONE, true);
        Furniture.sofa(c, -2, 0, 1, Dir.WEST, 3, "minecraft:spruce_stairs", "minecraft:spruce_trapdoor");
        for (int x = -4; x <= -3; x++) for (int z = -1; z <= 1; z++) c.set(x, 0, z, St.carpet(x == -4 && z == 0 ? "red" : "brown"));
        Lighting.underCarpet(c, -3, 0, 0, "minecraft:ochre_froglight", "red");
        Furniture.bookshelves(c, -5, -4, Dir.EAST, 3, 0, 1, Dir.SOUTH, true, null, 0);
        c.set(-4, 2, -4, St.lantern(false));
        Furniture.sideTable(c, -5, 0, 3, "minecraft:spruce_fence", "green");
        c.set(-1, 0, -3, St.potted("fern"));
        // Kitchen along the east wall and a lamp over it.
        Furniture.kitchen(c, 5, 0, -1, Dir.SOUTH, 4, Dir.WEST, "minecraft:stripped_spruce_wood[axis=y]", "minecraft:spruce_trapdoor", true);
        Lighting.hanging(c, 3, WALL_TOP, 1, 1, false);
        Lighting.hanging(c, 4, WALL_TOP, -4, 1, false);
        Lighting.hanging(c, -2, WALL_TOP, -2, 1, false);
        // Dining in the wing under its open roof.
        Furniture.diningSet(c, 3, 6, 3, 7, 0, "minecraft:spruce_slab", "minecraft:spruce_stairs");
        c.set(3, 1, 6, St.candle("white", 3, true));
        // The attic bedroom.
        Furniture.bed(c, -3, WALL_TOP + 1, 1, Dir.WEST, "red", St.barrel(Dir.UP, false), true);
        for (int x = -4; x <= -2; x++) for (int z = -1; z <= 0; z++) c.setIfAir(x, WALL_TOP + 1, z, St.carpet("light_gray"));
        c.set(-1, WALL_TOP + 1, -4, St.lantern(false));
        c.set(-3, WALL_TOP + 1, -4, St.candle("white", 3, true));
        c.set(-5, WALL_TOP + 1, 3, St.lantern(false));
        c.set(-5, WALL_TOP + 1, -1, St.of("chest", "facing", "east", "type", "single", "waterlogged", "false"));
        Furniture.desk(c, 2, WALL_TOP + 1, 2, Dir.NORTH, "minecraft:spruce_slab", "minecraft:spruce_stairs", false);

        // The roof: main gable, the wing's cross gable, a dormer each side.
        Roofs.Style roof = new Roofs.Style(Wood.DARK_OAK.family(), Wood.SPRUCE.family(), BOARDS, 1, Roofs.Pitch.STEEP, 0, true,
                "minecraft:spruce_trapdoor", true);
        new Roofs.Plan()
                .gable(MAIN, WALL_TOP, true, roof)
                .gable(WING, WALL_TOP, false, roof)
                .dormer(-3, 5, Dir.SOUTH, 3, 3, WALL_TOP + 1, 7, BOARDS, "minecraft:glass_pane", roof)
                .dormer(-2, -5, Dir.NORTH, 3, 3, WALL_TOP + 1, 7, BOARDS, "minecraft:glass_pane", roof)
                .draw(c);
        // Weather the roof: patches of spruce shingles among the dark oak.
        Palette.swapFamily(c, new Box(-9, WALL_TOP - 1, -8, 8, 16, 11), Wood.DARK_OAK.family(), Wood.SPRUCE.family(), 0.16, 2.5, SEED + 60);
        // The stair up, after the roof so its sloped ceiling does not fill the stairwell.
        Furniture.flight(c, 1, 0, -3, Dir.EAST, 4, 1, "minecraft:spruce_stairs", true);
        for (int x = 2; x <= 4; x++) c.set(x, WALL_TOP + 1, -2, St.fence("minecraft:spruce_fence"));
        c.set(1, WALL_TOP + 1, -3, St.fence("minecraft:spruce_fence"));
        Lighting.hanging(c, -1, 10, 0, 3, false);
        Lighting.hanging(c, -5, WALL_TOP, 6, 1, false);
        Lighting.hanging(c, -1, WALL_TOP, 6, 1, false);
        Lighting.hanging(c, 3, 7, 7, 2, false);
        // The gable windows.
        c.set(3, 6, 9, St.pane("minecraft:glass_pane"));
        c.set(-6, 6, 0, St.pane("minecraft:glass_pane"));
        c.set(6, 6, 0, St.pane("minecraft:glass_pane"));
        c.set(6, 7, 0, St.pane("minecraft:glass_pane"));

        // The chimney on the west gable: a stepped stone breast, then the flue up through the rake.
        Brush chim = Palette.patches(SEED + 20, 2, "minecraft:cobblestone", 3, "minecraft:stone_bricks", 2, "minecraft:mossy_cobblestone", 1);
        for (int y = -1; y <= 3; y++) {
            for (int z = -1; z <= 1; z++) {
                c.set(-7, y, z, chim.at(-7, y, z));
                if (y <= 2) c.set(-8, y, z, chim.at(-8, y, z));
            }
        }
        for (int z = -1; z <= 1; z++) c.set(-8, 3, z, Family.COBBLESTONE.stairs(Dir.EAST, false));
        c.set(-7, 4, -1, Family.COBBLESTONE.stairs(Dir.SOUTH, false));
        c.set(-7, 4, 1, Family.COBBLESTONE.stairs(Dir.NORTH, false));
        Roofs.chimney(c, -7, 0, 4, chim, Family.STONE_BRICKS, true);

        // A reading corner and the small things of a lived-in house.
        Furniture.chair(c, -4, 0, -3, Dir.SOUTH, "minecraft:spruce_stairs", "minecraft:spruce_trapdoor");
        Furniture.sideTable(c, -2, 0, -3, "minecraft:dark_oak_fence", "white");
        Furniture.diningSet(c, 1, 2, 2, 2, 0, "minecraft:dark_oak_slab", "minecraft:spruce_stairs");
        Clutter.room(c, new Box(-5, 0, -4, 5, 3, 4), Clutter.Kind.LIVING, 0.5, 0.12, true, new Box(-4, 0, 3, -2, 1, 4), SEED + 70);
        c.decor(org.papiricoh.supernaturalcraft.buildkit.Decor.painting(0, 2, -4, Dir.SOUTH, "kebab"));
        c.anchor("hearth", -4, 0, 0);
        c.anchor("bed", -3, WALL_TOP + 1, 1);
    }

    // --- the garden ------------------------------------------------------------------------------------------------------------

    static void garden(Canvas c, Ground g) {
        Paths.Style path = new Paths.Style(3,
                Palette.patches2(SEED + 30, 2, "minecraft:dirt_path", 8, "minecraft:coarse_dirt", 1),
                Palette.patches2(SEED + 31, 1.5, "minecraft:coarse_dirt", 3, "minecraft:gravel", 1, "minecraft:moss_block", 1,
                        "minecraft:dirt_path", 2, "minecraft:packed_mud", 1),
                null, null, "minecraft:spruce_fence", 9, "minecraft:cobblestone",
                new Object[]{"minecraft:short_grass", 6, "minecraft:fern", 2, "minecraft:oxeye_daisy", 2, "minecraft:azure_bluet", 2,
                        "minecraft:cornflower", 1, "minecraft:tall_grass", 1});
        List<int[]> paved = Paths.path(c, g, List.of(new int[]{-3, 8}, new int[]{-4, 12}, new int[]{-1, 16}, new int[]{-2, 20},
                new int[]{-4, 25}), path, SEED + 32);
        java.util.Set<Long> pavedSet = new java.util.HashSet<>();
        for (int[] p : paved) pavedSet.add(Ground.col(p[0], p[1]));
        // A side path to the back door.
        Paths.path(c, g, List.of(new int[]{0, -7}, new int[]{3, -10}, new int[]{4, -15}), Paths.countryPath(2, SEED + 33), SEED + 34);

        // A picket fence round the front garden with a gate on the path.
        for (int x = -10; x <= 10; x++) fencePost(c, g, x, 12, pavedSet, Dir.SOUTH);
        for (int z = 6; z < 12; z++) {
            fencePost(c, g, -10, z, pavedSet, Dir.WEST);
            fencePost(c, g, 10, z, pavedSet, Dir.EAST);
        }

        // A vegetable patch east of the path, watered down the middle.
        Terrain.level(c, g, new Box(3, 0, 13, 9, 0, 19), g.topOr(6, 16, -1), Brush.of("minecraft:grass_block"), Brush.of("minecraft:dirt"), 1);
        for (int x = 4; x <= 8; x++) {
            for (int z = 14; z <= 18; z++) {
                if (!g.has(x, z)) continue;
                int t = g.top(x, z);
                if (x == 6) {
                    c.set(x, t, z, St.water());
                    c.set(x, t - 1, z, "minecraft:mud");
                    g.markWater(x, z);
                    continue;
                }
                c.set(x, t, z, St.of("farmland", "moisture", "7"));
                String crop = x == 4 ? St.crop("minecraft:wheat", 7) : x == 5 ? St.crop("minecraft:carrots", 7)
                        : x == 7 ? St.crop("minecraft:potatoes", 7) : St.crop("minecraft:beetroots", 3);
                c.set(x, t + 1, z, crop);
            }
        }
        for (int x = 3; x <= 9; x++) {
            for (int z = 13; z <= 19; z++) {
                if ((x == 3 || x == 9 || z == 13 || z == 19) && g.has(x, z)) {
                    int t = g.top(x, z);
                    c.set(x, t, z, St.axis("minecraft:spruce_log", x == 3 || x == 9 ? "z" : "x"));
                }
            }
        }
        c.set(9, g.top(9, 19) + 1, 19, "minecraft:composter");

        // Trees and shrubs.
        tree(c, g, 13, 4, (x, y, z) -> Trees.oak(c, x, y, z, 2, SEED + 40));
        tree(c, g, -14, 9, (x, y, z) -> Trees.cherry(c, x, y, z, 1, SEED + 41));
        tree(c, g, 12, -9, (x, y, z) -> Trees.birch(c, x, y, z, 1, SEED + 42));
        tree(c, g, 15, -3, (x, y, z) -> Trees.birch(c, x, y, z, 0, SEED + 43));
        tree(c, g, -13, -8, (x, y, z) -> Trees.spruce(c, x, y, z, 1, SEED + 44));
        tree(c, g, -6, -13, (x, y, z) -> Trees.spruce(c, x, y, z, 0, SEED + 45));
        tree(c, g, -15, 1, (x, y, z) -> Trees.bush(c, x, y, z, "minecraft:flowering_azalea_leaves", 1.6, SEED + 46));
        tree(c, g, 8, 7, (x, y, z) -> Trees.bush(c, x, y, z, "minecraft:azalea_leaves", 1.2, SEED + 47));
        tree(c, g, -8, 7, (x, y, z) -> Trees.bush(c, x, y, z, "minecraft:flowering_azalea_leaves", 1.1, SEED + 48));
        tree(c, g, 6, 21, (x, y, z) -> Trees.oak(c, x, y, z, 1, SEED + 49));

        // A flower bed along the front of the porch, then a meadow everywhere else.
        Scatter.flora(c, g, new Box(-9, 0, 9, -5, 0, 11), 0.9, 1, SEED + 50, Scatter.GARDEN);
        Scatter.flora(c, g, null, 0.45, 1, SEED + 51, Scatter.MEADOW);
    }

    interface Grow {
        void at(int x, int y, int z);
    }

    static void tree(Canvas c, Ground g, int x, int z, Grow grow) {
        if (!g.has(x, z)) return;
        grow.at(x, g.top(x, z) + 1, z);
    }

    static void fencePost(Canvas c, Ground g, int x, int z, java.util.Set<Long> path, Dir face) {
        if (!g.has(x, z) || g.water(x, z)) return;
        int t = g.top(x, z);
        if (!c.isAir(x, t + 1, z) && !org.papiricoh.supernaturalcraft.buildkit.Kinds.soilPlant(c.get(x, t + 1, z))) return;
        if (path.contains(Ground.col(x, z))) {
            c.set(x, t + 1, z, St.gate("minecraft:spruce_fence_gate", face, false, false));
        } else {
            c.set(x, t + 1, z, St.fence("minecraft:spruce_fence"));
        }
        String up = c.get(x, t + 2, z);
        if (up != null && "upper".equals(St.get(up, "half"))) c.carve(x, t + 2, z);
    }
}
