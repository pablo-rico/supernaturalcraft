package org.papiricoh.supernaturalcraft.datagen.cain;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cain.CainSite;

import java.util.concurrent.CompletableFuture;

/** Cain's world data with a provider of its own (v0.21): the farm country his farm may stand in. */
public final class CainWorldData {

    private CainWorldData() {
    }

    public static void gather(GatherDataEvent event, DataGenerator generator, PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
        generator.addProvider(event.includeServer(), new CainBiomeTags(output, lookup, existing));
    }

    static final class CainBiomeTags extends TagsProvider<Biome> {

        CainBiomeTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
            super(output, Registries.BIOME, lookup, SupernaturalCraft.MODID + "_cain", existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(CainSite.BIOMES).add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.FLOWER_FOREST, Biomes.FOREST,
                    Biomes.BIRCH_FOREST, Biomes.SAVANNA);
        }
    }
}
