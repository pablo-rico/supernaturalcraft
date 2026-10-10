package org.papiricoh.supernaturalcraft.buildkit;

/**
 * Light that is part of the build (pure): hidden sources under carpets and behind grates, chains of lanterns, candles, sconces,
 * lamp posts and chandeliers. Every fixture here is supported the way vanilla needs (lanterns hang from something or stand on
 * something; candles stand on a sturdy top). {@code y} is the cell the fixture occupies unless said otherwise.
 */
public final class Lighting {

    private Lighting() {
    }

    /** A light block in the floor at {@code y - 1} with a carpet over it at {@code y}: an even glow with no visible source. */
    public static void underCarpet(Canvas c, int x, int y, int z, String lightBlock, String carpetColor) {
        c.set(x, y - 1, z, lightBlock);
        c.set(x, y, z, St.carpet(carpetColor));
    }

    /** As {@link #underCarpet} with moss carpet (gardens, ruins). */
    public static void underMoss(Canvas c, int x, int y, int z, String lightBlock) {
        c.set(x, y - 1, z, lightBlock);
        c.set(x, y, z, St.mossCarpet());
    }

    /** The invisible light block in an air cell (a fill light where no fixture fits). */
    public static void invisible(Canvas c, int x, int y, int z, int level) {
        c.setIfAir(x, y, z, St.light(level));
    }

    /** A light behind a grate, set into a wall or ceiling (the grate is a see-through full cube). */
    public static void behindGrate(Canvas c, int x, int y, int z, Dir out, String lightBlock, String grate) {
        c.set(x, y, z, lightBlock);
        c.set(x + out.dx, y + out.dy, z + out.dz, grate);
    }

    /** A lantern hanging {@code drop} cells under a ceiling block at {@code ceilingY} (chains above it). */
    public static void hanging(Canvas c, int x, int ceilingY, int z, int drop, boolean soul) {
        for (int y = ceilingY - 1; y > ceilingY - drop; y--) c.set(x, y, z, St.chain("y"));
        c.set(x, ceilingY - drop, z, soul ? St.soulLantern(true) : St.lantern(true));
    }

    /** Candles standing at (x, y, z) (on a sturdy top). */
    public static void candles(Canvas c, int x, int y, int z, String color, int count) {
        c.set(x, y, z, St.candle(color, count, true));
    }

    /** A sconce: a top-half trapdoor bracket at {@code y} against the wall behind it ({@code out} away from it) and a lantern on it. */
    public static void sconce(Canvas c, int x, int y, int z, Dir out, String trapdoor, boolean soul) {
        c.set(x, y, z, St.trapdoor(trapdoor, out.opposite(), true, false));
        c.set(x, y + 1, z, soul ? St.soulLantern(false) : St.lantern(false));
    }

    /**
     * A lamp post standing on the ground at (x, y, z): a base block, a {@code post} column ({@code height} cells, a fence or a
     * wall id), and either a lantern on top or, with {@code arm} set, a fence arm toward {@code arm} with a lantern hanging from
     * its end.
     */
    public static void lampPost(Canvas c, int x, int y, int z, int height, String base, String post, Dir arm, boolean soul) {
        c.set(x, y, z, base);
        String p = post.endsWith("_wall") ? St.wall(post) : St.fence(post);
        for (int h = 1; h <= height; h++) c.set(x, y + h, z, p);
        int top = y + height;
        if (arm == null) {
            c.set(x, top + 1, z, soul ? St.soulLantern(false) : St.lantern(false));
        } else {
            String f = post.endsWith("_wall") ? St.fence("minecraft:dark_oak_fence") : St.fence(post);
            c.set(x + arm.dx, top, z + arm.dz, f);
            c.set(x + arm.dx, top - 1, z + arm.dz, soul ? St.soulLantern(true) : St.lantern(true));
            if (post.endsWith("_wall")) c.set(x, top + 1, z, St.slabBottom(post.replace("_wall", "_slab")));
        }
    }

    /**
     * A chandelier hanging from a ceiling block at {@code ceilingY}: a chain {@code drop - 1} long, a fence cross at the bottom
     * with candles on its arms and a lantern hanging under the middle.
     */
    public static void chandelier(Canvas c, int x, int ceilingY, int z, int drop, String fence, String candleColor) {
        int y = ceilingY - drop;
        for (int yy = ceilingY - 1; yy > y; yy--) c.set(x, yy, z, St.chain("y"));
        c.set(x, y, z, St.fence(fence));
        for (Dir d : Dir.HORIZONTAL) {
            c.set(x + d.dx, y, z + d.dz, St.fence(fence));
            c.set(x + d.dx, y + 1, z + d.dz, St.candle(candleColor, 1 + Noise.pick(x + d.dx, y, z + d.dz, 41, 3), true));
        }
        c.set(x, y - 1, z, St.lantern(true));
    }
}
