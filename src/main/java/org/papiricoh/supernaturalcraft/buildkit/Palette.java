package org.papiricoh.supernaturalcraft.buildkit;

import java.util.ArrayList;
import java.util.List;

/**
 * Surfaces that read as built, not as a fill (pure). A modern build never paints a wall with one block: it mixes 2–4 close blocks,
 * grades them (darker and rougher toward the ground, lighter up high) and weathers them where water and time would. This class
 * makes those {@link Brush}es, and holds the named schemes of the mod's scenarios.
 *
 * <ul>
 *   <li>{@link #mix}: weighted salt-and-pepper (small speckle: gravel, petals, a planks floor's odd board);</li>
 *   <li>{@link #patches}: weighted, but in clumps {@code scale} blocks across (moss, cracked bricks, worn floor);</li>
 *   <li>{@link #gradient}: bands along a {@link Field} (height, distance…), boundaries dithered with noise;</li>
 *   <li>{@link #weathered}: a chain from pristine to ruined, each cell as far along it as a wear field and noise say;</li>
 *   <li>{@link #courses}: horizontal courses (a plinth band, a string course every n rows).</li>
 * </ul>
 */
public final class Palette {

    private Palette() {
    }

    /** A scalar over space, usually in [0, 1]. */
    @FunctionalInterface
    public interface Field {
        double at(int x, int y, int z);

        /** 0 at {@code y0}, 1 at {@code y1}, clamped. */
        static Field height(int y0, int y1) {
            return (x, y, z) -> Math.max(0, Math.min(1, (y - y0) / (double) Math.max(1, y1 - y0)));
        }

        /** 1 at {@code y0} falling to 0 at {@code y0 + span}: damp near the ground. */
        static Field nearGround(int y0, int span) {
            return (x, y, z) -> Math.max(0, Math.min(1, 1 - (y - y0) / (double) Math.max(1, span)));
        }

        /** Smooth noise in [0, 1). */
        static Field noise(double scale, int seed) {
            return (x, y, z) -> Noise.patch3(x, y, z, scale, seed);
        }

        static Field constant(double v) {
            return (x, y, z) -> v;
        }

        default Field plus(Field o, double weight) {
            return (x, y, z) -> at(x, y, z) + weight * o.at(x, y, z);
        }

        default Field times(double k) {
            return (x, y, z) -> at(x, y, z) * k;
        }
    }

    // --- mixes ------------------------------------------------------------------------------------------------------------

    /** Weighted choice per cell: {@code state-or-brush, weight, state-or-brush, weight…}. */
    public static Brush mix(int seed, Object... stateAndWeight) {
        Weighted w = new Weighted(stateAndWeight);
        return (x, y, z) -> w.pick(Noise.hash01(x, y, z, seed), x, y, z);
    }

    /** Weighted choice in clumps about {@code scale} blocks across (the weights are the share of cells each covers). */
    public static Brush patches(int seed, double scale, Object... stateAndWeight) {
        Weighted w = new Weighted(stateAndWeight);
        return (x, y, z) -> {
            double u = Noise.patch3(x, y, z, scale, seed) + (Noise.hash01(x, y, z, seed + 1) - 0.5) * 0.06;
            return w.pick(Math.max(0, Math.min(0.999999, u)), x, y, z);
        };
    }

    /** Weighted choice in clumps over columns only (floors and ground: the same pick all the way down a column). */
    public static Brush patches2(int seed, double scale, Object... stateAndWeight) {
        Weighted w = new Weighted(stateAndWeight);
        return (x, y, z) -> {
            double u = Noise.patch2(x, z, scale, seed) + (Noise.hash01(x, 0, z, seed + 1) - 0.5) * 0.06;
            return w.pick(Math.max(0, Math.min(0.999999, u)), x, y, z);
        };
    }

    private static final class Weighted {
        final List<Brush> brushes = new ArrayList<>();
        final double[] cumulative;

