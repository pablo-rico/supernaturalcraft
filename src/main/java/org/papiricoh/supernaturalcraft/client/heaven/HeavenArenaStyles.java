package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Heaven's arenas (v0.18): their dome colour per phase and their music.
 * <ul>
 *   <li>Naomi's reprogramming room: sterile cyan-white, then alarm red when she stops pretending (phase 2);
 *   {@code music.naomi}.</li>
 *   <li>Zachariah's office: fluorescent paper-white, carbon-copy blue under review, burnt gold when it is already written
 *   (his wings), and the golden sky of Final Judgment; {@code music.zachariah}.</li>
 *   <li>A staged memory: Heaven's warm white; Heaven's own music.</li>
 * </ul>
 */
public final class HeavenArenaStyles {

    private static final int[] REPROGRAMMING = {0xCFF4FA, 0xFF4E4E};
    private static final int[] OFFICE = {0xF4F1E6, 0xB9C8E8, 0xE0A94A, 0xFFD86B};
    private static final int[] MEMORY = {0xFFF4DA};

    private HeavenArenaStyles() {
    }

    public static boolean handles(int theme) {
        return theme == ArenaTheme.REPROGRAMMING || theme == ArenaTheme.OFFICE || theme == ArenaTheme.MEMORY;
    }

    public static int color(int theme, int phase) {
        int[] row = theme == ArenaTheme.REPROGRAMMING ? REPROGRAMMING : theme == ArenaTheme.OFFICE ? OFFICE : MEMORY;
        return row[Mth.clamp(phase - 1, 0, row.length - 1)];
    }

    public static SoundEvent music(int theme, int phase) {
        String event = theme == ArenaTheme.REPROGRAMMING ? "music.naomi" : theme == ArenaTheme.OFFICE ? "music.zachariah" : "music.heaven";
        return AllSounds.heaven(event);
    }
}
