package org.papiricoh.supernaturalcraft.heaven.roadhouse;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Clutter;
import org.papiricoh.supernaturalcraft.buildkit.Decor;
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
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.List;

/**
 * Harvelle's Roadhouse in Heaven (v0.18, pure): the hub every plot's red door leads to, plot 0 at the dimension's origin. Ash
 * keeps the bar. Contract points fixed by the foundations (see {@code HeavenPlotLayout} for the conventions).
 * <p>The build (buildkit, the {@code ROADHOUSE} scheme): a weathered spruce and dark oak roadhouse on a cobble footing under a
 * rusted tin roof, a deep porch with its name board, inside a long bar with a back bar of shelves, booths, a pool table, a jukebox
 * nook and hanging lamps; a gravel lot with picnic tables and barrels in front, the red door's frame by the landing, old oaks.
 * Zones: {@code hub} (the building and its yard), {@code island} (terrain, last).
 */
public final class RoadhouseLayout {

    /** Where someone through a plot's red door arrives (facing north, towards the Roadhouse). */
    public static final LayoutPoint HUB_LANDING = new LayoutPoint(0, 0, 40);
    /** Behind the bar: Ash stands here. */
    public static final LayoutPoint ASH_SPOT = new LayoutPoint(0, 0, -6);
    /** The door back to one's own plot (a red door frame near the landing). */
    public static final LayoutPoint PLOT_DOOR = new LayoutPoint(0, 0, 48);
    public static final int RADIUS = 110;

    static final Box HALL = new Box(-12, 0, -12, 12, 0, 8);
    static final int WALL_TOP = 5;

    private RoadhouseLayout() {
    }

    public static LayoutPlan plan(long seed) {
        return canvas(seed).toPlan();
    }

    /** The hub as a finished kit canvas (tests and the layout dump). */
    public static Canvas canvas(long seed) {
        int s = (int) (seed ^ (seed >>> 32)) * 17 + 5;
        Canvas c = new Canvas("heaven_roadhouse");
        c.zone("hub", new Box(-40, -8, -30, 40, 24, 64));
        c.zone("island", new Box(-70, -48, -60, 70, 30, 90));
        Ground[] gr = new Ground[1];
        c.inZone("island", () -> {
            Ground g = Terrain.island(c, Terrain.island(0, 16, 50, -1, 2, 22, s));
            Terrain.level(c, g, new Box(-22, 0, -20, 22, 0, 52), -1, Palette.patches2(s + 1, 4, "minecraft:grass_block", 10, "minecraft:coarse_dirt", 1),
                    Brush.of("minecraft:dirt"), 4);
            gr[0] = g;
        });
        Ground g = gr[0];
        c.inZone("hub", () -> build(c, g, s));
        c.inZone("island", () -> {
            Scatter.flora(c, g, null, 0.45, 1, s + 40, Scatter.MEADOW);
            Scatter.prune(c);
        });
        c.anchor("hub_landing", HUB_LANDING.x(), HUB_LANDING.y(), HUB_LANDING.z());
        c.anchor("ash_spot", ASH_SPOT.x(), ASH_SPOT.y(), ASH_SPOT.z());
        c.anchor("plot_door", PLOT_DOOR.x(), PLOT_DOOR.y(), PLOT_DOOR.z());
        return c.finish();
    }

