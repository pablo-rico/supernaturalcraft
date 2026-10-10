package org.papiricoh.supernaturalcraft.buildkit;

/**
 * Vegetation spread over a {@link Ground} (pure) with rules a gardener would follow: plants only on soil with room above
 * (two cells for tall plants), never on water or steep steps, thicker in drifts (smooth noise) than in a uniform sprinkle,
 * moss carpet only on moss, lily pads only on water. Plants are given as weighted ids ({@code id, weight, id, weight…}); the
 * ids name the plant, the right state is chosen here: tall plants get both halves, pink petals a count and a turn, sweet berry
 * bushes ripe fruit.
 */
public final class Scatter {

    private Scatter() {
    }

    /** The tall plants (two cells). */
    public static boolean tall(String id) {
        String p = St.path(id);
        return p.equals("tall_grass") || p.equals("large_fern") || p.equals("lilac") || p.equals("rose_bush") || p.equals("peony")
                || p.equals("sunflower") || p.equals("pitcher_plant");
    }

    /** A weighted pick (white noise) from {@code id, weight…}. */
    public static String pick(int x, int z, int seed, Object... weighted) {
        double total = 0;
        for (int i = 1; i < weighted.length; i += 2) total += ((Number) weighted[i]).doubleValue();
        double r = Noise.hash01(x, 7, z, seed) * total;
        for (int i = 0; i + 1 < weighted.length; i += 2) {
            r -= ((Number) weighted[i + 1]).doubleValue();
            if (r < 0) return St.full((String) weighted[i]);
        }
        return St.full((String) weighted[weighted.length - 2]);
    }

    /** Plants {@code id} at (x, y, z) (the cell above the soil) if there is room; returns whether it did. */
    public static boolean plant(Canvas c, int x, int y, int z, String id, int seed) {
        id = St.full(id);
        String p = St.path(id);
        if (!c.isAir(x, y, z)) return false;
        if (tall(id)) {
            if (!c.isAir(x, y + 1, z)) return false;
            c.setPair(x, y, z, St.tall(id));
            return true;
        }
        String state = switch (p) {
            case "pink_petals" -> St.petals(1 + Noise.pick(x, y, z, seed + 1, 4), Dir.HORIZONTAL[Noise.pick(x, y, z, seed + 2, 4)]);
            case "sweet_berry_bush" -> St.crop(id, 2 + Noise.pick(x, y, z, seed + 3, 2));
            case "sugar_cane" -> St.crop(id, 0);
            default -> id;
        };
        c.set(x, y, z, state);
        return true;
    }

    /**
     * Spreads plants over every ground column inside {@code area} (null: all): {@code density} is the share of open soil columns
     * planted where the drift noise is strongest (about half that on average), {@code maxSlope} skips steeper cells.
     */
    public static int flora(Canvas c, Ground g, Box area, double density, int maxSlope, int seed, Object... weighted) {
        int n = 0;
        for (int[] col : g.columns()) {
            int x = col[0], z = col[1];
            if (area != null && !area.containsColumn(x, z)) continue;
            if (g.water(x, z) || g.slope(x, z) > maxSlope) continue;
            int t = g.top(x, z);
            String soil = c.get(x, t, z);
            if (soil == null || !Kinds.soil(soil) || St.path(soil).equals("gravel") || St.path(soil).equals("clay") || St.path(soil).startsWith("sand")) continue;
            double drift = Noise.patch2(x, z, 6, seed);
            if (Noise.hash01(x, 1, z, seed + 1) >= density * (0.25 + 1.2 * drift)) continue;
            String id = pick(x, z, seed + 2, weighted);
            if (St.path(id).equals("moss_carpet")) {
                if (!St.path(soil).equals("moss_block")) continue;
                if (c.isAir(x, t + 1, z)) c.set(x, t + 1, z, St.mossCarpet());
                n++;
                continue;
            }
            if (plant(c, x, t + 1, z, id, seed)) n++;
        }
        return n;
    }

