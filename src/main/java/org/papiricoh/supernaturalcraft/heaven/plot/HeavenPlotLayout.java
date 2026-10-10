package org.papiricoh.supernaturalcraft.heaven.plot;

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
import org.papiricoh.supernaturalcraft.buildkit.Noise;
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

import java.util.ArrayList;
import java.util.List;

/**
 * A hunter's own Heaven (v0.18, pure): a floating island built as a real, crafted place. The origin is the plot's centre at
 * {@code HeavenDimension.PLOT_Y} ({@code y = 0} the first air above the island's main floor).
 * <p><b>Contract</b> (the foundations fixed it; the architect fills in the build): the points below are fixed whatever the seed
 * or style, and the code that writes the plot, lands visitors, stages memories and opens the wing reads them. Moving one means
 * changing the constant here and nowhere else. Zones, in write order: {@code gate_plaza} (written first, synchronously),
 * {@code memory_lane}, {@code home}, {@code road}, {@code wing} (the shell round Naomi's room), {@code stage}, {@code island}
 * (terrain, last). Naomi's room ({@code ReprogrammingRoomLayout}) is written at {@link #WING_ORIGIN} and Zachariah's office
 * ({@code ZachariahOfficeLayout}) at {@link #OFFICE_ORIGIN} by the world work, after the plot.
 * <p>Gate cells are left as air (the code opens the gates); veils, seals and the hearth's and storage's blocks are placed here
 * or, when missing, by the writer.
 * <p>The build (buildkit): a long sky island of meadow over a calcite underside hung with dripstone, a land bridge west to the
 * wing's walled garden and east along the old road, and a cloud causeway to the round stage island. South, the gate plaza: a
 * paved round with benches and lamps before the gate's quartz portal. Then the memory lane: a pale stone walk with flower borders,
 * lamps and trees, twelve little hip-roofed shrine pavilions alternating left and right, each with its veil in a doorway facing
 * the walk. North, the house in its owner's style ({@link Style}): a hunter's Lawrence farmhouse (cream siding over a brick
 * plinth, slate roof, a deep porch), an angel's white chapel cottage (calcite and quartz, a steep copper roof, lancet windows, a
 * bell-cote), or a demon's dark brick townhouse (blackstone and nether brick, a mansard roof, red glass, an iron railing). Inside:
 * a hall, the great room round the hearth, the trophy hall's pedestals, a storeroom, a bedroom.
 */
public final class HeavenPlotLayout {

    /** The house's look follows its owner: a Lawrence farmhouse for a hunter, a white chapel cottage for an angel, a dark townhouse for a demon. */
    public enum Style { HUNTER, ANGEL, DEMON }

    /** Where a visitor through a gate lands, facing north (towards the house). */
    public static final LayoutPoint LANDING = new LayoutPoint(0, 0, 62);
    /** The plot's own gate out (a frame of light, 3 wide, 4 tall, centred here, facing south). */
    public static final LayoutPoint EXIT_GATE = new LayoutPoint(0, 0, 70);
    /** The memory shrines' veils, chronological from the gate to the house (each veil a 1x2 doorway at the point). */
    public static final List<LayoutPoint> SHRINES = shrines();
    /** The house's front door (sealed by a celestial seal until the home is unlocked). */
    public static final LayoutPoint HOME_DOOR = new LayoutPoint(0, 0, -30);
    /** The hearth (the rest block). */
    public static final LayoutPoint HEARTH = new LayoutPoint(0, 0, -40);
    /** The storage room's chests and barrels (one block each). */
    public static final List<LayoutPoint> STORAGE = List.of(new LayoutPoint(8, 0, -44), new LayoutPoint(9, 0, -44), new LayoutPoint(10, 0, -44));
    /** Trophy pedestals in the trophy hall (busts go on top, one block above). */
    public static final List<LayoutPoint> TROPHIES = trophies();
    /** The red door at the end of the road (into Ash's Roadhouse). */
    public static final LayoutPoint ROAD_DOOR = new LayoutPoint(70, 0, 0);
    /** The clinical wing's door (sealed until enough memories are gathered). */
    public static final LayoutPoint WING_DOOR = new LayoutPoint(-58, 0, 0);
    /** Origin of Naomi's room (its own {@code y = 0} sits on this plot's {@code y = 0}). */
    public static final LayoutPoint WING_ORIGIN = new LayoutPoint(-88, 0, 0);
    /** Origin of Zachariah's office, high above the wing. */
    public static final LayoutPoint OFFICE_ORIGIN = new LayoutPoint(-88, 48, -110);
    /** Centre of the memory stage (a disc of radius {@code MemoryScenes.RADIUS}) and where a visitor stands when a memory begins. */
    public static final LayoutPoint STAGE_CENTER = new LayoutPoint(96, 0, -96);
    public static final LayoutPoint STAGE_ENTRY = new LayoutPoint(96, 0, -72);
    /** Half the island's extent: nothing of the plot reaches further from its centre. */
    public static final int RADIUS = 130;

    /** The stage's floor radius (the scenes' {@code MemoryScenes.RADIUS}) and the room kept clear above it. */
    static final int STAGE_R = 26, STAGE_H = 24;
    /** The house's walls (inclusive) and their top row. */
    static final Box HOUSE = new Box(-13, 0, -54, 13, 0, -30);
    static final int WALL_TOP = 5;
    /** Naomi's room's footprint round {@link #WING_ORIGIN} (the room writes it; the plot leaves it alone). */
    static final int ROOM_R = 20;

    private HeavenPlotLayout() {
    }

    /** The whole plot for this seed and owner's style. */
    public static LayoutPlan plan(long seed, Style style) {
        return canvas(seed, style).toPlan();
    }

