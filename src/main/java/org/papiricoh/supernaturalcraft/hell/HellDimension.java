package org.papiricoh.supernaturalcraft.hell;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Hell: a roofed cavern world like the Nether, 256 blocks tall, with the Pit and Lucifer's Cage
 * at 0, 0. Everything about its shape is a datapack entry (see datagen {@code SNHell}); these
 * are the keys the code refers to it by.
 */
public final class HellDimension {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("hell");
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, ID);
    public static final ResourceKey<LevelStem> STEM = ResourceKey.create(Registries.LEVEL_STEM, ID);
    public static final ResourceKey<DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ID);
    public static final ResourceKey<NoiseGeneratorSettings> NOISE = ResourceKey.create(Registries.NOISE_SETTINGS, ID);
    /** The client's sky, fog and lightmap for Hell. */
    public static final ResourceLocation EFFECTS = ID;

    public static final ResourceKey<Biome> THE_RACK = biome("the_rack");
    public static final ResourceKey<Biome> ASH_WASTES = biome("ash_wastes");
    public static final ResourceKey<Biome> CROWLEYS_CORRIDORS = biome("crowleys_corridors");
    public static final ResourceKey<Biome> THE_PIT = biome("the_pit");

    /** The world floor and roof: a bedrock crust at each end, lava below {@link #LAVA_LEVEL}. */
    public static final int MIN_Y = 0, HEIGHT = 256, LAVA_LEVEL = 32;
    /** Rifts never open lower or higher than this, nor nearer the Pit than {@link #RIFT_KEEP_OUT}. */
    public static final int RIFT_MIN_Y = 40, RIFT_MAX_Y = 200, RIFT_KEEP_OUT = 112;

    private HellDimension() {
    }

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, SupernaturalCraft.asResource(name));
    }

    public static boolean isHell(Level level) {
        return level.dimension().equals(LEVEL);
    }
}
