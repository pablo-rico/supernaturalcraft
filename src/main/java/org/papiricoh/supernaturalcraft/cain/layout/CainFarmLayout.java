package org.papiricoh.supernaturalcraft.cain.layout;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Cain's farm (v0.21, pure): where the Father of Murder keeps his bees, one per world. Origin = {@link #ARENA_CENTRE}, the middle
 * of the yard between the fields, on the ground ({@code y = 0} the first air above it); drawn for {@code q = 0} with the lane and
 * its gate to the south (+Z), the farmhouse to the north, the barn to the west and the apiary to the east, and turned by
 * {@code q} clockwise quarter turns ({@link #rotate}, the same as {@code RowenaHouseLayout.rotate}; points with {@link #at}).
 * Not seeded: one design.
 * <p>The build (buildkit): a weathered one-and-a-half-storey farmhouse of fieldstone and clapboard under a spruce-shingle roof,
 * a porch along its front (a kitchen with honey on the shelves, a sitting room with a hearth, a stair up to his bedroom and a
 * study full of books); a red timber-framed barn with a gambrel roof (hay, stalls, a honey workshop); a fenced lane with a gate;
 * four fields round a cross of dirt paths (wheat, carrots, potatoes, beetroots, water channels) with a scarecrow; a row of
 * six beehives among flowers on the east side; a stone well; an oak over a small fenced grave plot.
 * <p><b>Contract</b>: within {@link #ARENA_RADIUS} (horizontally) of {@link #ARENA_CENTRE} the ground is flat at {@code y = -1}
 * and only low things stand on it (crops, fences, hay, a scarecrow); no block entity lies within {@code ARENA_RADIUS + 1}
 * (the fight's arena restores what it touches). The {@link #HIVES} (beehives, block entities) stand between
 * {@code ARENA_RADIUS + 1.5} and {@code ARENA_RADIUS + 7} from the centre, all on the east side; {@link #FIELDS} are farmland cells
 * inside the arena ({@code y = -1}); {@link #GRAVE} is an air cell over grass (with air above it) in the grave plot, beyond the
 * arena, for a one-block headstone. The farmhouse and barn (beds, chests, barrels, a campfire hearth) are beyond the arena.
 */
public final class CainFarmLayout {

    public static final int RADIUS = 32, HEIGHT = 22, DEPTH = 5;
    public static final int ARENA_RADIUS = 16;
    /** The middle of the yard between the fields: the fight's centre. */
    public static final LayoutPoint ARENA_CENTRE = new LayoutPoint(0, 0, 0);
    /** On the lane just outside the gate. */
    public static final LayoutPoint ENTRY = new LayoutPoint(0, 0, 28);
    /** On the farmhouse porch, by the door: where Cain stands by day. */
    public static final LayoutPoint NPC_HOME = new LayoutPoint(1, 0, -19);
    /** In the yard, toward the farmhouse: where the fight's Cain stands. */
    public static final LayoutPoint BOSS_SPAWN = new LayoutPoint(0, 0, -6);
    /** In the fenced plot under the oak: a one-block headstone goes here. */
    public static final LayoutPoint GRAVE = new LayoutPoint(17, 0, 14);
    /** The beehives of the apiary (block entities), facing the yard. */
    public static final List<LayoutPoint> HIVES = List.of(new LayoutPoint(19, 0, -7), new LayoutPoint(20, 0, -4), new LayoutPoint(19, 0, -1),
            new LayoutPoint(20, 0, 2), new LayoutPoint(19, 0, 5), new LayoutPoint(20, 0, 8));
    /** Farmland cells in the fields (the farmland block itself, {@code y = -1}). */
    public static final List<LayoutPoint> FIELDS = List.of(new LayoutPoint(4, -1, 3), new LayoutPoint(8, -1, 7), new LayoutPoint(11, -1, 3),
            new LayoutPoint(-4, -1, 3), new LayoutPoint(-9, -1, 7), new LayoutPoint(-12, -1, 3), new LayoutPoint(4, -1, -3),
            new LayoutPoint(8, -1, -8), new LayoutPoint(-4, -1, -3), new LayoutPoint(-8, -1, -7), new LayoutPoint(12, -1, -4),
            new LayoutPoint(3, -1, 11), new LayoutPoint(-3, -1, -12));
    /** Room points (reachability): the sitting room, the kitchen, the bedroom, the study and the barn. */
    public static final LayoutPoint SITTING_ROOM = new LayoutPoint(-5, 0, -22), KITCHEN = new LayoutPoint(2, 0, -21),
            BEDROOM = new LayoutPoint(-4, 5, -22), STUDY = new LayoutPoint(4, 5, -22), BARN = new LayoutPoint(-24, 0, 0);

    /** The farmhouse's walls (outline) and its rows. */
    static final Box HOUSE = new Box(-8, 0, -28, 6, 0, -20);
    static final int GF_TOP = 3, FF = 4, WALL_TOP = 7;
    /** The barn's walls (outline) and its wall top. */
    static final Box BARN_BOX = new Box(-30, 0, -8, -20, 0, 6);
    static final int BARN_TOP = 5;
    /** The grave plot's fence (outline). */
    static final Box PLOT = new Box(14, 0, 11, 20, 0, 17);
    /** The well's ring (outline). */
    static final Box WELL = new Box(9, 0, -18, 11, 0, -16);
    /** The south fence's row and the fields' reach. */
    static final int FENCE_Z = 25, FIELD_R = 14;
    static final int SEED = 6131;

    private CainFarmLayout() {
    }

    /** Local X/Z turned by {@code q} clockwise quarter turns (as {@code RowenaHouseLayout.rotate} and {@link Canvas#turn}). */
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

    /** The farm as a finished kit canvas (tests and the layout dump). */
    public static Canvas canvas(int q) {
        Canvas south = drawn();
        Canvas c = new Canvas("cain_farm");
        c.zone("farm", new Box(-RADIUS, -DEPTH, -RADIUS, RADIUS, HEIGHT, RADIUS));
        c.inZone("farm", () -> c.stamp(south, 0, 0, 0, Math.floorMod(q, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    /** Horizontal distance from the arena's centre. */
    static double fromCentre(int x, int z) {
        return Math.hypot(x, z);
    }

    // --- brushes -------------------------------------------------------------------------------------------------------------

    static final Brush FIELDSTONE = Palette.patches(SEED + 1, 2, "minecraft:cobblestone", 4, "minecraft:mossy_cobblestone", 2,
            "minecraft:andesite", 2, "minecraft:stone", 1);
    static final Brush CLAPBOARD = Palette.mix(SEED + 2, "minecraft:stripped_birch_wood[axis=y]", 4, "minecraft:birch_planks", 3,
            "minecraft:oak_planks", 2, "minecraft:stripped_oak_wood[axis=y]", 1);
    static final Brush FLOOR = Palette.boards(true, Brush.of("minecraft:spruce_planks"), Brush.of("minecraft:oak_planks"),
            Brush.of("minecraft:stripped_spruce_wood[axis=y]"), 4);
    static final Brush LOFT = Palette.boards(false, Brush.of("minecraft:oak_planks"), Brush.of("minecraft:spruce_planks"),
            Brush.of("minecraft:stripped_oak_wood[axis=y]"), 3);
    static final Brush PANEL = Palette.mix(SEED + 3, "minecraft:spruce_planks", 3, "minecraft:stripped_spruce_wood[axis=y]", 2,
            "minecraft:oak_planks", 2);
    static final Brush BARN_RED = Palette.patches(SEED + 4, 1.5, "minecraft:red_terracotta", 5, "minecraft:terracotta", 2,
            "minecraft:brown_terracotta", 1, "minecraft:red_concrete_powder", 1);
    static final Brush BARN_FLOOR = Palette.patches2(SEED + 5, 2, "minecraft:packed_mud", 3, "minecraft:coarse_dirt", 2,
            "minecraft:spruce_planks", 2, "minecraft:stripped_spruce_wood[axis=y]", 1);
    static final String POST = "minecraft:stripped_dark_oak_log[axis=y]";

    // --- drawing -------------------------------------------------------------------------------------------------------------

    static Canvas drawn() {
        Canvas c = new Canvas("cain_south");
        Ground g = ground(c);
        fields(c);
        house(c);
        rooms(c);
        upstairs(c);
        roof(c);
        barn(c);
        apiary(c);
        grave(c);
        well(c);
        lane(c, g);
        outdoors(c, g);
        // The headstone's spot stays bare (the meadow's flora is scattered over everything before it).
        for (int y = 0; y <= 2; y++) c.carve(GRAVE.x(), y, GRAVE.z());
        c.anchor("entry", ENTRY.x(), ENTRY.y(), ENTRY.z());
        c.anchor("npc_home", NPC_HOME.x(), NPC_HOME.y(), NPC_HOME.z());
        c.anchor("boss_spawn", BOSS_SPAWN.x(), BOSS_SPAWN.y(), BOSS_SPAWN.z());
        c.anchor("grave", GRAVE.x(), GRAVE.y(), GRAVE.z());
        c.anchor("arena_centre", ARENA_CENTRE.x(), ARENA_CENTRE.y(), ARENA_CENTRE.z());
        for (int i = 0; i < HIVES.size(); i++) c.anchor("hive_" + i, HIVES.get(i).x(), HIVES.get(i).y(), HIVES.get(i).z());
        for (int i = 0; i < FIELDS.size(); i++) c.anchor("field_" + i, FIELDS.get(i).x(), FIELDS.get(i).y(), FIELDS.get(i).z());
        c.anchor("sitting_room", SITTING_ROOM.x(), SITTING_ROOM.y(), SITTING_ROOM.z());
        c.anchor("kitchen", KITCHEN.x(), KITCHEN.y(), KITCHEN.z());
        c.anchor("bedroom", BEDROOM.x(), BEDROOM.y(), BEDROOM.z());
        c.anchor("study", STUDY.x(), STUDY.y(), STUDY.z());
        c.anchor("barn", BARN.x(), BARN.y(), BARN.z());
        return c;
    }

    /**
     * The levelled ground: a ragged patch of meadow (surface at {@code y = -1}, soil under it, stone at {@code y = -4}), the air
     * cleared above it, padded under every build and kept inside the canvas's reach.
     */
    private static Ground ground(Canvas c) {
        Brush lawn = Palette.patches2(SEED + 10, 4, "minecraft:grass_block", 16, "minecraft:coarse_dirt", 1, "minecraft:podzol", 1,
                "minecraft:rooted_dirt", 1);
        Ground g = new Ground();
        int radius = 27, reach = RADIUS - 1;
        for (int x = -reach; x <= reach; x++) {
            for (int z = -reach; z <= reach; z++) {
                double r = radius * (0.88 + 0.26 * Noise.value2(x, z, 7, SEED + 11));
                double d = Math.hypot(x, z);
                boolean pad = HOUSE.growXZ(3).containsColumn(x, z) || BARN_BOX.growXZ(3).containsColumn(x, z) || PLOT.growXZ(3).containsColumn(x, z)
                        || new Box(17, 0, -11, 25, 0, 12).containsColumn(x, z) || new Box(-3, 0, 17, 3, 0, reach).containsColumn(x, z)
                        || WELL.growXZ(2).containsColumn(x, z) || new Box(-22, 0, FENCE_Z - 1, 22, 0, FENCE_Z + 1).containsColumn(x, z);
                if (d > r && !pad) continue;
                c.set(x, -1, z, lawn.at(x, -1, z));
                c.set(x, -2, z, "minecraft:dirt");
                c.set(x, -3, z, "minecraft:dirt");
                c.set(x, -4, z, "minecraft:stone");
                int up = d < r - 3 || pad ? 14 : 4;
                for (int y = 0; y < up; y++) c.carve(x, y, z);
                g.set(x, z, -1, Math.max(0.05, 1 - d / Math.max(r, d + 1)));
            }
        }
        return g;
    }

    /** The four fields round a cross of dirt paths, channels of water between their rows, a scarecrow. */
    private static void fields(Canvas c) {
        Brush yard = Palette.patches2(SEED + 20, 2, "minecraft:dirt_path", 6, "minecraft:coarse_dirt", 2, "minecraft:gravel", 1);
        for (int x = -FIELD_R - 1; x <= FIELD_R + 1; x++) {
            for (int z = -FIELD_R - 1; z <= FIELD_R + 1; z++) {
                double d = fromCentre(x, z);
                if (d > FIELD_R + 1) continue;
                boolean cross = Math.abs(x) <= 1 || Math.abs(z) <= 1;
                if (d < 5 || cross) {
                    if (cross || d < 3.5) c.set(x, -1, z, yard.at(x, -1, z));
                    continue;
                }
                if (Math.abs(x) <= 2 || Math.abs(z) <= 2 || d > FIELD_R) {
                    c.set(x, -1, z, "minecraft:coarse_dirt");
                    continue;
                }
                boolean channel = Math.floorMod(z, 5) == 0 && Math.floorMod(x, 7) != 3;
                if (channel) {
                    c.set(x, -1, z, St.water());
                    c.set(x, -2, z, "minecraft:mud");
                    continue;
                }
                c.set(x, -1, z, St.of("farmland", "moisture", "7"));
                c.set(x, 0, z, crop(x, z));
            }
        }
        // The scarecrow in the north-west field: a post, arms, a straw body, a pumpkin head.
        int sx = -6, sz = -8;
        c.set(sx, -1, sz, "minecraft:coarse_dirt");
        c.set(sx, 0, sz, St.fence("minecraft:spruce_fence"));
        c.set(sx, 1, sz, "minecraft:hay_block[axis=y]");
        c.set(sx, 2, sz, St.facing("minecraft:carved_pumpkin", Dir.SOUTH));
        c.set(sx - 1, 1, sz, St.fence("minecraft:spruce_fence"));
        c.set(sx + 1, 1, sz, St.fence("minecraft:spruce_fence"));
        for (int dx : new int[]{-1, 1}) c.remove(sx + dx, 0, sz);
        for (int dx : new int[]{-1, 1}) c.set(sx + dx, 0, sz, St.crop("minecraft:wheat", 2));
        // A few hay bales by the west path, a water butt (cauldron) and a bench by the east one.
        for (int[] h : new int[][]{{-12, 2}, {-13, 2}, {-13, -2}, {11, -2}, {12, -2}}) c.set(h[0], -1, h[1], "minecraft:coarse_dirt");
        c.set(-12, 0, 2, "minecraft:hay_block[axis=x]");
        c.set(-13, 0, 2, "minecraft:hay_block[axis=x]");
        c.set(-13, 0, -2, "minecraft:hay_block[axis=z]");
        c.set(11, 0, -2, St.of("water_cauldron", "level", "3"));
        c.set(12, 0, -2, St.slabBottom("minecraft:spruce_slab"));
        // Lanterns on mounting stones at the corners of the yard (low: nothing in the arena stands taller than a man's waist).
        for (int[] p : new int[][]{{3, 3}, {-3, -3}}) {
            c.set(p[0], 0, p[1], "minecraft:mossy_cobblestone");
            c.set(p[0], 1, p[1], St.lantern(false));
        }
    }

    private static String crop(int x, int z) {
        int quadrant = (x > 0 ? 1 : 0) + (z > 0 ? 2 : 0);
        int age = 3 + Noise.pick(x, 0, z, SEED + 21, 5);
        return switch (quadrant) {
            case 0 -> St.crop("minecraft:wheat", Math.min(7, age + 1));
            case 1 -> St.crop("minecraft:carrots", Math.min(7, age));
            case 2 -> St.crop("minecraft:potatoes", Math.min(7, age));
            default -> St.crop("minecraft:beetroots", Math.min(3, age - 2));
        };
    }

    static Walls.Run south() {
        return new Walls.Run(6, -20, -8, -20, Dir.SOUTH);
    }

    static Walls.Run north() {
        return new Walls.Run(-8, -28, 6, -28, Dir.NORTH);
    }

    static Walls.Run east() {
        return new Walls.Run(6, -28, 6, -20, Dir.EAST);
    }

    static Walls.Run west() {
        return new Walls.Run(-8, -20, -8, -28, Dir.WEST);
    }

    /** The farmhouse's shell: floors, walls, windows, the door and the porch. */
    private static void house(Canvas c) {
        Box b = HOUSE;
        for (int x = b.x0(); x <= b.x1(); x++) {
            for (int z = b.z0(); z <= b.z1(); z++) {
                boolean edge = x == b.x0() || x == b.x1() || z == b.z0() || z == b.z1();
                c.set(x, -1, z, edge ? "minecraft:cobblestone" : FLOOR.at(x, -1, z));
                c.set(x, -2, z, "minecraft:cobblestone");
                if (edge) continue;
                for (int y = 0; y <= WALL_TOP; y++) c.carve(x, y, z);
                c.set(x, FF, z, LOFT.at(x, FF, z));
            }
        }
        for (Walls.Run r : Walls.around(b)) {
            Walls.body(c, r, 0, 1, FIELDSTONE);
            Walls.body(c, r, 2, GF_TOP, CLAPBOARD);
            Walls.body(c, r, FF, FF, Brush.of("minecraft:stripped_spruce_wood[axis=y]"));
            Walls.body(c, r, FF + 1, WALL_TOP, CLAPBOARD);
            // No skirting along the front: the porch's deck runs up to the wall there.
            Walls.plinth(c, r, 0, 1, Family.COBBLESTONE, r.out() != Dir.SOUTH);
            Walls.beam(c, r, FF, Wood.SPRUCE, true);
            Walls.cornice(c, r, WALL_TOP, Wood.SPRUCE.family(), St.trapdoor("minecraft:spruce_trapdoor", r.out(), true, false));
        }
        Walls.cornerPosts(c, b, 2, WALL_TOP - 1, POST);
        Openings.WindowStyle low = new Openings.WindowStyle("minecraft:glass_pane", null, Family.COBBLESTONE, Wood.SPRUCE.family(),
                "minecraft:spruce_trapdoor", null, false, null);
        Openings.WindowStyle box = new Openings.WindowStyle("minecraft:glass_pane", null, null, Wood.SPRUCE.family(),
                "minecraft:spruce_trapdoor", null, false, null);
        Openings.WindowStyle high = new Openings.WindowStyle("minecraft:glass_pane", null, Wood.SPRUCE.family(), null, null, null, false, null);
        Walls.Run s = south(), n = north(), e = east(), w = west();
        Openings.window(c, s, 2, 1, 2, 2, box);
        Openings.window(c, s, 10, 1, 2, 2, box);
        Openings.window(c, s, 3, 5, 1, 2, high);
        Openings.window(c, s, 11, 5, 1, 2, high);
        Openings.window(c, n, 2, 1, 2, 2, low);
        Openings.window(c, n, 10, 1, 1, 2, low);
        Openings.window(c, n, 6, 5, 2, 2, high);
        Openings.window(c, e, 2, 1, 1, 2, low);
        Openings.window(c, e, 6, 1, 1, 2, low);
        Openings.window(c, e, 4, 5, 1, 2, high);
        Openings.window(c, w, 1, 1, 1, 2, low);
        Openings.window(c, w, 7, 1, 1, 2, low);
        Openings.window(c, w, 4, 5, 1, 2, high);
        // The front door (east of middle), under the porch.
        Openings.door(c, s, 6, 0, 1, new Openings.DoorStyle("minecraft:spruce_door", Brush.of(POST), null, null, null, null));
        Openings.porch(c, s, 1, 13, 2, -1, 3, 5, 7, new Openings.PorchStyle(
                Palette.boards(true, Brush.of("minecraft:spruce_planks"), Brush.of("minecraft:oak_planks"), 3),
                St.fence("minecraft:spruce_fence"), "minecraft:spruce_fence", 4, Wood.SPRUCE.family(), null, Wood.SPRUCE));
        Lighting.sconce(c, 2, 1, -19, Dir.SOUTH, "minecraft:spruce_trapdoor", false);
        Lighting.sconce(c, -5, 1, -19, Dir.SOUTH, "minecraft:spruce_trapdoor", false);
        // A rocking chair and a little table on the porch; honey jars by the door.
        Furniture.chair(c, -3, 0, -18, Dir.SOUTH, "minecraft:spruce_stairs", "minecraft:spruce_trapdoor");
        Furniture.sideTable(c, -1, 0, -18, "minecraft:spruce_fence", "yellow");
        c.set(4, 0, -19, "minecraft:honey_block");
        c.set(5, 0, -19, "minecraft:honeycomb_block");
    }

    private static void doorway(Canvas c, int x, int y, int z) {
        c.carve(x, y, z);
        c.carve(x, y + 1, z);
        c.set(x, y + 2, z, POST.replace("axis=y", "axis=z"));
    }

    /** The ground floor: the sitting room (west), the kitchen and the stair (east). */
    private static void rooms(Canvas c) {
        for (int z = -27; z <= -21; z++) for (int y = 0; y <= GF_TOP; y++) c.set(-2, y, z, PANEL.at(-2, y, z));
        doorway(c, -2, 0, -23);
        // The stair up along the north wall, climbing east, onto the landing.
        Furniture.flight(c, 0, 0, -27, Dir.EAST, 5, 2, "minecraft:spruce_stairs", true);
        for (int x = 1; x <= 4; x++) c.set(x, FF + 1, -25, St.fence("minecraft:spruce_fence"));
        c.set(0, FF + 1, -25, St.fence("minecraft:spruce_fence"));
        // The kitchen: a run along the east wall, a table, honey everywhere.
        Furniture.kitchen(c, 5, 0, -24, Dir.SOUTH, 3, Dir.WEST, "minecraft:stripped_spruce_wood[axis=y]", "minecraft:spruce_trapdoor", true);
        Furniture.diningSet(c, 2, -23, 3, -23, 0, "minecraft:spruce_slab", "minecraft:oak_stairs");
        c.set(2, 1, -23, St.candle("yellow", 3, true));
        c.set(-1, 0, -27, "minecraft:honeycomb_block");
        c.set(-1, 1, -27, "minecraft:honey_block");
        c.set(-1, 0, -26, St.barrel(Dir.EAST, false));
        c.set(-1, 1, -26, St.candle("orange", 2, true));
        c.set(-1, 0, -21, St.potted("dandelion"));
        c.set(5, 0, -21, "minecraft:chest[facing=west,type=single,waterlogged=false]");
        Lighting.hanging(c, 2, FF, -22, 1, false);
        Lighting.hanging(c, 4, FF, -25, 1, false);
        Lighting.hanging(c, 0, FF, -24, 1, false);
        // The sitting room: the hearth on the west wall, an old sofa, a rug, shelves of books.
        Furniture.fireplace(c, -7, 0, -24, Dir.EAST, Family.COBBLESTONE, true);
        Furniture.sofa(c, -4, 0, -25, Dir.WEST, 3, "minecraft:spruce_stairs", "minecraft:spruce_trapdoor");
        Furniture.sideTable(c, -3, 0, -21, "minecraft:spruce_fence", "brown");
        Furniture.chair(c, -6, 0, -21, Dir.NORTH, "minecraft:oak_stairs", null);
        Furniture.bookshelves(c, -7, -27, Dir.EAST, 4, 0, 2, Dir.SOUTH, false, "minecraft:stripped_spruce_log[axis=y]", 3);
        for (int x = -6; x <= -5; x++) for (int z = -25; z <= -23; z++) c.setIfAir(x, 0, z, St.carpet(z == -24 ? "brown" : "orange"));
        Lighting.hanging(c, -5, FF, -24, 1, false);
        Lighting.hanging(c, -4, FF, -21, 1, false);
        Lighting.hanging(c, -5, FF, -26, 1, false);
    }

    /** Upstairs under the roof: his bedroom (west) and a study full of books (east), the landing over the stair. */
    private static void upstairs(Canvas c) {
        int y0 = FF + 1;
        for (int z = -27; z <= -21; z++) for (int y = y0; y <= WALL_TOP; y++) c.set(-2, y, z, PANEL.at(-2, y, z));
        doorway(c, -2, y0, -23);
        Furniture.bed(c, -5, y0, -24, Dir.NORTH, "brown", "minecraft:spruce_planks", true);
        Furniture.cabinet(c, -7, y0, -21, Dir.EAST, "minecraft:spruce_planks", "minecraft:spruce_trapdoor", false);
        c.set(-7, y0, -27, "minecraft:chest[facing=east,type=single,waterlogged=false]");
        for (int x = -5; x <= -3; x++) c.setIfAir(x, y0, -22, St.carpet("green"));
        Lighting.sconce(c, -3, y0 + 1, -27, Dir.SOUTH, "minecraft:spruce_trapdoor", false);
        Lighting.sconce(c, -7, y0 + 1, -23, Dir.EAST, "minecraft:spruce_trapdoor", false);
        Lighting.sconce(c, -3, y0 + 1, -21, Dir.NORTH, "minecraft:spruce_trapdoor", false);
        // The study: shelves on the south wall, a desk, an armchair, a jar of honey by the books.
        Furniture.bookshelves(c, 0, -21, Dir.EAST, 4, y0, y0 + 1, Dir.NORTH, true, null, 0);
        Furniture.desk(c, 1, y0, -23, Dir.NORTH, "minecraft:spruce_slab", "minecraft:spruce_stairs", false);
        Furniture.chair(c, -1, y0, -22, Dir.EAST, "minecraft:oak_stairs", null);
        c.set(-1, y0, -27, "minecraft:honey_block");
        c.set(5, y0, -21, "minecraft:bookshelf");
        c.set(5, y0 + 1, -21, St.candle("white", 3, true));
        Lighting.sconce(c, 5, y0 + 1, -23, Dir.WEST, "minecraft:spruce_trapdoor", false);
        Lighting.sconce(c, -1, y0 + 1, -24, Dir.EAST, "minecraft:spruce_trapdoor", false);
    }

    private static void roof(Canvas c) {
        Roofs.Style shingles = new Roofs.Style(Wood.SPRUCE.family(), Wood.DARK_OAK.family(), CLAPBOARD, 1, Roofs.Pitch.STEEP, 0, true,
                "minecraft:spruce_trapdoor", true);
        new Roofs.Plan()
                .gable(HOUSE, WALL_TOP, true, shingles)
                .dormer(-4, -20, Dir.SOUTH, 3, 3, WALL_TOP, WALL_TOP + 2, CLAPBOARD, "minecraft:glass_pane", shingles.withOverhang(0))
                .draw(c);
        Palette.swapFamily(c, HOUSE.growXZ(2).withY(WALL_TOP, HEIGHT), Wood.SPRUCE.family(), Wood.DARK_OAK.family(), 0.2, 2.5, SEED + 30);
        // The chimney up the west gable, over the hearth.
        Brush stack = Palette.patches(SEED + 31, 2, "minecraft:cobblestone", 3, "minecraft:mossy_cobblestone", 2, "minecraft:stone_bricks", 1);
        for (int z = -25; z <= -23; z++) for (int y = -1; y <= GF_TOP; y++) c.set(-9, y, z, stack.at(-9, y, z));
        Roofs.chimney(c, -9, -24, GF_TOP + 1, stack, Family.COBBLESTONE, false);
    }

    static Walls.Run barnEast() {
        return new Walls.Run(-20, -8, -20, 6, Dir.EAST);
    }

    /** The red barn: timber frame, a gambrel roof, hay, stalls and a honey workshop; its wide door opens on the yard. */
    private static void barn(Canvas c) {
        Box b = BARN_BOX;
        for (int x = b.x0(); x <= b.x1(); x++) {
            for (int z = b.z0(); z <= b.z1(); z++) {
                boolean edge = x == b.x0() || x == b.x1() || z == b.z0() || z == b.z1();
                c.set(x, -1, z, edge ? "minecraft:cobblestone" : BARN_FLOOR.at(x, -1, z));
                c.set(x, -2, z, "minecraft:cobblestone");
                if (!edge) for (int y = 0; y <= BARN_TOP; y++) c.carve(x, y, z);
            }
        }
        for (Walls.Run r : Walls.around(b)) {
            Walls.body(c, r, 0, 0, FIELDSTONE);
            Walls.timberFrame(c, r, 1, BARN_TOP, Wood.DARK_OAK, BARN_RED, 4, 3, true, "minecraft:dark_oak_stairs");
            Walls.plinth(c, r, 0, 1, Family.COBBLESTONE, true);
        }
        Walls.Run e = barnEast();
        Openings.archway(c, e, 6, 0, 3, 4, "minecraft:dark_oak_stairs");
        for (int z = -2; z <= 0; z++) c.set(-20, -1, z, BARN_FLOOR.at(-20, -1, z));
        // Open barn doors folded back against the wall either side.
        for (int y = 0; y <= 2; y++) {
            c.set(-19, y, -3, St.trapdoorAgainst("minecraft:spruce_trapdoor", Dir.NORTH));
            c.set(-19, y, 1, St.trapdoorAgainst("minecraft:spruce_trapdoor", Dir.SOUTH));
        }
        Openings.WindowStyle win = new Openings.WindowStyle("minecraft:glass_pane", null, Wood.DARK_OAK.family(), null, null, null, false, null);
        for (Walls.Run r : Walls.around(b)) {
            if (r.out() == Dir.EAST) continue;
            int len = r.length();
            for (int i = 2; i < len - 2; i += 4) Openings.window(c, r, i, 2, 1, 2, win);
        }
        Roofs.Style gambrel = new Roofs.Style(Family.DEEPSLATE_TILES, Wood.DARK_OAK.family(), BARN_RED, 1, Roofs.Pitch.MANSARD, 3, true,
                "minecraft:dark_oak_trapdoor", true);
        Roofs.gable(c, b, BARN_TOP, false, gambrel);
        Palette.swapFamily(c, b.growXZ(2).withY(BARN_TOP, HEIGHT), Family.DEEPSLATE_TILES, Family.COBBLED_DEEPSLATE, 0.2, 2.0, SEED + 40);
        // Tie beams across, lanterns hanging from them.
        for (int z : new int[]{-4, 2}) {
            for (int x = b.x0() + 1; x <= b.x1() - 1; x++) c.set(x, BARN_TOP, z, Wood.DARK_OAK.log("x"));
            Lighting.hanging(c, -25, BARN_TOP, z, 2, false);
            Lighting.hanging(c, -28, BARN_TOP, z, 2, false);
            Lighting.hanging(c, -22, BARN_TOP, z, 2, false);
        }
        // Stalls along the west wall.
        for (int z = -7; z <= 5; z++) {
            if (z == -5 || z == -1 || z == 3) {
                for (int x = -29; x <= -27; x++) c.set(x, 0, z, St.fence("minecraft:spruce_fence"));
            }
        }
        for (int z : new int[]{-7, -6, -3, -2, 1, 2, 5}) c.set(-27, 0, z, St.fence("minecraft:spruce_fence"));
        for (int z : new int[]{-4, 0, 4}) c.set(-27, 0, z, St.gate("minecraft:spruce_fence_gate", Dir.EAST, false, false));
        for (int[] h : new int[][]{{-29, -7}, {-28, -7}, {-29, -6}, {-29, -3}, {-29, 1}, {-28, 2}, {-29, 5}}) {
            c.set(h[0], 0, h[1], "minecraft:hay_block[axis=y]");
        }
        c.set(-29, 1, -7, "minecraft:hay_block[axis=x]");
        c.set(-29, 0, -4, St.of("water_cauldron", "level", "3"));
        // The honey workshop in the north-east corner: combs and honey, a cauldron, barrels.
        c.set(-21, 0, -7, "minecraft:honeycomb_block");
        c.set(-22, 0, -7, "minecraft:honey_block");
        c.set(-23, 0, -7, St.barrel(Dir.SOUTH, false));
        c.set(-24, 0, -7, St.barrel(Dir.SOUTH, false));
        c.set(-24, 1, -7, "minecraft:honeycomb_block");
        c.set(-21, 0, -6, St.of("cauldron"));
        Furniture.table(c, -21, 4, -23, 4, 0, "minecraft:spruce_slab");
        c.set(-22, 1, 4, St.candle("yellow", 2, true));
        c.set(-21, 0, 5, "minecraft:chest[facing=west,type=single,waterlogged=false]");
        for (int z : new int[]{-6, 0, 4}) Lighting.sconce(c, -29, 1, z, Dir.EAST, "minecraft:spruce_trapdoor", false);
        Lighting.sconce(c, -21, 1, -3, Dir.WEST, "minecraft:spruce_trapdoor", false);
        Lighting.sconce(c, -21, 1, 2, Dir.WEST, "minecraft:spruce_trapdoor", false);
    }

    /** The apiary on the east side: six hives among flowers, a low fence behind them. */
    private static void apiary(Canvas c) {
        String[] flowers = {"minecraft:cornflower", "minecraft:allium", "minecraft:oxeye_daisy", "minecraft:poppy", "minecraft:azure_bluet",
                "minecraft:dandelion", "minecraft:lily_of_the_valley"};
        for (int x = 18; x <= 24; x++) {
            for (int z = -10; z <= 11; z++) {
                c.set(x, -1, z, "minecraft:grass_block");
                for (int y = 0; y <= 3; y++) c.carve(x, y, z);
                if (x == 24) {
                    c.set(x, 0, z, St.fence("minecraft:spruce_fence"));
                } else if (x >= 21 && Noise.chance(x, 0, z, SEED + 50, 0.7)) {
                    c.set(x, 0, z, flowers[Noise.pick(x, 0, z, SEED + 51, flowers.length)]);
                } else if (x == 18 && Noise.chance(x, 0, z, SEED + 52, 0.35)) {
                    c.set(x, 0, z, flowers[Noise.pick(x, 0, z, SEED + 53, flowers.length)]);
                }
            }
        }
        for (LayoutPoint h : HIVES) {
            c.set(h.x(), -1, h.z(), "minecraft:spruce_planks");
            c.set(h.x(), 0, h.z(), "minecraft:beehive[facing=west,honey_level=" + (Math.floorMod(h.z(), 3) + 2) + "]");
            c.set(h.x(), 1, h.z(), St.slabBottom("minecraft:spruce_slab"));
        }
        c.set(23, 0, -10, St.of("lilac", "half", "lower"));
        c.set(23, 1, -10, St.of("lilac", "half", "upper"));
        c.set(23, 0, 11, St.of("peony", "half", "lower"));
        c.set(23, 1, 11, St.of("peony", "half", "upper"));
    }

    /** The grave plot under the oak: a low picket fence with a gate, lilies; the headstone's spot left bare. */
    private static void grave(Canvas c) {
        Box p = PLOT;
        for (int x = p.x0(); x <= p.x1(); x++) {
            for (int z = p.z0(); z <= p.z1(); z++) {
                c.set(x, -1, z, "minecraft:grass_block");
                for (int y = 0; y <= 3; y++) c.carve(x, y, z);
                boolean edge = x == p.x0() || x == p.x1() || z == p.z0() || z == p.z1();
                if (edge) c.set(x, 0, z, St.fence("minecraft:spruce_fence"));
            }
        }
        c.set(p.x0(), 0, GRAVE.z(), St.gate("minecraft:spruce_fence_gate", Dir.WEST, false, false));
        for (int[] f : new int[][]{{16, 13}, {18, 13}, {16, 15}, {18, 15}, {19, 12}, {15, 16}}) c.set(f[0], 0, f[1], "minecraft:lily_of_the_valley");
        c.set(GRAVE.x(), -1, GRAVE.z() + 1, "minecraft:podzol");
        c.set(GRAVE.x(), -1, GRAVE.z(), "minecraft:grass_block");
        Trees.oak(c, 21, 0, 18, 1, SEED + 60);
        for (int y = 0; y <= 2; y++) c.carve(GRAVE.x(), y, GRAVE.z());
    }

    /** The stone well by the farmhouse: a ring, water, posts and a little roof. */
    private static void well(Canvas c) {
        Box w = WELL;
        int cx = w.centerX(), cz = w.centerZ();
        for (int x = w.x0(); x <= w.x1(); x++) {
            for (int z = w.z0(); z <= w.z1(); z++) {
                boolean mid = x == cx && z == cz;
                if (mid) {
                    c.set(x, -1, z, St.water());
                    c.set(x, -2, z, St.water());
                    c.set(x, -3, z, "minecraft:stone");
                    for (int y = 0; y <= 2; y++) c.carve(x, y, z);
                    continue;
                }
                c.set(x, -2, z, "minecraft:cobblestone");
                c.set(x, -1, z, "minecraft:mossy_cobblestone");
                boolean corner = (x == w.x0() || x == w.x1()) && (z == w.z0() || z == w.z1());
                c.set(x, 0, z, corner ? "minecraft:cobblestone" : (x + z) % 2 == 0 ? "minecraft:mossy_cobblestone" : "minecraft:cobblestone");
                if (corner) for (int y = 1; y <= 2; y++) c.set(x, y, z, St.fence("minecraft:spruce_fence"));
            }
        }
        for (int x = w.x0(); x <= w.x1(); x++) for (int z = w.z0(); z <= w.z1(); z++) c.set(x, 3, z, St.slabBottom("minecraft:spruce_slab"));
        c.set(cx, 2, cz, St.chain("y"));
        c.set(cx, 1, cz, St.lantern(true));
    }

    /** The lane from the gate, the fence along the south with its gate and lamps, and the paths to the house, barn, hives and grave. */
    private static void lane(Canvas c, Ground g) {
        Paths.Style country = Paths.countryPath(3, SEED + 70);
        Paths.path(c, g, List.of(new int[]{0, RADIUS - 1}, new int[]{0, 22}, new int[]{0, 16}), country, SEED + 71);
        Paths.Style narrow = Paths.countryPath(2, SEED + 72);
        Paths.path(c, g, List.of(new int[]{0, -15}, new int[]{1, -17}), narrow, SEED + 73);
        Paths.path(c, g, List.of(new int[]{-15, 0}, new int[]{-19, -1}), narrow, SEED + 74);
        Paths.path(c, g, List.of(new int[]{15, 0}, new int[]{17, 0}), narrow, SEED + 75);
        Paths.path(c, g, List.of(new int[]{11, 9}, new int[]{12, 12}, new int[]{13, 14}), narrow, SEED + 76);
        // The fence along the south, a gate in it, lamps either side.
        for (int x = -22; x <= 22; x++) {
            if (Math.abs(x) <= 1) continue;
            c.set(x, -1, FENCE_Z, "minecraft:grass_block");
            for (int y = 1; y <= 3; y++) c.carve(x, y, FENCE_Z);
            c.set(x, 0, FENCE_Z, St.fence("minecraft:spruce_fence"));
        }
        c.set(0, 0, FENCE_Z, St.gate("minecraft:spruce_fence_gate", Dir.SOUTH, false, false));
        c.set(-1, 0, FENCE_Z, St.gate("minecraft:spruce_fence_gate", Dir.SOUTH, false, false));
        c.set(1, 0, FENCE_Z, St.gate("minecraft:spruce_fence_gate", Dir.SOUTH, false, false));
        for (int x : new int[]{-2, 2}) Lighting.lampPost(c, x, 0, FENCE_Z, 2, "minecraft:cobblestone", "minecraft:spruce_fence", null, false);
        c.set(-2, -1, FENCE_Z, "minecraft:cobblestone");
        c.set(2, -1, FENCE_Z, "minecraft:cobblestone");
    }

    /** Trees round the edge, flowers and grass in the meadow. */
    private static void outdoors(Canvas c, Ground g) {
        int[][] trees = {{-23, 16, 0}, {-11, 27, 2}, {24, -18, 0}, {15, -25, 2}, {-25, -19, 1}, {26, 3, 2}, {-15, -24, 0}, {24, 21, 0},
                {-26, 13, 1}, {11, 27, 2}};
        for (int[] t : trees) {
            // Trees just past the meadow's ragged edge get a little ground of their own.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int x = t[0] + dx, z = t[1] + dz;
                    if (g.has(x, z)) continue;
                    c.set(x, -1, z, "minecraft:grass_block");
                    c.set(x, -2, z, "minecraft:dirt");
                    c.set(x, -3, z, "minecraft:stone");
                }
            }
            switch (t[2]) {
                case 0 -> Trees.oak(c, t[0], 0, t[1], 0, SEED + 80 + t[0]);
                case 1 -> Trees.spruce(c, t[0], 0, t[1], 1, SEED + 81 + t[0]);
                default -> Trees.birch(c, t[0], 0, t[1], 0, SEED + 82 + t[0]);
            }
        }
        List<int[]> hedge = new ArrayList<>();
        for (int z = 9; z <= 22; z++) hedge.add(new int[]{-21, z});
        Trees.hedge(c, hedge, 0, 1, "minecraft:oak_leaves", SEED + 85);
        Scatter.flora(c, g, null, 0.35, 1, SEED + 90, Scatter.MEADOW);
        Scatter.prune(c);
        // Nothing tall in the arena: the meadow's tall plants there are cut back to grass.
        for (int x = -ARENA_RADIUS - 1; x <= ARENA_RADIUS + 1; x++) {
            for (int z = -ARENA_RADIUS - 1; z <= ARENA_RADIUS + 1; z++) {
                if (fromCentre(x, z) > ARENA_RADIUS + 1) continue;
                String up = c.get(x, 1, z);
                if (up != null && "upper".equals(St.get(up, "half"))) {
                    c.carve(x, 1, z);
                    c.set(x, 0, z, "minecraft:short_grass");
                }
            }
        }
    }
}