    /** The plot as a finished kit canvas (tests and the layout dump). */
    public static Canvas canvas(long seed, Style style) {
        int s = (int) (seed ^ (seed >>> 32)) * 31 + style.ordinal();
        Canvas c = new Canvas("heaven_plot_" + style.name().toLowerCase(java.util.Locale.ROOT));
        c.zone("gate_plaza", new Box(-16, -8, 50, 16, 14, 84));
        c.zone("memory_lane", new Box(-34, -8, -36, 34, 18, 64));
        c.zone("home", new Box(-34, -8, -66, 34, 32, -24));
        c.zone("road", new Box(8, -8, -16, 88, 14, 16));
        c.zone("wing", new Box(-126, -8, -42, -32, 16, 46));
        c.zone("stage", new Box(56, -8, -134, 134, 14, -56));
        c.zone("island", new Box(-RADIUS, -48, -RADIUS, RADIUS, 32, RADIUS));
        Look look = Look.of(style);
        Ground[] ground = new Ground[1];
        c.inZone("island", () -> ground[0] = island(c, s, look));
        Ground g = ground[0];
        c.inZone("gate_plaza", () -> plaza(c, g, look, s));
        c.inZone("memory_lane", () -> lane(c, g, look, s));
        c.inZone("home", () -> House.build(c, g, look, s));
        c.inZone("road", () -> road(c, g, look, s));
        c.inZone("wing", () -> wing(c, g, look, s));
        c.inZone("stage", () -> stage(c, g, look, s));
        c.inZone("island", () -> {
            causeway(c, look, s);
            Scatter.flora(c, g, null, 0.45, 1, s + 90, look.meadow);
            Scatter.prune(c);
        });
        c.anchor("landing", LANDING.x(), LANDING.y(), LANDING.z());
        c.anchor("exit_gate", EXIT_GATE.x(), EXIT_GATE.y(), EXIT_GATE.z());
        c.anchor("home_door", HOME_DOOR.x(), HOME_DOOR.y(), HOME_DOOR.z());
        c.anchor("hearth", HEARTH.x(), HEARTH.y(), HEARTH.z());
        c.anchor("road_door", ROAD_DOOR.x(), ROAD_DOOR.y(), ROAD_DOOR.z());
        c.anchor("wing_door", WING_DOOR.x(), WING_DOOR.y(), WING_DOOR.z());
        c.anchor("stage_center", STAGE_CENTER.x(), STAGE_CENTER.y(), STAGE_CENTER.z());
        c.anchor("stage_entry", STAGE_ENTRY.x(), STAGE_ENTRY.y(), STAGE_ENTRY.z());
        return c.finish();
    }

    // --- the owner's look -------------------------------------------------------------------------------------------------

    /** What changes with the style beyond the house: paving, lamp posts, the shrines' stone and roofs, trees, flowers. */
    record Look(Style style, Brush paving, Brush pavingEdge, Family stone, String pillar, Family shrineRoof, String lampPost, String lampBase,
                boolean soul, Object[] meadow, Object[] borders) {

        static Look of(Style s) {
            Object[] meadow = {"minecraft:short_grass", 30, "minecraft:tall_grass", 8, "minecraft:fern", 4, "minecraft:oxeye_daisy", 3,
                    "minecraft:azure_bluet", 3, "minecraft:cornflower", 2, "minecraft:dandelion", 2, "minecraft:poppy", 1, "minecraft:pink_petals", 2,
                    "minecraft:lily_of_the_valley", 2, "minecraft:moss_carpet", 2};
            return switch (s) {
                case HUNTER -> new Look(s,
                        Palette.patches2(11, 2, "minecraft:polished_andesite", 4, "minecraft:andesite", 2, "minecraft:stone_bricks", 2, "minecraft:cobblestone", 1),
                        Palette.patches2(12, 1.5, "minecraft:gravel", 2, "minecraft:coarse_dirt", 2, "minecraft:moss_block", 1, "minecraft:cobblestone", 1),
                        Family.STONE_BRICKS, St.axis("minecraft:stripped_spruce_log", "y"), Family.DEEPSLATE_TILES, "minecraft:spruce_fence",
                        "minecraft:cobblestone", false, meadow,
                        new Object[]{"minecraft:oxeye_daisy", 3, "minecraft:cornflower", 3, "minecraft:poppy", 2, "minecraft:peony", 2, "minecraft:short_grass", 4});
                case ANGEL -> new Look(s,
                        Palette.patches2(21, 2, "minecraft:calcite", 4, "minecraft:polished_diorite", 3, "minecraft:smooth_quartz", 1),
                        Palette.patches2(22, 1.5, "minecraft:diorite", 2, "minecraft:calcite", 2, "minecraft:moss_block", 1, "minecraft:white_concrete_powder", 1),
                        Family.SMOOTH_QUARTZ, St.axis("minecraft:quartz_pillar", "y"), Family.OXIDIZED_CUT_COPPER, "minecraft:diorite_wall",
                        "minecraft:quartz_bricks", false, meadow,
                        new Object[]{"minecraft:white_tulip", 3, "minecraft:lily_of_the_valley", 3, "minecraft:azure_bluet", 3, "minecraft:lilac", 2,
                                "minecraft:oxeye_daisy", 2});
                case DEMON -> new Look(s,
                        Palette.patches2(31, 2, "minecraft:polished_blackstone_bricks", 4, "minecraft:polished_blackstone", 2, "minecraft:cracked_polished_blackstone_bricks", 1),
                        Palette.patches2(32, 1.5, "minecraft:blackstone", 2, "minecraft:coarse_dirt", 2, "minecraft:basalt[axis=y]", 1, "minecraft:gravel", 1),
                        Family.POLISHED_BLACKSTONE_BRICKS, St.axis("minecraft:polished_basalt", "y"), Family.BLACKSTONE, "minecraft:polished_blackstone_wall",
                        "minecraft:polished_blackstone", true, meadow,
                        new Object[]{"minecraft:red_tulip", 3, "minecraft:poppy", 3, "minecraft:rose_bush", 2, "minecraft:allium", 2, "minecraft:fern", 2});
            };
        }
    }

    // --- the island -------------------------------------------------------------------------------------------------------

    private static Ground island(Canvas c, int s, Look look) {
        Terrain.Mask shape = Terrain.Mask.union(
                Terrain.Mask.blob(0, 8, 34, 76, 0.1, s + 1),
                Terrain.Mask.strip(10, 2, 80, 0, 18, s + 2),
                Terrain.Mask.strip(-10, 2, -64, 2, 22, s + 3),
                Terrain.Mask.blob(-88, 4, 36, 40, 0.1, s + 4),
                Terrain.Mask.blob(STAGE_CENTER.x(), STAGE_CENTER.z(), 31, 31, 0.06, s + 5));
        Terrain.Island spec = Terrain.island(0, 0, 40, -1, 2, 22, s + 6);
        Ground g = Terrain.island(c, spec, shape, 16);
        Brush grass = Palette.patches2(s + 7, 5, "minecraft:grass_block", 14, "minecraft:moss_block", 1, "minecraft:podzol", 1);
        Brush soil = Brush.of("minecraft:dirt");
        Terrain.level(c, g, new Box(-16, 0, 50, 16, 0, 84), -1, grass, soil, 3);
        Terrain.level(c, g, new Box(-15, 0, -60, 15, 0, 50), -1, grass, soil, 4);
        Terrain.level(c, g, new Box(10, 0, -6, 80, 0, 6), -1, grass, soil, 3);
        Terrain.level(c, g, new Box(-124, 0, -36, -50, 0, 42), -1, grass, soil, 2);
        Terrain.level(c, g, new Box(STAGE_CENTER.x() - 34, 0, STAGE_CENTER.z() - 34, STAGE_CENTER.x() + 34, 0, STAGE_CENTER.z() + 34), -1, grass, soil, 2);
        return g;
    }

