package org.papiricoh.supernaturalcraft.datagen.abaddon;

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
 * Abaddon's sound definitions (v0.21), called from {@code SNSoundDefinitions}. Each event {@code abaddon.<id>} points at its own file
 * ({@code sounds/abaddon/<id>.ogg}, written by {@code tools/soundgen/abaddon_sfx.py}) once that file exists; until then at a re-pitched
 * vanilla event ({@link #FALLBACK}), so {@code runData} never depends on the sound work. Rerun {@code runData} after the files
 * land. Subtitles: {@link AbaddonLang#SUBTITLES}.
 */
public final class AbaddonSoundData {

    private AbaddonSoundData() {
    }

    /** Stand-ins until the synthesised files exist: vanilla event, pitch. */
    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("abaddon.ambient", new Object[]{"entity.witch.ambient", 0.8f}),
            Map.entry("abaddon.laugh", new Object[]{"entity.witch.celebrate", 0.7f}),
            Map.entry("abaddon.hurt", new Object[]{"entity.witch.hurt", 0.8f}),
            Map.entry("abaddon.death", new Object[]{"entity.blaze.death", 0.6f}),
            Map.entry("abaddon.sweep", new Object[]{"item.firecharge.use", 0.7f}),
            Map.entry("abaddon.smoke", new Object[]{"entity.wither.shoot", 0.5f}),
            Map.entry("abaddon.possess", new Object[]{"entity.vex.charge", 0.5f}),
            Map.entry("abaddon.pin", new Object[]{"entity.evoker.cast_spell", 0.7f}),
            Map.entry("abaddon.brand", new Object[]{"entity.blaze.shoot", 0.5f}),
            Map.entry("abaddon.trapped", new Object[]{"block.respawn_anchor.deplete", 0.8f}),
            Map.entry("abaddon.knight_ambient", new Object[]{"entity.iron_golem.step", 0.6f}),
            Map.entry("abaddon.knight_hurt", new Object[]{"entity.iron_golem.hurt", 0.7f}),
            Map.entry("abaddon.knight_death", new Object[]{"entity.iron_golem.death", 0.6f}),
            Map.entry("abaddon.herald_mark", new Object[]{"block.bell.use", 0.6f}));

    /**
     * Heard from further off (48 blocks instead of 24; her hall is 18 round): her laugh, her death, the Brand, the smoke, the
     * herald's bell and the trap's hum (both tolls that should carry across the whole hall).
     */
    static final java.util.Set<String> FAR = java.util.Set.of("abaddon.laugh", "abaddon.death", "abaddon.brand", "abaddon.smoke",
            "abaddon.herald_mark", "abaddon.trapped");

    public static void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        AllSounds.ABADDON_SOUNDS.forEach((event, holder) -> {
            // abaddon.sweep -> supernaturalcraft:abaddon/sweep (sounds/abaddon/sweep.ogg)
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, event.replace('.', '/'));
            SoundDefinition def = SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event);
            if (existing.exists(file, PackType.CLIENT_RESOURCES, ".ogg", "sounds")) {
                def.with(SoundDefinition.Sound.sound(file, SoundDefinition.SoundType.SOUND).attenuationDistance(FAR.contains(event) ? 48 : 24));
            } else {
                Object[] f = FALLBACK.getOrDefault(event, new Object[]{"entity.witch.ambient", 1.0f});
                def.with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace((String) f[0]), SoundDefinition.SoundType.EVENT)
                        .pitch((Float) f[1]));
            }
            add.accept(holder, def);
        });
    }
}
