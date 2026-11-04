package org.papiricoh.supernaturalcraft.datagen.crowley;

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
 * Crowley's sound definitions (v0.35), called from {@code SNSoundDefinitions}. Each event {@code crowley.<id>} points at its own
 * file ({@code sounds/crowley/<id>.ogg}, from {@code tools/soundgen/crowley_sfx.py}) once it exists; until then at a re-pitched
 * vanilla event ({@link #FALLBACK}). Rerun {@code runData} after the files land. Subtitles: {@link CrowleyLang#SUBTITLES}.
 */
public final class CrowleySoundData {

    private CrowleySoundData() {
    }

    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("crowley.snap", new Object[]{"block.note_block.hat", 1.4f}),
            Map.entry("crowley.smoke_out", new Object[]{"entity.wither.shoot", 0.6f}),
            Map.entry("crowley.smoke_in", new Object[]{"entity.illusioner.mirror_move", 0.7f}),
            Map.entry("crowley.paper_storm", new Object[]{"item.book.page_turn", 0.7f}),
            Map.entry("crowley.hellfire", new Object[]{"item.firecharge.use", 0.7f}),
            Map.entry("crowley.seal_break", new Object[]{"block.glass.break", 0.6f}),
            Map.entry("crowley.hounds", new Object[]{"entity.wolf.growl", 0.6f}),
            Map.entry("crowley.laugh", new Object[]{"entity.villager.trade", 0.7f}),
            Map.entry("crowley.hurt", new Object[]{"entity.villager.hurt", 0.8f}),
            Map.entry("crowley.death", new Object[]{"entity.blaze.death", 0.6f}),
            Map.entry("crowley.suit_hurt", new Object[]{"entity.pillager.hurt", 0.8f}),
            Map.entry("crowley.suit_death", new Object[]{"entity.pillager.death", 0.7f}),
            Map.entry("crowley.patient_ambient", new Object[]{"entity.zombie.ambient", 1.2f}),
            Map.entry("crowley.patient_hurt", new Object[]{"entity.zombie.hurt", 1.2f}),
            Map.entry("crowley.patient_death", new Object[]{"entity.zombie.death", 1.2f}));
    /** Heard from further off (his voice and his tricks carry through the asylum). */
    static final Set<String> FAR = Set.of("crowley.laugh", "crowley.death", "crowley.seal_break", "crowley.hounds", "crowley.hellfire");

    public static void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        AllSounds.CROWLEY_SOUNDS.forEach((event, holder) -> {
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
