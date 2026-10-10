package org.papiricoh.supernaturalcraft.buildkit;

/**
 * A kind of wood and everything vanilla 1.21.1 makes of it (pure). {@code name} is the prefix of its ids ({@code oak},
 * {@code dark_oak}…); the log ids differ for the nether stems and bamboo, so they are fields.
 */
public record Wood(String name, String log, String wood, String strippedLog, String strippedWood, String leaves) {

    public static final Wood OAK = std("oak", "oak_leaves");
    public static final Wood SPRUCE = std("spruce", "spruce_leaves");
    public static final Wood BIRCH = std("birch", "birch_leaves");
    public static final Wood JUNGLE = std("jungle", "jungle_leaves");
    public static final Wood ACACIA = std("acacia", "acacia_leaves");
    public static final Wood DARK_OAK = std("dark_oak", "dark_oak_leaves");
    public static final Wood MANGROVE = std("mangrove", "mangrove_leaves");
    public static final Wood CHERRY = std("cherry", "cherry_leaves");
    public static final Wood BAMBOO = new Wood("bamboo", "minecraft:bamboo_block", "minecraft:bamboo_block", "minecraft:stripped_bamboo_block",
            "minecraft:stripped_bamboo_block", null);
    public static final Wood CRIMSON = new Wood("crimson", "minecraft:crimson_stem", "minecraft:crimson_hyphae", "minecraft:stripped_crimson_stem",
            "minecraft:stripped_crimson_hyphae", null);
    public static final Wood WARPED = new Wood("warped", "minecraft:warped_stem", "minecraft:warped_hyphae", "minecraft:stripped_warped_stem",
            "minecraft:stripped_warped_hyphae", null);

    private static Wood std(String n, String leaves) {
        return new Wood(n, "minecraft:" + n + "_log", "minecraft:" + n + "_wood", "minecraft:stripped_" + n + "_log",
                "minecraft:stripped_" + n + "_wood", "minecraft:" + leaves);
    }

    public String planks() {
        return "minecraft:" + name + "_planks";
    }

    public String stairsId() {
        return "minecraft:" + name + "_stairs";
    }

    public String slabId() {
        return "minecraft:" + name + "_slab";
    }

    public String fenceId() {
        return "minecraft:" + name + "_fence";
    }

    public String trapdoorId() {
        return "minecraft:" + name + "_trapdoor";
    }

    public String doorId() {
        return "minecraft:" + name + "_door";
    }

    public String gateId() {
        return "minecraft:" + name + "_fence_gate";
    }

    public String buttonId() {
        return "minecraft:" + name + "_button";
    }

    public String plateId() {
        return "minecraft:" + name + "_pressure_plate";
    }

    /** Planks, their stairs and slab (fences are not walls: no wall). */
    public Family family() {
        return new Family(planks(), stairsId(), slabId(), null);
    }

    public String stairs(Dir facing, boolean top) {
        return St.stairs(stairsId(), facing, top);
    }

    public String slabBottom() {
        return St.slabBottom(slabId());
    }

    public String slabTop() {
        return St.slabTop(slabId());
    }

    public String fence() {
        return St.fence(fenceId());
    }

    public String trapdoor(Dir facing, boolean top, boolean open) {
        return St.trapdoor(trapdoorId(), facing, top, open);
    }

    /** A log along an axis. */
    public String log(String axis) {
        return St.axis(log, axis);
    }

    public String strippedLog(String axis) {
        return St.axis(strippedLog, axis);
    }

    /** Bark on all sides (a beam whose end grain should not show). */
    public String wood(String axis) {
        return St.axis(wood, axis);
    }

    public String strippedWood(String axis) {
        return St.axis(strippedWood, axis);
    }

    public String leavesState() {
        if (leaves == null) throw new IllegalStateException(name + " has no leaves");
        return St.leaves(leaves);
    }
}
