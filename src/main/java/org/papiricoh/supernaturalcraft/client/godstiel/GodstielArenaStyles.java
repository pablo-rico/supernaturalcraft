package org.papiricoh.supernaturalcraft.client.godstiel;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Godstiel's dome colour and music (v0.39): soul-light white and blue for the new God, darkening through violet as the
 * Leviathans tear out of him, black over the reservoir. Music: his own, {@code music.godstiel}, for the whole fight.
 */
public final class GodstielArenaStyles {

    private static final int[] COLORS = {0xCFEFFF, 0x9FD8FF, 0x7A5AB8, 0x4A2A78, 0x1A0A28};

    private GodstielArenaStyles() {
    }

    public static int color(int phase) {
        return COLORS[Mth.clamp(phase - 1, 0, COLORS.length - 1)];
    }

    public static SoundEvent music() {
        return AllSounds.MUSIC_GODSTIEL.get();
    }
}
