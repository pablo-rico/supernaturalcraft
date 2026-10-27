package org.papiricoh.supernaturalcraft.structure.spire;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.chorus.Melody;

import java.util.Set;

/**
 * The Hymnal Spire's materials (pure): white choir stone (quartz bricks and calcite, smooth quartz trim, gold inlay) gone grey
 * and mossy toward the ground, footings of old stone brick, verdigris copper roofs. Every part of the spire paints from here so
 * they read as one place.
 *
 * <h2>Footings</h2>
 * The parts are drawn level, but they stand on a mountainside. A column whose <b>lowest planned cell</b> is one of
 * {@link #FOOTING_IDS} (with nothing planned under it) is a footing: whoever writes the plan carries it on down to the ground
 * with {@link #footing} ({@code SpireBuilder} against the world, {@link #extendFootings} against a sampled terrain). Anything
 * else (quartz, slabs, stairs, air) ends where it is drawn: corbels and bridges hang.
 */
public final class SpireKit {

    public static final String QUARTZ_BRICKS = "minecraft:quartz_bricks", SMOOTH = "minecraft:smooth_quartz", CHISELED = "minecraft:chiseled_quartz_block",
            QUARTZ = "minecraft:quartz_block", CALCITE = "minecraft:calcite", GOLD = "minecraft:gold_block", DIORITE = "minecraft:polished_diorite";
    /** The railing and parapet wall. */
    public static final String BALUSTER = "minecraft:diorite_wall", MOSSY_BALUSTER = "minecraft:mossy_stone_brick_wall";
    public static final String ALTAR = "supernaturalcraft:choir_altar";

    /** What a footing is made of: the lowest cell of a column in one of these is carried on down to the ground. */
    public static final Set<String> FOOTING_IDS = Set.of("minecraft:stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks",
            "minecraft:cobblestone", "minecraft:mossy_cobblestone", "minecraft:andesite", "minecraft:tuff");
    /** How far a footing may run down before it gives up (a cliff it cannot reach). */
    public static final int FOOTING_LIMIT = 48;

    /** Loot tables of the spire's chests. */
    public static final String LOOT_SUMMIT = "supernaturalcraft:chests/spire/summit", LOOT_TEMPLE = "supernaturalcraft:chests/spire/temple",
            LOOT_CRYPT = "supernaturalcraft:chests/spire/crypt", LOOT_MONASTERY = "supernaturalcraft:chests/spire/monastery";

    public static final Family TRIM = Family.SMOOTH_QUARTZ, QUARTZ_TRIM = Family.QUARTZ, FOOT = Family.STONE_BRICKS,
            ROOF = Family.OXIDIZED_CUT_COPPER;

    static {
        for (String name : Melody.NAMES) Kinds.declareLike(bell(indexOf(name)), "minecraft:bell");
        Kinds.declareLight(ALTAR, 9);
    }

    private SpireKit() {
    }

    private static int indexOf(String name) {
        for (int i = 0; i < Melody.NAMES.length; i++) if (Melody.NAMES[i].equals(name)) return i;
        throw new IllegalArgumentException(name);
    }

    /** Choir bell {@code note} (0–6, {@link Melody#NAMES}). */
    public static String bell(int note) {
        return "supernaturalcraft:choir_bell_" + Melody.NAMES[note];
    }

    /** The full stained glass block of a note's colour ({@link Melody#DYES}). */
    public static String noteGlass(int note) {
        return "minecraft:" + Melody.DYES[note].getSerializedName() + "_stained_glass";
    }

    /** Makes sure the static declarations ran (call before drawing). */
    static void init() {
    }

    // --- brushes -------------------------------------------------------------------------------------------------------------

    /**
     * Choir masonry: quartz bricks with calcite and smooth quartz in clumps, a stain of diorite; within about three rows of
     * {@code groundY} it goes over to mossy and cracked stone brick (damp creeping up from the ground).
     */
    public static Brush wall(int seed, int groundY) {
        Brush white = Palette.patches(seed, 2.0, QUARTZ_BRICKS, 5, CALCITE, 3, SMOOTH, 2, "minecraft:diorite", 1);
        Brush damp = Palette.patches(seed + 1, 2.0, "minecraft:mossy_stone_bricks", 3, "minecraft:stone_bricks", 2,
                "minecraft:cracked_stone_bricks", 2, CALCITE, 1);
        return (x, y, z) -> {
            double wet = 1 - (y - groundY) / 3.5 + (Noise.patch3(x, y, z, 3, seed + 2) - 0.5) * 1.1;
            return wet > 0.55 ? damp.at(x, y, z) : white.at(x, y, z);
        };
    }

