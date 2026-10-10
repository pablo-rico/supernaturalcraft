package org.papiricoh.supernaturalcraft.buildkit;

/**
 * Windows, doors and porches cut into a {@link Walls.Run} (pure). An opening is where a build shows craft: a sill that stands out,
 * a lintel or a hood, shutters, a flower box, panes set back into a deep reveal, a door in a frame under an arch with a step.
 * Positions are along the run ({@code i} = cell index from its start); rows are absolute Y.
 */
public final class Openings {

    private Openings() {
    }

    /**
     * How a window is dressed. Any part may be null.
     *
     * @param pane      the glass ({@code minecraft:glass_pane}, a stained pane, or a full glass block id)
     * @param frame     the jambs' block (the plane cells either side and the lintel row); null keeps the wall
     * @param sill      upside-down stairs one cell out under the window (a protruding sill)
     * @param hood      a drip hood over the window: upside-down stairs one cell out above it
     * @param shutters  a trapdoor id: open shutters flat on the wall either side
     * @param flowerBox soil block for a box under the sill (with trapdoor sides, flowers on top), replaces the sill
     * @param deep      when the wall is two thick, the pane goes in the inner layer (a deep reveal) and the outer is carved
     * @param mullion   for windows 3+ wide: a block in the plane splitting it in two (null: one opening)
     */
    public record WindowStyle(String pane, Brush frame, Family sill, Family hood, String shutters, String flowerBox, boolean deep, String mullion) {

        public WindowStyle withShutters(String trapdoor) {
            return new WindowStyle(pane, frame, sill, hood, trapdoor, flowerBox, deep, mullion);
        }

        public WindowStyle withFlowerBox(String soil) {
            return new WindowStyle(pane, frame, sill, hood, shutters, soil, deep, mullion);
        }
    }

    /**
     * A window {@code width} cells from run cell {@code i}, glass rows {@code y0..y0+height-1}. Draw it after the wall it cuts.
     * Flower boxes plant a weighted pick of small flowers on their soil.
     */
    public static void window(Canvas c, Walls.Run r, int i, int y0, int width, int height, WindowStyle s) {
        Walls.Run o = r.offset(1), in = r.offset(-1);
        int mid = width >= 3 && s.mullion() != null ? width / 2 : -1;
        boolean fullGlass = !s.pane().endsWith("_pane");
        for (int k = 0; k < width; k++) {
            int x = r.x(i + k), z = r.z(i + k);
            for (int y = y0; y < y0 + height; y++) {
                if (k == mid) {
                    c.set(x, y, z, s.mullion());
                    continue;
                }
                String glass = fullGlass ? s.pane() : St.pane(s.pane());
                if (s.deep()) {
                    c.carve(x, y, z);
                    c.set(in.x(i + k), y, in.z(i + k), glass);
                } else {
                    c.set(x, y, z, glass);
                }
            }
        }
        if (s.frame() != null) {
            for (int y = y0 - 1; y <= y0 + height; y++) {
                for (int k : new int[]{-1, width}) c.set(r.x(i + k), y, r.z(i + k), s.frame().at(r.x(i + k), y, r.z(i + k)));
            }
            for (int k = 0; k < width; k++) {
                c.set(r.x(i + k), y0 + height, r.z(i + k), s.frame().at(r.x(i + k), y0 + height, r.z(i + k)));
                c.set(r.x(i + k), y0 - 1, r.z(i + k), s.frame().at(r.x(i + k), y0 - 1, r.z(i + k)));
            }
        }
        if (s.flowerBox() != null) {
            for (int k = 0; k < width; k++) {
                int x = o.x(i + k), z = o.z(i + k);
                c.set(x, y0 - 1, z, s.flowerBox());
                c.setIfAir(x + r.out().dx, y0 - 1, z + r.out().dz, St.trapdoorAgainst(trapdoorFor(s), r.out().opposite()));
                c.setIfAir(x, y0 - 2, z, St.trapdoor(trapdoorFor(s), r.out().opposite(), true, false));
                double n = Noise.hash01(x, y0, z, 811);
                String flower = n < 0.25 ? "minecraft:poppy" : n < 0.45 ? "minecraft:azure_bluet" : n < 0.65 ? "minecraft:oxeye_daisy"
                        : n < 0.8 ? "minecraft:cornflower" : n < 0.9 ? "minecraft:fern" : "minecraft:red_tulip";
                c.setIfAir(x, y0, z, flower);
            }
            for (int k : new int[]{-1, width}) {
                Dir side = k < 0 ? r.along().opposite() : r.along();
                c.setIfAir(o.x(i + k), y0 - 1, o.z(i + k), St.trapdoorAgainst(trapdoorFor(s), side.opposite()));
            }
        } else if (s.sill() != null) {
            for (int k = -1; k <= width; k++) c.setIfAir(o.x(i + k), y0 - 1, o.z(i + k), s.sill().stairs(r.out().opposite(), true));
        }
        if (s.hood() != null) {
            for (int k = -1; k <= width; k++) c.setIfAir(o.x(i + k), y0 + height, o.z(i + k), s.hood().stairs(r.out().opposite(), true));
        }
        if (s.shutters() != null) {
            for (int y = y0; y < y0 + height; y++) {
                c.setIfAir(o.x(i - 1), y, o.z(i - 1), St.trapdoorAgainst(s.shutters(), r.out().opposite()));
                c.setIfAir(o.x(i + width), y, o.z(i + width), St.trapdoorAgainst(s.shutters(), r.out().opposite()));
            }
        }
    }

