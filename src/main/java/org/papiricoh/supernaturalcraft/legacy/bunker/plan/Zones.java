package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import java.util.List;

/**
 * The bunker's floor plan (pure): the box each part of the plan may write in, the doorways joined across their borders, the
 * levels' floors, and a camera spot for every room (the preview walks them).
 *
 * <p>Local axes: X west(−)/east(+), Z north(−)/south(+) (the door looks south), Y = 0 the ground's top block (one stands at 1).
 * Floors are the y of the floor block; one stands one above.
 *
 * <pre>
 * surface   the hill and the façade (door at 0,1,5), the vestibule; the stair tunnel down to the rotunda's gallery
 * level 1   floor −19: LIBRARY (west) · ROTUNDA, the war room (centre, gallery at −11) · ARCHIVE (east); the CORE stairwell north
 * level 2   floor −29: dormitories, baths, kitchen, the Dean cave, the lab, the armoury (all under level 1 and north of it)
 * level 3   floor −39: the garage, the dungeon, the boiler room, the artifact vault
 * </pre>
 */
public final class Zones {

    /** Floors (the floor block's y). */
    public static final int GROUND = 0, GALLERY = -11, LEVEL1 = -19, LEVEL2 = -29, LEVEL3 = -39;
    /** The rotunda's centre (Z) and its radius (the open floor inside its walls). */
    public static final int ROTUNDA_Z = -30, ROTUNDA_R = 14;

    /** An inclusive local box. */
    public record Box(int x0, int x1, int y0, int y1, int z0, int z1) {
        public boolean contains(int x, int y, int z) {
            return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
        }
    }

    /** Where a part may write: inside one of {@code boxes} and outside every one of {@code holes}. */
    public record Zone(String name, List<Box> boxes, List<Box> holes) {
        public boolean contains(int x, int y, int z) {
            for (Box h : holes) if (h.contains(x, y, z)) return false;
            for (Box b : boxes) if (b.contains(x, y, z)) return true;
            return false;
        }
    }

    /** A doorway across a border: both sides may write in its box and both must leave it open (air or a door). */
    public record Portal(String name, Box box, String a, String b) {
    }

    /**
     * A room's spot: the block one stands in (feet; the preview's camera stands there, its eye 1.6 up) and the point it looks at.
     * Every spot must be reachable on foot from the door and lit.
     */
    public record View(int x, int y, int z, double lx, double ly, double lz) {
    }

    public static final Box CORE_BOX = new Box(-6, 6, -40, -13, -60, -47);

    public static final Zone ENTRANCE = new Zone("entrance", List.of(new Box(-18, 18, -13, 14, -13, 16)), List.of());
    public static final Zone ROTUNDA = new Zone("rotunda", List.of(new Box(-16, 16, -21, -1, -46, -14)), List.of());
    public static final Zone CORE = new Zone("core", List.of(CORE_BOX), List.of());
    public static final Zone LIBRARY = new Zone("library", List.of(new Box(-46, -17, -21, -1, -52, -10)), List.of());
    public static final Zone ARCHIVE = new Zone("archive", List.of(new Box(17, 46, -21, -1, -52, -10)), List.of());
    public static final Zone LEVEL_2 = new Zone("level2", List.of(new Box(-46, 46, -31, -22, -78, -10)), List.of(CORE_BOX));
    public static final Zone LEVEL_3 = new Zone("level3", List.of(new Box(-46, 46, -41, -32, -78, -10)), List.of(CORE_BOX));

    public static final List<Zone> ALL = List.of(ENTRANCE, ROTUNDA, CORE, LIBRARY, ARCHIVE, LEVEL_2, LEVEL_3);

    /** The doorways between zones (each 3 wide; floors and galleries). */
    public static final List<Portal> PORTALS = List.of(
            new Portal("tunnel", new Box(-1, 1, -10, -8, -15, -13), "entrance", "rotunda"),
            new Portal("library_floor", new Box(-19, -14, -18, -15, -31, -29), "rotunda", "library"),
            new Portal("library_gallery", new Box(-19, -14, -10, -8, -31, -29), "rotunda", "library"),
            new Portal("archive_floor", new Box(14, 19, -18, -15, -31, -29), "rotunda", "archive"),
            new Portal("archive_gallery", new Box(14, 19, -10, -8, -31, -29), "rotunda", "archive"),
            new Portal("core_level1", new Box(-1, 1, -18, -15, -48, -44), "rotunda", "core"),
            new Portal("core_level2", new Box(-1, 1, -28, -25, -48, -44), "core", "level2"),
            new Portal("core_level3", new Box(-1, 1, -38, -35, -48, -44), "core", "level3"));

    /** The whole plan's extent (local). */
    public static final int MIN_X = -46, MAX_X = 46, MIN_Y = -41, MAX_Y = 14, MIN_Z = -78, MAX_Z = 16;

    private Zones() {
    }

    public static Zone zone(String name) {
        for (Zone z : ALL) if (z.name().equals(name)) return z;
        throw new IllegalArgumentException(name);
    }

    public static Portal portal(String name) {
        for (Portal p : PORTALS) if (p.name().equals(name)) return p;
        throw new IllegalArgumentException(name);
    }

    /** Whether {@code zone} may write at a point: inside it, or inside one of its doorways. */
    public static boolean writable(Zone zone, int x, int y, int z) {
        if (zone.contains(x, y, z)) return true;
        for (Portal p : PORTALS) {
            if ((p.a().equals(zone.name()) || p.b().equals(zone.name())) && p.box().contains(x, y, z)) return true;
        }
        return false;
    }
}
