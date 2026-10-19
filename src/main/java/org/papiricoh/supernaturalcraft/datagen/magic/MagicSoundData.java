package org.papiricoh.supernaturalcraft.datagen.magic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.MagicAssets;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The magic's sound definitions (v0.21), called from {@code SNSoundDefinitions}: every event points at the file tools/soundgen's
 * {@code magic_sfx.py} writes for it ({@link MagicAssets#soundFile}), with a volume that evens out how loud each school sounds
 * and how far each kind of moment carries. The client plays them from the effects' own payloads ({@code client/magic/fx}).
 * Subtitles: {@link MagicLang#SUBTITLES}.
 */
public final class MagicSoundData {

    private MagicSoundData() {
    }

    /** How far (blocks) each kind of sound carries. */
    private static final Map<String, Integer> DISTANCE = Map.of(
            "cast", 24, "impact", 32, "charge", 16, "complete", 64,
            "bowl.boil", 8, "bowl.seal", 24, "bowl.send", 24, "ritual.rift", 64, "ritual.implode", 32);

    /** Volume per file, evening out the schools' loudness (Infernal's fire runs hot, Enochian's bells thin). */
    private static final Map<String, Float> VOLUME = Map.ofEntries(
            Map.entry("enochian_cast", 0.9f), Map.entry("enochian_impact", 1.0f), Map.entry("enochian_charge", 0.8f),
            Map.entry("enochian_complete", 1.0f),
            Map.entry("infernal_cast", 0.8f), Map.entry("infernal_impact", 0.6f), Map.entry("infernal_charge", 0.6f),
            Map.entry("infernal_complete", 0.65f),
            Map.entry("witchcraft_cast", 0.8f), Map.entry("witchcraft_impact", 0.8f), Map.entry("witchcraft_charge", 1.0f),
            Map.entry("witchcraft_complete", 0.85f),
            Map.entry("hunter_cast", 0.9f), Map.entry("hunter_impact", 1.0f), Map.entry("hunter_charge", 0.75f),
            Map.entry("hunter_complete", 1.0f),
            Map.entry("bowl_boil", 0.7f), Map.entry("bowl_seal", 0.9f), Map.entry("bowl_send", 0.9f),
            Map.entry("ritual_rift", 1.0f), Map.entry("ritual_implode", 0.9f));

    public static void sounds(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        for (String event : MagicAssets.SOUNDS) {
            String file = MagicAssets.soundFile(event);
            // sounds/magic/enochian_cast.ogg -> supernaturalcraft:magic/enochian_cast
            String id = file.substring("sounds/".length(), file.length() - ".ogg".length());
            String base = id.substring(id.lastIndexOf('/') + 1);
            String kind = event.startsWith("magic.") ? event.substring(event.lastIndexOf('.') + 1) : event;
            add.accept(AllSounds.MAGIC_SOUNDS.get(event), SoundDefinition.definition()
                    .subtitle("subtitles.supernaturalcraft." + event)
                    .with(SoundDefinition.Sound.sound(ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, id),
                                    SoundDefinition.SoundType.SOUND)
                            .volume(VOLUME.getOrDefault(base, 0.9f))
                            .attenuationDistance(DISTANCE.getOrDefault(kind, 24))));
        }
    }
}