    // --- the gate plaza ---------------------------------------------------------------------------------------------------

    private static void plaza(Canvas c, Ground g, Look look, int s) {
        int cx = 0, cz = 66, r = 11;
        for (int x = cx - r - 1; x <= cx + r + 1; x++) {
            for (int z = cz - r - 1; z <= cz + r + 1; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d > r + 0.5) continue;
                String top = d > r - 0.7 ? look.stone.block() : (int) Math.floor(d) % 4 == 3 ? look.pavingEdge.at(x, -1, z) : look.paving.at(x, -1, z);
                if (d < 2.5) top = (int) d == 2 ? look.stone.block() : "supernaturalcraft:cloud_bricks";
                c.set(x, -1, z, top);
                c.set(x, -2, z, "minecraft:stone");
                c.set(x, -3, z, "minecraft:stone");
                for (int y = 0; y <= 3; y++) c.carve(x, y, z);
                g.set(x, z, -1, g.inner(x, z));
            }
        }
        // Benches round the paving facing in, lamp posts between them, planters.
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + Math.PI / 8;
            int x = cx + (int) Math.round(Math.cos(a) * (r - 1.5)), z = cz + (int) Math.round(Math.sin(a) * (r - 1.5));
            if (Math.abs(x - cx) <= 2 && (z > cz || z < cz)) continue;
            Dir in = Dir.toward(cx - x, cz - z);
            if (k % 2 == 0) {
                Furniture.sofa(c, x - in.cw().dx, 0, z - in.cw().dz, in, 3, look.stone.stairs(), look.style == Style.DEMON ? "minecraft:dark_oak_trapdoor" : "minecraft:birch_trapdoor");
            } else {
                Lighting.lampPost(c, x, 0, z, 3, look.lampBase, look.lampPost, null, look.soul);
            }
        }
        // The gate's portal: quartz-family piers, a lintel, the 3 x 4 gate cells left as air, steps up to it.
        int gz = EXIT_GATE.z();
        for (int y = 0; y <= 4; y++) {
            for (int x : new int[]{-3, -2, 2, 3}) c.set(x, y, gz, y == 0 ? look.stone.block() : Math.abs(x) == 3 && y == 4 ? look.stone.block() : look.pillar);
        }
        for (int x = -3; x <= 3; x++) {
            c.set(x, 5, gz, Math.abs(x) <= 1 ? "minecraft:chiseled_quartz_block" : look.stone.block());
            c.set(x, 6, gz, look.stone.slabBottom());
            c.set(x, 4, gz + 1, look.stone.stairs(Dir.NORTH, true));
            c.set(x, 4, gz - 1, look.stone.stairs(Dir.SOUTH, true));
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 3; y++) c.carve(x, y, gz);
            c.set(x, 4, gz, look.stone.block());
        }
        c.anchor("plaza_centre", cx, 0, cz);
    }

    // --- the memory lane ---------------------------------------------------------------------------------------------------

    private static void lane(Canvas c, Ground g, Look look, int s) {
        Paths.Style walk = new Paths.Style(5, look.paving, look.pavingEdge, null, null, look.lampPost, 12, look.lampBase, look.borders);
        Paths.path(c, g, List.of(new int[]{0, 56}, new int[]{0, 30}, new int[]{0, 0}, new int[]{0, -28}), walk, s + 20);
        for (int i = 0; i < SHRINES.size(); i++) {
            LayoutPoint p = SHRINES.get(i);
            int side = p.x() < 0 ? -1 : 1;
            Paths.path(c, g, List.of(new int[]{side * 2, p.z()}, new int[]{p.x() - side, p.z()}),
                    new Paths.Style(2, look.paving, look.paving, null, null, null, 0, null, null), s + 21 + i);
            shrine(c, p, side, look, s + 40 + i);
        }
        // Trees between the shrines' rows, set back from the walk.
        for (int i = 0; i < 6; i++) {
            int z = 45 - i * 12;
            for (int side : new int[]{-1, 1}) {
                int x = side * (20 + Noise.pick(i, side, 0, s + 3, 4)), zz = z + side * 3;
                if (!g.has(x, zz)) continue;
                int y = g.top(x, zz) + 1;
                switch (look.style) {
                    case HUNTER -> {
                        if ((i + (side > 0 ? 1 : 0)) % 2 == 0) Trees.oak(c, x, y, zz, 1, s + 60 + i);
                        else Trees.birch(c, x, y, zz, 1, s + 60 + i);
                    }
                    case ANGEL -> {
                        if ((i + (side > 0 ? 1 : 0)) % 2 == 0) Trees.cherry(c, x, y, zz, 1, s + 60 + i);
                        else Trees.birch(c, x, y, zz, 2, s + 60 + i);
                    }
                    case DEMON -> {
                        if ((i + (side > 0 ? 1 : 0)) % 2 == 0) Trees.deadOak(c, x, y, zz, 1, i % 3 == 0, s + 60 + i);
                        else Trees.spruce(c, x, y, zz, 1, s + 60 + i);
                    }
                }
            }
        }
    }

    /**
     * A shrine pavilion: its front wall on the walk side holds the veil's doorway (left as air) under a stepped head, corner
     * pillars, side walls with a slit window, a back niche with a lantern, a hip roof.
     */
    private static void shrine(Canvas c, LayoutPoint p, int side, Look look, int s) {
        int fx = p.x(), z0 = p.z() - 2, z1 = p.z() + 2, bx = fx + side * 4;
        Box fp = Box.of(fx, 0, z0, bx, 0, z1);
        Brush wall = look.style == Style.DEMON
                ? Palette.patches(s, 2, "minecraft:nether_bricks", 3, "minecraft:polished_blackstone_bricks", 2, "minecraft:cracked_nether_bricks", 1)
                : look.style == Style.ANGEL ? Palette.patches(s, 2, "minecraft:calcite", 3, "minecraft:smooth_quartz", 2, "minecraft:quartz_bricks", 1)
                : Palette.patches(s, 2, "minecraft:stone_bricks", 3, "minecraft:mossy_stone_bricks", 1, "minecraft:cracked_stone_bricks", 1);
        for (int x = Math.min(fx, bx); x <= Math.max(fx, bx); x++) {
            for (int z = z0; z <= z1; z++) {
                c.set(x, -1, z, look.stone.block());
                c.set(x, -2, z, "minecraft:stone");
                boolean edge = x == fx || x == bx || z == z0 || z == z1;
                boolean corner = (x == fx || x == bx) && (z == z0 || z == z1);
                for (int y = 0; y <= 3; y++) {
                    if (corner) c.set(x, y, z, look.pillar);
                    else if (edge) c.set(x, y, z, wall.at(x, y, z));
                    else c.carve(x, y, z);
                }
            }
        }
        // The doorway: the veil's 1 x 2 at the point, a stepped head over it.
        c.carve(fx, 0, p.z());
        c.carve(fx, 1, p.z());
        c.set(fx, 2, p.z() - 1, look.stone.stairs(Dir.NORTH, true));
        c.set(fx, 2, p.z() + 1, look.stone.stairs(Dir.SOUTH, true));
        c.set(fx, 2, p.z(), look.stone.slabTop());
        c.set(fx - side, -1, p.z(), look.stone.block());
        // Slits in the sides, a lit niche at the back with a cushion before it.
        for (int z : new int[]{z0, z1}) c.set(fx + side * 2, 1, z, St.pane(look.style == Style.DEMON ? "minecraft:red_stained_glass_pane" : "minecraft:white_stained_glass_pane"));
        c.set(bx - side, 0, p.z(), look.stone.block());
        c.set(bx - side, 1, p.z(), look.soul ? St.soulLantern(false) : St.lantern(false));
        c.set(fx + side * 2, 0, p.z(), St.carpet(look.style == Style.DEMON ? "red" : look.style == Style.ANGEL ? "white" : "brown"));
        Lighting.invisible(c, fx + side * 2, 3, p.z(), 13);
        Roofs.Style roof = new Roofs.Style(look.shrineRoof, look.stone, wall, 1, Roofs.Pitch.STEEP, 0, true, null, false);
        Roofs.hip(c, fp, 3, roof);
    }

    // --- the road to the red door -------------------------------------------------------------------------------------------

    private static void road(Canvas c, Ground g, Look look, int s) {
        Paths.Style dirt = new Paths.Style(4,
                Palette.patches2(s + 50, 1.5, "minecraft:dirt_path", 6, "minecraft:coarse_dirt", 2, "minecraft:packed_mud", 1),
                Palette.patches2(s + 51, 1.5, "minecraft:coarse_dirt", 3, "minecraft:gravel", 2, "minecraft:dirt_path", 1),
                null, null, "minecraft:spruce_fence", 14, "minecraft:cobblestone",
                new Object[]{"minecraft:short_grass", 6, "minecraft:tall_grass", 3, "minecraft:dandelion", 1, "minecraft:cornflower", 1});
        Paths.path(c, g, List.of(new int[]{17, 3}, new int[]{35, 1}, new int[]{55, 0}, new int[]{ROAD_DOOR.x() - 2, 0}), dirt, s + 52);
        // A split-rail fence along the road's north side, leaning and broken in places.
        for (int x = 22; x <= 62; x++) {
            if (Noise.chance(x, 0, 5, s + 53, 0.15) || !g.has(x, 5)) continue;
            int t = g.top(x, 5);
            String here = c.get(x, t + 1, 5);
            if (here != null && !St.isAir(here) && !org.papiricoh.supernaturalcraft.buildkit.Kinds.soilPlant(here)) continue;
            c.set(x, t + 1, 5, St.fence(x % 4 == 0 ? "minecraft:spruce_fence" : "minecraft:oak_fence"));
            c.carve(x, t + 2, 5);
        }
        // The red door: a freestanding painted frame on a stone stoop, the doorway itself left as air for the gate.
        int x = ROAD_DOOR.x();
        for (int dx = -2; dx <= 2; dx++) for (int dz = -3; dz <= 3; dz++) {
            c.set(x + dx, -1, dz, Math.abs(dz) == 3 || Math.abs(dx) == 2 ? "minecraft:stone_bricks" : "minecraft:polished_andesite");
            for (int y = 0; y <= 4; y++) c.carve(x + dx, y, dz);
        }
        for (int y = 0; y <= 2; y++) {
            c.set(x, y, -1, y == 2 ? "minecraft:red_terracotta" : "minecraft:red_concrete");
            c.set(x, y, 1, y == 2 ? "minecraft:red_terracotta" : "minecraft:red_concrete");
        }
        c.set(x, 2, 0, "minecraft:red_terracotta");
        c.set(x, 3, -1, Family.BRICKS.stairs(Dir.SOUTH, false));
        c.set(x, 3, 0, Family.BRICKS.slabBottom());
        c.set(x, 3, 1, Family.BRICKS.stairs(Dir.NORTH, false));
        c.carve(x, 0, 0);
        c.carve(x, 1, 0);
        for (int dz : new int[]{-2, 2}) {
            c.set(x, 0, dz, St.wall("minecraft:brick_wall"));
            c.set(x, 1, dz, St.lantern(false));
        }
    }

    // --- the wing: a walled garden round Naomi's room ---------------------------------------------------------------------

    private static boolean inRoom(int x, int z) {
        return Math.abs(x - WING_ORIGIN.x()) <= ROOM_R && Math.abs(z - WING_ORIGIN.z()) <= ROOM_R;
    }

    private static void wing(Canvas c, Ground g, Look look, int s) {
        int x0 = -118, x1 = WING_DOOR.x(), z0 = -26, z1 = 34;
        Brush paving = Palette.checker(1, Brush.of("minecraft:smooth_quartz"), Brush.of("minecraft:calcite"));
        // The garden: lawn, the paving of the walks, the wall round it.
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (inRoom(x, z)) continue;
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                if (!g.has(x, z)) {
                    c.set(x, -1, z, "minecraft:smooth_quartz");
                    c.set(x, -2, z, "minecraft:calcite");
                }
                if (edge) {
                    boolean post = (x - x0) % 6 == 0 && (z == z0 || z == z1) || (z - z0) % 6 == 0 && (x == x0 || x == x1);
                    for (int y = 0; y <= 2; y++) c.set(x, y, z, post ? St.axis("minecraft:quartz_pillar", "y") : "minecraft:smooth_quartz");
                    c.set(x, 3, z, post ? "minecraft:chiseled_quartz_block" : Family.SMOOTH_QUARTZ.slabBottom());
                    if (post) c.set(x, 4, z, St.lantern(false));
                    c.set(x, -1, z, "minecraft:quartz_bricks");
                } else {
                    for (int y = 0; y <= 3; y++) c.carve(x, y, z);
                }
            }
        }
        // The gate in the east wall: the seal's 1 x 2 at the door point, a tall frame round it.
        int gz = WING_DOOR.z();
        for (int y = 0; y <= 4; y++) {
            c.set(x1, y, gz - 1, St.axis("minecraft:quartz_pillar", "y"));
            c.set(x1, y, gz + 1, St.axis("minecraft:quartz_pillar", "y"));
        }
        c.set(x1, 2, gz, "minecraft:chiseled_quartz_block");
        c.set(x1, 3, gz, "minecraft:chiseled_quartz_block");
        c.set(x1, 4, gz, Family.SMOOTH_QUARTZ.slabBottom());
        c.carve(x1, 0, gz);
        c.carve(x1, 1, gz);
        // A colonnade walk from the gate round to the room's entrance (its south side).
        int ex = WING_ORIGIN.x(), ez = WING_ORIGIN.z() + 19;
        List<int[]> walk = new ArrayList<>();
        for (int x = x1 - 1; x >= -62; x--) walk.add(new int[]{x, gz});
        for (int z = gz; z <= 26; z++) walk.add(new int[]{-62, z});
        for (int x = -62; x >= ex; x--) walk.add(new int[]{x, 26});
        for (int z = 26; z > ez; z--) walk.add(new int[]{ex, z});
        for (int[] p : walk) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (inRoom(p[0] + dx, p[1] + dz) || p[0] + dx >= x1) continue;
                    c.set(p[0] + dx, -1, p[1] + dz, paving.at(p[0] + dx, -1, p[1] + dz));
                }
            }
        }
        for (int i = 0; i < walk.size(); i += 4) {
            int[] p = walk.get(i);
            for (int[] o : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
                int x = p[0] + o[0], z = p[1] + o[1];
                if (inRoom(x, z) || x >= x1 || isWalk(walk, x, z)) continue;
                for (int y = 0; y <= 2; y++) c.set(x, y, z, St.axis("minecraft:quartz_pillar", "y"));
                c.set(x, 3, z, "minecraft:chiseled_quartz_block");
            }
        }
        // White gardens: low hedges and white flowers along the walls, a reflecting pool by the walk.
        Scatter.flora(c, g, new Box(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1), 0.5, 1, s + 70, "minecraft:white_tulip", 3,
                "minecraft:lily_of_the_valley", 3, "minecraft:short_grass", 6, "minecraft:oxeye_daisy", 2, "minecraft:azure_bluet", 2);
        List<int[]> hedge = new ArrayList<>();
        for (int x = x0 + 2; x < x1 - 3; x++) if (!inRoom(x, z1 - 2) && x % 7 != 0) hedge.add(new int[]{x, z1 - 2});
        Trees.hedge(c, hedge, 0, 2, "minecraft:azalea_leaves", s + 71);
        for (int x = -77; x <= -67; x++) {
            for (int z = 29; z <= 31; z++) {
                boolean rim = x == -77 || x == -67 || z == 29 || z == 31;
                c.set(x, -1, z, rim ? "minecraft:quartz_bricks" : St.water());
                if (!rim) c.set(x, -2, z, "minecraft:calcite");
                if (rim) c.set(x, 0, z, Family.SMOOTH_QUARTZ.slabBottom());
            }
        }
    }

    private static boolean isWalk(List<int[]> walk, int x, int z) {
        for (int[] p : walk) if (Math.abs(p[0] - x) <= 1 && Math.abs(p[1] - z) <= 1) return true;
        return false;
    }

    // --- the stage --------------------------------------------------------------------------------------------------------

    private static void stage(Canvas c, Ground g, Look look, int s) {
        int cx = STAGE_CENTER.x(), cz = STAGE_CENTER.z();
        for (int x = cx - 31; x <= cx + 31; x++) {
            for (int z = cz - 31; z <= cz + 31; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d > 30.5) continue;
                String floor;
                if (d <= STAGE_R + 0.5) {
                    int ring = (int) Math.floor(d);
                    floor = ring % 6 == 5 ? "minecraft:chiseled_quartz_block" : ring % 2 == 0 ? "minecraft:calcite" : "minecraft:polished_diorite";
                    if (d < 2) floor = "supernaturalcraft:cloud_bricks";
                } else {
                    floor = look.paving.at(x, -1, z);
                }
                c.set(x, -1, z, floor);
                c.set(x, -2, z, "minecraft:stone");
                for (int y = 0; y <= 3; y++) c.carve(x, y, z);
            }
        }
        // A ring of columns carrying a circular architrave, open on the south where the causeway arrives.
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI * 2 / 24;
            int x = cx + (int) Math.round(Math.cos(a) * 29), z = cz + (int) Math.round(Math.sin(a) * 29);
            if (z > cz && Math.abs(x - cx) <= 4) continue;
            c.set(x, 0, z, look.stone.block());
            for (int y = 1; y <= 5; y++) c.set(x, y, z, look.pillar);
            c.set(x, 6, z, "minecraft:chiseled_quartz_block");
            c.set(x, 7, z, look.soul ? St.soulLantern(false) : St.lantern(false));
        }
        c.anchor("stage_ring", cx, 0, cz + 29);
    }

    // --- the cloud causeway to the stage ------------------------------------------------------------------------------------

    private static void causeway(Canvas c, Look look, int s) {
        List<double[]> pts = Paths.spline(List.of(new int[]{18, -46}, new int[]{45, -50}, new int[]{75, -58},
                new int[]{STAGE_CENTER.x(), STAGE_CENTER.z() + 31}), 0.3);
        java.util.Map<Long, double[]> near = Paths.near(pts, 3.2);
        for (java.util.Map.Entry<Long, double[]> e : near.entrySet()) {
            int x = Canvas.kx(e.getKey()), z = Canvas.kz(e.getKey());
            double d = e.getValue()[0];
            String here = c.get(x, -1, z);
            if (here != null && !St.isAir(here) && !St.path(here).contains("leaves")) continue;
            if (d <= 1.6) c.set(x, -1, z, "supernaturalcraft:cloud_bricks");
            else if (d <= 2.6) c.set(x, -1, z, "supernaturalcraft:cloud_stone");
            else if (Noise.chance(x, 0, z, s + 80, 0.35)) c.set(x, -1, z, "supernaturalcraft:cloud_stone");
            else continue;
            c.set(x, -2, z, "supernaturalcraft:cloud_stone");
            if (Noise.chance(x, 1, z, s + 81, 0.3)) c.set(x, -3, z, "supernaturalcraft:cloud_stone");
            for (int y = 0; y <= 2; y++) c.carve(x, y, z);
        }
        double next = 6;
        int side = 1;
        for (int i = 1; i < pts.size(); i++) {
            double[] p = pts.get(i), o = pts.get(i - 1);
            if (p[2] < next) continue;
            next += 12;
            double tx = p[0] - o[0], tz = p[1] - o[1], len = Math.hypot(tx, tz);
            int x = (int) Math.round(p[0] - tz / len * 2.4 * side), z = (int) Math.round(p[1] + tx / len * 2.4 * side);
            side = -side;
            String under = c.get(x, -1, z);
            if (under == null || !St.id(under).startsWith("supernaturalcraft:cloud")) continue;
            Lighting.lampPost(c, x, 0, z, 2, "supernaturalcraft:cloud_bricks", look.lampPost.endsWith("_fence") ? "minecraft:diorite_wall" : look.lampPost, null, look.soul);
        }
    }

    // --- points -----------------------------------------------------------------------------------------------------------

    private static List<LayoutPoint> shrines() {
        List<LayoutPoint> out = new ArrayList<>();
        for (int i = 0; i < 12; i++) out.add(new LayoutPoint(i % 2 == 0 ? -9 : 9, 0, 48 - i * 6));
        return List.copyOf(out);
    }

    private static List<LayoutPoint> trophies() {
        List<LayoutPoint> out = new ArrayList<>();
        for (int i = 0; i < 10; i++) out.add(new LayoutPoint(-10 + (i % 5) * 2, 0, i < 5 ? -46 : -50));
        return List.copyOf(out);
    }

    // --- the house ----------------------------------------------------------------------------------------------------------

    /** The owner's house, its rooms and furniture, in the {@link Look}'s style. */
    static final class House {

        private House() {
        }

        static void build(Canvas c, Ground g, Look look, int s) {
            Style st = look.style;
            Brush wall, interior;
            Family plinth, roofFam, trim;
            String corner, glass, shutters;
            Wood wood;
            switch (st) {
                case HUNTER -> {
                    wall = (x, y, z) -> Math.floorMod(y, 2) == 0 ? "minecraft:birch_planks" : "minecraft:smooth_sandstone";
                    // Wainscot of spruce below a stripped rail, birch boards above.
                    interior = Palette.courses(0, 2, Brush.of("minecraft:spruce_planks"),
                            (x, y, z) -> y == 2 ? "minecraft:stripped_spruce_wood[axis=y]" : Math.floorMod(x + z, 2) == 0 ? "minecraft:birch_planks" : "minecraft:stripped_birch_wood[axis=y]",
                            null, 0);
                    plinth = Family.BRICKS;
                    roofFam = Family.DEEPSLATE_TILES;
                    trim = Family.SMOOTH_QUARTZ;
                    corner = St.log("minecraft:stripped_birch_log");
                    glass = "minecraft:glass_pane";
                    shutters = "minecraft:dark_oak_trapdoor";
                    wood = Wood.OAK;
                }
                case ANGEL -> {
                    wall = Palette.patches(s + 2, 3, "minecraft:calcite", 4, "minecraft:smooth_quartz", 2, "minecraft:white_concrete_powder", 1);
                    interior = Palette.patches(s + 3, 3, "minecraft:smooth_quartz", 3, "minecraft:calcite", 2);
                    plinth = Family.POLISHED_DIORITE;
                    roofFam = Family.OXIDIZED_CUT_COPPER;
                    trim = Family.SMOOTH_QUARTZ;
                    corner = St.axis("minecraft:quartz_pillar", "y");
                    glass = "minecraft:white_stained_glass_pane";
                    shutters = null;
                    wood = Wood.BIRCH;
                }
                default -> {
                    wall = Palette.patches(s + 4, 2.5, "minecraft:deepslate_bricks", 4, "minecraft:nether_bricks", 2, "minecraft:polished_blackstone_bricks", 2,
                            "minecraft:cracked_deepslate_bricks", 1);
                    interior = Palette.courses(0, 2, Brush.of("minecraft:mangrove_planks"),
                            (x, y, z) -> y == 2 ? "minecraft:stripped_dark_oak_wood[axis=y]" : Math.floorMod(x + z, 2) == 0 ? "minecraft:dark_oak_planks" : "minecraft:stripped_mangrove_wood[axis=y]",
                            null, 0);
                    plinth = Family.POLISHED_BLACKSTONE;
                    roofFam = Family.DEEPSLATE_TILES;
                    trim = Family.POLISHED_BLACKSTONE;
                    corner = St.log("minecraft:polished_basalt");
                    glass = "minecraft:red_stained_glass_pane";
                    shutters = null;
                    wood = Wood.DARK_OAK;
                }
            }
            // The pad and floors.
            Terrain.level(c, g, HOUSE.growXZ(3), -1, Brush.of("minecraft:grass_block"), Brush.of("minecraft:dirt"), 0);
            Brush boards = Palette.boards(true, Brush.of(wood.planks()), Brush.of(st == Style.DEMON ? "minecraft:mangrove_planks" : "minecraft:spruce_planks"),
                    Brush.of(wood.strippedLog("x")), 5);
            for (int x = HOUSE.x0(); x <= HOUSE.x1(); x++) {
                for (int z = HOUSE.z0(); z <= HOUSE.z1(); z++) {
                    boolean edge = x == HOUSE.x0() || x == HOUSE.x1() || z == HOUSE.z0() || z == HOUSE.z1();
                    c.set(x, -1, z, edge ? plinth.block() : boards.at(x, -1, z));
                    c.set(x, -2, z, "minecraft:stone");
                    if (!edge) for (int y = 0; y <= WALL_TOP; y++) c.carve(x, y, z);
                }
            }
            // Walls: plinth with a skirting, the body, corner posts, a beam of trim under the eaves on the long sides.
            for (Walls.Run r : Walls.around(HOUSE)) {
                Walls.body(c, r, 0, 0, Brush.of(plinth.block()));
                Walls.body(c, r, 1, WALL_TOP, wall);
                Walls.plinth(c, r, 0, 0, plinth, true);
                if (st != Style.HUNTER) {
                    int[] pil = Walls.pillarPositions(r, 4);
                    for (int i = 1; i + 1 < pil.length; i++) for (int y = 1; y <= WALL_TOP; y++) c.set(r.x(pil[i]), y, r.z(pil[i]), corner);
                }
            }
            Walls.cornerPosts(c, HOUSE, 0, WALL_TOP, corner);
            // Windows: pairs along every wall, the front centred on the door.
            Openings.WindowStyle win = new Openings.WindowStyle(glass, null, trim, st == Style.DEMON ? trim : null, shutters,
                    st == Style.HUNTER ? "minecraft:rooted_dirt" : null, false, null);
            int wy = 1, wh = st == Style.ANGEL ? 3 : 2;
            Walls.Run front = new Walls.Run(HOUSE.x0(), HOUSE.z1(), HOUSE.x1(), HOUSE.z1(), Dir.SOUTH);
            Walls.Run back = new Walls.Run(HOUSE.x1(), HOUSE.z0(), HOUSE.x0(), HOUSE.z0(), Dir.NORTH);
            Walls.Run east = new Walls.Run(HOUSE.x1(), HOUSE.z0(), HOUSE.x1(), HOUSE.z1(), Dir.EAST);
            Walls.Run west = new Walls.Run(HOUSE.x0(), HOUSE.z1(), HOUSE.x0(), HOUSE.z0(), Dir.WEST);
            for (int i : new int[]{3, 7, 18, 22}) Openings.window(c, front, i, wy, 2, wh, win);
            for (int i : new int[]{3, 11, 15, 21}) Openings.window(c, back, i, wy, 2, wh, win.withFlowerBox(null));
            for (int i : new int[]{3, 9, 15, 20}) Openings.window(c, east, i, wy, 2, wh, win.withFlowerBox(null));
            for (int i : new int[]{3, 9, 15, 20}) Openings.window(c, west, i, wy, 2, wh, win.withFlowerBox(null));
            // The front door: the seal's 1 x 2 at HOME_DOOR, a frame and a canopy.
            int dx = HOME_DOOR.x(), dz = HOME_DOOR.z();
            for (int y = 0; y <= 2; y++) {
                c.set(dx - 1, y, dz, corner);
                c.set(dx + 1, y, dz, corner);
            }
            c.set(dx, 2, dz, trim.block());
            c.carve(dx, 0, dz);
            c.carve(dx, 1, dz);
            for (int y = 0; y <= 2; y++) c.carve(dx, y, dz + 1);
            // Interior walls: the hall, the great room, the back rooms.
            partitions(c, interior, wood);
            rooms(c, look, wood, s);
            // The front: a porch (hunter), a stone stoop under a hood (angel), a railed stoop (demon).
            switch (st) {
                case HUNTER -> Openings.porch(c, front, 1, 25, 3, -1, 3, 12, 14,
                        new Openings.PorchStyle(Palette.boards(true, Brush.of("minecraft:spruce_planks"), Brush.of("minecraft:oak_planks"), 4),
                                St.log("minecraft:stripped_birch_log"), "minecraft:birch_fence", 4, Family.DEEPSLATE_TILES, Family.STONE_BRICKS, Wood.BIRCH));
                case ANGEL -> {
                    for (int x = -2; x <= 2; x++) for (int z = -29; z <= -28; z++) c.set(x, -1, z, "minecraft:polished_diorite");
                    for (int x = -2; x <= 2; x++) c.set(x, 3, -29, trim.stairs(Dir.NORTH, true));
                    c.set(-2, 0, -29, St.potted("white_tulip"));
                    c.set(2, 0, -29, St.potted("white_tulip"));
                }
                case DEMON -> {
                    for (int x = -3; x <= 3; x++) for (int z = -29; z <= -27; z++) c.set(x, -1, z, "minecraft:polished_blackstone_bricks");
                    for (int z = -29; z <= -27; z++) {
                        c.set(-3, 0, z, St.bars());
                        c.set(3, 0, z, St.bars());
                    }
                    c.set(-3, 1, -27, St.soulLantern(false));
                    c.set(3, 1, -27, St.soulLantern(false));
                    c.set(-3, 0, -27, St.wall("minecraft:polished_blackstone_wall"));
                    c.set(3, 0, -27, St.wall("minecraft:polished_blackstone_wall"));
                }
            }
            // The roof.
            Roofs.Style roof = new Roofs.Style(roofFam, st == Style.ANGEL ? Family.SMOOTH_QUARTZ : st == Style.DEMON ? Family.POLISHED_BLACKSTONE : Wood.SPRUCE.family(),
                    st == Style.ANGEL ? Palette.halfTimber(wall, "minecraft:quartz_pillar[axis=y]", 3) : wall, 1,
                    st == Style.HUNTER ? Roofs.Pitch.HALF : st == Style.ANGEL ? Roofs.Pitch.STEEP : Roofs.Pitch.MANSARD, 2, true,
                    st == Style.DEMON ? null : "minecraft:spruce_trapdoor", true);
            Roofs.Plan plan = new Roofs.Plan();
            switch (st) {
                case HUNTER -> plan.gable(HOUSE, WALL_TOP, true, roof).gable(new Box(-5, 0, -36, 5, 0, -30), WALL_TOP, false, roof);
                case ANGEL -> plan.gable(new Box(-7, 0, -54, 7, 0, -30), WALL_TOP, false, roof)
                        .shed(new Box(-13, 0, -54, -8, 0, -30), WALL_TOP, Dir.EAST, roof.withPitch(Roofs.Pitch.HALF))
                        .shed(new Box(8, 0, -54, 13, 0, -30), WALL_TOP, Dir.WEST, roof.withPitch(Roofs.Pitch.HALF));
                case DEMON -> plan.hip(HOUSE, WALL_TOP, roof);
            }
            plan.draw(c);
            if (st == Style.HUNTER) Palette.swapFamily(c, HOUSE.growXZ(2).withY(WALL_TOP, 30), Family.DEEPSLATE_TILES, Family.COBBLED_DEEPSLATE, 0.14, 2.5, s + 9);
            // The hearth's chimney, up through the roof; a bell-cote on the chapel's front gable.
            Family stack = st == Style.DEMON ? Family.POLISHED_BLACKSTONE_BRICKS : st == Style.ANGEL ? Family.SMOOTH_QUARTZ : Family.BRICKS;
            for (int x = -1; x <= 1; x++) for (int y = 0; y <= WALL_TOP; y++) c.set(x, y, HEARTH.z() - 1, stack.block());
            Roofs.chimney(c, 0, HEARTH.z() - 1, WALL_TOP + 1, Brush.of(stack.block()), stack == Family.SMOOTH_QUARTZ ? Family.QUARTZ : stack, true);
            if (st == Style.ANGEL) belfry(c);
            // The hearth itself, the storeroom's chests.
            c.set(HEARTH.x(), HEARTH.y(), HEARTH.z(), "supernaturalcraft:hearth[facing=south]");
            for (LayoutPoint p : STORAGE) c.set(p.x(), p.y(), p.z(), St.of("chest", "facing", "south", "type", "single", "waterlogged", "false"));
        }

        /** The chapel's bell-cote: quartz piers over the front gable's peak, a bell between them, a little copper cap. */
        private static void belfry(Canvas c) {
            int z = -29, top = c.topAt(0, -30, 40, WALL_TOP);
            int y0 = top + 1;
            for (int y = y0; y <= y0 + 2; y++) {
                c.set(-1, y, z - 1, St.axis("minecraft:quartz_pillar", "y"));
                c.set(1, y, z - 1, St.axis("minecraft:quartz_pillar", "y"));
            }
            c.set(0, y0 + 2, z - 1, "minecraft:bell[attachment=ceiling,facing=north,powered=false]");
            for (int x = -1; x <= 1; x++) c.set(x, y0 + 3, z - 1, "minecraft:chiseled_quartz_block");
            c.set(0, y0 + 4, z - 1, Family.OXIDIZED_CUT_COPPER.slabBottom());
            c.set(0, y0 + 5, z - 1, St.rod("minecraft:lightning_rod", Dir.UP));
            for (int x = -1; x <= 1; x++) c.set(x, y0 - 1, z - 1, Family.SMOOTH_QUARTZ.block());
        }

        /** The inner walls with their doorways. */
        private static void partitions(Canvas c, Brush interior, Wood wood) {
            // Hall | great room, at z = -36; great room | back rooms, at z = -43; trophy hall | east rooms at x = 0; storeroom | bedroom at z = -48.
            for (int x = -12; x <= 12; x++) {
                for (int y = 0; y <= WALL_TOP; y++) {
                    if (Math.abs(x) > 2 || y > 2) c.set(x, y, -36, interior.at(x, y, -36));
                    if (!(x >= -7 && x <= -5 && y <= 2) && !(x >= 2 && x <= 4 && y <= 2)) c.set(x, y, -43, interior.at(x, y, -43));
                }
            }
            for (int z = -53; z <= -44; z++) for (int y = 0; y <= WALL_TOP; y++) if (!(z >= -46 && z <= -45 && y <= 2)) c.set(0, y, z, interior.at(0, y, z));
            for (int x = 1; x <= 12; x++) for (int y = 0; y <= WALL_TOP; y++) if (!(x >= 2 && x <= 3 && y <= 2)) c.set(x, y, -48, interior.at(x, y, -48));
            // Door frames: a beam over each opening.
            for (int x = -2; x <= 2; x++) c.set(x, 3, -36, wood.strippedLog("x"));
            for (int x = -7; x <= -5; x++) c.set(x, 3, -43, wood.strippedLog("x"));
            for (int x = 2; x <= 4; x++) c.set(x, 3, -43, wood.strippedLog("x"));
        }

        /** Furniture and light, room by room. */
        private static void rooms(Canvas c, Look look, Wood wood, int s) {
            boolean demon = look.style == Style.DEMON;
            String stairs = wood.stairsId(), slab = wood.slabId(), trap = wood.trapdoorId(), fence = wood.fenceId();
            String rug = demon ? "red" : look.style == Style.ANGEL ? "light_blue" : "brown";
            // The hall: a runner rug over hidden lights, benches, plants, a chandelier.
            for (int x = -2; x <= 2; x++) {
                for (int z = -35; z <= -31; z++) {
                    if (x == 0 && z == -33) Lighting.underCarpet(c, x, 0, z, "minecraft:ochre_froglight", rug);
                    else if (Math.abs(x) <= 1) c.set(x, 0, z, St.carpet(rug));
                }
            }
            Furniture.sofa(c, -12, 0, -32, Dir.EAST, 3, stairs, trap);
            Furniture.sofa(c, 12, 0, -34, Dir.WEST, 3, stairs, trap);
            c.set(-7, 0, -35, St.potted("fern"));
            c.set(7, 0, -35, St.potted("azalea_bush"));
            Lighting.hanging(c, 0, WALL_TOP + 1, -33, 2, look.soul);
            Lighting.hanging(c, -8, WALL_TOP + 1, -33, 2, look.soul);
            Lighting.hanging(c, 8, WALL_TOP + 1, -33, 2, look.soul);
            // The great room: sofas round the hearth, a rug, shelves, a dining table, a chandelier.
            for (int x = -3; x <= 3; x++) for (int z = -39; z <= -37; z++) c.setIfAir(x, 0, z, St.carpet(rug));
            Lighting.underCarpet(c, 0, 0, -38, "minecraft:ochre_froglight", rug);
            Furniture.sofa(c, -4, 0, -38, Dir.EAST, 2, stairs, trap);
            Furniture.sofa(c, 4, 0, -37, Dir.WEST, 2, stairs, trap);
            Furniture.bookshelves(c, -12, -42, Dir.EAST, 6, 0, 2, Dir.SOUTH, true, wood.strippedLog("y"), 3);
            Furniture.diningSet(c, 7, -40, 10, -40, 0, slab, stairs);
            c.set(8, 1, -40, St.candle("white", 3, true));
            c.set(10, 1, -40, St.potted("red_tulip"));
            Lighting.chandelier(c, 0, WALL_TOP + 1, -38, 3, fence, "white");
            Lighting.hanging(c, 8, WALL_TOP + 1, -38, 2, look.soul);
            Lighting.hanging(c, -8, WALL_TOP + 1, -38, 2, look.soul);
            // The trophy hall: two rows of pedestals (busts go on top), banners' places on the walls, a carpet down the middle.
            for (LayoutPoint p : TROPHIES) c.set(p.x(), p.y(), p.z(), look.stone.block());
            for (int x = -11; x <= -1; x++) c.setIfAir(x, 0, -48, St.carpet(demon ? "black" : "red"));
            Lighting.hanging(c, -6, WALL_TOP + 1, -46, 2, look.soul);
            Lighting.hanging(c, -6, WALL_TOP + 1, -50, 2, look.soul);
            Lighting.hanging(c, -10, WALL_TOP + 1, -52, 2, look.soul);
            Lighting.hanging(c, -2, WALL_TOP + 1, -52, 2, look.soul);
            c.decor(Decor.painting(-6, 2, -53, Dir.SOUTH, demon ? "skull_and_roses" : "wanderer"));
            // The storeroom: barrels and shelves round the chests.
            for (int x = 6; x <= 12; x++) if (x < 8 || x > 10) c.set(x, 0, -44, St.barrel(Dir.UP, false));
            for (int z = -47; z <= -45; z++) Furniture.cabinet(c, 12, 0, z, Dir.WEST, wood.planks(), null, true);
            Lighting.hanging(c, 9, WALL_TOP + 1, -46, 2, look.soul);
            Lighting.hanging(c, 3, WALL_TOP + 1, -46, 2, look.soul);
            // The bedroom: a bed, a desk, a wardrobe, a rug.
            Furniture.bed(c, 9, 0, -51, Dir.NORTH, demon ? "black" : look.style == Style.ANGEL ? "white" : "red", St.barrel(Dir.UP, false), true);
            Furniture.desk(c, 3, 0, -52, Dir.NORTH, slab, stairs, false);
            for (int z = -53; z <= -51; z++) c.set(12, 0, z, z == -52 ? St.barrel(Dir.WEST, false) : wood.planks());
            for (int x = 4; x <= 7; x++) c.setIfAir(x, 0, -50, St.carpet(rug));
            Lighting.hanging(c, 6, WALL_TOP + 1, -51, 2, look.soul);
            Clutter.room(c, new Box(-12, 0, -42, 12, 2, -37), Clutter.Kind.LIVING, 0.4, 0.1, true, new Box(-3, 0, -42, 3, 1, -37), s + 30);
            // Fill light: invisible light blocks hung in a grid under the ceiling where the air is free.
            for (int x = HOUSE.x0() + 1; x < HOUSE.x1(); x++) {
                for (int z = HOUSE.z0() + 1; z < HOUSE.z1(); z++) {
                    if (Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0) Lighting.invisible(c, x, 3, z, 15);
                }
            }
            Lighting.invisible(c, 1, 3, -46, 15);
            Clutter.room(c, new Box(1, 0, -53, 12, 2, -49), Clutter.Kind.BEDROOM, 0.5, 0.1, true, null, s + 31);
        }
    }
}
