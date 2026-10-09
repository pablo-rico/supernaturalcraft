package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import java.util.ArrayList;
import java.util.List;

/**
 * Level 1's wings (pure). West of the war room is the library ({@link Level1Library}), after the series' set: a wide hall of
 * red-brown brick on a dark parquet. Two rows of square cream pillars with fluted shafts divide it into a nave and two aisles.
 * One long reading table runs down the nave under rows of globe pendants. Brass sconces sit on every pillar, and low bookcases
 * line every wall. A curtained arch at the far end holds the telescope. A mezzanine over the east end is reached from the war
 * room's gallery, with a flight down each aisle. East of the war room are the records room and, above it at gallery level, Henry's
 * study ({@link Level1East}).
 */
public final class Level1 {

    /** The four research desks, in the library's aisles, each with its chair and a free cell beside it. */
    public static final int[][] DESKS = {{-42, -18, -45}, {-35, -18, -45}, {-42, -18, -15}, {-35, -18, -15}};

    static final String DESK = "supernaturalcraft:research_desk", SHELF = "supernaturalcraft:archive_shelf";

    private Level1() {
    }

    public static void build(Plan p) {
        p.zone(Zones.LIBRARY);
        Level1Library.build(p);
        p.zone(Zones.ARCHIVE);
        Level1East.build(p);
        p.zone(null);
    }

    // --- shared furniture ---------------------------------------------------------------------------------------------------

    /** A chiseled bookshelf looking {@code facing}, its six slots filled by chance (the same every time). */
    static String chiseled(String facing, int x, int y, int z) {
        List<String> kv = new ArrayList<>(List.of("facing", facing));
        for (int i = 0; i < 6; i++) {
            kv.add("slot_" + i + "_occupied");
            kv.add(String.valueOf(Plan.noise(x, y, z, 50 + i) < 0.72));
        }
        return St.of("chiseled_bookshelf", kv.toArray(String[]::new));
    }

    /** A shelf of books: mostly plain bookshelves, some chiseled ones half-emptied, a few of the order's archive shelves. */
    static String books(String facing, int x, int y, int z) {
        double n = Plan.noise(x, y, z, 41);
        return n < 0.56 ? "minecraft:bookshelf" : n < 0.86 ? chiseled(facing, x, y, z) : SHELF;
    }

    /**
     * A straight run of bookcases from (xa, za) to (xb, zb) (one of the two axes fixed), {@code h} shelves from {@code y0}, fronts
     * looking {@code facing}, crowned with upside-down {@code cap} stairs whose backs lean on the wall behind.
     */
    static void bookcases(Plan p, int xa, int za, int xb, int zb, int y0, int h, String facing, String cap) {
        for (int x = Math.min(xa, xb); x <= Math.max(xa, xb); x++) {
            for (int z = Math.min(za, zb); z <= Math.max(za, zb); z++) {
                for (int y = y0; y < y0 + h; y++) p.set(x, y, z, books(facing, x, y, z));
                if (cap != null) p.set(x, y0 + h, z, St.stairs(cap, St.opposite(facing), true));
            }
        }
    }

    /** A table lamp with an amber shade: a brass stem (a lightning rod) under a shroomlight. */
    static void amberLamp(Plan p, int x, int y, int z) {
        p.set(x, y, z, St.of("lightning_rod", "facing", "up", "powered", "false", "waterlogged", "false"));
        p.set(x, y + 1, z, "minecraft:shroomlight");
    }

    static String barrel(String facing) {
        return St.of("barrel", "facing", facing, "open", "false");
    }

    static String pot(String facing) {
        return St.of("decorated_pot", "cracked", "false", "facing", facing, "waterlogged", "false");
    }
}