    private static void build(Canvas c, Ground g, int s) {
        Palette.Scheme rh = Palette.ROADHOUSE;
        // The lot: gravel and packed earth from the porch down to the landing.
        Paths.Style lot = new Paths.Style(9, Palette.patches2(s + 2, 2, "minecraft:gravel", 4, "minecraft:coarse_dirt", 3, "minecraft:packed_mud", 1),
                Palette.patches2(s + 3, 1.5, "minecraft:coarse_dirt", 3, "minecraft:gravel", 2, "minecraft:dirt", 1), null, null,
                "minecraft:spruce_fence", 14, "minecraft:cobblestone", new Object[]{"minecraft:short_grass", 6, "minecraft:dead_bush", 1, "minecraft:tall_grass", 2});
        Paths.path(c, g, List.of(new int[]{0, 12}, new int[]{1, 26}, new int[]{0, 40}, new int[]{0, 50}), lot, s + 4);
        // Floor and walls: a cobble footing, weathered boards, dark oak posts at the corners and every few blocks.
        for (int x = HALL.x0(); x <= HALL.x1(); x++) {
            for (int z = HALL.z0(); z <= HALL.z1(); z++) {
                boolean edge = x == HALL.x0() || x == HALL.x1() || z == HALL.z0() || z == HALL.z1();
                c.set(x, -1, z, edge ? "minecraft:cobblestone" : rh.floor().at(x, -1, z));
                c.set(x, -2, z, "minecraft:cobblestone");
                if (!edge) for (int y = 0; y <= WALL_TOP; y++) c.carve(x, y, z);
            }
        }
        for (Walls.Run r : Walls.around(HALL)) {
            Walls.body(c, r, 0, 0, rh.plinth());
            Walls.body(c, r, 1, WALL_TOP, rh.wall());
            Walls.plinth(c, r, 0, 0, Family.COBBLESTONE, true);
            for (int p : Walls.pillarPositions(r, 4)) for (int y = 1; y <= WALL_TOP; y++) c.set(r.x(p), y, r.z(p), St.log("minecraft:dark_oak_log"));
            Walls.beam(c, r, WALL_TOP, Wood.SPRUCE, false);
        }
        Openings.WindowStyle win = new Openings.WindowStyle("minecraft:orange_stained_glass_pane", null, Wood.SPRUCE.family(), null,
                "minecraft:spruce_trapdoor", null, false, null);
        Walls.Run front = new Walls.Run(-12, 8, 12, 8, Dir.SOUTH), back = new Walls.Run(12, -12, -12, -12, Dir.NORTH);
        Walls.Run east = new Walls.Run(12, -12, 12, 8, Dir.EAST), west = new Walls.Run(-12, 8, -12, -12, Dir.WEST);
        for (int i : new int[]{2, 6, 17, 21}) Openings.window(c, front, i, 1, 2, 2, win);
        for (int i : new int[]{2, 6, 17, 21}) Openings.window(c, back, i, 2, 2, 2, win.withShutters(null));
        for (int i : new int[]{2, 9, 14}) Openings.window(c, east, i, 1, 2, 2, win);
        for (int i : new int[]{5, 9, 16}) Openings.window(c, west, i, 1, 2, 2, win);
        Openings.door(c, front, 11, 0, 2, new Openings.DoorStyle("minecraft:dark_oak_door", Brush.of("minecraft:stripped_dark_oak_log[axis=y]"), null,
                null, null, null));
        // The porch with its name board.
        Openings.porch(c, front, 0, 24, 3, -1, 3, 10, 13, new Openings.PorchStyle(Palette.boards(true, Brush.of("minecraft:spruce_planks"),
                Brush.of("minecraft:dark_oak_planks"), 5), St.log("minecraft:dark_oak_log"), "minecraft:spruce_fence", 4, Family.WEATHERED_CUT_COPPER,
                Family.COBBLESTONE, Wood.DARK_OAK));
        c.set(0, 4, 9, St.wallSign("dark_oak", Dir.SOUTH));
        c.decor(Decor.sign(0, 4, 9, "Harvelle's", "Roadhouse"));
        c.set(-5, 0, 10, St.stairs("minecraft:spruce_stairs", Dir.NORTH, false));
        c.set(-6, 0, 10, St.stairs("minecraft:spruce_stairs", Dir.NORTH, false));
        c.set(6, 0, 10, St.barrel(Dir.UP, false));
        c.set(7, 0, 10, St.barrel(Dir.UP, false));
        Lighting.hanging(c, -8, 5, 9, 2, false);
        Lighting.hanging(c, 8, 5, 9, 2, false);
        Lighting.hanging(c, 0, 5, 10, 2, false);
        Lighting.hanging(c, 9, WALL_TOP + 1, -9, 2, false);
        Lighting.hanging(c, -9, WALL_TOP + 1, -9, 2, false);
        // The roof: rusted tin, half pitch, a chimney at the east end.
        Roofs.gable(c, HALL, WALL_TOP, true, new Roofs.Style(Family.WEATHERED_CUT_COPPER, Wood.SPRUCE.family(), rh.wall(), 1, Roofs.Pitch.HALF, 0,
                true, "minecraft:spruce_trapdoor", true));
        Palette.swapFamily(c, HALL.growXZ(2).withY(WALL_TOP, 20), Family.WEATHERED_CUT_COPPER, Family.OXIDIZED_CUT_COPPER, 0.18, 2.5, s + 5);
        Palette.swapFamily(c, HALL.growXZ(2).withY(WALL_TOP, 20), Family.WEATHERED_CUT_COPPER, Family.EXPOSED_CUT_COPPER, 0.15, 2, s + 6);
        Roofs.chimney(c, 10, -10, 0, Palette.patches(s + 7, 2, "minecraft:bricks", 3, "minecraft:cobblestone", 2), Family.COBBLESTONE, true);
        // The bar: a counter with stools, Ash's aisle behind it, a back bar of shelves and bottles.
        Furniture.bar(c, -6, 0, -4, Dir.EAST, 13, Dir.SOUTH, "minecraft:stripped_dark_oak_wood[axis=y]", "minecraft:dark_oak_stairs", "minecraft:dark_oak_fence");
        for (int x = -7; x <= 7; x++) {
            for (int y = 0; y <= 2; y++) {
                String sh = y == 1 ? "minecraft:bookshelf" : x % 3 == 0 ? St.barrel(Dir.SOUTH, false) : "minecraft:dark_oak_planks";
                c.set(x, y, -11, sh);
            }
            c.set(x, 3, -11, St.trapdoor("minecraft:dark_oak_trapdoor", Dir.SOUTH, true, false));
            if (x % 2 == 0) c.set(x, 4, -11, "minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=true,has_bottle_2=false]");
        }
        c.set(-7, 0, -4, St.stairs("minecraft:dark_oak_stairs", Dir.EAST, false));
        Lighting.hanging(c, -4, WALL_TOP + 1, -8, 2, false);
        Lighting.hanging(c, 0, WALL_TOP + 1, -9, 2, false);
        Lighting.invisible(c, -3, 2, -6, 13);
        Lighting.invisible(c, 3, 2, -6, 13);
        Lighting.hanging(c, 4, WALL_TOP + 1, -8, 2, false);
        // Booths down the west wall, a pool table and the jukebox to the east, tables in the middle.
        for (int z : new int[]{-8, -3, 2}) {
            Furniture.chair(c, -11, 0, z, Dir.SOUTH, "minecraft:spruce_stairs", null);
            Furniture.table(c, -11, z + 1, -10, z + 1, 0, "minecraft:dark_oak_slab");
            Furniture.chair(c, -11, 0, z + 2, Dir.NORTH, "minecraft:spruce_stairs", null);
            Furniture.chair(c, -10, 0, z + 2, Dir.NORTH, "minecraft:spruce_stairs", null);
            Furniture.chair(c, -10, 0, z, Dir.SOUTH, "minecraft:spruce_stairs", null);
            c.set(-11, 1, z + 1, St.candle("orange", 1, true));
            Lighting.hanging(c, -10, WALL_TOP + 1, z + 1, 3, false);
        }
        Furniture.poolTable(c, 6, 0, 1, true, "minecraft:dark_oak_planks");
        Lighting.hanging(c, 7, WALL_TOP + 1, 2, 3, false);
        Lighting.hanging(c, 9, WALL_TOP + 1, 2, 3, false);
        Furniture.jukeboxNook(c, 11, 0, -3, Dir.WEST, true);
        Furniture.sideTable(c, -3, 0, 4, "minecraft:dark_oak_fence", "red");
        Furniture.sideTable(c, 2, 0, 5, "minecraft:dark_oak_fence", "red");
        for (int[] ch : new int[][]{{-4, 4, 0}, {-3, 3, 1}, {1, 5, 0}, {2, 4, 1}}) {
            Furniture.chair(c, ch[0], 0, ch[1], ch[2] == 0 ? Dir.EAST : Dir.SOUTH, "minecraft:spruce_stairs", null);
        }
        Lighting.hanging(c, 0, WALL_TOP + 1, 3, 2, false);
        c.decor(Decor.painting(-6, 2, 7, Dir.NORTH, "wanderer"));
        Clutter.room(c, new Box(-11, 0, -11, 11, 2, 7), Clutter.Kind.BAR, 0.45, 0.08, true, new Box(-1, 0, -10, 1, 1, 7), s + 8);
        c.carve(ASH_SPOT.x(), 0, ASH_SPOT.z());
        c.carve(ASH_SPOT.x(), 1, ASH_SPOT.z());
        // The yard: picnic tables, a woodpile, the red door's frame by the landing, oaks.
        for (int[] t : new int[][]{{-12, 20}, {11, 22}}) {
            Furniture.table(c, t[0], t[1], t[0] + 2, t[1], 0, "minecraft:spruce_slab");
            for (int x = t[0]; x <= t[0] + 2; x++) {
                c.set(x, 0, t[1] - 1, St.slabBottom("minecraft:spruce_slab"));
                c.set(x, 0, t[1] + 1, St.slabBottom("minecraft:spruce_slab"));
            }
        }
        for (int z = 14; z <= 16; z++) for (int y = 0; y <= 1; y++) c.set(16, y, z, St.axis("minecraft:oak_log", "x"));
        int dz = PLOT_DOOR.z();
        for (int y = 0; y <= 2; y++) {
            c.set(-1, y, dz, y == 2 ? "minecraft:red_terracotta" : "minecraft:red_concrete");
            c.set(1, y, dz, y == 2 ? "minecraft:red_terracotta" : "minecraft:red_concrete");
        }
        c.set(0, 2, dz, "minecraft:red_terracotta");
        c.set(-1, 3, dz, Family.BRICKS.stairs(Dir.EAST, false));
        c.set(0, 3, dz, Family.BRICKS.slabBottom());
        c.set(1, 3, dz, Family.BRICKS.stairs(Dir.WEST, false));
        c.carve(0, 0, dz);
        c.carve(0, 1, dz);
        for (int x : new int[]{-2, 2}) {
            c.set(x, 0, dz, St.wall("minecraft:brick_wall"));
            c.set(x, 1, dz, St.lantern(false));
        }
        for (int[] t : new int[][]{{-26, 0}, {26, 8}, {-20, 30}, {22, 34}}) {
            if (g.has(t[0], t[1])) Trees.oak(c, t[0], g.top(t[0], t[1]) + 1, t[1], 2, s + t[0]);
        }
        if (g.has(-16, -18)) Trees.deadOak(c, -16, g.top(-16, -18) + 1, -18, 1, false, s + 9);
    }
}
