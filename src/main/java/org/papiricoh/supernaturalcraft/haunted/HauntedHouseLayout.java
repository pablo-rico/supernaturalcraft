package org.papiricoh.supernaturalcraft.haunted;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Decor;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Furniture;
import org.papiricoh.supernaturalcraft.buildkit.Ground;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The haunted house (pure): a derelict Victorian mansion in its own overgrown, fenced garden. Origin = the middle of the front
 * hall's door line on the ground ({@code y = 0} is the first air over the garden; the house's ground floor stands one block up,
 * its boards at {@code y = 0}). Drawn for {@code dir = 0} with the front door to the south, +Z; the seed may mirror it east–west
 * (which side the turret stands on, {@link #mirrored}) before it is turned by {@code dir} quarter turns ({@link Canvas#turn}).
 * Every public point is given as drawn: place it with {@link #at(LayoutPoint, long, int)}.
 * <p>The build (buildkit): two storeys over a cellar, grey weathered stone below a stone belt course and dark clapboard above,
 * under a steep slate roof with dormers, a front-gabled wing with a bay window, an octagonal turret on the other front corner
 * with a lookout under its spire, and a wrap-around porch that curves round the turret's foot. Brick chimneys on the west gable
 * and through the kitchen. Ground floor: the hall with the grand stair and a back door, the parlour (soul-fire hearth, sheeted
 * sofa and piano), the dining room (a laid table under a dust sheet), the study opening into the turret, the kitchen with the
 * cellar stair. First floor: the master bedroom over the parlour, a guest room, the nursery in the turret, a bathroom; a narrow
 * stair up to the attic of trunks and sheeted junk, and through it to the turret's lookout. The garden: a wrought-iron fence and
 * a broken gate, a path to the porch steps, dead trees and spruces, hedges gone wild, a small family graveyard behind the house
 * and a dry well with a ladder down it. The seed also sets how far gone it is ({@link #ruin}: broken and boarded windows, holes
 * in the roof, a sagging porch, gaps in the fence, ivy).
 * <p><b>The family</b>: 2–4 of the five {@link Hiding} spots hold a ghost's bones ({@link #hidings}). The plan leaves each one an
 * air cell ({@link #bones}; the writer puts {@code supernaturalcraft:grave_bones} there) on a solid floor, under a cover one can
 * dig or break from a cell a hunter can walk (or climb the well's ladder) to ({@link Spot#access}), with the cell over the bones
 * free once the cover is gone so they can be salted and burned.
 * <p><b>Contract</b>: {@link #STUDY_CHEST} is free floor in an alcove of the study's shelves (code puts the study's chest there:
 * {@link #STUDY_LOOT}); {@link #CHESTS} hold {@link #loot}; everything fits in {@link #RADIUS}, {@link #HEIGHT} and {@link #DEPTH}.
 */
public final class HauntedHouseLayout {

    public static final int RADIUS = 30, HEIGHT = 28, DEPTH = 9;
    /** Outside the front gate, on the path. */
    public static final LayoutPoint ENTRY = new LayoutPoint(1, 0, 17);
    /** Where the study's chest appears (free floor, in a gap of the shelves). */
    public static final LayoutPoint STUDY_CHEST = new LayoutPoint(7, 1, -1);
    /** The loot chests: cellar, master bedroom, nursery, attic. */
    public static final List<LayoutPoint> CHESTS = List.of(new LayoutPoint(6, -4, -7), new LayoutPoint(-9, 7, 7),
            new LayoutPoint(9, 7, -1), new LayoutPoint(5, 12, -4));
    public static final String LOOT_PREFIX = "supernaturalcraft:chests/haunted_house/";
    /** The study's chest (placed by code once the family is at rest). */
    public static final String STUDY_LOOT = LOOT_PREFIX + "study";
    private static final String[] CHEST_LOOT = {"cellar", "bedroom", "nursery", "attic"};

    /** The loot table of chest {@code i} of {@link #CHESTS}. */
    public static String loot(int i) {
        return LOOT_PREFIX + CHEST_LOOT[i];
    }

    /** Where a ghost's bones may be hidden. */
    public enum Hiding {
        /** Behind the cellar's bookshelves, in a bricked-up niche. */
        CELLAR,
        /** In one of the three graves of the family plot behind the house, two blocks down. */
        GRAVEYARD,
        /** Behind a false wall of newer boards at the attic's west gable. */
        ATTIC,
        /** Under the mud at the bottom of the dry well. */
        WELL,
        /** Under a loose board of the parlour floor. */
        PARLOUR
    }

    /** One hiding place: the bones' cell and the cell (walkable, or on the well's floor) one digs them out from. */
    public record Spot(Hiding where, LayoutPoint bones, LayoutPoint access) {
    }

    // --- geometry (as drawn, unmirrored) -----------------------------------------------------------------------------------

    /** The main body's and the front wing's wall outlines. */
    static final Box BODY = new Box(-10, 0, -8, 10, 0, 4), WING = new Box(-10, 0, 4, -2, 0, 9);
    /** Floor rows: ground floor, first floor, attic (= wall top). */
    static final int F0 = 0, F1 = 6, F2 = 11, WALL_TOP = 11;
    /** The turret: an octagon of radius 3 round (TX, TZ), walls up to {@code TURRET_TOP}. */
    static final int TX = 10, TZ = 4, TURRET_TOP = 17;
    /** The cellar's air (under the kitchen). */
    static final Box CELLAR = new Box(5, -4, -7, 9, -1, -2);
    /** The garden fence's line, the graveyard's fence, the well's ring. */
    static final Box GARDEN = new Box(-18, 0, -21, 20, 0, 15), GRAVEYARD = new Box(-16, 0, -19, -9, 0, -13),
            WELL = new Box(12, 0, -16, 15, 0, -13);
    static final int[] GRAVES = {-14, -12, -10};
    static final int GRAVE_Z = -17;

    private HauntedHouseLayout() {
    }

    // --- the seed ----------------------------------------------------------------------------------------------------------

    static int mix(long seed) {
        return (int) (seed ^ (seed >>> 32));
    }

    /** Whether the house is drawn mirrored east–west (turret on the west, wing on the east). */
    public static boolean mirrored(long seed) {
        return Noise.chance(1, 7, 3, mix(seed) + 60, 0.5);
    }

    /** How far gone it is: 0 (shut up for years), 1 (falling in), 2 (a wreck). */
    public static int ruin(long seed) {
        return Noise.pick(2, 9, 5, mix(seed) + 61, 3);
    }

    /** Which grave of the family plot holds bones, if the plot is one of the hidings. */
    static int grave(long seed) {
        return Noise.pick(4, 4, 4, mix(seed) + 62, GRAVES.length);
    }

    /** The 2–4 hidings that hold bones (one ghost each), in {@link Hiding} order. */
    public static List<Hiding> hidings(long seed) {
        int s = mix(seed);
        int n = 2 + Noise.pick(3, 1, 4, s + 63, 3);
        List<Hiding> all = new ArrayList<>(List.of(Hiding.values()));
        all.sort(Comparator.comparingDouble(h -> Noise.hash01(h.ordinal(), 5, 9, s + 64)));
        List<Hiding> out = new ArrayList<>(all.subList(0, n));
        out.sort(Comparator.naturalOrder());
        return List.copyOf(out);
    }

    /** The spot of {@code h} as drawn (unmirrored, dir 0). */
    static Spot drawn(Hiding h, long seed) {
        return switch (h) {
            case CELLAR -> new Spot(h, new LayoutPoint(3, -4, -5), new LayoutPoint(5, -4, -5));
            case GRAVEYARD -> {
                int gx = GRAVES[grave(seed)];
                yield new Spot(h, new LayoutPoint(gx, -3, GRAVE_Z), new LayoutPoint(gx, 0, GRAVE_Z));
            }
            case ATTIC -> new Spot(h, new LayoutPoint(-9, 12, -2), new LayoutPoint(-7, 12, -2));
            case WELL -> new Spot(h, new LayoutPoint(14, -7, -14), new LayoutPoint(14, -5, -14));
            case PARLOUR -> new Spot(h, new LayoutPoint(-5, -1, 6), new LayoutPoint(-5, 1, 6));
        };
    }

    /** The bones' hiding spots of a house, placed (mirrored by the seed, turned to {@code dir}). */
    public static List<Spot> spots(long seed, int dir) {
        List<Spot> out = new ArrayList<>();
        for (Hiding h : hidings(seed)) {
            Spot s = drawn(h, seed);
            out.add(new Spot(h, at(s.bones(), seed, dir), at(s.access(), seed, dir)));
        }
        return out;
    }

    /** Where the ghosts' bones go (2–4 cells, air in the plan), placed. */
    public static List<LayoutPoint> bones(long seed, int dir) {
        return spots(seed, dir).stream().map(Spot::bones).toList();
    }

    /** {@code p} (as drawn) mirrored as the seed says and turned to {@code dir}. */
    public static LayoutPoint at(LayoutPoint p, long seed, int dir) {
        int x = mirrored(seed) ? -p.x() : p.x();
        int[] r = Canvas.turn(x, p.z(), dir);
        return new LayoutPoint(r[0], p.y(), r[1]);
    }

    public static LayoutPlan plan(long seed, int dir) {
        return canvas(seed, dir).toPlan();
    }

    /** The house as a finished kit canvas (tests and the layout dump). */
    public static Canvas canvas(long seed, int dir) {
        Canvas south = draw(seed);
        if (mirrored(seed)) south = mirror(south);
        Canvas c = new Canvas("haunted_house");
        c.zone("house", new Box(-RADIUS, -DEPTH, -RADIUS, RADIUS, HEIGHT, RADIUS));
        Canvas src = south;
        c.inZone("house", () -> c.stamp(src, 0, 0, 0, Math.floorMod(dir, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    /** Whether the sampled ground (tops and ocean floors at the centre and the corners) is dry land within 3 blocks of level. */
    public static boolean suitable(int[] tops, int[] floors, int seaLevel) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < tops.length; i++) {
            if (floors[i] < tops[i] || tops[i] <= seaLevel) return false;
            min = Math.min(min, tops[i]);
            max = Math.max(max, tops[i]);
        }
        return max - min <= 3;
    }

    // --- shapes ------------------------------------------------------------------------------------------------------------

    /** An octagon of radius {@code r}: a square with its corners cut. */
    static boolean oct(int dx, int dz, int r) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        return Math.max(ax, az) <= r && ax + az <= r + (r - 1) / 2;
    }

    static boolean house(int x, int z) {
        return BODY.containsColumn(x, z) || WING.containsColumn(x, z);
    }

    static boolean turret(int x, int z) {
        return oct(x - TX, z - TZ, 3);
    }

    /** A cell of the house's (body + wing) outer wall: in the house with a neighbour (diagonals too) outside it. */
    static boolean houseWall(int x, int z) {
        if (!house(x, z)) return false;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (!house(x + dx, z + dz)) return true;
        return false;
    }

    static boolean houseInside(int x, int z) {
        return house(x, z) && !houseWall(x, z);
    }

    static boolean turretWall(int x, int z) {
        if (!turret(x, z)) return false;
        for (Dir d : Dir.HORIZONTAL) if (!turret(x + d.dx, z + d.dz)) return true;
        return false;
    }

    /** The porch deck: a band along the front between wing and turret, down the east side, and round the turret's foot. */
    static boolean deck(int x, int z) {
        if (house(x, z) || turret(x, z)) return false;
        boolean front = z >= 5 && z <= 7 && x >= -1 && x <= 9;
        boolean east = x >= 11 && x <= 13 && z >= -6 && z <= 1;
        boolean round = oct(x - TX, z - TZ, 5) && z >= -1 && x >= 5;
        return front || east || round;
    }

    static Walls.Run north() {
        return new Walls.Run(-10, -8, 10, -8, Dir.NORTH);
    }

    static Walls.Run east() {
        return new Walls.Run(10, -8, 10, 4, Dir.EAST);
    }

    /** The body's front, from the turret to the wing. */
    static Walls.Run front() {
        return new Walls.Run(10, 4, -2, 4, Dir.SOUTH);
    }

    static Walls.Run wingSide() {
        return new Walls.Run(-2, 4, -2, 9, Dir.EAST);
    }

    static Walls.Run wingFront() {
        return new Walls.Run(-2, 9, -10, 9, Dir.SOUTH);
    }

    static Walls.Run west() {
        return new Walls.Run(-10, 9, -10, -8, Dir.WEST);
    }

    static List<Walls.Run> runs() {
        return List.of(north(), east(), front(), wingSide(), wingFront(), west());
    }

    // --- brushes -----------------------------------------------------------------------------------------------------------

    static Brush stone(int s) {
        Brush b = Palette.weathered(Palette.STONE_BRICK_WEAR, Palette.Field.nearGround(1, 4), 0.3, s + 10);
        return b.sprinkle(Palette.mix(s + 11, "minecraft:andesite", 3, "minecraft:tuff", 2, "minecraft:cobblestone", 1), 0.16, s + 12);
    }

    /** Clapboard: courses of dark boards with paler ones between, a board gone grey here and there. */
    static Brush clapboard(int s) {
        return (x, y, z) -> {
            double n = Noise.hash01(x, y, z, s + 13);
            if (n < 0.06) return "minecraft:stripped_dark_oak_wood[axis=y]";
            if (n < 0.1) return "minecraft:spruce_planks";
            return (y & 1) == 0 ? "minecraft:dark_oak_planks" : "minecraft:stripped_spruce_wood[axis=y]";
        };
    }

    static final Brush PLINTH = Palette.patches(71, 2, "minecraft:cobbled_deepslate", 4, "minecraft:deepslate_bricks", 3,
            "minecraft:cracked_deepslate_bricks", 2, "minecraft:mossy_cobblestone", 1);
    static final Brush FLOOR = Palette.boards(true, Brush.of("minecraft:dark_oak_planks"),
            Brush.of("minecraft:stripped_dark_oak_wood[axis=y]"), 4);
    static final Brush UPPER_FLOOR = Palette.boards(false, Brush.of("minecraft:dark_oak_planks"),
            Brush.of("minecraft:stripped_dark_oak_wood[axis=y]"), 3);
    static final Brush PANEL = Palette.mix(72, "minecraft:stripped_dark_oak_wood[axis=y]", 4, "minecraft:dark_oak_planks", 3,
            "minecraft:spruce_planks", 1);
    static final Brush BRICK = Palette.patches(73, 2, "minecraft:bricks", 6, "minecraft:mud_bricks", 1, "minecraft:granite", 1,
            "minecraft:cracked_stone_bricks", 1);
    static final String POST = "minecraft:stripped_dark_oak_log[axis=y]";
    static final String DARK_FENCE = "minecraft:dark_oak_fence";
    static final String SHEET = "minecraft:white_carpet";

    // --- drawing -----------------------------------------------------------------------------------------------------------

    static Canvas draw(long seed) {
        int s = mix(seed);
        int ruin = ruin(seed);
        List<Hiding> hide = hidings(seed);
        Canvas c = new Canvas("haunted_south");
        Ground g = ground(c, s);
        foundations(c, s, hide);
        cellar(c, s, hide);
        shell(c, s, ruin);
        turretShell(c, s);
        groundFloor(c, s, hide);
        firstFloor(c, s);
        roof(c, s, ruin);
        turretTop(c, s);
        attic(c, s, hide);
        porch(c, s, ruin);
        garden(c, g, s, ruin, hide, seed);
        weather(c, s, ruin);
        keepClear(c, seed, hide);
        c.anchor("entry", ENTRY.x(), ENTRY.y(), ENTRY.z());
        c.anchor("study_chest", STUDY_CHEST.x(), STUDY_CHEST.y(), STUDY_CHEST.z());
        for (Hiding h : hide) {
            LayoutPoint b = drawn(h, seed).bones();
            c.anchor("bones_" + h.name().toLowerCase(), b.x(), b.y(), b.z());
        }
        return c;
    }

    /** The garden's ground: a rounded-square patch with a ragged edge, surface at y = -1, the air over it cleared. */
    private static Ground ground(Canvas c, int s) {
        Ground g = new Ground();
        Brush grass = Palette.patches2(s + 1, 4, "minecraft:grass_block", 12, "minecraft:coarse_dirt", 3, "minecraft:podzol", 2,
                "minecraft:moss_block", 1, "minecraft:rooted_dirt", 1);
        double cx = 1, cz = -3, rx = 25, rz = 24;
        for (int x = -29; x <= 29; x++) {
            for (int z = -29; z <= 29; z++) {
                double u = (x - cx) / rx, v = (z - cz) / rz;
                double d = Math.pow(Math.abs(u), 4) + Math.pow(Math.abs(v), 4);
                double edge = 0.85 + 0.25 * Noise.value2(x, z, 6, s + 2);
                if (d > edge) continue;
                c.set(x, -1, z, grass.at(x, -1, z));
                c.set(x, -2, z, "minecraft:dirt");
                c.set(x, -3, z, "minecraft:dirt");
                c.set(x, -4, z, "minecraft:stone");
                boolean near = Math.abs(x - 1) <= 17 && Math.abs(z + 1) <= 14;
                int up = d > edge - 0.12 ? 4 : near ? 26 : 12;
                for (int y = 0; y < up; y++) c.carve(x, y, z);
                g.set(x, z, -1, Math.max(0.05, 1 - d / edge));
            }
        }
        return g;
    }

    /** Footings, the ground floor's boards and what lies under them (and the parlour's loose board). */
    private static void foundations(Canvas c, int s, List<Hiding> hide) {
        for (int x = -11; x <= 14; x++) {
            for (int z = -9; z <= 10; z++) {
                if (!house(x, z) && !turret(x, z)) continue;
                for (int y = -3; y <= -1; y++) c.set(x, y, z, y == -1 ? PLINTH.at(x, y, z) : "minecraft:cobbled_deepslate");
                c.set(x, F0, z, FLOOR.at(x, F0, z));
            }
        }
        // The kitchen and the bathroom over it get tiles, the hall a darker parquet.
        Brush tiles = Palette.checker(1, Brush.of("minecraft:polished_andesite"), Palette.mix(s + 14, "minecraft:polished_deepslate", 4,
                "minecraft:cracked_deepslate_tiles", 1));
        for (int x = 4; x <= 9; x++) for (int z = -7; z <= -3; z++) c.set(x, F0, z, tiles.at(x, F0, z));
        if (hide.contains(Hiding.PARLOUR)) {
            LayoutPoint b = drawn(Hiding.PARLOUR, 0).bones();
            c.carve(b.x(), b.y(), b.z());
            c.set(b.x(), F0, b.z(), "minecraft:spruce_planks");
        }
    }

    /** The outer walls of body and wing: stone below a belt course, clapboard above, windows, doors, chimneys. */
    private static void shell(Canvas c, int s, int ruin) {
        Brush stone = stone(s), clap = clapboard(s);
        for (int x = -11; x <= 11; x++) {
            for (int z = -9; z <= 10; z++) {
                if (!house(x, z)) continue;
                if (houseWall(x, z)) {
                    c.set(x, F0, z, PLINTH.at(x, F0, z));
                    for (int y = 1; y < F1; y++) c.set(x, y, z, stone.at(x, y, z));
                    c.set(x, F1, z, "minecraft:dark_oak_planks");
                    for (int y = F1 + 1; y <= WALL_TOP; y++) c.set(x, y, z, clap.at(x, y, z));
                } else {
                    for (int y = 1; y < F1; y++) c.carve(x, y, z);
                    c.set(x, F1, z, UPPER_FLOOR.at(x, F1, z));
                    for (int y = F1 + 1; y < F2; y++) c.carve(x, y, z);
                    c.set(x, F2, z, Palette.boards(true, Brush.of("minecraft:spruce_planks"), Brush.of("minecraft:dark_oak_planks"), 3)
                            .at(x, F2, z));
                }
            }
        }
        for (Walls.Run r : runs()) {
            Walls.plinth(c, r, 0, 1, Family.COBBLED_DEEPSLATE, true);
            Walls.belt(c, r, F1, Family.STONE_BRICKS);
            Walls.cornice(c, r, WALL_TOP, Wood.DARK_OAK.family(), St.trapdoor("minecraft:dark_oak_trapdoor", r.out(), true, false));
        }
        // Corner posts up the clapboard, quoins down the stone.
        for (int[] k : new int[][]{{-10, -8}, {10, -8}, {-10, 9}, {-2, 9}, {-2, 4}}) {
            for (int y = F1 + 1; y < WALL_TOP; y++) c.set(k[0], y, k[1], POST);
            for (int y = 1; y < F1; y += 2) c.set(k[0], y, k[1], "minecraft:polished_andesite");
        }
        // Partitions, both floors.
        for (int y : new int[]{1, F1 + 1}) {
            int top = y == 1 ? F1 - 1 : F2 - 1;
            for (int yy = y; yy <= top; yy++) {
                for (int z = -7; z <= 3; z++) {
                    c.set(-3, yy, z, PANEL.at(-3, yy, z));
                    c.set(3, yy, z, PANEL.at(3, yy, z));
                }
                for (int x : new int[]{-9, -8, -7, -6, -5, -4, 4, 5, 6, 7, 8, 9}) c.set(x, yy, -2, PANEL.at(x, yy, -2));
            }
        }
        for (int[] d : new int[][]{{-3, 1, -1}, {-3, 1, -5}, {3, 1, 1}, {3, 1, -5}, {-5, 1, -2}, {-3, F1 + 1, 3}, {-3, F1 + 1, -5},
                {3, F1 + 1, 2}, {3, F1 + 1, -5}}) doorway(c, d[0], d[1], d[2], s);

        windows(c, s, ruin);
        // The front door: a double door of dark oak, lamps on brackets either side.
        Openings.door(c, front(), 9, 1, 2, new Openings.DoorStyle("minecraft:dark_oak_door", Brush.of(POST), null, null, null,
                "minecraft:dark_oak_trapdoor"));
        c.set(1, 3, 4, "minecraft:chiseled_stone_bricks");
        c.set(0, 3, 4, "minecraft:chiseled_stone_bricks");
        // The back door, a stone step and a little hood over it.
        Openings.door(c, north(), 10, 1, 1, new Openings.DoorStyle("minecraft:spruce_door", Brush.of(POST), null, Family.STONE_BRICKS,
                Wood.DARK_OAK.family(), null));
        chimneys(c, s);
    }

    private static void doorway(Canvas c, int x, int y, int z, int s) {
        c.carve(x, y, z);
        c.carve(x, y + 1, z);
        boolean alongX = x != -3 && x != 3;
        c.set(x, y + 2, z, St.axis("minecraft:stripped_dark_oak_log", alongX ? "x" : "z"));
        // Now and then the door still hangs there, ajar.
        if (Noise.chance(x, y, z, s + 15, 0.4)) {
            Dir f = alongX ? Dir.SOUTH : Dir.EAST;
            c.setPair(x, y, z, St.door("minecraft:dark_oak_door", f, Noise.chance(x, y, z, s + 16, 0.5), true));
        }
    }

    private static void windows(Canvas c, int s, int ruin) {
        Openings.WindowStyle tall = new Openings.WindowStyle("minecraft:glass_pane", null, Family.STONE_BRICKS, Family.STONE_BRICKS,
                "minecraft:dark_oak_trapdoor", null, false, null);
        Openings.WindowStyle bay = new Openings.WindowStyle("minecraft:glass_pane", Brush.of(POST), Family.STONE_BRICKS,
                Family.STONE_BRICKS, null, null, false, POST);
        Openings.WindowStyle upper = new Openings.WindowStyle("minecraft:glass_pane", null, Wood.DARK_OAK.family(),
                Wood.DARK_OAK.family(), "minecraft:spruce_trapdoor", null, false, null);
        Openings.WindowStyle upperBay = new Openings.WindowStyle("minecraft:glass_pane", Brush.of(POST), Wood.DARK_OAK.family(),
                Wood.DARK_OAK.family(), null, null, false, POST);
        Walls.Run n = north(), e = east(), f = front(), ws = wingSide(), wf = wingFront(), w = west();
        // Ground floor: tall sashes (three rows of glass) with stone sills and hoods.
        for (int i : new int[]{2, 5, 14, 19}) Openings.window(c, n, i, 2, 1, 3, tall);
        for (int i : new int[]{2, 7}) Openings.window(c, e, i, 2, 1, 3, tall);
        for (int i : new int[]{5, 11}) Openings.window(c, f, i, 2, 1, 3, tall);
        Openings.window(c, ws, 3, 2, 1, 3, tall);
        Openings.window(c, wf, 3, 2, 3, 3, bay);
        for (int i : new int[]{3, 14}) Openings.window(c, w, i, 2, 1, 3, tall);
        // First floor.
        for (int i : new int[]{2, 5, 8, 12, 15, 19}) Openings.window(c, n, i, 8, 1, 2, upper);
        for (int i : new int[]{2, 7}) Openings.window(c, e, i, 8, 1, 2, upper);
        for (int i : new int[]{5, 9, 11}) Openings.window(c, f, i, 8, 1, 2, upper);
        Openings.window(c, ws, 3, 8, 1, 2, upper);
        Openings.window(c, wf, 3, 8, 3, 2, upperBay);
        for (int i : new int[]{3, 6, 14}) Openings.window(c, w, i, 8, 1, 2, upper);
        // Glass gone, a pane boarded over, a shutter fallen: how much depends on how far gone the house is.
        double broken = 0.18 + 0.14 * ruin, boarded = 0.08 + 0.06 * ruin;
        for (Walls.Run r : runs()) {
            for (int i = 0; i < r.length(); i++) {
                int x = r.x(i), z = r.z(i);
                for (int y = 1; y < WALL_TOP; y++) {
                    String b = c.get(x, y, z);
                    if (b == null || !St.path(b).equals("glass_pane")) continue;
                    double n0 = Noise.hash01(x, y, z, s + 17);
                    if (n0 < broken) c.carve(x, y, z);
                    else if (n0 < broken + boarded) c.set(x, y, z, y < F1 ? "minecraft:spruce_planks" : "minecraft:stripped_spruce_wood[axis=y]");
                    else if (n0 > 0.9) c.set(x, y, z, St.pane("minecraft:gray_stained_glass_pane"));
                }
                for (int y = 1; y < WALL_TOP; y++) {
                    int ox = x + r.out().dx, oz = z + r.out().dz;
                    String b = c.get(ox, y, oz);
                    if (b != null && St.path(b).endsWith("_trapdoor") && "true".equals(St.get(b, "open"))
                            && Noise.chance(ox, y, oz, s + 18, 0.15 + 0.1 * ruin)) c.carve(ox, y, oz);
                }
            }
        }
    }

    private static void chimneys(Canvas c, int s) {
        // The west chimney: a brick stack against the gable end (the parlour's and the bedroom's hearths), clear of the eaves.
        for (int y = -1; y <= WALL_TOP - 1; y++) {
            for (int z = -1; z <= 1; z++) {
                c.set(-11, y, z, BRICK.at(-11, y, z));
                c.set(-12, y, z, BRICK.at(-12, y, z));
            }
        }
        for (int z = -1; z <= 1; z++) c.set(-11, WALL_TOP, z, Family.BRICKS.stairs(Dir.EAST, false));
        for (int y = WALL_TOP; y <= 20; y++) {
            for (int z = -1; z <= 1; z++) c.set(-12, y, z, BRICK.at(-12, y, z));
        }
        for (int z = -2; z <= 2; z++) c.set(-13, 20, z, Family.DEEPSLATE_BRICKS.stairs(Dir.EAST, true));
        for (int z = -1; z <= 1; z += 2) c.set(-12, 21, z, Family.DEEPSLATE_BRICKS.slabBottom());
        c.set(-12, 20, -2, Family.DEEPSLATE_BRICKS.stairs(Dir.NORTH, true));
        c.set(-12, 20, 2, Family.DEEPSLATE_BRICKS.stairs(Dir.SOUTH, true));
        for (int z = -1; z <= 1; z++) c.setIfAir(-11, 20, z, Family.DEEPSLATE_BRICKS.stairs(Dir.WEST, true));
        c.set(-12, 21, 0, St.wall("minecraft:mud_brick_wall"));
    }

    /** The turret's shaft: an octagon open to the study and the nursery on its inner side, three floors, windows round it. */
    private static void turretShell(Canvas c, int s) {
        Brush stone = stone(s), clap = clapboard(s);
        Brush shingles = Palette.patches(s + 19, 1.5, "minecraft:deepslate_tiles", 4, "minecraft:cracked_deepslate_tiles", 1,
                "minecraft:polished_deepslate", 1);
        for (int x = TX - 3; x <= TX + 3; x++) {
            for (int z = TZ - 3; z <= TZ + 3; z++) {
                if (!turret(x, z)) continue;
                boolean inBody = houseInside(x, z);
                if (turretWall(x, z)) {
                    for (int y = -3; y <= TURRET_TOP; y++) {
                        if (inBody && y >= F0 && y <= WALL_TOP) continue;
                        if (y >= -3 && y <= -1) c.set(x, y, z, "minecraft:cobbled_deepslate");
                        else if (y == F0) c.set(x, y, z, PLINTH.at(x, y, z));
                        else if (y < F1) c.set(x, y, z, stone.at(x, y, z));
                        else if (y == F1) c.set(x, y, z, "minecraft:dark_oak_planks");
                        else if (y <= WALL_TOP) c.set(x, y, z, clap.at(x, y, z));
                        else c.set(x, y, z, shingles.at(x, y, z));
                    }
                } else {
                    for (int y = -3; y <= -1; y++) c.set(x, y, z, "minecraft:cobbled_deepslate");
                    c.set(x, F0, z, FLOOR.at(x, F0, z));
                    for (int y = 1; y < TURRET_TOP; y++) c.carve(x, y, z);
                    c.set(x, F1, z, UPPER_FLOOR.at(x, F1, z));
                    c.set(x, F2, z, "minecraft:spruce_planks");
                    c.set(x, TURRET_TOP, z, PANEL.at(x, TURRET_TOP, z));
                }
            }
        }
        // Tall windows on its outer faces, both floors; a ring of lights under the spire.
        int[][] faces = {{TX + 3, TZ}, {TX, TZ + 3}, {TX + 2, TZ + 2}, {TX + 3, TZ - 1}, {TX - 1, TZ + 3}};
        for (int[] w : faces) {
            for (int y : new int[]{2, 3, 4, F1 + 2, F1 + 3}) c.set(w[0], y, w[1], St.pane("minecraft:glass_pane"));
        }
        for (int[] w : new int[][]{{TX + 3, TZ}, {TX, TZ + 3}, {TX - 3, TZ}, {TX, TZ - 3}, {TX + 2, TZ + 2}, {TX - 2, TZ - 2},
                {TX + 2, TZ - 2}, {TX - 2, TZ + 2}}) {
            for (int y = F2 + 2; y <= F2 + 4; y++) c.set(w[0], y, w[1], St.pane("minecraft:glass_pane"));
        }
        // A belt course round it at the first floor and under the lookout.
        for (int x = TX - 4; x <= TX + 4; x++) {
            for (int z = TZ - 4; z <= TZ + 4; z++) {
                if (turret(x, z) || house(x, z) || deck(x, z)) continue;
                Dir toward = null;
                for (Dir d : Dir.HORIZONTAL) if (turretWall(x + d.dx, z + d.dz)) toward = d;
                if (toward == null) continue;
                c.setIfAir(x, WALL_TOP, z, Family.DEEPSLATE_TILES.stairs(toward, true));
            }
        }
    }

    // --- rooms -------------------------------------------------------------------------------------------------------------

    private static void groundFloor(Canvas c, int s, List<Hiding> hide) {
        int y = F0 + 1;
        // The hall: the grand stair up the west side, a runner to the back door, a clock, a chandelier.
        Furniture.flight(c, -2, y, 3, Dir.NORTH, 6, 2, "minecraft:dark_oak_stairs", true);
        for (int z = -7; z <= 3; z++) c.set(1, y, z, St.carpet(z % 4 == 0 ? "black" : "red"));
        // The long-case clock, stopped.
        c.set(2, y, -7, "minecraft:stripped_spruce_wood[axis=y]");
        c.set(2, y + 1, -7, "minecraft:spruce_planks");
        c.set(2, y + 2, -7, Wood.SPRUCE.stairs(Dir.SOUTH, true));
        c.decor(Decor.frame(2, y + 1, -6, Dir.SOUTH, "minecraft:clock"));
        c.set(-2, y, -7, "minecraft:dark_oak_planks");
        c.set(-2, y + 1, -7, St.potted("dead_bush"));
        c.set(-1, y, -7, St.slabTop("minecraft:dark_oak_slab"));
        c.set(-1, y + 1, -7, St.candle("white", 2, true));
        Lighting.chandelier(c, 1, F1, -5, 2, DARK_FENCE, "white");
        Lighting.hanging(c, 1, F1, 2, 1, false);
        c.decor(Decor.painting(-2, y + 1, -4, Dir.EAST, "wanderer"));
        c.decor(Decor.painting(2, y + 1, -2, Dir.WEST, "graham"));

        // The parlour (west, into the wing): a soul-fire hearth, a sheeted sofa and chairs, a sheeted piano in the bay.
        Furniture.fireplace(c, -9, y, 0, Dir.EAST, Family.DEEPSLATE_BRICKS, false);
        c.set(-8, y + 3, -1, St.candle("white", 3, true));
        c.set(-8, y + 3, 1, St.candle("white", 2, true));
        c.decor(Decor.painting(-9, y + 3, 0, Dir.EAST, "wasteland"));
        Furniture.sofa(c, -6, y, 2, Dir.WEST, 3, "minecraft:smooth_quartz_stairs", "minecraft:birch_trapdoor");
        for (int z = 0; z <= 2; z++) c.set(-6, y + 1, z, Noise.chance(-6, 1, z, s + 21, 0.5) ? SHEET : "minecraft:air");
        sheetedChair(c, -8, y, 3);
        sheetedChair(c, -8, y, -1);
        for (int x = -8; x <= -6; x++) {
            c.set(x, y, 8, "minecraft:dark_oak_planks");
            c.set(x, y + 1, 8, SHEET);
        }
        c.set(-7, y, 7, St.stairs("minecraft:dark_oak_stairs", Dir.SOUTH, false));
        c.set(-4, y, 8, St.slabTop("minecraft:spruce_slab"));
        c.set(-4, y + 1, 8, St.candle("white", 1, true));
        for (int x = -7; x <= -5; x++) for (int z = 3; z <= 5; z++) c.setIfAir(x, y, z, St.carpet("red"));
        Lighting.chandelier(c, -6, F1, 5, 2, DARK_FENCE, "white");
        c.set(-9, y, 6, "minecraft:dark_oak_planks");
        c.set(-9, y + 1, 6, St.potted("wither_rose"));
        c.set(-4, y, 3, "minecraft:jukebox");
        c.set(-4, y + 1, 3, St.candle("black", 2, true));

        // The dining room: the long table under a dust sheet, its chairs pushed back, a cabinet of china, a chandelier.
        Furniture.diningSet(c, -8, -5, -6, -5, y, "minecraft:dark_oak_slab", "minecraft:dark_oak_stairs");
        for (int x = -8; x <= -6; x++) c.setIfAir(x, y + 1, -5, SHEET);
        c.set(-7, y + 1, -5, St.candle("white", 3, true));
        Furniture.cabinet(c, -9, y, -7, Dir.EAST, "minecraft:dark_oak_planks", "minecraft:dark_oak_trapdoor", false);
        Furniture.cabinet(c, -9, y + 1, -7, Dir.EAST, "minecraft:dark_oak_planks", "minecraft:dark_oak_trapdoor", false);
        c.set(-9, y + 2, -7, St.candle("white", 1, true));
        Lighting.chandelier(c, -7, F1, -5, 2, DARK_FENCE, "white");
        c.decor(Decor.painting(-4, y + 2, -5, Dir.WEST, "graham"));

        // The study: shelves floor to ceiling with a gap where something belongs, a desk in the turret's bay, a reading lamp.
        Furniture.bookshelves(c, 4, -1, Dir.SOUTH, 2, y, y + 3, Dir.EAST, false, null, 0);
        Furniture.bookshelves(c, 4, 2, Dir.SOUTH, 2, y, y + 3, Dir.EAST, false, POST, 2);
        Furniture.bookshelves(c, 5, -1, Dir.EAST, 2, y, y + 3, Dir.SOUTH, false, null, 0);
        Furniture.bookshelves(c, 8, -1, Dir.EAST, 2, y, y + 3, Dir.SOUTH, false, null, 0);
        c.set(7, y + 3, -1, "minecraft:bookshelf");
        c.set(7, y + 2, -1, St.stairs("minecraft:dark_oak_stairs", Dir.NORTH, true));
        Furniture.desk(c, 12, y, 4, Dir.EAST, "minecraft:dark_oak_slab", "minecraft:dark_oak_stairs", false);
        c.set(12, y + 1, 4, St.candle("white", 3, true));
        c.set(10, y, 6, St.lectern(Dir.NORTH));
        sheetedChair(c, 6, y, 2);
        c.set(5, y, 3, St.slabTop("minecraft:spruce_slab"));
        c.set(5, y + 1, 3, St.candle("red", 2, true));
        Lighting.hanging(c, 6, F1, 1, 1, true);
        Lighting.hanging(c, TX, F1, TZ, 1, false);
        c.decor(Decor.painting(5, y + 2, -1, Dir.SOUTH, "kebab"));

        // The kitchen: a range on the chimney breast, counters, the cellar stair behind a rail, herbs drying under the joists.
        Furniture.kitchen(c, 4, y, -7, Dir.EAST, 2, Dir.SOUTH, "minecraft:stripped_dark_oak_wood[axis=y]", "minecraft:dark_oak_trapdoor", false);
        c.set(6, y, -7, St.facing("minecraft:smoker", Dir.SOUTH));
        c.set(7, y, -7, St.facing("minecraft:furnace", Dir.SOUTH));
        Furniture.table(c, 5, -4, 6, -4, y, "minecraft:spruce_slab");
        c.set(5, y + 1, -4, St.candle("white", 2, true));
        c.set(4, y, -3, St.of("water_cauldron", "level", "1"));
        for (int x = 4; x <= 8; x++) {
            for (int z = -6; z <= -3; z++) if (Noise.chance(x, 0, z, s + 20, 0.3)) c.setIfAir(x, F1 - 1, z, "minecraft:hanging_roots");
        }
        Lighting.hanging(c, 5, F1, -5, 1, false);
        Furniture.flight(c, 9, -4, -2, Dir.NORTH, 5, 1, "minecraft:stone_brick_stairs", false);
        for (int z = -5; z <= -3; z++) c.set(8, y, z, St.fence(DARK_FENCE));
    }

    /** A chair under a dust sheet: a lump of white wool with a sheet over it. */
    private static void sheetedChair(Canvas c, int x, int y, int z) {
        c.set(x, y, z, "minecraft:white_wool");
        c.set(x, y + 1, z, SHEET);
    }

    private static void firstFloor(Canvas c, int s) {
        int y = F1 + 1;
        // The landing: a rail round the stairwell, the narrow stair to the attic up the east side.
        for (int z = -1; z <= 1; z++) c.set(0, y, z, St.fence(DARK_FENCE));
        for (int x = -2; x <= -1; x++) c.set(x, y, 2, St.fence(DARK_FENCE));
        Furniture.flight(c, 2, y, 1, Dir.NORTH, 5, 1, "minecraft:spruce_stairs", false);
        for (int z = -7; z <= 3; z++) c.setIfAir(1, y, z, St.carpet("red"));
        Lighting.hanging(c, 0, F2, 3, 1, false);
        Lighting.hanging(c, -1, F2, -6, 1, true);
        Furniture.cabinet(c, -2, y, -7, Dir.SOUTH, "minecraft:dark_oak_planks", "minecraft:spruce_trapdoor", false);
        c.set(2, y, -7, "minecraft:dark_oak_planks");
        c.set(2, y + 1, -7, St.candle("white", 1, true));
        c.decor(Decor.painting(-2, y + 1, -3, Dir.EAST, "kebab"));

        // The master bedroom (west, over the parlour and the wing): a four-poster, a hearth, a vanity, a chest at the bed's foot.
        Furniture.fireplace(c, -9, y, 0, Dir.EAST, Family.DEEPSLATE_BRICKS, false);
        Furniture.bed(c, -8, y, 5, Dir.NORTH, "red", null, false);
        for (int[] p : new int[][]{{-9, 3}, {-7, 3}, {-9, 6}, {-7, 6}}) {
            for (int yy = y; yy <= y + 2; yy++) c.setIfAir(p[0], yy, p[1], St.fence(DARK_FENCE));
        }
        // The tester: a canopy of dark boards hung under the ceiling over the bed.
        for (int x = -9; x <= -7; x++) for (int z = 3; z <= 6; z++) c.set(x, y + 3, z, St.slabTop("minecraft:dark_oak_slab"));
        LayoutPoint bedChest = CHESTS.get(1);
        c.set(bedChest.x(), bedChest.y(), bedChest.z(), St.of("chest", "facing", "east", "type", "single", "waterlogged", "false"));
        c.decor(Decor.loot(bedChest.x(), bedChest.y(), bedChest.z(), loot(1)));
        Furniture.cabinet(c, -4, y, -1, Dir.WEST, "minecraft:dark_oak_planks", "minecraft:dark_oak_trapdoor", false);
        Furniture.cabinet(c, -4, y + 1, -1, Dir.WEST, "minecraft:dark_oak_planks", "minecraft:dark_oak_trapdoor", false);
        c.set(-3, y, 7, St.slabTop("minecraft:spruce_slab"));
        c.set(-3, y + 1, 7, St.candle("white", 2, true));
        sheetedChair(c, -6, y, 8);
        for (int x = -6; x <= -4; x++) for (int z = 2; z <= 4; z++) c.setIfAir(x, y, z, St.carpet("purple"));
        Lighting.chandelier(c, -6, F2, 4, 2, DARK_FENCE, "white");

        // The guest room (west back): a narrow bed, a washstand, the mirror over it cracked.
        Furniture.bed(c, -8, y, -4, Dir.NORTH, "gray", "minecraft:spruce_planks", true);
        Furniture.cabinet(c, -9, y, -7, Dir.EAST, "minecraft:spruce_planks", "minecraft:spruce_trapdoor", false);
        c.set(-5, y, -7, St.slabTop("minecraft:spruce_slab"));
        c.set(-5, y + 1, -7, St.of("water_cauldron", "level", "1"));
        Lighting.hanging(c, -6, F2, -5, 1, false);

        // The nursery (east, into the turret): a cot, a rocking horse, the toy chest, a music box with its candle.
        Furniture.bed(c, 11, y, 5, Dir.NORTH, "white", null, false);
        for (int z = 4; z <= 5; z++) c.set(12, y, z, St.fence("minecraft:birch_fence"));
        c.set(10, y, 4, St.fence("minecraft:birch_fence"));
        c.set(6, y, 2, St.stairs("minecraft:spruce_stairs", Dir.EAST, false));
        c.set(7, y, 2, St.stairs("minecraft:spruce_stairs", Dir.WEST, false));
        c.set(6, y + 1, 2, St.fence("minecraft:spruce_fence"));
        LayoutPoint toys = CHESTS.get(2);
        c.set(toys.x(), toys.y(), toys.z(), St.of("chest", "facing", "south", "type", "single", "waterlogged", "false"));
        c.decor(Decor.loot(toys.x(), toys.y(), toys.z(), loot(2)));
        c.set(4, y, -1, "minecraft:note_block");
        c.set(4, y + 1, -1, St.candle("pink", 1, true));
        for (int x = 5; x <= 8; x++) for (int z = 0; z <= 1; z++) c.setIfAir(x, y, z, St.carpet(((x + z) & 1) == 0 ? "pink" : "white"));
        Lighting.hanging(c, 6, F2, 0, 1, false);
        Lighting.hanging(c, TX, F2, TZ, 1, false);
        c.set(9, y, 3, "minecraft:white_wool");
        c.set(9, y + 1, 3, St.head("minecraft:skeleton_skull", 6));

        // The bathroom (east back): a tub (two cauldrons in a dark wood surround), a basin, the chimney breast.
        c.set(8, y, -7, St.of("water_cauldron", "level", "2"));
        c.set(9, y, -7, St.of("cauldron"));
        c.set(9, y, -6, St.slabTop("minecraft:dark_oak_slab"));
        c.set(4, y, -7, St.slabTop("minecraft:polished_andesite_slab"));
        c.set(4, y + 1, -7, St.candle("white", 3, true));
        c.set(4, y + 1, -6, St.candle("white", 1, true));
        c.set(4, y, -6, "minecraft:polished_andesite");
        Lighting.hanging(c, 6, F2, -4, 1, false);
        for (int x = 4; x <= 9; x++) for (int z = -7; z <= -3; z++) {
            String b = c.get(x, F1, z);
            if (b != null && !Kinds.air(b)) c.set(x, F1, z, ((x + z) & 1) == 0 ? "minecraft:calcite" : "minecraft:polished_deepslate");
        }
    }

    private static void roof(Canvas c, int s, int ruin) {
        Brush clap = clapboard(s);
        Roofs.Style slate = new Roofs.Style(Family.DEEPSLATE_TILES, Wood.DARK_OAK.family(), clap, 1, Roofs.Pitch.STEEP, 0, true,
                "minecraft:dark_oak_trapdoor", true);
        Roofs.Style small = slate.withOverhang(0);
        new Roofs.Plan()
                .gable(BODY, WALL_TOP, true, slate)
                .gable(WING, WALL_TOP, false, slate)
                .dormer(3, 4, Dir.SOUTH, 3, 3, WALL_TOP + 1, WALL_TOP + 3, clap, "minecraft:glass_pane", small)
                .dormer(-6, -8, Dir.NORTH, 3, 3, WALL_TOP + 1, WALL_TOP + 3, clap, "minecraft:glass_pane", small)
                .dormer(2, -8, Dir.NORTH, 3, 3, WALL_TOP + 1, WALL_TOP + 3, clap, "minecraft:glass_pane", small)
                .draw(c);
        Box over = new Box(-12, WALL_TOP, -10, 12, HEIGHT, 11);
        Palette.swapFamily(c, over, Family.DEEPSLATE_TILES, Family.COBBLED_DEEPSLATE, 0.2, 2.5, s + 30);
        Palette.swapFamily(c, over, Family.DEEPSLATE_TILES, Family.MOSSY_COBBLESTONE, 0.08 + 0.04 * ruin, 2, s + 31);
        // Windows high in the gables (the west one lights a room that is not there); finials on the apexes.
        c.set(-10, WALL_TOP + 2, -2, St.pane("minecraft:glass_pane"));
        c.set(-10, WALL_TOP + 3, -2, St.pane("minecraft:glass_pane"));
        c.set(10, WALL_TOP + 2, -3, St.pane("minecraft:glass_pane"));
        c.set(10, WALL_TOP + 3, -3, St.pane("minecraft:glass_pane"));
        c.set(-6, WALL_TOP + 2, 9, St.pane("minecraft:glass_pane"));
        c.set(-6, WALL_TOP + 3, 9, St.pane("minecraft:glass_pane"));
        for (int[] f : new int[][]{{-6, 9}, {-10, -2}, {10, -2}}) {
            int top = c.topAt(f[0], f[1], HEIGHT, WALL_TOP);
            c.set(f[0], top + 1, f[1], St.fence(DARK_FENCE));
        }
        // The kitchen's chimney breast rises through the house and out of the back slope.
        for (int x = 6; x <= 7; x++) {
            for (int y = F0 + 2; y <= 20; y++) c.set(x, y, -7, BRICK.at(x, y, -7));
            c.set(x, 21, -7, x == 6 ? St.wall("minecraft:mud_brick_wall") : Family.DEEPSLATE_BRICKS.slabBottom());
            c.setIfAir(x, 20, -8, Family.DEEPSLATE_BRICKS.stairs(Dir.SOUTH, true));
            c.setIfAir(x, 20, -6, Family.DEEPSLATE_BRICKS.stairs(Dir.NORTH, true));
        }
        c.setIfAir(5, 20, -7, Family.DEEPSLATE_BRICKS.stairs(Dir.EAST, true));
        c.setIfAir(8, 20, -7, Family.DEEPSLATE_BRICKS.stairs(Dir.WEST, true));
        // Holes where the slates have gone (and the boards under them), a bare rafter left across them.
        if (ruin >= 1) hole(c, -4, -6, -1, -4, s + 32);
        if (ruin >= 2) hole(c, 4, 0, 6, 2, s + 33);
    }

    /** A ragged hole through the roof over the box (x0..x1, z0..z1): roof and ceiling, never the attic floor. */
    private static void hole(Canvas c, int x0, int z0, int x1, int z1, int s) {
        double mx = (x0 + x1) / 2.0, mz = (z0 + z1) / 2.0, rx = (x1 - x0) / 2.0 + 0.5, rz = (z1 - z0) / 2.0 + 0.5;
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                double d = Math.hypot((x - mx) / rx, (z - mz) / rz);
                if (d > 1 + 0.35 * (Noise.hash01(x, 0, z, s) - 0.5)) continue;
                for (int y = F2 + 1; y <= HEIGHT; y++) {
                    String b = c.get(x, y, z);
                    if (b == null || Kinds.air(b)) continue;
                    String p = St.path(b);
                    if (p.contains("deepslate") || p.contains("cobble") || p.contains("dark_oak_s") || p.contains("planks")
                            || p.contains("stripped_spruce")) c.carve(x, y, z);
                }
            }
        }
        int top = c.topAt(x0 - 2, (z0 + z1) / 2, HEIGHT, F2);
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            if (c.isAir(x, top, (z0 + z1) / 2)) c.set(x, top, (z0 + z1) / 2, St.axis("minecraft:dark_oak_log", "x"));
        }
    }

    /** The turret above the eaves: the main roof cut away round it, the lookout's windows, its spire. */
    private static void turretTop(Canvas c, int s) {
        Brush shingles = Palette.patches(s + 19, 1.5, "minecraft:deepslate_tiles", 4, "minecraft:cracked_deepslate_tiles", 1,
                "minecraft:polished_deepslate", 1);
        for (int x = TX - 3; x <= TX + 3; x++) {
            for (int z = TZ - 3; z <= TZ + 3; z++) {
                if (!turret(x, z)) continue;
                for (int y = WALL_TOP + 1; y <= TURRET_TOP; y++) {
                    if (turretWall(x, z)) {
                        String b = c.get(x, y, z);
                        if (b == null || !St.path(b).equals("glass_pane")) c.set(x, y, z, shingles.at(x, y, z));
                    } else if (y < TURRET_TOP) {
                        c.carve(x, y, z);
                    }
                }
            }
        }
        // The way in from the attic.
        c.carve(TX - 1, F2 + 1, TZ - 3);
        c.carve(TX - 1, F2 + 2, TZ - 3);
        c.set(TX - 1, F2 + 3, TZ - 3, St.axis("minecraft:stripped_dark_oak_log", "x"));
        // A witch's hat: octagonal courses stepping in, two rows each (one at the tip), an iron finial.
        for (int r = 4; r >= 1; r--) {
            int y0 = r == 4 ? TURRET_TOP + 1 : TURRET_TOP + 2 + 2 * (3 - r);
            for (int x = TX - r; x <= TX + r; x++) {
                for (int z = TZ - r; z <= TZ + r; z++) {
                    int dx = x - TX, dz = z - TZ;
                    if (!oct(dx, dz, r) || oct(dx, dz, r - 1) && r > 1) continue;
                    if (dx == 0 && dz == 0) continue;
                    Dir in = Dir.toward(-dx, -dz);
                    c.set(x, y0, z, Family.DEEPSLATE_TILES.stairs(in, false));
                    if (r < 4 && r > 1) c.set(x, y0 + 1, z, shingles.at(x, y0 + 1, z));
                    if (r == 4) c.setIfAir(x, y0 - 1, z, St.trapdoorAgainst("minecraft:dark_oak_trapdoor", in));
                }
            }
            // Fill the course's inside so the hat is solid from above.
            for (int x = TX - r + 1; x <= TX + r - 1; x++) {
                for (int z = TZ - r + 1; z <= TZ + r - 1; z++) {
                    if (oct(x - TX, z - TZ, r - 1)) c.set(x, y0, z, shingles.at(x, y0, z));
                }
            }
        }
        int apex = TURRET_TOP + 2 + 2 * 2 + 1;
        c.set(TX, apex, TZ, "minecraft:polished_deepslate");
        c.set(TX, apex + 1, TZ, St.fence(DARK_FENCE));
        c.set(TX, apex + 2, TZ, St.rod("minecraft:lightning_rod", Dir.UP));
        Lighting.hanging(c, TX, TURRET_TOP, TZ, 2, true);
        c.set(TX + 1, F2 + 1, TZ + 1, St.stairs("minecraft:dark_oak_stairs", Dir.NORTH, false));
        c.set(TX - 1, F2 + 1, TZ + 1, "minecraft:spruce_planks");
        c.set(TX - 1, F2 + 2, TZ + 1, St.candle("black", 3, true));
    }

    /** The attic: boards over the joists, trunks and sheeted junk, dress forms, cobwebs; the false wall when it hides bones. */
    private static void attic(Canvas c, int s, List<Hiding> hide) {
        int y = F2 + 1;
        // Rails round the stair's well.
        for (int z = -3; z <= 0; z++) c.setIfAir(1, y, z, St.fence(DARK_FENCE));
        c.setIfAir(2, y, 1, St.fence(DARK_FENCE));
        LayoutPoint chest = CHESTS.get(3);
        c.set(chest.x(), chest.y(), chest.z(), St.of("chest", "facing", "south", "type", "single", "waterlogged", "false"));
        c.decor(Decor.loot(chest.x(), chest.y(), chest.z(), loot(3)));
        c.set(6, y, -4, St.barrel(Dir.UP, false));
        c.set(6, y + 1, -4, St.barrel(Dir.UP, false));
        c.set(7, y, -3, "minecraft:white_wool");
        c.set(7, y + 1, -3, SHEET);
        c.set(-5, y, -4, "minecraft:white_wool");
        c.set(-5, y + 1, -4, "minecraft:white_wool");
        c.set(-5, y + 2, -4, SHEET);
        c.set(-4, y, -4, St.stairs("minecraft:smooth_quartz_stairs", Dir.SOUTH, false));
        c.set(-2, y, 0, St.slabTop("minecraft:spruce_slab"));
        c.set(-1, y, 0, St.slabTop("minecraft:spruce_slab"));
        c.set(-2, y + 1, 0, SHEET);
        c.set(-1, y + 1, 0, St.candle("white", 2, true));
        c.set(4, y, 0, "minecraft:spruce_planks");
        c.set(4, y + 1, 0, St.potted("dead_bush"));
        c.set(-6, y, 0, St.stairs("minecraft:dark_oak_stairs", Dir.SOUTH, false));
        c.decor(Decor.armorStand(-3, y, -1, Dir.SOUTH, "", "minecraft:leather_chestplate", "", "", ""));
        c.decor(Decor.armorStand(5, y, -1, Dir.WEST, "", "minecraft:leather_chestplate", "minecraft:leather_leggings", "", ""));
        for (int x : new int[]{-6, -1, 4, 8}) {
            int ceil = ceiling(c, x, -2, y);
            if (ceil > y + 3) Lighting.hanging(c, x, ceil, -2, 2, x == -1);
        }
        for (int x = -9; x <= 9; x++) {
            for (int z = -7; z <= 3; z++) {
                int ceil = ceiling(c, x, z, y);
                if (ceil < 0 || !c.isAir(x, ceil - 1, z)) continue;
                if (Noise.chance(x, 1, z, s + 40, 0.12)) c.set(x, ceil - 1, z, "minecraft:cobweb");
            }
        }
        if (hide.contains(Hiding.ATTIC)) {
            // A wall of newer boards across the gable end; behind it a slot of a room, a candle burning by the bones.
            for (int z = -4; z <= 0; z++) {
                int x = z == -4 || z == 0 ? -9 : -8;
                int ceil = ceiling(c, x, z, y);
                for (int yy = y; yy < ceil; yy++) c.set(x, yy, z, "minecraft:spruce_planks");
            }
            for (int z = -3; z <= -1; z++) for (int yy = y; yy < ceiling(c, -9, z, y); yy++) c.carve(-9, yy, z);
            c.set(-9, y, -3, St.candle("white", 2, true));
            c.set(-9, y, -1, St.head("minecraft:skeleton_skull", 4));
            c.set(-9, y + 2, -1, "minecraft:cobweb");
        }
    }

    /** The first non-air cell over (x, z) from {@code y} up (the underside of the roof), or -1. */
    static int ceiling(Canvas c, int x, int z, int y) {
        for (int yy = y; yy <= HEIGHT; yy++) if (!c.isAir(x, yy, z)) return yy;
        return -1;
    }

    /** The porch: boards on the ground, railings and posts, a flat slate roof with a fascia, lamps; round the turret's foot. */
    private static void porch(Canvas c, int s, int ruin) {
        Brush boards = Palette.boards(true, Brush.of("minecraft:spruce_planks"), Brush.of("minecraft:dark_oak_planks"), 3);
        for (int x = -2; x <= 16; x++) {
            for (int z = -8; z <= 10; z++) {
                if (!deck(x, z)) continue;
                c.set(x, -1, z, "minecraft:cobbled_deepslate");
                c.set(x, F0, z, boards.at(x, F0, z));
                for (int y = 1; y <= 4; y++) c.carve(x, y, z);
                boolean edge = edge(x, z);
                c.set(x, 5, z, edge ? "minecraft:dark_oak_planks" : St.slabTop("minecraft:dark_oak_slab"));
                c.set(x, 6, z, Family.DEEPSLATE_TILES.slabBottom());
                if (!edge) continue;
                boolean gap = z == 7 && x >= 0 && x <= 1 || z == -6 && x >= 12 && x <= 13;
                int outs = 0;
                for (Dir d : Dir.HORIZONTAL) if (outside(x + d.dx, z + d.dz)) outs++;
                boolean post = !gap && (outs >= 2 || Math.floorMod(x + z, 3) == 0);
                if (post) for (int y = 1; y <= 4; y++) c.set(x, y, z, St.fence(DARK_FENCE));
                else if (!gap) c.set(x, 1, z, St.fence(DARK_FENCE));
                // Brackets under the beam either side of a post.
                if (post) {
                    for (Dir d : Dir.HORIZONTAL) {
                        int bx = x + d.dx, bz = z + d.dz;
                        if (deck(bx, bz) && edge(bx, bz)) c.setIfAir(bx, 4, bz, St.stairs("minecraft:dark_oak_stairs", d.opposite(), true));
                    }
                }
            }
        }
        // The eaves: a slate overhang with a board along its face, all round the outer edge.
        for (int x = -2; x <= 17; x++) {
            for (int z = -8; z <= 11; z++) {
                if (deck(x, z) || house(x, z) || turret(x, z)) continue;
                Dir toward = null;
                for (Dir d : Dir.HORIZONTAL) if (deck(x + d.dx, z + d.dz)) toward = d;
                boolean diag = false;
                for (int dx = -1; dx <= 1; dx += 2) for (int dz = -1; dz <= 1; dz += 2) diag |= deck(x + dx, z + dz);
                if (toward == null && !diag) continue;
                c.setIfAir(x, 6, z, Family.DEEPSLATE_TILES.slabBottom());
                if (toward != null) c.setIfAir(x, 5, z, St.trapdoorAgainst("minecraft:dark_oak_trapdoor", toward));
            }
        }
        // Steps up at the front and at the east end.
        for (int x = 0; x <= 1; x++) c.set(x, F0, 8, St.stairs("minecraft:stone_brick_stairs", Dir.NORTH, false));
        for (int x = 12; x <= 13; x++) c.set(x, F0, -7, St.stairs("minecraft:stone_brick_stairs", Dir.SOUTH, false));
        for (int[] l : new int[][]{{12, 7}, {12, -3}, {4, 6}}) {
            c.set(l[0], 5, l[1], "minecraft:dark_oak_planks");
            Lighting.hanging(c, l[0], 5, l[1], 1, false);
        }
        Furniture.chair(c, 12, 1, -1, Dir.WEST, "minecraft:dark_oak_stairs", null);
        c.set(5, 1, 6, "minecraft:carved_pumpkin[facing=south]");
        c.set(-1, 4, 5, "minecraft:cobweb");
        c.set(11, 4, -5, "minecraft:cobweb");
        // A wreck of a house: the porch sags at the turret's far side, a post gone, the boards and slates down on the deck.
        if (ruin >= 2) {
            for (int x = 13; x <= 15; x++) {
                for (int z = 6; z <= 8; z++) {
                    if (!deck(x, z) && !outside(x, z)) continue;
                    c.carve(x, 6, z);
                    if (deck(x, z)) {
                        for (int y = 1; y <= 5; y++) c.carve(x, y, z);
                        if (Noise.chance(x, 0, z, s + 41, 0.6)) c.set(x, 1, z, Family.DEEPSLATE_TILES.slabBottom());
                    } else {
                        c.carve(x, 5, z);
                    }
                }
            }
        }
    }

    /** A cell beside the porch that is neither deck nor house. */
    private static boolean outside(int x, int z) {
        return !deck(x, z) && !house(x, z) && !turret(x, z);
    }

    private static boolean edge(int x, int z) {
        for (Dir d : Dir.HORIZONTAL) if (outside(x + d.dx, z + d.dz)) return true;
        return false;
    }

    /** The cellar under the kitchen: rubble-stone walls, wine racks, coal, chains; the bookshelves (and what they hide). */
    private static void cellar(Canvas c, int s, List<Hiding> hide) {
        Brush shell = Palette.patches(s + 50, 2, "minecraft:cobblestone", 4, "minecraft:mossy_cobblestone", 2, "minecraft:stone_bricks", 2,
                "minecraft:cobbled_deepslate", 2, "minecraft:tuff", 1);
        Brush floor = Palette.patches(s + 51, 2, "minecraft:stone_bricks", 3, "minecraft:cracked_stone_bricks", 2, "minecraft:gravel", 1,
                "minecraft:cobblestone", 2);
        for (int x = 1; x <= 10; x++) {
            for (int z = -8; z <= -1; z++) {
                for (int y = -5; y <= -1; y++) {
                    if (CELLAR.contains(x, y, z)) c.carve(x, y, z);
                    else c.set(x, y, z, y == -5 ? floor.at(x, y, z) : shell.at(x, y, z));
                }
            }
        }
        int y = -4;
        Furniture.bookshelves(c, 4, -6, Dir.SOUTH, 4, y, y + 2, Dir.EAST, false, null, 0);
        if (hide.contains(Hiding.CELLAR)) {
            for (int x = 2; x <= 3; x++) for (int z = -5; z <= -4; z++) for (int yy = y; yy <= y + 2; yy++) c.carve(x, yy, z);
            c.set(2, y, -4, St.candle("white", 3, true));
            c.set(2, y, -5, St.head("minecraft:skeleton_skull", 12));
            c.set(2, y + 2, -4, "minecraft:cobweb");
        }
        // Wine racks and casks down the north wall, coal by the stair, chains from the joists.
        for (int x = 5; x <= 8; x++) {
            if (x == 6) continue;
            c.set(x, y, -7, St.barrel(Dir.SOUTH, false));
            c.set(x, y + 1, -7, St.barrel(Dir.SOUTH, false));
        }
        LayoutPoint chest = CHESTS.get(0);
        c.set(chest.x(), chest.y(), chest.z(), St.of("chest", "facing", "south", "type", "single", "waterlogged", "false"));
        c.decor(Decor.loot(chest.x(), chest.y(), chest.z(), loot(0)));
        c.set(9, y, -7, "minecraft:coal_block");
        // The old boiler, its flue chained up to the joists.
        c.set(6, y, -2, "minecraft:blast_furnace[facing=north,lit=false]");
        c.set(6, y + 1, -2, "minecraft:polished_deepslate");
        for (int yy = y + 2; yy <= -1; yy++) c.set(6, yy, -2, St.chain("y"));
        Lighting.hanging(c, 6, F0, -4, 1, true);
        Lighting.hanging(c, 8, F0, -6, 2, true);
        c.set(7, -1, -4, St.chain("y"));
        for (int[] w : new int[][]{{5, -1, -7}, {9, -1, -2}, {5, -1, -3}}) c.setIfAir(w[0], w[1], w[2], "minecraft:cobweb");
    }

    // --- the garden ---------------------------------------------------------------------------------------------------------

    private static void garden(Canvas c, Ground g, int s, int ruin, List<Hiding> hide, long seed) {
        Paths.Style style = Paths.countryPath(3, s + 80);
        Paths.path(c, g, List.of(new int[]{1, 9}, new int[]{1, 12}, new int[]{0, 15}, new int[]{1, 21}), style, s + 81);
        Paths.Style narrow = Paths.countryPath(2, s + 82);
        Paths.path(c, g, List.of(new int[]{0, -9}, new int[]{-4, -11}, new int[]{-9, -12}, new int[]{-12, -12}), narrow, s + 83);
        Paths.path(c, g, List.of(new int[]{0, -9}, new int[]{6, -11}, new int[]{11, -12}), narrow, s + 84);
        Paths.path(c, g, List.of(new int[]{12, -8}, new int[]{12, -11}), narrow, s + 85);
        fence(c, g, s, ruin);
        graveyard(c, g, s, hide.contains(Hiding.GRAVEYARD) ? grave(seed) : -1);
        well(c, s, hide.contains(Hiding.WELL));
        // Dead trees and dark spruces; the hedges along the front fence grown wild.
        if (g.has(-13, 12)) snag(c, -13, 12, s + 86);
        if (g.has(17, -5)) snag(c, 17, -5, s + 87);
        for (int[] t : new int[][]{{-16, 2}, {17, -19}, {18, 11}, {-5, -19}}) if (g.has(t[0], t[1])) Trees.spruce(c, t[0], 0, t[1], 1, s + 88 + t[0]);
        List<int[]> hedge = new ArrayList<>();
        for (int x = -17; x <= 19; x++) {
            if (x >= -2 && x <= 4) continue;
            if (g.has(x, 14) && Noise.chance(x, 0, 14, s + 89, 0.8)) hedge.add(new int[]{x, 14});
        }
        Trees.hedge(c, hedge, 0, 1, "minecraft:dark_oak_leaves", s + 90);
        Trees.bush(c, -15, 0, 9, "minecraft:azalea_leaves", 1.6, s + 91);
        Trees.bush(c, 16, 0, 13, "minecraft:dark_oak_leaves", 1.4, s + 92);
        // A lamp post at the gate, its lantern long out (a soul flame left in it).
        Lighting.lampPost(c, 3, 0, 16, 3, "minecraft:cobblestone", DARK_FENCE, Dir.WEST, true);
        Lighting.lampPost(c, -14, 0, 10, 3, "minecraft:mossy_cobblestone", DARK_FENCE, null, false);
        Scatter.flora(c, g, null, 0.45, 1, s + 93, "minecraft:short_grass", 30, "minecraft:tall_grass", 12, "minecraft:fern", 8,
                "minecraft:large_fern", 4, "minecraft:dead_bush", 4, "minecraft:rose_bush", 1, "minecraft:wither_rose", 1,
                "minecraft:lily_of_the_valley", 1, "minecraft:brown_mushroom", 1);
        Scatter.prune(c);
    }

    /** A thin dead tree: a trunk leaning as it rises, a crooked limb low on the other side, a twig at the top. */
    private static void snag(Canvas c, int x, int z, int s) {
        Wood w = Wood.DARK_OAK;
        int lean = Noise.chance(x, 0, z, s, 0.5) ? 1 : -1;
        boolean alongX = Noise.chance(x, 1, z, s, 0.5);
        int lx = alongX ? lean : 0, lz = alongX ? 0 : lean;
        for (int y = 0; y <= 2; y++) c.set(x, y, z, w.log("y"));
        Trees.line(c, x, 2, z, x + lx, 6, z + lz, w);
        Trees.line(c, x, 1, z, x - lx * 2 + lz, 4, z - lz * 2 + lx, w);
        Trees.line(c, x + lx, 6, z + lz, x + lx * 2 - lz, 7, z + lz * 2 - lx, w);
        c.set(x, -1, z, "minecraft:rooted_dirt");
    }

    /** The wrought-iron fence round the garden: bars on a low kerb, stone piers, the gate long gone; gaps in a ruin. */
    private static void fence(Canvas c, Ground g, int s, int ruin) {
        Box b = GARDEN;
        List<int[]> line = new ArrayList<>();
        for (int x = b.x0(); x <= b.x1(); x++) {
            line.add(new int[]{x, b.z0()});
            line.add(new int[]{x, b.z1()});
        }
        for (int z = b.z0() + 1; z < b.z1(); z++) {
            line.add(new int[]{b.x0(), z});
            line.add(new int[]{b.x1(), z});
        }
        for (int[] p : line) {
            int x = p[0], z = p[1];
            if (!g.has(x, z)) continue;
            if (z == b.z1() && x >= 0 && x <= 2) continue;
            boolean pier = Math.floorMod(x + z, 6) == 0 || x == -1 && z == b.z1() || x == 3 && z == b.z1();
            if (pier) {
                c.set(x, 0, z, "minecraft:mossy_stone_bricks");
                c.set(x, 1, z, "minecraft:stone_bricks");
                c.set(x, 2, z, Family.STONE_BRICKS.slabBottom());
                continue;
            }
            if (Noise.chance(x, 0, z, s + 94, 0.04 + 0.08 * ruin)) continue;
            c.set(x, 0, z, St.bars());
            if (!Noise.chance(x, 1, z, s + 95, 0.05 + 0.1 * ruin)) c.set(x, 1, z, St.bars());
        }
        c.set(-1, 3, b.z1(), St.head("minecraft:skeleton_skull", 8));
    }

    /** The family plot behind the house: three graves in a ring of iron, an obelisk; one of them, maybe, not at rest. */
    private static void graveyard(Canvas c, Ground g, int s, int restless) {
        Box b = GRAVEYARD;
        for (int x = b.x0(); x <= b.x1(); x++) {
            for (int z = b.z0(); z <= b.z1(); z++) {
                boolean rim = x == b.x0() || x == b.x1() || z == b.z0() || z == b.z1();
                if (!rim) {
                    c.set(x, -1, z, Palette.mix(s + 100, "minecraft:podzol", 3, "minecraft:coarse_dirt", 2, "minecraft:grass_block", 2)
                            .at(x, -1, z));
                    continue;
                }
                if (z == b.z1() && (x == -12 || x == -11)) continue;
                if (x == b.x0() && z == b.z0() || x == b.x1() && z == b.z0() || x == b.x0() && z == b.z1() || x == b.x1() && z == b.z1()
                        || z == b.z1() && (x == -13 || x == -10)) {
                    c.set(x, 0, z, "minecraft:mossy_stone_bricks");
                    c.set(x, 1, z, St.wall("minecraft:mossy_stone_brick_wall"));
                } else {
                    c.set(x, 0, z, St.bars());
                }
            }
        }
        String[] stones = {"minecraft:mossy_stone_brick_wall", "minecraft:cobblestone_wall", "minecraft:mossy_cobblestone_wall"};
        for (int i = 0; i < GRAVES.length; i++) {
            int x = GRAVES[i];
            c.set(x, 0, GRAVE_Z - 1, St.wall(stones[i]));
            if (i == 1) {
                c.set(x, 0, GRAVE_Z - 1, "minecraft:chiseled_stone_bricks");
                c.set(x, 1, GRAVE_Z - 1, St.wall("minecraft:stone_brick_wall"));
            }
            for (int k = 0; k <= 1; k++) {
                int z = GRAVE_Z + k;
                c.set(x, -1, z, Noise.chance(x, k, z, s + 101, 0.5) ? "minecraft:coarse_dirt" : "minecraft:podzol");
                c.set(x, -2, z, "minecraft:dirt");
                c.set(x, -3, z, "minecraft:dirt");
                c.set(x, -4, z, "minecraft:stone");
            }
            if (i == restless) {
                c.carve(x, -3, GRAVE_Z);
                // The grass will not grow over this one.
                c.set(x, -1, GRAVE_Z, "minecraft:coarse_dirt");
                c.set(x, -1, GRAVE_Z + 1, "minecraft:coarse_dirt");
                c.set(x, 0, GRAVE_Z + 1, St.candle("white", 1, false));
            }
        }
        // An obelisk in the corner, a wilted wreath on the middle grave.
        c.set(-15, 0, -14, "minecraft:stone_bricks");
        c.set(-15, 1, -14, "minecraft:chiseled_stone_bricks");
        c.set(-15, 2, -14, St.wall("minecraft:stone_brick_wall"));
        c.set(-15, 3, -14, St.wall("minecraft:stone_brick_wall"));
        c.set(-12, 0, GRAVE_Z, St.potted("dead_bush"));
    }

    /** The well: a mossy stone ring, a beam on two posts under a little slate roof, a ladder down a dry shaft to the mud. */
    private static void well(Canvas c, int s, boolean bones) {
        Box b = WELL;
        Brush ring = Palette.mix(s + 110, "minecraft:mossy_cobblestone", 3, "minecraft:cobblestone", 2, "minecraft:mossy_stone_bricks", 2);
        for (int x = b.x0(); x <= b.x1(); x++) {
            for (int z = b.z0(); z <= b.z1(); z++) {
                boolean inner = x > b.x0() && x < b.x1() && z > b.z0() && z < b.z1();
                for (int y = -8; y <= 0; y++) {
                    if (!inner) c.set(x, y, z, ring.at(x, y, z));
                    else if (y == -8) c.set(x, y, z, "minecraft:stone");
                    else if (y == -7) c.set(x, y, z, "minecraft:packed_mud");
                    else if (y == -6) c.set(x, y, z, "minecraft:mud");
                    else c.carve(x, y, z);
                }
                if (inner) for (int y = 1; y <= 4; y++) c.carve(x, y, z);
            }
        }
        if (bones) {
            LayoutPoint p = drawn(Hiding.WELL, 0).bones();
            c.carve(p.x(), p.y(), p.z());
        }
        int lx = b.x0() + 1, lz = b.z0() + 1;
        for (int y = -5; y <= 0; y++) c.set(lx, y, lz, St.ladder(Dir.SOUTH));
        // Posts on the rim, the beam across, a little gabled roof.
        for (int y = 1; y <= 3; y++) {
            c.set(b.x0(), y, lz, St.fence(DARK_FENCE));
            c.set(b.x1(), y, lz, St.fence(DARK_FENCE));
        }
        for (int x = b.x0(); x <= b.x1(); x++) c.set(x, 4, lz, St.axis("minecraft:stripped_dark_oak_log", "x"));
        for (int x = b.x0() - 1; x <= b.x1() + 1; x++) {
            c.set(x, 4, b.z0(), Family.DEEPSLATE_TILES.stairs(Dir.SOUTH, false));
            c.set(x, 5, lz, Family.DEEPSLATE_TILES.slabBottom());
            c.set(x, 5, lz + 1, Family.DEEPSLATE_TILES.slabBottom());
            c.set(x, 4, b.z1(), Family.DEEPSLATE_TILES.stairs(Dir.NORTH, false));
        }
        c.set(b.x0() - 1, 4, lz, Wood.DARK_OAK.stairs(Dir.EAST, true));
        c.set(b.x1() + 1, 4, lz, Wood.DARK_OAK.stairs(Dir.WEST, true));
        // The bucket's rope: a chain down the shaft to a soul lantern.
        Lighting.hanging(c, lx + 1, 5, lz + 1, 8, true);
    }

    /** Moss, ivy and cobwebs; more the further gone the house is. */
    private static void weather(Canvas c, int s, int ruin) {
        double ivy = 0.12 + 0.1 * ruin;
        for (int x = -12; x <= 14; x++) {
            for (int z = -10; z <= 11; z++) {
                for (int y = 0; y <= WALL_TOP; y++) {
                    if (!c.isAir(x, y, z) || deck(x, z)) continue;
                    for (Dir d : Dir.HORIZONTAL) {
                        int wx = x + d.dx, wz = z + d.dz;
                        String w = c.get(wx, y, wz);
                        if (w == null || !Kinds.fullCube(w) || !(house(wx, wz) || turret(wx, wz)) || house(x, z) || turret(x, z)) continue;
                        double grow = ivy * (1.2 - y / (double) (WALL_TOP + 2));
                        if (Noise.patch3(x, y, z, 3, s + 120) < grow * 2.2 && Noise.chance(x, y, z, s + 121, 0.7)) {
                            c.set(x, y, z, St.vine(d));
                            break;
                        }
                    }
                }
            }
        }
        // Cobwebs in the corners of the rooms, moss creeping over the boards.
        for (int x = -9; x <= 12; x++) {
            for (int z = -7; z <= 8; z++) {
                for (int yTop : new int[]{F1 - 1, F2 - 1}) {
                    if (!c.isAir(x, yTop, z) || c.isAir(x, yTop + 1, z) || overStep(c, x, yTop, z)) continue;
                    int walls = 0;
                    for (Dir d : Dir.HORIZONTAL) if (!c.isAir(x + d.dx, yTop, z + d.dz)) walls++;
                    if (walls >= 2 && Noise.chance(x, yTop, z, s + 122, 0.35 + 0.15 * ruin)) c.set(x, yTop, z, "minecraft:cobweb");
                }
                for (int yFloor : new int[]{F0 + 1, F1 + 1}) {
                    if (c.isAir(x, yFloor, z) && Kinds.fullCube(c.get(x, yFloor - 1, z)) && houseInside(x, z)
                            && Noise.chance(x, yFloor, z, s + 123, 0.03 + 0.04 * ruin)) c.set(x, yFloor, z, St.mossCarpet());
                }
            }
        }
    }

    /** Whether a stair lies within four cells under (x, y, z) (its headroom stays clear). */
    private static boolean overStep(Canvas c, int x, int y, int z) {
        for (int k = 1; k <= 4; k++) if (Kinds.stairs(c.world(x, y - k, z))) return true;
        return false;
    }

    /** What must stay free whatever was strewn: the study's chest spot and the way to it, the bones' access cells. */
    private static void keepClear(Canvas c, long seed, List<Hiding> hide) {
        for (LayoutPoint p : List.of(STUDY_CHEST, new LayoutPoint(STUDY_CHEST.x(), STUDY_CHEST.y(), STUDY_CHEST.z() + 1))) {
            c.carve(p.x(), p.y(), p.z());
            c.carve(p.x(), p.y() + 1, p.z());
        }
        for (Hiding h : hide) {
            LayoutPoint a = drawn(h, seed).access();
            c.carve(a.x(), a.y(), a.z());
            c.carve(a.x(), a.y() + 1, a.z());
        }
    }

    // --- mirroring ---------------------------------------------------------------------------------------------------------

    /** The canvas mirrored east–west (x to -x): facings, sides, hinges and rotations follow; shapes are redone on finish. */
    static Canvas mirror(Canvas src) {
        Canvas out = new Canvas(src.name() + "_mirrored");
        for (Map.Entry<Long, String> e : src.map().entrySet()) {
            long k = e.getKey();
            out.set(-Canvas.kx(k), Canvas.ky(k), Canvas.kz(k), mirrorState(e.getValue()));
        }
        for (Decor d : src.decor()) {
            out.decor(new Decor(d.kind(), -d.x(), d.y(), d.z(), flip(d.facing()), d.data(), d.extra()));
        }
        for (Map.Entry<String, int[]> a : src.anchors().entrySet()) out.anchor(a.getKey(), -a.getValue()[0], a.getValue()[1], a.getValue()[2]);
        return out;
    }

    static String flip(String dir) {
        return switch (dir) {
            case "east" -> "west";
            case "west" -> "east";
            default -> dir;
        };
    }

    static String mirrorState(String state) {
        if (state.indexOf('[') < 0) return state;
        Map<String, String> p = St.props(state);
        java.util.TreeMap<String, String> out = new java.util.TreeMap<>(p);
        String f = p.get("facing");
        if (f != null) out.put("facing", flip(f));
        String hinge = p.get("hinge");
        if (hinge != null) out.put("hinge", hinge.equals("left") ? "right" : "left");
        String type = p.get("type");
        if (type != null && (type.equals("left") || type.equals("right")) && St.path(state).contains("chest")) {
            out.put("type", type.equals("left") ? "right" : "left");
        }
        String r = p.get("rotation");
        if (r != null) out.put("rotation", String.valueOf((16 - Integer.parseInt(r)) & 15));
        if (p.containsKey("east") || p.containsKey("west")) {
            String e = p.get("east"), w = p.get("west");
            if (w != null) out.put("east", w);
            else out.remove("east");
            if (e != null) out.put("west", e);
            else out.remove("west");
        }
        return St.build(St.id(state), out);
    }
}
