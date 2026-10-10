package org.papiricoh.supernaturalcraft.buildkit;

import java.util.Map;
import java.util.Set;

/**
 * What a layout needs to know about a block without Minecraft (pure): whether it is a full opaque cube, how it passes light, how
 * much light it gives, which of its faces are sturdy (what bars, fences and walls join and what can hang from it), what it needs
 * to stand on, whether a walker passes through it, and whether it has a block entity. Approximations of vanilla 1.21.1 that err
 * on the dark and the cautious side. Mod blocks ({@code supernaturalcraft:}) count as opaque cubes unless listed in
 * {@link #declarePartial}.
 */
public final class Kinds {

    private static final java.util.concurrent.ConcurrentHashMap<String, Integer> MOD_PARTIAL = new java.util.concurrent.ConcurrentHashMap<>();

    /** Full cubes light passes through. */
    private static final String[] CLEAR_CUBE_SUFFIX = {"glass", "copper_grate", "leaves"};
    /** Thin things: light passes as through air; nothing stands on them. */
    private static final String[] THIN_SUFFIX = {"_bars", "chain", "_pane", "lantern", "torch", "_carpet", "_trapdoor", "_door", "_fence",
            "_fence_gate", "_wall", "_rod", "_sign", "_hanging_sign", "_button", "_pressure_plate", "candle", "_banner", "_bed",
            "flower_pot", "ladder", "vine", "vines", "vines_plant", "rail", "_coral_fan", "short_grass", "tall_grass", "fern", "lichen",
            "_sapling", "cobweb", "scaffolding", "tripwire", "string", "lever", "_head", "_skull", "pointed_dripstone", "sea_pickle",
            "bell", "_frame", "_petals", "light", "sugar_cane", "glow_lichen", "lily_pad", "_propagule", "_roots", "sculk_vein",
            "small_dripleaf", "big_dripleaf_stem", "pitcher_plant", "pitcher_crop", "torchflower_crop", "wheat", "carrots",
            "potatoes", "beetroots", "melon_stem", "pumpkin_stem", "cocoa", "frogspawn", "snow"};
    /** Partial blocks (by the end of their name): light passes, a little dimmed. */
    private static final String[] PARTIAL_SUFFIX = {"_slab", "_stairs", "cauldron", "lectern", "anvil", "brewing_stand", "grindstone",
            "enchanting_table", "hopper", "composter", "decorated_pot", "heavy_core", "chest", "campfire", "daylight_detector",
            "stonecutter", "end_portal_frame", "conduit", "_cluster", "_bud", "piston_head", "_tulip", "_orchid", "_mushroom",
            "berry_bush", "dead_bush", "seagrass", "dripleaf", "spore_blossom", "hanging_roots", "azalea", "moss_carpet", "_egg",
            "candle_cake", "_cake", "bamboo_sapling", "cactus", "dried_kelp_block", "chorus_plant", "chorus_flower", "_banner",
            "bamboo", "_shulker_box", "shulker_box", "bell", "lantern"};
    /** Partial blocks by exact name. */
    private static final Set<String> PARTIAL_EXACT = Set.of("mud", "bamboo", "farmland", "dirt_path", "soul_sand", "honey_block",
            "vault", "trial_spawner", "spawner", "cake", "dandelion", "poppy", "allium", "azure_bluet", "oxeye_daisy", "cornflower",
            "lily_of_the_valley", "lilac", "rose_bush", "peony", "sunflower", "kelp", "kelp_plant", "torchflower", "wither_rose",
            "blue_orchid", "large_fern", "crafter", "sniffer_egg", "mangrove_roots", "water", "lava", "bubble_column", "ice",
            "frosted_ice", "slime_block", "beacon", "powder_snow", "pink_petals", "sweet_berry_bush", "dead_bush", "nether_sprouts",
            "crimson_roots", "warped_roots", "weeping_vines", "twisting_vines", "glow_berries", "cave_vines", "cave_vines_plant",
            "tinted_glass");

    /** Flowers and small plants that need soil under them (by exact name). */
    private static final Set<String> SOIL_PLANTS = Set.of("short_grass", "fern", "dandelion", "poppy", "blue_orchid", "allium",
            "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley",
            "torchflower", "wither_rose", "pink_petals", "tall_grass", "large_fern", "lilac", "rose_bush", "peony", "sunflower",
            "pitcher_plant", "sweet_berry_bush", "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling", "acacia_sapling",
            "dark_oak_sapling", "cherry_sapling", "mangrove_propagule", "azalea", "flowering_azalea", "dead_bush", "sugar_cane",
            "bamboo", "brown_mushroom", "red_mushroom", "small_dripleaf", "big_dripleaf");
    private static final Set<String> SOILS = Set.of("grass_block", "dirt", "coarse_dirt", "podzol", "rooted_dirt", "moss_block", "mud",
            "muddy_mangrove_roots", "mycelium", "farmland", "clay", "sand", "red_sand", "suspicious_sand", "gravel");

