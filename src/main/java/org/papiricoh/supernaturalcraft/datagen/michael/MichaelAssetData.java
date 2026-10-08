package org.papiricoh.supernaturalcraft.datagen.michael;

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
import org.papiricoh.supernaturalcraft.datagen.chuck.ChuckAssetData;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The Archangel Michael's assets as datagen sees them (v0.12): item models, the trophy's block state, particle sprites,
 * sound events and their subtitles. Owned by the art work (it follows what tools/artgen and tools/soundgen write) and
 * called from the shared providers, like {@code HorsemenAssetData}.
 */
public final class MichaelAssetData {

    /**
     * Where the Lance's tip lies from its grip along local +Z, in blocks ({@code michael_lance_art.TIP_BLOCKS}: 33 px of
     * its 48). The projectiles' renderers shift the model back by it so the tip leads.
     */
    public static final float LANCE_TIP = 2.0625f;

    /** Subtitles of Michael's sounds, by event path. */
    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("entity.michael.ambient", "Michael breathes light"),
            Map.entry("entity.michael.hurt", "Michael is struck"),
            Map.entry("entity.michael.death", "The Sword of Heaven falls"),
            Map.entry("entity.michael.wings", "Great wings beat"),
            Map.entry("entity.michael.smite", "A smiting burns"),
            Map.entry("entity.michael.ask_yes", "Michael asks for your yes"),
            Map.entry("entity.michael.trumpet", "Trumpets of Heaven"),
            Map.entry("entity.michael.transform", "A vessel burns away"),
            Map.entry("entity.michael.halo_break", "A halo shatters"),
            Map.entry("entity.michael.lance_throw", "A lance is hurled"),
            Map.entry("entity.michael.lance_impact", "A lance strikes home"),
            Map.entry("entity.michael.lance_recall", "A lance flies back"),
            Map.entry("entity.michael.feather_storm", "Steel feathers whistle"),
            Map.entry("entity.michael.choir", "A choir swells"),
            Map.entry("entity.michael.dive", "Michael dives"),
            Map.entry("entity.host_angel.ambient", "A soldier of the Host murmurs"),
            Map.entry("entity.host_angel.hurt", "A soldier of the Host is hurt"),
            Map.entry("entity.host_angel.death", "A soldier of the Host burns out"),
            Map.entry("entity.host_angel.march", "The Host marches"),
            Map.entry("entity.host_angel.shield", "Shields lock"),
            Map.entry("item.general_armor.ward", "A wing of light takes the blow"),
            Map.entry("item.michaels_grace.flight", "Wings carry you"));

    /**
     * The files tools/soundgen writes ({@code sounds/michael/<name>.ogg}), by event path: {@code MichaelAssetsTest} checks
     * each exists. Volumes and hearing distances are set in {@link #sounds}.
     */
    public static final Map<String, List<String>> SOUND_FILES = Map.ofEntries(
            Map.entry("entity.michael.ambient", List.of("michael_ambient_1", "michael_ambient_2")),
            Map.entry("entity.michael.hurt", List.of("michael_hurt_1", "michael_hurt_2")),
            Map.entry("entity.michael.death", List.of("michael_death")),
            Map.entry("entity.michael.wings", List.of("michael_wings_1", "michael_wings_2")),
            Map.entry("entity.michael.smite", List.of("michael_smite")),
            Map.entry("entity.michael.ask_yes", List.of("michael_ask_yes")),
            Map.entry("entity.michael.trumpet", List.of("michael_trumpet")),
            Map.entry("entity.michael.transform", List.of("michael_transform")),
            Map.entry("entity.michael.halo_break", List.of("michael_halo_break")),
            Map.entry("entity.michael.lance_throw", List.of("michael_lance_throw")),
            Map.entry("entity.michael.lance_impact", List.of("michael_lance_impact")),
            Map.entry("entity.michael.lance_recall", List.of("michael_lance_recall")),
            Map.entry("entity.michael.feather_storm", List.of("michael_feather_storm")),
            Map.entry("entity.michael.choir", List.of("michael_choir")),
            Map.entry("entity.michael.dive", List.of("michael_dive")),
            Map.entry("entity.host_angel.ambient", List.of("host_ambient_1", "host_ambient_2")),
            Map.entry("entity.host_angel.hurt", List.of("host_hurt_1", "host_hurt_2")),
            Map.entry("entity.host_angel.death", List.of("host_death")),
            Map.entry("entity.host_angel.march", List.of("host_march_1", "host_march_2")),
            Map.entry("entity.host_angel.shield", List.of("host_shield")),
            Map.entry("item.general_armor.ward", List.of("general_armor_ward")),
            Map.entry("item.michaels_grace.flight", List.of("grace_flight")));

    /** Particle sprite sets: name -> frames ({@code textures/particle/<name>_<i>.png}). */
    public static final Map<String, Integer> PARTICLE_FRAMES = Map.of("steel_feather", 4, "halo_ray", 4);

    private MichaelAssetData() {
    }

    public static void itemModels(SNItemModelProvider p) {
        lance(p, AllItems.MICHAEL_LANCE.getId().getPath(), "michael_lance_icon");
        lance(p, AllItems.BORROWED_LANCE.getId().getPath(), "borrowed_lance_icon");
        for (var item : List.of(AllItems.MICHAELS_GRACE, AllItems.GENERAL_HELMET, AllItems.GENERAL_CHESTPLATE, AllItems.GENERAL_LEGGINGS,
                AllItems.GENERAL_BOOTS)) {
            String id = item.getId().getPath();
            p.withExistingParent(id, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + id));
        }
        String trophy = AllItems.MICHAEL_TROPHY.getId().getPath();
        p.withExistingParent(trophy, p.modLoc("block/" + trophy));
        for (var egg : List.of(AllItems.MICHAEL_SPAWN_EGG, AllItems.HOST_ANGEL_SPAWN_EGG)) {
            p.withExistingParent(egg.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
        }
    }

    /**
     * The Lance (and the borrowed one): the GeckoLib model everywhere but the inventory, where its rendered icon is used.
     * The model lies along +Z with its grip at the origin; each rotation here is the GeckoLib weapons' one (blade up) with
     * a quarter turn about X first (+Z up), and the scale treats it as 32 px tall so the spear reads longer than a sword.
     */
    private static void lance(SNItemModelProvider p, String name, String icon) {
        float k = 20f / 32f;
        ModelFile entity = new ModelFile.UncheckedModelFile("builtin/entity");
        ItemModelBuilder base = p.nested().parent(entity).transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(-100, 0, -90).translation(0, 1, 0.5f).scale(0.85f * k).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(170, 0, 90).translation(0, 1, 0.5f).scale(0.85f * k).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(-70, 0, -90).translation(1.13f, 1.5f, 1.13f).scale(0.68f * k).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(-160, 0, 90).translation(1.13f, 1.5f, 1.13f).scale(0.68f * k).end()
                .transform(ItemDisplayContext.GROUND).rotation(-90, 45, 0).translation(0, 2, 0).scale(0.5f * k).end()
                .transform(ItemDisplayContext.FIXED).rotation(-90, -45, 180).scale(k).end()
                .transform(ItemDisplayContext.HEAD).rotation(-90, 0, 180).translation(0, 13, 7).scale(k).end()
                .end();
        ItemModelBuilder gui = p.nested().parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", p.modLoc("item/" + icon));
        p.getBuilder(name).customLoader(SeparateTransformsModelBuilder::begin).base(base)
                .perspective(ItemDisplayContext.GUI, gui).end();
    }

    /** The trophy is a tools/artgen block model (michael_items_art.py: a bust facing north), turned by FACING. */
    public static void blockStates(SNBlockStateProvider p) {
        p.horizontalBlock(AllBlocks.MICHAEL_TROPHY.get(), new ModelFile.UncheckedModelFile(p.modLoc("block/" + AllBlocks.MICHAEL_TROPHY.getId().getPath())));
    }

    public static void particles(ChuckAssetData.ParticleSink sink) {
        sink.spriteSet(AllParticles.STEEL_FEATHER.get(), "steel_feather", PARTICLE_FRAMES.get("steel_feather"));
        sink.spriteSet(AllParticles.HALO_RAY.get(), "halo_ray", PARTICLE_FRAMES.get("halo_ray"));
    }

    /**
     * tools/soundgen's OGG files in {@code sounds/michael/} (mono, so positional; variants picked at random), a few with a
     * vanilla sound remixed in as another layer. Heaven is loud: the set pieces carry across the whole arena.
     */
    public static void sounds(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        own(add, AllSounds.MICHAEL_AMBIENT, 0.8f, 24);
        own(add, AllSounds.MICHAEL_HURT, 1.0f, 24);
        own(add, AllSounds.MICHAEL_DEATH, 1.0f, 96);
        add.accept(AllSounds.MICHAEL_WINGS, withFiles(AllSounds.MICHAEL_WINGS, 1.0f, 48).with(vanilla("entity.ender_dragon.flap", 1.3f, 0.5f)));
        own(add, AllSounds.MICHAEL_SMITE, 1.0f, 32);
        own(add, AllSounds.MICHAEL_ASK_YES, 1.0f, 64);
        own(add, AllSounds.MICHAEL_TRUMPET, 1.0f, 128);
        own(add, AllSounds.MICHAEL_TRANSFORM, 1.0f, 128);
        own(add, AllSounds.MICHAEL_HALO_BREAK, 1.0f, 96);
        own(add, AllSounds.MICHAEL_LANCE_THROW, 1.0f, 48);
        own(add, AllSounds.MICHAEL_LANCE_IMPACT, 1.0f, 64);
        own(add, AllSounds.MICHAEL_LANCE_RECALL, 1.0f, 48);
        own(add, AllSounds.MICHAEL_FEATHER_STORM, 1.0f, 48);
        own(add, AllSounds.MICHAEL_CHOIR, 1.0f, 128);
        own(add, AllSounds.MICHAEL_DIVE, 1.0f, 64);
        own(add, AllSounds.HOST_AMBIENT, 0.7f, 16);
        own(add, AllSounds.HOST_HURT, 1.0f, 16);
        own(add, AllSounds.HOST_DEATH, 1.0f, 24);
        own(add, AllSounds.HOST_MARCH, 0.6f, 24);
        own(add, AllSounds.HOST_SHIELD, 0.9f, 24);
        own(add, AllSounds.GENERAL_ARMOR_WARD, 1.0f, 16);
        own(add, AllSounds.GRACE_FLIGHT, 0.6f, 16);
    }

    private static SoundDefinition def(DeferredHolder<SoundEvent, SoundEvent> event) {
        return SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event.getId().getPath());
    }

    /** One event over its generated variants ({@link #SOUND_FILES}), each at the given volume and hearing distance. */
    private static SoundDefinition withFiles(DeferredHolder<SoundEvent, SoundEvent> event, float volume, int distance) {
        SoundDefinition definition = def(event);
        for (String name : SOUND_FILES.get(event.getId().getPath())) definition.with(file(name, volume, distance));
        return definition;
    }

    private static void own(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add,
                            DeferredHolder<SoundEvent, SoundEvent> event, float volume, int distance) {
        add.accept(event, withFiles(event, volume, distance));
    }

    private static SoundDefinition.Sound file(String name, float volume, int distance) {
        return SoundDefinition.Sound.sound(ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, "michael/" + name),
                SoundDefinition.SoundType.SOUND).volume(volume).attenuationDistance(distance);
    }

    /** A vanilla sound event as one more variant, re-pitched. */
    private static SoundDefinition.Sound vanilla(String event, float pitch, float volume) {
        return SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace(event), SoundDefinition.SoundType.EVENT)
                .pitch(pitch).volume(volume);
    }
}
