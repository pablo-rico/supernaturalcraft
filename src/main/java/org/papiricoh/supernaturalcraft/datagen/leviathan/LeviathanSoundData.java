package org.papiricoh.supernaturalcraft.datagen.leviathan;

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
 * The Leviathans' and Dick Roman's sound definitions and subtitles (v0.25), called from {@code SNSoundDefinitions}. Each event
 * {@code leviathan.<id>} / {@code dick_roman.<id>} points at its own file ({@code sounds/leviathan/<id>.ogg}…, written by
 * {@code tools/soundgen/leviathan_sfx.py}) once that file exists; until then at a re-pitched vanilla event ({@link #FALLBACK}).
 * Rerun {@code runData} after the files land.
 */
public final class LeviathanSoundData {

    private LeviathanSoundData() {
    }

    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("leviathan.ambient", "A Leviathan gurgles"),
            Map.entry("leviathan.reveal", "A Leviathan's head splits open"),
            Map.entry("leviathan.bite", "A Leviathan bites"),
            Map.entry("leviathan.hurt", "A Leviathan is hurt"),
            Map.entry("leviathan.death", "A Leviathan dies"),
            Map.entry("leviathan.head_crawl", "A severed head crawls"),
            Map.entry("leviathan.borax_fizz", "Borax fizzes"),
            Map.entry("dick_roman.arrive", "Dick Roman arrives"),
            Map.entry("dick_roman.laugh", "Dick Roman laughs"),
            Map.entry("dick_roman.maw", "Dick Roman's head splits open"),
            Map.entry("dick_roman.bite", "Dick Roman bites"),
            Map.entry("dick_roman.goo", "Black goo splatters"),
            Map.entry("dick_roman.copy", "Dick Roman changes his face"),
            Map.entry("dick_roman.hurt", "Dick Roman is hurt"),
            Map.entry("dick_roman.death", "Dick Roman bursts"));

    /** Stand-ins until the synthesised files exist: vanilla event, pitch. */
    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("leviathan.ambient", new Object[]{"entity.zombie.ambient", 0.6f}),
            Map.entry("leviathan.reveal", new Object[]{"entity.ravager.roar", 1.3f}),
            Map.entry("leviathan.bite", new Object[]{"entity.evoker_fangs.attack", 0.7f}),
            Map.entry("leviathan.hurt", new Object[]{"entity.zombie.hurt", 0.6f}),
            Map.entry("leviathan.death", new Object[]{"entity.slime.death", 0.5f}),
            Map.entry("leviathan.head_crawl", new Object[]{"entity.slime.squish_small", 0.7f}),
            Map.entry("leviathan.borax_fizz", new Object[]{"block.fire.extinguish", 1.3f}),
            Map.entry("dick_roman.arrive", new Object[]{"block.iron_door.open", 0.7f}),
            Map.entry("dick_roman.laugh", new Object[]{"entity.villager.celebrate", 0.5f}),
            Map.entry("dick_roman.maw", new Object[]{"entity.ravager.roar", 0.9f}),
            Map.entry("dick_roman.bite", new Object[]{"entity.evoker_fangs.attack", 0.5f}),
            Map.entry("dick_roman.goo", new Object[]{"entity.slime.squish", 0.5f}),
            Map.entry("dick_roman.copy", new Object[]{"entity.illusioner.mirror_move", 0.8f}),
            Map.entry("dick_roman.hurt", new Object[]{"entity.ravager.hurt", 1.0f}),
            Map.entry("dick_roman.death", new Object[]{"entity.generic.explode", 0.6f}));

    /** Heard from far: Dick's arrival, the maw and his end. */
    static final Set<String> FAR = Set.of("dick_roman.arrive", "dick_roman.maw", "dick_roman.death");

    public static void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        AllSounds.LEVIATHAN_SOUNDS.forEach((event, holder) -> add.accept(holder, definition(existing, event)));
        AllSounds.DICK_SOUNDS.forEach((event, holder) -> add.accept(holder, definition(existing, event)));
    }

    private static SoundDefinition definition(ExistingFileHelper existing, String event) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, event.replace('.', '/'));
        SoundDefinition def = SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event);
        if (existing.exists(file, PackType.CLIENT_RESOURCES, ".ogg", "sounds")) {
            def.with(SoundDefinition.Sound.sound(file, SoundDefinition.SoundType.SOUND).attenuationDistance(FAR.contains(event) ? 64 : 16));
        } else {
            Object[] f = FALLBACK.getOrDefault(event, new Object[]{"entity.slime.squish", 1.0f});
            def.with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace((String) f[0]), SoundDefinition.SoundType.EVENT)
                    .pitch((Float) f[1]));
        }
        return def;
    }
}
