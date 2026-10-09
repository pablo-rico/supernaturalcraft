package org.papiricoh.supernaturalcraft.legacy.bunker;

import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Core;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Decor;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.EntranceAndWarRoom;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Level1;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Level2;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Level3;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Plan;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.PlanShapes;
import org.papiricoh.supernaturalcraft.legacy.bunker.plan.Zones;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Men of Letters' bunker as a plan (pure, tested in JUnit): every block as a block-state string in local coordinates, the
 * {@link Decor} (frames, paintings, banners, loot…) and the hill over the way in. Local +Z is where the door looks (south before
 * rotation), Y = 0 the ground's top block, X = 0 the door's line. Drawn by the parts in {@code plan/} (see {@link Zones} for
 * the floor plan and {@code plan/Kit} for the style), then finished by {@link PlanShapes} (stair corners, joined bars and walls).
 *
 * <p>Stored packed: positions sorted by column (x, then z, then y) with an index into a palette, so the builder can walk just
 * the columns of the chunk it is building.
 */
public final class BunkerLayout {

    /** The plan's extent, local (the hill's trees are cleared up to {@link #CLEAR_TO} above it). */
    public static final int MIN_X = Zones.MIN_X, MAX_X = Zones.MAX_X, MIN_Z = Zones.MIN_Z, MAX_Z = Zones.MAX_Z, MIN_Y = Zones.MIN_Y,
            MAX_Y = Zones.MAX_Y, CLEAR_TO = MAX_Y + 10;
    /** Level floors (the floor block's y). */
    public static final int FLOOR1 = Zones.LEVEL1, FLOOR2 = Zones.LEVEL2, FLOOR3 = Zones.LEVEL3;
    /** The hill's height over the ground at its top. */
    public static final int HILL = 9;

    /** The door's lower half; the spot outside it; the map table Henry points at; where he stands; the dungeon's devil's trap. */
    public static final int[] DOOR = EntranceAndWarRoom.DOOR, OUTSIDE = EntranceAndWarRoom.OUTSIDE, MAP_TABLE = EntranceAndWarRoom.MAP_TABLE,
            HENRY = EntranceAndWarRoom.HENRY, TRAP = Level3.TRAP, WAR_ROOM = EntranceAndWarRoom.WAR_ROOM, EMBLEM_AT = EntranceAndWarRoom.EMBLEM;
    /** The four research desks. */
    public static final int[][] DESKS = Level1.DESKS;
    /** The map table's blocks (3×2). */
    public static final int[][] MAP_TABLES = EntranceAndWarRoom.MAP_TABLES;

    /** One block of the plan. */
    public record Cell(int x, int y, int z, String state) {
    }

    static final String AIR = "minecraft:air", DOOR_ID = "supernaturalcraft:bunker_door", DESK = "supernaturalcraft:research_desk",
            TABLE = "supernaturalcraft:map_table", SHELF = "supernaturalcraft:archive_shelf", EMBLEM = "supernaturalcraft:men_of_letters_emblem";

    private static final long[] KEYS;
    private static final short[] STATES;
    private static final String[] PALETTE;
    private static final List<Decor> DECOR;
    private static final Map<String, Zones.View> VIEWS;
    private static List<Cell> cells;

    static {
        Plan p = new Plan();
        EntranceAndWarRoom.build(p);
        Core.build(p);
        Level1.build(p);
        Level2.build(p);
        Level3.build(p);
        PlanShapes.apply(p.cells());
        long[] keys = p.cells().keySet().stream().mapToLong(Long::longValue).sorted().toArray();
        Map<String, Short> index = new HashMap<>();
        List<String> palette = new ArrayList<>();
        short[] states = new short[keys.length];
        for (int i = 0; i < keys.length; i++) {
            String s = p.cells().get(keys[i]);
            Short k = index.get(s);
            if (k == null) {
                k = (short) palette.size();
                index.put(s, k);
                palette.add(s);
            }
            states[i] = k;
        }
        KEYS = keys;
        STATES = states;
        PALETTE = palette.toArray(String[]::new);
        DECOR = List.copyOf(p.decor());
        VIEWS = Collections.unmodifiableMap(new java.util.LinkedHashMap<>(p.views()));
    }

    private BunkerLayout() {
    }

