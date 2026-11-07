package org.papiricoh.supernaturalcraft.purgatory;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Purgatory (v0.39): an endless dark forest under a grey sky that never changes, where the souls of monsters hunt each other
 * forever, and where Eve waits in her glade. Everything about its shape is a datapack entry (datagen {@code SNPurgatory}); these
 * are the keys the code refers to it by.
 */
public final class PurgatoryDimension {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("purgatory");
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, ID);
    public static final ResourceKey<LevelStem> STEM = ResourceKey.create(Registries.LEVEL_STEM, ID);
    public static final ResourceKey<DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ID);
    public static final ResourceKey<NoiseGeneratorSettings> NOISE = ResourceKey.create(Registries.NOISE_SETTINGS, ID);
    /** The client's sky, fog and lightmap for Purgatory. */
    public static final ResourceLocation EFFECTS = ID;

    public static final ResourceKey<Biome> ENDLESS_FOREST = biome("endless_forest");
    public static final ResourceKey<Biome> MIRE = biome("purgatory_mire");
    public static final ResourceKey<Biome> CRAGS = biome("purgatory_crags");
    public static final ResourceKey<Biome> MOTHERS_WOOD = biome("mothers_wood");

    /** The noises its land is drawn from (datapack entries too). */
    public static final ResourceKey<NormalNoise.NoiseParameters> HILLS = noise("purgatory_hills"), CLIFFS = noise("purgatory_cliffs"),
            RAVINES = noise("purgatory_ravines"), DETAIL = noise("purgatory_detail");

    /** World floor and height; the water that fills the lowest hollows and the bottoms of the ravines. */
    public static final int MIN_Y = 0, HEIGHT = 256, SEA_LEVEL = 54;
    /** The hour it is forever: just after sunset, a grey dusk. */
    public static final long FIXED_TIME = 12_700L;

    private PurgatoryDimension() {
    }

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, SupernaturalCraft.asResource(name));
    }

    private static ResourceKey<NormalNoise.NoiseParameters> noise(String name) {
        return ResourceKey.create(Registries.NOISE, SupernaturalCraft.asResource(name));
    }

    public static boolean isPurgatory(Level level) {
        return level.dimension().equals(LEVEL);
    }
}
