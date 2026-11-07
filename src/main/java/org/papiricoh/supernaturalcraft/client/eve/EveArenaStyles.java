package org.papiricoh.supernaturalcraft.client.eve;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Eve's glade's dome colour and music (v0.39). The dome reads as the woods closing in: the glade's moss-green dusk, the roaches'
 * brown in the Swarm, a bruised violet with her call, and the amber of her crown in the Mother of All. Music: her own event,
 * {@code music.eve} (provisional: a vanilla track re-pitched, {@code PurgatorySoundData}).
 */
public final class EveArenaStyles {

    private static final int[] COLORS = {0x3E5A2A, 0x5A3E22, 0x5A3A7A, 0xC8821E};

    private EveArenaStyles() {
    }

    public static int color(int phase) {
        return COLORS[Mth.clamp(phase - 1, 0, COLORS.length - 1)];
    }

    public static SoundEvent music() {
        return AllSounds.MUSIC_EVE.get();
    }
}
