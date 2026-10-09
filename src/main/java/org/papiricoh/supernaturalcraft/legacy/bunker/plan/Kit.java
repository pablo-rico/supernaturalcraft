package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import java.util.List;

/**
 * The bunker's style kit (pure): the "Lebanon" palette and the brushes every part draws with, so the whole bunker reads as one
 * architect's work. The look, after the series' war room: dark green tile below a black dado, cream ashlar above, black iron
 * columns, art-deco railings, grated ceilings with light behind them, checkered concrete and dark oak.
 *
 * <p>Rules the brushes keep (and the plan's tests check):
 * <ul>
 *   <li>stairs are always placed with the right {@code facing} and {@code half}; their {@code shape} (corners) comes from
 *       {@link PlanShapes}, so a run of moulding turns its corners by itself;</li>
 *   <li>bars, panes, fences and walls are placed unconnected: {@link PlanShapes} joins them;</li>
 *   <li>every flight of stairs keeps 3 blocks of headroom over each step;</li>
 *   <li>lanterns hang from something or stand on something; nothing floats that vanilla would drop.</li>
 * </ul>
 */
public final class Kit {

    /** A block state chosen by position. */
    @FunctionalInterface
    public interface Pattern {
        String at(int x, int y, int z);
    }

    // --- the palette ------------------------------------------------------------------------------------------------------

    /** The dado's foot, the black bands and the iron. */
    public static final String SKIRTING = "minecraft:polished_blackstone_bricks", BLACK = "minecraft:polished_blackstone",
            BLACK_CHISELED = "minecraft:chiseled_polished_blackstone", BLACK_STAIRS = "minecraft:polished_blackstone_stairs",
            BLACK_SLAB = "minecraft:polished_blackstone_slab", BLACK_WALL = "minecraft:polished_blackstone_wall",
            IRON_COLUMN = "minecraft:polished_basalt";
    /** The green tile and its cap. */
    public static final String TILE = "minecraft:dark_prismarine", TILE_CAP = "minecraft:polished_deepslate",
            TILE_CAP_STAIRS = "minecraft:polished_deepslate_stairs", TILE_CAP_SLAB = "minecraft:polished_deepslate_slab";
    /** The cream ashlar above. */
    public static final String CREAM = "minecraft:smooth_sandstone", CREAM_COURSE = "minecraft:cut_sandstone",
            CREAM_STAIRS = "minecraft:smooth_sandstone_stairs", CREAM_SLAB = "minecraft:smooth_sandstone_slab",
            CREAM_CUT_SLAB = "minecraft:cut_sandstone_slab", CREAM_ACCENT = "minecraft:chiseled_sandstone";
    /** Steel (stairs, landings, frames). */
    public static final String STEEL = "minecraft:polished_deepslate", STEEL_STAIRS = "minecraft:polished_deepslate_stairs",
            STEEL_SLAB = "minecraft:polished_deepslate_slab", STEEL_TILES = "minecraft:deepslate_tiles";
    /** Dark oak (furniture, panelling, rails). */
    public static final String OAK = "minecraft:dark_oak_planks", OAK_STAIRS = "minecraft:dark_oak_stairs",
            OAK_SLAB = "minecraft:dark_oak_slab", OAK_LOG = "minecraft:stripped_dark_oak_log", OAK_FENCE = "minecraft:dark_oak_fence",
            OAK_TRAPDOOR = "minecraft:dark_oak_trapdoor", OAK_DOOR = "minecraft:dark_oak_door";
    /** The grated ceilings (light shows through them) and the light behind. */
    public static final String GRATE = "minecraft:waxed_oxidized_copper_grate", GRATE_WARM = "minecraft:waxed_copper_grate",
            HIDDEN_LIGHT = "minecraft:ochre_froglight", COOL_LIGHT = "minecraft:pearlescent_froglight";
    /** Concrete floors. */
    public static final String FLOOR_A = "minecraft:polished_andesite", FLOOR_B = "minecraft:polished_tuff",
            FLOOR_BORDER = "minecraft:polished_deepslate";

