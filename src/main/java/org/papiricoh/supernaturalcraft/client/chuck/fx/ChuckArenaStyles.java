package org.papiricoh.supernaturalcraft.client.chuck.fx;

import net.minecraft.sounds.SoundEvent;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/** The dome's colour in each chapter of the Author's arena, and its music (called by {@code ArenaStyles}). */
public final class ChuckArenaStyles {

    // Eden gold, Hell red, the storm's white-blue, the library's parchment, the blank page.
    private static final int[] COLORS = {0xFFE59A, 0xC8261E, 0xCFE4FF, 0xF2E6C4, 0xFFFFFF};

    private ChuckArenaStyles() {
    }

    public static int color(int phase) {
        return COLORS[Math.max(0, Math.min(COLORS.length - 1, phase - 1))];
    }

    public static SoundEvent music() {
        return AllSounds.MUSIC_CHUCK.get();
    }
}
