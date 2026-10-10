package org.papiricoh.supernaturalcraft.buildkit;

import java.util.List;

/**
 * Walls with depth (pure). A flat wall reads as a primitive; a built one has a plinth with a skirting, pillars standing proud of
 * recessed panels, a belt course at each floor, a cornice under the eaves, or a timber frame of posts, beams and braces. Each
 * brush here works on a {@link Run}: a straight wall line and the side it faces.
 *
 * <p>Rows are absolute Y. A wall's plane is the run's cells; "out" cells are one step toward {@link Run#out}, "in" cells one step
 * the other way. Moulding runs (skirting, belts, cornices) extend one cell past both ends so that, on a closed outline, the
 * corner stairs meet and {@link Shapes} turns them into outer corners.
 */
public final class Walls {

    private Walls() {
    }

    /**
     * A straight wall line from (x0, z0) to (x1, z1), along X or along Z (inclusive), whose outside faces {@code out}
     * (perpendicular to the line).
     */
    public record Run(int x0, int z0, int x1, int z1, Dir out) {

        public Run {
            if (x0 != x1 && z0 != z1) throw new IllegalArgumentException("a run is straight: " + x0 + "," + z0 + " → " + x1 + "," + z1);
            if (!out.horizontal() || x0 != x1 && out.dx != 0 || z0 != z1 && out.dz != 0) {
                throw new IllegalArgumentException("out must be horizontal and perpendicular to the run: " + out);
            }
        }

