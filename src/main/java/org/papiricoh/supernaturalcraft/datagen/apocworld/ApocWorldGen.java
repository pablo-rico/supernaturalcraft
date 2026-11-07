package org.papiricoh.supernaturalcraft.datagen.apocworld;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldDimension;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.apocworld.camp.CampStructure;
import org.papiricoh.supernaturalcraft.apocworld.worldgen.AUBiomeSource;
import org.papiricoh.supernaturalcraft.apocworld.worldgen.AUTerrain;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * The Apocalypse World as datapack entries (v0.41): its dimension type (a late afternoon through smoke forever, no rain, water
 * boils off, beds explode), its five noises and its land ({@link AUTerrain} over them), its burned trees, its ruins and its drifts
 * of ash, its five biomes, the dimension itself, and Michael's outposts ({@link CampStructure}). Called from {@code ApocWorldData}.
 */
final class ApocWorldGen {

    static final ResourceKey<ConfiguredFeature<?, ?>> BURNED_TREE_CF = configured("au_burned_tree"), RUIN_CF = configured("au_ruin"),
            ASH_DRIFT_CF = configured("au_ash_drift"), EMBERS_CF = configured("au_embers");
    static final ResourceKey<PlacedFeature> FOREST_TREES = placed("au_forest_trees"), SCATTERED_TREES = placed("au_scattered_trees"),
            RUINS = placed("au_ruins"), RARE_RUINS = placed("au_rare_ruins"), ASH_DRIFTS = placed("au_ash_drifts"), EMBERS = placed("au_embers");

    private ApocWorldGen() {
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, SupernaturalCraft.asResource(name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, SupernaturalCraft.asResource(name));
    }

    // --- dimension type --------------------------------------------------------------------------

