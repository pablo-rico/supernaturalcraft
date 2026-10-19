package org.papiricoh.supernaturalcraft.coven.layout;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
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
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.buildkit.Wood;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.List;

/**
 * Rowena's house (v0.21, pure): a Victorian witch's townhouse at a forest's edge, one per world. Origin = the middle of the
 * ground-floor hall on its floor ({@code y = 0} the first air above it); drawn for {@code q = 0} with the front door to the south,
 * +Z, and turned by {@code q} clockwise quarter turns ({@link #rotate}, the same as {@code CabinLayout.rotate}; points with
 * {@link #at}). Not seeded: one design.
 * <p>The build (buildkit): two storeys of dark brick over a deepslate plinth, clapboarded above a belt course, under a steep
 * slate roof with dormers, barge boards and a chimney; a round turret with a pointed roof on the south-east corner; a porch
 * over the door. Ground floor: the hall with the stair up, a parlour with a soul-fire hearth and a sofa, the library (shelves
 * floor to ceiling, her desk), the spell room (cauldron, candles, amethyst, herbs drying under the joists) and the dining room
 * opening into the turret. Upstairs: her bedroom (a canopied divan, no bed), a study, the turret room. Outside: a wide front
 * garden ({@link #FRONT}: lawn, flowerbeds, a ring of flagstones in its middle, lamp posts, hedges down its sides) behind an iron
 * fence and gate, with the façade and the porch at its head; an herb garden behind with a watered bed, and in its far corner a
 * gazebo with the spell bowl. <b>Contract</b>: the fight with Rowena (P6) is in the front garden — no block entity lies within
 * {@link #ARENA_RADIUS} (horizontally) of {@link #ARENA_CENTRE} (a fight there restores everything in that radius), and the
 * ground there is open lawn; {@link #BOWL} is the {@code supernaturalcraft:spell_bowl}, outside it.
 */
public final class RowenaHouseLayout {

    public static final int RADIUS = 40, HEIGHT = 22, DEPTH = 5;
    public static final int ARENA_RADIUS = 12;
    /** Outside the front gate, on the path. */
    public static final LayoutPoint ENTRY = new LayoutPoint(0, 0, 39);
    /** In the library beside her desk. */
    public static final LayoutPoint NPC_HOME = new LayoutPoint(-5, 0, -3);
    /** The spell bowl on its pedestal in the gazebo (a block entity, beyond the arena). */
    public static final LayoutPoint BOWL = new LayoutPoint(13, 1, -11);
    /** Before the bowl, in the gazebo. */
    public static final LayoutPoint BOWL_STAND = new LayoutPoint(12, 0, -11);
    /** On the path through the herb garden. */
    public static final LayoutPoint GARDEN = new LayoutPoint(-5, 0, -12);
    /** In the middle of the front garden, on the ring of flagstones (v0.21 P6: her fight is outdoors). */
    public static final LayoutPoint ARENA_CENTRE = new LayoutPoint(0, 0, 22);
    /** The front garden: from the porch to the fence ({@link #FENCE_Z}), hedged at its sides. */
    static final Box FRONT = new Box(-18, 0, 7, 18, 0, 40);
    static final int FENCE_Z = 36;

    static final Box HOUSE = new Box(-7, 0, -6, 7, 0, 6);
    static final int GF_TOP = 4, FF = 5, WALL_TOP = 9;
    /** The turret's middle and the box its roof covers. */
    static final int TX = 8, TZ = 7;
    static final Box TURRET = new Box(TX - 2, 0, TZ - 2, TX + 2, 0, TZ + 2);
    static final int TURRET_TOP = 15;
    static final Box GAZEBO = new Box(11, 0, -13, 15, 0, -9);
    static final Box HERBS = new Box(-11, 0, -15, -2, 0, -9);
    static final int SEED = 2117, PATCH = 17;

    private RowenaHouseLayout() {
    }