    private static final Map<String, Integer> LIGHT = Map.ofEntries(
            Map.entry("lantern", 15), Map.entry("soul_lantern", 10), Map.entry("torch", 14), Map.entry("wall_torch", 14),
            Map.entry("soul_torch", 10), Map.entry("soul_wall_torch", 10), Map.entry("redstone_torch", 7), Map.entry("redstone_wall_torch", 7),
            Map.entry("sea_lantern", 15), Map.entry("glowstone", 15), Map.entry("shroomlight", 15), Map.entry("ochre_froglight", 15),
            Map.entry("verdant_froglight", 15), Map.entry("pearlescent_froglight", 15), Map.entry("end_rod", 14),
            Map.entry("jack_o_lantern", 15), Map.entry("beacon", 15), Map.entry("conduit", 15), Map.entry("lava", 15),
            Map.entry("magma_block", 3), Map.entry("crying_obsidian", 10), Map.entry("enchanting_table", 7), Map.entry("ender_chest", 7),
            Map.entry("glow_lichen", 7), Map.entry("amethyst_cluster", 5), Map.entry("large_amethyst_bud", 4),
            Map.entry("brewing_stand", 1), Map.entry("sculk_sensor", 1), Map.entry("sea_pickle", 6), Map.entry("lava_cauldron", 15),
            Map.entry("vault", 6), Map.entry("trial_spawner", 4), Map.entry("brown_mushroom", 1), Map.entry("dragon_egg", 1),
            Map.entry("end_gateway", 15), Map.entry("fire", 15), Map.entry("soul_fire", 10), Map.entry("nether_portal", 11),
            Map.entry("spore_blossom", 0), Map.entry("small_amethyst_bud", 1), Map.entry("medium_amethyst_bud", 2),
            Map.entry("respawn_anchor", 0), Map.entry("glow_berries", 14));

    /** Blocks with a block entity (they cannot be written through {@code ArenaController.mutate}, which keeps no block entities). */
    private static final String[] BLOCK_ENTITY_SUFFIX = {"chest", "_sign", "_hanging_sign", "_banner", "_bed", "shulker_box",
            "_head", "_skull", "campfire", "_command_block", "furnace"};
    private static final Set<String> BLOCK_ENTITY_EXACT = Set.of("barrel", "smoker", "blast_furnace", "brewing_stand", "beacon", "bell",
            "beehive", "bee_nest", "chiseled_bookshelf", "command_block", "comparator", "conduit", "crafter", "daylight_detector",
            "decorated_pot", "dispenser", "dropper", "enchanting_table", "end_gateway", "end_portal", "hopper", "jigsaw", "jukebox",
            "lectern", "spawner", "moving_piston", "sculk_catalyst", "sculk_sensor", "calibrated_sculk_sensor", "sculk_shrieker",
            "structure_block", "trial_spawner", "vault", "suspicious_sand", "suspicious_gravel", "piston_head");

    private Kinds() {
    }

    /** Mod blocks that are not full opaque cubes: {@code lightCost} 1 (thin) or 2 (partial). */
    public static void declarePartial(String id, int lightCost) {
        MOD_PARTIAL.put(id, lightCost);
    }

    public static boolean air(String state) {
        if (state == null) return true;
        String id = St.id(state);
        return id.equals(St.AIR) || id.equals("minecraft:cave_air") || id.equals("minecraft:void_air");
    }

    /** How much light a block takes off on its way through: 1 as air, 2 for partial blocks, 16 (none passes) for opaque. */
    public static int lightCost(String state) {
        if (air(state)) return 1;
        String id = St.id(state);
        if (!id.startsWith("minecraft:")) return MOD_PARTIAL.getOrDefault(id, 16);
        String path = St.path(state);
        if (path.equals("tinted_glass") || path.equals("sea_lantern") || path.equals("jack_o_lantern") || path.equals("glowstone")
                || path.endsWith("froglight") || path.equals("shroomlight")) return 16;
        if (path.equals("water") || path.equals("ice")) return 2;
        for (String s : CLEAR_CUBE_SUFFIX) if (path.endsWith(s)) return path.endsWith("leaves") ? 2 : 1;
        if (path.equals("light")) return 1;
        for (String s : PARTIAL_SUFFIX) if (path.endsWith(s)) return 2;
        for (String s : THIN_SUFFIX) if (path.endsWith(s)) return 1;
        if (PARTIAL_EXACT.contains(path) || path.startsWith("potted_")) return 2;
        return 16;
    }

