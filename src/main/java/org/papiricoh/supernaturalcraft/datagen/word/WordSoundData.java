package org.papiricoh.supernaturalcraft.datagen.word;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.function.BiConsumer;

/** The Word's sounds (v0.36): event {@code word.<id>} → {@code sounds/word/<id>.ogg} (tools/soundgen/word_sfx.py), mono. */
public final class WordSoundData {

    private WordSoundData() {
    }

    public static void sounds(BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        for (var event : List.of(AllSounds.BANISH_BLAST, AllSounds.WARD_HUM, AllSounds.ALARM_CHIME, AllSounds.TABLET_WHISPER,
                AllSounds.GLYPH_WRONG, AllSounds.OIL_IGNITE)) {
            String path = event.getId().getPath();
            add.accept(event, SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + path)
                    .with(SoundDefinition.Sound.sound(ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, path.replace('.', '/')),
                            SoundDefinition.SoundType.SOUND).attenuationDistance(path.equals("word.banish_blast") ? 48 : 20)));
        }
    }
}