    /** Local X/Z turned by {@code q} clockwise quarter turns (as {@code CabinLayout.rotate} and {@link Canvas#turn}). */
    public static int[] rotate(int x, int z, int q) {
        return switch (Math.floorMod(q, 4)) {
            case 1 -> new int[]{-z, x};
            case 2 -> new int[]{-x, -z};
            case 3 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    public static LayoutPoint at(LayoutPoint p, int q) {
        int[] r = rotate(p.x(), p.z(), q);
        return new LayoutPoint(r[0], p.y(), r[1]);
    }

    public static LayoutPlan plan(int q) {
        return canvas(q).toPlan();
    }

    /** The house as a finished kit canvas (tests and the layout dump). */
    public static Canvas canvas(int q) {
        Canvas south = drawn();
        Canvas c = new Canvas("coven_rowena_house");
        c.zone("house", new Box(-RADIUS, -DEPTH, -RADIUS, RADIUS, HEIGHT, RADIUS));
        c.inZone("house", () -> c.stamp(south, 0, 0, 0, Math.floorMod(q, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    // --- drawing -------------------------------------------------------------------------------------------------------------

    static final Brush BRICK = Palette.patches(SEED + 1, 2, "minecraft:bricks", 6, "minecraft:mud_bricks", 1, "minecraft:granite", 1,
            "minecraft:polished_granite", 1);
    static final Brush BOARDS = Palette.mix(SEED + 2, "minecraft:dark_oak_planks", 6, "minecraft:spruce_planks", 2,
            "minecraft:stripped_dark_oak_wood[axis=y]", 1);
    static final Brush PLINTH = Palette.patches(SEED + 3, 2, "minecraft:polished_deepslate", 3, "minecraft:deepslate_bricks", 3,
            "minecraft:cracked_deepslate_bricks", 1);
    static final Brush FLOOR = Palette.boards(true, Brush.of("minecraft:dark_oak_planks"), Brush.of("minecraft:spruce_planks"),
            Brush.of("minecraft:stripped_dark_oak_wood[axis=y]"), 4);
    static final Brush PANEL = Palette.mix(SEED + 4, "minecraft:stripped_dark_oak_wood[axis=y]", 3, "minecraft:dark_oak_planks", 2,
            "minecraft:spruce_planks", 1);
    static final String POST = "minecraft:stripped_dark_oak_log[axis=y]";

    static Canvas drawn() {
        int s = SEED;
        Canvas c = new Canvas("rowena_south");
        Brush lawn = Palette.patches2(s + 5, 4, "minecraft:grass_block", 14, "minecraft:moss_block", 1, "minecraft:podzol", 1,
                "minecraft:coarse_dirt", 1);
        Ground g = Patch.draw(c, s + 6, PATCH, lawn, Brush.of("minecraft:dirt"), 13);
        Patch.pad(c, g, HERBS.growXZ(1), lawn);
        Patch.pad(c, g, GAZEBO.growXZ(1), lawn);
        Patch.pad(c, g, HOUSE.growXZ(4), lawn);
        Patch.pad(c, g, TURRET.growXZ(1), lawn);
        Patch.pad(c, g, new Box(-3, 0, 9, 3, 0, PATCH + 1), lawn);
        front(c, g, lawn);
        shell(c);
        rooms(c);
        upstairs(c);
        roof(c);
        turret(c);
        garden(c, g);
        gazebo(c);
        c.anchor("entry", ENTRY.x(), ENTRY.y(), ENTRY.z());
        c.anchor("npc_home", NPC_HOME.x(), NPC_HOME.y(), NPC_HOME.z());
        c.anchor("bowl", BOWL.x(), BOWL.y(), BOWL.z());
        c.anchor("arena_centre", ARENA_CENTRE.x(), ARENA_CENTRE.y(), ARENA_CENTRE.z());
        return c;
    }

    static Walls.Run south() {
        return new Walls.Run(7, 6, -7, 6, Dir.SOUTH);
    }

    static Walls.Run north() {
        return new Walls.Run(-7, -6, 7, -6, Dir.NORTH);
    }

    static Walls.Run east() {
        return new Walls.Run(7, -6, 7, 6, Dir.EAST);
    }

    static Walls.Run west() {
        return new Walls.Run(-7, 6, -7, -6, Dir.WEST);
    }

    /** Foundations, floors, the outer walls and their openings. */
    private static void shell(Canvas c) {
        Box b = HOUSE;
        for (int x = b.x0(); x <= b.x1(); x++) {
            for (int z = b.z0(); z <= b.z1(); z++) {
                boolean edge = x == b.x0() || x == b.x1() || z == b.z0() || z == b.z1();
                c.set(x, -1, z, edge ? PLINTH.at(x, -1, z) : FLOOR.at(x, -1, z));
                c.set(x, -2, z, "minecraft:cobbled_deepslate");
                c.set(x, -3, z, "minecraft:cobbled_deepslate");
                if (edge) continue;
                for (int y = 0; y <= WALL_TOP; y++) c.carve(x, y, z);
                c.set(x, FF, z, Palette.boards(false, Brush.of("minecraft:spruce_planks"), Brush.of("minecraft:dark_oak_planks"), 3).at(x, FF, z));
            }
        }
        for (Walls.Run r : Walls.around(b)) {
            Walls.body(c, r, 0, 0, PLINTH);
            Walls.body(c, r, 1, GF_TOP, BRICK);
            Walls.body(c, r, FF, FF, Brush.of("minecraft:deepslate_bricks"));
            Walls.body(c, r, FF + 1, WALL_TOP, BOARDS);
            Walls.plinth(c, r, 0, 1, Family.DEEPSLATE_BRICKS, true);
            Walls.belt(c, r, FF, Family.DEEPSLATE_BRICKS);
            Walls.cornice(c, r, WALL_TOP, Wood.DARK_OAK.family(), St.trapdoor("minecraft:dark_oak_trapdoor", r.out(), true, false));
        }
        Walls.cornerPosts(c, b, FF + 1, WALL_TOP - 1, POST);
        Walls.quoins(c, b, 1, GF_TOP, Brush.of("minecraft:deepslate_bricks"));
        // Windows: tall sashes with shutters and drip hoods below, plain sashes above.
        Openings.WindowStyle low = new Openings.WindowStyle("minecraft:glass_pane", null, Family.DEEPSLATE_BRICKS, Family.DEEPSLATE_BRICKS,
                "minecraft:dark_oak_trapdoor", null, false, null);
        Openings.WindowStyle bay = new Openings.WindowStyle("minecraft:glass_pane", Brush.of(POST), null, Family.DEEPSLATE_BRICKS, null,
                "minecraft:rooted_dirt", false, POST);
        Openings.WindowStyle high = new Openings.WindowStyle("minecraft:glass_pane", null, Wood.DARK_OAK.family(), null,
                "minecraft:spruce_trapdoor", null, false, null);
        Walls.Run s = south(), n = north(), e = east(), w = west();
        Openings.window(c, s, 11, 1, 3, 3, bay);
        Openings.window(c, s, 3, 1, 1, 3, low);
        Openings.window(c, s, 11, 7, 2, 2, high);
        Openings.window(c, s, 3, 7, 2, 2, high);
        Openings.window(c, n, 11, 1, 1, 3, low);
        Openings.window(c, n, 6, 1, 1, 2, low);
        Openings.window(c, n, 3, 7, 1, 2, high);
        Openings.window(c, n, 12, 7, 1, 2, high);
        Openings.window(c, e, 3, 1, 1, 3, low);
        Openings.window(c, e, 8, 1, 1, 3, low);
        Openings.window(c, e, 3, 7, 1, 2, high);
        Openings.window(c, w, 1, 1, 1, 3, low);
        Openings.window(c, w, 3, 7, 1, 2, high);
        Openings.window(c, w, 9, 7, 1, 2, high);
        // The front door under its porch.
        Openings.door(c, s, 7, 0, 1, new Openings.DoorStyle("minecraft:dark_oak_door", Brush.of(POST), null, null, null, null));
        Openings.porch(c, s, 4, 10, 2, -1, 3, 5, 9, new Openings.PorchStyle(
                Palette.boards(true, Brush.of("minecraft:dark_oak_planks"), Brush.of("minecraft:spruce_planks"), 4),
                St.fence("minecraft:dark_oak_fence"), "minecraft:dark_oak_fence", 0, Family.DEEPSLATE_TILES, null, Wood.DARK_OAK));
        Lighting.hanging(c, 0, 3, 8, 1, false);
        // Lamps on the side walls.
        Lighting.sconce(c, -8, 2, -3, Dir.WEST, "minecraft:dark_oak_trapdoor", false);
        Lighting.sconce(c, -8, 2, 5, Dir.WEST, "minecraft:dark_oak_trapdoor", false);
        Lighting.sconce(c, 8, 2, -2, Dir.EAST, "minecraft:dark_oak_trapdoor", false);
        Lighting.sconce(c, -4, 2, -7, Dir.NORTH, "minecraft:dark_oak_trapdoor", false);
        Lighting.sconce(c, 4, 2, -7, Dir.NORTH, "minecraft:dark_oak_trapdoor", false);
    }

    /** The ground floor's rooms. */
    private static void rooms(Canvas c) {
        // Partitions: hall between x = -2 and x = 2; the side rooms split front and back at z = 0.
        for (int z = -5; z <= 5; z++) {
            for (int y = 0; y <= GF_TOP; y++) {
                c.set(-2, y, z, PANEL.at(-2, y, z));
                c.set(2, y, z, PANEL.at(2, y, z));
            }
        }
        for (int x : new int[]{-6, -5, -4, -3, 3, 4, 5, 6}) for (int y = 0; y <= GF_TOP; y++) c.set(x, y, 0, PANEL.at(x, y, 0));
        for (int[] d : new int[][]{{-2, 3}, {-2, -3}, {2, 3}, {-4, 0}, {4, 0}}) doorway(c, d[0], 0, d[1]);
        // Rugs over the boards.
        for (int z = 1; z <= 5; z++) c.set(-1, 0, z, St.carpet("red"));
        // The stair up, along the hall's east side.
        Furniture.flight(c, 0, 0, 1, Dir.NORTH, 6, 2, "minecraft:dark_oak_stairs", true);
        for (int x = 0; x <= 1; x++) c.set(x, FF + 1, 0, St.fence("minecraft:dark_oak_fence"));
        Lighting.hanging(c, 0, FF, 4, 1, false);
        Lighting.hanging(c, -1, FF, -2, 1, false);
        c.set(-1, 0, 5, "minecraft:note_block");
        c.set(-1, 1, 5, St.potted("fern"));
        c.set(1, 0, 5, St.fence("minecraft:dark_oak_fence"));
        c.set(1, 1, 5, St.carpet("black"));
        c.decor(Decor.painting(-1, 2, 1, Dir.EAST, "skull_and_roses"));

        // The parlour (west front): a soul-fire hearth, a sofa and an armchair, a rug, a chandelier.
        Furniture.fireplace(c, -6, 0, 3, Dir.EAST, Family.DEEPSLATE_BRICKS, false);
        Furniture.sofa(c, -4, 0, 4, Dir.WEST, 3, "minecraft:dark_oak_stairs", "minecraft:spruce_trapdoor");
        Furniture.chair(c, -5, 0, 5, Dir.NORTH, "minecraft:spruce_stairs", null);
        Furniture.sideTable(c, -3, 0, 5, "minecraft:dark_oak_fence", "green");
        Lighting.underCarpet(c, -5, 0, 2, "minecraft:ochre_froglight", "purple");
        Lighting.chandelier(c, -4, FF, 3, 2, "minecraft:dark_oak_fence", "white");
        c.set(-6, GF_TOP, 1, "minecraft:cobweb");

        // The library (west back): shelves floor to ceiling on two walls, her desk.
        Furniture.bookshelves(c, -6, -5, Dir.SOUTH, 5, 0, GF_TOP, Dir.EAST, false, POST, 3);
        Furniture.bookshelves(c, -5, -5, Dir.EAST, 3, 0, GF_TOP, Dir.SOUTH, false, null, 0);
        Furniture.desk(c, -4, 0, -3, Dir.NORTH, "minecraft:dark_oak_slab", "minecraft:dark_oak_stairs", false);
        c.set(-3, 0, -4, St.fence("minecraft:dark_oak_fence"));
        c.set(-3, 1, -4, St.candle("red", 3, true));
        Lighting.chandelier(c, -4, FF, -2, 2, "minecraft:dark_oak_fence", "red");

        // The spell room (east back): the cauldron, candles round it, amethyst, herbs hung from the joists.
        c.set(5, 0, -3, St.of("water_cauldron", "level", "3"));
        for (int[] k : new int[][]{{4, -4}, {6, -2}, {4, -2}}) c.set(k[0], 0, k[1], St.candle(k[0] == 6 ? "purple" : "black", 3, true));
        Furniture.table(c, 3, -5, 6, -5, 0, "minecraft:dark_oak_slab");
        c.set(3, 1, -5, St.of("amethyst_cluster", "facing", "up", "waterlogged", "false"));
        c.set(4, 1, -5, St.potted("crimson_fungus"));
        c.set(5, 1, -5, St.candle("purple", 2, true));
        c.set(6, 1, -5, "minecraft:potted_wither_rose");
        for (int x = 3; x <= 6; x++) {
            for (int z = -4; z <= -1; z++) {
                if (Noise.chance(x, 0, z, SEED + 20, 0.35)) c.set(x, GF_TOP, z, "minecraft:hanging_roots");
            }
        }
        Lighting.hanging(c, 4, FF, -3, 1, true);
        Lighting.hanging(c, 3, FF, -2, 1, false);
        Lighting.hanging(c, -1, FF, -5, 1, false);
        Lighting.hanging(c, 6, FF, -1, 2, false);

        // The dining room (east front), open into the turret.
        Furniture.diningSet(c, 4, 3, 5, 3, 0, "minecraft:dark_oak_slab", "minecraft:dark_oak_stairs");
        c.set(4, 1, 3, St.candle("white", 3, true));
        c.set(5, 1, 3, St.candle("white", 2, true));
        Furniture.kitchen(c, 6, 0, 1, Dir.SOUTH, 2, Dir.WEST, "minecraft:stripped_dark_oak_wood[axis=y]", "minecraft:dark_oak_trapdoor", false);
        Lighting.chandelier(c, 4, FF, 3, 2, "minecraft:dark_oak_fence", "white");
    }

    private static void doorway(Canvas c, int x, int y, int z) {
        c.carve(x, y, z);
        c.carve(x, y + 1, z);
        c.set(x, y + 2, z, POST.replace("axis=y", "axis=" + (x == -2 || x == 2 ? "z" : "x")));
    }

    /** The first floor: bedroom, study, landing and gallery. */
    private static void upstairs(Canvas c) {
        int y0 = FF + 1;
        for (int z = -5; z <= 5; z++) {
            for (int y = y0; y <= WALL_TOP; y++) {
                c.set(-2, y, z, PANEL.at(-2, y, z));
                c.set(2, y, z, PANEL.at(2, y, z));
            }
        }
        doorway(c, -2, y0, -4);
        doorway(c, 2, y0, -4);
        // The bedroom: a canopied divan (no bed), a wardrobe, a dresser, a rug.
        for (int z = 0; z <= 1; z++) {
            c.set(-5, y0, z, St.slabBottom("minecraft:dark_oak_slab"));
            c.set(-6, y0, z, St.slabBottom("minecraft:dark_oak_slab"));
        }
        c.set(-6, y0 + 1, 0, St.carpet("red"));
        c.set(-6, y0 + 1, 1, St.carpet("red"));
        c.set(-5, y0 + 1, 0, St.carpet("white"));
        c.set(-5, y0 + 1, 1, St.carpet("red"));
        for (int[] p : new int[][]{{-6, -1}, {-4, -1}, {-6, 2}, {-4, 2}}) {
            for (int y = y0; y <= y0 + 2; y++) c.set(p[0], y, p[1], St.fence("minecraft:dark_oak_fence"));
        }
        for (int x = -6; x <= -4; x++) for (int z = -1; z <= 2; z++) c.set(x, y0 + 3, z, St.carpet(x == -5 ? "purple" : "black"));
        for (int x = -6; x <= -4; x++) {
            c.set(x, y0 + 3, -1, St.slabTop("minecraft:dark_oak_slab"));
            c.set(x, y0 + 3, 2, St.slabTop("minecraft:dark_oak_slab"));
        }
        for (int z = 0; z <= 1; z++) {
            c.set(-6, y0 + 3, z, St.slabTop("minecraft:dark_oak_slab"));
            c.set(-4, y0 + 3, z, St.slabTop("minecraft:dark_oak_slab"));
        }
        c.set(-5, y0 + 3, 0, St.slabTop("minecraft:dark_oak_slab"));
        c.set(-5, y0 + 3, 1, St.slabTop("minecraft:dark_oak_slab"));
        Furniture.cabinet(c, -6, y0, -4, Dir.EAST, "minecraft:dark_oak_planks", "minecraft:dark_oak_trapdoor", false);
        Furniture.cabinet(c, -6, y0 + 1, -4, Dir.EAST, "minecraft:dark_oak_planks", "minecraft:dark_oak_trapdoor", false);
        c.set(-6, y0, 4, "minecraft:stripped_dark_oak_wood[axis=y]");
        c.set(-6, y0 + 1, 4, St.candle("white", 3, true));
        c.set(-6, y0, 5, "minecraft:stripped_dark_oak_wood[axis=y]");
        c.set(-6, y0 + 1, 5, St.potted("lily_of_the_valley"));
        for (int x = -5; x <= -3; x++) for (int z = 3; z <= 4; z++) c.setIfAir(x, y0, z, St.carpet("purple"));
        Lighting.sconce(c, -3, y0 + 1, -3, Dir.WEST, "minecraft:dark_oak_trapdoor", false);
        Lighting.sconce(c, -3, y0 + 1, 2, Dir.WEST, "minecraft:dark_oak_trapdoor", false);
        Lighting.sconce(c, -6, y0 + 1, -2, Dir.EAST, "minecraft:dark_oak_trapdoor", false);
        // The study: a writing desk, shelves, an armchair by the turret.
        Furniture.desk(c, 5, y0, -3, Dir.EAST, "minecraft:dark_oak_slab", "minecraft:dark_oak_stairs", false);
        Furniture.bookshelves(c, 6, -5, Dir.SOUTH, 1, y0, y0 + 2, Dir.WEST, false, null, 0);
        Furniture.bookshelves(c, 3, -5, Dir.EAST, 2, y0, y0 + 2, Dir.SOUTH, false, null, 0);
        Furniture.chair(c, 5, y0, 3, Dir.SOUTH, "minecraft:spruce_stairs", "minecraft:spruce_trapdoor");
        Furniture.sideTable(c, 3, y0, 3, "minecraft:dark_oak_fence", "black");
        Lighting.sconce(c, 3, y0 + 1, -1, Dir.EAST, "minecraft:dark_oak_trapdoor", true);
        Lighting.sconce(c, 6, y0 + 1, 1, Dir.WEST, "minecraft:dark_oak_trapdoor", false);
        Lighting.sconce(c, 3, y0 + 1, 5, Dir.EAST, "minecraft:dark_oak_trapdoor", false);
        // The landing and the gallery over the front door.
        Lighting.sconce(c, -1, y0 + 1, -5, Dir.SOUTH, "minecraft:dark_oak_trapdoor", false);
        Lighting.sconce(c, -1, y0 + 1, 5, Dir.NORTH, "minecraft:dark_oak_trapdoor", false);
        c.set(1, y0, 5, St.potted("dead_bush"));
        c.set(1, y0, 4, St.carpet("red"));
        c.set(0, y0, 4, St.carpet("red"));
        // Under the open roof: tie beams across the rooms, lanterns hung from them over the bedroom's rug, the landing and the
        // study, and a few more things on the floor (the rooms read as lived in, not an empty loft).
        for (int bx : new int[]{-4, 0, 4}) {
            for (int z = -5; z <= 5; z++) c.setIfAir(bx, WALL_TOP + 1, z, St.axis("minecraft:dark_oak_log", "z"));
        }
        Lighting.hanging(c, -4, WALL_TOP + 1, 4, 2, false);
        Lighting.hanging(c, 0, WALL_TOP + 1, -2, 2, true);
        Lighting.hanging(c, 4, WALL_TOP + 1, 0, 2, false);
        c.setIfAir(-3, y0, 5, St.potted("azure_bluet"));
        c.setIfAir(-6, y0, 3, "minecraft:amethyst_block");
        c.setIfAir(-6, y0 + 1, 3, "minecraft:amethyst_cluster[facing=up,waterlogged=false]");
        c.setIfAir(6, y0, 3, "minecraft:cartography_table");
        c.setIfAir(4, y0, -1, St.carpet("purple"));
        c.setIfAir(5, y0, -1, St.carpet("purple"));
    }

    private static void roof(Canvas c) {
        Roofs.Style slate = new Roofs.Style(Family.DEEPSLATE_TILES, Wood.DARK_OAK.family(), BOARDS, 1, Roofs.Pitch.STEEP, 0, true,
                "minecraft:dark_oak_trapdoor", true);
        Roofs.Style small = slate.withOverhang(0);
        new Roofs.Plan()
                .gable(HOUSE, WALL_TOP, true, slate)
                .dormer(-4, 6, Dir.SOUTH, 3, 3, WALL_TOP + 1, WALL_TOP + 3, BOARDS, "minecraft:glass_pane", small)
                .dormer(3, 6, Dir.SOUTH, 3, 3, WALL_TOP + 1, WALL_TOP + 3, BOARDS, "minecraft:glass_pane", small)
                .dormer(0, -6, Dir.NORTH, 3, 3, WALL_TOP + 1, WALL_TOP + 3, BOARDS, "minecraft:glass_pane", small)
                .draw(c);
        Palette.swapFamily(c, HOUSE.growXZ(2).withY(WALL_TOP, HEIGHT), Family.DEEPSLATE_TILES, Family.POLISHED_DEEPSLATE, 0.18, 2.5, SEED + 30);
        // Round windows high in the gables.
        c.set(-7, WALL_TOP + 3, 0, St.pane("minecraft:purple_stained_glass_pane"));
        c.set(7, WALL_TOP + 3, 0, St.pane("minecraft:purple_stained_glass_pane"));
        // The chimney up the west gable, over the parlour's hearth.
        Brush stack = Palette.patches(SEED + 31, 2, "minecraft:deepslate_bricks", 3, "minecraft:bricks", 2, "minecraft:cracked_deepslate_bricks", 1);
        for (int z = 2; z <= 4; z++) for (int y = -1; y <= GF_TOP; y++) c.set(-8, y, z, stack.at(-8, y, z));
        Roofs.chimney(c, -8, 3, GF_TOP + 1, stack, Family.DEEPSLATE_BRICKS, false);
    }

    private static boolean ring(int x, int z) {
        int dx = Math.abs(x - TX), dz = Math.abs(z - TZ);
        return Math.max(dx, dz) == 2 && !(dx == 2 && dz == 2);
    }

    private static boolean inTurret(int x, int z) {
        return Math.abs(x - TX) <= 1 && Math.abs(z - TZ) <= 1;
    }

    /** The round turret on the south-east corner, a pointed roof over it; its rooms open off the dining room and the study. */
    private static void turret(Canvas c) {
        Brush body = (x, y, z) -> y == 0 ? PLINTH.at(x, y, z) : y <= GF_TOP ? BRICK.at(x, y, z) : y == FF ? "minecraft:deepslate_bricks" : BOARDS.at(x, y, z);
        for (int x = TX - 2; x <= TX + 2; x++) {
            for (int z = TZ - 2; z <= TZ + 2; z++) {
                if (ring(x, z)) {
                    for (int y = -3; y <= TURRET_TOP; y++) c.set(x, y, z, y < 0 ? "minecraft:cobbled_deepslate" : body.at(x, y, z));
                } else if (!inTurret(x, z)) {
                    // The corners fill in above the eaves: the turret's top is square, corbelled out over the round shaft.
                    for (int y = WALL_TOP + 1; y <= TURRET_TOP; y++) c.set(x, y, z, BOARDS.at(x, y, z));
                } else {
                    c.set(x, -1, z, FLOOR.at(x, -1, z));
                    c.set(x, -2, z, "minecraft:cobbled_deepslate");
                    for (int y = 0; y < TURRET_TOP; y++) c.carve(x, y, z);
                    c.set(x, FF, z, "minecraft:spruce_planks");
                    c.set(x, TURRET_TOP, z, "minecraft:dark_oak_planks");
                }
            }
        }
        // Openings into the house: through the dining room's corner and the study's.
        for (int[] o : new int[][]{{6, 6}, {7, 5}}) {
            for (int y = 0; y <= 1; y++) c.carve(o[0], y, o[1]);
            for (int y = FF + 1; y <= FF + 2; y++) c.carve(o[0], y, o[1]);
        }
        c.set(7, -1, 6, FLOOR.at(7, -1, 6));
        c.set(6, -1, 6, FLOOR.at(6, -1, 6));
        c.set(7, -1, 5, FLOOR.at(7, -1, 5));
        c.set(6, FF, 6, "minecraft:spruce_planks");
        c.set(7, FF, 5, "minecraft:spruce_planks");
        c.set(7, FF, 6, "minecraft:spruce_planks");
        for (int y = 0; y < TURRET_TOP; y++) c.carve(7, y, 6);
        c.set(7, FF, 6, "minecraft:spruce_planks");
        // Windows on its three outer faces, both floors and the top.
        for (int[] w : new int[][]{{TX + 2, TZ}, {TX, TZ + 2}}) {
            for (int y : new int[]{1, 2, FF + 2, FF + 3, 12, 13}) c.set(w[0], y, w[1], St.pane("minecraft:glass_pane"));
        }
        Roofs.Style cone = new Roofs.Style(Family.DEEPSLATE_TILES, Wood.DARK_OAK.family(), BOARDS, 1, Roofs.Pitch.STEEP, 0, false,
                "minecraft:dark_oak_trapdoor", false);
        Roofs.hip(c, TURRET, TURRET_TOP, cone);
        int top = c.topAt(TX, TZ, HEIGHT, TURRET_TOP);
        c.set(TX, top + 1, TZ, St.fence("minecraft:dark_oak_fence"));
        c.set(TX, top + 2, TZ, St.rod("minecraft:lightning_rod", Dir.UP));
        Lighting.hanging(c, TX, FF, TZ, 1, false);
        Lighting.hanging(c, TX, TURRET_TOP, TZ, 3, false);
        Furniture.chair(c, TX + 1, FF + 1, TZ + 1, Dir.NORTH, "minecraft:dark_oak_stairs", null);
        c.set(TX + 1, 0, TZ + 1, St.potted("fern"));
        c.set(TX - 1, FF + 1, TZ + 1, St.lantern(false));
        c.set(TX + 1, FF + 1, TZ - 1, St.candle("white", 3, true));
        for (int[] k : new int[][]{{TX - 2, TZ + 2}, {TX + 2, TZ + 2}, {TX + 2, TZ - 2}}) c.set(k[0], 0, k[1], St.lantern(false));
    }

    private static void garden(Canvas c, Ground g) {
        // The path from the gate to the porch, and round the house to the herb garden and the gazebo.
        Paths.Style gravel = new Paths.Style(3, Palette.patches2(SEED + 40, 1.5, "minecraft:gravel", 4, "minecraft:dirt_path", 2, "minecraft:tuff", 1),
                Palette.patches2(SEED + 41, 1.5, "minecraft:coarse_dirt", 2, "minecraft:gravel", 2, "minecraft:moss_block", 1), null, null, null, 0, null,
                new Object[]{"minecraft:short_grass", 4, "minecraft:allium", 1, "minecraft:lily_of_the_valley", 1, "minecraft:fern", 2});
        Paths.path(c, g, List.of(new int[]{0, ENTRY.z()}, new int[]{0, FENCE_Z}, new int[]{0, ARENA_CENTRE.z() + 3}), gravel, SEED + 42);
        Paths.path(c, g, List.of(new int[]{0, ARENA_CENTRE.z() - 3}, new int[]{0, 13}, new int[]{0, 10}), gravel, SEED + 46);
        Paths.Style side = Paths.countryPath(2, SEED + 43);
        Paths.path(c, g, List.of(new int[]{-3, 10}, new int[]{-9, 9}, new int[]{-10, 0}, new int[]{-8, -9}, new int[]{-5, -12}), side, SEED + 44);
        Paths.path(c, g, List.of(new int[]{-5, -12}, new int[]{0, -9}, new int[]{6, -9}, new int[]{10, -11}), side, SEED + 45);
        // The iron fence across the front of the garden, a gate, two lamps.
        int fz = FENCE_Z;
        for (int x = FRONT.x0() + 1; x <= FRONT.x1() - 1; x++) {
            if (Math.abs(x) <= 1 || !g.has(x, fz) || g.water(x, fz)) continue;
            if (!c.isAir(x, 0, fz) && !org.papiricoh.supernaturalcraft.buildkit.Kinds.soilPlant(c.get(x, 0, fz))) continue;
            c.set(x, -1, fz, "minecraft:polished_blackstone_bricks");
            c.set(x, 0, fz, Math.floorMod(x, 4) == 0 ? St.wall("minecraft:polished_blackstone_brick_wall") : St.bars());
            c.carve(x, 1, fz);
        }
        for (int x : new int[]{-2, 2}) Lighting.lampPost(c, x, 0, fz, 2, "minecraft:polished_blackstone_bricks", "minecraft:polished_blackstone_brick_wall", null, false);
        // The herb garden behind the house: watered beds of herbs and roots, a berry hedge, flowers.
        Box h = HERBS;
        for (int x = h.x0(); x <= h.x1(); x++) {
            for (int z = h.z0(); z <= h.z1(); z++) {
                boolean border = x == h.x0() || x == h.x1() || z == h.z0() || z == h.z1();
                for (int y = 0; y <= 2; y++) c.carve(x, y, z);
                if (border) {
                    c.set(x, -1, z, St.axis("minecraft:stripped_spruce_log", x == h.x0() || x == h.x1() ? "z" : "x"));
                    continue;
                }
                if (z == -12) {
                    c.set(x, -1, z, x == -5 ? "minecraft:gravel" : St.water());
                    if (x != -5) c.set(x, -2, z, "minecraft:mud");
                    continue;
                }
                c.set(x, -1, z, St.of("farmland", "moisture", "7"));
                int pick = Math.floorMod(x * 7 + z * 3, 5);
                String crop = switch (pick) {
                    case 0 -> St.crop("minecraft:wheat", 5);
                    case 1 -> St.crop("minecraft:carrots", 6);
                    case 2 -> St.crop("minecraft:potatoes", 7);
                    case 3 -> St.crop("minecraft:beetroots", 3);
                    default -> St.crop("minecraft:wheat", 7);
                };
                c.set(x, 0, z, crop);
            }
        }
        // A gravel crossing over the water at the path.
        c.set(-5, -1, -12, "minecraft:gravel");
        c.set(-5, -2, -12, "minecraft:dirt");
        for (int z : new int[]{-14, -13, -11, -10}) {
            c.set(-5, -1, z, "minecraft:gravel");
            c.carve(-5, 0, z);
        }
        for (int x = h.x0(); x <= h.x1(); x++) {
            int z = h.z0() - 1;
            if (!g.has(x, z)) continue;
            c.set(x, -1, z, "minecraft:grass_block");
            if (Math.floorMod(x, 3) != 0) c.set(x, 0, z, St.of("sweet_berry_bush", "age", "3"));
        }
        // Trees at the forest's edge, flowers and grass in the lawns.
        int[][] trees = {{-14, 9, 0}, {-14, -5, 1}, {5, -16, 1}, {15, 1, 2}, {16, 12, 0}, {-16, 33, 2}, {16, 33, 1}};
        for (int[] t : trees) {
            if (!g.has(t[0], t[1])) continue;
            switch (t[2]) {
                case 0 -> Trees.oak(c, t[0], 0, t[1], 0, SEED + 50 + t[0]);
                case 1 -> Trees.spruce(c, t[0], 0, t[1], 1, SEED + 51 + t[0]);
                default -> Trees.birch(c, t[0], 0, t[1], 0, SEED + 52 + t[0]);
            }
        }
        Scatter.flora(c, g, null, 0.4, 1, SEED + 53, Scatter.GARDEN);
        Scatter.flora(c, g, null, 0.4, 1, SEED + 54, Scatter.MEADOW);
        Scatter.prune(c);
    }

    /**
     * The front garden (v0.21 P6, where Rowena fights): open lawn levelled from the porch to the fence and cleared well up, hedges
     * of azalea down its sides, flowerbeds and a ring of flagstones round its middle, lamp posts. No block entity anywhere in it.
     */
    private static void front(Canvas c, Ground g, Brush lawn) {
        Box f = FRONT;
        for (int x = f.x0(); x <= f.x1(); x++) {
            for (int z = f.z0(); z <= f.z1(); z++) {
                if (z < 10 && Math.abs(x) <= 10) continue; // the façade, the turret and the porch keep their own ground
                if (!g.has(x, z)) {
                    c.set(x, -1, z, lawn.at(x, -1, z));
                    c.set(x, -2, z, "minecraft:dirt");
                    c.set(x, -3, z, "minecraft:dirt");
                    c.set(x, -4, z, "minecraft:stone");
                    g.set(x, z, -1, 0.2);
                }
                for (int y = 0; y < 10; y++) c.carve(x, y, z);
            }
        }
        // Hedges down the sides, short of the fence and the house.
        for (int z = 11; z < FENCE_Z; z++) {
            for (int x : new int[]{f.x0(), f.x1()}) {
                c.set(x, 0, z, St.leaves("minecraft:azalea_leaves"));
                if (Math.floorMod(z, 3) != 0) c.set(x, 1, z, St.leaves(Math.floorMod(z, 5) == 0 ? "minecraft:flowering_azalea_leaves" : "minecraft:azalea_leaves"));
            }
        }
        // A ring of flagstones round the middle.
        int cx = ARENA_CENTRE.x(), cz = ARENA_CENTRE.z();
        Brush flag = Palette.patches2(SEED + 60, 1.5, "minecraft:polished_andesite", 3, "minecraft:mossy_stone_bricks", 2,
                "minecraft:stone_bricks", 2, "minecraft:cracked_stone_bricks", 1);
        for (int x = cx - 4; x <= cx + 4; x++) {
            for (int z = cz - 4; z <= cz + 4; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d <= 3.6 && (d >= 2.4 || d < 0.8)) c.set(x, -1, z, flag.at(x, -1, z));
            }
        }
        c.set(cx, -1, cz, "minecraft:chiseled_stone_bricks");
        // Flowerbeds: moss and flowers, edged with coarse dirt.
        Object[] blooms = {"minecraft:allium", 3, "minecraft:lily_of_the_valley", 3, "minecraft:cornflower", 2, "minecraft:poppy", 2,
                "minecraft:azure_bluet", 2, "minecraft:oxeye_daisy", 1};
        for (int[] bed : new int[][]{{-9, 14}, {6, 14}, {-9, 27}, {6, 27}}) {
            for (int x = bed[0]; x <= bed[0] + 3; x++) {
                for (int z = bed[1]; z <= bed[1] + 2; z++) {
                    boolean edge = x == bed[0] || x == bed[0] + 3 || z == bed[1] || z == bed[1] + 2;
                    c.set(x, -1, z, edge ? "minecraft:coarse_dirt" : "minecraft:moss_block");
                    c.set(x, 0, z, Scatter.pick(x, z, SEED + 61, blooms));
                }
            }
        }
        // Lamp posts in the garden.
        for (int[] l : new int[][]{{-10, 22}, {10, 22}, {-14, 13}, {14, 13}, {-14, 31}, {14, 31}}) {
            Lighting.lampPost(c, l[0], 0, l[1], 2, "minecraft:polished_blackstone_bricks", "minecraft:polished_blackstone_brick_wall", null, false);
        }
    }

    /** The gazebo in the garden's far corner, the spell bowl on a pedestal inside. */
    private static void gazebo(Canvas c) {
        Box b = GAZEBO;
        Brush floor = Palette.checker(1, Brush.of("minecraft:polished_deepslate"), Brush.of("minecraft:polished_andesite"));
        for (int x = b.x0(); x <= b.x1(); x++) {
            for (int z = b.z0(); z <= b.z1(); z++) {
                c.set(x, -1, z, floor.at(x, -1, z));
                for (int y = 0; y <= 3; y++) c.carve(x, y, z);
            }
        }
        int[][] posts = {{b.x0(), b.z0()}, {b.x1(), b.z0()}, {b.x0(), b.z1()}, {b.x1(), b.z1()}};
        for (int[] p : posts) for (int y = 0; y <= 2; y++) c.set(p[0], y, p[1], St.fence("minecraft:dark_oak_fence"));
        for (Walls.Run r : Walls.around(b)) {
            Walls.beam(c, r, 3, Wood.DARK_OAK, true);
            if (r.out() == Dir.WEST) continue;
            for (int i = 1; i < r.length() - 1; i++) c.set(r.x(i), 0, r.z(i), St.fence("minecraft:dark_oak_fence"));
        }
        Roofs.Style roof = new Roofs.Style(Family.DEEPSLATE_TILES, Wood.DARK_OAK.family(), Brush.of("minecraft:dark_oak_planks"), 1,
                Roofs.Pitch.STEEP, 0, false, "minecraft:dark_oak_trapdoor", false);
        Roofs.hip(c, b, 3, roof);
        c.set(BOWL.x(), 0, BOWL.z(), "minecraft:chiseled_polished_blackstone");
        c.set(BOWL.x(), BOWL.y(), BOWL.z(), "supernaturalcraft:spell_bowl");
        c.set(b.x1() - 1, 0, b.z0() + 1, St.lantern(false));
        c.set(b.x1() - 1, 0, b.z1() - 1, St.lantern(false));
        c.set(BOWL.x() + 1, 0, BOWL.z(), St.candle("purple", 3, true));
    }
}
