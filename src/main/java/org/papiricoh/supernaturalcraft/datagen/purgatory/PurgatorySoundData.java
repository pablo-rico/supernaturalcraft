package org.papiricoh.supernaturalcraft.datagen.purgatory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Purgatory's sound definitions (v0.39), called from {@code SNSoundDefinitions}. Each event {@code purgatory.<id>} points at its own
 * file ({@code sounds/purgatory/<id>.ogg}, from {@code tools/soundgen/purgatory_sfx.py}) once it exists; until then at a re-pitched
 * vanilla event ({@link #FALLBACK}). Rerun {@code runData} after the files land. Eve's music ({@code music.eve}) is a vanilla track
 * for now. Subtitles: {@link PurgatoryLang#SUBTITLES}.
 */
public final class PurgatorySoundData {

    private PurgatorySoundData() {
    }

    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("purgatory.eve_laugh", new Object[]{"entity.allay.ambient_without_item", 0.6f}),
            Map.entry("purgatory.eve_call", new Object[]{"entity.warden.roar", 1.6f}),
            Map.entry("purgatory.eve_birth", new Object[]{"block.rooted_dirt.break", 0.5f}),
            Map.entry("purgatory.eve_roots", new Object[]{"block.mangrove_roots.break", 0.6f}),
            Map.entry("purgatory.eve_roaches", new Object[]{"entity.silverfish.ambient", 0.7f}),
            Map.entry("purgatory.eve_hurt", new Object[]{"entity.witch.hurt", 1.3f}),
            Map.entry("purgatory.eve_death", new Object[]{"entity.witch.death", 0.8f}),
            Map.entry("purgatory.eve_whisper", new Object[]{"ambient.soul_sand_valley.additions", 1.2f}),
            Map.entry("purgatory.alpha_vampire_command", new Object[]{"entity.cat.hiss", 0.5f}),
            Map.entry("purgatory.alpha_vampire_lunge", new Object[]{"entity.phantom.swoop", 0.8f}),
            Map.entry("purgatory.alpha_werewolf_howl", new Object[]{"entity.wolf.howl", 0.6f}),
            Map.entry("purgatory.alpha_werewolf_slam", new Object[]{"entity.ravager.stunned", 0.7f}),
            Map.entry("purgatory.alpha_shapeshifter_steal", new Object[]{"entity.slime.squish", 0.6f}),
            Map.entry("purgatory.alpha_shapeshifter_slough", new Object[]{"entity.slime.squish_small", 0.5f}),
            Map.entry("purgatory.soul_ambient", new Object[]{"particle.soul_escape", 0.6f}),
            Map.entry("purgatory.soul_hurt", new Object[]{"entity.vex.hurt", 0.6f}),
            Map.entry("purgatory.soul_death", new Object[]{"entity.vex.death", 0.6f}),
            Map.entry("purgatory.rift_open", new Object[]{"block.end_portal.spawn", 0.6f}));
    /** Heard from further off. */
    static final Set<String> FAR = Set.of("purgatory.eve_call", "purgatory.eve_death", "purgatory.alpha_werewolf_howl", "purgatory.rift_open");

    public static void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        AllSounds.PURGATORY_SOUNDS.forEach((event, holder) -> {
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, event.replace('.', '/'));
            SoundDefinition def = SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event);
            if (existing.exists(file, PackType.CLIENT_RESOURCES, ".ogg", "sounds")) {
                def.with(SoundDefinition.Sound.sound(file, SoundDefinition.SoundType.SOUND).attenuationDistance(FAR.contains(event) ? 48 : 20));
            } else {
                Object[] f = FALLBACK.getOrDefault(event, new Object[]{"entity.vex.ambient", 1.0f});
                def.with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace((String) f[0]), SoundDefinition.SoundType.EVENT)
                        .pitch((Float) f[1]));
            }
            add.accept(holder, def);
        });
        add.accept(AllSounds.MUSIC_EVE, SoundDefinition.definition().subtitle("subtitles.supernaturalcraft.music.eve")
                .with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace("music.overworld.deep_dark"), SoundDefinition.SoundType.EVENT)
                        .pitch(0.9f).volume(0.85f)));
    }
}
