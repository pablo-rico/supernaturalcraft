package org.papiricoh.supernaturalcraft.client.amara;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.darkness.DarknessRegistry;

/** The Darkness's arena (v0.42): the dome's colour and the score per phase, one cue each ({@code music.amara.phase<n>}). */
public final class AmaraArenaStyles {

    /** Smoke violet, a child's cold lilac, noir plum, the siblings' white-gold against violet, star-blue, ember. */
    private static final int[] COLORS = {0xFF6A3FA8, 0xFF9A7AD8, 0xFF7A2F7A, 0xFFE8D8B0, 0xFF8FA8FF, 0xFFFF5A2A};

    private AmaraArenaStyles() {
    }

    public static int color(int phase) {
        return COLORS[Mth.clamp(phase - 1, 0, COLORS.length - 1)];
    }

    public static SoundEvent music(int phase) {
        return DarknessRegistry.music(Mth.clamp(phase, 1, 6));
    }
}