    /** White floor paving: smooth quartz and quartz bricks in a loose checker, calcite and cracked patches, rare moss. */
    public static Brush floor(int seed) {
        Brush a = Palette.patches2(seed, 2.2, SMOOTH, 5, CALCITE, 2, "minecraft:diorite", 1);
        Brush b = Palette.patches2(seed + 1, 2.2, QUARTZ_BRICKS, 5, CALCITE, 2, "minecraft:mossy_stone_bricks", 1);
        return Palette.checker(1, a, b);
    }

    /** Footing stone (and what {@link #extendFootings} and the world writer carry down to the ground). */
    public static Brush footing(int seed) {
        return Palette.mix(seed, "minecraft:stone_bricks", 4, "minecraft:mossy_stone_bricks", 3, "minecraft:cracked_stone_bricks", 2,
                "minecraft:cobblestone", 1, "minecraft:mossy_cobblestone", 1, "minecraft:andesite", 1);
    }

    /** Copper roofing in its slow turn from weathered to verdigris (see {@link #weatherRoof}). */
    public static Roofs.Style roof(int seed, Brush infill) {
        return new Roofs.Style(ROOF, Family.WEATHERED_CUT_COPPER, infill, 1, Roofs.Pitch.STEEP, 0, true, null, true);
    }

    /** Patches of a roof still weathered rather than oxidized, and a few slipped to stone. */
    public static void weatherRoof(Canvas c, Box region, int seed) {
        Palette.swapFamily(c, region, ROOF, Family.WEATHERED_CUT_COPPER, 0.3, 2.5, seed);
        Palette.swapFamily(c, region, ROOF, Family.EXPOSED_CUT_COPPER, 0.06, 1.5, seed + 1);
    }

    public static String pillar(String axis) {
        return St.axis("minecraft:quartz_pillar", axis);
    }

    // --- recurring pieces -----------------------------------------------------------------------------------------------------

    /** A short balustrade cell: a wall post (mossy now and then). */
    public static String baluster(int x, int y, int z, int seed) {
        return St.wall(Noise.chance(x, y, z, seed, 0.18) ? MOSSY_BALUSTER : BALUSTER);
    }

    /** A footing column from {@code top} down to {@code bottom} inclusive (its lowest cell makes it run on to the ground). */
    public static void pier(Canvas c, int x, int z, int top, int bottom, Brush brush) {
        for (int y = top; y >= bottom; y--) c.set(x, y, z, brush.at(x, y, z));
    }

    /** Vines on the open faces of the cells round a column range, by chance (weathering). */
    public static void ivy(Canvas c, Box region, double chance, int seed) {
        ivy(c, region, null, chance, seed);
    }

