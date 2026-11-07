package org.papiricoh.supernaturalcraft.datagen.slice;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * v0.41: one content slice's share of {@code runData}, so a slice never edits the shared providers. Every shared provider
 * walks {@link Slices#ALL} and calls the hook it owns; a slice overrides only what it needs. The English text goes through
 * {@link #lang}; the Spanish is hand-written in {@code es_es.json} as always.
 */
public interface SliceData {

    /** English text: {@code add.accept(key, text)}. */
    default void lang(BiConsumer<String, String> add) {
    }

    /** English names by registry id (items, blocks, entities) where the title-cased id is wrong. */
    default Map<String, String> names() {
        return Map.of();
    }

    /** Subtitles by sound path (as {@code AllSounds} registered it); the rest fall back to a title-cased path. */
    default Map<String, String> subtitles() {
        return Map.of();
    }

    default void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
    }

    default void blockStates(SNBlockStateProvider p, ExistingFileHelper existing) {
    }

    default void itemModels(SNItemModelProvider p, ExistingFileHelper existing) {
    }

    default void entityLoot(BiConsumer<EntityType<?>, LootTable.Builder> add) {
    }

    /** Block loot: {@code dropSelf} for the plain ones, {@code add} for the rest. */
    default void blockLoot(Consumer<Block> dropSelf, BiConsumer<Block, LootTable.Builder> add) {
    }

    default void blockTags(BiConsumer<TagKey<Block>, Block> add) {
    }

    default void itemTags(BiConsumer<TagKey<Item>, Item> add) {
    }

    default void entityTags(BiConsumer<TagKey<EntityType<?>>, EntityType<?>> add) {
    }

    default void recipes(RecipeOutput out) {
    }

    // --- Worldgen (datapack registries) ---------------------------------------------------------------------------

    default void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
    }

    default void placedFeatures(BootstrapContext<PlacedFeature> ctx) {
    }

    default void dimensionTypes(BootstrapContext<DimensionType> ctx) {
    }

    default void noises(BootstrapContext<NormalNoise.NoiseParameters> ctx) {
    }

    default void noiseSettings(BootstrapContext<NoiseGeneratorSettings> ctx) {
    }

    default void biomes(BootstrapContext<Biome> ctx) {
    }

    default void levelStems(BootstrapContext<LevelStem> ctx) {
    }

    default void structures(BootstrapContext<Structure> ctx) {
    }

    default void structureSets(BootstrapContext<StructureSet> ctx) {
    }

    /** Anything else (its own providers: structure tags, biome tags…). */
    default void gather(GatherDataEvent event, DataGenerator generator, PackOutput output,
                        CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
    }
}
