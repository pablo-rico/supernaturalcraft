package org.papiricoh.supernaturalcraft.buildkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The finishing pass over a drawn canvas (pure): every block that shapes itself after its neighbours gets the state vanilla would
 * give it, so the writer can place the layout as it is with no block updates and nothing changes when the world later updates it.
 *
 * <ul>
 *   <li>Stairs: {@code shape} by vanilla's {@code StairBlock.getStairsShape} (outer corners from the stair in front, inner from
 *       the one behind, same half, crossing axis). So a run of eaves or a cornice turns its corners by itself, and hip roofs get
 *       their hips.</li>
 *   <li>Iron bars and panes, fences, walls: their sides join what vanilla joins (sturdy faces, their own kind, walls, gates in
 *       line); walls get {@code low}/{@code tall} sides and their post as {@code WallBlock} does.</li>
 * </ul>
 * Off the canvas the {@link Canvas#outside} decides.
 */
public final class Shapes {

    private Shapes() {
    }

    /** Re-shapes every stair, bar, pane, fence and wall of the canvas in place. */
    public static void apply(Canvas canvas) {
        Map<Long, String> cells = canvas.raw();
        // Stairs first (the faces they show depend on their shape), then what joins to faces.
        Map<Long, String> out = new HashMap<>();
        for (Map.Entry<Long, String> e : cells.entrySet()) {
            long k = e.getKey();
            if (Kinds.stairs(e.getValue())) {
                out.put(k, St.with(e.getValue(), "shape", stairShape(canvas, e.getValue(), Canvas.kx(k), Canvas.ky(k), Canvas.kz(k))));
            }
        }
        cells.putAll(out);
        out.clear();
        List<Long> walls = new ArrayList<>();
        for (Map.Entry<Long, String> e : cells.entrySet()) {
            String s = e.getValue();
            long k = e.getKey();
            int x = Canvas.kx(k), y = Canvas.ky(k), z = Canvas.kz(k);
            if (Kinds.barsLike(s)) out.put(k, joinCross(canvas, s, x, y, z, false));
            else if (Kinds.fence(s)) out.put(k, joinCross(canvas, s, x, y, z, true));
            else if (Kinds.wall(s)) walls.add(k);
        }
        cells.putAll(out);
        // Walls from the top down: a wall's post rises if the wall above it has its post up.
        walls.sort(Comparator.comparingInt(Canvas::ky).reversed());
        for (long k : walls) cells.put(k, wallState(canvas, cells.get(k), Canvas.kx(k), Canvas.ky(k), Canvas.kz(k)));
    }

    static String neighbour(Canvas c, int x, int y, int z, Dir d) {
        return c.world(x + d.dx, y + d.dy, z + d.dz);
    }

    // --- stairs ------------------------------------------------------------------------------------------------------------

    /** The shape vanilla gives a stair at (x, y, z) among its neighbours on the canvas. */
    public static String stairShape(Canvas c, String s, int x, int y, int z) {
        Dir facing = Dir.of(St.get(s, "facing"));
        String half = St.get(s, "half");
        String front = neighbour(c, x, y, z, facing);
        if (Kinds.stairs(front) && half.equals(St.get(front, "half"))) {
            Dir f1 = Dir.of(St.get(front, "facing"));
            if (!f1.axis().equals(facing.axis()) && canTakeShape(c, s, x, y, z, f1.opposite())) {
                return f1 == facing.ccw() ? "outer_left" : "outer_right";
            }
        }
        String back = neighbour(c, x, y, z, facing.opposite());
        if (Kinds.stairs(back) && half.equals(St.get(back, "half"))) {
            Dir f2 = Dir.of(St.get(back, "facing"));
            if (!f2.axis().equals(facing.axis()) && canTakeShape(c, s, x, y, z, f2)) {
                return f2 == facing.ccw() ? "inner_left" : "inner_right";
            }
        }
        return "straight";
    }

    private static boolean canTakeShape(Canvas c, String s, int x, int y, int z, Dir d) {
        String n = neighbour(c, x, y, z, d);
        return !Kinds.stairs(n) || !St.get(n, "facing").equals(St.get(s, "facing")) || !St.get(n, "half").equals(St.get(s, "half"));
    }

    // --- bars, panes, fences --------------------------------------------------------------------------------------------------

    private static String joinCross(Canvas c, String s, int x, int y, int z, boolean fence) {
        Map<String, String> p = St.props(s);
        for (Dir d : Dir.HORIZONTAL) {
            String n = neighbour(c, x, y, z, d);
            boolean sturdy = !Kinds.connectionException(n) && Kinds.sturdy(n, d.opposite());
            boolean join = fence
                    ? sturdy || Kinds.fence(n) && Kinds.woodenFence(n) == Kinds.woodenFence(s) || gateInLine(n, d)
                    : sturdy || Kinds.barsLike(n) || Kinds.wall(n);
            p.put(d.id(), String.valueOf(join));
        }
        return St.build(St.id(s), p);
    }

    /** Vanilla {@code FenceGateBlock.connectsToDirection}: a gate joins along the line it closes. */
    private static boolean gateInLine(String n, Dir dir) {
        return Kinds.fenceGate(n) && Dir.of(St.get(n, "facing")).axis().equals(dir.cw().axis());
    }

    // --- walls --------------------------------------------------------------------------------------------------------------

    static boolean wallJoins(Canvas c, int x, int y, int z, Dir d) {
        String n = neighbour(c, x, y, z, d);
        boolean sturdy = !Kinds.connectionException(n) && Kinds.sturdy(n, d.opposite());
        return Kinds.wall(n) || sturdy || Kinds.barsLike(n) || gateInLine(n, d);
    }

    private static String wallState(Canvas c, String s, int x, int y, int z) {
        String above = c.world(x, y + 1, z);
        Map<String, String> p = St.props(s);
        Map<Dir, String> side = new HashMap<>();
        for (Dir d : Dir.HORIZONTAL) {
            String v = !wallJoins(c, x, y, z, d) ? "none" : covers(c, above, x, y + 1, z, d) ? "tall" : "low";
            side.put(d, v);
            p.put(d.id(), v);
        }
        p.put("up", String.valueOf(raisePost(above, side)));
        return St.build(St.id(s), p);
    }

    /** Whether the block above covers a wall's side toward {@code d} (vanilla: its bottom face covers the side's column). */
    private static boolean covers(Canvas c, String above, int ax, int ay, int az, Dir d) {
        if (fullBottom(above)) return true;
        return Kinds.wall(above) && wallJoins(c, ax, ay, az, d);
    }

    private static boolean fullBottom(String s) {
        if (Kinds.air(s)) return false;
        if (Kinds.sturdy(s, Dir.DOWN)) return true;
        return St.path(s).endsWith("_carpet");
    }

    private static boolean raisePost(String above, Map<Dir, String> side) {
        if (Kinds.wall(above) && "true".equals(St.get(above, "up"))) return true;
        boolean n = side.get(Dir.NORTH).equals("none"), s = side.get(Dir.SOUTH).equals("none"),
                e = side.get(Dir.EAST).equals("none"), w = side.get(Dir.WEST).equals("none");
        if (n && s && e && w || n != s || w != e) return true;
        if (side.get(Dir.NORTH).equals("tall") && side.get(Dir.SOUTH).equals("tall") || side.get(Dir.EAST).equals("tall") && side.get(Dir.WEST).equals("tall")) {
            return false;
        }
        if (fullBottom(above) || Kinds.wall(above)) return true;
        String p = St.path(above);
        return p.endsWith("torch") || p.endsWith("_sign") || p.endsWith("_banner") || p.endsWith("_pressure_plate") || p.endsWith("lantern")
                || p.endsWith("candle") || p.equals("chain") || p.equals("end_rod") || p.equals("lightning_rod") || p.equals("flower_pot")
                || p.startsWith("potted_");
    }
}