        Weighted(Object... stateAndWeight) {
            if (stateAndWeight.length < 2 || stateAndWeight.length % 2 != 0) throw new IllegalArgumentException("state, weight pairs");
            cumulative = new double[stateAndWeight.length / 2];
            double total = 0;
            for (int i = 0; i < stateAndWeight.length; i += 2) {
                Object o = stateAndWeight[i];
                brushes.add(o instanceof Brush b ? b : Brush.of((String) o));
                total += ((Number) stateAndWeight[i + 1]).doubleValue();
                cumulative[i / 2] = total;
            }
            for (int i = 0; i < cumulative.length; i++) cumulative[i] /= total;
        }

        String pick(double u, int x, int y, int z) {
            for (int i = 0; i < cumulative.length; i++) if (u < cumulative[i]) return brushes.get(i).at(x, y, z);
            return brushes.get(brushes.size() - 1).at(x, y, z);
        }
    }

    // --- gradients and weathering --------------------------------------------------------------------------------------------

    /**
     * Bands along {@code field} (in [0, 1]): {@code bands[0]} where it is 0, the last where it is 1; each boundary is dithered by
     * {@code dither} (a fraction of a band) of white and smooth noise mixed, so bands interlock instead of meeting on a line.
     */
    public static Brush gradient(Field field, double dither, int seed, Brush... bands) {
        int n = bands.length;
        return (x, y, z) -> {
            double t = field.at(x, y, z) * n;
            double jitter = ((Noise.hash01(x, y, z, seed) - 0.5) * 0.6 + (Noise.patch3(x, y, z, 3, seed + 7) - 0.5) * 0.4) * 2 * dither;
            int i = (int) Math.floor(t + jitter);
            return bands[Math.max(0, Math.min(n - 1, i))].at(x, y, z);
        };
    }

    /** {@link #gradient} over height: {@code bands[0]} at {@code y0}, the last at {@code y1}. */
    public static Brush gradientY(int y0, int y1, double dither, int seed, Brush... bands) {
        return gradient(Field.height(y0, y1), dither, seed, bands);
    }

    public static Brush gradientY(int y0, int y1, double dither, int seed, String... bands) {
        Brush[] b = new Brush[bands.length];
        for (int i = 0; i < bands.length; i++) b[i] = Brush.of(bands[i]);
        return gradientY(y0, y1, dither, seed, b);
    }

    /**
     * A weathering chain: {@code chain[0]} pristine … the last most ruined. Each cell goes as far along it as {@code wear}
     * (0 none – 1 full) plus smooth noise of amplitude {@code noise} say; most cells stay near the start unless wear is high.
     */
    public static Brush weathered(String[] chain, Field wear, double noise, int seed) {
        Brush[] b = new Brush[chain.length];
        for (int i = 0; i < chain.length; i++) b[i] = Brush.of(chain[i]);
        return (x, y, z) -> {
            double w = wear.at(x, y, z) + (Noise.patch3(x, y, z, 2.5, seed) - 0.5) * 2 * noise + (Noise.hash01(x, y, z, seed + 3) - 0.5) * 0.15;
            double t = Math.max(0, Math.min(0.999, w)) * chain.length;
            // Bias toward the pristine end: wear must accumulate to reach the ruined blocks.
            int i = (int) Math.floor(t * t / chain.length);
            return b[Math.max(0, Math.min(chain.length - 1, i))].at(x, y, z);
        };
    }

    /**
     * Horizontal courses by height above {@code floor}: rows below {@code plinthRows} are {@code plinth}; every
     * {@code courseEvery}-th row above is {@code course}; the rest {@code body}. Pass a null course for none.
     */
    public static Brush courses(int floor, int plinthRows, Brush plinth, Brush body, Brush course, int courseEvery) {
        return (x, y, z) -> {
            int off = y - floor;
            if (off < plinthRows) return plinth.at(x, y, z);
            if (course != null && courseEvery > 0 && (off - plinthRows) % courseEvery == courseEvery - 1) return course.at(x, y, z);
            return body.at(x, y, z);
        };
    }

    /**
     * Half-timbering for a wall plane (a gable, a jettied storey): an upright {@code timber} every {@code every} cells along the
     * plane (by {@code x + z}, so it works on planes facing either axis), {@code plaster} between. Regular on purpose: framing reads
     * as design, random speckle as noise.
     */
    public static Brush halfTimber(Brush plaster, String timber, int every) {
        String t = St.full(timber);
        return (x, y, z) -> Math.floorMod(x + z, every) == 0 ? t : plaster.at(x, y, z);
    }

