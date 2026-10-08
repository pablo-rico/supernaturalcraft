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
            {0x8A0E14, 0xFF3B1F, 0x8FD8FF, 0x6A0F2A, 0xFFF3C4, 0xFFFFFF},   // the Abyss: chains, hellfire, cold, legion, star, light
            {0xE8C22E, 0xFF8A1E},   // Azazel: sulphur, then smoke and fire
            {0xD9DCE6, 0xBFD6FF, 0xFFFFFF},   // Lilith: pale, cold, then white
            {0xFFD978, 0xFFC94A, 0xF2E6C4, 0xFFFFFF},   // Metatron: gold, gold, parchment, the Tablet's white
    };

    private ArenaStyles() {
    }

    public static int color(int theme, int phase) {
        if (theme == ArenaTheme.AUTHOR) return org.papiricoh.supernaturalcraft.client.chuck.fx.ChuckArenaStyles.color(phase);
        if (theme == ArenaTheme.HEAVEN) return org.papiricoh.supernaturalcraft.client.michael.MichaelArenaStyles.color(phase);
        if (org.papiricoh.supernaturalcraft.client.horsemen.HorsemenArenaStyles.handles(theme)) {
            return org.papiricoh.supernaturalcraft.client.horsemen.HorsemenArenaStyles.color(theme, phase);
        }
        int[] row = COLORS[Mth.clamp(theme, 0, COLORS.length - 1)];
        return row[Mth.clamp(phase - 1, 0, row.length - 1)];
    }

    public static SoundEvent music(int theme) {
        return switch (theme) {
            case ArenaTheme.DARKNESS -> AllSounds.MUSIC_AMARA.get();
            case ArenaTheme.CHORUS -> AllSounds.MUSIC_CHORUS.get();
            case ArenaTheme.ABYSS -> AllSounds.MUSIC_UNCAGED.get();
            case ArenaTheme.SULFUR -> AllSounds.MUSIC_AZAZEL.get();
            case ArenaTheme.SEAL -> AllSounds.MUSIC_LILITH.get();
            case ArenaTheme.SCRIPTORIUM -> AllSounds.MUSIC_METATRON.get();
            case ArenaTheme.AUTHOR -> org.papiricoh.supernaturalcraft.client.chuck.fx.ChuckArenaStyles.music();
            case ArenaTheme.HEAVEN -> org.papiricoh.supernaturalcraft.client.michael.MichaelArenaStyles.music();
            case ArenaTheme.WAR, ArenaTheme.FAMINE, ArenaTheme.PLAGUE, ArenaTheme.DEATH ->
                    org.papiricoh.supernaturalcraft.client.horsemen.HorsemenArenaStyles.music(theme);
            default -> AllSounds.MUSIC_LUCIFER.get();
        };
    }
}
