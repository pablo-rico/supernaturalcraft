package org.papiricoh.supernaturalcraft.client.cain;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Cain's yard's dome colour and music (v0.21): the dusk amber of the Beekeeper's evening, the Mark's ember red as it wakes, the
 * blood red of the Father of Murder. Music: his own, {@code music.cain}, for the whole fight.
 */
public final class CainArenaStyles {

    private static final int[] COLORS = {0xE09A3C, 0xD0402A, 0x8E0E12};

    private CainArenaStyles() {
    }

    public static int color(int phase) {
        return COLORS[Mth.clamp(phase - 1, 0, COLORS.length - 1)];
    }

    public static SoundEvent music(int phase) {
        return AllSounds.MUSIC_CAIN.get();
    }

    public static SoundEvent music() {
        return music(1);
    }
}
