package org.papiricoh.supernaturalcraft.buildkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A layout while it is drawn (pure): block states by cell, named zones and anchors, and the {@link Decor}.
 *
 * <h2>Coordinates</h2>
 * X east, Y up, Z south (north = -Z). By convention a layout's origin is on its main floor: {@code y = 0} is the first air
 * cell above the floor and the floor block is at {@code y = -1}. A cell that was never written is "unplanned": the world keeps
 * what it has there ({@link #outside} says what the checks assume it is). Writing {@code minecraft:air} (see {@link #carve})
 * plans air: the writer clears the cell.
 *
 * <h2>Write modes</h2>
 * <ul>
 *   <li>{@link #set}: overwrite (the default; later writes win);</li>
 *   <li>{@link #setIfEmpty}: only where nothing is planned yet (backgrounds drawn after the foreground);</li>
 *   <li>{@link #setIfAir}: where nothing is planned or air is planned (furniture into a room, plants on ground);</li>
 *   <li>{@link #overlay}: only over a planned non-air block (re-skin, weather, moss over what exists);</li>
 *   <li>{@link #carve}: plan air (doorways, windows, the inside of a room).</li>
 * </ul>
 * Every state is checked when written ({@link St#check}): a malformed string fails at once instead of turning into air in the
 * world. Unknown ids and properties are caught by the layout's test ({@code LayoutQuality.assertIdsKnown}).
 *
 * <h2>Zones and anchors</h2>
 * {@link #zone} names a box; while a zone is {@link #enter entered}, a write outside it throws (each builder keeps to its own
 * ground), and {@link #anchor}s set are recorded both globally and on the zone. Anchor names must be unique on the canvas.
 *
 * <h2>Finishing</h2>
 * {@link #finish} runs {@link Shapes}: stair corners and fence/wall/pane/bar joins as vanilla would compute them, so the writer
 * can place the states as they are, with no block updates. Call it once, at the end.
 */
public final class Canvas {

    /** How a write treats what is already planned. */
    public enum Mode {SET, IF_EMPTY, IF_AIR, OVERLAY}

    private static final int OFF = 1 << 20;
    private static final Set<String> CHECKED = ConcurrentHashMap.newKeySet();

    private final String name;
    private final Map<Long, String> cells = new HashMap<>();
    /** The zone each cell was last written in (absent: written outside every zone). */
    private final Map<Long, String> owner = new HashMap<>();
    private final List<Decor> decor = new ArrayList<>();
    private final Map<String, int[]> anchors = new LinkedHashMap<>();
    private final Map<String, Box> zones = new LinkedHashMap<>();
    private final Map<String, Map<String, int[]>> zoneAnchors = new LinkedHashMap<>();
    private String active;
    private Outside outside = Outside.AIR;
    private boolean finished;

    public Canvas(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    // --- keys --------------------------------------------------------------------------------------------------------------------

    /** A cell packed in a long: 21 bits per axis, coordinates within ±1 048 575. */
    public static long key(int x, int y, int z) {
        return ((long) (x + OFF) << 42) | ((long) (z + OFF) << 21) | (y + OFF);
    }

    public static int kx(long key) {
        return (int) (key >>> 42) - OFF;
    }

    public static int kz(long key) {
        return (int) ((key >>> 21) & 0x1FFFFF) - OFF;
    }

    public static int ky(long key) {
        return (int) (key & 0x1FFFFF) - OFF;
    }

    // --- writing -------------------------------------------------------------------------------------------------------------

    private String valid(String state) {
        String s = St.full(state);
        if (!CHECKED.contains(s)) {
            St.check(s);
            CHECKED.add(s);
        }
        return s;
    }

    private void guard(int x, int y, int z, String what) {
        if (active != null && !zones.get(active).contains(x, y, z)) {
            throw new IllegalStateException("zone " + active + " writes outside itself at " + x + "," + y + "," + z + ": " + what);
        }
    }

    /** Writes with {@code mode}; returns whether it wrote. */
    public boolean put(Mode mode, int x, int y, int z, String state) {
        String s = valid(state);
        long k = key(x, y, z);
        String cur = cells.get(k);
        boolean write = switch (mode) {
            case SET -> true;
            case IF_EMPTY -> cur == null;
            case IF_AIR -> cur == null || Kinds.air(cur);
            case OVERLAY -> cur != null && !Kinds.air(cur);
        };
        if (!write) return false;
        guard(x, y, z, s);
        cells.put(k, s);
        if (active != null) owner.put(k, active);
        else owner.remove(k);
        return true;
    }

    public void set(int x, int y, int z, String state) {
        put(Mode.SET, x, y, z, state);
    }

    public void set(int x, int y, int z, Brush brush) {
        put(Mode.SET, x, y, z, brush.at(x, y, z));
    }

    public boolean setIfEmpty(int x, int y, int z, String state) {
        return put(Mode.IF_EMPTY, x, y, z, state);
    }

    public boolean setIfAir(int x, int y, int z, String state) {
        return put(Mode.IF_AIR, x, y, z, state);
    }

    public boolean overlay(int x, int y, int z, String state) {
        return put(Mode.OVERLAY, x, y, z, state);
    }

    /** Plans air: the writer clears the cell. */
    public void carve(int x, int y, int z) {
        put(Mode.SET, x, y, z, St.AIR);
    }

    /** Forgets a cell: the world keeps what it has there. */
    public void remove(int x, int y, int z) {
        guard(x, y, z, "(remove)");
        cells.remove(key(x, y, z));
        owner.remove(key(x, y, z));
    }

    /** The two halves of a two-high block ([lower, upper], e.g. {@link St#door} or {@link St#tall}) from {@code y} up. */
    public void setPair(int x, int y, int z, String[] lowerUpper) {
        set(x, y, z, lowerUpper[0]);
        set(x, y + 1, z, lowerUpper[1]);
    }

    /** The two halves of a bed ([foot, head] from {@link St#bed}) with its foot at (x, y, z). */
    public void setBed(int x, int y, int z, String[] footHead) {
        Dir f = Dir.of(St.get(footHead[0], "facing"));
        set(x, y, z, footHead[0]);
        set(x + f.dx, y, z + f.dz, footHead[1]);
    }

    public void fill(Box b, Brush brush) {
        fill(b, brush, Mode.SET);
    }

    public void fill(Box b, String state) {
        fill(b, Brush.of(state), Mode.SET);
    }

    public void fill(Box b, Brush brush, Mode mode) {
        for (int x = b.x0(); x <= b.x1(); x++) {
            for (int y = b.y0(); y <= b.y1(); y++) {
                for (int z = b.z0(); z <= b.z1(); z++) put(mode, x, y, z, brush.at(x, y, z));
            }
        }
    }

    /** Plans air over a box. */
    public void carve(Box b) {
        fill(b, Brush.of(St.AIR), Mode.SET);
    }

    /** The faces of a box only (a hollow shell). */
    public void shell(Box b, Brush brush) {
        for (int x = b.x0(); x <= b.x1(); x++) {
            for (int y = b.y0(); y <= b.y1(); y++) {
                for (int z = b.z0(); z <= b.z1(); z++) {
                    if (x == b.x0() || x == b.x1() || y == b.y0() || y == b.y1() || z == b.z0() || z == b.z1()) set(x, y, z, brush.at(x, y, z));
                }
            }
        }
    }

    // --- reading --------------------------------------------------------------------------------------------------------------

    /** The planned state, or null when the cell is unplanned. */
    public String get(int x, int y, int z) {
        return cells.get(key(x, y, z));
    }

    public boolean has(int x, int y, int z) {
        return cells.containsKey(key(x, y, z));
    }

    /** What the cell will be: the planned state, or what lies {@link #outside}. */
    public String world(int x, int y, int z) {
        String s = cells.get(key(x, y, z));
        return s != null ? s : outside.at(x, y, z);
    }

    /** Whether the cell will be air (planned air, or unplanned with air outside). */
    public boolean isAir(int x, int y, int z) {
        return Kinds.air(world(x, y, z));
    }

    /** The top non-air planned cell of a column at or below {@code fromY}, or {@code Integer.MIN_VALUE}. */
    public int topAt(int x, int z, int fromY, int downTo) {
        for (int y = fromY; y >= downTo; y--) {
            String s = cells.get(key(x, y, z));
            if (s != null && !Kinds.air(s)) return y;
        }
        return Integer.MIN_VALUE;
    }

    public int size() {
        return cells.size();
    }

    /** Planned cells, read-only (keys from {@link #key}). */
    public Map<Long, String> map() {
        return Collections.unmodifiableMap(cells);
    }

    /** Mutable access for the finishing passes ({@link Shapes}). */
    Map<Long, String> raw() {
        return cells;
    }

    /** Every planned cell, sorted by y, then z, then x (a stable order for dumps and writers). */
    public List<Cell> cells() {
        List<Cell> out = new ArrayList<>(cells.size());
        for (Map.Entry<Long, String> e : cells.entrySet()) {
            long k = e.getKey();
            out.add(new Cell(kx(k), ky(k), kz(k), e.getValue()));
        }
        out.sort(Comparator.comparingInt(Cell::y).thenComparingInt(Cell::z).thenComparingInt(Cell::x));
        return out;
    }

    /**
     * Every planned cell moved by {@code -origin} and mapped, e.g. for a Horseman-style ground plan whose {@code dy = 0} replaces
     * the surface block (our floor, {@code y = -1}): {@code canvas.export(0, -1, 0, ArenaCell::new)}.
     */
    public <T> List<T> export(int ox, int oy, int oz, Cell.Mapper<T> mapper) {
        List<T> out = new ArrayList<>(cells.size());
        for (Cell c : cells()) out.add(mapper.map(c.x() - ox, c.y() - oy, c.z() - oz, c.state()));
        return out;
    }

    /** The box round every planned cell (and decor). */
    public Box bounds() {
        if (cells.isEmpty()) return new Box(0, 0, 0, 0, 0, 0);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (long k : cells.keySet()) {
            int x = kx(k), y = ky(k), z = kz(k);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }
        for (Decor d : decor) {
            minX = Math.min(minX, d.x());
            maxX = Math.max(maxX, d.x());
            minY = Math.min(minY, d.y());
            maxY = Math.max(maxY, d.y());
            minZ = Math.min(minZ, d.z());
            maxZ = Math.max(maxZ, d.z());
        }
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    // --- what lies outside --------------------------------------------------------------------------------------------------

    public Canvas outside(Outside o) {
        this.outside = o;
        return this;
    }

    public Outside outside() {
        return outside;
    }

    // --- zones and anchors ----------------------------------------------------------------------------------------------------

    /** Names a zone (its box may overlap others). */
    public Canvas zone(String zoneName, Box bounds) {
        if (zones.containsKey(zoneName)) throw new IllegalStateException("two zones named " + zoneName);
        zones.put(zoneName, bounds);
        zoneAnchors.put(zoneName, new LinkedHashMap<>());
        return this;
    }

    /** From now on writes must fall inside the zone; anchors are recorded on it too. */
    public Canvas enter(String zoneName) {
        if (!zones.containsKey(zoneName)) throw new IllegalArgumentException("no zone " + zoneName);
        active = zoneName;
        return this;
    }

    public Canvas leave() {
        active = null;
        return this;
    }

    /** Runs {@code draw} inside a zone (entered, then left even if it throws). */
    public Canvas inZone(String zoneName, Runnable draw) {
        String before = active;
        enter(zoneName);
        try {
            draw.run();
        } finally {
            active = before;
        }
        return this;
    }

    public String activeZone() {
        return active;
    }

    /** A named spot (feet cell). Unique per canvas; also recorded on the active zone. */
    public Canvas anchor(String anchorName, int x, int y, int z) {
        if (anchors.containsKey(anchorName)) throw new IllegalStateException("two anchors named " + anchorName);
        anchors.put(anchorName, new int[]{x, y, z});
        if (active != null) zoneAnchors.get(active).put(anchorName, new int[]{x, y, z});
        return this;
    }

    public int[] anchor(String anchorName) {
        int[] a = anchors.get(anchorName);
        if (a == null) throw new IllegalArgumentException("no anchor " + anchorName + " (has " + anchors.keySet() + ")");
        return a.clone();
    }

    public Map<String, int[]> anchors() {
        return Collections.unmodifiableMap(anchors);
    }

    public List<LayoutZone> zones() {
        List<LayoutZone> out = new ArrayList<>();
        for (Map.Entry<String, Box> e : zones.entrySet()) {
            out.add(new LayoutZone(e.getKey(), e.getValue(), Collections.unmodifiableMap(new LinkedHashMap<>(zoneAnchors.get(e.getKey())))));
        }
        return out;
    }

    public LayoutZone zone(String zoneName) {
        for (LayoutZone z : zones()) if (z.name().equals(zoneName)) return z;
        throw new IllegalArgumentException("no zone " + zoneName);
    }

    // --- decor --------------------------------------------------------------------------------------------------------------

    public Canvas decor(Decor d) {
        guard(d.x(), d.y(), d.z(), d.toString());
        decor.add(d);
        return this;
    }

    public List<Decor> decor() {
        return Collections.unmodifiableList(decor);
    }

    // --- prefabs ------------------------------------------------------------------------------------------------------------

    /**
     * Stamps another canvas into this one: turned {@code quarterTurns} clockwise (seen from above) round its origin, then moved
     * by (dx, dy, dz). States are turned with {@link St#rotate}; decor and anchors come along (anchors prefixed with
     * {@code prefix}, or dropped when it is null). Draw a prefab facing south and stamp it turned.
     */
    public void stamp(Canvas prefab, int dx, int dy, int dz, int quarterTurns, Mode mode, String prefix) {
        int q = Math.floorMod(quarterTurns, 4);
        for (Map.Entry<Long, String> e : prefab.cells.entrySet()) {
            long k = e.getKey();
            int[] r = turn(kx(k), kz(k), q);
            put(mode, r[0] + dx, ky(k) + dy, r[1] + dz, St.rotate(e.getValue(), q));
        }
        for (Decor d : prefab.decor) {
            int[] r = turn(d.x(), d.z(), q);
            String f = d.facing().isEmpty() || d.facing().equals("up") || d.facing().equals("down") ? d.facing() : rotateDir(Dir.of(d.facing()), q).id();
            decor(new Decor(d.kind(), r[0] + dx, d.y() + dy, r[1] + dz, f, d.data(), d.extra()));
        }
        if (prefix != null) {
            for (Map.Entry<String, int[]> a : prefab.anchors.entrySet()) {
                int[] r = turn(a.getValue()[0], a.getValue()[2], q);
                anchor(prefix + a.getKey(), r[0] + dx, a.getValue()[1] + dy, r[1] + dz);
            }
        }
    }

    /** (x, z) turned {@code q} quarter turns clockwise seen from above (east → south → west → north). */
    public static int[] turn(int x, int z, int q) {
        return switch (Math.floorMod(q, 4)) {
            case 1 -> new int[]{-z, x};
            case 2 -> new int[]{-x, -z};
            case 3 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    static Dir rotateDir(Dir d, int q) {
        Dir out = d;
        for (int i = 0; i < Math.floorMod(q, 4); i++) out = out.cw();
        return out;
    }

    // --- the shared contract ------------------------------------------------------------------------------------------------

    /** The zone name that {@link #toPlan} gives cells written outside every zone (terrain, landscaping): written first. */
    public static final String BASE_ZONE = "base";

    /**
     * This canvas as the mod's layout contract ({@link org.papiricoh.supernaturalcraft.layout.LayoutPlan}), coordinates as they
     * are ({@code y = 0} the first air above the main floor). Zones in write order: first {@link #BASE_ZONE} (cells written while
     * no zone was entered, when there are any), then every zone in the order it was declared, each holding the cells last written
     * while it was entered. Decor maps to {@link org.papiricoh.supernaturalcraft.layout.LayoutDecor} kinds, anchors to points.
     * Call {@link #finish} first.
     */
    public org.papiricoh.supernaturalcraft.layout.LayoutPlan toPlan() {
        Map<String, List<org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell>> byZone = new LinkedHashMap<>();
        byZone.put(BASE_ZONE, new ArrayList<>());
        for (String z : zones.keySet()) byZone.put(z, new ArrayList<>());
        for (Cell c : cells()) {
            String z = owner.get(key(c.x(), c.y(), c.z()));
            byZone.get(z == null ? BASE_ZONE : z).add(new org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell(c.x(), c.y(), c.z(), c.state()));
        }
        List<org.papiricoh.supernaturalcraft.layout.LayoutPlan.Zone> out = new ArrayList<>();
        for (Map.Entry<String, List<org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell>> e : byZone.entrySet()) {
            if (e.getKey().equals(BASE_ZONE) && e.getValue().isEmpty()) continue;
            out.add(new org.papiricoh.supernaturalcraft.layout.LayoutPlan.Zone(e.getKey(), List.copyOf(e.getValue())));
        }
        List<org.papiricoh.supernaturalcraft.layout.LayoutDecor> d = new ArrayList<>();
        for (Decor x : decor) d.add(x.toContract());
        Map<String, org.papiricoh.supernaturalcraft.layout.LayoutPoint> points = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> a : anchors.entrySet()) {
            points.put(a.getKey(), new org.papiricoh.supernaturalcraft.layout.LayoutPoint(a.getValue()[0], a.getValue()[1], a.getValue()[2]));
        }
        return new org.papiricoh.supernaturalcraft.layout.LayoutPlan(List.copyOf(out), List.copyOf(d), java.util.Collections.unmodifiableMap(points));
    }

    /**
     * A canvas rebuilt from a plan (for the quality checks and the dump of layouts that only expose a plan): every cell, the
     * decor (kinds the kit knows), the points as anchors, and each plan zone as a zone whose box bounds its cells.
     */
    public static Canvas fromPlan(String name, org.papiricoh.supernaturalcraft.layout.LayoutPlan plan) {
        Canvas c = new Canvas(name);
        for (org.papiricoh.supernaturalcraft.layout.LayoutPlan.Zone z : plan.zones()) {
            if (z.cells().isEmpty()) continue;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (var a : z.cells()) {
                minX = Math.min(minX, a.dx());
                maxX = Math.max(maxX, a.dx());
                minY = Math.min(minY, a.dy());
                maxY = Math.max(maxY, a.dy());
                minZ = Math.min(minZ, a.dz());
                maxZ = Math.max(maxZ, a.dz());
            }
            if (!c.zones.containsKey(z.name())) c.zone(z.name(), new Box(minX, minY, minZ, maxX, maxY, maxZ));
            c.active = z.name();
            for (var a : z.cells()) c.put(Mode.SET, a.dx(), a.dy(), a.dz(), a.block());
            c.active = null;
        }
        for (org.papiricoh.supernaturalcraft.layout.LayoutDecor d : plan.decor()) {
            Decor k = Decor.fromContract(d);
            if (k != null) c.decor.add(k);
        }
        for (Map.Entry<String, org.papiricoh.supernaturalcraft.layout.LayoutPoint> p : plan.points().entrySet()) {
            c.anchors.put(p.getKey(), new int[]{p.getValue().x(), p.getValue().y(), p.getValue().z()});
        }
        c.finished = true;
        return c;
    }

    /** A deep copy (cells, owners, decor, anchors, zones, outside) under another name. */
    public Canvas copy(String newName) {
        Canvas c = new Canvas(newName);
        c.cells.putAll(cells);
        c.owner.putAll(owner);
        c.decor.addAll(decor);
        c.anchors.putAll(anchors);
        c.zones.putAll(zones);
        for (Map.Entry<String, Map<String, int[]>> e : zoneAnchors.entrySet()) c.zoneAnchors.put(e.getKey(), new LinkedHashMap<>(e.getValue()));
        c.outside = outside;
        c.finished = finished;
        return c;
    }

    // --- finishing ----------------------------------------------------------------------------------------------------------

    /** Shapes stairs and joins fences, walls, panes and bars ({@link Shapes}). Idempotent. */
    public Canvas finish() {
        Shapes.apply(this);
        finished = true;
        return this;
    }

    public boolean finished() {
        return finished;
    }
}
