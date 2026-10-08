package org.papiricoh.supernaturalcraft.datagen.raphael;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Raphael's assets as datagen sees them (v0.16): item models, block states, sound events and their subtitles. Owned by the
 * art work (it follows what tools/artgen and tools/soundgen write) and called from the shared providers, like
 * {@code GabrielAssetData}. Names: {@code RaphaelAssets}.
 */
public final class RaphaelAssetData {

    /** Subtitles of Raphael's sounds, by event path ({@code raphael.<id>}). */
    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("raphael.arrive", "Raphael comes down in a bolt"),
            Map.entry("raphael.thunder", "Thunder cracks close by"),
            Map.entry("raphael.smite", "Raphael smites"),
            Map.entry("raphael.snap", "Fingers snap, and the air bursts"),
            Map.entry("raphael.heal", "Grace hums"),
            Map.entry("raphael.wings", "Great wings beat"),
            Map.entry("raphael.trapped", "Holy fire roars"),
            Map.entry("raphael.ambient", "Raphael breathes, the storm with him"),
            Map.entry("raphael.hurt", "Raphael is struck"),
            Map.entry("raphael.death", "The archangel of the storm falls"),
            Map.entry("raphael.stormcaller_zap", "Lightning leaps"),
            Map.entry("raphael.stormcaller_heal", "A healing grace settles"));

    private RaphaelAssetData() {
    }

    /**
     * The Stormcaller (raphael_items_art.py: upright, centred, {@link #STAFF_HEIGHT} px tall) in the hands and on the ground
     * through GeckoLib, held like the other GeckoLib weapons; its hand-drawn icon in the GUI. The trophy is its block model;
     * the eggs vanilla's template.
     */
    public static void itemModels(SNItemModelProvider p) {
        staff(p, AllItems.RAPHAELS_STORMCALLER.getId().getPath(), STAFF_HEIGHT);
        String trophy = AllItems.RAPHAEL_TROPHY.getId().getPath();
        p.withExistingParent(trophy, p.modLoc("block/" + trophy));
        for (var egg : List.of(AllItems.RAPHAEL_SPAWN_EGG, AllItems.GARRISON_ANGEL_SPAWN_EGG)) {
            p.withExistingParent(egg.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
        }
    }

    /** The staff's height in model pixels (tools/artgen prints it): the in-hand transforms shrink it to a sword's size. */
    static final float STAFF_HEIGHT = 41.7f;

    private static void staff(SNItemModelProvider p, String name, float height) {
        float k = Math.min(1f, 26f / height);
        ModelFile entity = new ModelFile.UncheckedModelFile("builtin/entity");
        ItemModelBuilder base = p.nested().parent(entity).transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(80, -90, 0).translation(0, 2.5f, 2.5f).scale(0.85f * k).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(80, 90, 0).translation(0, 2.5f, 2.5f).scale(0.85f * k).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, -90, -20).translation(0.2f, 2.6f, 1.13f).scale(0.58f * k).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 90, 20).translation(0.2f, 2.6f, 1.13f).scale(0.58f * k).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 0, -45).translation(0, 2, 0).scale(0.5f * k).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 180, -45).scale(k).end()
                .transform(ItemDisplayContext.HEAD).rotation(0, 180, 0).translation(0, 13, 7).scale(k).end()
                .end();
        ItemModelBuilder gui = p.nested().parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", p.modLoc("item/" + name + "_icon"));
        p.getBuilder(name).customLoader(SeparateTransformsModelBuilder::begin).base(base)
                .perspective(ItemDisplayContext.GUI, gui).end();
    }

    /** The trophy is a tools/artgen block model (his bust, facing north), turned by FACING; the slick a floor decal. */
    public static void blockStates(SNBlockStateProvider p) {
        p.horizontalBlock(AllBlocks.RAPHAEL_TROPHY.get(),
                new ModelFile.UncheckedModelFile(p.modLoc("block/" + AllBlocks.RAPHAEL_TROPHY.getId().getPath())));
        String slick = AllBlocks.HOLY_OIL_SLICK.getId().getPath();
        p.simpleBlock(AllBlocks.HOLY_OIL_SLICK.get(), p.models().withExistingParent(slick, p.modLoc("block/template_floor_decal"))
                .texture("decal", p.modLoc("block/" + slick)));
    }

    /** Every sound is tools/soundgen's own file {@code sounds/raphael/<id>.ogg} (raphael_sfx.py; mono, so positional). */
    public static void sounds(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        own(add, AllSounds.RAPHAEL_ARRIVE, 1.0f, 128);
        own(add, AllSounds.RAPHAEL_THUNDER, 1.0f, 128);
        own(add, AllSounds.RAPHAEL_SMITE, 1.0f, 48);
        own(add, AllSounds.RAPHAEL_SNAP, 1.0f, 64);
        own(add, AllSounds.RAPHAEL_HEAL, 0.9f, 32);
        own(add, AllSounds.RAPHAEL_WINGS, 1.0f, 48);
        own(add, AllSounds.RAPHAEL_TRAPPED, 1.0f, 48);
        own(add, AllSounds.RAPHAEL_AMBIENT, 0.8f, 24);
        own(add, AllSounds.RAPHAEL_HURT, 1.0f, 16);
        own(add, AllSounds.RAPHAEL_DEATH, 1.0f, 128);
        own(add, AllSounds.STORMCALLER_ZAP, 1.0f, 24);
        own(add, AllSounds.STORMCALLER_HEAL, 0.9f, 16);
    }

    private static void own(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add,
                            DeferredHolder<SoundEvent, SoundEvent> event, float volume, int distance) {
        String id = event.getId().getPath().substring("raphael.".length());
        add.accept(event, SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event.getId().getPath())
                .with(SoundDefinition.Sound.sound(ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, "raphael/" + id),
                        SoundDefinition.SoundType.SOUND).volume(volume).attenuationDistance(distance)));
    }
}
