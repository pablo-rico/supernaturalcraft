package org.papiricoh.supernaturalcraft.client.abaddon;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Abaddon's hall's dome colour and music (v0.21). The dome is drawn translucent over the world, so its colour reads as the air
 * of the hall: the Knight of Hell's smoke-black with an ember cast (a dark wall of smoke round the blackstone), Possession's
 * crimson, the Brand's hellfire. Music: her own, {@code music.abaddon}, for the whole fight.
 */
public final class AbaddonArenaStyles {

    private static final int[] COLORS = {0x3A1416, 0xA0142A, 0xFF5A1E};

    private AbaddonArenaStyles() {
    }

    public static int color(int phase) {
        return COLORS[Mth.clamp(phase - 1, 0, COLORS.length - 1)];
    }

    public static SoundEvent music() {
        return AllSounds.MUSIC_ABADDON.get();
    }
}
