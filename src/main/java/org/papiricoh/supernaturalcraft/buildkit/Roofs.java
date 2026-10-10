package org.papiricoh.supernaturalcraft.buildkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Roofs as a builder lays them (pure): rows of stairs with their backs to the ridge, half-steps of stairs and top slabs for a
 * lower pitch, a one-block overhang with a fascia under the eaves, barge boards of a contrasting trim on the rakes, a ridge cap,
 * gable ends filled with a contrasting material, sloped ceilings inside, dormers and chimneys.
 *
 * <h2>How it works</h2>
 * A {@link Plan} holds roof volumes. Each volume is a wall outline ({@code footprint}, the wall cells) with its wall top at
 * {@code wallTop}; its roof starts at {@code wallTop + 1} over the wall plane and hangs {@code overhang} cells past it. For
 * each column the volume computes {@code d}, the distance in from the eave edge, and its {@link Pitch} turns that into pieces
 * (a stair, a top slab, a full block) at heights over the wall top. Where volumes overlap (cross gables, dormers, an L-shaped
 * house), the higher roof surface wins the column: valleys and intersections come out by themselves, and {@link Shapes} turns
 * the stairs' corners (hips, valleys) when the canvas is finished.
 *
 * <p>Draw a roof after the walls (it fills the gable triangles over them) and before chimneys.
 */
public final class Roofs {

    private Roofs() {
    }

    /** Which way a volume slopes. */
    public enum Shape {
        /** Two slopes facing north and south, ridge along X, gable ends east and west. */
        GABLE_X,
        /** Two slopes facing east and west, ridge along Z, gable ends north and south. */
        GABLE_Z,
        /** Four slopes meeting at hips. */
        HIP,
        /** One slope rising toward the volume's {@code high} side. */
        SHED
    }

    /**
     * The roof's profile: {@link #STEEP} 45° stairs; {@link #HALF} ≈27°, a stair then a top slab per block of rise; {@link #LOW}
     * ≈18°, a stair then two top slabs; {@link #MANSARD} a steep lower part (two stairs per row) for {@code lowerRows} rows, then
     * half pitch (a gambrel when used on a gable, a mansard on a hip).
     */
    public enum Pitch {STEEP, HALF, LOW, MANSARD}

    /** What a piece of roof is. */
    public enum Kind {STAIR, SLAB_TOP, FULL}

    public record Piece(Kind kind, int y) {
    }

    /**
     * How a roof is finished.
     *
     * @param surface   the roof's stairs, slabs and block
     * @param trim      barge boards on the rakes, the ridge cap and the soffit under the rakes
     * @param infill    the gable triangles and anything between the wall tops and the roof
     * @param overhang  cells past the walls (1 is the norm, 0 for none)
     * @param pitch     the profile
     * @param lowerRows rows of the steep part ({@link Pitch#MANSARD} only)
     * @param ridgeCap  a row of trim slabs along the ridge
     * @param fascia    a trapdoor id hung under the eave row (a board along the eaves), or null
     * @param ceiling   sloped ceilings: upside-down stairs under the roof's stairs inside the walls
     */
    public record Style(Family surface, Family trim, Brush infill, int overhang, Pitch pitch, int lowerRows, boolean ridgeCap,
                        String fascia, boolean ceiling) {

        public Style withPitch(Pitch p) {
            return new Style(surface, trim, infill, overhang, p, lowerRows, ridgeCap, fascia, ceiling);
        }

        public Style withOverhang(int o) {
            return new Style(surface, trim, infill, o, pitch, lowerRows, ridgeCap, fascia, ceiling);
        }

        public Style withInfill(Brush b) {
            return new Style(surface, trim, b, overhang, pitch, lowerRows, ridgeCap, fascia, ceiling);
        }
    }

    /**
     * A roof volume (see {@link Plan}). {@code high}: the side a {@link Shape#SHED} rises to, or the way a dormer faces;
     * {@code wallFrom}: draws its own walls from that row (dormers: cheeks and front; the back is left open).
     */
    public record Volume(Box footprint, int wallTop, Shape shape, Dir high, Style style, Integer wallFrom, Brush wall) {
    }