    /** Squares of {@code size} of two brushes on the floor plane (checkered tiles). */
    public static Brush checker(int size, Brush a, Brush b) {
        return (x, y, z) -> ((Math.floorDiv(x, size) + Math.floorDiv(z, size)) & 1) == 0 ? a.at(x, y, z) : b.at(x, y, z);
    }

    /**
     * Floor boards along X (or Z): rows alternate between {@code a} and {@code b}, and boards {@code len} long with staggered
     * joints are now and then {@code odd} (a third tone; null for none), so no 5 × 5 patch is one block.
     */
    public static Brush boards(boolean alongX, Brush a, Brush b, Brush odd, int len) {
        return (x, y, z) -> {
            int across = alongX ? z : x, along = alongX ? x : z;
            int joint = Math.floorDiv(along + across * 3, len);
            if (odd != null && Noise.hash01(across, joint, 0, 515) < 0.15) return odd.at(x, y, z);
            return Math.floorMod(across, 2) == 0 ? a.at(x, y, z) : b.at(x, y, z);
        };
    }

    /** Two-tone boards. */
    public static Brush boards(boolean alongX, Brush a, Brush b, int len) {
        return boards(alongX, a, b, null, len);
    }

    /**
     * Weathers what is already drawn: inside {@code region}, a share of the blocks of family {@code from} (its block, stairs, slab
     * and wall) become the same shape in family {@code to}, in clumps about {@code scale} across — a roof of dark oak with
     * patches of spruce, a deepslate roof with mossy repairs, a stone wall with cobbled mends. Properties are kept.
     */
    public static int swapFamily(Canvas c, Box region, Family from, Family to, double share, double scale, int seed) {
        java.util.Map<String, String> ids = new java.util.HashMap<>();
        ids.put(from.block(), to.block());
        if (from.stairs() != null && to.stairs() != null) ids.put(from.stairs(), to.stairs());
        if (from.slab() != null && to.slab() != null) ids.put(from.slab(), to.slab());
        if (from.wall() != null && to.wall() != null) ids.put(from.wall(), to.wall());
        int n = 0;
        for (java.util.Map.Entry<Long, String> e : new java.util.ArrayList<>(c.map().entrySet())) {
            int x = Canvas.kx(e.getKey()), y = Canvas.ky(e.getKey()), z = Canvas.kz(e.getKey());
            if (!region.contains(x, y, z)) continue;
            String id = St.id(e.getValue()), swap = ids.get(id);
            if (swap == null || Noise.patch3(x, y, z, scale, seed) >= share) continue;
            c.set(x, y, z, swap + e.getValue().substring(id.length()));
            n++;
        }
        return n;
    }

    // --- weathering chains -------------------------------------------------------------------------------------------------

    public static final String[] STONE_BRICK_WEAR = {"minecraft:stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:mossy_stone_bricks",
            "minecraft:mossy_cobblestone", "minecraft:moss_block"};
    public static final String[] COBBLE_WEAR = {"minecraft:cobblestone", "minecraft:andesite", "minecraft:mossy_cobblestone", "minecraft:moss_block"};
    public static final String[] DEEPSLATE_WEAR = {"minecraft:deepslate_bricks", "minecraft:cracked_deepslate_bricks", "minecraft:cobbled_deepslate",
            "minecraft:deepslate"};
    public static final String[] OAK_WEAR = {"minecraft:oak_planks", "minecraft:stripped_oak_wood[axis=y]", "minecraft:spruce_planks",
            "minecraft:stripped_spruce_wood[axis=y]"};
    public static final String[] MARBLE_WEAR = {"minecraft:calcite", "minecraft:diorite", "minecraft:white_concrete_powder"};
    public static final String[] BRICK_WEAR = {"minecraft:bricks", "minecraft:granite", "minecraft:mud_bricks", "minecraft:packed_mud"};
    public static final String[] COPPER_WEAR = {"minecraft:waxed_cut_copper", "minecraft:waxed_exposed_cut_copper",
            "minecraft:waxed_weathered_cut_copper", "minecraft:waxed_oxidized_cut_copper"};

