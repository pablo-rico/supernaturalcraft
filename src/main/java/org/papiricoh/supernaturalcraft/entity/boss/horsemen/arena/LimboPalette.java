package org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The world of the dead, one block at a time (pure, tested in JUnit): every block of Death's living world has a grey
 * twin, and the twin turns back into it. One to one, so the world can flip back and forth as often as he likes.
 */
public final class LimboPalette {

    private static final Map<String, String> TO_LIMBO = new LinkedHashMap<>();
    private static final Map<String, String> TO_LIVING = new HashMap<>();

    static {
        pair("minecraft:grass_block", "minecraft:light_gray_concrete");
        pair("minecraft:dirt", "minecraft:gray_concrete");
        pair("minecraft:coarse_dirt", "minecraft:gray_terracotta");
        pair("minecraft:podzol", "minecraft:light_gray_terracotta");
        pair("minecraft:moss_block", "minecraft:tuff");
        pair("minecraft:oak_log", "minecraft:basalt");
        pair("minecraft:oak_leaves[persistent=true]", "minecraft:cobweb");
        pair("minecraft:oak_planks", "minecraft:polished_andesite");
        pair("minecraft:cobblestone", "minecraft:cobbled_deepslate");
        pair("minecraft:stone_bricks", "minecraft:deepslate_bricks");
        pair("minecraft:short_grass", "minecraft:light_gray_carpet");
        pair("minecraft:poppy", "minecraft:gray_carpet");
        pair("minecraft:dandelion", "minecraft:white_carpet");
        pair("minecraft:cornflower", "minecraft:black_carpet");
        pair("minecraft:hay_block", "minecraft:bone_block");
        pair("minecraft:lantern", "minecraft:soul_lantern");
        pair("minecraft:torch", "minecraft:soul_torch");
        pair("minecraft:water", "minecraft:light_gray_stained_glass");
    }

    private LimboPalette() {
    }

    private static void pair(String living, String limbo) {
        TO_LIMBO.put(living, limbo);
        TO_LIVING.put(limbo, living);
    }

    /** The grey twin, or the block itself if it has none. */
    public static String toLimbo(String living) {
        return TO_LIMBO.getOrDefault(living, living);
    }

    /** Back to life, or the block itself if it is not one of the grey twins. */
    public static String toLiving(String limbo) {
        return TO_LIVING.getOrDefault(limbo, limbo);
    }

    public static Map<String, String> pairs() {
        return java.util.Collections.unmodifiableMap(TO_LIMBO);
    }
}