    /** One column's share of a volume. */
    public record Column(Volume volume, int d, int e, Dir up, boolean ridge, boolean rake, boolean perimeter, boolean eave, List<Piece> pieces) {

        /** The top of the surface in half blocks (2 per block). */
        public int top() {
            int t = Integer.MIN_VALUE;
            for (Piece p : pieces) t = Math.max(t, 2 * (volume.wallTop() + 1 + p.y()) + 2);
            return t;
        }

        public int bottomY() {
            int b = Integer.MAX_VALUE;
            for (Piece p : pieces) b = Math.min(b, volume.wallTop() + 1 + p.y());
            return b;
        }
    }

    // --- shortcuts ------------------------------------------------------------------------------------------------------------

    public static void gable(Canvas c, Box footprint, int wallTop, boolean ridgeAlongX, Style s) {
        new Plan().gable(footprint, wallTop, ridgeAlongX, s).draw(c);
    }

    public static void hip(Canvas c, Box footprint, int wallTop, Style s) {
        new Plan().hip(footprint, wallTop, s).draw(c);
    }

    public static void shed(Canvas c, Box footprint, int wallTop, Dir high, Style s) {
        new Plan().shed(footprint, wallTop, high, s).draw(c);
    }

    /** Several volumes drawn together (cross gables, wings, dormers). */
    public static final class Plan {
        private final List<Volume> volumes = new ArrayList<>();
        private final List<int[]> dormerWindows = new ArrayList<>();
        private final List<String> dormerPanes = new ArrayList<>();

        public Plan gable(Box footprint, int wallTop, boolean ridgeAlongX, Style s) {
            volumes.add(new Volume(footprint, wallTop, ridgeAlongX ? Shape.GABLE_X : Shape.GABLE_Z, null, s, null, null));
            return this;
        }

        public Plan hip(Box footprint, int wallTop, Style s) {
            volumes.add(new Volume(footprint, wallTop, Shape.HIP, null, s, null, null));
            return this;
        }

        public Plan shed(Box footprint, int wallTop, Dir high, Style s) {
            volumes.add(new Volume(footprint, wallTop, Shape.SHED, high, s, null, null));
            return this;
        }

        /**
         * A gabled dormer on the main roof: its front wall stands on the main wall plane over cells centred on ({@code x},
         * {@code z}) — the main wall cell under the dormer's middle — {@code width} wide (odd), {@code depth} deep into the roof,
         * facing {@code out}. Its walls ({@code wall}) rise from {@code fromY} to {@code wallTop}, a window of {@code pane}
         * {@code width - 2} wide fills its front, and its own small gable roof (style {@code s}) runs back into the main roof.
         */
        public Plan dormer(int x, int z, Dir out, int width, int depth, int fromY, int wallTop, Brush wall, String pane, Style s) {
            int half = width / 2;
            Dir along = out.cw();
            int ax = along.dx, az = along.dz;
            int fx0 = x - ax * half, fz0 = z - az * half, fx1 = x + ax * half, fz1 = z + az * half;
            int bx = -out.dx * (depth - 1), bz = -out.dz * (depth - 1);
            Box fp = Box.of(fx0, 0, fz0, fx1 + bx, 0, fz1 + bz);
            fp = Box.of(Math.min(fp.x0(), Math.min(fx0, fx1)), 0, Math.min(fp.z0(), Math.min(fz0, fz1)),
                    Math.max(fp.x1(), Math.max(fx0, fx1)), 0, Math.max(fp.z1(), Math.max(fz0, fz1)));
            boolean ridgeAlongX = out.dx != 0;
            volumes.add(new Volume(fp, wallTop, ridgeAlongX ? Shape.GABLE_X : Shape.GABLE_Z, out, s, fromY, wall));
            dormerWindows.add(new int[]{x, z, out.ordinal(), width - 2, fromY, wallTop});
            dormerPanes.add(pane);
            return this;
        }

        public List<Volume> volumes() {
            return volumes;
        }

