package org.papiricoh.supernaturalcraft.datagen.allegiance;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceAssets;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The allegiance's assets as datagen sees them (v0.13): item models, the holy oil fire's block state, sound events and
 * their subtitles. Owned by the art work (it follows what tools/artgen and tools/soundgen write) and called from the
 * shared providers, like {@code MichaelAssetData}. Names: {@code AllegianceAssets}.
 */
public final class AllegianceAssetData {

    /** Subtitles of the allegiance's sounds, by event path ({@code allegiance.<id>}). */
    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("allegiance.ascend_angel", "A choir raises you"),
            Map.entry("allegiance.ascend_demon", "Hell roars its welcome"),
            Map.entry("allegiance.ascend_hunter", "A hunter earns a name"),
            Map.entry("allegiance.teleport", "Unseen wings flutter"),
            Map.entry("allegiance.smoke", "Black smoke pours out"),
            Map.entry("allegiance.smite", "A smiting burns"),
            Map.entry("allegiance.radio", "Angel radio whispers"),
            Map.entry("allegiance.throne", "The throne commands"),
            Map.entry("allegiance.true_form", "True form blazes"),
            Map.entry("allegiance.expel", "A demon is torn out"),
            Map.entry("allegiance.holy_oil", "Holy oil catches"),
            Map.entry("allegiance.heal", "A healing touch"),
            Map.entry("allegiance.lance", "A lance of light flies"),
            Map.entry("allegiance.telekinesis", "Something is seized"));

    /** Volume and hearing distance of each sound ({@code sounds/allegiance/<id>.ogg}, one file each). */
    private static final Map<String, float[]> MIX = Map.ofEntries(
            Map.entry("ascend_angel", new float[]{1.0f, 48}), Map.entry("ascend_demon", new float[]{1.0f, 48}),
            Map.entry("ascend_hunter", new float[]{1.0f, 32}), Map.entry("teleport", new float[]{0.8f, 16}),
            Map.entry("smoke", new float[]{0.9f, 16}), Map.entry("smite", new float[]{1.0f, 24}),
            Map.entry("radio", new float[]{0.6f, 8}), Map.entry("throne", new float[]{1.0f, 32}),
            Map.entry("true_form", new float[]{1.0f, 64}), Map.entry("expel", new float[]{1.0f, 32}),
            Map.entry("holy_oil", new float[]{0.9f, 16}), Map.entry("heal", new float[]{0.7f, 12}),
            Map.entry("lance", new float[]{1.0f, 32}), Map.entry("telekinesis", new float[]{0.8f, 16}));

    private AllegianceAssetData() {
    }

    public static void itemModels(SNItemModelProvider p) {
        for (var item : List.of(AllItems.VIAL_OF_GRACE, AllItems.HOLY_OIL, AllItems.PURIFIED_BLOOD)) {
            String id = item.getId().getPath();
            p.withExistingParent(id, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + id));
        }
        p.withExistingParent(AllItems.RIVAL_HUNTER_SPAWN_EGG.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
    }

    /** The holy oil fire: vanilla's floor-fire planes ({@code template_fire_floor}) with either of two animated textures. */
    public static void blockStates(SNBlockStateProvider p) {
        ConfiguredModel[] models = AllegianceAssets.FIRE_TEXTURES.stream()
                .map(tex -> new ConfiguredModel(p.models().withExistingParent(tex, p.mcLoc("block/template_fire_floor"))
                        .texture("fire", p.modLoc("block/" + tex)).texture("particle", p.modLoc("block/" + tex)).renderType("cutout")))
                .toArray(ConfiguredModel[]::new);
        p.getVariantBuilder(AllBlocks.HOLY_OIL_FIRE.get()).partialState().setModels(models);
    }

    /** tools/soundgen's OGG files in {@code sounds/allegiance/} (mono, so positional). */
    public static void sounds(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        for (var event : List.of(AllSounds.ALLEGIANCE_ASCEND_ANGEL, AllSounds.ALLEGIANCE_ASCEND_DEMON, AllSounds.ALLEGIANCE_ASCEND_HUNTER,
                AllSounds.ALLEGIANCE_TELEPORT, AllSounds.ALLEGIANCE_SMOKE, AllSounds.ALLEGIANCE_SMITE, AllSounds.ALLEGIANCE_RADIO,
                AllSounds.ALLEGIANCE_THRONE, AllSounds.ALLEGIANCE_TRUE_FORM, AllSounds.ALLEGIANCE_EXPEL, AllSounds.ALLEGIANCE_HOLY_OIL,
                AllSounds.ALLEGIANCE_HEAL, AllSounds.ALLEGIANCE_LANCE, AllSounds.ALLEGIANCE_TELEKINESIS)) {
            String path = event.getId().getPath();
            String id = path.substring("allegiance.".length());
            float[] mix = MIX.getOrDefault(id, new float[]{1.0f, 16});
            add.accept(event, SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + path)
                    .with(SoundDefinition.Sound.sound(ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, "allegiance/" + id),
                            SoundDefinition.SoundType.SOUND).volume(mix[0]).attenuationDistance((int) mix[1])));
        }
    }
}
