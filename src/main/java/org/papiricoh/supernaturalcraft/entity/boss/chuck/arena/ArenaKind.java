package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

/**
 * What one cell of an arena the Author writes is made of. Pure: {@link ArenaPalette} is the one place that turns a
 * kind into a block state, so the layouts (and their tests) never touch Minecraft.
 *
 * <p>Nothing here may become a block entity (the arena cannot unwrite those) or an {@code #arena_immune} block (it
 * could never be unwritten again): no bells, no chiseled shelves, no campfires, no Cage bars.
 */
public enum ArenaKind {
    // --- every chapter -----------------------------------------------------------------------------------------
    /** Carved out (a gap in the clouds, a hole in the page). */
    AIR(Flag.NONE),
    PAGE(Flag.SOLID),
    INK(Flag.SOLID),
    BURNING_INK(Flag.SOLID_LIGHT),

    // --- Eden -------------------------------------------------------------------------------------------------
    QUARTZ(Flag.SOLID),
    QUARTZ_BRICKS(Flag.SOLID),
    CHISELED_QUARTZ(Flag.SOLID),
    QUARTZ_PILLAR(Flag.SOLID),
    /** A quartz pillar lying on its side, along x. */
    QUARTZ_PILLAR_X(Flag.SOLID),
    /** A quartz pillar lying on its side, along z. */
    QUARTZ_PILLAR_Z(Flag.SOLID),
    QUARTZ_SLAB(Flag.NONE),
    GOLD(Flag.SOLID),
    MOSS(Flag.SOLID),
    GRASS(Flag.SOLID),
    /** A flowering azalea bush. */
    AZALEA(Flag.NONE),
    FLOWERING_LEAVES(Flag.NONE),
    AZALEA_LEAVES(Flag.NONE),
    OAK_LEAVES(Flag.NONE),
    OAK_LOG(Flag.SOLID),
    OAK_WOOD(Flag.SOLID),
    /** The tree of knowledge's fruit: a golden glow among the leaves. */
    GOLDEN_APPLE(Flag.SOLID_LIGHT),
    WHITE_TULIP(Flag.NONE),
    LILY(Flag.NONE),
    DAISY(Flag.NONE),
    BLUET(Flag.NONE),
    WATER(Flag.WATER),
    /** A shaft of light: an upright end rod. */
    LIGHT_SHAFT(Flag.LIGHT),

    // --- Hell and the Cage --------------------------------------------------------------------------------------
    BLACKSTONE(Flag.SOLID),
    POLISHED_BLACKSTONE(Flag.SOLID),
    BLACKSTONE_BRICKS(Flag.SOLID),
    GILDED_BLACKSTONE(Flag.SOLID),
    CRYING_OBSIDIAN(Flag.SOLID),
    BASALT(Flag.SOLID),
    SMOOTH_BASALT(Flag.SOLID),
    NETHERRACK(Flag.SOLID),
    /** A crack of the lake of fire, cooled to a crust (hot underfoot). */
    MAGMA(Flag.SOLID),
    /** Bars of the Cage (vanilla iron: the Cage's own bars are immune and could never be unwritten). */
    BARS(Flag.NONE),
    CHAIN(Flag.NONE),
    /** Hellfire in a brazier: fire on netherrack, which never burns out. */
    HELLFIRE(Flag.LIGHT),

    // --- the Chorus's storm -------------------------------------------------------------------------------------
    CLOUD(Flag.SOLID),
    CLOUD_SHADE(Flag.SOLID),
    SNOW(Flag.SOLID),
    CALCITE(Flag.SOLID),
    DIORITE(Flag.SOLID),
    /** The body of a bell (a block of raw gold: a real bell is a block entity). */
    BELL(Flag.SOLID),
    SEA_LANTERN(Flag.SOLID_LIGHT),
    /** A lantern hanging from the block above. */
    HANGING_LANTERN(Flag.LIGHT),

    // --- the Scribe's library -----------------------------------------------------------------------------------
    BOOKSHELF(Flag.SOLID),
    DARK_PLANKS(Flag.SOLID),
    DARK_LOG(Flag.SOLID),
    /** The leather of the giant book's cover. */
    COVER(Flag.SOLID),
    /** Three lit candles. */
    CANDLES(Flag.LIGHT),
    LANTERN(Flag.LIGHT),
    FENCE(Flag.NONE);

    /** What a kind is, for the rules and the tests. */
    enum Flag {
        NONE(false, false, false), SOLID(true, false, false), LIGHT(false, true, false), SOLID_LIGHT(true, true, false),
        WATER(false, false, true);

        final boolean solid, light, water;

        Flag(boolean solid, boolean light, boolean water) {
            this.solid = solid;
            this.light = light;
            this.water = water;
        }
    }

    private final Flag flag;

    ArenaKind(Flag flag) {
        this.flag = flag;
    }

    /** A full block a hunter can stand on. */
    public boolean solid() {
        return flag.solid;
    }

    /** Gives off light ("light hurts" reads these). */
    public boolean light() {
        return flag.light;
    }

    /** Water ("water burns" reads these). */
    public boolean water() {
        return flag.water;
    }
}
