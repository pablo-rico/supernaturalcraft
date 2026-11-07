package org.papiricoh.supernaturalcraft.datagen.purgatory;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
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
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.ThreeLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.DarkOakFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.DarkOakTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryDimension;
import org.papiricoh.supernaturalcraft.purgatory.worldgen.PurgatoryBiomeSource;
import org.papiricoh.supernaturalcraft.purgatory.worldgen.PurgatoryShape;
import org.papiricoh.supernaturalcraft.purgatory.worldgen.PurgatoryTerrain;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Purgatory as datapack entries (v0.39): its dimension type (a grey dusk forever, no sun, no moon, no beds), its four noises and
 * its terrain ({@link PurgatoryTerrain} over them), its trees and its ore, its four biomes and the dimension itself. Called from
 * {@code SNDataGenerators}.
 */
public final class SNPurgatory {

    public static final ResourceKey<ConfiguredFeature<?, ?>> GNARLED_TREE_CF = configured("purgatory_gnarled_tree"),
            TALL_TREE_CF = configured("purgatory_tall_tree"), SOUL_AMBER_ORE_CF = configured("ore_soul_amber");
    public static final ResourceKey<PlacedFeature> FOREST_TREES = placed("purgatory_forest_trees"), WOOD_TREES = placed("mothers_wood_trees"),
            MIRE_TREES = placed("purgatory_mire_trees"), CRAG_TREES = placed("purgatory_crag_trees"),
            SOUL_AMBER_ORE = placed("ore_soul_amber"), SOUL_AMBER_ORE_RICH = placed("ore_soul_amber_rich");

    private SNPurgatory() {
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, SupernaturalCraft.asResource(name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, SupernaturalCraft.asResource(name));
    }

    // --- dimension type --------------------------------------------------------------------------

