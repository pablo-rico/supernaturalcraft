package org.papiricoh.supernaturalcraft.buildkit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The checks every layout test runs (test helper, generalised from {@code BunkerLayoutTest}). Each method takes a finished
 * {@link Canvas} and fails with a short list of offending cells. What lies off the canvas is the canvas's {@link Outside}.
 *
 * <pre>{@code
 * Canvas c = MyLayout.canvas();                       // finished
 * LayoutQuality.assertIdsKnown(c, BlockIds.vanilla().allow("supernaturalcraft:angel_statue"));
 * LayoutQuality.assertBounds(c, new Box(-40, -30, -40, 40, 40, 40));
 * LayoutQuality.assertBudget(c, 60_000);
 * LayoutQuality.assertShapes(c);
 * LayoutQuality.assertSupported(c);
 * LayoutQuality.assertHeadroom(c, c.anchor("entry"));
 * LayoutQuality.assertReachable(c, c.anchor("entry"), c.anchors().values());
 * LayoutQuality.assertNoDarkInteriors(c, 8);
 * LayoutQuality.assertPalette(c, 40);
 * LayoutQuality.assertSurfaceVariety(c, LayoutQuality.NATURAL);
 * LayoutQuality.assertDepth(c, c.zone("house").bounds());
 * LayoutQuality.assertWaterContained(c);
 * LayoutQuality.assertNoBlockEntities(c);              // only for layouts written through an arena
 * }</pre>
 */
public final class LayoutQuality {

    /** Natural ground left out of {@link #assertSurfaceVariety}: terrain is varied by its own noise, and a lawn may be all grass. */
    public static final Set<String> NATURAL = Set.of("minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol",
            "minecraft:moss_block", "minecraft:rooted_dirt", "minecraft:stone", "minecraft:andesite", "minecraft:calcite",
            "minecraft:diorite", "minecraft:tuff", "minecraft:dripstone_block", "minecraft:sand", "minecraft:gravel", "minecraft:clay",
            "minecraft:mud", "minecraft:dirt_path", "minecraft:farmland", "minecraft:snow_block", "minecraft:packed_mud",
            "minecraft:deepslate", "minecraft:granite");

    private LayoutQuality() {
    }

    private static String at(Canvas c, int x, int y, int z) {
        return c.world(x, y, z);
    }

    private static String pos(long k) {
        return Canvas.kx(k) + "," + Canvas.ky(k) + "," + Canvas.kz(k);
    }

    private static void report(String what, List<String> bad, int allowed) {
        if (bad.size() > allowed) fail(bad.size() + " " + what + ", e.g. " + bad.subList(0, Math.min(12, bad.size())));
    }

    // --- ids, bounds, budget ---------------------------------------------------------------------------------------------------

    /** Every state names a known block with known properties and values (the game would read it as written). */
    public static void assertIdsKnown(Canvas c, BlockIds ids) {
        Set<String> bad = new TreeSet<>();
        for (String s : new HashSet<>(c.map().values())) {
            String why = ids.check(s);
            if (why != null) bad.add(why);
        }
        for (Decor d : c.decor()) {
            if (d.kind() == Decor.Kind.BLOCK_DISPLAY) {
                String why = ids.check(d.data());
                if (why != null) bad.add("block display: " + why);
            }
        }
        assertTrue(bad.isEmpty(), bad.size() + " unreadable states: " + bad);
    }

    public static void assertBounds(Canvas c, Box limit) {
        Box b = c.bounds();
        assertTrue(limit.contains(b.x0(), b.y0(), b.z0()) && limit.contains(b.x1(), b.y1(), b.z1()), "layout " + b + " outside " + limit);
    }

    /** At most {@code maxCells} planned cells (air included: carving costs writes too). */
    public static void assertBudget(Canvas c, int maxCells) {
        assertTrue(c.size() <= maxCells, c.size() + " cells, budget " + maxCells);
    }

    /** No block entities (a layout written through {@code ArenaController.mutate} cannot restore them), and no decor. */
    public static void assertNoBlockEntities(Canvas c) {
        List<String> bad = new ArrayList<>();
        for (Map.Entry<Long, String> e : c.map().entrySet()) if (Kinds.blockEntity(e.getValue())) bad.add(pos(e.getKey()) + " " + St.path(e.getValue()));
        report("block entities", bad, 0);
        assertTrue(c.decor().isEmpty(), "decor needs a permanent writer: " + c.decor().size());
    }

