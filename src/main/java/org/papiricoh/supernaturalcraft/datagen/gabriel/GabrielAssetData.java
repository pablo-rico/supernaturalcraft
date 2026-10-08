package org.papiricoh.supernaturalcraft.datagen.gabriel;

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
 * Gabriel's assets as datagen sees them (v0.14): item models, the trophy's block state, sound events and their subtitles.
 * Owned by the art work (it follows what tools/artgen and tools/soundgen write) and called from the shared providers, like
 * {@code MichaelAssetData}. Names: {@code GabrielAssets}.
 *
 * <p>Every sound is tools/soundgen's own file {@code sounds/gabriel/<id>.ogg} (gabriel_sfx.py; mono, so positional); the four
 * jingles are the channels' arena music (seamless loops of 24–32 s), streamed.
 */
public final class GabrielAssetData {

    /** Subtitles of Gabriel's sounds, by event path ({@code gabriel.<id>}). */
    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("gabriel.laugh_track", "A studio audience laughs"),
            Map.entry("gabriel.applause", "A studio audience applauds"),
            Map.entry("gabriel.buzzer", "A buzzer sounds"),
            Map.entry("gabriel.ding", "Ding ding ding!"),
            Map.entry("gabriel.static", "TV static hisses"),
            Map.entry("gabriel.monitor_beep", "A heart monitor beeps"),
            Map.entry("gabriel.defib", "A defibrillator charges"),
            Map.entry("gabriel.piano", "A piano falls"),
            Map.entry("gabriel.pie", "A pie splats"),
            Map.entry("gabriel.snap", "Fingers snap"),
            Map.entry("gabriel.welcome", "Welcome to TV Land!"),
            Map.entry("gabriel.jingle_sitcom", "A sitcom theme plays"),
            Map.entry("gabriel.jingle_game_show", "A game show theme plays"),
            Map.entry("gabriel.jingle_hospital", "A medical drama theme plays"),
            Map.entry("gabriel.jingle_commercial", "A commercial jingle plays"),
            Map.entry("gabriel.ambient", "Gabriel chuckles"),
            Map.entry("gabriel.hurt", "Gabriel is struck"),
            Map.entry("gabriel.death", "The Trickster falls"));

    private GabrielAssetData() {
    }

    public static void itemModels(SNItemModelProvider p) {
        for (var item : List.of(AllItems.TRICKSTER_BAIT, AllItems.TRICKSTER_CANDY, AllItems.CANDY_WRAPPER, AllItems.GABRIEL_BLADE)) {
            String id = item.getId().getPath();
            p.withExistingParent(id, p.mcLoc(item == AllItems.GABRIEL_BLADE ? "item/handheld" : "item/generated"))
                    .texture("layer0", p.modLoc("item/" + id));
        }
        remote(p, AllItems.TRICKSTER_REMOTE.getId().getPath());
        String trophy = AllItems.GABRIEL_TROPHY.getId().getPath();
        p.withExistingParent(trophy, p.modLoc("block/" + trophy));
        p.withExistingParent(AllItems.GABRIEL_SPAWN_EGG.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
    }

    /**
     * The remote: its GeckoLib model (gabriel_items_art.py: upright, centred, 10 px tall, the buttons on its north face) in
     * the hands and on the ground, its rendered icon in the GUI. The transforms are the GeckoLib weapons' (a short sword
     * held up and forward reads as pointing a remote at something).
     */
    private static void remote(SNItemModelProvider p, String name) {
        ModelFile entity = new ModelFile.UncheckedModelFile("builtin/entity");
        ItemModelBuilder base = p.nested().parent(entity).transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(0, -90, 10).translation(0, 3, 0.5f).scale(0.85f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(0, 90, -10).translation(0, 3, 0.5f).scale(0.85f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(60, 90, 0).translation(1.0f, 3.5f, 0f).scale(0.42f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(60, -90, 0).translation(-1.0f, 3.5f, 0f).scale(0.42f).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 0, 0).translation(0, 2, 0).scale(0.5f).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 180, 0).scale(1f).end()
                .transform(ItemDisplayContext.HEAD).rotation(0, 180, 0).translation(0, 13, 7).scale(1f).end()
                .end();
        ItemModelBuilder gui = p.nested().parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", p.modLoc("item/" + name + "_icon"));
        p.getBuilder(name).customLoader(SeparateTransformsModelBuilder::begin).base(base)
                .perspective(ItemDisplayContext.GUI, gui).end();
    }

    /** The trophy is a tools/artgen block model (gabriel_items_art.py: an old wooden television facing north), turned by FACING. */
    public static void blockStates(SNBlockStateProvider p) {
        p.horizontalBlock(AllBlocks.GABRIEL_TROPHY.get(), new ModelFile.UncheckedModelFile(p.modLoc("block/" + AllBlocks.GABRIEL_TROPHY.getId().getPath())));
    }

    /** Volumes and hearing distances: the studio sounds carry across the set; the jingles are streamed music. */
    public static void sounds(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        own(add, AllSounds.GABRIEL_LAUGH_TRACK, 1.0f, 64);
        own(add, AllSounds.GABRIEL_APPLAUSE, 1.0f, 64);
        own(add, AllSounds.GABRIEL_BUZZER, 1.0f, 48);
        own(add, AllSounds.GABRIEL_DING, 1.0f, 48);
        own(add, AllSounds.GABRIEL_STATIC, 0.8f, 24);
        own(add, AllSounds.GABRIEL_MONITOR_BEEP, 0.9f, 48);
        own(add, AllSounds.GABRIEL_DEFIB, 1.0f, 32);
        own(add, AllSounds.GABRIEL_PIANO, 1.0f, 48);
        own(add, AllSounds.GABRIEL_PIE, 1.0f, 16);
        own(add, AllSounds.GABRIEL_SNAP, 1.0f, 32);
        own(add, AllSounds.GABRIEL_WELCOME, 1.0f, 96);
        own(add, AllSounds.GABRIEL_AMBIENT, 0.8f, 16);
        own(add, AllSounds.GABRIEL_HURT, 1.0f, 16);
        own(add, AllSounds.GABRIEL_DEATH, 1.0f, 96);
        for (var jingle : List.of(AllSounds.GABRIEL_JINGLE_SITCOM, AllSounds.GABRIEL_JINGLE_GAME_SHOW, AllSounds.GABRIEL_JINGLE_HOSPITAL,
                AllSounds.GABRIEL_JINGLE_COMMERCIAL)) {
            add.accept(jingle, def(jingle).with(file(jingle, 0.8f, 16).stream(true)));
        }
    }

    private static SoundDefinition def(DeferredHolder<SoundEvent, SoundEvent> event) {
        return SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event.getId().getPath());
    }

    private static void own(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add,
                            DeferredHolder<SoundEvent, SoundEvent> event, float volume, int distance) {
        add.accept(event, def(event).with(file(event, volume, distance)));
    }

    /** {@code gabriel.<id>} -> {@code sounds/gabriel/<id>.ogg}. */
    private static SoundDefinition.Sound file(DeferredHolder<SoundEvent, SoundEvent> event, float volume, int distance) {
        String id = event.getId().getPath().substring("gabriel.".length());
        return SoundDefinition.Sound.sound(ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, "gabriel/" + id),
                SoundDefinition.SoundType.SOUND).volume(volume).attenuationDistance(distance);
    }
}
