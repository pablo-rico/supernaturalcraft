package org.papiricoh.supernaturalcraft.empty;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * The Empty (v0.41), the keys: where angels and demons go when they die, asleep in the dark. A void (datapack entries in
 * {@code datagen/nephilim/NephilimData}); its one floor is written by code ({@link EmptyWorld}). The GameTest server has no
 * such dimension: {@link EmptyWorld#level} then falls back to a far corner of the Overworld.
 */
public final class EmptyDimension {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("the_empty");
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, ID);
    public static final ResourceKey<LevelStem> STEM = ResourceKey.create(Registries.LEVEL_STEM, ID);
    public static final ResourceKey<DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ID);
    public static final ResourceLocation EFFECTS = ID;
    public static final ResourceKey<Biome> DARK = ResourceKey.create(Registries.BIOME, SupernaturalCraft.asResource("the_empty"));
    public static final int MIN_Y = 0, HEIGHT = 256;
    /** A fixed midnight: it is always dark. */
    public static final long FIXED_TIME = 18000L;

    private EmptyDimension() {
    }

    public static boolean isEmpty(Level level) {
        return level.dimension() == LEVEL;
    }
}
