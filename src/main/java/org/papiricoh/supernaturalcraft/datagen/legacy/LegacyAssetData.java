package org.papiricoh.supernaturalcraft.datagen.legacy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.legacy.LegacyAssets;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The Men of Letters' generated assets (v0.17): item models, block states and sound definitions. Owned by the art work (agent C);
 * names in {@code legacy.LegacyAssets}; the art is tools/artgen {@code legacy_art.py} / {@code legacy_items_art.py}, the sounds
 * tools/soundgen {@code legacy_sfx.py}.
 */
public final class LegacyAssetData {

    /** Subtitles of the Men of Letters' sounds, by event path ({@code legacy.<id>}). */
    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("legacy.bunker_door", "Bunker door wheel turns"),
            Map.entry("legacy.map_table", "Map table hums"),
            Map.entry("legacy.typewriter", "Typewriter clacks"),
            Map.entry("legacy.paper", "Papers rustle"),
            Map.entry("legacy.research_done", "Research finished"),
            Map.entry("legacy.rank_up", "The Legacy welcomes you"),
            Map.entry("legacy.henry_greet", "Henry clears his throat"),
            Map.entry("legacy.vampire_ambient", "Vampire breathes"),
            Map.entry("legacy.vampire_hiss", "Vampire hisses"),
            Map.entry("legacy.vampire_bite", "Vampire bites"),
            Map.entry("legacy.vampire_hurt", "Vampire hurts"),
            Map.entry("legacy.vampire_death", "Vampire dies"),
            Map.entry("legacy.werewolf_ambient", "Werewolf pants"),
            Map.entry("legacy.werewolf_howl", "Werewolf howls"),
            Map.entry("legacy.werewolf_growl", "Werewolf growls"),
            Map.entry("legacy.werewolf_hurt", "Werewolf hurts"),
            Map.entry("legacy.werewolf_death", "Werewolf dies"),
            Map.entry("legacy.werewolf_turn", "Bones crack and change"),
            Map.entry("legacy.shapeshifter_ambient", "Shapeshifter mutters"),
            Map.entry("legacy.shapeshifter_shed", "Skin sloughs off"),
            Map.entry("legacy.shapeshifter_hurt", "Shapeshifter hurts"),
            Map.entry("legacy.shapeshifter_death", "Shapeshifter dies"));

    /** Item property ids (registered client side by {@code client.legacy.LegacyClientEvents}). */
    public static final ResourceLocation NOTE_TOPIC = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, "note_topic");
    public static final ResourceLocation ARTIFACT_FORM = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, "artifact_form");

    private LegacyAssetData() {
    }

    public static void itemModels(SNItemModelProvider p) {
        for (DeferredItem<? extends Item> item : List.of(AllItems.BUNKER_KEY, AllItems.CASE_FILE, AllItems.DEAD_MANS_BLOOD,
                AllItems.MEN_OF_LETTERS_RING, AllItems.SPELLWRIGHTS_SPECTACLES, AllItems.HENRYS_CASE, AllItems.AQUARIAN_STAR,
                AllItems.BUNKER_DOOR)) {
            p.basicItem(item.get());
        }
        for (DeferredItem<? extends Item> egg : List.of(AllItems.HENRY_WINCHESTER_SPAWN_EGG, AllItems.VAMPIRE_SPAWN_EGG,
                AllItems.WEREWOLF_SPAWN_EGG, AllItems.SHAPESHIFTER_SPAWN_EGG)) {
            p.withExistingParent(egg.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
        }
        for (DeferredItem<? extends Item> block : List.of(AllItems.RESEARCH_DESK, AllItems.MAP_TABLE, AllItems.ARCHIVE_SHELF,
                AllItems.MEN_OF_LETTERS_EMBLEM)) {
            p.withExistingParent(block.getId().getPath(), p.modLoc("block/" + block.getId().getPath()));
        }
        // Field notes: one icon per topic family, picked by the note_topic property (0, .25, .5, .75).
        List<String> topics = LegacyAssets.NOTE_TOPICS;
        for (String t : topics) {
            p.getBuilder("item/field_notes_" + t).parent(new ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", p.modLoc("item/field_notes_" + t));
        }
        ItemModelBuilder notes = p.getBuilder(AllItems.FIELD_NOTES.getId().getPath()).parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", p.modLoc("item/field_notes_" + topics.get(0)));
        for (int i = 1; i < topics.size(); i++) {
            notes.override().predicate(NOTE_TOPIC, i / 4f).model(new ModelFile.UncheckedModelFile(p.modLoc("item/field_notes_" + topics.get(i)))).end();
        }
        // Cursed artifacts: a form icon (layer0) and its halo tinted by rarity (layer1), picked by artifact_form (index / 8).
        List<String> forms = LegacyAssets.ARTIFACT_FORMS;
        for (String f : forms) {
            p.getBuilder("item/artifact_" + f).parent(new ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", p.modLoc("item/artifact_" + f + "_aura")).texture("layer1", p.modLoc("item/artifact_" + f));
        }
        ItemModelBuilder artifact = p.getBuilder(AllItems.CURSED_ARTIFACT.getId().getPath()).parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", p.modLoc("item/artifact_" + forms.get(0) + "_aura")).texture("layer1", p.modLoc("item/artifact_" + forms.get(0)));
        for (int i = 1; i < forms.size(); i++) {
            artifact.override().predicate(ARTIFACT_FORM, i / 8f).model(new ModelFile.UncheckedModelFile(p.modLoc("item/artifact_" + forms.get(i)))).end();
        }
    }

    /** The door is a vanilla door; the rest are tools/artgen block models, turned by their facing if they have one. */
    public static void blockStates(SNBlockStateProvider p) {
        if (AllBlocks.BUNKER_DOOR.get() instanceof DoorBlock door) {
            p.doorBlockWithRenderType(door, p.modLoc("block/bunker_door_bottom"), p.modLoc("block/bunker_door_top"), "cutout");
        }
        facing(p, AllBlocks.RESEARCH_DESK.get(), "research_desk");
        facing(p, AllBlocks.MAP_TABLE.get(), "map_table");
        facing(p, AllBlocks.ARCHIVE_SHELF.get(), "archive_shelf");
        facing(p, AllBlocks.MEN_OF_LETTERS_EMBLEM.get(), "men_of_letters_emblem");
    }

    private static void facing(SNBlockStateProvider p, Block block, String model) {
        ModelFile file = new ModelFile.UncheckedModelFile(p.modLoc("block/" + model));
        if (block.defaultBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) p.horizontalBlock(block, file);
        else p.simpleBlock(block, file);
    }

    /** Every sound is tools/soundgen's own file {@code sounds/legacy/<id>.ogg} (mono, so positional). */
    public static void sounds(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        for (DeferredHolder<SoundEvent, SoundEvent> h : AllSounds.ALL) {
            String path = h.getId().getPath();
            if (!path.startsWith("legacy.")) continue;
            String id = path.substring("legacy.".length());
            float volume = id.endsWith("ambient") ? 0.8f : 1.0f;
            int distance = id.equals("werewolf_howl") || id.equals("rank_up") ? 64 : id.equals("bunker_door") ? 32 : 16;
            add.accept(h, SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + path)
                    .with(SoundDefinition.Sound.sound(ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, "legacy/" + id),
                            SoundDefinition.SoundType.SOUND).volume(volume).attenuationDistance(distance)));
        }
    }
}
