package org.papiricoh.supernaturalcraft.entity.boss.zachariah.arena;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Zachariah's office (v0.18, pure): Heaven's endless bureaucracy, high above a hunter's Heaven at
 * {@code HeavenPlotLayout.OFFICE_ORIGIN}. Built from a tile of {@link #TILE} blocks that repeats in X and Z, so that within
 * {@link #WRAP_WINDOW} of the origin {@code cell(x, y, z) == cell(x + TILE, y, z) == cell(x, y, z + TILE)}: crossing the window
 * teleports a hunter one {@link #WRAP_SHIFT} back without a visible seam ({@code OfficeWrap}).
 * <p><b>Contract</b> fixed by the foundations: the constants and points below. No block entities anywhere (the office is written
 * through the fight's arena; {@link #shuffle} and {@link #dissolve} are arena mutations).
 * <p>The tile (buildkit, every cell a function of its place in the tile, so the repetition is exact): a stepped quartz dais with
 * a dark oak desk under a skylight in the middle; four filing cabinets round it (each copy carries the number of the cabinet of
 * its class, so the four real ones of {@link #CABINETS} are already right); a cubicle pod of glass-topped partitions at the tile's
 * corner with four desks; green "Approved" desks under a green light at the tile's edge midpoints; grey carpet tiles, aisles of
 * polished andesite, a smooth stone ceiling with sea-lantern troffers. Past {@link #FOG} the office ends in bands of white glass
 * and powder: the fog.
 */
public final class ZachariahOfficeLayout {

    /** The repeating tile's side, and the half-width of the window inside which the office repeats exactly. */
    public static final int TILE = 16, WRAP_WINDOW = 24;
    /** How far the wrap moves whoever crosses the window (a whole number of tiles). */
    public static final int WRAP_SHIFT = 48;
    /** Floor to ceiling. */
    public static final int CEILING = 7;
    /** Zachariah's desk on its dais, under the skylight (he stands here between attacks). */
    public static final LayoutPoint DAIS = new LayoutPoint(0, 1, 0);
    /** Where the lift from Naomi's room arrives. */
    public static final LayoutPoint ENTRY = new LayoutPoint(0, 0, 20);
    /** The four filing cabinets I-IV (index 0 = I). */
    public static final List<LayoutPoint> CABINETS = List.of(new LayoutPoint(-12, 0, -12), new LayoutPoint(12, 0, -12),
            new LayoutPoint(12, 0, 12), new LayoutPoint(-12, 0, 12));
    /** The green "Approved" desks a Termination Notice can be survived at. */
    public static final List<LayoutPoint> DESK_SAFE = List.of(new LayoutPoint(-8, 0, 0), new LayoutPoint(8, 0, 0), new LayoutPoint(0, 0, -8),
            new LayoutPoint(0, 0, 8));
    /** Where clerks come out of the cubicles. */
    public static final List<LayoutPoint> CLERK_SPAWNS = List.of(new LayoutPoint(-16, 0, -4), new LayoutPoint(16, 0, 4),
            new LayoutPoint(-4, 0, 16), new LayoutPoint(4, 0, -16));
    /** Walls where the hunter's memories hang as framed décor. */
    public static final List<LayoutPoint> FRAMES = List.of(new LayoutPoint(-5, 1, -3), new LayoutPoint(5, 1, -3), new LayoutPoint(-5, 1, 3),
            new LayoutPoint(5, 1, 3));
    /** Everything of the office (fog band included) fits in this radius. */
    public static final int RADIUS = 32;

    /** From here out (on either axis) the office is fog. */
    static final int FOG = 29;

    private ZachariahOfficeLayout() {
    }

    /** The permanent office. */
    public static LayoutPlan plan() {
        return canvas().toPlan();
    }

    /** The office as a finished kit canvas (tests and the layout dump). */
    public static Canvas canvas() {
        Canvas c = new Canvas("zachariah_office");
        c.zone("office", new Box(-RADIUS, -2, -RADIUS, RADIUS, CEILING + 1, RADIUS));
        c.inZone("office", () -> {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    for (int y = -2; y <= CEILING + 1; y++) c.set(x, y, z, cell(x, y, z));
                }
            }
        });
        c.anchor("entry", ENTRY.x(), ENTRY.y(), ENTRY.z());
        c.anchor("dais", DAIS.x(), DAIS.y(), DAIS.z());
        for (int i = 0; i < CABINETS.size(); i++) c.anchor("cabinet_" + (i + 1), CABINETS.get(i).x(), 0, CABINETS.get(i).z());
        for (int i = 0; i < DESK_SAFE.size(); i++) c.anchor("desk_safe_" + i, DESK_SAFE.get(i).x(), 0, DESK_SAFE.get(i).z());
        for (int i = 0; i < CLERK_SPAWNS.size(); i++) c.anchor("clerk_" + i, CLERK_SPAWNS.get(i).x(), 0, CLERK_SPAWNS.get(i).z());
        return c.finish();
    }

    /** A coordinate's place in its tile: -8..7, 0 at the tile's centre (the dais). */
    static int t(int v) {
        return Math.floorMod(v + TILE / 2, TILE) - TILE / 2;
    }

    /** Whether (x, z) is in the fog band. */
    static boolean fog(int x, int z) {
        return Math.abs(x) >= FOG || Math.abs(z) >= FOG;
    }

    /** The cabinet number (1-4) a tile position carries, or 0. Tile positions of CABINETS I-IV: (4,4), (-4,4), (-4,-4), (4,-4). */
    static int cabinet(int u, int v) {
        if (Math.abs(u) != 4 || Math.abs(v) != 4) return 0;
        for (int n = 0; n < CABINETS.size(); n++) {
            LayoutPoint p = CABINETS.get(n);
            if (t(p.x()) == u && t(p.z()) == v) return n + 1;
        }
        return 0;
    }

    /** The office at a cell (periodic inside the fog band). */
    static String cell(int x, int y, int z) {
        if (fog(x, z)) return fogCell(x, y, z);
        int u = t(x), v = t(z);
        return tile(u, y, v);
    }

    private static String fogCell(int x, int y, int z) {
        int d = Math.max(Math.abs(x), Math.abs(z));
        if (y == -2) return "minecraft:smooth_stone";
        if (y == -1) return "minecraft:white_concrete";
        if (y >= CEILING) return y == CEILING ? "minecraft:white_concrete" : "minecraft:smooth_stone_slab[type=bottom,waterlogged=false]";
        return d == FOG ? "minecraft:white_stained_glass" : d == FOG + 1 ? "minecraft:white_stained_glass" : "minecraft:white_concrete_powder";
    }

    /** One tile: u, v in -8..7. */
    static String tile(int u, int y, int v) {
        int au = Math.abs(u), av = Math.abs(v);
        boolean dais = au <= 2 && av <= 2;
        boolean aisle = u == -8 || v == -8 || au <= 1 && !dais || av <= 1 && !dais;
        if (y == -2) return "minecraft:smooth_stone";
        if (y == -1) {
            if (dais) return "minecraft:polished_diorite";
            if (aisle) return u == -8 && v == -8 ? "minecraft:smooth_stone" : "minecraft:polished_andesite";
            return ((Math.floorDiv(u, 2) + Math.floorDiv(v, 2)) & 1) == 0 ? "minecraft:light_gray_wool" : "minecraft:gray_wool";
        }
        if (y == CEILING + 1) return "minecraft:smooth_stone_slab[type=bottom,waterlogged=false]";
        if (y == CEILING) {
            if (au <= 1 && av <= 1) return "minecraft:glass";
            if (au <= 2 && av <= 2) return "minecraft:smooth_quartz";
            if (u == -8 && v == 0 || u == 0 && v == -8) return "minecraft:verdant_froglight";
            if (Math.floorMod(u, 4) == 0 && Math.floorMod(v, 4) == 0) return "minecraft:sea_lantern";
            if (u == -8 || v == -8) return "minecraft:smooth_quartz";
            return "minecraft:smooth_stone";
        }
        // y 0..CEILING-1: the room. Invisible fill lights (the light block) hang between the troffers where the air is free.
        if (y == 3 && Math.floorMod(u, 4) == 2 && Math.floorMod(v, 4) == 2 && !dais) return St.light(14);
        if (dais) {
            if (y == 0) return au == 2 || av == 2 ? daisStep(u, v) : "minecraft:smooth_quartz";
            if (y == 1) {
                if (v == 1 && au <= 1) return St.slabTop("minecraft:dark_oak_slab");
                if (u == 0 && v == -1) return St.stairs("minecraft:dark_oak_stairs", Dir.NORTH, false);
            }
            if (y == 2 && u == 1 && v == 1) return St.candle("white", 3, true);
            if (y == 2 && u == -1 && v == 1) return St.carpet("white");
            return St.AIR;
        }
        int n = cabinet(u, v);
        if (n > 0) {
            if (y == 0) return St.of("supernaturalcraft:filing_cabinet", "facing", v < 0 ? "north" : "south", "number", String.valueOf(n));
            return St.AIR;
        }
        // The Approved desks at the tile's edge midpoints: the spot (lime carpet) between two birch desks with a lime candle.
        if (u == -8 && v == 0 || u == 0 && v == -8) {
            return y == 0 ? St.carpet("lime") : St.AIR;
        }
        if (u == -8 && av == 1) {
            if (y == 0) return St.slabTop("minecraft:birch_slab");
            if (y == 1 && v == 1) return St.candle("lime", 2, true);
            return St.AIR;
        }
        if (v == -8 && au == 1) {
            if (y == 0) return St.slabTop("minecraft:birch_slab");
            if (y == 1 && u == 1) return St.candle("lime", 2, true);
            return St.AIR;
        }
        // The cubicle pod round the tile's corner: a post, glass-topped partitions in a plus, four desks with chairs.
        int du = t(u + 8), dv = t(v + 8); // offset from the tile's corner (-8, -8), wrapped
        boolean corner = du == 0 && dv == 0;
        boolean arm = du == 0 && Math.abs(dv) >= 2 && Math.abs(dv) <= 3 || dv == 0 && Math.abs(du) >= 2 && Math.abs(du) <= 3;
        if (corner) return y <= 2 ? St.axis("minecraft:quartz_pillar", "y") : St.AIR;
        if (arm) {
            if (y == 0) return "minecraft:light_gray_concrete";
            if (y == 1) return St.pane("minecraft:light_gray_stained_glass_pane");
            return St.AIR;
        }
        if (Math.abs(du) == 2 && Math.abs(dv) == 2) {
            if (y == 0) return St.slabTop("minecraft:birch_slab");
            if (y == 1) return St.trapdoor("minecraft:iron_trapdoor", du < 0 ? Dir.EAST : Dir.WEST, false, true);
            return St.AIR;
        }
        if (Math.abs(du) == 3 && Math.abs(dv) == 2) {
            return y == 0 ? St.stairs("minecraft:birch_stairs", du < 0 ? Dir.WEST : Dir.EAST, false) : St.AIR;
        }
        return St.AIR;
    }

    private static String daisStep(int u, int v) {
        if (Math.abs(u) == 2 && Math.abs(v) == 2) return "minecraft:smooth_quartz";
        Dir back = Math.abs(u) == 2 ? (u < 0 ? Dir.EAST : Dir.WEST) : (v < 0 ? Dir.SOUTH : Dir.NORTH);
        return Family.SMOOTH_QUARTZ.stairs(back, false);
    }

    /**
     * One step of the cubicle shuffle: the pods' partitions rise to the ceiling's height along one axis (even steps: the arms
     * running north-south, odd: east-west), lengthened to block the aisles between pods. Written by the arena and undone after
     * a while. Cells relative to the office's origin; none on the dais, the cabinets, the Approved spots, the clerks' lanes or
     * the entry.
     */
    public static List<ArenaCell> shuffle(int step) {
        boolean ns = Math.floorMod(step, 2) == 0;
        List<ArenaCell> out = new ArrayList<>();
        for (int x = -FOG + 1; x < FOG; x++) {
            for (int z = -FOG + 1; z < FOG; z++) {
                int u = t(x), v = t(z);
                int du = t(u + 8), dv = t(v + 8);
                boolean wall = ns ? du == 0 && Math.abs(dv) >= 1 && Math.abs(dv) <= 5 : dv == 0 && Math.abs(du) >= 1 && Math.abs(du) <= 5;
                if (!wall) continue;
                for (int y = 0; y <= 2; y++) out.add(new ArenaCell(x, y, z, y == 2 ? "minecraft:light_gray_stained_glass" : "minecraft:light_gray_concrete"));
            }
        }
        return out;
    }

    /** Phase IV: the ceiling, the fog and every partition dissolve into golden sky (air); the floor, the dais and the desks stay. */
    public static List<ArenaCell> dissolve() {
        List<ArenaCell> out = new ArrayList<>();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                for (int y = 0; y <= CEILING + 1; y++) {
                    String s = cell(x, y, z);
                    if (Kinds.air(s)) continue;
                    boolean keep = !fog(x, z) && y < CEILING && !St.path(s).contains("pane") && !St.path(s).equals("light_gray_concrete")
                            && !St.path(s).equals("quartz_pillar");
                    if (!keep) out.add(new ArenaCell(x, y, z, St.AIR));
                }
            }
        }
        return out;
    }
}
