package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The finishing pass over a drawn plan (pure): every block that shapes itself after its neighbours gets the state vanilla would
 * give it, so the builder can place the plan as it is with no block updates and nothing changes when the world later updates it.
 *
 * <ul>
 *   <li>Stairs: {@code shape} by vanilla's {@code StairBlock.getStairsShape} (outer corners from the stair in front, inner from
 *       the one behind, same half, crossing axis).</li>
 *   <li>Iron bars and panes, fences, walls: their sides join what vanilla joins (sturdy faces, their own kind, walls, gates in
 *       line); walls get {@code low}/{@code tall} sides and their post as {@code WallBlock} does.</li>
 * </ul>
 * Off the plan is natural ground: rock below the surface (sturdy), open air above.
 */
public final class PlanShapes {

    private PlanShapes() {
    }

    /** Re-shapes every stair, bar, pane, fence and wall of the plan in place. */
    public static void apply(Map<Long, String> cells) {
        // Stairs first (what faces they show depends on their shape), then what joins to faces.
        Map<Long, String> out = new HashMap<>();
        for (Map.Entry<Long, String> e : cells.entrySet()) {
            long k = e.getKey();
            if (Kinds.stairs(e.getValue())) out.put(k, St.with(e.getValue(), "shape", stairShape(cells, e.getValue(), Plan.kx(k), Plan.ky(k), Plan.kz(k))));
        }
        cells.putAll(out);
        out.clear();
        List<Long> walls = new ArrayList<>();
        for (Map.Entry<Long, String> e : cells.entrySet()) {
            String s = e.getValue();
            long k = e.getKey();
            int x = Plan.kx(k), y = Plan.ky(k), z = Plan.kz(k);
            if (Kinds.barsLike(s)) out.put(k, joinCross(cells, s, x, y, z, false));
            else if (Kinds.fence(s)) out.put(k, joinCross(cells, s, x, y, z, true));
            else if (Kinds.wall(s)) walls.add(k);
        }
        cells.putAll(out);
        // Walls from the top down: a wall's post rises if the wall above it has its post up.
        walls.sort(Comparator.comparingInt(Plan::ky).reversed());
        for (long k : walls) cells.put(k, wallState(cells, cells.get(k), Plan.kx(k), Plan.ky(k), Plan.kz(k)));
    }

    static String at(Map<Long, String> cells, int x, int y, int z) {
        String s = cells.get(Plan.key(x, y, z));
        if (s != null) return s;
        return y > Zones.GROUND ? St.AIR : "minecraft:stone";
    }

    static String neighbour(Map<Long, String> cells, int x, int y, int z, String dir) {
        return at(cells, x + St.dx(dir), y + St.dy(dir), z + St.dz(dir));
    }

    // --- stairs ------------------------------------------------------------------------------------------------------------

    public static String stairShape(Map<Long, String> cells, String s, int x, int y, int z) {
        String facing = St.get(s, "facing"), half = St.get(s, "half");
        String front = neighbour(cells, x, y, z, facing);
        if (Kinds.stairs(front) && half.equals(St.get(front, "half"))) {
            String f1 = St.get(front, "facing");
            if (!St.axisOf(f1).equals(St.axisOf(facing)) && canTakeShape(cells, s, x, y, z, St.opposite(f1))) {
                return f1.equals(St.ccw(facing)) ? "outer_left" : "outer_right";
            }
        }
        String back = neighbour(cells, x, y, z, St.opposite(facing));
        if (Kinds.stairs(back) && half.equals(St.get(back, "half"))) {
            String f2 = St.get(back, "facing");
            if (!St.axisOf(f2).equals(St.axisOf(facing)) && canTakeShape(cells, s, x, y, z, f2)) {
                return f2.equals(St.ccw(facing)) ? "inner_left" : "inner_right";
            }
        }
        return "straight";
    }