    /** A full cube that blocks light and is sturdy on every face (the default: anything not known to be thin or partial). */
    public static boolean opaqueCube(String state) {
        return lightCost(state) >= 16;
    }

    /** A full cube of any kind (opaque, or glass, grates and leaves, or a double slab). */
    public static boolean fullCube(String state) {
        if (air(state)) return false;
        if (opaqueCube(state)) return true;
        String path = St.path(state);
        if (path.endsWith("_slab")) return "double".equals(St.get(state, "type"));
        if (path.equals("ice") || path.equals("slime_block") || path.equals("honey_block") || path.equals("tinted_glass")) return true;
        for (String s : CLEAR_CUBE_SUFFIX) if (path.endsWith(s)) return !path.endsWith("pane");
        return false;
    }

    /** The light a block gives (vanilla levels; candles 3 each, copper bulbs by their weathering, the light block its level). */
    public static int emission(String state) {
        if (air(state)) return 0;
        String id = St.id(state);
        if (!id.startsWith("minecraft:")) return 0;
        String path = St.path(state);
        if (path.equals("light")) {
            String l = St.get(state, "level");
            return l == null ? 15 : Integer.parseInt(l);
        }
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
        if (path.equals("sea_pickle")) {
            if (!"true".equals(St.get(state, "waterlogged"))) return 0;
            String n = St.get(state, "pickles");
            return 3 + 3 * (n == null ? 1 : Integer.parseInt(n));
        }
        if (path.equals("cave_vines") || path.equals("cave_vines_plant")) return "true".equals(St.get(state, "berries")) ? 14 : 0;
        return LIGHT.getOrDefault(path, 0);
    }

    public static boolean stairs(String state) {
        return state != null && St.path(state).endsWith("_stairs");
    }