    /** The rock round every room (rarely seen: caves). */
    public static final Pattern ROCK = (x, y, z) -> {
        double n = Plan.noise(x, y, z, 7);
        return n < 0.78 ? "minecraft:stone" : n < 0.9 ? "minecraft:andesite" : n < 0.96 ? "minecraft:tuff" : "minecraft:cobblestone";
    };

    private Kit() {
    }

    // --- patterns -----------------------------------------------------------------------------------------------------------

    public static Pattern solid(String state) {
        return (x, y, z) -> state;
    }

    /** States by weight ({@code state, weight, state, weight…}), the same at a point every time. */
    public static Pattern mix(int salt, Object... stateAndWeight) {
        double total = 0;
        for (int i = 1; i < stateAndWeight.length; i += 2) total += ((Number) stateAndWeight[i]).doubleValue();
        final double sum = total;
        return (x, y, z) -> {
            double r = Plan.noise(x, y, z, salt) * sum;
            for (int i = 0; i + 1 < stateAndWeight.length; i += 2) {
                r -= ((Number) stateAndWeight[i + 1]).doubleValue();
                if (r < 0) return (String) stateAndWeight[i];
            }
            return (String) stateAndWeight[stateAndWeight.length - 2];
        };
    }

    /** Squares of {@code size} of two patterns. */
    public static Pattern checker(int size, Pattern a, Pattern b) {
        return (x, y, z) -> ((Math.floorDiv(x, size) + Math.floorDiv(z, size)) & 1) == 0 ? a.at(x, y, z) : b.at(x, y, z);
    }

    /** The war room's floor: a quiet checker of polished concrete in 3-block squares, mottled. */
    public static Pattern concreteFloor() {
        return checker(3, mix(11, FLOOR_A, 12, "minecraft:polished_andesite", 0, "minecraft:smooth_stone", 1),
                mix(12, FLOOR_B, 12, "minecraft:tuff_bricks", 1));
    }

    /** A wooden floor: planks in boards along an axis, two woods. */
    public static Pattern parquet(String a, String b, boolean alongX) {
        return (x, y, z) -> ((alongX ? Math.floorDiv(z, 1) : Math.floorDiv(x, 1)) + Math.floorDiv(alongX ? x : z, 4) & 1) == 0 ? a : b;
    }

    /**
     * The Lebanon wall by height above its floor: a black skirting, two courses of green tile, a dark cap, then cream ashlar
     * with a cut course every third row.
     */
    public static Pattern lebanonWall(int floor) {
        return (x, y, z) -> {
            int off = y - floor;
            if (off <= 1) return SKIRTING;
            if (off <= 3) return TILE;
            if (off == 4) return TILE_CAP;
            return (off - 5) % 3 == 2 ? CREAM_COURSE : CREAM;
        };
    }

    /** An upper storey's wall: a black skirting, then cream ashlar. */
    public static Pattern creamWall(int floor) {
        return (x, y, z) -> {
            int off = y - floor;
            if (off <= 1) return SKIRTING;
            return (off - 2) % 3 == 2 ? CREAM_COURSE : CREAM;
        };
    }

    // --- rooms ----------------------------------------------------------------------------------------------------------------

    /** A room's inside (inclusive): {@code floor} is the floor block's y, the air runs {@code floor+1 .. floor+height}. */
    public record Room(int x0, int x1, int floor, int height, int z0, int z1) {
        public int ceiling() {
            return floor + height + 1;
        }

        public int top() {
            return floor + height;
        }
    }

    /**
     * How a room is finished: floor, wall and ceiling patterns, an optional cornice (stairs id, run upside-down round the top),
     * optional pilasters (a state, every {@code pilasterEvery} blocks along each wall) and lights set in the ceiling every
     * {@code lightEvery} blocks (a state; null for none).
     */
    public record Style(Pattern floor, Pattern wall, Pattern ceiling, String cornice, String pilaster, int pilasterEvery, String light, int lightEvery) {
    }

