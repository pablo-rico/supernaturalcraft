package org.papiricoh.supernaturalcraft.datagen.horsemen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.datagen.chuck.ChuckAssetData;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The Four Horsemen's assets as datagen sees them (v0.11): item models, the trophies' block states, particle sprites,
 * sound events and their subtitles. Owned by the art work (it follows what tools/artgen and tools/soundgen write) and
 * called from the shared providers, like {@link ChuckAssetData}.
 */
public final class HorsemenAssetData {

    /** Subtitles of the Horsemen's sounds, by event path. */
    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("entity.war.ambient", "War chuckles"),
            Map.entry("entity.war.hurt", "War grunts"),
            Map.entry("entity.war.death", "War falls"),
            Map.entry("entity.war.rage", "War roars"),
            Map.entry("entity.war.sword_clash", "Swords clash"),
            Map.entry("entity.war.parry", "A blow is parried"),
            Map.entry("entity.famine.ambient", "Famine wheezes"),
            Map.entry("entity.famine.hurt", "Famine groans"),
            Map.entry("entity.famine.death", "Famine rattles his last"),
            Map.entry("entity.famine.devour", "Famine devours a soul"),
            Map.entry("entity.famine.hunger", "A terrible hunger growls"),
            Map.entry("entity.pestilence.ambient", "Pestilence sniffles"),
            Map.entry("entity.pestilence.hurt", "Pestilence hacks"),
            Map.entry("entity.pestilence.death", "Pestilence chokes"),
            Map.entry("entity.pestilence.cough", "Pestilence coughs"),
            Map.entry("entity.death.ambient", "Death breathes"),
            Map.entry("entity.death.hurt", "Death is displeased"),
            Map.entry("entity.death.death", "Death withdraws"),
            Map.entry("entity.death.reap", "A scythe sweeps"),
            Map.entry("entity.death.clock_tick", "A clock ticks"),
            Map.entry("entity.death.limbo_bell", "A distant bell tolls"),
            Map.entry("entity.death.world_flip", "The world turns over"),
            Map.entry("entity.reaper.ambient", "A reaper whispers"),
            Map.entry("entity.reaper.attack", "A reaper strikes"),
            Map.entry("entity.fly_swarm.buzz", "Flies swarm"),
            Map.entry("entity.horseman_steed.neigh", "A Horseman's steed screams"),
            Map.entry("entity.horseman_steed.gallop", "Hooves thunder"));

    private HorsemenAssetData() {
    }

    public static void itemModels(SNItemModelProvider p) {
        String vial = AllItems.ANTIDOTE_VIAL.getId().getPath();
        p.withExistingParent(vial, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + vial));
        for (var trophy : List.of(AllItems.WAR_TROPHY, AllItems.FAMINE_TROPHY, AllItems.PESTILENCE_TROPHY, AllItems.DEATH_TROPHY)) {
            String id = trophy.getId().getPath();
            p.withExistingParent(id, p.modLoc("block/" + id));
        }
        for (var egg : List.of(AllItems.WAR_SPAWN_EGG, AllItems.FAMINE_SPAWN_EGG, AllItems.PESTILENCE_SPAWN_EGG, AllItems.DEATH_SPAWN_EGG,
                AllItems.REAPER_SPAWN_EGG, AllItems.HORSEMAN_STEED_SPAWN_EGG)) {
            p.withExistingParent(egg.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
        }
    }

    /** The trophies are tools/artgen block models (horsemen_items_art.py: a bust facing north), turned by FACING. */
    public static void blockStates(SNBlockStateProvider p) {
        for (var trophy : List.of(AllBlocks.WAR_TROPHY, AllBlocks.FAMINE_TROPHY, AllBlocks.PESTILENCE_TROPHY, AllBlocks.DEATH_TROPHY)) {
            p.horizontalBlock(trophy.get(), new ModelFile.UncheckedModelFile(p.modLoc("block/" + trophy.getId().getPath())));
        }
    }

    public static void particles(ChuckAssetData.ParticleSink sink) {
        sink.spriteSet(AllParticles.FLY.get(), "fly", 4);
        sink.spriteSet(AllParticles.PLAGUE_SPORE.get(), "plague_spore", 4);
        sink.spriteSet(AllParticles.SOUL_WISP.get(), "soul_wisp", 4);
    }

    /**
     * tools/soundgen's OGG files in {@code sounds/horsemen/} (mono, so positional; variants picked at random), a few with a
     * vanilla sound remixed in as another variant. The arenas are wide: the set pieces carry far.
     */
    public static void sounds(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        own(add, AllSounds.WAR_AMBIENT, 0.9f, 20, "war_ambient_1", "war_ambient_2");
        own(add, AllSounds.WAR_HURT, 1.0f, 20, "war_hurt_1", "war_hurt_2");
        own(add, AllSounds.WAR_DEATH, 1.0f, 64, "war_death");
        add.accept(AllSounds.WAR_RAGE, def(AllSounds.WAR_RAGE)
                .with(file("war_rage", 1.0f, 64))
                .with(vanilla("entity.ravager.roar", 0.6f, 1.0f)));
        own(add, AllSounds.WAR_SWORD_CLASH, 1.0f, 32, "war_clash_1", "war_clash_2", "war_clash_3");
        own(add, AllSounds.WAR_PARRY, 1.0f, 32, "war_parry");
        own(add, AllSounds.FAMINE_AMBIENT, 0.9f, 20, "famine_ambient_1", "famine_ambient_2");
        own(add, AllSounds.FAMINE_HURT, 1.0f, 20, "famine_hurt_1", "famine_hurt_2");
        own(add, AllSounds.FAMINE_DEATH, 1.0f, 64, "famine_death");
        own(add, AllSounds.FAMINE_DEVOUR, 1.0f, 48, "famine_devour");
        own(add, AllSounds.FAMINE_HUNGER, 1.0f, 32, "famine_hunger");
        own(add, AllSounds.PESTILENCE_AMBIENT, 0.9f, 20, "pestilence_ambient_1", "pestilence_ambient_2");
        own(add, AllSounds.PESTILENCE_HURT, 1.0f, 20, "pestilence_hurt_1", "pestilence_cough_3");
        own(add, AllSounds.PESTILENCE_DEATH, 1.0f, 64, "pestilence_death");
        own(add, AllSounds.PESTILENCE_COUGH, 1.0f, 32, "pestilence_cough_1", "pestilence_cough_2", "pestilence_cough_3");
        own(add, AllSounds.DEATH_AMBIENT, 0.8f, 24, "death_ambient_1", "death_ambient_2");
        own(add, AllSounds.DEATH_HURT, 1.0f, 24, "death_hurt_1", "death_hurt_2");
        own(add, AllSounds.DEATH_DEATH, 1.0f, 64, "death_death");
        own(add, AllSounds.DEATH_REAP, 1.0f, 32, "death_reap");
        own(add, AllSounds.DEATH_CLOCK_TICK, 0.8f, 16, "death_tick_1", "death_tick_2");
        own(add, AllSounds.DEATH_LIMBO_BELL, 1.0f, 64, "death_limbo_bell");
        own(add, AllSounds.DEATH_WORLD_FLIP, 1.0f, 96, "death_world_flip");
        add.accept(AllSounds.REAPER_AMBIENT, def(AllSounds.REAPER_AMBIENT)
                .with(file("reaper_whisper_1", 0.8f, 16)).with(file("reaper_whisper_2", 0.8f, 16))
                .with(vanilla("ambient.soul_sand_valley.additions", 0.7f, 0.8f)));
        own(add, AllSounds.REAPER_ATTACK, 1.0f, 20, "reaper_attack");
        own(add, AllSounds.FLY_SWARM_BUZZ, 0.7f, 12, "fly_buzz");
        add.accept(AllSounds.STEED_NEIGH, def(AllSounds.STEED_NEIGH)
                .with(file("steed_neigh_1", 1.0f, 32)).with(file("steed_neigh_2", 1.0f, 32))
                .with(vanilla("entity.horse.angry", 0.75f, 1.0f)).with(vanilla("entity.skeleton_horse.ambient", 0.7f, 1.0f)));
        add.accept(AllSounds.STEED_GALLOP, def(AllSounds.STEED_GALLOP)
                .with(file("steed_gallop_1", 0.8f, 24)).with(file("steed_gallop_2", 0.8f, 24))
                .with(vanilla("entity.horse.gallop", 0.8f, 0.9f)));
    }

    private static SoundDefinition def(DeferredHolder<SoundEvent, SoundEvent> event) {
        return SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event.getId().getPath());
    }

    /** One event over its generated variants, each at the given volume and hearing distance. */
    private static void own(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add,
                            DeferredHolder<SoundEvent, SoundEvent> event, float volume, int distance, String... files) {
        SoundDefinition definition = def(event);
        for (String name : files) definition.with(file(name, volume, distance));
        add.accept(event, definition);
    }

    private static SoundDefinition.Sound file(String name, float volume, int distance) {
        return SoundDefinition.Sound.sound(ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, "horsemen/" + name),
                SoundDefinition.SoundType.SOUND).volume(volume).attenuationDistance(distance);
    }

    /** A vanilla sound event as one more variant, re-pitched. */
    private static SoundDefinition.Sound vanilla(String event, float pitch, float volume) {
        return SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace(event), SoundDefinition.SoundType.EVENT)
                .pitch(pitch).volume(volume);
    }
}
