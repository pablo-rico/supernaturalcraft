package org.papiricoh.supernaturalcraft.heaven;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Heaven (v0.18): a bright, empty sky where every hunter gets a floating island of their own (a plot, far apart from the next),
 * built from their memories. Everything about its shape is a datapack entry (datagen {@code SNHeaven}: a void flat world, one
 * biome, a fixed noon); these are the keys the code refers to it by.
 */
public final class HeavenDimension {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("heaven");
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, ID);
    public static final ResourceKey<LevelStem> STEM = ResourceKey.create(Registries.LEVEL_STEM, ID);
    public static final ResourceKey<DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ID);
    /** The client's sky, fog and light for Heaven. */
    public static final ResourceLocation EFFECTS = ID;
    /** Its only biome: a garden of light. */
    public static final ResourceKey<Biome> GARDEN = ResourceKey.create(Registries.BIOME, SupernaturalCraft.asResource("heaven_garden"));

    /** World floor and height (a void: nothing generates). */
    public static final int MIN_Y = 0, HEIGHT = 256;
    /** The height of every plot's main floor (the island's grass, the plaza where gates land). */
    public static final int PLOT_Y = 100;
    /** Below this a fall is caught and the faller carried back to a landing. */
    public static final int RESCUE_Y = PLOT_Y - 48;
    /** Blocks between the centres of two neighbouring plots (plot 0 is Ash's Roadhouse at the origin). */
    public static final int PLOT_SPACING = 1024;

    private HeavenDimension() {
    }

    public static boolean isHeaven(Level level) {
        return level.dimension().equals(LEVEL);
    }
}
