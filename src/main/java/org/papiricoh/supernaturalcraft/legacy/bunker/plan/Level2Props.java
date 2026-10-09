package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

/**
 * The small fittings of the lower levels (pure): block states for the furniture levels 2 and 3 share (drawers, pots, rods,
 * levers, skulls…) and a few brushes (beds, doors, rugs, paintings by the cells they cover, item frames). Every state is
 * given in full, as the builder places it without block updates and the GameTest compares it block by block.
 */
final class Level2Props {

    private Level2Props() {
    }

    // --- states ---------------------------------------------------------------------------------------------------------------

    static String barrel(String facing) {
        return St.of("barrel", "facing", facing, "open", "false");
    }

    static String chest(String facing) {
        return St.of("chest", "facing", facing, "type", "single", "waterlogged", "false");
    }

    static String pot(String facing) {
        return St.of("decorated_pot", "cracked", "false", "facing", facing, "waterlogged", "false");
    }

    /** A chiseled bookshelf facing out, its six slots filled by the bits of {@code mask} (0: an empty chest of drawers). */
    static String books(String facing, int mask) {
        String[] kv = new String[14];
        kv[0] = "facing";
        kv[1] = facing;
        for (int i = 0; i < 6; i++) {
            kv[2 + 2 * i] = "slot_" + i + "_occupied";
            kv[3 + 2 * i] = String.valueOf((mask >> i & 1) == 1);
        }
        return St.of("chiseled_bookshelf", kv);
    }

    static String rod(String facing) {
        return St.of("lightning_rod", "facing", facing, "powered", "false", "waterlogged", "false");
    }

    static String endRod(String facing) {
        return St.of("end_rod", "facing", facing);
    }

    static String lever(String face, String facing) {
        return St.of("lever", "face", face, "facing", facing, "powered", "false");
    }

    /** A tripwire hook: a tap on a wall ({@code facing} out of it). */
    static String hook(String facing) {
        return St.of("tripwire_hook", "attached", "false", "facing", facing, "powered", "false");
    }

    static String skull(String id, int rotation) {
        return St.of(id, "powered", "false", "rotation", String.valueOf(rotation));
    }

    static String water(int level) {
        return St.of("water_cauldron", "level", String.valueOf(level));
    }

    static String top(String slab) {
        return St.slab(slab, true);
    }

    static String bottom(String slab) {
        return St.slab(slab, false);
    }

    static String oven(String id, String facing) {
        return St.of(id, "facing", facing, "lit", "false");
    }

    static String noteBlock() {
        return St.of("note_block", "instrument", "harp", "note", "0", "powered", "false");
    }

    static String jukebox() {
        return St.of("jukebox", "has_record", "false");
    }

    static String lectern(String facing, boolean book) {
        return St.of("lectern", "facing", facing, "has_book", String.valueOf(book), "powered", "false");
    }

    /** A brewing stand. Planned empty whatever is asked: its block entity holds no bottles and would clear them on its first tick. */
    static String brewing(boolean a, boolean b, boolean c) {
        return St.of("brewing_stand", "has_bottle_0", "false", "has_bottle_1", "false", "has_bottle_2", "false");
    }

    static String amethyst(String facing) {
        return St.of("amethyst_cluster", "facing", facing, "waterlogged", "false");
    }

    static String plate(String id) {
        return id.contains("weighted") ? St.of(id, "power", "0") : St.of(id, "powered", "false");
    }

    // --- brushes ------------------------------------------------------------------------------------------------------------

    static void frame(Plan p, int x, int y, int z, String facing, String item) {
        p.decor(Decor.frame(x, y, z, facing, item));
    }

    static void glow(Plan p, int x, int y, int z, String facing, String item) {
        p.decor(Decor.glowFrame(x, y, z, facing, item));
    }

    /** A bed whose foot is at {@code (x, z)} and whose head lies toward {@code toHead}. */
    static void bed(Plan p, int x, int y, int z, String color, String toHead) {
        String[] b = St.bed(color, toHead);
        p.set(x, y, z, b[0]);
        p.set(x + St.dx(toHead), y, z + St.dz(toHead), b[1]);
    }

    /** A door's two halves (lower at {@code y}). */
    static void door(Plan p, int x, int y, int z, String id, String facing, String hinge, boolean open) {
        String[] d = St.door(id, facing, hinge, open);
        p.set(x, y, z, d[0]);
        p.set(x, y + 1, z, d[1]);
    }

    /** A rug of carpet on a rectangle, a border round a field, laid only where the floor cell is still air. */
    static void rug(Plan p, int x0, int x1, int y, int z0, int z1, String border, String fill) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!p.isAir(x, y, z)) continue;
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                p.set(x, y, z, St.carpet(edge ? border : fill));
            }
        }
    }

    /**
     * A painting on the wall cells in front of the plane at {@code (x or z fixed)}: given the lowest coordinate along the wall
     * ({@code alongMin}) and the lowest y it should cover, finds the anchor vanilla centres it on.
     */
    static void painting(Plan p, int x, int z, String facing, String variant, int alongMin, int yMin) {
        int[] size = Decor.PAINTINGS.get(variant);
        String along = St.ccw(facing);
        int d = St.dx(along) + St.dz(along);
        int anchor = d > 0 ? alongMin + (size[0] - 1) / 2 : alongMin + size[0] / 2;
        int y = yMin + (size[1] - 1) / 2;
        boolean alongX = St.dx(along) != 0;
        p.decor(Decor.painting(alongX ? anchor : x, y, alongX ? z : anchor, facing, variant));
    }

    /** A pendant with a green enamel shade: chains from the ceiling, a closed trapdoor (the shade) and the lantern under it. */
    static void shadedPendant(Plan p, int x, int ceiling, int z, int drop, String shade) {
        for (int y = ceiling - 1; y > ceiling - drop + 1; y--) p.set(x, y, z, St.chain("y"));
        p.set(x, ceiling - drop + 1, z, St.trapdoor(shade, "north", false, false));
        p.set(x, ceiling - drop, z, St.lantern(true));
    }
}