        /** Every column's winning share (the higher surface; ties go to the volume added first). */
        public Map<Long, Column> columns() {
            Map<Long, Column> out = new HashMap<>();
            for (Volume v : volumes) {
                int o = v.style().overhang();
                Box b = v.footprint().growXZ(o);
                for (int x = b.x0(); x <= b.x1(); x++) {
                    for (int z = b.z0(); z <= b.z1(); z++) {
                        Column col = column(v, x, z);
                        if (col == null) continue;
                        long k = Canvas.key(x, 0, z);
                        Column cur = out.get(k);
                        if (cur == null || col.top() > cur.top()) out.put(k, col);
                    }
                }
            }
            return out;
        }

        /** Draws every volume onto the canvas. */
        public void draw(Canvas c) {
            Map<Long, Column> cols = columns();
            // 1. Walls of their own (dormers), gable infill and the attic, up to under the winning roof.
            for (Volume v : volumes) {
                Box fp = v.footprint();
                for (int x = fp.x0(); x <= fp.x1(); x++) {
                    for (int z = fp.z0(); z <= fp.z1(); z++) {
                        Column win = cols.get(Canvas.key(x, 0, z));
                        if (win == null) continue;
                        boolean perim = x == fp.x0() || x == fp.x1() || z == fp.z0() || z == fp.z1();
                        if (v.wallFrom() != null && v.high() != null && isBack(fp, v.high(), x, z) && !isSide(fp, v.high(), x, z)) perim = false;
                        int under = win.bottomY() - 1;
                        if (v.wallFrom() != null && perim) {
                            for (int y = v.wallFrom(); y <= Math.min(v.wallTop(), under); y++) c.set(x, y, z, v.wall().at(x, y, z));
                        } else if (v.wallFrom() != null) {
                            for (int y = v.wallFrom(); y <= Math.min(v.wallTop(), under); y++) c.carve(x, y, z);
                        }
                        for (int y = v.wallTop() + 1; y <= under; y++) {
                            if (perim) c.set(x, y, z, v.style().infill().at(x, y, z));
                            else c.setIfEmpty(x, y, z, St.AIR);
                        }
                    }
                }
            }
            // 2. The pieces.
            for (Map.Entry<Long, Column> e : cols.entrySet()) {
                int x = Canvas.kx(e.getKey()), z = Canvas.kz(e.getKey());
                Column col = e.getValue();
                Volume v = col.volume();
                Style s = v.style();
                Family fam = col.rake() ? s.trim() : s.surface();
                int base = v.wallTop() + 1;
                for (Piece p : col.pieces()) {
                    int y = base + p.y();
                    String state = switch (p.kind()) {
                        case STAIR -> fam.stairs(col.up(), false);
                        case SLAB_TOP -> col.perimeter() && !col.rake() ? fam.block() : fam.slabTop();
                        case FULL -> fam.block();
                    };
                    c.set(x, y, z, state);
                }
                int top = col.pieces().stream().mapToInt(Piece::y).max().orElse(0) + base;
                Piece topPiece = topPiece(col);
                if (col.ridge() && s.ridgeCap() && topPiece.kind() != Kind.STAIR) {
                    c.set(x, top + 1, z, s.trim().slabBottom());
                } else if (col.ridge() && s.ridgeCap() && sameRidgePair(cols, col, x, z)) {
                    c.set(x, top + 1, z, s.trim().slabBottom());
                }
                // Under the eaves: the fascia board; under the rakes: a soffit of upside-down trim stairs.
                int low = col.bottomY();
                if (col.eave() && !col.rake() && s.fascia() != null && col.d() == 0) {
                    c.setIfAir(x, low - 1, z, St.trapdoor(s.fascia(), col.up(), true, false));
                }
                if (col.rake() && topPiece.kind() == Kind.STAIR && col.d() > 0 && s.trim().hasStairs()) {
                    c.setIfAir(x, low - 1, z, s.trim().stairs(col.up().opposite(), true));
                }
                if (s.ceiling() && !col.perimeter() && !col.rake() && inside(v, x, z) && topPiece.kind() == Kind.STAIR && low - 1 >= v.wallTop() + 3) {
                    c.setIfAir(x, low - 1, z, s.surface().stairs(col.up().opposite(), true));
                }
            }
            // 3. Dormer windows.
            for (int i = 0; i < dormerWindows.size(); i++) {
                int[] w = dormerWindows.get(i);
                Dir out = Dir.values()[w[2]];
                Dir along = out.cw();
                int half = w[3] / 2;
                for (int k = -half; k <= half; k++) {
                    for (int y = w[4] + 1; y <= w[5] - 1; y++) {
                        int x = w[0] + along.dx * k, z = w[1] + along.dz * k;
                        c.set(x, y, z, dormerPanes.get(i).endsWith("_pane") ? St.pane(dormerPanes.get(i)) : dormerPanes.get(i));
                    }
                }
            }
        }

