package org.papiricoh.supernaturalcraft.datagen.cain;

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
 * Cain's sound definitions (v0.21), called from {@code SNSoundDefinitions}. Each event {@code cain.<id>} points at its own file
 * ({@code sounds/cain/<id>.ogg}, written by {@code tools/soundgen/cain_sfx.py}) once that file exists; until then at a re-pitched
 * vanilla event ({@link #FALLBACK}), so {@code runData} never depends on the sound work. Rerun {@code runData} after the files
 * land. Subtitles: {@link CainLang#SUBTITLES}.
 */
public final class CainSoundData {

    private CainSoundData() {
    }

    /** Stand-ins until the synthesised files exist: vanilla event, pitch. */
    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("cain.ambient", new Object[]{"entity.vindicator.ambient", 0.7f}),
            Map.entry("cain.hurt", new Object[]{"entity.vindicator.hurt", 0.7f}),
            Map.entry("cain.death", new Object[]{"entity.vindicator.death", 0.6f}),
            Map.entry("cain.lunge", new Object[]{"entity.ravager.attack", 1.3f}),
            Map.entry("cain.swarm", new Object[]{"entity.bee.loop_aggressive", 0.8f}),
            Map.entry("cain.bees", new Object[]{"block.beehive.work", 1.0f}),
            Map.entry("cain.furrow", new Object[]{"block.rooted_dirt.break", 0.6f}),
            Map.entry("cain.abels_blood", new Object[]{"block.pointed_dripstone.land", 0.6f}),
            Map.entry("cain.throw", new Object[]{"item.trident.throw", 0.8f}),
            Map.entry("cain.execution", new Object[]{"entity.player.attack.crit", 0.6f}),
            Map.entry("cain.mark_flare", new Object[]{"entity.warden.sonic_charge", 0.8f}),
            Map.entry("cain.offer_mark", new Object[]{"entity.warden.heartbeat", 0.7f}));

    /** Heard across the whole yard and beyond (the arena is 16 blocks round): the Mark's roar, his fall, Abel's blood. */
    static final java.util.Set<String> FAR = java.util.Set.of("cain.mark_flare", "cain.death", "cain.abels_blood", "cain.swarm");

    public static void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        AllSounds.CAIN_SOUNDS.forEach((event, holder) -> {
            // cain.lunge -> supernaturalcraft:cain/lunge (sounds/cain/lunge.ogg)
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, event.replace('.', '/'));
            SoundDefinition def = SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event);
            if (existing.exists(file, PackType.CLIENT_RESOURCES, ".ogg", "sounds")) {
                def.with(SoundDefinition.Sound.sound(file, SoundDefinition.SoundType.SOUND).attenuationDistance(FAR.contains(event) ? 48 : 24));
            } else {
                Object[] f = FALLBACK.getOrDefault(event, new Object[]{"entity.vindicator.ambient", 1.0f});
                def.with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace((String) f[0]), SoundDefinition.SoundType.EVENT)
                        .pitch((Float) f[1]));
            }
            add.accept(holder, def);
        });
    }
}
