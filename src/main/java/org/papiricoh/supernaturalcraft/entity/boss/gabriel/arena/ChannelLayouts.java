package org.papiricoh.supernaturalcraft.entity.boss.gabriel.arena;

import org.papiricoh.supernaturalcraft.entity.boss.gabriel.Channel;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The four sets of TV Land (pure, tested in JUnit), one per {@link Channel}, all on one footprint: each a plan of
 * {@link ArenaCell}s relative to the surface. The front of every set (where an audience would sit) is the south (+z), its
 * back the north (-z); "left to right as seen from the front" is west (-x) to east (+x).
 * <ul>
 *   <li>{@link Channel#SITCOM}: an 80s living room on a parquet floor: a couch, a rug, two lamps, a staircase to nowhere, a
 *   back flat with three open doors (the extras walk in through them), a studio audience's bleacher and studio lights;</li>
 *   <li>{@link Channel#GAME_SHOW}: a neon game-show studio: three big platforms (RED, BLUE, YELLOW, {@link #PLATFORMS}), each
 *   over a trapdoor pit with foam (slime) at the bottom, a scoreboard wall, neon posts, the host's podium;</li>
 *   <li>{@link Channel#HOSPITAL}: Dr. Sexy's ward in teal and white: beds and curtains along both sides, a giant heart
 *   monitor on the back wall, surgery lights, an operating table;</li>
 *   <li>{@link Channel#COMMERCIAL}: a bright white stage for "Trickster Treats": five podiums in an arc ({@link #PODIUMS}),
 *   a giant wrapped candy, spotlights.</li>
 * </ul>
 * The arena is pinned once over the {@link #union} of every set's cells and each change of channel rewrites those same
 * positions ({@link #blocksOf}): a cell a set does not use becomes air above the surface and plain ground below it. Every
 * set stays inside the arena (a margin from the wall), under {@link #BUDGET}, keeps the centre (the altar) clear and uses
 * vanilla blocks without block entities only (the arena cannot rewrite a block entity).
 */
public final class ChannelLayouts {

    /** The most blocks one set may change; the most positions the four together may pin. */
    public static final int BUDGET = 8000, UNION_BUDGET = 12000;
    /** Cells this close to the wall are left alone. */
    public static final int MARGIN = 2;
    /** Nothing stands this close to the centre (the altar, the ring of holy oil). */
    public static final int CLEAR_CENTRE = 4;
    /** The highest and lowest a set builds or digs, relative to the surface. */
    public static final int TOP = 10, BOTTOM = -3;
    /** The smallest arena the sets are drawn for (the props sit at fixed offsets). */
    public static final int MIN_RADIUS = 18;

    public static final String AIR = "minecraft:air";

    /** A position relative to the centre column's surface ({@code dy} 0 is the floor). */
    public record Spot(int dx, int dy, int dz) {
        public int distanceSq() {
            return dx * dx + dz * dz;
        }
    }

    // --- the game show's platforms -----------------------------------------------------------------------------------
    /** The three platforms' centres, RED, BLUE and YELLOW (index 0, 1, 2), left to right as seen from the front. */
    public static final List<Spot> PLATFORMS = List.of(new Spot(-8, 0, -7), new Spot(0, 0, -9), new Spot(8, 0, -7));
    /** Each platform is (2·half+1)² cells at the floor's level, over a pit this deep whose bottom is foam. */
    public static final int PLATFORM_HALF = 2, PIT_DEPTH = 3;
    public static final List<String> PLATFORM_BLOCKS = List.of("minecraft:red_concrete", "minecraft:blue_concrete", "minecraft:yellow_concrete");
    public static final String FOAM = "minecraft:slime_block";

    // --- the commercial's podiums ------------------------------------------------------------------------------------
    /** Where the five spokesmen stand (on top of each podium: {@code dy} 2), in an arc round the north of the stage. */
    public static final List<Spot> PODIUMS = List.of(new Spot(-8, 2, -4), new Spot(-4, 2, -8), new Spot(0, 2, -9), new Spot(4, 2, -8),
            new Spot(8, 2, -4));

    // --- the sitcom's doors --------------------------------------------------------------------------------------------
    /** The back flat's line. */
    public static final int FLAT_Z = -12;
    /** The three doors in the back flat (their lower half). */
    public static final List<Spot> DOORS = List.of(new Spot(-8, 1, FLAT_Z), new Spot(0, 1, FLAT_Z), new Spot(8, 1, FLAT_Z));
    /** Backstage, behind each door: where the extras come from. */
    public static final List<Spot> BACKSTAGE = List.of(new Spot(-8, 1, FLAT_Z - 2), new Spot(0, 1, FLAT_Z - 2), new Spot(8, 1, FLAT_Z - 2));

    // --- the hospital's beds -------------------------------------------------------------------------------------------
    /** Beside each bed's foot, where the nurses get up from (both sides of the ward; the bed runs on toward the wall). */
    public static final List<Spot> BEDS;

    static {
        List<Spot> beds = new ArrayList<>();
        for (int side : new int[]{-1, 1}) for (int z = -8; z <= 8; z += 4) beds.add(new Spot(side * 10, 1, z));
        BEDS = List.copyOf(beds);
    }

    private ChannelLayouts() {
    }

    /** One channel's set. */
    public static List<ArenaCell> plan(Channel channel, int radius) {
        return switch (channel) {
            case SITCOM -> sitcom(radius);
            case GAME_SHOW -> gameShow(radius);
            case HOSPITAL -> hospital(radius);
            case COMMERCIAL -> commercial(radius);
        };
    }

    /**
     * Every position any set writes, once, highest first (what stands on a block goes before the block itself), with an
     * empty block: the arena pins these and {@link #blocksOf} fills them in.
     */
    public static List<ArenaCell> union(int radius) {
        Map<Long, ArenaCell> all = new LinkedHashMap<>();
        for (Channel c : Channel.values()) {
            for (ArenaCell cell : plan(c, radius)) all.putIfAbsent(key(cell.dx(), cell.dy(), cell.dz()), new ArenaCell(cell.dx(), cell.dy(), cell.dz(), ""));
        }
        List<ArenaCell> out = new ArrayList<>(all.values());
        out.sort(Comparator.comparingInt((ArenaCell c) -> -c.dy()).thenComparingInt(ArenaCell::dx).thenComparingInt(ArenaCell::dz));
        return out;
    }

    /** What {@code channel}'s set puts at each of {@code union}'s positions, in the same order. */
    public static List<String> blocksOf(Channel channel, List<ArenaCell> union, int radius) {
        Map<Long, String> mine = new LinkedHashMap<>();
        for (ArenaCell c : plan(channel, radius)) mine.put(key(c.dx(), c.dy(), c.dz()), c.block());
        List<String> out = new ArrayList<>(union.size());
        for (ArenaCell c : union) {
            String b = mine.get(key(c.dx(), c.dy(), c.dz()));
            out.add(b != null ? b : filler(channel, c.dx(), c.dy(), c.dz()));
        }
        return out;
    }

    /** What a set leaves where it builds nothing: air above its floor, its floor, plain ground below. */
    public static String filler(Channel channel, int dx, int dy, int dz) {
        if (dy > 0) return AIR;
        if (dy < 0) return "minecraft:dirt";
        return floor(channel, dx, dz);
    }

    /** The cells of platform {@code index} (its top, at the floor's level). */
    public static List<Spot> platformCells(int index) {
        Spot c = PLATFORMS.get(index);
        List<Spot> out = new ArrayList<>();
        for (int x = -PLATFORM_HALF; x <= PLATFORM_HALF; x++) {
            for (int z = -PLATFORM_HALF; z <= PLATFORM_HALF; z++) out.add(new Spot(c.dx() + x, 0, c.dz() + z));
        }
        return out;
    }

    /** The platform whose top covers column (dx, dz), or -1. */
    public static int platformAt(int dx, int dz) {
        for (int i = 0; i < PLATFORMS.size(); i++) {
            Spot c = PLATFORMS.get(i);
            if (Math.abs(dx - c.dx()) <= PLATFORM_HALF && Math.abs(dz - c.dz()) <= PLATFORM_HALF) return i;
        }
        return -1;
    }

    // --- floors --------------------------------------------------------------------------------------------------------

    /** Each set's floor at a column. */
    public static String floor(Channel channel, int dx, int dz) {
        return switch (channel) {
            case SITCOM -> sitcomFloor(dx, dz);
            case GAME_SHOW -> {
                boolean xl = Math.floorMod(dx, 5) == 0, zl = Math.floorMod(dz, 5) == 0;
                yield xl && zl ? "minecraft:pearlescent_froglight" : xl ? "minecraft:magenta_concrete" : zl ? "minecraft:cyan_concrete"
                        : "minecraft:black_concrete";
            }
            case HOSPITAL -> Math.abs(dx) <= 1 ? "minecraft:cyan_terracotta"
                    : Math.floorMod(dx + dz, 2) == 0 ? "minecraft:white_concrete" : "minecraft:light_gray_concrete";
            case COMMERCIAL -> {
                int d2 = dx * dx + dz * dz;
                if (d2 <= 9) yield "minecraft:quartz_block";
                double a = Math.atan2(dz, dx);
                int ray = (int) Math.floor((a + Math.PI) / (Math.PI / 8));
                yield ray % 2 == 0 ? "minecraft:pink_concrete" : "minecraft:white_concrete";
            }
        };
    }

    private static String sitcomFloor(int dx, int dz) {
        // The rug in the middle of the living room.
        if (Math.abs(dx) <= 5 && dz >= -4 && dz <= 3) {
            if (Math.abs(dx) == 5 || dz == -4 || dz == 3) return "minecraft:brown_wool";
            return (Math.abs(dx) + Math.abs(dz)) % 3 == 0 ? "minecraft:yellow_wool" : "minecraft:orange_wool";
        }
        // The set's parquet, the studio's grey floor round it.
        if (Math.abs(dx) <= 12 && dz >= FLAT_Z && dz <= 8) {
            return (Math.floorMod(dx, 4) < 2) == (Math.floorMod(dz, 4) < 2) ? "minecraft:oak_planks" : "minecraft:spruce_planks";
        }
        return "minecraft:gray_concrete";
    }

    // --- CH 2: the sitcom ----------------------------------------------------------------------------------------------

    public static List<ArenaCell> sitcom(int radius) {
        Plan p = new Plan(radius);
        p.floor(Channel.SITCOM);
        // The back flat: wallpaper in stripes over a dark baseboard, three open doors, two windows, the LAUGH sign on top.
        for (int x = -10; x <= 10; x++) {
            for (int y = 1; y <= 6; y++) {
                String b = y == 1 || y == 6 ? "minecraft:dark_oak_planks"
                        : Math.floorMod(x, 3) == 0 ? "minecraft:orange_terracotta" : "minecraft:yellow_terracotta";
                if (Math.abs(x) == 5 && (y == 3 || y == 4)) b = "minecraft:light_blue_stained_glass";
                p.put(x, y, FLAT_Z, b);
            }
        }
        for (Spot d : DOORS) {
            p.put(d.dx(), 1, d.dz(), "minecraft:oak_door[facing=south,half=lower,hinge=left,open=true]");
            p.put(d.dx(), 2, d.dz(), "minecraft:oak_door[facing=south,half=upper,hinge=left,open=true]");
            p.put(d.dx(), 3, d.dz(), "minecraft:dark_oak_planks");
        }
        for (int x = -3; x <= 3; x++) p.put(x, 7, FLAT_Z, Math.abs(x) == 3 ? "minecraft:shroomlight" : "minecraft:red_concrete");
        // The staircase to nowhere, along the west side.
        for (int i = 0; i <= 5; i++) {
            for (int x = -14; x <= -13; x++) {
                int z = -2 - i;
                for (int y = 1; y < 1 + i; y++) p.put(x, y, z, "minecraft:oak_planks");
                p.put(x, 1 + i, z, "minecraft:oak_stairs[facing=north]");
            }
        }
        // The couch facing the audience, its coffee table, two lamps.
        for (int x = -3; x <= 3; x++) {
            p.put(x, 1, -7, "minecraft:red_nether_brick_stairs[facing=north]");
            p.put(x, 1, -8, "minecraft:red_wool");
            p.put(x, 2, -8, "minecraft:red_wool");
        }
        for (int s : new int[]{-4, 4}) {
            p.put(s, 1, -7, "minecraft:red_nether_bricks");
            p.put(s, 1, -8, "minecraft:red_nether_bricks");
        }
        for (int x = -1; x <= 1; x++) p.put(x, 1, -5, "minecraft:dark_oak_slab[type=bottom]");
        for (int s : new int[]{-6, 6}) {
            p.put(s, 1, -8, "minecraft:dark_oak_fence");
            p.put(s, 2, -8, "minecraft:dark_oak_fence");
            p.put(s, 3, -8, "minecraft:lantern[hanging=false]");
        }
        // The kitchen counter on the east, with its sink.
        for (int z = -8; z <= -3; z++) {
            p.put(12, 1, z, z == -6 ? "minecraft:cauldron" : "minecraft:smooth_quartz");
            p.put(13, 1, z, "minecraft:white_terracotta");
        }
        // The studio audience's bleacher at the front.
        for (int z = 10; z <= 14; z++) {
            int tier = z - 9;
            for (int x = -12; x <= 12; x++) {
                if (!p.inside(x, z)) continue;
                for (int y = 1; y < tier; y++) p.put(x, y, z, "minecraft:spruce_planks");
                p.put(x, tier, z, Math.floorMod(x, 4) == 0 ? "minecraft:dark_oak_planks" : "minecraft:red_wool");
            }
        }
        // Studio lights on poles.
        for (int[] at : new int[][]{{-13, -4}, {13, -4}, {-11, 8}, {11, 8}}) lightPole(p, at[0], at[1], 7, "minecraft:sea_lantern");
        return p.cells();
    }

    // --- CH 5: the game show -------------------------------------------------------------------------------------------

    public static List<ArenaCell> gameShow(int radius) {
        Plan p = new Plan(radius);
        p.floor(Channel.GAME_SHOW);
        // The three platforms over their trapdoor pits, framed in white with lights at the corners.
        for (int i = 0; i < PLATFORMS.size(); i++) {
            Spot c = PLATFORMS.get(i);
            for (int x = -PLATFORM_HALF - 1; x <= PLATFORM_HALF + 1; x++) {
                for (int z = -PLATFORM_HALF - 1; z <= PLATFORM_HALF + 1; z++) {
                    int ax = c.dx() + x, az = c.dz() + z;
                    boolean rim = Math.abs(x) > PLATFORM_HALF || Math.abs(z) > PLATFORM_HALF;
                    if (rim) {
                        boolean corner = Math.abs(x) > PLATFORM_HALF && Math.abs(z) > PLATFORM_HALF;
                        p.put(ax, 0, az, corner ? "minecraft:sea_lantern" : "minecraft:white_concrete");
                        continue;
                    }
                    p.put(ax, 0, az, PLATFORM_BLOCKS.get(i));
                    for (int y = -1; y > -PIT_DEPTH; y--) p.put(ax, y, az, AIR);
                    p.put(ax, -PIT_DEPTH, az, FOAM);
                }
            }
        }
        // The scoreboard wall behind them: black, framed in magenta, rows of bulbs.
        for (int x = -7; x <= 7; x++) {
            for (int y = 1; y <= 8; y++) {
                boolean frame = Math.abs(x) == 7 || y == 1 || y == 8;
                String b = frame ? "minecraft:magenta_concrete" : (y % 2 == 0 && Math.floorMod(x, 2) == 0) ? "minecraft:ochre_froglight"
                        : "minecraft:black_concrete";
                p.put(x, y, -14, b);
            }
        }
        // The host's podium at the front.
        for (int x = -1; x <= 1; x++) p.put(x, 1, 7, "minecraft:gold_block");
        // Neon posts down both sides, confetti cannons.
        for (int side : new int[]{-1, 1}) {
            for (int z = -6; z <= 6; z += 4) {
                for (int y = 1; y <= 5; y++) p.put(side * 14, y, z, "minecraft:purpur_pillar");
                p.put(side * 14, 6, z, Math.floorMod(z, 8) == 2 ? "minecraft:verdant_froglight" : "minecraft:pearlescent_froglight");
            }
            p.put(side * 12, 1, 4, "minecraft:red_glazed_terracotta");
            p.put(side * 12, 2, 4, "minecraft:piston[facing=up]");
        }
        return p.cells();
    }

    // --- CH 7: the hospital --------------------------------------------------------------------------------------------

    public static List<ArenaCell> hospital(int radius) {
        Plan p = new Plan(radius);
        p.floor(Channel.HOSPITAL);
        // Beds along both sides (blanket, pillow, an iron headboard at the wall), curtains between them.
        for (Spot by : BEDS) {
            int side = Integer.signum(by.dx());
            p.put(by.dx() + side, 1, by.dz(), "minecraft:light_blue_wool");
            p.put(by.dx() + side * 2, 1, by.dz(), "minecraft:white_wool");
            p.put(by.dx() + side * 3, 1, by.dz(), "minecraft:iron_bars");
            p.put(by.dx() + side * 3, 2, by.dz(), "minecraft:iron_bars");
        }
        for (int side : new int[]{-1, 1}) {
            for (int z = -6; z <= 6; z += 4) {
                for (int x = 11; x <= 13; x++) for (int y = 1; y <= 3; y++) p.put(side * x, y, z, "minecraft:cyan_stained_glass");
            }
            // Low teal walls at the ends of the ward.
            for (int z = -5; z <= 5; z++) for (int y = 1; y <= 2; y++) p.put(side * 15, y, z, "minecraft:cyan_terracotta");
        }
        // The giant heart monitor on the back wall: a black screen in a grey frame, a green trace with a spike.
        for (int x = -7; x <= 7; x++) {
            for (int y = 1; y <= 8; y++) {
                boolean frame = Math.abs(x) == 7 || y == 1 || y == 8;
                p.put(x, y, -14, frame ? "minecraft:light_gray_concrete" : "minecraft:black_concrete");
            }
        }
        int[] trace = {4, 4, 4, 5, 4, 7, 2, 6, 4, 4, 5, 4, 4};
        for (int i = 0; i < trace.length; i++) {
            int x = i - 6;
            int y0 = trace[i], y1 = i + 1 < trace.length ? trace[i + 1] : trace[i];
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) p.put(x, y, -14, "minecraft:verdant_froglight");
        }
        // Surgery lights over the operating area, the table at the front.
        for (int side : new int[]{-1, 1}) {
            for (int y = 1; y <= 6; y++) p.put(side * 7, y, -2, "minecraft:iron_bars");
            for (int x = 5; x <= 7; x++) p.put(side * x, 7, -2, "minecraft:sea_lantern");
        }
        for (int x = -1; x <= 1; x++) p.put(x, 1, 7, "minecraft:quartz_slab[type=top]");
        return p.cells();
    }

    // --- CH 9: the commercial ------------------------------------------------------------------------------------------

    public static List<ArenaCell> commercial(int radius) {
        Plan p = new Plan(radius);
        p.floor(Channel.COMMERCIAL);
        // Five podiums in an arc: a quartz top to stand on, pink arms round it.
        for (Spot s : PODIUMS) {
            p.put(s.dx(), 1, s.dz(), "minecraft:smooth_quartz");
            p.put(s.dx() + 1, 1, s.dz(), "minecraft:pink_concrete");
            p.put(s.dx() - 1, 1, s.dz(), "minecraft:pink_concrete");
            p.put(s.dx(), 1, s.dz() - 1, "minecraft:pink_concrete");
            p.put(s.dx(), 1, s.dz() + 1, "minecraft:pink_concrete");
        }
        // The giant wrapped candy on its stand at the back, red and white, its wrapper twisted at both ends.
        int cz = -14;
        for (int x = -4; x <= 4; x++) {
            for (int y = 3; y <= 7; y++) {
                double ex = x / 4.5, ey = (y - 5) / 2.5;
                if (ex * ex + ey * ey <= 1.0) p.put(x, y, cz, Math.floorMod(x - y, 3) == 0 ? "minecraft:red_concrete" : "minecraft:white_concrete");
            }
        }
        for (int k = 1; k <= 3; k++) {
            for (int side : new int[]{-1, 1}) {
                for (int y = 5 - k + 1; y <= 5 + k - 1; y++) p.put(side * (4 + k), y, cz, "minecraft:magenta_stained_glass");
            }
        }
        for (int side : new int[]{-1, 1}) for (int y = 1; y <= 2; y++) p.put(side * 2, y, cz, "minecraft:iron_bars");
        // The sponsor's banner above it.
        for (int x = -6; x <= 6; x++) p.put(x, 9, cz, Math.floorMod(x, 2) == 0 ? "minecraft:yellow_concrete" : "minecraft:pink_concrete");
        // Spotlights.
        for (int[] at : new int[][]{{-12, -8}, {12, -8}, {-12, 8}, {12, 8}}) lightPole(p, at[0], at[1], 7, "minecraft:sea_lantern");
        return p.cells();
    }

    private static void lightPole(Plan p, int x, int z, int height, String lamp) {
        for (int y = 1; y < height; y++) p.put(x, y, z, "minecraft:iron_bars");
        p.put(x, height, z, lamp);
    }

    static long key(int dx, int dy, int dz) {
        return ((long) (dx + 512) << 40) | ((long) (dy + 512) << 20) | (dz + 512);
    }

    /** A set being drawn: cells keyed by position (a later put replaces an earlier one), kept inside the margin. */
    private static final class Plan {
        private final int radius;
        private final LinkedHashMap<Long, ArenaCell> cells = new LinkedHashMap<>();

        Plan(int radius) {
            this.radius = radius;
        }

        boolean inside(int dx, int dz) {
            int r = radius - MARGIN;
            return dx * dx + dz * dz <= r * r;
        }

        /** The whole floor of the set, every column inside the margin. */
        void floor(Channel channel) {
            int r = radius - MARGIN;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) if (inside(dx, dz)) put(dx, 0, dz, ChannelLayouts.floor(channel, dx, dz));
            }
        }

        void put(int dx, int dy, int dz, String block) {
            if (!inside(dx, dz)) return;
            // The centre stays clear above the floor: the altar and its ring of fire stand there.
            if (dy >= 1 && dx * dx + dz * dz <= CLEAR_CENTRE * CLEAR_CENTRE) return;
            cells.put(key(dx, dy, dz), new ArenaCell(dx, dy, dz, block));
        }

        List<ArenaCell> cells() {
            return new ArrayList<>(cells.values());
        }
    }
}
