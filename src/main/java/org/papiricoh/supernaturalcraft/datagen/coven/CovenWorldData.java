package org.papiricoh.supernaturalcraft.datagen.coven;

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
import org.papiricoh.supernaturalcraft.coven.CovenBiomes;
import org.papiricoh.supernaturalcraft.coven.church.ChurchStructure;
import org.papiricoh.supernaturalcraft.coven.covenstead.CovenStructure;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaSite;

import java.util.concurrent.CompletableFuture;

/** The coven's world data with providers of its own (v0.21): the biomes its structures, its house and its witches use. */
public final class CovenWorldData {

    private CovenWorldData() {
    }

    public static void gather(GatherDataEvent event, DataGenerator generator, PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
        generator.addProvider(event.includeServer(), new CovenBiomeTags(output, lookup, existing));
    }

    static final class CovenBiomeTags extends TagsProvider<Biome> {

        CovenBiomeTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
            super(output, Registries.BIOME, lookup, SupernaturalCraft.MODID + "_coven", existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(ChurchStructure.BIOMES).add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.FOREST, Biomes.BIRCH_FOREST,
                    Biomes.FLOWER_FOREST, Biomes.TAIGA, Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA);
            // v0.31: the haunted house, in any forest or plains.
            tag(org.papiricoh.supernaturalcraft.haunted.HauntedHouseStructure.BIOMES).addTag(net.minecraft.tags.BiomeTags.IS_FOREST)
                    .add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.SNOWY_PLAINS);
            tag(CovenStructure.BIOMES).add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP, Biomes.DARK_FOREST, Biomes.TAIGA,
                    Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA);
            tag(RowenaSite.BIOMES).add(Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.FLOWER_FOREST,
                    Biomes.DARK_FOREST, Biomes.MEADOW, Biomes.PLAINS, Biomes.TAIGA);
            tag(CovenBiomes.HEDGE_WITCH_SPAWNS).add(Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.SWAMP,
                    Biomes.MANGROVE_SWAMP, Biomes.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.PLAINS);
        }
    }
}