    // --- shapes ----------------------------------------------------------------------------------------------------------------

    /** Stairs carry facing and half; every stair, fence, wall, pane and bar is shaped as {@link Shapes} (vanilla) would shape it. */
    public static void assertShapes(Canvas c) {
        Canvas again = c.copy(c.name() + "_reshaped");
        Shapes.apply(again);
        List<String> bad = new ArrayList<>();
        for (Map.Entry<Long, String> e : c.map().entrySet()) {
            String s = e.getValue();
            if (Kinds.stairs(s) && (St.get(s, "facing") == null || St.get(s, "half") == null)) bad.add(pos(e.getKey()) + " stairs without facing/half");
            String want = again.get(Canvas.kx(e.getKey()), Canvas.ky(e.getKey()), Canvas.kz(e.getKey()));
            if (!s.equals(want)) bad.add(pos(e.getKey()) + " " + s + " → " + want);
        }
        report("mis-shaped blocks (call canvas.finish())", bad, 0);
    }

    // --- support ---------------------------------------------------------------------------------------------------------------

    /**
     * Nothing hangs on air that vanilla would drop: lanterns hang or stand, torches/signs/banners/ladders have a wall, carpets,
     * candles, plates and pots stand on something, both halves of doors, beds and tall plants are there, small plants grow on
     * soil, lily pads on water, sugar cane by water, vines and pointed dripstone are attached.
     */
    public static void assertSupported(Canvas c) {
        List<String> loose = new ArrayList<>();
        for (Map.Entry<Long, String> e : c.map().entrySet()) {
            String s = e.getValue(), p = St.path(s);
            int x = Canvas.kx(e.getKey()), y = Canvas.ky(e.getKey()), z = Canvas.kz(e.getKey());
            String below = at(c, x, y - 1, z), above = at(c, x, y + 1, z);
            String why = null;
            if ((p.endsWith("lantern") && !p.equals("sea_lantern") && !p.equals("jack_o_lantern"))) {
                boolean hanging = "true".equals(St.get(s, "hanging"));
                if (hanging ? !Kinds.holdsBelow(above) : !Kinds.holdsAbove(below)) why = "lantern";
            } else if (p.endsWith("wall_torch") || p.endsWith("_wall_sign") || p.endsWith("_wall_banner") || p.equals("ladder")) {
                Dir f = Dir.of(St.get(s, "facing"));
                String behind = at(c, x - f.dx, y, z - f.dz);
                if (!Kinds.sturdy(behind, f)) why = "nothing behind";
            } else if (p.endsWith("_hanging_sign")) {
                if (!Kinds.holdsBelow(above) && !Kinds.fence(above) && !St.path(above).endsWith("_hanging_sign")) why = "hanging sign on air";
            } else if (p.endsWith("_carpet") || p.endsWith("candle") || p.endsWith("_pressure_plate") || p.equals("flower_pot")
                    || p.startsWith("potted_") || p.equals("torch") || p.equals("soul_torch") || p.equals("redstone_torch")) {
                if (Kinds.air(below)) why = "nothing under";
                else if (!p.endsWith("_carpet") && !Kinds.holdsAbove(below) && !Kinds.stairs(below)) why = "no sturdy top under";
            } else if (p.endsWith("_door")) {
                boolean lower = "lower".equals(St.get(s, "half"));
                String other = at(c, x, lower ? y + 1 : y - 1, z);
                if (!St.id(other).equals(St.id(s)) || !(lower ? "upper" : "lower").equals(St.get(other, "half"))) why = "half a door";
                else if (lower && !Kinds.sturdy(below, Dir.UP)) why = "door on air";
            } else if (p.endsWith("_bed")) {
                Dir f = Dir.of(St.get(s, "facing"));
                int sgn = "foot".equals(St.get(s, "part")) ? 1 : -1;
                String other = at(c, x + sgn * f.dx, y, z + sgn * f.dz);
                if (!St.id(other).equals(St.id(s)) || St.get(other, "part").equals(St.get(s, "part"))) why = "half a bed";
            } else if (Kinds.twoHigh(s) && Scatter.tall(St.id(s))) {
                boolean lower = "lower".equals(St.get(s, "half"));
                String other = at(c, x, lower ? y + 1 : y - 1, z);
                if (!St.id(other).equals(St.id(s))) why = "half a tall plant";
                else if (lower && !Kinds.soil(below)) why = "tall plant off soil";
            } else if (p.equals("lily_pad")) {
                if (!Kinds.water(below)) why = "lily pad off water";
            } else if (p.equals("sugar_cane")) {
                if (!p.equals(St.path(below))) {
                    boolean wet = false;
                    for (Dir d : Dir.HORIZONTAL) wet |= Kinds.water(at(c, x + d.dx, y - 1, z + d.dz));
                    if (!Kinds.soil(below) || !wet) why = "sugar cane without soil by water";
                }
            } else if (Kinds.soilPlant(s)) {
                if (!Kinds.soil(below) && !St.path(below).equals("farmland")) why = "plant off soil (" + St.path(below) + ")";
            } else if (p.equals("wheat") || p.equals("carrots") || p.equals("potatoes") || p.equals("beetroots")) {
                if (!St.path(below).equals("farmland")) why = "crop off farmland";
            } else if (p.equals("vine")) {
                boolean any = "true".equals(St.get(s, "up")) && Kinds.sturdy(above, Dir.DOWN);
                for (Dir d : Dir.HORIZONTAL) if ("true".equals(St.get(s, d.id()))) any |= Kinds.sturdy(at(c, x + d.dx, y, z + d.dz), d.opposite());
                if (!any) why = "vine on nothing";
            } else if (p.equals("pointed_dripstone")) {
                boolean down = "down".equals(St.get(s, "vertical_direction"));
                String hold = down ? above : below;
                boolean ok = St.path(hold).equals("pointed_dripstone") || Kinds.sturdy(hold, down ? Dir.DOWN : Dir.UP);
                if (!ok) why = "dripstone on air";
            } else if (p.equals("hanging_roots")) {
                if (!Kinds.sturdy(above, Dir.DOWN)) why = "roots on air";
            } else if (p.equals("moss_carpet") || p.equals("snow")) {
                if (Kinds.air(below)) why = "nothing under";
            } else if (p.endsWith("_button") || p.equals("lever")) {
                String face = St.get(s, "face");
                Dir f = Dir.of(St.get(s, "facing"));
                String hold = "floor".equals(face) ? below : "ceiling".equals(face) ? above : at(c, x - f.dx, y, z - f.dz);
                if (Kinds.air(hold)) why = "button on air";
            }
            if (why != null) loose.add(x + "," + y + "," + z + " " + p + ": " + why);
        }
        report("unsupported blocks", loose, 0);
    }

