package org.papiricoh.supernaturalcraft.datagen.heaven;

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
import org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsStructure;

import java.util.concurrent.CompletableFuture;

/** The wild crossroads' world data with providers of its own (v0.18): the biomes it is found in. Owned by the crossroads work. */
public final class CrossroadsWildData {

    private CrossroadsWildData() {
    }

    public static void gather(GatherDataEvent event, DataGenerator generator, PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
        generator.addProvider(event.includeServer(), new CrossroadsBiomeTags(output, lookup, existing));
    }

    /** {@code #supernaturalcraft:has_structure/crossroads}: open country, where old roads cross. */
    static final class CrossroadsBiomeTags extends TagsProvider<Biome> {

        CrossroadsBiomeTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
            super(output, Registries.BIOME, lookup, SupernaturalCraft.MODID + "_crossroads", existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(CrossroadsStructure.BIOMES).add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.FOREST, Biomes.BIRCH_FOREST,
                    Biomes.FLOWER_FOREST, Biomes.SAVANNA);
        }
    }
}
