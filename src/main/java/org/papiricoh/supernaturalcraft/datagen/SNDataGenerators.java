package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Entry point for {@code ./gradlew runData}. Models, lang, loot, tags, recipes, sounds and the
 * worldgen/damage-type registries are generated. Textures and GeckoLib models come from
 * {@code tools/artgen}; sigils, ritual patterns and ritual recipes are hand-written JSON on purpose,
 * as the reference examples for datapack authors.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public class SNDataGenerators {

    private static final RegistrySetBuilder DATAPACK_ENTRIES = new RegistrySetBuilder()
            .add(Registries.DAMAGE_TYPE, AllDamageTypes::bootstrap)
            .add(Registries.CONFIGURED_FEATURE, SNWorldgen::bootstrapConfigured)
            .add(Registries.PLACED_FEATURE, SNWorldgen::bootstrapPlaced)
            .add(Registries.DIMENSION_TYPE, SNHell::bootstrapType)
            .add(Registries.NOISE_SETTINGS, SNHell::bootstrapNoise)
            .add(Registries.BIOME, SNHell::bootstrapBiomes)
            .add(Registries.LEVEL_STEM, SNHell::bootstrapStem)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, SNWorldgen::bootstrapBiomeModifiers)
            .add(Registries.STRUCTURE, SNStructures::bootstrapStructures)
            .add(Registries.STRUCTURE_SET, SNStructures::bootstrapSets);

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        var existing = event.getExistingFileHelper();

        DatapackBuiltinEntriesProvider datapack = new DatapackBuiltinEntriesProvider(
                output, event.getLookupProvider(), DATAPACK_ENTRIES, Set.of(SupernaturalCraft.MODID));
        generator.addProvider(event.includeServer(), datapack);
        // Tag providers that touch datapack registries must see the entries generated above.
        CompletableFuture<HolderLookup.Provider> lookup = datapack.getRegistryProvider();

        var blockTags = generator.addProvider(event.includeServer(), new SNTagsProviders.Blocks(output, lookup, existing));
        generator.addProvider(event.includeServer(), new SNTagsProviders.Items(output, lookup, blockTags.contentsGetter(), existing));
        generator.addProvider(event.includeServer(), new SNTagsProviders.Entities(output, lookup, existing));
        generator.addProvider(event.includeServer(), new SNTagsProviders.DamageTypes(output, lookup, existing));
        generator.addProvider(event.includeServer(), new SNTagsProviders.Structures(output, lookup, existing));
        generator.addProvider(event.includeServer(), new GraveBiomeTags(output, lookup, existing));
        generator.addProvider(event.includeServer(), new SNLootTableProvider(output, lookup));
        generator.addProvider(event.includeServer(), new SNRecipeProvider(output, lookup));
        generator.addProvider(event.includeServer(), new SNLootModifiers(output, lookup));
        generator.addProvider(event.includeServer(), new net.neoforged.neoforge.common.data.AdvancementProvider(output, lookup, existing,
                java.util.List.of(new SNAdvancements())));

        generator.addProvider(event.includeClient(), new SNLanguageProvider(output));
        generator.addProvider(event.includeClient(), new SNBlockStateProvider(output, existing));
        generator.addProvider(event.includeClient(), new SNItemModelProvider(output, existing));
        generator.addProvider(event.includeClient(), new SNSoundDefinitions(output, existing));
        generator.addProvider(event.includeClient(), new SNParticleDescriptions(output, existing));
    }
}