    /**
     * Builds a room: air inside, the floor, a wall of finish one block thick round it, the ceiling, and rock beyond (only where the
     * plan has nothing yet, so neighbouring rooms keep their own finish). Then the pilasters, the cornice and the ceiling lights.
     */
    public static void room(Plan p, Room r, Style s) {
        enclose(p, r.x0 - 2, r.x1 + 2, r.floor - 1, r.ceiling() + 1, r.z0 - 2, r.z1 + 2);
        for (int x = r.x0 - 1; x <= r.x1 + 1; x++) {
            for (int z = r.z0 - 1; z <= r.z1 + 1; z++) {
                boolean in = x >= r.x0 && x <= r.x1 && z >= r.z0 && z <= r.z1;
                for (int y = r.floor; y <= r.ceiling(); y++) {
                    if (y == r.floor) p.set(x, y, z, s.floor.at(x, y, z));
                    else if (y == r.ceiling()) p.set(x, y, z, s.ceiling.at(x, y, z));
                    else p.set(x, y, z, in ? St.AIR : s.wall.at(x, y, z));
                }
            }
        }
        if (s.pilaster != null && s.pilasterEvery > 0) {
            for (int x = r.x0; x <= r.x1; x++) {
                if (Math.floorMod(x - r.x0, s.pilasterEvery) != 0) continue;
                for (int y = r.floor + 1; y <= r.top(); y++) {
                    p.set(x, y, r.z0 - 1, s.pilaster);
                    p.set(x, y, r.z1 + 1, s.pilaster);
                }
            }
            for (int z = r.z0; z <= r.z1; z++) {
                if (Math.floorMod(z - r.z0, s.pilasterEvery) != 0) continue;
                for (int y = r.floor + 1; y <= r.top(); y++) {
                    p.set(r.x0 - 1, y, z, s.pilaster);
                    p.set(r.x1 + 1, y, z, s.pilaster);
                }
            }
        }
        if (s.cornice != null) cornice(p, r, s.cornice);
        if (s.light != null && s.lightEvery > 0) {
            int ox = (r.x1 - r.x0) % s.lightEvery / 2, oz = (r.z1 - r.z0) % s.lightEvery / 2;
            for (int x = r.x0 + ox; x <= r.x1; x += s.lightEvery) {
                for (int z = r.z0 + oz; z <= r.z1; z += s.lightEvery) p.set(x, r.ceiling(), z, s.light);
            }
        }
    }