    public static void bootstrapType(BootstrapContext<DimensionType> ctx) {
        ctx.register(PurgatoryDimension.TYPE, new DimensionType(
                OptionalLong.of(PurgatoryDimension.FIXED_TIME), // a grey dusk that never moves
                true,                       // sky light (dim: it is always just after sunset)
                false,                      // no roof
                false,
                true,                       // natural
                1.0,
                false,                      // beds do not work here
                false,
                PurgatoryDimension.MIN_Y, PurgatoryDimension.HEIGHT, PurgatoryDimension.HEIGHT,
                BlockTags.INFINIBURN_OVERWORLD,
                PurgatoryDimension.EFFECTS,
                0.04f,
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(15), 0)));
    }

    // --- noises ----------------------------------------------------------------------------------

    public static void bootstrapNoises(BootstrapContext<NormalNoise.NoiseParameters> ctx) {
        ctx.register(PurgatoryDimension.HILLS, new NormalNoise.NoiseParameters(-8, 1.0, 1.0, 0.5, 0.25));
        ctx.register(PurgatoryDimension.CLIFFS, new NormalNoise.NoiseParameters(-7, 1.0, 0.4, 0.15));
        ctx.register(PurgatoryDimension.RAVINES, new NormalNoise.NoiseParameters(-8, 1.0, 0.0, 0.25));
        ctx.register(PurgatoryDimension.DETAIL, new NormalNoise.NoiseParameters(-4, 1.0, 0.5));
    }

    // --- terrain ---------------------------------------------------------------------------------

    public static void bootstrapNoise(BootstrapContext<NoiseGeneratorSettings> ctx) {
        HolderGetter<DensityFunction> functions = ctx.lookup(Registries.DENSITY_FUNCTION);
        HolderGetter<NormalNoise.NoiseParameters> noises = ctx.lookup(Registries.NOISE);
        ctx.register(PurgatoryDimension.NOISE, new NoiseGeneratorSettings(
                NoiseSettings.create(PurgatoryDimension.MIN_Y, PurgatoryDimension.HEIGHT, 1, 2),
                AllBlocks.PURGATORY_STONE.get().defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                router(functions, noises),
                surface(),
                List.of(),
                PurgatoryDimension.SEA_LEVEL,
                false, false, false, false));
    }

    private static DensityFunction flat(HolderGetter<NormalNoise.NoiseParameters> noises, ResourceKey<NormalNoise.NoiseParameters> key, double xz) {
        return DensityFunctions.cache2d(DensityFunctions.noise(noises.getOrThrow(key), xz, 0.0));
    }

    /**
     * The land's three 2D noises go into the climate's {@code continents}, {@code erosion} and {@code ridges} slots too, so the
     * biome source picks the Mire, the Crags and the Mother's Wood from the same land it sees.
     */
    private static NoiseRouter router(HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction shiftX = new DensityFunctions.HolderHolder(functions.getOrThrow(key("shift_x")));
        DensityFunction shiftZ = new DensityFunctions.HolderHolder(functions.getOrThrow(key("shift_z")));
        DensityFunction temperature = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noises.getOrThrow(Noises.TEMPERATURE));
        DensityFunction vegetation = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noises.getOrThrow(Noises.VEGETATION));
        DensityFunction hills = flat(noises, PurgatoryDimension.HILLS, 1.0);
        DensityFunction cliffs = flat(noises, PurgatoryDimension.CLIFFS, 1.0);
        DensityFunction ravines = flat(noises, PurgatoryDimension.RAVINES, 1.0);
        DensityFunction detail = DensityFunctions.noise(noises.getOrThrow(PurgatoryDimension.DETAIL), 1.0, 1.0);
        DensityFunction land = new PurgatoryTerrain(hills, cliffs, ravines, detail);
        DensityFunction zero = DensityFunctions.zero();
        return new NoiseRouter(zero, zero, zero, zero, temperature, vegetation, hills, cliffs, zero, ravines, zero,
                land, zero, zero, zero);
    }

    private static ResourceKey<DensityFunction> key(String path) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, net.minecraft.resources.ResourceLocation.withDefaultNamespace(path));
    }

    private static SurfaceRules.RuleSource block(Block b) {
        return SurfaceRules.state(b.defaultBlockState());
    }

    private static SurfaceRules.RuleSource surface() {
        SurfaceRules.ConditionSource patch = SurfaceRules.noiseCondition(Noises.SURFACE, 0.12);
        SurfaceRules.ConditionSource gravel = SurfaceRules.noiseCondition(Noises.GRAVEL, 0.25);
        SurfaceRules.ConditionSource dry = SurfaceRules.waterBlockCheck(-1, 0);
        SurfaceRules.ConditionSource steep = SurfaceRules.steep();
        SurfaceRules.RuleSource soil = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(steep, block(AllBlocks.PURGATORY_STONE.get())),
                        SurfaceRules.ifTrue(SurfaceRules.not(dry), block(Blocks.MUD)),
                        SurfaceRules.ifTrue(patch, block(Blocks.PODZOL)),
                        block(Blocks.GRASS_BLOCK))),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(Blocks.DIRT)));
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)),
                        block(Blocks.BEDROCK)),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(PurgatoryDimension.MIRE), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(patch, block(Blocks.GRASS_BLOCK)),
                                block(Blocks.MUD))),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(Blocks.MUD)))),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(PurgatoryDimension.CRAGS), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(gravel, block(Blocks.GRAVEL)),
                                SurfaceRules.ifTrue(patch, block(Blocks.COARSE_DIRT)),
                                block(AllBlocks.PURGATORY_STONE.get()))))),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(PurgatoryDimension.MOTHERS_WOOD), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(steep, block(AllBlocks.PURGATORY_STONE.get())),
                                SurfaceRules.ifTrue(patch, block(Blocks.MOSS_BLOCK)),
                                block(Blocks.PODZOL))),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(Blocks.ROOTED_DIRT)))),
                soil);
    }

    // --- features --------------------------------------------------------------------------------

    public static void bootstrapConfigured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        BlockStateProvider log = BlockStateProvider.simple(AllBlocks.PURGATORY_LOG.get());
        BlockStateProvider leaves = BlockStateProvider.simple(AllBlocks.PURGATORY_LEAVES.get());
        // Gnarled, low and broad (a dark oak's bones), and tall and narrow (a starved pine's).
        ctx.register(GNARLED_TREE_CF, new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                log, new DarkOakTrunkPlacer(6, 3, 2), leaves, new DarkOakFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0)),
                new ThreeLayersFeatureSize(1, 1, 0, 1, 2, OptionalInt.empty())).ignoreVines().build()));
        ctx.register(TALL_TREE_CF, new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                log, new StraightTrunkPlacer(11, 5, 2), leaves,
                new SpruceFoliagePlacer(UniformInt.of(1, 2), UniformInt.of(0, 1), UniformInt.of(2, 4)),
                new TwoLayersFeatureSize(2, 0, 2)).ignoreVines().build()));
        ctx.register(SOUL_AMBER_ORE_CF, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(
                new BlockMatchTest(AllBlocks.PURGATORY_STONE.get()), AllBlocks.SOUL_AMBER_ORE.get().defaultBlockState(), 6)));
    }

    public static void bootstrapPlaced(BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        var gnarled = cf.getOrThrow(GNARLED_TREE_CF);
        var tall = cf.getOrThrow(TALL_TREE_CF);
        ctx.register(FOREST_TREES, new PlacedFeature(tall, VegetationPlacements.treePlacement(PlacementUtils.countExtra(9, 0.2f, 3), Blocks.OAK_SAPLING)));
        ctx.register(WOOD_TREES, new PlacedFeature(gnarled, VegetationPlacements.treePlacement(PlacementUtils.countExtra(8, 0.2f, 2), Blocks.OAK_SAPLING)));
        ctx.register(MIRE_TREES, new PlacedFeature(gnarled, VegetationPlacements.treePlacement(PlacementUtils.countExtra(1, 0.25f, 1), Blocks.OAK_SAPLING)));
        ctx.register(CRAG_TREES, new PlacedFeature(tall, VegetationPlacements.treePlacement(PlacementUtils.countExtra(1, 0.5f, 2), Blocks.OAK_SAPLING)));
        ctx.register(SOUL_AMBER_ORE, new PlacedFeature(cf.getOrThrow(SOUL_AMBER_ORE_CF), List.of(CountPlacement.of(6), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(8), VerticalAnchor.absolute(96)), BiomeFilter.biome())));
        ctx.register(SOUL_AMBER_ORE_RICH, new PlacedFeature(cf.getOrThrow(SOUL_AMBER_ORE_CF), List.of(CountPlacement.of(14), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(8), VerticalAnchor.absolute(140)), BiomeFilter.biome())));
    }

    // --- biomes ----------------------------------------------------------------------------------

    public static void bootstrapBiomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE);
        var carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        // Every biome lists its features in the same order (a cycle between biomes would stop the world from loading).
        ctx.register(PurgatoryDimension.ENDLESS_FOREST, biome(
                effects(0x6c7884, 0x4f5c52, new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.006f)),
                spawns(14, 10),
                gen(placed, carvers, SOUL_AMBER_ORE, FOREST_TREES, true, false)));
        ctx.register(PurgatoryDimension.MIRE, biome(
                effects(0x58646e, 0x46524a, new AmbientParticleSettings(ParticleTypes.MYCELIUM, 0.03f)),
                spawns(10, 6),
                gen(placed, carvers, SOUL_AMBER_ORE, MIRE_TREES, false, true)));
        ctx.register(PurgatoryDimension.CRAGS, biome(
                effects(0x7a8692, 0x58645a, new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.012f)),
                spawns(8, 14),
                gen(placed, carvers, SOUL_AMBER_ORE_RICH, CRAG_TREES, false, false)));
        ctx.register(PurgatoryDimension.MOTHERS_WOOD, biome(
                effects(0x5e665a, 0x4a5a40, new AmbientParticleSettings(ParticleTypes.SPORE_BLOSSOM_AIR, 0.004f)),
                spawns(12, 12),
                gen(placed, carvers, SOUL_AMBER_ORE, WOOD_TREES, true, false)));
    }

    private static BiomeGenerationSettings gen(HolderGetter<PlacedFeature> placed,
                                               HolderGetter<net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver<?>> carvers,
                                               ResourceKey<PlacedFeature> ore, ResourceKey<PlacedFeature> trees, boolean ferns, boolean wet) {
        BiomeGenerationSettings.Builder b = new BiomeGenerationSettings.Builder(placed, carvers);
        b.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, placed.getOrThrow(ore));
        b.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed.getOrThrow(trees));
        if (ferns) b.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed.getOrThrow(VegetationPlacements.PATCH_LARGE_FERN));
        b.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed.getOrThrow(VegetationPlacements.PATCH_GRASS_TAIGA));
        b.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed.getOrThrow(VegetationPlacements.PATCH_DEAD_BUSH));
        if (wet) b.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed.getOrThrow(VegetationPlacements.PATCH_WATERLILY));
        b.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed.getOrThrow(VegetationPlacements.BROWN_MUSHROOM_TAIGA));
        return b.build();
    }

    /** Only Purgatory's own: the souls of vampires and werewolves, in packs. */
    private static MobSpawnSettings spawns(int vampires, int werewolves) {
        return new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(AllEntities.PURGATORY_VAMPIRE.get(), vampires, 2, 4))
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(AllEntities.PURGATORY_WEREWOLF.get(), werewolves, 2, 3))
                .build();
    }

    private static BiomeSpecialEffects effects(int fog, int grass, AmbientParticleSettings particles) {
        return new BiomeSpecialEffects.Builder()
                .fogColor(fog).skyColor(0x8a949e).waterColor(0x3a4650).waterFogColor(0x1e262c)
                .grassColorOverride(grass).foliageColorOverride(grass)
                .ambientParticle(particles)
                .ambientLoopSound(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP)
                .ambientMoodSound(new AmbientMoodSettings(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD, 6000, 8, 2.0))
                .ambientAdditionsSound(new AmbientAdditionsSettings(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 0.0111))
                .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_DEEP_DARK))
                .build();
    }

    private static Biome biome(BiomeSpecialEffects effects, MobSpawnSettings spawns, BiomeGenerationSettings gen) {
        return new Biome.BiomeBuilder().hasPrecipitation(false).temperature(0.4f).downfall(0.4f)
                .specialEffects(effects).mobSpawnSettings(spawns).generationSettings(gen).build();
    }

    // --- the dimension -----------------------------------------------------------------------------

    public static void bootstrapStem(BootstrapContext<LevelStem> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        HolderGetter<DimensionType> types = ctx.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noise = ctx.lookup(Registries.NOISE_SETTINGS);
        ctx.register(PurgatoryDimension.STEM, new LevelStem(types.getOrThrow(PurgatoryDimension.TYPE), new NoiseBasedChunkGenerator(
                new PurgatoryBiomeSource(biomes.getOrThrow(PurgatoryDimension.ENDLESS_FOREST), biomes.getOrThrow(PurgatoryDimension.MIRE),
                        biomes.getOrThrow(PurgatoryDimension.CRAGS), biomes.getOrThrow(PurgatoryDimension.MOTHERS_WOOD)),
                noise.getOrThrow(PurgatoryDimension.NOISE))));
    }

    /** Unused but kept for the reader: the land never rises past this, so the heightmap search for a landing starts here. */
    static final int TOP = PurgatoryShape.SKY_Y;
}