        /** The dormer's back row (away from {@code out}): left open into the roof space. */
        private static boolean isBack(Box fp, Dir out, int x, int z) {
            return switch (out) {
                case NORTH -> z == fp.z1();
                case SOUTH -> z == fp.z0();
                case EAST -> x == fp.x0();
                case WEST -> x == fp.x1();
                default -> false;
            };
        }

        /** A dormer's cheek (its two side walls). */
        private static boolean isSide(Box fp, Dir out, int x, int z) {
            return out.axis().equals("z") ? x == fp.x0() || x == fp.x1() : z == fp.z0() || z == fp.z1();
        }

        private static boolean sameRidgePair(Map<Long, Column> cols, Column col, int x, int z) {
            Column other = cols.get(Canvas.key(x + col.up().dx, 0, z + col.up().dz));
            return other != null && other.ridge() && other.up() == col.up().opposite();
        }

        private static boolean inside(Volume v, int x, int z) {
            Box f = v.footprint();
            return x > f.x0() && x < f.x1() && z > f.z0() && z < f.z1();
        }

        private static Piece topPiece(Column col) {
            Piece best = col.pieces().get(0);
            for (Piece p : col.pieces()) if (p.y() > best.y()) best = p;
            return best;
        }
    }

    // --- one column ---------------------------------------------------------------------------------------------------------

    /** A volume's share of column (x, z), or null when it does not cover it. */
    public static Column column(Volume v, int x, int z) {
        Box f = v.footprint();
        int o = v.style().overhang();
        int ex0 = f.x0() - o, ex1 = f.x1() + o, ez0 = f.z0() - o, ez1 = f.z1() + o;
        if (x < ex0 || x > ex1 || z < ez0 || z > ez1) return null;
        int d, dmax;
        Dir up;
        boolean rake = false, eave;
        boolean perimeter = x == f.x0() || x == f.x1() || z == f.z0() || z == f.z1();
        boolean ridge;
        switch (v.shape()) {
            case GABLE_X -> {
                int a = z - ez0, b = ez1 - z;
                d = Math.min(a, b);
                up = a <= b ? Dir.SOUTH : Dir.NORTH;
                dmax = (ez1 - ez0) / 2;
                rake = x < f.x0() || x > f.x1();
                eave = d == 0;
                ridge = d == dmax;
                if (a == b) up = Dir.SOUTH;
            }
            case GABLE_Z -> {
                int a = x - ex0, b = ex1 - x;
                d = Math.min(a, b);
                up = a <= b ? Dir.EAST : Dir.WEST;
                dmax = (ex1 - ex0) / 2;
                rake = z < f.z0() || z > f.z1();
                eave = d == 0;
                ridge = d == dmax;
            }
            case HIP -> {
                int ax = x - ex0, bx = ex1 - x, az = z - ez0, bz = ez1 - z;
                d = Math.min(Math.min(ax, bx), Math.min(az, bz));
                // Prefer the long sides' slopes on ties so the ridge runs along the long axis.
                boolean longX = (ex1 - ex0) >= (ez1 - ez0);
                if (longX) up = az == d ? Dir.SOUTH : bz == d ? Dir.NORTH : ax == d ? Dir.EAST : Dir.WEST;
                else up = ax == d ? Dir.EAST : bx == d ? Dir.WEST : az == d ? Dir.SOUTH : Dir.NORTH;
                dmax = Math.min(ex1 - ex0, ez1 - ez0) / 2;
                eave = d == 0;
                ridge = d == dmax;
            }
            case SHED -> {
                Dir h = v.high();
                up = h;
                d = switch (h) {
                    case NORTH -> ez1 - z;
                    case SOUTH -> z - ez0;
                    case EAST -> x - ex0;
                    case WEST -> ex1 - x;
                    default -> throw new IllegalArgumentException("shed rises to a horizontal side");
                };
                dmax = Integer.MAX_VALUE;
                rake = h.axis().equals("z") ? x < f.x0() || x > f.x1() : z < f.z0() || z > f.z1();
                eave = d == 0;
                ridge = false;
            }
            default -> throw new IllegalStateException();
        }
        int e = d - o;
        List<Piece> pieces = new ArrayList<>(2);
        profile(v.style(), e, pieces);
        if (ridge && v.shape() != Shape.SHED) {
            boolean odd = v.shape() == Shape.GABLE_X ? (ez1 - ez0) % 2 == 0 : v.shape() == Shape.GABLE_Z ? (ex1 - ex0) % 2 == 0
                    : Math.min(ex1 - ex0, ez1 - ez0) % 2 == 0;
            if (odd) {
                // A single centre column: flatten its top piece into a full block (the cap goes on top).
                Piece t = pieces.get(pieces.size() - 1);
                if (t.kind() == Kind.STAIR) pieces.set(pieces.size() - 1, new Piece(Kind.FULL, t.y()));
            }
        }
        return new Column(v, d, e, up, ridge, rake, perimeter, eave, pieces);
    }

