package org.papiricoh.supernaturalcraft.client.crowley;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Crowley's asylum's dome colour and music (v0.35): the crossroads' dusk red in the hall, his red smoke thickening in the cell
 * wing, the throne room's deep hellfire crimson. Music: his own, {@code music.crowley}, for the whole fight.
 */
public final class CrowleyArenaStyles {

    private static final int[] COLORS = {0x5A1418, 0x8A1A16, 0xB0201A};

    private CrowleyArenaStyles() {
    }

    public static int color(int phase) {
        return COLORS[Mth.clamp(phase - 1, 0, COLORS.length - 1)];
    }

    public static SoundEvent music() {
        return AllSounds.MUSIC_CROWLEY.get();
    }
}
