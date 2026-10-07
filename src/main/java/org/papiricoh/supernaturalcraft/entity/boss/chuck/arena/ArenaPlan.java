package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One chapter's arena as the Author writes it: every cell he sets, in the order he sets them (bottom-up, then
 * outward from the centre), plus where he stands. Pure.
 *
 * <p><b>Coordinates</b> are offsets from the arena's centre column; {@code dy} counts from the <i>floor layer</i>:
 * {@code dy = 0} is the block the hunters walk on (one below the centre, which is feet level), {@code dy = 1} the first
 * block above it, negative {@code dy} the ground beneath (pits, pool bottoms, the clouds' undersides).
 */
public final class ArenaPlan {

    /** Lowest and highest {@code dy} a plan may use: the AUTHOR arena reaches 12 below its centre and 48 above. */
    public static final int MIN_DY = -11, MAX_DY = 46;

    /** One block of the plan. */
    public record Cell(int dx, int dy, int dz, ArenaKind kind) {
        public int dist2() {
            return dx * dx + dz * dz;
        }
    }

    private final int radius;
    private final List<Cell> cells;
    private final Map<Long, ArenaKind> lookup;
    private final Cell spawn;
    private final List<Cell> lights, water;

    private ArenaPlan(int radius, List<Cell> cells, Map<Long, ArenaKind> lookup, Cell spawn) {
        this.radius = radius;
        this.cells = cells;
        this.lookup = lookup;
        this.spawn = spawn;
        List<Cell> l = new ArrayList<>(), w = new ArrayList<>();
        for (Cell c : cells) {
            if (c.kind().light()) l.add(c);
            if (c.kind().water()) w.add(c);
        }
        this.lights = Collections.unmodifiableList(l);
        this.water = Collections.unmodifiableList(w);
    }

    public int radius() {
        return radius;
    }

    /** Every cell, in writing order: bottom-up, then from the centre outward. */
    public List<Cell> cells() {
        return cells;
    }

    /** What the plan puts at a cell, or null if it leaves it alone. */
    public ArenaKind at(int dx, int dy, int dz) {
        return lookup.get(key(dx, dy, dz));
    }

    /** Where the Author stands: the cell his feet are in. */
    public Cell spawn() {
        return spawn;
    }

    /** Cells that give off light. */
    public List<Cell> lights() {
        return lights;
    }

    /** Cells of water. */
    public List<Cell> water() {
        return water;
    }

    public static long key(int dx, int dy, int dz) {
        return ((long) (dx + 1024) << 32) | ((long) (dy + 1024) << 16) | (dz + 1024);
    }

    /** The order the Author writes in: bottom-up, then outward, then a fixed tie-break. */
    public static final Comparator<Cell> WRITE_ORDER = Comparator.comparingInt(Cell::dy).thenComparingInt(Cell::dist2)
            .thenComparingInt(Cell::dx).thenComparingInt(Cell::dz);

    /** Collects cells (a later set overwrites an earlier one), clipped to the arena's cylinder. */
    public static final class Builder {

        private final int radius;
        private final Map<Long, Cell> cells = new LinkedHashMap<>();

        public Builder(int radius) {
            this.radius = radius;
        }

        public int radius() {
            return radius;
        }

        /** Whether a column lies inside the arena. */
        public boolean inside(int dx, int dz) {
            return dx * dx + dz * dz <= radius * radius;
        }

        public Builder set(int dx, int dy, int dz, ArenaKind kind) {
            if (inside(dx, dz) && dy >= MIN_DY && dy <= MAX_DY) cells.put(key(dx, dy, dz), new Cell(dx, dy, dz, kind));
            return this;
        }

        /** Sets a cell only if nothing is planned there yet. */
        public Builder setIfAbsent(int dx, int dy, int dz, ArenaKind kind) {
            if (get(dx, dy, dz) == null) set(dx, dy, dz, kind);
            return this;
        }

        public ArenaKind get(int dx, int dy, int dz) {
            Cell c = cells.get(key(dx, dy, dz));
            return c == null ? null : c.kind();
        }

        /** A vertical run of {@code kind} from {@code from} to {@code to} inclusive. */
        public Builder column(int dx, int dz, int from, int to, ArenaKind kind) {
            for (int y = from; y <= to; y++) set(dx, y, dz, kind);
            return this;
        }

        public ArenaPlan build(int spawnDx, int spawnDy, int spawnDz) {
            List<Cell> list = new ArrayList<>(cells.values());
            list.sort(WRITE_ORDER);
            Map<Long, ArenaKind> lookup = new HashMap<>(list.size() * 2);
            for (Cell c : list) lookup.put(key(c.dx(), c.dy(), c.dz()), c.kind());
            return new ArenaPlan(radius, Collections.unmodifiableList(list), lookup,
                    new Cell(spawnDx, spawnDy, spawnDz, ArenaKind.AIR));
        }
    }
}
