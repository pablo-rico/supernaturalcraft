package org.papiricoh.supernaturalcraft.buildkit;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Block-state strings (pure): {@code namespace:block[key=value,...]} with the properties sorted and the string interned (a
 * layout holds tens of thousands). Every builder here writes the properties vanilla 1.21.1 knows for that block, so a state
 * never parses to something unexpected; {@link #check} rejects malformed text and {@code BlockIds} (tests) rejects unknown ids,
 * property names and values. A state may leave properties out: the game uses the block's defaults.
 *
 * <p>Directions follow {@link Dir}: north = -Z, east = +X. Conventions of the shaped blocks:
 * <ul>
 *   <li>stairs: {@code facing} is where the tall back is (a stair facing north climbs toward the north);</li>
 *   <li>trapdoors: {@code facing} is the side the hinge is away from; open, the trapdoor lies flat against the cell's face
 *       {@code facing.opposite()}… vanilla: an open trapdoor facing north covers the cell's south face;</li>
 *   <li>doors: {@code facing} is the way one looks walking out through the door from its inside;</li>
 *   <li>beds: {@code facing} points from foot to head;</li>
 *   <li>wall-mounted things (torches, signs, banners, ladders): {@code facing} points away from the wall they hang on.</li>
 * </ul>
 */
public final class St {

    public static final String AIR = "minecraft:air";
    private static final Pattern SYNTAX = Pattern.compile(
            "[a-z0-9_.-]+:[a-z0-9_./-]+(\\[[a-z0-9_]+=[a-z0-9_]+(,[a-z0-9_]+=[a-z0-9_]+)*])?");

    private St() {
    }

    // --- parsing --------------------------------------------------------------------------------------------------------------

    /** A state from an id ({@code minecraft:} if no namespace) and key/value pairs. */
    public static String of(String id, String... kv) {
        if (kv.length % 2 != 0) throw new IllegalArgumentException("odd key/value list for " + id);
        Map<String, String> props = new TreeMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) props.put(kv[i], kv[i + 1]);
        return build(full(id), props);
    }

    /** The state with {@code minecraft:} added when it has no namespace. */
    public static String full(String state) {
        int b = state.indexOf('[');
        String id = b < 0 ? state : state.substring(0, b);
        return id.indexOf(':') < 0 ? ("minecraft:" + state).intern() : state.intern();
    }

    /**
     * Rejects anything the game's parser would not read as written (it would silently become air in {@code HorsemenGround}):
     * lower-case {@code ns:path}, an optional bracketed {@code key=value} list, nothing else.
     */
    public static String check(String state) {
        if (state == null || !SYNTAX.matcher(state).matches()) throw new IllegalArgumentException("malformed block state: '" + state + "'");
        return state;
    }

    public static boolean wellFormed(String state) {
        return state != null && SYNTAX.matcher(state).matches();
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

    public static boolean is(String state, String key, String value) {
        return value.equals(get(state, key));
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

    /** The id of a sibling block: {@code sibling("minecraft:stone_bricks", "_stairs")} is not this; see {@link Family}. */
    public static String suffix(String id, String suffix) {
        return full(id) + suffix;
    }

    // --- shaped blocks --------------------------------------------------------------------------------------------------------

    /** Stairs; {@code facing} is where the tall back is. The corner {@code shape} is fixed by {@link Shapes}. */
    public static String stairs(String id, Dir facing, boolean top) {
        return of(id, "facing", facing.id(), "half", top ? "top" : "bottom", "shape", "straight", "waterlogged", "false");
    }

    public static String slab(String id, String type) {
        if (!type.equals("top") && !type.equals("bottom") && !type.equals("double")) throw new IllegalArgumentException(type);
        return of(id, "type", type, "waterlogged", "false");
    }

    public static String slabBottom(String id) {
        return slab(id, "bottom");
    }

    public static String slabTop(String id) {
        return slab(id, "top");
    }

    /** A trapdoor: closed it lies in the {@code top} or bottom half; open it stands against the cell's {@code facing.opposite()} face. */
    public static String trapdoor(String id, Dir facing, boolean top, boolean open) {
        return of(id, "facing", facing.id(), "half", top ? "top" : "bottom", "open", String.valueOf(open), "powered", "false", "waterlogged", "false");
    }

    /**
     * An open trapdoor flat against the face of its cell that is toward {@code wallSide} (a shutter beside a window, a
     * cabinet front): vanilla's open trapdoor facing F covers the face opposite F.
     */
    public static String trapdoorAgainst(String id, Dir wallSide) {
        return trapdoor(id, wallSide.opposite(), false, true);
    }

    /** The two halves of a door: [lower, upper]. {@code facing} is the way one looks walking out; {@code hinge} left or right. */
    public static String[] door(String id, Dir facing, boolean rightHinge, boolean open) {
        String h = rightHinge ? "right" : "left";
        return new String[]{
                of(id, "facing", facing.id(), "half", "lower", "hinge", h, "open", String.valueOf(open), "powered", "false"),
                of(id, "facing", facing.id(), "half", "upper", "hinge", h, "open", String.valueOf(open), "powered", "false")};
    }

    /** A fence gate across a path that runs along {@code facing}. {@code inWall} lowers it to sit between wall blocks. */
    public static String gate(String id, Dir facing, boolean open, boolean inWall) {
        return of(id, "facing", facing.id(), "in_wall", String.valueOf(inWall), "open", String.valueOf(open), "powered", "false");
    }

    /** The two halves of a bed: [foot, head]; {@code facing} points from foot to head. */
    public static String[] bed(String color, Dir facing) {
        return new String[]{
                of(color + "_bed", "facing", facing.id(), "occupied", "false", "part", "foot"),
                of(color + "_bed", "facing", facing.id(), "occupied", "false", "part", "head")};
    }

    /** The two halves of a tall plant ({@code tall_grass}, {@code large_fern}, {@code lilac}, {@code rose_bush}, {@code peony}, {@code sunflower}, {@code pitcher_plant}): [lower, upper]. */
    public static String[] tall(String id) {
        return new String[]{of(id, "half", "lower"), of(id, "half", "upper")};
    }

    /** A block along an axis ({@code x}, {@code y}, {@code z}): logs, wood, pillars, basalt, hay, chains. */
    public static String axis(String id, String axis) {
        if (!axis.equals("x") && !axis.equals("y") && !axis.equals("z")) throw new IllegalArgumentException(axis);
        return of(id, "axis", axis);
    }

    public static String log(String id) {
        return axis(id, "y");
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

    /** {@code color} null or empty for the plain candle; 1–4 candles, 3 light each when lit. */
    public static String candle(String color, int candles, boolean lit) {
        if (candles < 1 || candles > 4) throw new IllegalArgumentException("candles " + candles);
        return of(color == null || color.isEmpty() ? "candle" : color + "_candle", "candles", String.valueOf(candles), "lit", String.valueOf(lit), "waterlogged", "false");
    }

    /** The invisible light block (the game's {@code light}), 0–15. */
    public static String light(int level) {
        if (level < 0 || level > 15) throw new IllegalArgumentException("light " + level);
        return of("light", "level", String.valueOf(level), "waterlogged", "false");
    }

    /** A lit copper bulb (stays lit: it toggles only on a redstone pulse). */
    public static String bulb(String id) {
        return of(id, "lit", "true", "powered", "false");
    }

    /** Leaves that never decay. */
    public static String leaves(String id) {
        return of(id, "distance", "1", "persistent", "true", "waterlogged", "false");
    }

    /** Pink petals, 1–4 flowers, turned to {@code facing}. */
    public static String petals(int amount, Dir facing) {
        if (amount < 1 || amount > 4) throw new IllegalArgumentException("petals " + amount);
        return of("pink_petals", "facing", facing.id(), "flower_amount", String.valueOf(amount));
    }

    /** Connecting blocks start unconnected: {@link Shapes} joins them to their neighbours. */
    public static String bars() {
        return pane("iron_bars");
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

    /** Any block with only a horizontal {@code facing} (furnaces, looms, carved pumpkins, lecterns' facing…). */
    public static String facing(String id, Dir facing) {
        return of(id, "facing", facing.id());
    }

    public static String torch() {
        return "minecraft:torch";
    }

    /** A torch on the wall behind it; {@code facing} points away from that wall. */
    public static String wallTorch(Dir facing) {
        return of("wall_torch", "facing", facing.id());
    }

    public static String soulWallTorch(Dir facing) {
        return of("soul_wall_torch", "facing", facing.id());
    }

    /** {@code face}: floor, wall or ceiling. */
    public static String button(String id, String face, Dir facing) {
        return of(id, "face", face, "facing", facing.id(), "powered", "false");
    }

    public static String lever(String face, Dir facing) {
        return of("lever", "face", face, "facing", facing.id(), "powered", "false");
    }

    public static String carpet(String color) {
        return of(color + "_carpet");
    }

    public static String mossCarpet() {
        return "minecraft:moss_carpet";
    }

    public static String ladder(Dir facing) {
        return of("ladder", "facing", facing.id(), "waterlogged", "false");
    }

    /** A wall sign hung on the wall behind it ({@code facing} out of the wall); {@code wood} like {@code oak}. */
    public static String wallSign(String wood, Dir facing) {
        return of(wood + "_wall_sign", "facing", facing.id(), "waterlogged", "false");
    }

    /** A hanging sign under a block ({@code attached=false}), its face along {@code rotation} 0–15. */
    public static String hangingSign(String wood, int rotation) {
        return of(wood + "_hanging_sign", "attached", "false", "rotation", String.valueOf(rotation), "waterlogged", "false");
    }

    public static String wallBanner(String color, Dir facing) {
        return of(color + "_wall_banner", "facing", facing.id());
    }

    /** A pointed dripstone hanging ({@code down}) or standing ({@code up}); thickness tip, frustum, middle or base. */
    public static String dripstone(Dir vertical, String thickness) {
        return of("pointed_dripstone", "thickness", thickness, "vertical_direction", vertical.id(), "waterlogged", "false");
    }

    public static String campfire(boolean lit, boolean signal, Dir facing) {
        return of("campfire", "facing", facing.id(), "lit", String.valueOf(lit), "signal_fire", String.valueOf(signal), "waterlogged", "false");
    }

    /** A potted plant ({@code fern} → {@code potted_fern}). */
    public static String potted(String plant) {
        return full("potted_" + plant);
    }

    public static String barrel(Dir facing, boolean open) {
        return of("barrel", "facing", facing.id(), "open", String.valueOf(open));
    }

    /** A chiseled bookshelf whose books fill the slots set in {@code mask} (6 bits, slot 0 top-left). */
    public static String chiseledShelf(Dir facing, int mask) {
        return of("chiseled_bookshelf", "facing", facing.id(),
                "slot_0_occupied", String.valueOf((mask & 1) != 0), "slot_1_occupied", String.valueOf((mask & 2) != 0),
                "slot_2_occupied", String.valueOf((mask & 4) != 0), "slot_3_occupied", String.valueOf((mask & 8) != 0),
                "slot_4_occupied", String.valueOf((mask & 16) != 0), "slot_5_occupied", String.valueOf((mask & 32) != 0));
    }

    public static String decoratedPot(Dir facing) {
        return of("decorated_pot", "cracked", "false", "facing", facing.id(), "waterlogged", "false");
    }

    /** A lectern (a block entity) facing the reader. */
    public static String lectern(Dir facing) {
        return of("lectern", "facing", facing.id(), "has_book", "false", "powered", "false");
    }

    public static String rod(String id, Dir facing) {
        return of(id, "facing", facing.id());
    }

    public static String endRod(Dir facing) {
        return of("end_rod", "facing", facing.id());
    }

    public static String vine(Dir... onWalls) {
        Map<String, String> p = new TreeMap<>(Map.of("east", "false", "north", "false", "south", "false", "up", "false", "west", "false"));
        for (Dir d : onWalls) p.put(d.id(), "true");
        return build("minecraft:vine", p);
    }

    /** A still source block. */
    public static String water() {
        return of("water", "level", "0");
    }

    /** Falling water (a waterfall's column). */
    public static String waterFalling() {
        return of("water", "level", "8");
    }

    /** {@code state} with water in it (only for blocks that have {@code waterlogged}). */
    public static String waterlogged(String state) {
        if (get(state, "waterlogged") == null) throw new IllegalArgumentException("not waterloggable here: " + state);
        return with(state, "waterlogged", "true");
    }

    public static String snow(int layers) {
        return of("snow", "layers", String.valueOf(layers));
    }

    public static String crop(String id, int age) {
        return of(id, "age", String.valueOf(age));
    }

    public static String seaPickle(int pickles, boolean wet) {
        return of("sea_pickle", "pickles", String.valueOf(pickles), "waterlogged", String.valueOf(wet));
    }

    public static String head(String id, int rotation) {
        return of(id, "powered", "false", "rotation", String.valueOf(rotation));
    }

    public static String wallHead(String id, Dir facing) {
        return of(id.replace("_head", "_wall_head").replace("_skull", "_wall_skull"), "facing", facing.id(), "powered", "false");
    }

    /** Grass, podzol or mycelium with snow on it or not. */
    public static String snowy(String id, boolean snowy) {
        return of(id, "snowy", String.valueOf(snowy));
    }

    // --- turning --------------------------------------------------------------------------------------------------------------

    /**
     * The state turned {@code quarterTurns} clockwise seen from above (as a structure template rotates it): horizontal
     * {@code facing}, {@code axis} x↔z, the side properties of fences, walls, panes and vines, and {@code rotation} (0–15).
     * Stair shapes and door hinges are unchanged by a rotation.
     */
    public static String rotate(String state, int quarterTurns) {
        int q = Math.floorMod(quarterTurns, 4);
        if (q == 0 || state.indexOf('[') < 0) return state;
        Map<String, String> p = props(state);
        Map<String, String> out = new TreeMap<>(p);
        String f = p.get("facing");
        if (f != null && !f.equals("up") && !f.equals("down")) out.put("facing", turn(Dir.of(f), q).id());
        String a = p.get("axis");
        if (a != null && (q & 1) == 1 && !a.equals("y")) out.put("axis", a.equals("x") ? "z" : "x");
        String r = p.get("rotation");
        if (r != null) out.put("rotation", String.valueOf((Integer.parseInt(r) + 4 * q) & 15));
        boolean sides = false;
        for (Dir d : Dir.HORIZONTAL) sides |= p.containsKey(d.id());
        if (sides) {
            for (Dir d : Dir.HORIZONTAL) {
                String v = p.get(d.id());
                if (v != null) out.put(turn(d, q).id(), v);
                else out.remove(turn(d, q).id());
            }
        }
        return build(id(state), out);
    }

    private static Dir turn(Dir d, int q) {
        Dir out = d;
        for (int i = 0; i < q; i++) out = out.cw();
        return out;
    }

    // --- classification shortcuts ---------------------------------------------------------------------------------------------

    public static boolean isAir(String state) {
        return Kinds.air(state);
    }
}
