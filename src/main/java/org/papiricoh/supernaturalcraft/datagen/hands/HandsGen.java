package org.papiricoh.supernaturalcraft.datagen.hands;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;
import org.papiricoh.supernaturalcraft.hands.reliquary.ReliquaryStructure;

import java.util.concurrent.CompletableFuture;

/**
 * The reliquaries as datapack entries (v0.42): one configured structure and one structure set per kind (rare, by
 * {@link ReliquaryStructure#spread}), their biome tags ({@code has_structure/<kind>}), the banshee's haunts and the structure tag
 * {@code #supernaturalcraft:hand_reliquaries} the tracking rite and Casifer search. No terrain adjustment: every plan lays its own
 * ground and the builder puts footings where the land falls away. Called from {@code HandsData}.
 */
final class HandsGen {

    private HandsGen() {
    }

    static void structures(BootstrapContext<Structure> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        for (S11Assets.Hand h : S11Assets.Hand.values()) {
            ctx.register(ReliquaryStructure.key(h), new ReliquaryStructure(new Structure.StructureSettings.Builder(biomes.getOrThrow(ReliquaryStructure.biomes(h)))
                    .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
                    .terrainAdapation(TerrainAdjustment.NONE).build(), h));
        }
    }

    static void structureSets(BootstrapContext<StructureSet> ctx) {
        HolderGetter<Structure> structures = ctx.lookup(Registries.STRUCTURE);
        for (S11Assets.Hand h : S11Assets.Hand.values()) {
            ReliquaryStructure.Spread s = ReliquaryStructure.spread(h);
            ctx.register(ReliquaryStructure.set(h), new StructureSet(structures.getOrThrow(ReliquaryStructure.key(h)),
                    new RandomSpreadStructurePlacement(s.spacing(), s.separation(), RandomSpreadType.LINEAR, s.salt())));
        }
    }

    static void gather(GatherDataEvent event, DataGenerator generator, PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                       ExistingFileHelper existing) {
        generator.addProvider(event.includeServer(), new BiomeTagsOf(output, lookup, existing));
        generator.addProvider(event.includeServer(), new StructureTagsOf(output, lookup, existing));
    }

    static final class BiomeTagsOf extends TagsProvider<Biome> {

        BiomeTagsOf(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
            super(output, Registries.BIOME, lookup, SupernaturalCraft.MODID + "_hands", existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(ReliquaryStructure.biomes(S11Assets.Hand.DELPHINES_HAND)).addTag(BiomeTags.IS_OCEAN);
            tag(ReliquaryStructure.biomes(S11Assets.Hand.NAIL_OF_THE_CROSS)).add(Biomes.STONY_PEAKS, Biomes.JAGGED_PEAKS, Biomes.WINDSWEPT_HILLS,
                    Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST);
            tag(ReliquaryStructure.biomes(S11Assets.Hand.APOSTLES_CHALICE)).add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW,
                    Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.FLOWER_FOREST, Biomes.DARK_FOREST, Biomes.TAIGA);
            tag(ReliquaryStructure.biomes(S11Assets.Hand.ROD_OF_AARON)).add(Biomes.DESERT);
            tag(ReliquaryStructure.biomes(S11Assets.Hand.TRUMPET_OF_JERICHO)).add(Biomes.SNOWY_SLOPES, Biomes.GROVE, Biomes.FROZEN_PEAKS);
            tag(HandsRegistry.BANSHEE_HAUNTS).add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP, Biomes.DARK_FOREST);
        }
    }

    static final class StructureTagsOf extends TagsProvider<Structure> {

        StructureTagsOf(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
            super(output, Registries.STRUCTURE, lookup, SupernaturalCraft.MODID + "_hands", existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            var tag = tag(HandsRegistry.HAND_RELIQUARIES);
            for (S11Assets.Hand h : S11Assets.Hand.values()) tag.add(ReliquaryStructure.key(h));
        }
    }
}
