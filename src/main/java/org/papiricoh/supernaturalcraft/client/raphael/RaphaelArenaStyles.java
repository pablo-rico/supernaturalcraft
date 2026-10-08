package org.papiricoh.supernaturalcraft.client.raphael;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * The storm's dome colour and music (v0.16): slate storm-blue in the house, the warm gold of grace while his garrison heals
 * him, the white-blue of lightning in the wrath of Heaven. Music: provisional (the Broken Chorus's, a storm too).
 */
public final class RaphaelArenaStyles {

    private static final int[] COLORS = {0x7A8FB8, 0xFFE3A5, 0xC9E4FF};

    private RaphaelArenaStyles() {
    }

    public static int color(int phase) {
        return COLORS[Mth.clamp(phase - 1, 0, COLORS.length - 1)];
    }

    public static SoundEvent music() {
        return AllSounds.MUSIC_CHORUS.get();
    }
}
