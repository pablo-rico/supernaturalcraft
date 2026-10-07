package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.NetherPlacements;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.hell.worldgen.HellBiomeSource;
import org.papiricoh.supernaturalcraft.hell.worldgen.HellPit;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.List;
import java.util.OptionalLong;

/**
 * Hell as datapack entries: its dimension type, its terrain (a Nether-like cavern twice as tall, with
 * the Pit sunk through the middle), its four biomes and the dimension itself.
 */
public final class SNHell {

    private SNHell() {
    }

    // --- dimension type --------------------------------------------------------------------------

    public static void bootstrapType(BootstrapContext<DimensionType> ctx) {
        ctx.register(HellDimension.TYPE, new DimensionType(
                OptionalLong.of(18000L),   // fixed time: no day, no night
                false,                      // no sky light
                true,                       // a roof
                true,                       // ultrawarm: water boils away, lava runs far
                false,                      // not natural: compasses spin, beds explode
                8.0,                        // one block here is eight in the Overworld
                false,                      // beds explode
                false,                      // respawn anchors do not work
                HellDimension.MIN_Y, HellDimension.HEIGHT, HellDimension.HEIGHT,
                BlockTags.INFINIBURN_NETHER,
                HellDimension.EFFECTS,
                0.2f,
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(11), 15)));
    }

    // --- terrain ---------------------------------------------------------------------------------

    public static void bootstrapNoise(BootstrapContext<NoiseGeneratorSettings> ctx) {
        HolderGetter<DensityFunction> functions = ctx.lookup(Registries.DENSITY_FUNCTION);
        HolderGetter<NormalNoise.NoiseParameters> noises = ctx.lookup(Registries.NOISE);
        ctx.register(HellDimension.NOISE, new NoiseGeneratorSettings(
                NoiseSettings.create(HellDimension.MIN_Y, HellDimension.HEIGHT, 1, 2),
                AllBlocks.HELLSTONE.get().defaultBlockState(),
                Blocks.LAVA.defaultBlockState(),
                router(functions, noises),
                surface(),
                List.of(),
                HellDimension.LAVA_LEVEL,
                false, false, false, true));
    }

    /**
     * The Nether's router, stretched to 256 blocks: the same blended cavern noise (a little taller per
     * cell, for higher halls), slid to solid rock at floor and roof, then the Pit taken out with {@code min}.
     */
    private static NoiseRouter router(HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction shiftX = new DensityFunctions.HolderHolder(functions.getOrThrow(key("shift_x")));
        DensityFunction shiftZ = new DensityFunctions.HolderHolder(functions.getOrThrow(key("shift_z")));
        DensityFunction temperature = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noises.getOrThrow(Noises.TEMPERATURE));
        DensityFunction vegetation = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noises.getOrThrow(Noises.VEGETATION));
        DensityFunction caverns = BlendedNoise.createUnseeded(0.25, 0.3, 80.0, 72.0, 8.0);
        DensityFunction slid = slide(caverns, HellDimension.MIN_Y, HellDimension.HEIGHT, 24, 0, 0.9375, -8, 24, 2.5);
        DensityFunction terrain = DensityFunctions.mul(DensityFunctions.interpolated(DensityFunctions.blendDensity(slid)),
                DensityFunctions.constant(0.64)).squeeze();
        DensityFunction finalDensity = DensityFunctions.min(terrain, HellPit.DEFAULT);
        DensityFunction zero = DensityFunctions.zero();
        return new NoiseRouter(zero, zero, zero, zero, temperature, vegetation, zero, zero, zero, zero, zero,
                finalDensity, zero, zero, zero);
    }

    private static ResourceKey<DensityFunction> key(String path) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, net.minecraft.resources.ResourceLocation.withDefaultNamespace(path));
    }

    /** NoiseRouterData.slide (private there): fade to {@code topDelta} at the roof and {@code bottomDelta} at the floor. */
    private static DensityFunction slide(DensityFunction f, int minY, int height, int topStart, int topEnd, double topDelta,
                                         int bottomStart, int bottomEnd, double bottomDelta) {
        DensityFunction top = DensityFunctions.yClampedGradient(minY + height - topStart, minY + height - topEnd, 1.0, 0.0);
        DensityFunction withTop = DensityFunctions.lerp(top, topDelta, f);
        DensityFunction bottom = DensityFunctions.yClampedGradient(minY + bottomStart, minY + bottomEnd, 0.0, 1.0);
        return DensityFunctions.lerp(bottom, bottomDelta, withTop);
    }

    private static SurfaceRules.RuleSource block(Block b) {
        return SurfaceRules.state(b.defaultBlockState());
    }

    private static SurfaceRules.RuleSource surface() {
        SurfaceRules.RuleSource bedrock = block(Blocks.BEDROCK);
        SurfaceRules.ConditionSource patch = SurfaceRules.noiseCondition(Noises.SOUL_SAND_LAYER, -0.012);
        SurfaceRules.ConditionSource selector = SurfaceRules.noiseCondition(Noises.NETHER_STATE_SELECTOR, 0.0);
        SurfaceRules.ConditionSource aboveLava = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(HellDimension.LAVA_LEVEL), 0);
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), bedrock),
                SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.verticalGradient("bedrock_roof", VerticalAnchor.belowTop(5), VerticalAnchor.top())), bedrock),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(HellDimension.THE_RACK), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.ifTrue(aboveLava,
                                SurfaceRules.ifTrue(patch, block(AllBlocks.CONGEALED_BLOOD.get())))),
                        block(AllBlocks.RACK_STONE.get()))),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(HellDimension.ASH_WASTES), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.ifTrue(aboveLava, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(selector, block(AllBlocks.ASH_BLOCK.get())), block(Blocks.BLACKSTONE)))),
                        block(AllBlocks.HELLSTONE.get()))),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(HellDimension.CROWLEYS_CORRIDORS), block(AllBlocks.CORRIDOR_STONE.get())),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(HellDimension.THE_PIT), block(AllBlocks.ABYSSAL_STONE.get())),
                block(AllBlocks.HELLSTONE.get()));
    }

    // --- biomes ----------------------------------------------------------------------------------

    public static void bootstrapBiomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = ctx.lookup(Registries.CONFIGURED_CARVER);

        ctx.register(HellDimension.THE_RACK, biome(
                effects(0x3a0508, new AmbientParticleSettings(ParticleTypes.CRIMSON_SPORE, 0.012f),
                        SoundEvents.AMBIENT_CRIMSON_FOREST_LOOP, SoundEvents.AMBIENT_CRIMSON_FOREST_MOOD,
                        SoundEvents.AMBIENT_CRIMSON_FOREST_ADDITIONS, SoundEvents.MUSIC_BIOME_CRIMSON_FOREST),
                spawns(14, 6, 2, 4),
                common(placed, carvers)
                        .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed.getOrThrow(SNWorldgen.HANGING_HOOKS))
                        .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, placed.getOrThrow(SNWorldgen.BRIMSTONE_ORE_RARE))));

        ctx.register(HellDimension.ASH_WASTES, biome(
                effects(0x5a2a10, new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.05f),
                        SoundEvents.AMBIENT_BASALT_DELTAS_LOOP, SoundEvents.AMBIENT_BASALT_DELTAS_MOOD,
                        SoundEvents.AMBIENT_BASALT_DELTAS_ADDITIONS, SoundEvents.MUSIC_BIOME_BASALT_DELTAS),
                spawns(10, 4, 1, 3),
                common(placed, carvers)
                        .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, placed.getOrThrow(SNWorldgen.BRIMSTONE_ORE))
                        .addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, placed.getOrThrow(SNWorldgen.HELLFIRE_VENTS))
                        .addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, placed.getOrThrow(NetherPlacements.PATCH_FIRE))
                        .addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, placed.getOrThrow(NetherPlacements.BASALT_PILLAR))));

        ctx.register(HellDimension.CROWLEYS_CORRIDORS, biome(
                effects(0x2a1c1c, new AmbientParticleSettings(ParticleTypes.ASH, 0.004f),
                        SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD,
                        SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, SoundEvents.MUSIC_BIOME_SOUL_SAND_VALLEY),
                spawns(10, 14, 1, 1),
                common(placed, carvers)
                        .addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, placed.getOrThrow(NetherPlacements.PATCH_SOUL_FIRE))));

        ctx.register(HellDimension.THE_PIT, biome(
                effects(0x2a0a10, new AmbientParticleSettings(ParticleTypes.SMOKE, 0.006f),
                        SoundEvents.AMBIENT_WARPED_FOREST_LOOP, SoundEvents.AMBIENT_WARPED_FOREST_MOOD,
                        SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, SoundEvents.MUSIC_BIOME_WARPED_FOREST),
                new MobSpawnSettings.Builder().build(),
                new BiomeGenerationSettings.Builder(placed, carvers)
                        .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, placed.getOrThrow(SNWorldgen.ABYSSAL_SHARD_ORE))
                        .addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, placed.getOrThrow(NetherPlacements.GLOWSTONE))));
    }

    private static BiomeGenerationSettings.PlainBuilder common(HolderGetter<PlacedFeature> placed, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        return new BiomeGenerationSettings.Builder(placed, carvers)
                .addCarver(GenerationStep.Carving.AIR, carvers.getOrThrow(Carvers.NETHER_CAVE))
                .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placed.getOrThrow(MiscOverworldPlacements.SPRING_LAVA))
                .addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, placed.getOrThrow(NetherPlacements.SPRING_OPEN))
                .addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, placed.getOrThrow(NetherPlacements.GLOWSTONE_EXTRA))
                .addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, placed.getOrThrow(NetherPlacements.GLOWSTONE))
                .addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, placed.getOrThrow(OrePlacements.ORE_MAGMA))
                .addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, placed.getOrThrow(NetherPlacements.SPRING_CLOSED))
                .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, placed.getOrThrow(OrePlacements.ORE_QUARTZ_NETHER));
    }

    /** Demons and hellhounds, weighted per biome. */
    private static MobSpawnSettings spawns(int demons, int occultists, int houndMin, int houndMax) {
        return new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(AllEntities.BLACK_EYED_DEMON.get(), demons, 1, 3))
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(AllEntities.DEMON_OCCULTIST.get(), occultists, 1, 1))
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(AllEntities.HELLHOUND.get(), 8, houndMin, houndMax))
                .build();
    }

    private static BiomeSpecialEffects effects(int fog, AmbientParticleSettings particles, Holder<net.minecraft.sounds.SoundEvent> loop,
                                               Holder<net.minecraft.sounds.SoundEvent> mood, Holder<net.minecraft.sounds.SoundEvent> additions,
                                               Holder<net.minecraft.sounds.SoundEvent> music) {
        return new BiomeSpecialEffects.Builder()
                .fogColor(fog).skyColor(0x1a0000).waterColor(0x6a0a0a).waterFogColor(0x2a0000)
                .ambientParticle(particles)
                .ambientLoopSound(loop)
                .ambientMoodSound(new AmbientMoodSettings(mood, 6000, 8, 2.0))
                .ambientAdditionsSound(new AmbientAdditionsSettings(additions, 0.0111))
                .backgroundMusic(Musics.createGameMusic(music))
                .build();
    }

    private static Biome biome(BiomeSpecialEffects effects, MobSpawnSettings spawns, BiomeGenerationSettings.PlainBuilder gen) {
        return new Biome.BiomeBuilder().hasPrecipitation(false).temperature(2.0f).downfall(0.0f)
                .specialEffects(effects).mobSpawnSettings(spawns).generationSettings(gen.build()).build();
    }

    // --- the dimension -----------------------------------------------------------------------------

    public static void bootstrapStem(BootstrapContext<LevelStem> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        HolderGetter<DimensionType> types = ctx.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noise = ctx.lookup(Registries.NOISE_SETTINGS);
        ctx.register(HellDimension.STEM, new LevelStem(types.getOrThrow(HellDimension.TYPE), new NoiseBasedChunkGenerator(
                new HellBiomeSource(biomes.getOrThrow(HellDimension.THE_RACK), biomes.getOrThrow(HellDimension.ASH_WASTES),
                        biomes.getOrThrow(HellDimension.CROWLEYS_CORRIDORS), biomes.getOrThrow(HellDimension.THE_PIT)),
                noise.getOrThrow(HellDimension.NOISE))));
    }
}