    static void dimensionTypes(BootstrapContext<DimensionType> ctx) {
        ctx.register(ApocWorldDimension.TYPE, new DimensionType(
                OptionalLong.of(ApocWorldDimension.FIXED_TIME), // a late afternoon through the smoke, forever
                true,                       // sky light
                false,                      // no roof
                true,                       // ultra-warm: water boils off, as everything here has
                true,                       // natural
                1.0,
                false,                      // beds do not work here
                false,
                ApocWorldDimension.MIN_Y, ApocWorldDimension.HEIGHT, ApocWorldDimension.HEIGHT,
                BlockTags.INFINIBURN_OVERWORLD,
                ApocWorldDimension.EFFECTS,
                0.02f,
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(15), 0)));
    }

    // --- noises ----------------------------------------------------------------------------------

    static void noises(BootstrapContext<NormalNoise.NoiseParameters> ctx) {
        ctx.register(ApocWorldDimension.HILLS, new NormalNoise.NoiseParameters(-8, 1.0, 1.0, 0.5, 0.25));
        ctx.register(ApocWorldDimension.RIDGES, new NormalNoise.NoiseParameters(-9, 1.0, 0.6, 0.3));
        ctx.register(ApocWorldDimension.PITS, new NormalNoise.NoiseParameters(-6, 1.0, 0.5));
        ctx.register(ApocWorldDimension.RIVERS, new NormalNoise.NoiseParameters(-9, 1.0, 0.0, 0.3));
        ctx.register(ApocWorldDimension.DETAIL, new NormalNoise.NoiseParameters(-4, 1.0, 0.5));
    }

    // --- terrain ---------------------------------------------------------------------------------

    static void noiseSettings(BootstrapContext<NoiseGeneratorSettings> ctx) {
        HolderGetter<DensityFunction> functions = ctx.lookup(Registries.DENSITY_FUNCTION);
        HolderGetter<NormalNoise.NoiseParameters> noises = ctx.lookup(Registries.NOISE);
        ctx.register(ApocWorldDimension.NOISE, new NoiseGeneratorSettings(
                NoiseSettings.create(ApocWorldDimension.MIN_Y, ApocWorldDimension.HEIGHT, 1, 2),
                ApocWorldRegistry.SCORCHED_STONE.get().defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                router(functions, noises),
                surface(),
                List.of(),
                ApocWorldDimension.SEA_LEVEL,
                false, false, false, false));
    }

    private static DensityFunction flat(HolderGetter<NormalNoise.NoiseParameters> noises, ResourceKey<NormalNoise.NoiseParameters> key) {
        return DensityFunctions.cache2d(DensityFunctions.noise(noises.getOrThrow(key), 1.0, 0.0));
    }

    /**
     * The land's 2D noises also fill the climate's slots, so the biome source reads the same land: {@code erosion} the ridges,
     * {@code ridges} (weirdness) the craters, {@code temperature} the riverbeds; {@code vegetation} is its own (the old forests).
     */
    private static NoiseRouter router(HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction shiftX = new DensityFunctions.HolderHolder(functions.getOrThrow(key("shift_x")));
        DensityFunction shiftZ = new DensityFunctions.HolderHolder(functions.getOrThrow(key("shift_z")));
        DensityFunction vegetation = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noises.getOrThrow(Noises.VEGETATION));
        DensityFunction hills = flat(noises, ApocWorldDimension.HILLS);
        DensityFunction ridges = flat(noises, ApocWorldDimension.RIDGES);
        DensityFunction pits = flat(noises, ApocWorldDimension.PITS);
        DensityFunction rivers = flat(noises, ApocWorldDimension.RIVERS);
        DensityFunction detail = DensityFunctions.noise(noises.getOrThrow(ApocWorldDimension.DETAIL), 1.0, 1.0);
        DensityFunction land = new AUTerrain(hills, ridges, pits, rivers, detail);
        DensityFunction zero = DensityFunctions.zero();
        return new NoiseRouter(zero, zero, zero, zero, rivers, vegetation, hills, ridges, zero, pits, zero, land, zero, zero, zero);
    }

    private static ResourceKey<DensityFunction> key(String path) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace(path));
    }

    private static SurfaceRules.RuleSource block(Block b) {
        return SurfaceRules.state(b.defaultBlockState());
    }

    private static SurfaceRules.RuleSource surface() {
        SurfaceRules.ConditionSource patch = SurfaceRules.noiseCondition(Noises.SURFACE, 0.12);
        SurfaceRules.ConditionSource rare = SurfaceRules.noiseCondition(Noises.SURFACE, 0.45);
        SurfaceRules.ConditionSource gravel = SurfaceRules.noiseCondition(Noises.GRAVEL, 0.25);
        SurfaceRules.ConditionSource steep = SurfaceRules.steep();
        Block stone = ApocWorldRegistry.SCORCHED_STONE.get(), soil = ApocWorldRegistry.SCORCHED_SOIL.get(), ash = ApocWorldRegistry.ASH_BLOCK.get(),
                embers = ApocWorldRegistry.SMOULDERING_ASH.get();
        SurfaceRules.RuleSource plain = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(steep, block(stone)),
                        SurfaceRules.ifTrue(rare, block(Blocks.COARSE_DIRT)),
                        SurfaceRules.ifTrue(patch, block(ash)),
                        block(soil))),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(Blocks.COARSE_DIRT)));
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)),
                        block(Blocks.BEDROCK)),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ApocWorldDimension.ASH_PLAINS), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(steep, block(stone)),
                                SurfaceRules.ifTrue(patch, block(soil)),
                                block(ash))),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(ash)))),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ApocWorldDimension.CRATERED_FIELDS), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(steep, block(stone)),
                                SurfaceRules.ifTrue(rare, block(embers)),
                                SurfaceRules.ifTrue(gravel, block(Blocks.GRAVEL)),
                                SurfaceRules.ifTrue(patch, block(Blocks.COARSE_DIRT)),
                                block(soil))),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(stone)))),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ApocWorldDimension.BROKEN_RIDGES), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(gravel, block(Blocks.GRAVEL)),
                                SurfaceRules.ifTrue(patch, block(Blocks.BLACKSTONE)),
                                block(stone))))),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ApocWorldDimension.DRY_RIVERBED), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(steep, block(Blocks.COARSE_DIRT)),
                                SurfaceRules.ifTrue(gravel, block(Blocks.GRAVEL)),
                                block(Blocks.PACKED_MUD))),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(Blocks.PACKED_MUD)))),
                plain);
    }

    // --- features --------------------------------------------------------------------------------

    private static <F extends FeatureConfiguration> void cf(BootstrapContext<ConfiguredFeature<?, ?>> ctx,
                                                            ResourceKey<ConfiguredFeature<?, ?>> key, Feature<F> feature, F config) {
        ctx.register(key, new ConfiguredFeature<>(feature, config));
    }

    static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        cf(ctx, BURNED_TREE_CF, ApocWorldRegistry.BURNED_TREE.get(), NoneFeatureConfiguration.INSTANCE);
        cf(ctx, RUIN_CF, ApocWorldRegistry.RUIN.get(), NoneFeatureConfiguration.INSTANCE);
        cf(ctx, ASH_DRIFT_CF, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(ApocWorldRegistry.ASH_LAYER.get())), List.of(), 48));
        cf(ctx, EMBERS_CF, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.FIRE)), List.of(ApocWorldRegistry.SMOULDERING_ASH.get()), 24));
    }

    private static List<PlacementModifier> surface(PlacementModifier... head) {
        List<PlacementModifier> out = new ArrayList<>(List.of(head));
        out.add(InSquarePlacement.spread());
        out.add(PlacementUtils.HEIGHTMAP_WORLD_SURFACE);
        out.add(BiomeFilter.biome());
        return out;
    }

    static void placedFeatures(BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        ctx.register(FOREST_TREES, new PlacedFeature(cf.getOrThrow(BURNED_TREE_CF), surface(CountPlacement.of(7))));
        ctx.register(SCATTERED_TREES, new PlacedFeature(cf.getOrThrow(BURNED_TREE_CF), surface(RarityFilter.onAverageOnceEvery(3))));
        ctx.register(RUINS, new PlacedFeature(cf.getOrThrow(RUIN_CF), surface(RarityFilter.onAverageOnceEvery(5))));
        ctx.register(RARE_RUINS, new PlacedFeature(cf.getOrThrow(RUIN_CF), surface(RarityFilter.onAverageOnceEvery(14))));
        ctx.register(ASH_DRIFTS, new PlacedFeature(cf.getOrThrow(ASH_DRIFT_CF), surface(CountPlacement.of(3))));
        ctx.register(EMBERS, new PlacedFeature(cf.getOrThrow(EMBERS_CF), surface(RarityFilter.onAverageOnceEvery(2))));
    }

    // --- biomes ----------------------------------------------------------------------------------

    static void biomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE);
        var carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        // Every biome lists its features in the same order (a cycle between biomes would stop the world from loading).
        ctx.register(ApocWorldDimension.ASH_PLAINS, biome(effects(0x6a5444, 0x5a4a3a, ParticleTypes.WHITE_ASH, 0.03f), spawns(8),
                gen(placed, carvers, RUINS, SCATTERED_TREES, ASH_DRIFTS, null, true)));
        ctx.register(ApocWorldDimension.BURNED_FOREST, biome(effects(0x5a463a, 0x3c3028, ParticleTypes.ASH, 0.04f), spawns(8),
                gen(placed, carvers, RARE_RUINS, FOREST_TREES, ASH_DRIFTS, null, true)));
        ctx.register(ApocWorldDimension.CRATERED_FIELDS, biome(effects(0x6e4a36, 0x4a3a2c, ParticleTypes.ASH, 0.05f), spawns(10),
                gen(placed, carvers, RARE_RUINS, SCATTERED_TREES, null, EMBERS, true)));
        ctx.register(ApocWorldDimension.BROKEN_RIDGES, biome(effects(0x5c4c42, 0x4a4038, ParticleTypes.WHITE_ASH, 0.02f), spawns(4),
                gen(placed, carvers, null, null, null, null, false)));
        ctx.register(ApocWorldDimension.DRY_RIVERBED, biome(effects(0x705a48, 0x5e4c3a, ParticleTypes.WHITE_ASH, 0.02f), spawns(6),
                gen(placed, carvers, RARE_RUINS, null, ASH_DRIFTS, null, true)));
    }

    private static BiomeGenerationSettings gen(HolderGetter<PlacedFeature> placed,
                                               HolderGetter<net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?>> carvers,
                                               ResourceKey<PlacedFeature> ruins, ResourceKey<PlacedFeature> trees, ResourceKey<PlacedFeature> ash,
                                               ResourceKey<PlacedFeature> embers, boolean bushes) {
        BiomeGenerationSettings.Builder b = new BiomeGenerationSettings.Builder(placed, carvers);
        if (ruins != null) b.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, placed.getOrThrow(ruins));
        if (trees != null) b.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed.getOrThrow(trees));
        if (ash != null) b.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed.getOrThrow(ash));
        if (embers != null) b.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed.getOrThrow(embers));
        if (bushes) b.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed.getOrThrow(VegetationPlacements.PATCH_DEAD_BUSH));
        return b.build();
    }

    /** Only Michael's soldiers walk here (in small squads). */
    private static MobSpawnSettings spawns(int soldiers) {
        return new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ApocWorldRegistry.AU_ANGEL_SOLDIER.get(), soldiers, 1, 3))
                .build();
    }

    private static BiomeSpecialEffects effects(int fog, int grass, net.minecraft.core.particles.ParticleOptions particle, float density) {
        return new BiomeSpecialEffects.Builder()
                .fogColor(fog).skyColor(0x4a3a30).waterColor(0x4a3c30).waterFogColor(0x2a2018)
                .grassColorOverride(grass).foliageColorOverride(grass)
                .ambientParticle(new AmbientParticleSettings(particle, density))
                .ambientLoopSound(SoundEvents.AMBIENT_BASALT_DELTAS_LOOP)
                .ambientMoodSound(new AmbientMoodSettings(SoundEvents.AMBIENT_BASALT_DELTAS_MOOD, 6000, 8, 2.0))
                .ambientAdditionsSound(new AmbientAdditionsSettings(SoundEvents.AMBIENT_BASALT_DELTAS_ADDITIONS, 0.0111))
                .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_BASALT_DELTAS))
                .build();
    }

    private static Biome biome(BiomeSpecialEffects effects, MobSpawnSettings spawns, BiomeGenerationSettings gen) {
        return new Biome.BiomeBuilder().hasPrecipitation(false).temperature(2.0f).downfall(0f)
                .specialEffects(effects).mobSpawnSettings(spawns).generationSettings(gen).build();
    }

    // --- the dimension -----------------------------------------------------------------------------

    static void levelStems(BootstrapContext<LevelStem> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        HolderGetter<DimensionType> types = ctx.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noise = ctx.lookup(Registries.NOISE_SETTINGS);
        ctx.register(ApocWorldDimension.STEM, new LevelStem(types.getOrThrow(ApocWorldDimension.TYPE), new NoiseBasedChunkGenerator(
                new AUBiomeSource(biomes.getOrThrow(ApocWorldDimension.ASH_PLAINS), biomes.getOrThrow(ApocWorldDimension.BURNED_FOREST),
                        biomes.getOrThrow(ApocWorldDimension.CRATERED_FIELDS), biomes.getOrThrow(ApocWorldDimension.BROKEN_RIDGES),
                        biomes.getOrThrow(ApocWorldDimension.DRY_RIVERBED)),
                noise.getOrThrow(ApocWorldDimension.NOISE))));
    }

    // --- Michael's outposts ------------------------------------------------------------------------

    static void structures(BootstrapContext<Structure> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        ctx.register(CampStructure.KEY, new CampStructure(new Structure.StructureSettings.Builder(HolderSet.direct(
                biomes.getOrThrow(ApocWorldDimension.ASH_PLAINS), biomes.getOrThrow(ApocWorldDimension.BURNED_FOREST),
                biomes.getOrThrow(ApocWorldDimension.CRATERED_FIELDS), biomes.getOrThrow(ApocWorldDimension.DRY_RIVERBED)))
                .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
                .terrainAdapation(TerrainAdjustment.BEARD_THIN).build()));
    }

    static void structureSets(BootstrapContext<StructureSet> ctx) {
        HolderGetter<Structure> structures = ctx.lookup(Registries.STRUCTURE);
        ctx.register(CampStructure.SET, new StructureSet(structures.getOrThrow(CampStructure.KEY),
                new RandomSpreadStructurePlacement(CampStructure.SPACING, CampStructure.SEPARATION, RandomSpreadType.LINEAR, CampStructure.SALT)));
    }
}
