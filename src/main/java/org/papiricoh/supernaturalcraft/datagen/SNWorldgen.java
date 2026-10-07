package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.List;

/** Ores: rock salt through the overworld's upper stone, sulfur in netherrack. */
public class SNWorldgen {

    public static final ResourceKey<ConfiguredFeature<?, ?>> ROCK_SALT_ORE_CF = configured("ore_rock_salt");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SULFUR_ORE_CF = configured("ore_sulfur");
    public static final ResourceKey<PlacedFeature> ROCK_SALT_ORE = placed("ore_rock_salt");
    public static final ResourceKey<PlacedFeature> SULFUR_ORE = placed("ore_sulfur");
    // Hell
    public static final ResourceKey<ConfiguredFeature<?, ?>> BRIMSTONE_ORE_CF = configured("ore_brimstone");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ABYSSAL_SHARD_ORE_CF = configured("ore_abyssal_shard");
    public static final ResourceKey<ConfiguredFeature<?, ?>> HANGING_HOOKS_CF = configured("hanging_hooks");
    public static final ResourceKey<ConfiguredFeature<?, ?>> HELLFIRE_VENT_CF = configured("hellfire_vent");
    public static final ResourceKey<PlacedFeature> BRIMSTONE_ORE = placed("ore_brimstone");
    public static final ResourceKey<PlacedFeature> BRIMSTONE_ORE_RARE = placed("ore_brimstone_rare");
    public static final ResourceKey<PlacedFeature> ABYSSAL_SHARD_ORE = placed("ore_abyssal_shard");
    public static final ResourceKey<PlacedFeature> HANGING_HOOKS = placed("hanging_hooks");
    public static final ResourceKey<PlacedFeature> HELLFIRE_VENTS = placed("hellfire_vents");

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, SupernaturalCraft.asResource(name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, SupernaturalCraft.asResource(name));
    }

    public static void bootstrapConfigured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(ROCK_SALT_ORE_CF, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(List.of(
                OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), AllBlocks.ROCK_SALT_ORE.get().defaultBlockState()),
                OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), AllBlocks.DEEPSLATE_ROCK_SALT_ORE.get().defaultBlockState())),
                10)));
        ctx.register(SULFUR_ORE_CF, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(
                new TagMatchTest(BlockTags.BASE_STONE_NETHER), AllBlocks.NETHER_SULFUR_ORE.get().defaultBlockState(), 9)));
        var hellStones = new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(AllBlocks.HELLSTONE.get());
        var rackStones = new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(AllBlocks.RACK_STONE.get());
        ctx.register(BRIMSTONE_ORE_CF, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(List.of(
                OreConfiguration.target(hellStones, AllBlocks.BRIMSTONE_ORE.get().defaultBlockState()),
                OreConfiguration.target(rackStones, AllBlocks.BRIMSTONE_ORE.get().defaultBlockState())), 8)));
        ctx.register(ABYSSAL_SHARD_ORE_CF, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(
                new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(AllBlocks.ABYSSAL_STONE.get()),
                AllBlocks.ABYSSAL_SHARD_ORE.get().defaultBlockState(), 5)));
        ctx.register(HANGING_HOOKS_CF, new ConfiguredFeature<>(org.papiricoh.supernaturalcraft.registry.AllWorldgen.HANGING_HOOKS.get(),
                net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration.NONE));
        ctx.register(HELLFIRE_VENT_CF, new ConfiguredFeature<>(Feature.SIMPLE_BLOCK,
                new net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration(
                        net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider.simple(AllBlocks.HELLFIRE_VENT.get()))));
    }

    public static void bootstrapPlaced(BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
        ctx.register(ROCK_SALT_ORE, new PlacedFeature(cf.getOrThrow(ROCK_SALT_ORE_CF), List.of(
                CountPlacement.of(8), InSquarePlacement.spread(),
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-24), VerticalAnchor.absolute(96)), BiomeFilter.biome())));
        ctx.register(SULFUR_ORE, new PlacedFeature(cf.getOrThrow(SULFUR_ORE_CF), List.of(
                CountPlacement.of(12), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(10), VerticalAnchor.absolute(118)), BiomeFilter.biome())));
        ctx.register(BRIMSTONE_ORE, new PlacedFeature(cf.getOrThrow(BRIMSTONE_ORE_CF), List.of(
                CountPlacement.of(14), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(8), VerticalAnchor.absolute(240)), BiomeFilter.biome())));
        ctx.register(BRIMSTONE_ORE_RARE, new PlacedFeature(cf.getOrThrow(BRIMSTONE_ORE_CF), List.of(
                CountPlacement.of(3), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(8), VerticalAnchor.absolute(240)), BiomeFilter.biome())));
        ctx.register(ABYSSAL_SHARD_ORE, new PlacedFeature(cf.getOrThrow(ABYSSAL_SHARD_ORE_CF), List.of(
                CountPlacement.of(10), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(16), VerticalAnchor.absolute(232)), BiomeFilter.biome())));
        ctx.register(HANGING_HOOKS, new PlacedFeature(cf.getOrThrow(HANGING_HOOKS_CF), List.of(
                CountPlacement.of(24), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(40), VerticalAnchor.absolute(220)), BiomeFilter.biome())));
        ctx.register(HELLFIRE_VENTS, new PlacedFeature(cf.getOrThrow(HELLFIRE_VENT_CF), List.of(
                CountPlacement.of(20), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(34), VerticalAnchor.absolute(220)),
                net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement.scanningFor(net.minecraft.core.Direction.DOWN,
                        net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate.solid(),
                        net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate.ONLY_IN_AIR_PREDICATE, 16),
                net.minecraft.world.level.levelgen.placement.BlockPredicateFilter.forPredicate(
                        net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate.matchesBlocks(
                                new net.minecraft.core.Vec3i(0, 1, 0), net.minecraft.world.level.block.Blocks.AIR)),
                BiomeFilter.biome())));
    }

    public static void bootstrapBiomeModifiers(BootstrapContext<BiomeModifier> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE);
        ctx.register(modifier("add_rock_salt_ore"), new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD), HolderSet.direct(placed.getOrThrow(ROCK_SALT_ORE)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        ctx.register(modifier("add_sulfur_ore"), new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_NETHER), HolderSet.direct(placed.getOrThrow(SULFUR_ORE)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        SNSpawns.bootstrap(ctx, biomes);
    }

    static ResourceKey<BiomeModifier> modifier(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, SupernaturalCraft.asResource(name));
    }
}