        public int length() {
            return Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0)) + 1;
        }

        /** The direction from the first cell to the last. */
        public Dir along() {
            if (x1 > x0) return Dir.EAST;
            if (x1 < x0) return Dir.WEST;
            if (z1 > z0) return Dir.SOUTH;
            if (z1 < z0) return Dir.NORTH;
            return out.cw();
        }

        /** The i-th cell's x (i may run past the ends). */
        public int x(int i) {
            return x0 + along().dx * i;
        }

        public int z(int i) {
            return z0 + along().dz * i;
        }

        /** The run moved {@code steps} cells toward {@code out} (negative: inward). */
        public Run offset(int steps) {
            return new Run(x0 + out.dx * steps, z0 + out.dz * steps, x1 + out.dx * steps, z1 + out.dz * steps, out);
        }

        /** Cells {@code from..to} of this run as a run of its own. */
        public Run sub(int from, int to) {
            return new Run(x(from), z(from), x(to), z(to), out);
        }
    }

    /**
     * The four runs round a footprint (the wall cells' outline, inclusive), each facing out: north, east, south, west. Corner
     * cells belong to both runs that meet there.
     */
    public static List<Run> around(Box footprint) {
        int x0 = footprint.x0(), x1 = footprint.x1(), z0 = footprint.z0(), z1 = footprint.z1();
        return List.of(new Run(x0, z0, x1, z0, Dir.NORTH), new Run(x1, z0, x1, z1, Dir.EAST),
                new Run(x1, z1, x0, z1, Dir.SOUTH), new Run(x0, z1, x0, z0, Dir.WEST));
    }

    // --- surfaces -------------------------------------------------------------------------------------------------------------

    /** The wall plane from {@code y0} to {@code y1}. */
    public static void body(Canvas c, Run r, int y0, int y1, Brush brush) {
        for (int i = 0; i < r.length(); i++) for (int y = y0; y <= y1; y++) c.set(r.x(i), y, r.z(i), brush.at(r.x(i), y, r.z(i)));
    }

    /** Closed walls round a footprint (every run's plane). */
    public static void enclose(Canvas c, Box footprint, int y0, int y1, Brush brush) {
        for (Run r : around(footprint)) body(c, r, y0, y1, brush);
    }

    /**
     * A plinth: the wall plane's bottom {@code rows} rows in the family's block, and a skirting of bottom stairs in front of it
     * at {@code y0} (backs to the wall). Extends one cell past each end so closed outlines turn their corners.
     */
    public static void plinth(Canvas c, Run r, int y0, int rows, Family f, boolean skirting) {
        body(c, r, y0, y0 + rows - 1, Brush.of(f.block()));
        if (!skirting || !f.hasStairs()) return;
        Run o = r.offset(1);
        for (int i = -1; i <= r.length(); i++) c.setIfAir(o.x(i), y0, o.z(i), f.stairs(r.out().opposite(), false));
    }

    /**
     * A belt course at {@code y}: upside-down stairs one cell out, backs to the wall (a ledge with a chamfered underside), over
     * the whole run and one past each end. Never overwrites pillars or anything else already standing out.
     */
    public static void belt(Canvas c, Run r, int y, Family f) {
        Run o = r.offset(1);
        for (int i = -1; i <= r.length(); i++) c.setIfAir(o.x(i), y, o.z(i), f.stairs(r.out().opposite(), true));
    }

    /** A flat belt: top slabs one cell out (a thin shadow line between storeys). */
    public static void flatBelt(Canvas c, Run r, int y, Family f) {
        Run o = r.offset(1);
        for (int i = -1; i <= r.length(); i++) c.setIfAir(o.x(i), y, o.z(i), f.slabTop());
    }

    /**
     * A cornice at the wall's top row {@code y}: the plane's top row in {@code f}'s block, upside-down stairs one cell out at
     * {@code y}, and dentils under them ({@code dentil}: e.g. a button or a trapdoor state; null for none) every other cell.
     */
    public static void cornice(Canvas c, Run r, int y, Family f, String dentil) {
        for (int i = 0; i < r.length(); i++) c.set(r.x(i), y, r.z(i), f.block());
        Run o = r.offset(1);
        for (int i = -1; i <= r.length(); i++) {
            c.setIfAir(o.x(i), y, o.z(i), f.stairs(r.out().opposite(), true));
            if (dentil != null && i >= 0 && i < r.length() && Math.floorMod(i, 2) == 0) c.setIfAir(o.x(i), y - 1, o.z(i), dentil);
        }
    }

    // --- pillars and panels -----------------------------------------------------------------------------------------------------

    /** Where a run puts its pillars: both ends and every {@code every} cells between, spread evenly. */
    public static int[] pillarPositions(Run r, int every) {
        int n = r.length();
        int bays = Math.max(1, Math.round((n - 1) / (float) every));
        int[] out = new int[bays + 1];
        for (int b = 0; b <= bays; b++) out[b] = Math.round(b * (n - 1) / (float) bays);
        return out;
    }

    /**
     * Pillars standing one cell proud of the wall plane, at {@link #pillarPositions}, from {@code y0} to {@code y1}: a base block
     * with stairs flaring at its foot, a {@code shaft}, and a capital block with upside-down stairs flaring under the next row.
     * {@code ends}: whether pillars stand at the run's two ends too (false when the corners get their own).
     */
    public static void pillars(Canvas c, Run r, int y0, int y1, int every, Brush shaft, Family base, String capital, boolean ends) {
        Run o = r.offset(1);
        int[] at = pillarPositions(r, every);
        for (int k = 0; k < at.length; k++) {
            if (!ends && (k == 0 || k == at.length - 1)) continue;
            int i = at[k], x = o.x(i), z = o.z(i);
            for (int y = y0; y <= y1; y++) {
                String s = y == y0 ? base.block() : y == y1 && capital != null ? capital : shaft.at(x, y, z);
                c.set(x, y, z, s);
            }
            if (base.hasStairs()) {
                for (Dir side : new Dir[]{r.along(), r.along().opposite()}) {
                    c.setIfAir(x + side.dx, y0, z + side.dz, base.stairs(side.opposite(), false));
                    c.setIfAir(x + side.dx, y1, z + side.dz, base.stairs(side.opposite(), true));
                }
                c.setIfAir(x + r.out().dx, y0, z + r.out().dz, base.stairs(r.out().opposite(), false));
                c.setIfAir(x + r.out().dx, y1, z + r.out().dz, base.stairs(r.out().opposite(), true));
            }
        }
    }

    /**
     * Corner posts standing proud on the outline's outer corners (the diagonal cell outside each corner), from {@code y0} to
     * {@code y1}, plus the two cells beside it so the post reads 2 wide from each face: a quoin.
     */
    public static void quoins(Canvas c, Box footprint, int y0, int y1, Brush post) {
        int[][] corners = {{footprint.x0() - 1, footprint.z0() - 1}, {footprint.x1() + 1, footprint.z0() - 1},
                {footprint.x0() - 1, footprint.z1() + 1}, {footprint.x1() + 1, footprint.z1() + 1}};
        for (int[] k : corners) for (int y = y0; y <= y1; y++) c.set(k[0], y, k[1], post.at(k[0], y, k[1]));
    }

    /**
     * Recessed panels: between the given pillar positions, rows {@code y0..y1} of the plane are set back by one (the plane cell
     * becomes {@code panel} one cell in, the plane cell air) — the shadow line round every panel is the point. The inner cells
     * must be wall (a two-thick wall) or this opens the room.
     */
    public static void recess(Canvas c, Run r, int y0, int y1, int[] pillarsAt, Brush panel) {
        Run in = r.offset(-1);
        for (int k = 0; k + 1 < pillarsAt.length; k++) {
            for (int i = pillarsAt[k] + 1; i < pillarsAt[k + 1]; i++) {
                for (int y = y0; y <= y1; y++) {
                    c.carve(r.x(i), y, r.z(i));
                    c.set(in.x(i), y, in.z(i), panel.at(in.x(i), y, in.z(i)));
                }
            }
        }
    }

    /**
     * Panels framed in the plane itself: between pillar positions, the border cells of each bay (top and bottom rows, the cells
     * beside the pillars) are {@code frame}, the middle {@code panel}. Cheap depth for one-thick walls when combined with pillars.
     */
    public static void framedPanels(Canvas c, Run r, int y0, int y1, int[] pillarsAt, Brush frame, Brush panel) {
        for (int k = 0; k + 1 < pillarsAt.length; k++) {
            int a = pillarsAt[k] + 1, b = pillarsAt[k + 1] - 1;
            for (int i = a; i <= b; i++) {
                for (int y = y0; y <= y1; y++) {
                    boolean edge = y == y0 || y == y1 || i == a || i == b;
                    int x = r.x(i), z = r.z(i);
                    c.set(x, y, z, (edge ? frame : panel).at(x, y, z));
                }
            }
        }
    }

    // --- timber framing -----------------------------------------------------------------------------------------------------

    /**
     * A timber-framed storey on a run, from sill row {@code y0} to plate row {@code y1}: log posts at {@link #pillarPositions}
     * (every {@code postEvery}), log beams along the run at the sill and the plate (and at {@code midRail} when it is between
     * them; pass a value outside to skip), {@code infill} (plaster) between, and knee braces of {@code braceStairs} in the top
     * corners of each bay when {@code braces}.
     */
    public static void timberFrame(Canvas c, Run r, int y0, int y1, Wood wood, Brush infill, int postEvery, int midRail,
                                   boolean braces, String braceStairs) {
        String axisAlong = r.along().axis();
        int[] posts = pillarPositions(r, postEvery);
        java.util.Set<Integer> postSet = new java.util.HashSet<>();
        for (int p : posts) postSet.add(p);
        for (int i = 0; i < r.length(); i++) {
            int x = r.x(i), z = r.z(i);
            for (int y = y0; y <= y1; y++) {
                String s;
                if (postSet.contains(i)) s = wood.log("y");
                else if (y == y0 || y == y1 || y == midRail) s = wood.log(axisAlong);
                else s = infill.at(x, y, z);
                c.set(x, y, z, s);
            }
        }
        if (!braces || braceStairs == null) return;
        for (int k = 0; k + 1 < posts.length; k++) {
            int a = posts[k] + 1, b = posts[k + 1] - 1;
            if (b - a < 2) continue;
            int y = (midRail > y0 && midRail < y1 ? midRail : y1) - 1;
            if (y <= y0) continue;
            // A stair seen side-on is an L: back to the post, top to the beam — a knee brace.
            c.set(r.x(a), y, r.z(a), St.stairs(braceStairs, r.along().opposite(), true));
            c.set(r.x(b), y, r.z(b), St.stairs(braceStairs, r.along(), true));
        }
    }

    /** Log posts at an outline's four corners from {@code y0} to {@code y1} (timber frames, porches). */
    public static void cornerPosts(Canvas c, Box footprint, int y0, int y1, String logState) {
        int[][] corners = {{footprint.x0(), footprint.z0()}, {footprint.x1(), footprint.z0()}, {footprint.x0(), footprint.z1()}, {footprint.x1(), footprint.z1()}};
        for (int[] k : corners) for (int y = y0; y <= y1; y++) c.set(k[0], y, k[1], logState);
    }

    /** A horizontal log beam along a run at {@code y} (axis along the run), e.g. a jetty's bressummer. */
    public static void beam(Canvas c, Run r, int y, Wood wood, boolean stripped) {
        String axis = r.along().axis();
        for (int i = 0; i < r.length(); i++) c.set(r.x(i), y, r.z(i), stripped ? wood.strippedLog(axis) : wood.log(axis));
    }

    /**
     * A jetty: the upper storey's run one cell out over the lower one, carried on joist ends: at {@code y} (the upper floor row)
     * the out cells are a beam, and under it, every other cell, upside-down stairs as brackets.
     */
    public static void jetty(Canvas c, Run r, int y, Wood wood) {
        Run o = r.offset(1);
        String axis = r.along().axis();
        for (int i = -1; i <= r.length(); i++) {
            c.set(o.x(i), y, o.z(i), wood.log(axis));
            if (Math.floorMod(i, 2) == 0) c.setIfAir(o.x(i), y - 1, o.z(i), wood.stairs(r.out().opposite(), true));
        }
    }
}
