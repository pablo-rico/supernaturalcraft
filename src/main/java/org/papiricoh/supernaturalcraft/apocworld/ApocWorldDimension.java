package org.papiricoh.supernaturalcraft.apocworld;

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
 * The Apocalypse World (v0.41, season 13): the burned mirror of this world after Michael won his war. Scorched rolling land,
 * ash plains, dead forests, cratered fields, broken ridges and dried riverbeds under a smoke-brown sky that never moves past a
 * late afternoon. Everything about its shape is a datapack entry (datagen {@code ApocWorldGen}); these are the keys the code
 * refers to it by.
 */
public final class ApocWorldDimension {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("apocalypse_world");
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, ID);
    public static final ResourceKey<LevelStem> STEM = ResourceKey.create(Registries.LEVEL_STEM, ID);
    public static final ResourceKey<DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ID);
    public static final ResourceKey<NoiseGeneratorSettings> NOISE = ResourceKey.create(Registries.NOISE_SETTINGS, ID);
    /** The client's sky, fog and lightmap. */
    public static final ResourceLocation EFFECTS = ID;

    public static final ResourceKey<Biome> ASH_PLAINS = biome("au_ash_plains");
    public static final ResourceKey<Biome> BURNED_FOREST = biome("au_burned_forest");
    public static final ResourceKey<Biome> CRATERED_FIELDS = biome("au_cratered_fields");
    public static final ResourceKey<Biome> BROKEN_RIDGES = biome("au_broken_ridges");
    public static final ResourceKey<Biome> DRY_RIVERBED = biome("au_dry_riverbed");

    /** The noises its land is drawn from (datapack entries too). */
    public static final ResourceKey<NormalNoise.NoiseParameters> HILLS = noise("au_hills"), RIDGES = noise("au_ridges"),
            PITS = noise("au_pits"), RIVERS = noise("au_rivers"), DETAIL = noise("au_detail");

    /** World floor and height; there is no water (the sea line lies inside the deep rock). */
    public static final int MIN_Y = 0, HEIGHT = 256, SEA_LEVEL = 1;
    /** The hour it is forever: a late afternoon through the smoke. */
    public static final long FIXED_TIME = 10_400L;

    private ApocWorldDimension() {
    }

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, SupernaturalCraft.asResource(name));
    }

    private static ResourceKey<NormalNoise.NoiseParameters> noise(String name) {
        return ResourceKey.create(Registries.NOISE, SupernaturalCraft.asResource(name));
    }

    /** Whether {@code level} is the real Apocalypse World (the stand-in is only a place in the Overworld). */
    public static boolean isReal(Level level) {
        return level.dimension().equals(LEVEL);
    }
}
