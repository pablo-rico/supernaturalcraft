package org.papiricoh.supernaturalcraft.datagen.legacy;

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
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerStructure;

import java.util.concurrent.CompletableFuture;

/** The Men of Letters' world data with providers of its own (the bunker's biome tag), added from {@code SNDataGenerators}. Agent A. */
public final class LegacyWorldData {

    private LegacyWorldData() {
    }

    public static void gather(GatherDataEvent event, DataGenerator generator, PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
        generator.addProvider(event.includeServer(), new BunkerBiomeTags(output, lookup, existing));
    }

    /** The order's banner pattern (v0.17.1): the eye in the Aquarian star; the bunker hangs it in gold on black. */
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.block.entity.BannerPattern> MEN_OF_LETTERS =
            net.minecraft.resources.ResourceKey.create(Registries.BANNER_PATTERN, SupernaturalCraft.asResource("men_of_letters"));

    public static void bootstrapBanners(net.minecraft.data.worldgen.BootstrapContext<net.minecraft.world.level.block.entity.BannerPattern> ctx) {
        ctx.register(MEN_OF_LETTERS, new net.minecraft.world.level.block.entity.BannerPattern(SupernaturalCraft.asResource("men_of_letters"),
                "block.supernaturalcraft.banner.men_of_letters"));
    }

    /** {@code #supernaturalcraft:bunker_biomes}: open country, like Lebanon, Kansas. */
    static final class BunkerBiomeTags extends TagsProvider<Biome> {

        BunkerBiomeTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existing) {
            super(output, Registries.BIOME, lookup, SupernaturalCraft.MODID + "_bunker", existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(BunkerStructure.BIOMES).add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU,
                    Biomes.FOREST, Biomes.BIRCH_FOREST);
        }
    }
}