    // --- walking ---------------------------------------------------------------------------------------------------------------

    /**
     * Every walkable step (a bottom stair in a flight that a walker from {@code entry} can stand on) has 3 passable cells over
     * it. Roof courses and other stairs nobody stands on are not steps.
     */
    public static void assertHeadroom(Canvas c, int[] entry) {
        Set<Long> reach = walk(c, entry);
        List<String> low = new ArrayList<>();
        for (Map.Entry<Long, String> e : c.map().entrySet()) {
            String s = e.getValue();
            if (!Kinds.stairs(s) || !"bottom".equals(St.get(s, "half"))) continue;
            int x = Canvas.kx(e.getKey()), y = Canvas.ky(e.getKey()), z = Canvas.kz(e.getKey());
            Dir f = Dir.of(St.get(s, "facing"));
            String next = c.get(x + f.dx, y + 1, z + f.dz), prev = c.get(x - f.dx, y - 1, z - f.dz);
            boolean flight = next != null && Kinds.stairs(next) && f.id().equals(St.get(next, "facing")) && "bottom".equals(St.get(next, "half"))
                    || prev != null && Kinds.stairs(prev) && f.id().equals(St.get(prev, "facing")) && "bottom".equals(St.get(prev, "half"));
            if (!flight || !reach.contains(Canvas.key(x, y + 1, z))) continue;
            for (int h = 1; h <= 3; h++) {
                if (!Kinds.passable(at(c, x, y + h, z))) {
                    low.add(x + "," + y + "," + z + " +" + h + " " + St.path(at(c, x, y + h, z)));
                    break;
                }
            }
        }
        report("steps without 3 blocks of headroom", low, 0);
    }

