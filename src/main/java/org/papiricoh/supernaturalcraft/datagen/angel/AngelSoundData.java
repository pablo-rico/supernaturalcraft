package org.papiricoh.supernaturalcraft.datagen.angel;

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
 * The angels' and the Fall's sound definitions and subtitles (v0.22), called from {@code SNSoundDefinitions}. Each event
 * {@code angel.<id>} points at its own file ({@code sounds/angel/<id>.ogg}, written by {@code tools/soundgen/angel_sfx.py}) once
 * that file exists; until then at a re-pitched vanilla event ({@link #FALLBACK}). Rerun {@code runData} after the files land.
 */
public final class AngelSoundData {

    private AngelSoundData() {
    }

    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("angel.ambient", "An angel hums"),
            Map.entry("angel.hurt", "An angel is hurt"),
            Map.entry("angel.death", "An angel dies"),
            Map.entry("angel.fallen_ambient", "A fallen angel mutters"),
            Map.entry("angel.fallen_hurt", "A fallen angel is hurt"),
            Map.entry("angel.teleport", "Wings beat"),
            Map.entry("angel.smite_ring", "An angel's light pours out"),
            Map.entry("angel.smite_burst", "An angel burns out"),
            Map.entry("angel.fall_streak", "Something falls from the sky"),
            Map.entry("angel.fall_impact", "A distant impact"));

    /** Stand-ins until the synthesised files exist: vanilla event, pitch. */
    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("angel.ambient", new Object[]{"block.amethyst_block.chime", 0.6f}),
            Map.entry("angel.hurt", new Object[]{"entity.allay.hurt", 0.6f}),
            Map.entry("angel.death", new Object[]{"entity.allay.death", 0.5f}),
            Map.entry("angel.fallen_ambient", new Object[]{"entity.vex.ambient", 0.5f}),
            Map.entry("angel.fallen_hurt", new Object[]{"entity.vex.hurt", 0.6f}),
            Map.entry("angel.teleport", new Object[]{"entity.phantom.flap", 0.6f}),
            Map.entry("angel.smite_ring", new Object[]{"block.beacon.activate", 1.6f}),
            Map.entry("angel.smite_burst", new Object[]{"entity.firework_rocket.blast", 0.7f}),
            Map.entry("angel.fall_streak", new Object[]{"entity.firework_rocket.launch", 0.5f}),
            Map.entry("angel.fall_impact", new Object[]{"entity.generic.explode", 0.5f}));

    /** Heard from far: the smite's flash, and the Fall. */
    static final Set<String> FAR = Set.of("angel.smite_ring", "angel.smite_burst", "angel.fall_streak", "angel.fall_impact");

    public static void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        AllSounds.ANGEL_SOUNDS.forEach((event, holder) -> {
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, event.replace('.', '/'));
            SoundDefinition def = SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event);
            if (existing.exists(file, PackType.CLIENT_RESOURCES, ".ogg", "sounds")) {
                def.with(SoundDefinition.Sound.sound(file, SoundDefinition.SoundType.SOUND).attenuationDistance(FAR.contains(event) ? 64 : 16));
            } else {
                Object[] f = FALLBACK.getOrDefault(event, new Object[]{"block.amethyst_block.chime", 1.0f});
                def.with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace((String) f[0]), SoundDefinition.SoundType.EVENT)
                        .pitch((Float) f[1]));
            }
            add.accept(holder, def);
        });
    }
}