    public static boolean slab(String state) {
        return state != null && St.path(state).endsWith("_slab");
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

    public static boolean leaves(String state) {
        return state != null && St.path(state).endsWith("_leaves");
    }

    public static boolean log(String state) {
        if (state == null) return false;
        String p = St.path(state);
        return p.endsWith("_log") || p.endsWith("_wood") || p.endsWith("_stem") && !p.contains("melon") && !p.contains("pumpkin")
                || p.endsWith("_hyphae") || p.equals("bamboo_block") || p.equals("stripped_bamboo_block");
    }

    public static boolean water(String state) {
        return state != null && (St.path(state).equals("water") || "true".equals(St.get(state, "waterlogged")));
    }

    /** Vanilla's {@code isExceptionForConnection}: leaves, pumpkins, melons, barriers and shulker boxes never join. */
    public static boolean connectionException(String state) {
        String p = St.path(state);
        return p.endsWith("leaves") || p.equals("barrier") || p.equals("carved_pumpkin") || p.equals("jack_o_lantern") || p.equals("melon")
                || p.equals("pumpkin") || p.endsWith("shulker_box");
    }

    /**
     * Whether {@code state}'s face toward {@code face} (the direction from the block out through that face) is sturdy: full
     * cubes on all faces, slabs on their flat side, stairs on their back and their flat bottom or top, closed trapdoors on their
     * flat side.
     */
    public static boolean sturdy(String state, Dir face) {
        if (air(state)) return false;
        if (fullCube(state)) return true;
        String path = St.path(state);
        if (path.endsWith("_slab")) {
            String type = St.get(state, "type");
            return "double".equals(type) || "top".equals(type) && face == Dir.UP || ("bottom".equals(type) || type == null) && face == Dir.DOWN;
        }
        if (path.endsWith("_stairs")) {
            boolean top = "top".equals(St.get(state, "half"));
            if (face == (top ? Dir.UP : Dir.DOWN)) return true;
            return face.id().equals(St.get(state, "facing")) && "straight".equals(St.get(state, "shape"));
        }
        if (path.endsWith("_trapdoor") && !"true".equals(St.get(state, "open"))) {
            return face == ("top".equals(St.get(state, "half")) ? Dir.UP : Dir.DOWN);
        }
        if (path.equals("dirt_path") || path.equals("farmland") || path.equals("mud") || path.equals("soul_sand")) return face != Dir.UP;
        return false;
    }

    /** Whether something hanging (a lantern, a chain's end, a hanging sign) can hang under {@code above}: vanilla {@code canSupportCenter(DOWN)}. */
    public static boolean holdsBelow(String above) {
        if (air(above)) return false;
        if (sturdy(above, Dir.DOWN)) return true;
        String p = St.path(above);
        return p.equals("chain") && "y".equals(St.get(above, "axis")) || p.endsWith("_fence") || p.endsWith("_wall") && wall(above)
                || p.equals("iron_bars") || p.endsWith("glass_pane") || p.equals("end_rod") || p.equals("lightning_rod")
                || p.endsWith("_leaves") || p.endsWith("_log") || p.endsWith("_wood");
    }

    /** Whether something can stand on {@code below} at its centre: vanilla {@code canSupportCenter(UP)}. */
    public static boolean holdsAbove(String below) {
        if (air(below)) return false;
        if (sturdy(below, Dir.UP)) return true;
        String p = St.path(below);
        if (!St.id(below).startsWith("minecraft:")) return MOD_PARTIAL.getOrDefault(St.id(below), 16) >= 2;
        return p.equals("dirt_path") || p.equals("farmland") || p.equals("mud") || p.equals("soul_sand") || p.endsWith("_fence")
                || wall(below) || p.equals("chain") && "y".equals(St.get(below, "axis")) || p.equals("iron_bars")
                || p.endsWith("glass_pane") || p.equals("end_rod") || p.equals("lightning_rod") || p.equals("hopper") || p.equals("cauldron")
                || p.endsWith("_cauldron") || p.equals("lectern") || p.equals("anvil") || p.endsWith("_anvil") || p.equals("enchanting_table")
                || p.equals("stonecutter") || p.equals("composter") || p.equals("barrel") || p.endsWith("chest") || p.equals("heavy_core")
                || p.equals("scaffolding") || p.equals("grindstone") || p.equals("brewing_stand") || p.equals("decorated_pot")
                || p.equals("crafter") || p.equals("bell");
    }

    /** Small plants that need soil (grass, dirt, moss…) right under them (the lower half of tall ones). */
    public static boolean soilPlant(String state) {
        if (state == null) return false;
        String p = St.path(state);
        if (!SOIL_PLANTS.contains(p)) return false;
        return !"upper".equals(St.get(state, "half"));
    }

    public static boolean soil(String state) {
        return state != null && SOILS.contains(St.path(state));
    }

    /** The two-block things whose halves must match: doors, tall plants (by {@code half}) and beds (by {@code part}). */
    public static boolean twoHigh(String state) {
        if (state == null) return false;
        String h = St.get(state, "half");
        return ("lower".equals(h) || "upper".equals(h)) && !stairs(state);
    }

    /** Whether the block has a block entity (chests, signs, banners, beds, barrels, lecterns, campfires, pots, heads…). */
    public static boolean blockEntity(String state) {
        if (air(state)) return false;
        String p = St.path(state);
        if (BLOCK_ENTITY_EXACT.contains(p)) return true;
        for (String s : BLOCK_ENTITY_SUFFIX) if (p.endsWith(s)) return !p.equals("lit_furnace");
        return false;
    }

    // --- walking ---------------------------------------------------------------------------------------------------------------

    /** Nothing in a walker's way: air, carpets, plants, torches, signs, banners, doors and gates (they open), open trapdoors, light. */
    public static boolean passable(String s) {
        if (air(s)) return true;
        String p = St.path(s);
        if (p.endsWith("_trapdoor")) return "true".equals(St.get(s, "open"));
        if (p.equals("snow")) return "1".equals(St.get(s, "layers")) || St.get(s, "layers") == null;
        return p.endsWith("_door") || p.endsWith("_fence_gate") || p.endsWith("_carpet") || p.endsWith("torch") || p.endsWith("_sign")
                || p.endsWith("_banner") || p.equals("vine") || p.endsWith("_button") || p.endsWith("_pressure_plate") || p.equals("light")
                || p.endsWith("_petals") || p.equals("glow_lichen") || p.equals("lever") || p.endsWith("rail") || p.equals("tripwire")
                || p.equals("water") || p.equals("ladder") || p.equals("cobweb")
                || soilPlant(s) && !p.equals("sweet_berry_bush") && !p.equals("bamboo") && !p.equals("sugar_cane") && !p.equals("big_dripleaf")
                || "upper".equals(St.get(s, "half")) && !stairs(s) && !p.endsWith("_door") && SOIL_PLANTS.contains(p);
    }

    /** Something a walker can stand on: solid and not taller than a block (fences, walls and closed gates are 1.5 high). */
    public static boolean floor(String s) {
        if (passable(s)) return false;
        String p = St.path(s);
        return !fence(s) && !wall(s) && !p.equals("lava") && !p.endsWith("_fence_gate") && !p.equals("cactus") && !p.equals("magma_block")
                && !p.equals("sweet_berry_bush") && !p.equals("bamboo") && !p.equals("sugar_cane");
    }
}
