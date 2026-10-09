package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import java.util.Map;
import java.util.TreeMap;

/**
 * Block-state strings for the bunker's plan (pure): {@code namespace:block[key=value,...]} with the properties sorted, interned
 * (the plan holds hundreds of thousands of them). Horizontal directions are the plan's: north = -Z, south = +Z, west = -X,
 * east = +X.
 */
public final class St {

    public static final String AIR = "minecraft:air";
    public static final String[] HORIZONTAL = {"north", "east", "south", "west"};

    private St() {
    }

    /** A state from an id ({@code minecraft:} if no namespace) and key/value pairs. */
    public static String of(String id, String... kv) {
        Map<String, String> props = new TreeMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) props.put(kv[i], kv[i + 1]);
        return build(full(id), props);
    }

    public static String full(String id) {
        return id.indexOf(':') < 0 ? "minecraft:" + id : id;
    }

    public static String id(String state) {
        int b = state.indexOf('[');
        return b < 0 ? state : state.substring(0, b);
    }

    /** The path of the id ({@code minecraft:oak_stairs} → {@code oak_stairs}). */
    public static String path(String state) {
        String id = id(state);
        return id.substring(id.indexOf(':') + 1);
    }

    public static Map<String, String> props(String state) {
        Map<String, String> out = new TreeMap<>();
        int b = state.indexOf('[');
        if (b < 0) return out;
        for (String kv : state.substring(b + 1, state.length() - 1).split(",")) {
            int e = kv.indexOf('=');
            if (e > 0) out.put(kv.substring(0, e), kv.substring(e + 1));
        }
        return out;
    }

    public static String get(String state, String key) {
        return props(state).get(key);
    }

    public static String with(String state, String key, String value) {
        Map<String, String> p = props(state);
        p.put(key, value);
        return build(id(state), p);
    }

    public static String build(String id, Map<String, String> props) {
        if (props.isEmpty()) return id.intern();
        StringBuilder sb = new StringBuilder(id).append('[');
        boolean first = true;
        for (Map.Entry<String, String> e : new TreeMap<>(props).entrySet()) {
            if (!first) sb.append(',');
            sb.append(e.getKey()).append('=').append(e.getValue());
            first = false;
        }
        return sb.append(']').toString().intern();
    }

    // --- common shapes --------------------------------------------------------------------------------------------------

    /** Stairs ({@code id} is the stairs block); {@code facing} is where the tall back is; the shape is set by {@link PlanShapes}. */
    public static String stairs(String id, String facing, boolean top) {
        return of(id, "facing", facing, "half", top ? "top" : "bottom", "shape", "straight", "waterlogged", "false");
    }

    public static String slab(String id, String type) {
        return of(id, "type", type, "waterlogged", "false");
    }

    public static String slab(String id, boolean top) {
        return slab(id, top ? "top" : "bottom");
    }

    /** A trapdoor: {@code facing} is the side its hinge is away from (when open it lies against the {@code opposite} face). */
    public static String trapdoor(String id, String facing, boolean top, boolean open) {
        return of(id, "facing", facing, "half", top ? "top" : "bottom", "open", String.valueOf(open), "powered", "false", "waterlogged", "false");
    }

    /** A pillar-like block along an axis ({@code x}, {@code y}, {@code z}). */
    public static String axis(String id, String axis) {
        return of(id, "axis", axis);
    }

    public static String chain(String axis) {
        return of("chain", "axis", axis, "waterlogged", "false");
    }

    public static String lantern(boolean hanging) {
        return of("lantern", "hanging", String.valueOf(hanging), "waterlogged", "false");
    }

    public static String soulLantern(boolean hanging) {
        return of("soul_lantern", "hanging", String.valueOf(hanging), "waterlogged", "false");
    }

    public static String candle(String color, int candles, boolean lit) {
        return of(color == null || color.isEmpty() ? "candle" : color + "_candle", "candles", String.valueOf(candles), "lit", String.valueOf(lit), "waterlogged", "false");
    }

    /** A lit copper bulb (stays lit: it toggles only on a redstone pulse). */
    public static String bulb(String id) {
        return of(id, "lit", "true", "powered", "false");
    }

    /** Connecting blocks start unconnected: {@link PlanShapes} joins them to their neighbours. */
    public static String bars() {
        return of("iron_bars", "east", "false", "north", "false", "south", "false", "waterlogged", "false", "west", "false");
    }

    public static String pane(String id) {
        return of(id, "east", "false", "north", "false", "south", "false", "waterlogged", "false", "west", "false");
    }

    public static String fence(String id) {
        return of(id, "east", "false", "north", "false", "south", "false", "waterlogged", "false", "west", "false");
    }

    public static String wall(String id) {
        return of(id, "east", "none", "north", "none", "south", "none", "up", "true", "waterlogged", "false", "west", "none");
    }

    public static String facing(String id, String facing) {
        return of(id, "facing", facing);
    }

    public static String wallTorch(String facing) {
        return of("wall_torch", "facing", facing);
    }

    public static String button(String id, String face, String facing) {
        return of(id, "face", face, "facing", facing, "powered", "false");
    }

    public static String carpet(String color) {
        return of(color + "_carpet");
    }

    /** The two halves of a door: [lower, upper]. {@code facing} is the way one looks when walking out through it. */
    public static String[] door(String id, String facing, String hinge, boolean open) {
        return new String[]{
                of(id, "facing", facing, "half", "lower", "hinge", hinge, "open", String.valueOf(open), "powered", "false"),
                of(id, "facing", facing, "half", "upper", "hinge", hinge, "open", String.valueOf(open), "powered", "false")};
    }

    /** The two halves of a bed: [foot, head]; {@code facing} points from foot to head. */
    public static String[] bed(String color, String facing) {
        return new String[]{
                of(color + "_bed", "facing", facing, "occupied", "false", "part", "foot"),
                of(color + "_bed", "facing", facing, "occupied", "false", "part", "head")};
    }

    // --- directions -------------------------------------------------------------------------------------------------------

    public static int dx(String dir) {
        return switch (dir) {
            case "east" -> 1;
            case "west" -> -1;
            default -> 0;
        };
    }

    public static int dz(String dir) {
        return switch (dir) {
            case "south" -> 1;
            case "north" -> -1;
            default -> 0;
        };
    }

    public static int dy(String dir) {
        return switch (dir) {
            case "up" -> 1;
            case "down" -> -1;
            default -> 0;
        };
    }

    public static String opposite(String dir) {
        return switch (dir) {
            case "north" -> "south";
            case "south" -> "north";
            case "east" -> "west";
            case "west" -> "east";
            case "up" -> "down";
            case "down" -> "up";
            default -> throw new IllegalArgumentException(dir);
        };
    }

    /** As vanilla {@code Direction.getClockWise} seen from above: north → east → south → west. */
    public static String cw(String dir) {
        return switch (dir) {
            case "north" -> "east";
            case "east" -> "south";
            case "south" -> "west";
            case "west" -> "north";
            default -> throw new IllegalArgumentException(dir);
        };
    }

    public static String ccw(String dir) {
        return switch (dir) {
            case "north" -> "west";
            case "west" -> "south";
            case "south" -> "east";
            case "east" -> "north";
            default -> throw new IllegalArgumentException(dir);
        };
    }

    public static String axisOf(String dir) {
        return dir.equals("east") || dir.equals("west") ? "x" : dir.equals("up") || dir.equals("down") ? "y" : "z";
    }

    /** The horizontal direction of a step (dx, dz), one of them zero. */
    public static String dir(int dx, int dz) {
        if (dx > 0) return "east";
        if (dx < 0) return "west";
        if (dz > 0) return "south";
        if (dz < 0) return "north";
        throw new IllegalArgumentException("no direction");
    }
}
