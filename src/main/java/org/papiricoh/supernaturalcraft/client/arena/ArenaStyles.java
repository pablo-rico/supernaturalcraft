package org.papiricoh.supernaturalcraft.client.arena;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/** How each kind of arena looks and sounds: dome colour per phase and the fight music. */
public final class ArenaStyles {

    private static final int[][] COLORS = {
            {0xFF3B1F, 0xFF7A2A, 0x8FD8FF, 0xFFF3C4},   // the Cage: hellfire, embers, ice, grace
            {0x6A3FA8, 0x8A2F9A, 0xB48CFF, 0x2A1240},   // the Darkness
            {0xFFE7A0, 0xFFC45A, 0xFF8A3A, 0xFF4A2A},   // the Chorus: gold cracking to red
    };

    private ArenaStyles() {
    }

    public static int color(int theme, int phase) {
        int[] row = COLORS[Mth.clamp(theme, 0, COLORS.length - 1)];
        return row[Mth.clamp(phase - 1, 0, row.length - 1)];
    }

    public static SoundEvent music(int theme) {
        return switch (theme) {
            case ArenaTheme.DARKNESS -> AllSounds.MUSIC_AMARA.get();
            case ArenaTheme.CHORUS -> AllSounds.MUSIC_CHORUS.get();
            default -> AllSounds.MUSIC_LUCIFER.get();
        };
    }
}
