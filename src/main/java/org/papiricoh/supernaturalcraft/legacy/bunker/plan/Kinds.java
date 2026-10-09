package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import java.util.Map;
import java.util.Set;

/**
 * What the plan needs to know about a block without Minecraft (pure): whether it is a full opaque cube, how it passes light,
 * how much light it gives, which of its faces are sturdy (for what bars, fences and walls join), and what it needs to stand.
 * Approximations of vanilla, erring on the dark side for light.
 */
public final class Kinds {

    /** Mod blocks that are not full cubes (desks, the map table, the door) and their light. */
    private static final Set<String> MOD_PARTIAL = Set.of("supernaturalcraft:research_desk", "supernaturalcraft:map_table",
            "supernaturalcraft:bunker_door", "supernaturalcraft:devils_trap");
    private static final Map<String, Integer> MOD_LIGHT = Map.of("supernaturalcraft:research_desk", 10, "supernaturalcraft:map_table", 12);

    /** Full cubes light passes through. */
    private static final String[] CLEAR_CUBE_SUFFIX = {"glass", "copper_grate", "leaves"};
    /** Thin things: light passes as through air; nothing stands on them. */
    private static final String[] THIN_SUFFIX = {"_bars", "chain", "_pane", "lantern", "torch", "_carpet", "_trapdoor", "_door", "_fence",
            "_fence_gate", "_wall", "_rod", "_sign", "_hanging_sign", "_button", "_pressure_plate", "candle", "_banner", "_bed",
            "flower_pot", "ladder", "vine", "rail", "_coral_fan", "short_grass", "tall_grass", "fern", "lichen", "_sapling", "cobweb",
            "scaffolding", "tripwire", "string", "lever", "_head", "_skull", "pointed_dripstone", "sea_pickle", "bell", "_frame"};
    /** Partial blocks (by the end of their name): light passes, a little dimmed. */
    private static final String[] PARTIAL_SUFFIX = {"_slab", "_stairs", "cauldron", "lectern", "anvil", "brewing_stand", "grindstone",
            "enchanting_table", "hopper", "composter", "decorated_pot", "heavy_core", "chest", "campfire", "daylight_detector",
            "stonecutter", "end_portal_frame", "conduit", "_cluster", "_bud", "piston_head", "_tulip", "_orchid", "_mushroom",
            "berry_bush", "dead_bush", "seagrass", "dripleaf", "spore_blossom", "hanging_roots", "azalea", "moss_carpet", "_egg"};
    /** Partial blocks by exact name. */
    private static final Set<String> PARTIAL_EXACT = Set.of("mud", "snow", "bamboo", "farmland", "dirt_path", "soul_sand", "honey_block",
            "vault", "trial_spawner", "spawner", "cake", "dandelion", "poppy", "allium", "azure_bluet", "oxeye_daisy", "cornflower",
            "lily_of_the_valley", "lily_pad", "lilac", "rose_bush", "peony", "sunflower", "kelp", "kelp_plant", "torchflower");

    private static final Map<String, Integer> LIGHT = Map.ofEntries(
            Map.entry("lantern", 15), Map.entry("soul_lantern", 10), Map.entry("torch", 14), Map.entry("wall_torch", 14),
            Map.entry("soul_torch", 10), Map.entry("soul_wall_torch", 10), Map.entry("redstone_torch", 7), Map.entry("sea_lantern", 15),
            Map.entry("glowstone", 15), Map.entry("shroomlight", 15), Map.entry("ochre_froglight", 15), Map.entry("verdant_froglight", 15),
            Map.entry("pearlescent_froglight", 15), Map.entry("end_rod", 14), Map.entry("jack_o_lantern", 15), Map.entry("beacon", 15),
            Map.entry("conduit", 15), Map.entry("lava", 15), Map.entry("magma_block", 3), Map.entry("crying_obsidian", 10),
            Map.entry("enchanting_table", 7), Map.entry("ender_chest", 7), Map.entry("glow_lichen", 7), Map.entry("amethyst_cluster", 5),
            Map.entry("brewing_stand", 1), Map.entry("sculk_sensor", 1), Map.entry("respawn_anchor", 0), Map.entry("sea_pickle", 6),
            Map.entry("lava_cauldron", 15), Map.entry("vault", 6));

    private Kinds() {
    }

    public static boolean air(String state) {
        return state == null || St.id(state).equals(St.AIR) || St.id(state).equals("minecraft:cave_air");
    }

    /** How much light a block takes off on its way through: 1 as air, 2 for partial blocks, 16 (none passes) for opaque. */
    public static int lightCost(String state) {
        if (air(state)) return 1;
        String id = St.id(state);
        if (id.startsWith("supernaturalcraft:")) return MOD_PARTIAL.contains(id) ? 2 : 16;
        String path = St.path(state);
        if (path.equals("tinted_glass") || path.equals("sea_lantern") || path.equals("jack_o_lantern")) return 16;
        for (String s : CLEAR_CUBE_SUFFIX) if (path.endsWith(s)) return path.endsWith("leaves") ? 2 : 1;
        for (String s : THIN_SUFFIX) if (path.endsWith(s)) return 1;
        for (String s : PARTIAL_SUFFIX) if (path.endsWith(s)) return 2;
        if (PARTIAL_EXACT.contains(path) || path.startsWith("potted_")) return 2;
        return 16;
    }

    /** A full cube that blocks light and is sturdy on every face (the default: anything not known to be thin or partial). */
    public static boolean opaqueCube(String state) {
        return lightCost(state) >= 16;
    }