    /** Lily pads on a share of the water columns inside {@code area} (null: all). */
    public static void lilies(Canvas c, Ground g, Box area, double share, int seed) {
        for (int[] col : g.columns()) {
            int x = col[0], z = col[1];
            if (!g.water(x, z) || area != null && !area.containsColumn(x, z)) continue;
            for (int y = g.top(x, z) + 1; y < g.top(x, z) + 6; y++) {
                if (Kinds.water(c.get(x, y, z)) && c.isAir(x, y + 1, z)) {
                    if (Noise.chance(x, y, z, seed, share)) c.set(x, y + 1, z, "minecraft:lily_pad");
                    break;
                }
            }
        }
    }

    /**
     * Removes plants that lost what holds them (run it last, after everything that replaces ground): small and tall plants not
     * on soil (both halves), crops off farmland, lily pads off water, sugar cane off soil by water, vines on nothing, lone halves
     * of tall plants. Returns how many cells it cleared.
     */
    public static int prune(Canvas c) {
        int n = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (java.util.Map.Entry<Long, String> e : new java.util.ArrayList<>(c.map().entrySet())) {
                String s = e.getValue(), p = St.path(s);
                int x = Canvas.kx(e.getKey()), y = Canvas.ky(e.getKey()), z = Canvas.kz(e.getKey());
                String below = c.world(x, y - 1, z), above = c.world(x, y + 1, z);
                boolean drop = false;
                if (Kinds.soilPlant(s)) drop = !Kinds.soil(below) && !St.path(below).equals("farmland");
                else if (tall(St.id(s)) && "upper".equals(St.get(s, "half"))) drop = !St.id(below).equals(St.id(s));
                else if (p.equals("lily_pad")) drop = !Kinds.water(below);
                else if (p.equals("wheat") || p.equals("carrots") || p.equals("potatoes") || p.equals("beetroots")) drop = !St.path(below).equals("farmland");
                else if (p.equals("sugar_cane") && !St.path(below).equals("sugar_cane")) {
                    boolean wet = false;
                    for (Dir d : Dir.HORIZONTAL) wet |= Kinds.water(c.world(x + d.dx, y - 1, z + d.dz));
                    drop = !Kinds.soil(below) || !wet;
                } else if (p.equals("vine")) {
                    boolean any = "true".equals(St.get(s, "up")) && Kinds.sturdy(above, Dir.DOWN);
                    for (Dir d : Dir.HORIZONTAL) if ("true".equals(St.get(s, d.id()))) any |= Kinds.sturdy(c.world(x + d.dx, y, z + d.dz), d.opposite());
                    drop = !any;
                }
                if (tall(St.id(s)) && "lower".equals(St.get(s, "half")) && !St.id(above).equals(St.id(s))) drop = true;
                if (drop) {
                    c.carve(x, y, z);
                    n++;
                    changed = true;
                }
            }
        }
        return n;
    }

    /** A meadow mix: grasses, ferns and wildflowers. */
    public static final Object[] MEADOW = {"minecraft:short_grass", 30, "minecraft:tall_grass", 8, "minecraft:fern", 5,
            "minecraft:large_fern", 2, "minecraft:poppy", 2, "minecraft:dandelion", 2, "minecraft:oxeye_daisy", 3,
            "minecraft:azure_bluet", 3, "minecraft:cornflower", 2, "minecraft:allium", 1, "minecraft:lily_of_the_valley", 1,
            "minecraft:pink_petals", 2, "minecraft:moss_carpet", 3};

    /** A garden bed: tall and small flowers, few grasses. */
    public static final Object[] GARDEN = {"minecraft:rose_bush", 2, "minecraft:peony", 2, "minecraft:lilac", 2, "minecraft:allium", 3,
            "minecraft:red_tulip", 2, "minecraft:white_tulip", 2, "minecraft:pink_tulip", 2, "minecraft:cornflower", 2,
            "minecraft:oxeye_daisy", 2, "minecraft:short_grass", 4, "minecraft:fern", 2};

    /** A woodland floor: ferns, moss, berries, mushrooms (only where shaded). */
    public static final Object[] WOODLAND = {"minecraft:fern", 8, "minecraft:large_fern", 3, "minecraft:short_grass", 6,
            "minecraft:moss_carpet", 6, "minecraft:sweet_berry_bush", 1, "minecraft:lily_of_the_valley", 1};
}