    // --- named schemes -----------------------------------------------------------------------------------------------------

    /**
     * A scenario's look in roles: {@code wall} the main wall surface, {@code plinth} the base course, {@code trim} mouldings
     * (sills, belts, cornices), {@code accent} a contrasting detail block, {@code floor} interior floors, {@code roof} the roof
     * family, {@code wood} posts, beams and furniture, {@code glass} the window pane id, {@code light} the hidden light block,
     * {@code ground} outdoor ground near the build.
     */
    public record Scheme(String name, Brush wall, Brush plinth, Family trim, Brush accent, Brush floor, Family roof, Wood wood,
                         String glass, String light, Brush ground) {
    }

    /** Heaven's marble: calcite grading up into diorite and white powder, quartz trim, birch, pearl light. */
    public static final Scheme HEAVEN_MARBLE = new Scheme("heaven_marble",
            gradientY(0, 12, 0.35, 11, Brush.of("minecraft:calcite"), patches(12, 3, "minecraft:calcite", 3, "minecraft:diorite", 2),
                    patches(13, 4, "minecraft:diorite", 2, "minecraft:white_concrete_powder", 1, "minecraft:calcite", 2)),
            patches(14, 3, "minecraft:polished_diorite", 5, "minecraft:quartz_bricks", 3, "minecraft:smooth_quartz", 2),
            Family.SMOOTH_QUARTZ,
            mix(15, "minecraft:chiseled_quartz_block", 3, "minecraft:quartz_pillar[axis=y]", 2),
            checker(2, patches(16, 3, "minecraft:polished_diorite", 4, "minecraft:calcite", 1), patches(17, 3, "minecraft:smooth_quartz", 4, "minecraft:quartz_bricks", 1)),
            Family.POLISHED_DIORITE, Wood.BIRCH, "minecraft:white_stained_glass_pane", "minecraft:pearlescent_froglight",
            patches2(18, 4, "minecraft:grass_block", 6, "minecraft:moss_block", 1, "minecraft:calcite", 1));

    /** The Winchester house in Lawrence: cream clapboard over a brick plinth, dark slate roof, white trim, oak inside. */
    public static final Scheme LAWRENCE_HOUSE = new Scheme("lawrence_house",
            courses(0, 0, Brush.of("minecraft:bricks"), patches(21, 2.5, "minecraft:birch_planks", 6, "minecraft:stripped_birch_wood[axis=x]", 2,
                    "minecraft:smooth_sandstone", 1), null, 0),
            weathered(BRICK_WEAR, Field.constant(0.25), 0.3, 22),
            Family.SMOOTH_QUARTZ,
            Brush.of("minecraft:stripped_spruce_wood[axis=y]"),
            boards(true, Brush.of("minecraft:oak_planks"), Brush.of("minecraft:birch_planks"), Brush.of("minecraft:spruce_planks"), 5),
            Family.DEEPSLATE_TILES, Wood.OAK, "minecraft:glass_pane", "minecraft:ochre_froglight",
            patches2(23, 4, "minecraft:grass_block", 8, "minecraft:coarse_dirt", 1, "minecraft:podzol", 1));

    /** Harvelle's Roadhouse: weathered spruce and dark oak boards on a cobble footing, rusted tin roof, amber glass. */
    public static final Scheme ROADHOUSE = new Scheme("roadhouse",
            patches(31, 2.5, "minecraft:spruce_planks", 5, "minecraft:stripped_spruce_wood[axis=y]", 2, "minecraft:dark_oak_planks", 2,
                    "minecraft:stripped_dark_oak_wood[axis=y]", 1),
            weathered(COBBLE_WEAR, Field.constant(0.35), 0.35, 32),
            Wood.SPRUCE.family(),
            Brush.of("minecraft:dark_oak_log[axis=y]"),
            boards(true, Brush.of("minecraft:dark_oak_planks"), Brush.of("minecraft:spruce_planks"), Brush.of("minecraft:stripped_dark_oak_wood[axis=x]"), 4),
            Family.WEATHERED_CUT_COPPER, Wood.SPRUCE, "minecraft:orange_stained_glass_pane", "minecraft:ochre_froglight",
            patches2(33, 3, "minecraft:coarse_dirt", 4, "minecraft:dirt", 2, "minecraft:gravel", 1, "minecraft:packed_mud", 1));