    private static String trapdoorFor(WindowStyle s) {
        return s.shutters() != null ? s.shutters() : "minecraft:spruce_trapdoor";
    }

    /**
     * How a door is dressed.
     *
     * @param door   the door id
     * @param frame  jambs and head (the plane cells round the opening); null keeps the wall
     * @param arch   stairs id: upside-down stairs in the head's inner corners (only for openings 2+ wide), null for square
     * @param step   a family whose bottom slab (or stairs, when the floor is one up) makes a step one cell out; null for none
     * @param canopy a family for a little hood over the door (upside-down stairs one cell out), null for none
     * @param lights a wall lantern beside the door: lantern standing on a trapdoor bracket, or null
     */
    public record DoorStyle(String door, Brush frame, String arch, Family step, Family canopy, String lights) {
    }

    /**
     * A door at run cell {@code i}, {@code width} 1 or 2 (a double door), its lower half at {@code y}; it opens outward toward
     * {@code r.out} (its {@code facing}: one looks out through it). The outer cell in front is left clear (and gets the step).
     */
    public static void door(Canvas c, Walls.Run r, int i, int y, int width, DoorStyle s) {
        Walls.Run o = r.offset(1);
        for (int k = 0; k < width; k++) {
            int x = r.x(i + k), z = r.z(i + k);
            boolean rightHinge = width == 2 ? k == 1 : false;
            if (width == 2 && r.along() != r.out().cw()) rightHinge = !rightHinge;
            c.setPair(x, y, z, St.door(s.door(), r.out(), rightHinge, false));
            c.carve(o.x(i + k), y, o.z(i + k));
            c.carve(o.x(i + k), y + 1, o.z(i + k));
        }
        if (s.frame() != null) {
            for (int yy = y; yy <= y + 2; yy++) {
                for (int k : new int[]{-1, width}) c.set(r.x(i + k), yy, r.z(i + k), s.frame().at(r.x(i + k), yy, r.z(i + k)));
            }
            for (int k = 0; k < width; k++) c.set(r.x(i + k), y + 2, r.z(i + k), s.frame().at(r.x(i + k), y + 2, r.z(i + k)));
        }
        if (s.step() != null) {
            for (int k = 0; k < width; k++) {
                int x = o.x(i + k), z = o.z(i + k);
                if (c.isAir(x, y - 1, z) && !c.isAir(x, y - 2, z)) c.set(x, y - 1, z, s.step().block());
            }
        }
        if (s.canopy() != null) {
            for (int k = -1; k <= width; k++) c.setIfAir(o.x(i + k), y + 2, o.z(i + k), s.canopy().stairs(r.out().opposite(), true));
        }
        if (s.lights() != null) {
            for (int k : new int[]{-2, width + 1}) {
                int x = o.x(i + k), z = o.z(i + k);
                if (c.isAir(x, y + 1, z) && c.isAir(x, y + 2, z)) {
                    c.set(x, y + 1, z, St.trapdoor(s.lights(), r.out().opposite(), true, false));
                    c.set(x, y + 2, z, St.lantern(false));
                }
            }
        }
    }