    /** A full cube of any kind (opaque, or glass, grates and leaves). */
    public static boolean fullCube(String state) {
        if (air(state)) return false;
        if (opaqueCube(state)) return true;
        String path = St.path(state);
        if (path.endsWith("_slab")) return "double".equals(St.get(state, "type"));
        for (String s : CLEAR_CUBE_SUFFIX) if (path.endsWith(s)) return true;
        return path.equals("tinted_glass");
    }

    /** The light a block gives (vanilla levels; candles 3 each, copper bulbs by their weathering, lit or not). */
    public static int emission(String state) {
        if (air(state)) return 0;
        String id = St.id(state);
        if (id.startsWith("supernaturalcraft:")) return MOD_LIGHT.getOrDefault(id, 0);
        String path = St.path(state);
        if (path.endsWith("candle") || path.endsWith("candle_cake")) {
            if (!"true".equals(St.get(state, "lit"))) return 0;
            String n = St.get(state, "candles");
            return 3 * (n == null ? 1 : Integer.parseInt(n));
        }
        if (path.contains("copper_bulb")) {
            if (!"true".equals(St.get(state, "lit"))) return 0;
            if (path.contains("oxidized")) return 4;
            if (path.contains("weathered")) return 8;
            if (path.contains("exposed")) return 12;
            return 15;
        }
        if (path.equals("redstone_lamp")) return "true".equals(St.get(state, "lit")) ? 15 : 0;
        if (path.endsWith("campfire")) return "true".equals(St.get(state, "lit")) ? (path.startsWith("soul") ? 10 : 15) : 0;
        if (path.equals("furnace") || path.equals("smoker") || path.equals("blast_furnace")) return "true".equals(St.get(state, "lit")) ? 13 : 0;
        return LIGHT.getOrDefault(path, 0);
    }

    public static boolean stairs(String state) {
        return state != null && St.path(state).endsWith("_stairs");
    }

    /** Bars and panes (vanilla {@code IronBarsBlock}). */
    public static boolean barsLike(String state) {
        if (state == null) return false;
        String p = St.path(state);
        return p.equals("iron_bars") || p.endsWith("glass_pane");
    }

    public static boolean fence(String state) {
        return state != null && St.path(state).endsWith("_fence");
    }

    public static boolean woodenFence(String state) {
        return fence(state) && !St.path(state).equals("nether_brick_fence");
    }

    public static boolean wall(String state) {
        if (state == null) return false;
        String p = St.path(state);
        return p.endsWith("_wall") && !p.endsWith("_sign") && !p.contains("wall_");
    }

    public static boolean fenceGate(String state) {
        return state != null && St.path(state).endsWith("_fence_gate");
    }

    /** Vanilla's {@code isExceptionForConnection}: leaves, pumpkins, melons, barriers and shulker boxes never join. */
    public static boolean connectionException(String state) {
        String p = St.path(state);
        return p.endsWith("leaves") || p.equals("barrier") || p.equals("carved_pumpkin") || p.equals("jack_o_lantern") || p.equals("melon")
                || p.equals("pumpkin") || p.endsWith("shulker_box");
    }

    /**
     * Whether {@code state}'s face toward {@code face} (the direction from the block out through that face) is sturdy: full
     * cubes on all faces, slabs on their flat side, stairs on their back and their flat bottom or top.
     */
    public static boolean sturdy(String state, String face) {
        if (air(state)) return false;
        if (fullCube(state)) return true;
        String path = St.path(state);
        if (path.endsWith("_slab")) {
            String type = St.get(state, "type");
            return "double".equals(type) || "top".equals(type) && face.equals("up") || "bottom".equals(type) && face.equals("down");
        }
        if (path.endsWith("_stairs")) {
            boolean top = "top".equals(St.get(state, "half"));
            if (face.equals(top ? "up" : "down")) return true;
            return face.equals(St.get(state, "facing")) && "straight".equals(St.get(state, "shape"));
        }
        if (path.endsWith("_trapdoor") && !"true".equals(St.get(state, "open"))) {
            return face.equals("top".equals(St.get(state, "half")) ? "up" : "down");
        }
        return false;
    }

    /** Whether something hanging (a lantern, a chain's end) can hang under {@code above}: vanilla {@code canSupportCenter(DOWN)}. */
    public static boolean holdsBelow(String above) {
        if (air(above)) return false;
        if (sturdy(above, "down")) return true;
        String p = St.path(above);
        return p.equals("chain") && "y".equals(St.get(above, "axis")) || p.endsWith("_fence") || p.endsWith("_wall") || p.equals("iron_bars")
                || p.endsWith("glass_pane") || p.equals("end_rod") || p.equals("lightning_rod") || p.contains("hanging_sign");
    }

    /** Whether something can stand on {@code below} at its centre: vanilla {@code canSupportCenter(UP)}. */
    public static boolean holdsAbove(String below) {
        if (air(below)) return false;
        if (sturdy(below, "up")) return true;
        String p = St.path(below);
        if (St.id(below).startsWith("supernaturalcraft:")) return !St.id(below).equals("supernaturalcraft:bunker_door");
        return p.endsWith("_fence") || p.endsWith("_wall") || p.equals("chain") && "y".equals(St.get(below, "axis")) || p.equals("iron_bars")
                || p.endsWith("glass_pane") || p.equals("end_rod") || p.equals("lightning_rod") || p.equals("hopper") || p.equals("cauldron")
                || p.endsWith("_cauldron") || p.equals("lectern") || p.equals("anvil") || p.endsWith("_anvil") || p.equals("enchanting_table")
                || p.equals("stonecutter") || p.equals("composter") || p.equals("barrel") || p.endsWith("chest") || p.equals("heavy_core")
                || p.equals("scaffolding") || p.equals("grindstone") || p.equals("brewing_stand") || p.equals("decorated_pot");
    }
}
