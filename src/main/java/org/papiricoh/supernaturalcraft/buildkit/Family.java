package org.papiricoh.supernaturalcraft.buildkit;

/**
 * A block and its shaped siblings (pure): the full block, its stairs, slab and wall (null where vanilla has none). Trims, roofs and
 * mouldings are drawn with a family so one argument changes the whole look. Vanilla 1.21.1 families are constants here; wood
 * families come from {@link Wood#family()}.
 */
public record Family(String block, String stairs, String slab, String wall) {

    public Family {
        block = St.full(block);
        stairs = stairs == null ? null : St.full(stairs);
        slab = slab == null ? null : St.full(slab);
        wall = wall == null ? null : St.full(wall);
    }

    public static final Family STONE = new Family("stone", "stone_stairs", "stone_slab", null);
    public static final Family COBBLESTONE = new Family("cobblestone", "cobblestone_stairs", "cobblestone_slab", "cobblestone_wall");
    public static final Family MOSSY_COBBLESTONE = new Family("mossy_cobblestone", "mossy_cobblestone_stairs", "mossy_cobblestone_slab", "mossy_cobblestone_wall");
    public static final Family STONE_BRICKS = new Family("stone_bricks", "stone_brick_stairs", "stone_brick_slab", "stone_brick_wall");
    public static final Family MOSSY_STONE_BRICKS = new Family("mossy_stone_bricks", "mossy_stone_brick_stairs", "mossy_stone_brick_slab", "mossy_stone_brick_wall");
    public static final Family SMOOTH_STONE = new Family("smooth_stone", null, "smooth_stone_slab", null);
    public static final Family ANDESITE = new Family("andesite", "andesite_stairs", "andesite_slab", "andesite_wall");
    public static final Family POLISHED_ANDESITE = new Family("polished_andesite", "polished_andesite_stairs", "polished_andesite_slab", "andesite_wall");
    public static final Family DIORITE = new Family("diorite", "diorite_stairs", "diorite_slab", "diorite_wall");
    public static final Family POLISHED_DIORITE = new Family("polished_diorite", "polished_diorite_stairs", "polished_diorite_slab", "diorite_wall");
    public static final Family GRANITE = new Family("granite", "granite_stairs", "granite_slab", "granite_wall");
    public static final Family POLISHED_GRANITE = new Family("polished_granite", "polished_granite_stairs", "polished_granite_slab", "granite_wall");
    public static final Family COBBLED_DEEPSLATE = new Family("cobbled_deepslate", "cobbled_deepslate_stairs", "cobbled_deepslate_slab", "cobbled_deepslate_wall");
    public static final Family POLISHED_DEEPSLATE = new Family("polished_deepslate", "polished_deepslate_stairs", "polished_deepslate_slab", "polished_deepslate_wall");
    public static final Family DEEPSLATE_BRICKS = new Family("deepslate_bricks", "deepslate_brick_stairs", "deepslate_brick_slab", "deepslate_brick_wall");
    public static final Family DEEPSLATE_TILES = new Family("deepslate_tiles", "deepslate_tile_stairs", "deepslate_tile_slab", "deepslate_tile_wall");
    public static final Family TUFF = new Family("tuff", "tuff_stairs", "tuff_slab", "tuff_wall");
    public static final Family POLISHED_TUFF = new Family("polished_tuff", "polished_tuff_stairs", "polished_tuff_slab", "polished_tuff_wall");
    public static final Family TUFF_BRICKS = new Family("tuff_bricks", "tuff_brick_stairs", "tuff_brick_slab", "tuff_brick_wall");
    public static final Family BRICKS = new Family("bricks", "brick_stairs", "brick_slab", "brick_wall");
    public static final Family MUD_BRICKS = new Family("mud_bricks", "mud_brick_stairs", "mud_brick_slab", "mud_brick_wall");
    public static final Family SANDSTONE = new Family("sandstone", "sandstone_stairs", "sandstone_slab", "sandstone_wall");
    public static final Family SMOOTH_SANDSTONE = new Family("smooth_sandstone", "smooth_sandstone_stairs", "smooth_sandstone_slab", "sandstone_wall");
    public static final Family CUT_SANDSTONE = new Family("cut_sandstone", "smooth_sandstone_stairs", "cut_sandstone_slab", "sandstone_wall");
    public static final Family RED_SANDSTONE = new Family("red_sandstone", "red_sandstone_stairs", "red_sandstone_slab", "red_sandstone_wall");
    public static final Family SMOOTH_RED_SANDSTONE = new Family("smooth_red_sandstone", "smooth_red_sandstone_stairs", "smooth_red_sandstone_slab", "red_sandstone_wall");
    public static final Family QUARTZ = new Family("quartz_block", "quartz_stairs", "quartz_slab", null);
    public static final Family SMOOTH_QUARTZ = new Family("smooth_quartz", "smooth_quartz_stairs", "smooth_quartz_slab", null);
    public static final Family PRISMARINE = new Family("prismarine", "prismarine_stairs", "prismarine_slab", "prismarine_wall");
    public static final Family PRISMARINE_BRICKS = new Family("prismarine_bricks", "prismarine_brick_stairs", "prismarine_brick_slab", "prismarine_wall");
    public static final Family DARK_PRISMARINE = new Family("dark_prismarine", "dark_prismarine_stairs", "dark_prismarine_slab", "prismarine_wall");
    public static final Family BLACKSTONE = new Family("blackstone", "blackstone_stairs", "blackstone_slab", "blackstone_wall");
    public static final Family POLISHED_BLACKSTONE = new Family("polished_blackstone", "polished_blackstone_stairs", "polished_blackstone_slab", "polished_blackstone_wall");
    public static final Family POLISHED_BLACKSTONE_BRICKS = new Family("polished_blackstone_bricks", "polished_blackstone_brick_stairs", "polished_blackstone_brick_slab", "polished_blackstone_brick_wall");
    public static final Family NETHER_BRICKS = new Family("nether_bricks", "nether_brick_stairs", "nether_brick_slab", "nether_brick_wall");
    public static final Family RED_NETHER_BRICKS = new Family("red_nether_bricks", "red_nether_brick_stairs", "red_nether_brick_slab", "red_nether_brick_wall");
    public static final Family END_STONE_BRICKS = new Family("end_stone_bricks", "end_stone_brick_stairs", "end_stone_brick_slab", "end_stone_brick_wall");
    public static final Family PURPUR = new Family("purpur_block", "purpur_stairs", "purpur_slab", null);
    public static final Family CUT_COPPER = new Family("waxed_cut_copper", "waxed_cut_copper_stairs", "waxed_cut_copper_slab", null);
    public static final Family EXPOSED_CUT_COPPER = new Family("waxed_exposed_cut_copper", "waxed_exposed_cut_copper_stairs", "waxed_exposed_cut_copper_slab", null);
    public static final Family WEATHERED_CUT_COPPER = new Family("waxed_weathered_cut_copper", "waxed_weathered_cut_copper_stairs", "waxed_weathered_cut_copper_slab", null);
    public static final Family OXIDIZED_CUT_COPPER = new Family("waxed_oxidized_cut_copper", "waxed_oxidized_cut_copper_stairs", "waxed_oxidized_cut_copper_slab", null);

    /** Stairs of this family ({@code facing}: where the tall back is). */
    public String stairs(Dir facing, boolean top) {
        if (stairs == null) throw new IllegalStateException(block + " has no stairs");
        return St.stairs(stairs, facing, top);
    }

    public String slabBottom() {
        return St.slabBottom(need(slab, "slab"));
    }

    public String slabTop() {
        return St.slabTop(need(slab, "slab"));
    }

    /** An unconnected wall block ({@link Shapes} joins it). */
    public String wallBlock() {
        return St.wall(need(wall, "wall"));
    }

    public boolean hasStairs() {
        return stairs != null;
    }

    public boolean hasWall() {
        return wall != null;
    }

    /** The same family with another full block (e.g. stone bricks trimmed with their stairs but chiseled in the middle). */
    public Family withBlock(String other) {
        return new Family(other, stairs, slab, wall);
    }

    private String need(String id, String what) {
        if (id == null) throw new IllegalStateException(block + " has no " + what);
        return id;
    }
}