    /** As {@link #ivy(Canvas, Box, double, int)}, leaving out the columns of {@code inside} (rooms: ivy grows outdoors). */
    public static void ivy(Canvas c, Box region, Box inside, double chance, int seed) {
        for (int x = region.x0(); x <= region.x1(); x++) {
            for (int z = region.z0(); z <= region.z1(); z++) {
                if (inside != null && inside.containsColumn(x, z)) continue;
                for (int y = region.y1(); y >= region.y0(); y--) {
                    if (!c.isAir(x, y, z) || !Noise.chance(x, y, z, seed, chance)) continue;
                    for (Dir d : Dir.HORIZONTAL) {
                        String w = c.get(x + d.dx, y, z + d.dz);
                        if (w != null && Kinds.opaqueCube(w) && !Kinds.log(w)) {
                            c.set(x, y, z, St.vine(d));
                            // Hang a little further down while there is air and the wall goes on.
                            for (int k = 1; k <= 2 && c.isAir(x, y - k, z); k++) {
                                String below = c.get(x + d.dx, y - k, z + d.dz);
                                if (below == null || !Kinds.opaqueCube(below)) break;
                                c.set(x, y - k, z, St.vine(d));
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    // --- footings ---------------------------------------------------------------------------------------------------------------

    /** Whether a state is footing stone. */
    public static boolean isFooting(String state) {
        return state != null && FOOTING_IDS.contains(St.id(state));
    }

    /** Ground under a layout: the top solid y of a column in canvas coordinates (a sampled or synthetic terrain). */
    @FunctionalInterface
    public interface Ground {
        int top(int x, int z);
    }

    /** A footing steps out one cell for every this many blocks it runs down (a battered base, not a sheer wall). */
    public static final int BATTER = 5;

    /** Where footing cells go (the world, or a canvas). */
    @FunctionalInterface
    public interface Sink {
        void put(int x, int y, int z, String state);
    }

    /** Whether a cell is ground a footing stops on. */
    @FunctionalInterface
    public interface Solid {
        boolean at(int x, int y, int z);
    }

    /**
     * The footing columns of a plan: every column whose lowest planned cell is footing stone, as {x, lowest y, z}
     * ({@code cells}: x, y, z and state of each planned cell).
     */
    public static java.util.List<int[]> footingColumns(Iterable<? extends Object[]> cells) {
        java.util.Map<Long, Object[]> lowest = new java.util.HashMap<>();
        for (Object[] c : cells) {
            long k = colKey((int) c[0], (int) c[2]);
            Object[] l = lowest.get(k);
            if (l == null || (int) c[1] < (int) l[1]) lowest.put(k, c);
        }
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (Object[] l : lowest.values()) if (isFooting((String) l[3])) out.add(new int[]{(int) l[0], (int) l[1], (int) l[2]});
        out.sort(java.util.Comparator.<int[]>comparingInt(a -> a[0]).thenComparingInt(a -> a[2]));
        return java.util.List.copyOf(out);
    }

    public static long colKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /**
     * Runs footing columns ({x, lowest y, z}, offset by (ox, oy, oz)) down until they meet {@code solid} ground (at most
     * {@link #FOOTING_LIMIT}); on the outline, every {@link #BATTER} blocks down the face steps one cell further out (the
     * corners fill in), so a deep footing reads as a battered base. Returns the cells written.
     */
    public static int runFootings(java.util.List<int[]> columns, int ox, int oy, int oz, Solid solid, Sink sink, Brush brush) {
        java.util.Set<Long> set = new java.util.HashSet<>();
        for (int[] f : columns) set.add(colKey(f[0], f[2]));
        int n = 0;
        for (int[] f : columns) {
            int x = f[0] + ox, z = f[2] + oz;
            boolean e = !set.contains(colKey(f[0] + 1, f[2])), w = !set.contains(colKey(f[0] - 1, f[2]));
            boolean so = !set.contains(colKey(f[0], f[2] + 1)), no = !set.contains(colKey(f[0], f[2] - 1));
            for (int k = 1; k <= FOOTING_LIMIT; k++) {
                int y = f[1] + oy - k;
                if (solid.at(x, y, z)) break;
                sink.put(x, y, z, brush.at(x, y, z));
                n++;
                if (k % BATTER != 0) continue;
                // A new step: the ring of cells one further out runs from here to the ground.
                int step = k / BATTER;
                int x0 = w ? -step : 0, x1 = e ? step : 0, z0 = no ? -step : 0, z1 = so ? step : 0;
                for (int dx = x0; dx <= x1; dx++) {
                    for (int dz = z0; dz <= z1; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != step || set.contains(colKey(f[0] + dx, f[2] + dz))) continue;
                        for (int yy = y; yy > f[1] + oy - FOOTING_LIMIT && !solid.at(x + dx, yy, z + dz); yy--) {
                            sink.put(x + dx, yy, z + dz, brush.at(x + dx, yy, z + dz));
                            n++;
                        }
                    }
                }
            }
        }
        return n;
    }

    /** Carries every footing column of a canvas on down to {@code ground} (pure twin of the world writer). */
    public static int extendFootings(Canvas c, Ground ground, int seed) {
        java.util.List<Object[]> cells = new java.util.ArrayList<>();
        for (var e : c.map().entrySet()) cells.add(new Object[]{Canvas.kx(e.getKey()), Canvas.ky(e.getKey()), Canvas.kz(e.getKey()), e.getValue()});
        java.util.List<int[]> cols = footingColumns(cells);
        java.util.Map<Long, String> out = new java.util.HashMap<>();
        Solid solid = (x, y, z) -> y <= ground.top(x, z) || out.containsKey(Canvas.key(x, y, z));
        int n = runFootings(cols, 0, 0, 0, solid, (x, y, z, st) -> out.put(Canvas.key(x, y, z), st), footing(seed));
        for (var e : out.entrySet()) c.setIfEmpty(Canvas.kx(e.getKey()), Canvas.ky(e.getKey()), Canvas.kz(e.getKey()), e.getValue());
        return n;
    }
}
