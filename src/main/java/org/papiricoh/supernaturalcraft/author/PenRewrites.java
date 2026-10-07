package org.papiricoh.supernaturalcraft.author;

import java.util.List;
import java.util.Set;

/**
 * What the Author's Pen may rewrite, and into what (pure, tested in JUnit): a curated cycle of biomes, the turn of the
 * sky (clear day, rain, storm, clear night), and creatures into others of the same tier. Bosses and players are never
 * in a tier: the Pen cannot touch them.
 */
public final class PenRewrites {

    /** Mode ids ({@code PEN_MODE}). */
    public static final int BIOME = 0, SKY = 1, CREATURE = 2, MODES = 3;
    /** Cooldowns, in ticks: a minute, two, thirty seconds. */
    public static final int[] COOLDOWN = {1200, 2400, 600};
    /** Radius of a rewritten biome, in blocks. */
    public static final int RADIUS = 8;

    /** The biomes the Pen writes, in order: a used one turns into the next. */
    public static final List<String> BIOMES = List.of("minecraft:plains", "minecraft:flower_forest", "minecraft:cherry_grove",
            "minecraft:birch_forest", "minecraft:forest", "minecraft:taiga", "minecraft:snowy_plains", "minecraft:desert",
            "minecraft:savanna", "minecraft:badlands", "minecraft:jungle", "minecraft:swamp", "minecraft:mushroom_fields");

    /** Creatures of a kind and weight: a rewritten one becomes the next in its tier. */
    public static final List<List<String>> TIERS = List.of(
            List.of("minecraft:chicken", "minecraft:rabbit", "minecraft:parrot", "minecraft:bat"),
            List.of("minecraft:cow", "minecraft:pig", "minecraft:sheep", "minecraft:goat", "minecraft:mooshroom"),
            List.of("minecraft:horse", "minecraft:donkey", "minecraft:llama", "minecraft:camel"),
            List.of("minecraft:wolf", "minecraft:fox", "minecraft:cat", "minecraft:ocelot"),
            List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:spider", "minecraft:creeper", "minecraft:husk", "minecraft:stray"),
            List.of("minecraft:witch", "minecraft:pillager", "minecraft:vindicator", "minecraft:blaze"),
            List.of("minecraft:enderman", "minecraft:piglin", "minecraft:zombified_piglin"),
            List.of("minecraft:iron_golem", "minecraft:snow_golem"),
            List.of("minecraft:squid", "minecraft:glow_squid", "minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish"),
            List.of("minecraft:villager", "minecraft:wandering_trader"),
            List.of("supernaturalcraft:black_eyed_demon", "supernaturalcraft:demon_occultist"));

    /** Never rewritten whatever a tier says: the great enemies, and the hunters. */
    public static final Set<String> NEVER = Set.of("minecraft:player", "minecraft:ender_dragon", "minecraft:wither",
            "minecraft:warden", "minecraft:elder_guardian");

    /** The sky, as the Pen turns it. */
    public enum Sky {
        CLEAR_DAY, RAIN, STORM, CLEAR_NIGHT;

        /** The next sky after {@code this}. */
        public Sky next() {
            return values()[(ordinal() + 1) % values().length];
        }

        /** The sky now, read from the world. */
        public static Sky of(boolean raining, boolean thundering, boolean night) {
            if (thundering) return STORM;
            if (raining) return RAIN;
            return night ? CLEAR_NIGHT : CLEAR_DAY;
        }
    }

    private PenRewrites() {
    }

    /** The biome {@code current} becomes: the next in {@link #BIOMES} (the first if it is not there). */
    public static String nextBiome(String current) {
        int i = BIOMES.indexOf(current);
        return BIOMES.get((i + 1) % BIOMES.size());
    }

    /**
     * What creature {@code id} becomes, or null if the Pen may not touch it.
     *
     * @param boss whether it is one of the {@code #bosses}
     */
    public static String rewrite(String id, boolean boss) {
        if (boss || NEVER.contains(id)) return null;
        for (List<String> tier : TIERS) {
            int i = tier.indexOf(id);
            if (i >= 0) return tier.get((i + 1) % tier.size());
        }
        return null;
    }
}
