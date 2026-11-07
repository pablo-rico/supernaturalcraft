package org.papiricoh.supernaturalcraft.datagen.pact;

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
 * Godstiel's and Castiel's sound definitions (v0.39), called from {@code SNSoundDefinitions}. Each event {@code godstiel.<id>}
 * points at its own file ({@code sounds/godstiel/<id>.ogg}, from {@code tools/soundgen/godstiel_sfx.py}) once it exists; until
 * then at a re-pitched vanilla event ({@link #FALLBACK}). Rerun {@code runData} after the files land. Subtitles: {@link #SUBTITLES}.
 */
public final class GodstielSoundData {

    private GodstielSoundData() {
    }

    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("godstiel.wings", "Wings beat"),
            Map.entry("godstiel.snap", "Fingers snap"),
            Map.entry("godstiel.smite", "Light tears"),
            Map.entry("godstiel.judgment", "Heretics are judged"),
            Map.entry("godstiel.radiance", "Light blazes"),
            Map.entry("godstiel.crack", "Something cracks inside him"),
            Map.entry("godstiel.voices", "Voices whisper"),
            Map.entry("godstiel.bite", "A maw snaps"),
            Map.entry("godstiel.goo", "Goo spatters"),
            Map.entry("godstiel.struggle", "Castiel struggles"),
            Map.entry("godstiel.hurt", "Godstiel is struck"),
            Map.entry("godstiel.death", "The new God falls"));

    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("godstiel.wings", new Object[]{"entity.phantom.flap", 0.6f}),
            Map.entry("godstiel.snap", new Object[]{"block.note_block.hat", 1.2f}),
            Map.entry("godstiel.smite", new Object[]{"entity.lightning_bolt.impact", 1.4f}),
            Map.entry("godstiel.judgment", new Object[]{"block.beacon.deactivate", 0.6f}),
            Map.entry("godstiel.radiance", new Object[]{"block.beacon.activate", 1.4f}),
            Map.entry("godstiel.crack", new Object[]{"block.glass.break", 0.5f}),
            Map.entry("godstiel.voices", new Object[]{"ambient.soul_sand_valley.mood", 1.0f}),
            Map.entry("godstiel.bite", new Object[]{"entity.ravager.attack", 0.8f}),
            Map.entry("godstiel.goo", new Object[]{"block.slime_block.break", 0.6f}),
            Map.entry("godstiel.struggle", new Object[]{"entity.player.hurt_drown", 0.6f}),
            Map.entry("godstiel.hurt", new Object[]{"entity.player.hurt", 0.8f}),
            Map.entry("godstiel.death", new Object[]{"entity.warden.death", 1.2f}));
    /** Heard from further off. */
    static final Set<String> FAR = Set.of("godstiel.smite", "godstiel.judgment", "godstiel.radiance", "godstiel.voices", "godstiel.death");

    public static void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        AllSounds.GODSTIEL_SOUNDS.forEach((event, holder) -> {
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, event.replace('.', '/'));
            SoundDefinition def = SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event);
            if (existing.exists(file, PackType.CLIENT_RESOURCES, ".ogg", "sounds")) {
                def.with(SoundDefinition.Sound.sound(file, SoundDefinition.SoundType.SOUND).attenuationDistance(FAR.contains(event) ? 48 : 24));
            } else {
                Object[] f = FALLBACK.getOrDefault(event, new Object[]{"entity.villager.ambient", 1.0f});
                def.with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace((String) f[0]), SoundDefinition.SoundType.EVENT)
                        .pitch((Float) f[1]));
            }
            add.accept(holder, def);
        });
    }
}