    /** An opening with an arch: carved {@code width} × {@code height} from {@code y}, upside-down stairs rounding its top corners. */
    public static void archway(Canvas c, Walls.Run r, int i, int y, int width, int height, String stairs) {
        for (int k = 0; k < width; k++) for (int yy = y; yy < y + height; yy++) c.carve(r.x(i + k), yy, r.z(i + k));
        if (width >= 3 && stairs != null) {
            c.set(r.x(i), y + height - 1, r.z(i), St.stairs(stairs, r.along().opposite(), true));
            c.set(r.x(i + width - 1), y + height - 1, r.z(i + width - 1), St.stairs(stairs, r.along(), true));
        }
    }

    /**
     * How a porch is built: {@code deck} floor brush, {@code post} state for the posts (a fence, a wall or a log), {@code railing}
     * fence id (null: open), a post every {@code postEvery} cells, a {@code roof} family for the shed roof (null: none),
     * {@code steps} family for the steps down at the entry, {@code header} the beam state along the front under the roof (a log
     * along the run is good; null for none).
     */
    public record PorchStyle(Brush deck, String post, String railing, int postEvery, Family roof, Family steps, Wood header) {
    }

    /**
     * A porch in front of run cells {@code from..to}, {@code depth} deep: its deck at {@code floorY}, posts up to {@code topY - 1},
     * a header beam at {@code topY}, railings between posts except over the entry {@code gapFrom..gapTo} (run indices), a step
     * down at the entry when the deck stands above the ground, and a half-pitch shed roof from {@code topY + 1} rising toward
     * the wall with a one-cell overhang all round.
     */
    public static void porch(Canvas c, Walls.Run r, int from, int to, int depth, int floorY, int topY, int gapFrom, int gapTo, PorchStyle s) {
        for (int i = from; i <= to; i++) {
            for (int d = 1; d <= depth; d++) {
                int x = r.x(i) + r.out().dx * d, z = r.z(i) + r.out().dz * d;
                c.set(x, floorY, z, s.deck().at(x, floorY, z));
                for (int y = floorY + 1; y <= topY; y++) c.setIfEmpty(x, y, z, St.AIR);
            }
        }
        Walls.Run front = r.offset(depth);
        String axis = r.along().axis();
        for (int i = from; i <= to; i++) {
            int x = front.x(i), z = front.z(i);
            boolean isPost = i == from || i == to || s.postEvery() > 0 && (i - from) % s.postEvery() == 0;
            boolean gap = i >= gapFrom && i <= gapTo;
            if (isPost) {
                for (int y = floorY + 1; y < topY; y++) c.set(x, y, z, s.post());
            } else if (!gap && s.railing() != null) {
                c.set(x, floorY + 1, z, St.fence(s.railing()));
            }
            if (s.header() != null) c.set(x, topY, z, s.header().log(axis));
        }
        for (int d = 1; d < depth; d++) {
            for (int i : new int[]{from, to}) {
                int x = r.x(i) + r.out().dx * d, z = r.z(i) + r.out().dz * d;
                if (s.railing() != null) c.set(x, floorY + 1, z, St.fence(s.railing()));
                if (s.header() != null) c.set(x, topY, z, s.header().log(r.out().axis()));
            }
        }
        if (s.roof() != null) {
            for (int i = from - 1; i <= to + 1; i++) {
                for (int d = 1; d <= depth + 1; d++) {
                    int x = r.x(i) + r.out().dx * d, z = r.z(i) + r.out().dz * d;
                    int e = depth + 1 - d;
                    if (e % 2 == 0) c.set(x, topY + 1 + e / 2, z, s.roof().stairs(r.out().opposite(), false));
                    else c.set(x, topY + 1 + e / 2, z, s.roof().slabTop());
                }
            }
        }
        if (s.steps() != null) {
            for (int i = gapFrom; i <= gapTo; i++) {
                int x = front.x(i) + r.out().dx, z = front.z(i) + r.out().dz;
                if (c.isAir(x, floorY, z) && !c.isAir(x, floorY - 1, z)) c.set(x, floorY, z, s.steps().stairs(r.out().opposite(), false));
            }
        }
    }
}
