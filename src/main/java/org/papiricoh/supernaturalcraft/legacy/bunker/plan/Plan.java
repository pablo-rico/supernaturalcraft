package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The bunker's plan while it is drawn (pure): block states by local position (later writes win) and the {@link Decor}. While a
 * {@link Zones.Zone} is set, a write outside it (and outside its doorways) is an error: each part keeps to its own ground.
 */
public final class Plan {

    private final Map<Long, String> cells = new HashMap<>();
    private final List<Decor> decor = new ArrayList<>();
    private final Map<String, Zones.View> views = new java.util.LinkedHashMap<>();
    private Zones.Zone zone;

    public static long key(int x, int y, int z) {
        return ((long) (x + 1024) << 24) | ((long) (z + 1024) << 12) | (y + 1024);
    }

    public static int kx(long key) {
        return (int) (key >> 24) - 1024;
    }

    public static int kz(long key) {
        return (int) ((key >> 12) & 0xFFF) - 1024;
    }

    public static int ky(long key) {
        return (int) (key & 0xFFF) - 1024;
    }

    /** From now on writes must fall in {@code zone} (null: anywhere). */
    public void zone(Zones.Zone zone) {
        this.zone = zone;
    }

    public Zones.Zone zone() {
        return zone;
    }

    public void set(int x, int y, int z, String state) {
        if (zone != null && !Zones.writable(zone, x, y, z)) {
            throw new IllegalStateException("zone " + zone.name() + " writes outside itself at " + x + "," + y + "," + z + ": " + state);
        }
        cells.put(key(x, y, z), state.intern());
    }

    /** Writes only if the plan has nothing there yet (or air, when {@code overAir}). */
    public void fill(int x, int y, int z, String state, boolean overAir) {
        String cur = get(x, y, z);
        if (cur == null || overAir && Kinds.air(cur)) set(x, y, z, state);
    }

    /** The state planned at a point, or null. */
    public String get(int x, int y, int z) {
        return cells.get(key(x, y, z));
    }

    public boolean isAir(int x, int y, int z) {
        String s = get(x, y, z);
        return s != null && Kinds.air(s);
    }

    public void box(int x0, int x1, int y0, int y1, int z0, int z1, String state) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, state);
            }
        }
    }

    public void box(int x0, int x1, int y0, int y1, int z0, int z1, Kit.Pattern pattern) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, pattern.at(x, y, z));
            }
        }
    }

    public void air(int x0, int x1, int y0, int y1, int z0, int z1) {
        box(x0, x1, y0, y1, z0, z1, St.AIR);
    }

    public void decor(Decor d) {
        if (zone != null && !Zones.writable(zone, d.x(), d.y(), d.z())) {
            throw new IllegalStateException("zone " + zone.name() + " decorates outside itself at " + d);
        }
        decor.add(d);
    }

    /** A room's spot (see {@link Zones.View}): where one stands in it and what the preview looks at. */
    public void view(String room, int x, int y, int z, double lx, double ly, double lz) {
        if (views.containsKey(room)) throw new IllegalStateException("two spots for " + room);
        views.put(room, new Zones.View(x, y, z, lx, ly, lz));
    }

    public Map<String, Zones.View> views() {
        return views;
    }

    public Map<Long, String> cells() {
        return cells;
    }

    public List<Decor> decor() {
        return decor;
    }

    /** A stable pseudo-random number in [0, 1) for a point and a salt: variety that is the same every time. */
    public static double noise(int x, int y, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ salt * 0x27D4EB2F165667C5L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }
}