    private static boolean canTakeShape(Map<Long, String> cells, String s, int x, int y, int z, String dir) {
        String n = neighbour(cells, x, y, z, dir);
        return !Kinds.stairs(n) || !St.get(n, "facing").equals(St.get(s, "facing")) || !St.get(n, "half").equals(St.get(s, "half"));
    }

    // --- bars, panes, fences --------------------------------------------------------------------------------------------------

    private static String joinCross(Map<Long, String> cells, String s, int x, int y, int z, boolean fence) {
        Map<String, String> p = St.props(s);
        for (String d : St.HORIZONTAL) {
            String n = neighbour(cells, x, y, z, d);
            boolean sturdy = !Kinds.connectionException(n) && Kinds.sturdy(n, St.opposite(d));
            boolean join = fence
                    ? sturdy || Kinds.fence(n) && Kinds.woodenFence(n) == Kinds.woodenFence(s) || gateInLine(n, d)
                    : sturdy || Kinds.barsLike(n) || Kinds.wall(n);
            p.put(d, String.valueOf(join));
        }
        return St.build(St.id(s), p);
    }

    /** Vanilla {@code FenceGateBlock.connectsToDirection}: a gate joins along the line it closes. */
    private static boolean gateInLine(String n, String dir) {
        return Kinds.fenceGate(n) && St.axisOf(St.get(n, "facing")).equals(St.axisOf(St.cw(dir)));
    }

    // --- walls --------------------------------------------------------------------------------------------------------------

    static boolean wallJoins(Map<Long, String> cells, int x, int y, int z, String d) {
        String n = neighbour(cells, x, y, z, d);
        boolean sturdy = !Kinds.connectionException(n) && Kinds.sturdy(n, St.opposite(d));
        return Kinds.wall(n) || sturdy || Kinds.barsLike(n) || gateInLine(n, d);
    }

    private static String wallState(Map<Long, String> cells, String s, int x, int y, int z) {
        String above = at(cells, x, y + 1, z);
        Map<String, String> p = St.props(s);
        Map<String, String> side = new HashMap<>();
        for (String d : St.HORIZONTAL) {
            String v = !wallJoins(cells, x, y, z, d) ? "none" : covers(cells, above, x, y + 1, z, d) ? "tall" : "low";
            side.put(d, v);
            p.put(d, v);
        }
        p.put("up", String.valueOf(raisePost(cells, above, side, x, y + 1, z)));
        return St.build(St.id(s), p);
    }

    /** Whether the block above covers a wall's side toward {@code d} (vanilla: its bottom face covers the side's column). */
    private static boolean covers(Map<Long, String> cells, String above, int ax, int ay, int az, String d) {
        if (fullBottom(above)) return true;
        return Kinds.wall(above) && wallJoins(cells, ax, ay, az, d);
    }

    private static boolean fullBottom(String s) {
        if (Kinds.air(s)) return false;
        if (Kinds.sturdy(s, "down")) return true;
        return St.path(s).endsWith("_carpet");
    }

    private static boolean raisePost(Map<Long, String> cells, String above, Map<String, String> side, int ax, int ay, int az) {
        if (Kinds.wall(above) && "true".equals(St.get(above, "up"))) return true;
        boolean n = side.get("north").equals("none"), s = side.get("south").equals("none"),
                e = side.get("east").equals("none"), w = side.get("west").equals("none");
        if (n && s && e && w || n != s || w != e) return true;
        if (side.get("north").equals("tall") && side.get("south").equals("tall") || side.get("east").equals("tall") && side.get("west").equals("tall")) {
            return false;
        }
        if (fullBottom(above) || Kinds.wall(above)) return true;
        String p = St.path(above);
        return p.endsWith("torch") || p.endsWith("_sign") || p.endsWith("_banner") || p.endsWith("_pressure_plate") || p.endsWith("lantern")
                || p.endsWith("candle") || p.equals("chain") || p.equals("end_rod") || p.equals("lightning_rod") || p.equals("flower_pot")
                || p.startsWith("potted_");
    }
}
