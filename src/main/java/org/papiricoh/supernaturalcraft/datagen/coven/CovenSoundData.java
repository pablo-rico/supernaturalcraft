package org.papiricoh.supernaturalcraft.datagen.coven;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The witches', Rowena's and the Grand Witch's sound definitions (v0.21), called from {@code SNSoundDefinitions}. Each event
 * {@code witch.<id>}/{@code rowena.<id>}/{@code grand_witch.<id>} points at its own file ({@code sounds/witch/<id>.ogg}, written
 * by {@code tools/soundgen/coven_sfx.py}; {@code sounds/grand_witch/<id>.ogg} by {@code tools/soundgen/grandwitch_sfx.py}) once
 * that file exists; until then at a re-pitched vanilla event ({@link #FALLBACK}), so
 * {@code runData} never depends on the sound work. Rerun {@code runData} after the files land. Subtitles: {@link CovenLang#SUBTITLES}.
 */
public final class CovenSoundData {

    private CovenSoundData() {
    }

    /** Stand-ins until the synthesised files exist: vanilla event, pitch. */
    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("witch.hedge_ambient", new Object[]{"entity.witch.ambient", 1.1f}),
            Map.entry("witch.hedge_hurt", new Object[]{"entity.witch.hurt", 1.1f}),
            Map.entry("witch.hedge_death", new Object[]{"entity.witch.death", 1.1f}),
            Map.entry("witch.coven_ambient", new Object[]{"entity.witch.ambient", 0.8f}),
            Map.entry("witch.coven_hurt", new Object[]{"entity.witch.hurt", 0.8f}),
            Map.entry("witch.coven_death", new Object[]{"entity.witch.death", 0.8f}),
            Map.entry("witch.cast", new Object[]{"entity.evoker.cast_spell", 1.3f}),
            Map.entry("witch.hex", new Object[]{"entity.evoker.prepare_attack", 1.2f}),
            Map.entry("witch.teleport", new Object[]{"entity.illusioner.mirror_move", 0.9f}),
            Map.entry("witch.shield_up", new Object[]{"block.beacon.activate", 1.6f}),
            Map.entry("witch.shield_break", new Object[]{"block.glass.break", 0.7f}),
            Map.entry("witch.channel", new Object[]{"block.beacon.ambient", 1.4f}),
            Map.entry("witch.cat_ambient", new Object[]{"entity.cat.ambient", 0.8f}),
            Map.entry("witch.raven_caw", new Object[]{"entity.parrot.ambient", 0.6f}),
            Map.entry("witch.familiar_steal", new Object[]{"entity.fox.steal", 0.8f}),
            Map.entry("witch.victim_ambient", new Object[]{"entity.villager.ambient", 0.6f}),
            Map.entry("witch.victim_freed", new Object[]{"entity.villager.celebrate", 1.0f}),
            Map.entry("rowena.ambient", new Object[]{"entity.witch.ambient", 1.25f}),
            Map.entry("rowena.laugh", new Object[]{"entity.witch.celebrate", 1.3f}),
            Map.entry("rowena.talk", new Object[]{"entity.villager.yes", 1.2f}),
            Map.entry("rowena.hurt", new Object[]{"entity.witch.hurt", 1.3f}),
            Map.entry("rowena.death", new Object[]{"entity.witch.death", 1.2f}),
            Map.entry("rowena.fling", new Object[]{"entity.evoker.cast_spell", 1.0f}),
            Map.entry("rowena.ignis", new Object[]{"entity.blaze.shoot", 0.9f}),
            Map.entry("rowena.glacies", new Object[]{"block.glass.break", 1.4f}),
            Map.entry("rowena.summon", new Object[]{"entity.evoker.prepare_summon", 1.1f}),
            Map.entry("rowena.recite", new Object[]{"entity.evoker.prepare_wololo", 1.2f}),
            Map.entry("rowena.teleport", new Object[]{"entity.illusioner.mirror_move", 1.1f}),
            // v0.21 (P6): the Grand Witch.
            Map.entry("grand_witch.ambient", new Object[]{"entity.witch.ambient", 0.7f}),
            Map.entry("grand_witch.laugh", new Object[]{"entity.witch.celebrate", 0.75f}),
            Map.entry("grand_witch.hurt", new Object[]{"entity.witch.hurt", 0.7f}),
            Map.entry("grand_witch.death", new Object[]{"entity.witch.death", 0.6f}),
            Map.entry("grand_witch.shield", new Object[]{"block.beacon.power_select", 1.2f}),
            Map.entry("grand_witch.shield_break", new Object[]{"block.glass.break", 0.5f}),
            Map.entry("grand_witch.frost_hex", new Object[]{"block.glass.break", 1.2f}),
            Map.entry("grand_witch.inferno", new Object[]{"entity.blaze.shoot", 0.7f}),
            Map.entry("grand_witch.crush", new Object[]{"entity.evoker.cast_spell", 0.7f}),
            Map.entry("grand_witch.taglock", new Object[]{"entity.evoker.prepare_attack", 0.8f}),
            Map.entry("grand_witch.smoke_veil", new Object[]{"entity.illusioner.mirror_move", 0.8f}));

    /** Volume per synthesised file (1 if unlisted): the ambient voices and the channel's hum sit under the fight. */
    static final Map<String, Float> VOLUME = Map.ofEntries(
            Map.entry("witch.hedge_ambient", 0.8f), Map.entry("witch.coven_ambient", 0.8f), Map.entry("witch.channel", 0.7f),
            Map.entry("witch.cat_ambient", 0.7f), Map.entry("witch.raven_caw", 0.85f), Map.entry("witch.victim_ambient", 0.75f),
            Map.entry("rowena.ambient", 0.8f), Map.entry("rowena.recite", 0.85f), Map.entry("grand_witch.ambient", 0.8f),
            Map.entry("grand_witch.taglock", 0.9f));

    public static void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        AllSounds.COVEN_SOUNDS.forEach((event, holder) -> {
            // witch.cast -> supernaturalcraft:witch/cast (sounds/witch/cast.ogg)
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, event.replace('.', '/'));
            SoundDefinition def = SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event);
            if (existing.exists(file, PackType.CLIENT_RESOURCES, ".ogg", "sounds")) {
                def.with(SoundDefinition.Sound.sound(file, SoundDefinition.SoundType.SOUND).attenuationDistance(event.startsWith("rowena.") || event.startsWith("grand_witch.") ? 24 : 16)
                        .volume(VOLUME.getOrDefault(event, 1f)));
            } else {
                Object[] f = FALLBACK.getOrDefault(event, new Object[]{"entity.witch.ambient", 1.0f});
                def.with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace((String) f[0]), SoundDefinition.SoundType.EVENT)
                        .pitch((Float) f[1]));
            }
            add.accept(holder, def);
        });
    }
}