    /** Rock on the faces of a box where the plan has nothing yet. */
    public static void enclose(Plan p, int x0, int x1, int y0, int y1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1) p.fill(x, y, z, ROCK.at(x, y, z), false);
                }
            }
        }
    }

    /** Upside-down stairs round the top of a room's inside, backs to the walls (the corners turn by themselves). */
    public static void cornice(Plan p, Room r, String stairs) {
        int y = r.top();
        for (int x = r.x0; x <= r.x1; x++) {
            p.set(x, y, r.z0, St.stairs(stairs, "north", true));
            p.set(x, y, r.z1, St.stairs(stairs, "south", true));
        }
        for (int z = r.z0 + 1; z < r.z1; z++) {
            p.set(r.x0, y, z, St.stairs(stairs, "west", true));
            p.set(r.x1, y, z, St.stairs(stairs, "east", true));
        }
    }

    // --- openings -------------------------------------------------------------------------------------------------------------

    /**
     * A doorway through a wall: {@code (x, z)} is the opening's middle on the side one enters from, the passage runs
     * {@code through} for {@code depth} blocks, {@code width} wide (odd) and {@code height} tall above {@code floor}. Its jambs and
     * lintel through the whole depth are {@code frame}; {@code arch} (stairs id or null) rounds its top corners on both faces;
     * {@code door} (the two halves from {@link St#door}, or null) hangs in its first block.
     */
    public static void doorway(Plan p, int x, int floor, int z, String through, int depth, int width, int height, String frame, String arch, String[] door) {
        String side = St.cw(through);
        int sx = St.dx(side), sz = St.dz(side), tx = St.dx(through), tz = St.dz(through);
        int half = width / 2;
        for (int d = 0; d < depth; d++) {
            int cx = x + tx * d, cz = z + tz * d;
            for (int w = -half - 1; w <= half + 1; w++) {
                for (int y = floor + 1; y <= floor + height + 1; y++) {
                    boolean edge = Math.abs(w) == half + 1 || y == floor + height + 1;
                    p.set(cx + sx * w, y, cz + sz * w, edge ? frame : St.AIR);
                }
            }
            p.set(cx, floor, cz, p.get(cx, floor, cz) == null ? frame : p.get(cx, floor, cz));
            if (arch != null && width >= 3 && (d == 0 || d == depth - 1)) {
                p.set(cx + sx * half, floor + height, cz + sz * half, St.stairs(arch, side, true));
                p.set(cx - sx * half, floor + height, cz - sz * half, St.stairs(arch, St.opposite(side), true));
            }
        }
        if (door != null) {
            p.set(x, floor + 1, z, door[0]);
            p.set(x, floor + 2, z, door[1]);
        }
    }

    // --- stairs ------------------------------------------------------------------------------------------------------------

    /**
     * A straight flight going {@code down} from a landing whose floor block is at {@code topFloor}: its first step is at
     * {@code (x, z)} in that floor's plane (y = {@code topFloor}), each next one a block on and a block down, {@code steps} of
     * them, {@code width} wide toward {@code across}. The lower floor is then at {@code topFloor - steps}, one block past the last
     * step. Headroom (air) of 3 over every step. {@code underside}: stairs id for a smooth sloping soffit (upside-down stairs
     * under each step but the last, which stands on the floor), or null to leave what is below.
     */
    public static void flight(Plan p, int x, int z, int topFloor, String down, String across, int width, int steps, String stairs, String underside) {
        int dx = St.dx(down), dz = St.dz(down), ax = St.dx(across), az = St.dz(across);
        for (int i = 0; i < steps; i++) {
            int y = topFloor - i;
            for (int w = 0; w < width; w++) {
                int cx = x + dx * i + ax * w, cz = z + dz * i + az * w;
                p.set(cx, y, cz, St.stairs(stairs, St.opposite(down), false));
                for (int h = 1; h <= 3; h++) p.set(cx, y + h, cz, St.AIR);
                if (underside != null && i < steps - 1) p.set(cx, y - 1, cz, St.stairs(underside, down, true));
            }
        }
    }

    /** Thin chain balusters standing on each step of one lane of a {@link #flight} (same {@code x, z, topFloor, down, steps}). */
    public static void balusters(Plan p, int x, int z, int topFloor, String down, int steps) {
        for (int i = 0; i < steps; i++) p.set(x + St.dx(down) * i, topFloor - i + 1, z + St.dz(down) * i, St.chain("y"));
    }

    // --- railings and columns ----------------------------------------------------------------------------------------------

    /**
     * The art-deco railing on a run of cells at height {@code y}: iron bars, with a {@code post} (a wall block) at each end and
     * every {@code every} cells. The run is a list of (x, z) in order.
     */
    public static void railing(Plan p, int y, List<int[]> run, String post, int every) {
        for (int i = 0; i < run.size(); i++) {
            int[] c = run.get(i);
            boolean isPost = i == 0 || i == run.size() - 1 || every > 0 && i % every == 0;
            p.set(c[0], y, c[1], isPost ? St.wall(post) : St.bars());
        }
    }

    /**
     * A column from {@code y0} to {@code y1}: {@code shaft} (an axis block, upright), a {@code base} and {@code capital} block, and
     * upside-down {@code flare} stairs hugging the capital where there is air round it.
     */
    public static void column(Plan p, int x, int z, int y0, int y1, String shaft, String base, String capital, String flare) {
        for (int y = y0; y <= y1; y++) p.set(x, y, z, y == y0 && base != null ? base : y == y1 && capital != null ? capital : St.axis(shaft, "y"));
        if (flare == null) return;
        for (String d : St.HORIZONTAL) {
            int cx = x + St.dx(d), cz = z + St.dz(d);
            if (p.isAir(cx, y1, cz)) p.set(cx, y1, cz, St.stairs(flare, St.opposite(d), true));
        }
    }

    // --- lights ---------------------------------------------------------------------------------------------------------------

    /** A lamp set in a wall (the wall block becomes {@code light}) behind an open iron trapdoor: a lit louvre. */
    public static void wallLight(Plan p, int x, int y, int z, String out, String light) {
        p.set(x, y, z, light);
        p.set(x + St.dx(out), y, z + St.dz(out), St.trapdoor("minecraft:iron_trapdoor", out, false, true));
    }

    /** A sconce in front of a wall: a closed trapdoor shelf at {@code y} (top half) with a lantern standing on it. */
    public static void sconce(Plan p, int x, int y, int z, String trapdoor, boolean soul) {
        p.set(x, y, z, St.trapdoor(trapdoor, "north", true, false));
        p.set(x, y + 1, z, soul ? St.soulLantern(false) : St.lantern(false));
    }

    /** A lamp hanging {@code drop} blocks under a ceiling block at {@code ceiling}: chains, then a lantern. */
    public static void pendant(Plan p, int x, int ceiling, int z, int drop, boolean soul) {
        for (int y = ceiling - 1; y > ceiling - drop; y--) p.set(x, y, z, St.chain("y"));
        p.set(x, ceiling - drop, z, soul ? St.soulLantern(true) : St.lantern(true));
    }

    // --- furniture ------------------------------------------------------------------------------------------------------------

    /** A chair whose sitter looks {@code looks}: a stair seat, and trapdoor arms on its sides where there is room. */
    public static void chair(Plan p, int x, int y, int z, String looks, String stairs, String arms) {
        p.set(x, y, z, St.stairs(stairs, St.opposite(looks), false));
        if (arms == null) return;
        for (String side : new String[]{St.cw(looks), St.ccw(looks)}) {
            int ax = x + St.dx(side), az = z + St.dz(side);
            if (p.isAir(ax, y, az)) p.set(ax, y, az, St.trapdoor(arms, side, false, true));
        }
    }

    /** A table top (slabs, upper half) over a rectangle at {@code y}. */
    public static void table(Plan p, int x0, int x1, int y, int z0, int z1, String slab) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) p.set(x, y, z, St.slab(slab, true));
    }

    /** A wall banner hung on the wall behind it ({@code facing} out of the wall) with its pattern layers (see {@link Decor#banner}). */
    public static void banner(Plan p, int x, int y, int z, String facing, String color, String layers) {
        p.set(x, y, z, St.of(color + "_wall_banner", "facing", facing));
        p.decor(Decor.banner(x, y, z, color, layers));
    }

    /** A wall sign with its text (lines joined by the builder). */
    public static void sign(Plan p, int x, int y, int z, String facing, String wood, String color, String... lines) {
        p.set(x, y, z, St.of(wood + "_wall_sign", "facing", facing, "waterlogged", "false"));
        p.decor(Decor.sign(x, y, z, color, lines));
    }

    /** A container (barrel, chest…) already planned, filled from a loot table when the bunker is built. */
    public static void loot(Plan p, int x, int y, int z, String state, String table) {
        p.set(x, y, z, state);
        p.decor(Decor.loot(x, y, z, table));
    }

    /** The order's banner: black, the emblem in gold, a gold border. */
    public static final String ORDER_BANNER = "supernaturalcraft:men_of_letters:yellow,minecraft:border:yellow";
}