    /** Naomi's clinic: white concrete and smooth quartz, pale grey trim, cold light. */
    public static final Scheme CLINIC = new Scheme("clinic",
            patches(41, 3, "minecraft:white_concrete", 6, "minecraft:smooth_quartz", 3, "minecraft:quartz_bricks", 1),
            Brush.of("minecraft:light_gray_concrete"),
            Family.POLISHED_DIORITE,
            Brush.of("minecraft:light_blue_terracotta"),
            checker(1, Brush.of("minecraft:white_concrete"), Brush.of("minecraft:light_gray_concrete")),
            Family.POLISHED_ANDESITE, Wood.BIRCH, "minecraft:white_stained_glass_pane", "minecraft:sea_lantern",
            Brush.of("minecraft:smooth_stone"));

    /** Zachariah's office: beige panels, grey carpet tiles, birch and steel, fluorescent light. */
    public static final Scheme OFFICE = new Scheme("office",
            courses(0, 1, Brush.of("minecraft:stripped_birch_wood[axis=y]"),
                    patches(51, 3, "minecraft:smooth_sandstone", 5, "minecraft:white_terracotta", 2, "minecraft:birch_planks", 1),
                    Brush.of("minecraft:cut_sandstone"), 4),
            Brush.of("minecraft:polished_andesite"),
            Family.POLISHED_ANDESITE,
            Brush.of("minecraft:light_gray_terracotta"),
            patches(52, 2, "minecraft:gray_wool", 4, "minecraft:light_gray_wool", 3, "minecraft:gray_concrete_powder", 1),
            Family.SMOOTH_STONE, Wood.BIRCH, "minecraft:light_gray_stained_glass_pane", "minecraft:sea_lantern",
            Brush.of("minecraft:polished_andesite"));

    /** A dirt crossroads: packed earth, gravel and coarse dirt, ruts of path. */
    public static final Scheme DUST_ROAD = new Scheme("dust_road",
            patches(61, 3, "minecraft:coarse_dirt", 4, "minecraft:dirt", 2, "minecraft:packed_mud", 2, "minecraft:gravel", 1),
            Brush.of("minecraft:cobblestone"),
            Family.COBBLESTONE,
            Brush.of("minecraft:mossy_cobblestone"),
            patches2(62, 3, "minecraft:dirt_path", 5, "minecraft:coarse_dirt", 2, "minecraft:gravel", 1),
            Wood.SPRUCE.family(), Wood.SPRUCE, "minecraft:glass_pane", "minecraft:ochre_froglight",
            patches2(63, 3, "minecraft:coarse_dirt", 4, "minecraft:dirt", 2, "minecraft:packed_mud", 2, "minecraft:gravel", 1,
                    "minecraft:rooted_dirt", 1));

    /** Clouds: white wool and snow, shading to light grey underneath. */
    public static final Scheme CLOUD = new Scheme("cloud",
            gradientY(-6, 2, 0.4, 71, Brush.of("minecraft:light_gray_wool"), patches(72, 3, "minecraft:white_wool", 3, "minecraft:white_concrete_powder", 1),
                    patches(73, 3, "minecraft:snow_block", 2, "minecraft:white_wool", 3)),
            Brush.of("minecraft:white_wool"),
            Family.QUARTZ,
            Brush.of("minecraft:snow_block"),
            patches(74, 3, "minecraft:snow_block", 2, "minecraft:white_wool", 3, "minecraft:white_concrete_powder", 1),
            Family.QUARTZ, Wood.BIRCH, "minecraft:white_stained_glass_pane", "minecraft:pearlescent_froglight",
            patches(75, 3, "minecraft:snow_block", 2, "minecraft:white_wool", 3, "minecraft:white_concrete_powder", 1));
}