    /** Every block of the plan (built once, on first ask: the builder walks the packed form). */
    public static synchronized List<Cell> cells() {
        if (cells == null) {
            List<Cell> out = new ArrayList<>(KEYS.length);
            for (int i = 0; i < KEYS.length; i++) out.add(new Cell(Plan.kx(KEYS[i]), Plan.ky(KEYS[i]), Plan.kz(KEYS[i]), PALETTE[STATES[i]]));
            cells = Collections.unmodifiableList(out);
        }
        return cells;
    }

    public static int size() {
        return KEYS.length;
    }

    /** The distinct block states the plan uses. */
    public static Set<String> palette() {
        return new LinkedHashSet<>(Arrays.asList(PALETTE));
    }

    public static List<Decor> decor() {
        return DECOR;
    }

    /** Each room's spot (one stands there; the preview looks from it). */
    public static Map<String, Zones.View> views() {
        return VIEWS;
    }

    public static String blockId(String state) {
        int b = state.indexOf('[');
        return b < 0 ? state : state.substring(0, b);
    }

    /** The state of the plan at a local point, or null if the plan leaves it alone. */
    public static String at(int x, int y, int z) {
        int i = Arrays.binarySearch(KEYS, Plan.key(x, y, z));
        return i < 0 ? null : PALETTE[STATES[i]];
    }

    /** What the plan has in a local column: calls {@code out} with (y, state) from the bottom up. */
    public static void column(int x, int z, ColumnSink out) {
        long lo = Plan.key(x, -1024, z), hi = Plan.key(x, 1023, z);
        int i = Arrays.binarySearch(KEYS, lo);
        if (i < 0) i = -i - 1;
        for (; i < KEYS.length && KEYS[i] <= hi; i++) out.accept(Plan.ky(KEYS[i]), PALETTE[STATES[i]]);
    }

    /** The top of the plan in a local column, or {@link Integer#MIN_VALUE} if it has nothing there. */
    public static int columnTop(int x, int z) {
        long hi = Plan.key(x, 1023, z);
        int i = Arrays.binarySearch(KEYS, hi);
        if (i < 0) i = -i - 1;
        i--;
        if (i < 0 || Plan.kx(KEYS[i]) != x || Plan.kz(KEYS[i]) != z) return Integer.MIN_VALUE;
        return Plan.ky(KEYS[i]);
    }

    @FunctionalInterface
    public interface ColumnSink {
        void accept(int y, String state);
    }

    // --- the hill over the way in --------------------------------------------------------------------------------------------

    /**
     * How high the hill rises over the ground at a local column (0 for none): a rounded mound over the vestibule and the head of
     * the stair tunnel, flat-topped, dropping away round the sides; open in front of the façade.
     */
    public static int hill(int x, int z) {
        if (z >= 7 && Math.abs(x) <= 10) return 0;
        double dx = x / 15.0, dz = (z + 1) / 12.0;
        double d = Math.sqrt(dx * dx + dz * dz);
        double t = Math.max(0, Math.min(1, (1 - d) / 0.55));
        double h = HILL * t * t * (3 - 2 * t) + (Plan.noise(x, 0, z, 31) - 0.5) * 1.4 * t;
        return (int) Math.max(0, Math.round(h));
    }

    // --- rotation (as the Author's cabin: vanilla Rotation order NONE, CW_90, CW_180, CCW_90) ---------------------------------

    public static int[] rotate(int x, int z, int quarter) {
        return switch (Math.floorMod(quarter, 4)) {
            case 1 -> new int[]{-z, x};
            case 2 -> new int[]{-x, -z};
            case 3 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    /** A plan direction ({@code north}…) after the turn. */
    public static String rotate(String dir, int quarter) {
        if (dir.equals("up") || dir.equals("down") || dir.isEmpty()) return dir;
        String[] order = {"north", "east", "south", "west"};
        int i = Arrays.asList(order).indexOf(dir);
        return order[Math.floorMod(i + quarter, 4)];
    }

    public static int[] toWorld(int[] local, int ox, int oy, int oz, int quarter) {
        int[] r = rotate(local[0], local[2], quarter);
        return new int[]{ox + r[0], oy + local[1], oz + r[1]};
    }

    /** The plan's world box {minX, minY, minZ, maxX, maxY, maxZ}. */
    public static int[] box(int ox, int oy, int oz, int quarter) {
        int[] a = rotate(MIN_X, MIN_Z, quarter), b = rotate(MAX_X, MAX_Z, quarter);
        return new int[]{ox + Math.min(a[0], b[0]), oy + MIN_Y, oz + Math.min(a[1], b[1]),
                ox + Math.max(a[0], b[0]), oy + CLEAR_TO, oz + Math.max(a[1], b[1])};
    }
}