    /** The pieces of a profile at {@code e} cells in from the wall plane (negative over the overhang), y over the wall top + 1. */
    public static void profile(Style s, int e, List<Piece> out) {
        switch (s.pitch()) {
            case STEEP -> out.add(new Piece(Kind.STAIR, e));
            case HALF -> half(e, 0, out);
            case LOW -> {
                int k = Math.floorDiv(e, 3), r = Math.floorMod(e, 3);
                out.add(new Piece(r == 0 ? Kind.STAIR : Kind.SLAB_TOP, k));
            }
            case MANSARD -> {
                int m = Math.max(1, s.lowerRows());
                if (e < m) {
                    out.add(new Piece(Kind.FULL, 2 * e));
                    out.add(new Piece(Kind.STAIR, 2 * e + 1));
                    if (e < 0) out.set(0, new Piece(Kind.STAIR, 2 * e));
                } else {
                    half(e - m, 2 * m, out);
                }
            }
        }
    }

    private static void half(int e, int y0, List<Piece> out) {
        int k = Math.floorDiv(e, 2);
        out.add(new Piece(Math.floorMod(e, 2) == 0 ? Kind.STAIR : Kind.SLAB_TOP, y0 + k));
    }

    // --- chimneys -------------------------------------------------------------------------------------------------------------

    /**
     * A chimney stack at (x, z) from {@code y0} up through the roof to two above the roof surface there (found on the canvas),
     * in {@code body}: a corbelled cap of upside-down {@code cap} stairs round its top and, when {@code smoke}, a lit campfire
     * in the flue's mouth (a block entity: leave it off for arena layouts) — otherwise a wall-block pot.
     */
    public static void chimney(Canvas c, int x, int z, int y0, Brush body, Family cap, boolean smoke) {
        int roofTop = c.topAt(x, z, y0 + 64, y0);
        int top = Math.max(y0 + 2, roofTop + 2);
        for (int y = y0; y <= top; y++) c.set(x, y, z, body.at(x, y, z));
        for (Dir d : Dir.HORIZONTAL) {
            int nx = x + d.dx, nz = z + d.dz;
            if (c.isAir(nx, top, nz)) c.set(nx, top, nz, cap.stairs(d.opposite(), true));
            // Flashing where the stack meets the roof: nothing to do, the roof pieces already butt against it.
        }
        if (smoke) c.set(x, top + 1, z, St.campfire(true, true, Dir.NORTH));
        else if (cap.hasWall()) c.set(x, top + 1, z, cap.wallBlock());
        else c.set(x, top + 1, z, cap.slabBottom());
    }
}