    /** Whether a walker can stand in (x, y, z): its feet and head pass, something it can stand on is under it. */
    public static boolean standable(Canvas c, int x, int y, int z) {
        return Kinds.passable(at(c, x, y, z)) && Kinds.passable(at(c, x, y + 1, z)) && Kinds.floor(at(c, x, y - 1, z))
                && !St.path(at(c, x, y, z)).equals("water");
    }

    /** Every cell a walker reaches from {@code from} (feet cells), stepping up or down at most one block. */
    public static Set<Long> walk(Canvas c, int[] from) {
        Box b = c.bounds().grow(2);
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        if (!standable(c, from[0], from[1], from[2])) fail("cannot stand at the entry " + from[0] + "," + from[1] + "," + from[2] + ": "
                + at(c, from[0], from[1], from[2]) + " over " + at(c, from[0], from[1] - 1, from[2]));
        queue.add(from);
        seen.add(Canvas.key(from[0], from[1], from[2]));
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (Dir d : Dir.HORIZONTAL) {
                for (int dy = -1; dy <= 1; dy++) {
                    int qx = p[0] + d.dx, qy = p[1] + dy, qz = p[2] + d.dz;
                    if (!b.contains(qx, qy, qz) || !standable(c, qx, qy, qz)) continue;
                    if (dy == 1 && !Kinds.passable(at(c, p[0], p[1] + 2, p[2]))) continue;
                    if (dy == -1 && !Kinds.passable(at(c, qx, qy + 2, qz))) continue;
                    if (seen.add(Canvas.key(qx, qy, qz))) queue.add(new int[]{qx, qy, qz});
                }
            }
        }
        return seen;
    }

    /**
     * Every target can be walked to from {@code entry}: its own cell or one beside it (furniture, a bed's foot) or one below it
     * (a seat) is reachable.
     */
    public static Set<Long> assertReachable(Canvas c, int[] entry, Iterable<int[]> targets) {
        Set<Long> reach = walk(c, entry);
        List<String> lost = new ArrayList<>();
        for (int[] t : targets) {
            boolean ok = reach.contains(Canvas.key(t[0], t[1], t[2]));
            for (Dir d : Dir.HORIZONTAL) for (int dy = -1; dy <= 0; dy++) ok |= reach.contains(Canvas.key(t[0] + d.dx, t[1] + dy, t[2] + d.dz));
            if (!ok) lost.add(t[0] + "," + t[1] + "," + t[2]);
        }
        report("targets out of reach from " + entry[0] + "," + entry[1] + "," + entry[2], lost, 0);
        return reach;
    }

    // --- light ---------------------------------------------------------------------------------------------------------------

    /**
     * Whether a cell is inside: roofed (an opaque block, slab or stair within 12 above) and walled — an enclosing block (an
     * opaque cube that is not a log, glass or a pane, a door) within 6 cells in at least 3 of the 4 horizontal directions. Porches
     * behind railings, the strip under the eaves and the ground under trees are outside.
     */
    public static boolean interior(Canvas c, int x, int y, int z) {
        if (!Light.covered(c, x, y, z, 12)) return false;
        int walled = 0;
        for (Dir d : Dir.HORIZONTAL) {
            for (int k = 1; k <= 6; k++) {
                String s = at(c, x + d.dx * k, y, z + d.dz * k), s2 = at(c, x + d.dx * k, y + 1, z + d.dz * k);
                if (blocks(s) || blocks(s2)) {
                    walled++;
                    break;
                }
            }
        }
        return walled >= 3;
    }

    private static boolean blocks(String s) {
        if (Kinds.air(s)) return false;
        String p = St.path(s);
        if (NATURAL.contains(St.id(s)) || St.path(s).endsWith("cobblestone")) return false;
        return Kinds.opaqueCube(s) && !Kinds.log(s) || Kinds.fullCube(s) && !Kinds.leaves(s) || p.endsWith("_door") || Kinds.barsLike(s);
    }

    /** Every interior cell one can stand in has block light of at least {@code min} (1 stops monsters; 8+ reads as a lit room). */
    public static void assertNoDarkInteriors(Canvas c, int min) {
        Light light = Light.of(c);
        List<String> dark = new ArrayList<>();
        for (Map.Entry<Long, String> e : c.map().entrySet()) {
            int x = Canvas.kx(e.getKey()), y = Canvas.ky(e.getKey()), z = Canvas.kz(e.getKey());
            if (!Kinds.passable(e.getValue()) || !standable(c, x, y, z) || !interior(c, x, y, z)) continue;
            if (light.at(x, y, z) < min) dark.add(x + "," + y + "," + z + "=" + light.at(x, y, z));
        }
        report("dark interior cells (light < " + min + ")", dark, 0);
    }

    // --- looks -----------------------------------------------------------------------------------------------------------------

    /** At least {@code minDistinct} different blocks (by id, air left out). */
    public static void assertPalette(Canvas c, int minDistinct) {
        Set<String> ids = new TreeSet<>();
        for (String s : c.map().values()) if (!Kinds.air(s)) ids.add(St.id(s));
        assertTrue(ids.size() >= minDistinct, "only " + ids.size() + " blocks (want " + minDistinct + "): " + ids);
    }

    /**
     * No bland surfaces: on every flat run of exposed opaque faces (a wall, a floor, a ceiling; faces with air in front), no 5 × 5
     * window has more than 75 % of one block. Blocks in {@code ignore} (natural ground, usually {@link #NATURAL}) are not counted.
     */
    public static void assertSurfaceVariety(Canvas c, Set<String> ignore) {
        // Exposed faces by direction and plane: key (dir, plane) → in-plane cell (u, v) → block id.
        Map<String, Map<Long, String>> planes = new HashMap<>();
        for (Map.Entry<Long, String> e : c.map().entrySet()) {
            String s = e.getValue();
            if (!Kinds.opaqueCube(s) || Kinds.air(s) || ignore.contains(St.id(s))) continue;
            int x = Canvas.kx(e.getKey()), y = Canvas.ky(e.getKey()), z = Canvas.kz(e.getKey());
            for (Dir d : Dir.values()) {
                String n = at(c, x + d.dx, y + d.dy, z + d.dz);
                if (!Kinds.air(n)) continue;
                int plane, u, v;
                if (d.dx != 0) {
                    plane = x;
                    u = z;
                    v = y;
                } else if (d.dz != 0) {
                    plane = z;
                    u = x;
                    v = y;
                } else {
                    plane = y;
                    u = x;
                    v = z;
                }
                planes.computeIfAbsent(d + ":" + plane, k -> new HashMap<>()).put(Canvas.key(u, 0, v), St.id(s));
            }
        }
        List<String> bland = new ArrayList<>();
        for (Map.Entry<String, Map<Long, String>> pl : planes.entrySet()) {
            Map<Long, String> m = pl.getValue();
            if (m.size() < 25) continue;
            for (long k : m.keySet()) {
                int u0 = Canvas.kx(k), v0 = Canvas.kz(k);
                Map<String, Integer> count = new HashMap<>();
                boolean full = true;
                for (int du = 0; du < 5 && full; du++) {
                    for (int dv = 0; dv < 5; dv++) {
                        String id = m.get(Canvas.key(u0 + du, 0, v0 + dv));
                        if (id == null) {
                            full = false;
                            break;
                        }
                        count.merge(id, 1, Integer::sum);
                    }
                }
                if (!full) continue;
                for (Map.Entry<String, Integer> ce : count.entrySet()) {
                    if (ce.getValue() > 18) {
                        bland.add(pl.getKey() + " at " + u0 + "," + v0 + " " + St.path(ce.getKey()) + " ×" + ce.getValue());
                        break;
                    }
                }
            }
        }
        report("bland 5×5 surfaces (> 75 % one block)", bland, 0);
    }

    /**
     * Façades have relief: in {@code region}, for each side (north, east, south, west) seen from outside, every stretch of 10
     * cells along the façade (with something built in it) shows at least 2 depth layers — the outermost face of each (u, y) cell
     * column lies at at least 2 different depths. Leaves, plants and natural ground are left out.
     */
    public static void assertDepth(Canvas c, Box region) {
        List<String> flat = new ArrayList<>();
        for (Dir d : Dir.HORIZONTAL) {
            // u runs along the façade; depth is how far toward d the outermost face stands.
            Map<Integer, Set<Integer>> depthsAtU = new TreeMap<>();
            for (int x = region.x0(); x <= region.x1(); x++) {
                for (int y = region.y0(); y <= region.y1(); y++) {
                    for (int z = region.z0(); z <= region.z1(); z++) {
                        String s = c.get(x, y, z);
                        if (s == null || Kinds.air(s) || Kinds.leaves(s) || Kinds.soilPlant(s) || NATURAL.contains(St.id(s)) || Kinds.passable(s)) continue;
                        if (!Kinds.air(at(c, x + d.dx, y, z + d.dz))) continue;
                        int u = d.dx != 0 ? z : x;
                        int depth = d.dx != 0 ? x * d.dx : z * d.dz;
                        // Keep only faces that are the outermost of their (u, y) line.
                        boolean outermost = true;
                        for (int k = 1; k <= 3 && outermost; k++) {
                            String o = c.get(x + d.dx * k, y, z + d.dz * k);
                            if (o != null && !Kinds.air(o) && !Kinds.leaves(o) && !Kinds.soilPlant(o) && !Kinds.passable(o) && !NATURAL.contains(St.id(o))) outermost = false;
                        }
                        if (outermost) depthsAtU.computeIfAbsent(u, k -> new HashSet<>()).add(depth);
                    }
                }
            }
            if (depthsAtU.isEmpty()) continue;
            int uMin = depthsAtU.keySet().iterator().next(), uMax = ((TreeMap<Integer, Set<Integer>>) depthsAtU).lastKey();
            for (int u0 = uMin; u0 + 9 <= uMax; u0++) {
                Set<Integer> depths = new HashSet<>();
                int filled = 0;
                for (int u = u0; u < u0 + 10; u++) {
                    Set<Integer> ds = depthsAtU.get(u);
                    if (ds == null) continue;
                    filled++;
                    depths.addAll(ds);
                }
                if (filled >= 5 && depths.size() < 2) flat.add(d.id() + " façade, cells " + u0 + ".." + (u0 + 9));
            }
        }
        report("flat façade stretches", flat, 0);
    }

    // --- water ---------------------------------------------------------------------------------------------------------------

    /**
     * Water never spills: a source (or waterlogged block) has water or a solid block on every horizontal side except toward a
     * flowing cell and something under it; flowing water (levels 1–7) has falling water or water under it, or is held; falling
     * water (8+) ends on water or a solid block.
     */
    public static void assertWaterContained(Canvas c) {
        List<String> leaks = new ArrayList<>();
        for (Map.Entry<Long, String> e : c.map().entrySet()) {
            String s = e.getValue();
            if (!St.path(s).equals("water")) continue;
            int x = Canvas.kx(e.getKey()), y = Canvas.ky(e.getKey()), z = Canvas.kz(e.getKey());
            String lv = St.get(s, "level");
            int level = lv == null ? 0 : Integer.parseInt(lv);
            String below = at(c, x, y - 1, z);
            if (level >= 8) {
                if (Kinds.air(below)) leaks.add(x + "," + y + "," + z + " falls on air");
                continue;
            }
            if (Kinds.air(below) && level == 0) leaks.add(x + "," + y + "," + z + " source over air");
            if (level > 0 && St.path(below).equals("water")) continue;
            for (Dir d : Dir.HORIZONTAL) {
                String n = at(c, x + d.dx, y, z + d.dz);
                if (Kinds.water(n) || St.path(n).equals("water")) continue;
                if (Kinds.air(n) || Kinds.passable(n) && !Kinds.soilPlant(n)) leaks.add(x + "," + y + "," + z + " spills " + d.id());
            }
        }
        report("water leaks", leaks, 0);
    }
}
