package org.papiricoh.supernaturalcraft.buildkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ground of a layout (pure): for each column, the y of its top solid block (the surface block; one stands at {@code top + 1}),
 * how far inside the ground's outline it is ({@code inner}: 0 at the rim, 1 deep inside) and whether water lies on it. Made by
 * {@link Terrain} (or {@link #flat}) and read by {@link Paths}, {@link Scatter} and {@link Trees} placement so landscaping
 * follows the terrain.
 */
public final class Ground {

    private final Map<Long, Integer> top = new HashMap<>();
    private final Map<Long, Double> inner = new HashMap<>();
    private final Set<Long> water = new HashSet<>();

    public static long col(int x, int z) {
        return Canvas.key(x, 0, z);
    }

    /** A flat ground over a box's columns whose surface block is at {@code y}. */
    public static Ground flat(Box area, int y) {
        Ground g = new Ground();
        int hx = Math.max(1, area.sizeX() / 2), hz = Math.max(1, area.sizeZ() / 2);
        for (int x = area.x0(); x <= area.x1(); x++) {
            for (int z = area.z0(); z <= area.z1(); z++) {
                double ix = 1 - Math.abs(x - area.centerX()) / (double) hx, iz = 1 - Math.abs(z - area.centerZ()) / (double) hz;
                g.set(x, z, y, Math.max(0, Math.min(ix, iz)));
            }
        }
        return g;
    }

    public void set(int x, int z, int y, double innerness) {
        top.put(col(x, z), y);
        inner.put(col(x, z), innerness);
    }

    public void setTop(int x, int z, int y) {
        top.put(col(x, z), y);
        inner.putIfAbsent(col(x, z), 0.5);
    }

    public boolean has(int x, int z) {
        return top.containsKey(col(x, z));
    }

    public int top(int x, int z) {
        Integer y = top.get(col(x, z));
        if (y == null) throw new IllegalArgumentException("no ground at " + x + "," + z);
        return y;
    }

    public int topOr(int x, int z, int fallback) {
        Integer y = top.get(col(x, z));
        return y == null ? fallback : y;
    }

    public double inner(int x, int z) {
        Double v = inner.get(col(x, z));
        return v == null ? 0 : v;
    }

    public void markWater(int x, int z) {
        water.add(col(x, z));
    }

    public boolean water(int x, int z) {
        return water.contains(col(x, z));
    }

    /** Whether a horizontal neighbour has no ground (the column is on the rim). */
    public boolean edge(int x, int z) {
        for (Dir d : Dir.HORIZONTAL) if (!has(x + d.dx, z + d.dz)) return true;
        return false;
    }

    /** The largest height step to a horizontal neighbour (a missing neighbour counts as 99). */
    public int slope(int x, int z) {
        int t = top(x, z), s = 0;
        for (Dir d : Dir.HORIZONTAL) {
            Integer n = top.get(col(x + d.dx, z + d.dz));
            s = Math.max(s, n == null ? 99 : Math.abs(n - t));
        }
        return s;
    }

    /** Every column as {x, z}. */
    public List<int[]> columns() {
        List<int[]> out = new ArrayList<>(top.size());
        for (long k : top.keySet()) out.add(new int[]{Canvas.kx(k), Canvas.kz(k)});
        out.sort((a, b) -> a[1] != b[1] ? Integer.compare(a[1], b[1]) : Integer.compare(a[0], b[0]));
        return out;
    }

    public int size() {
        return top.size();
    }
}
