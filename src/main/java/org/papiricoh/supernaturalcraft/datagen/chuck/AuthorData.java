package org.papiricoh.supernaturalcraft.datagen.chuck;

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
import org.papiricoh.supernaturalcraft.author.AuthorCabinStructure;

import java.util.concurrent.CompletableFuture;

/**
 * The Author's world data that needs providers of its own (the cabin's biome tag), added from
 * {@code SNDataGenerators}. Owned by the world work.
 */
public final class AuthorData {

    private AuthorData() {
    }

    public static void gather(GatherDataEvent event, DataGenerator generator, PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
        generator.addProvider(event.includeServer(), new CabinBiomeTags(output, lookup, existing));
    }

    /** {@code #supernaturalcraft:author_cabin_biomes}: the quiet places he would choose to write in. */
    static final class CabinBiomeTags extends TagsProvider<Biome> {

        CabinBiomeTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
            // A label of its own: the mod's other biome tags provider already uses the plain mod id as its name.
            super(output, Registries.BIOME, lookup, SupernaturalCraft.MODID + "_author_cabin", existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(AuthorCabinStructure.QUIET_BIOMES).add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.CHERRY_GROVE,
                    Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST,
                    Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.SNOWY_PLAINS,
                    Biomes.SNOWY_TAIGA, Biomes.GROVE);
        }
    }
}
