package org.papiricoh.supernaturalcraft.client.horsemen;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.client.horsemen.fx.ClientHorsemen;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/** The Horsemen's domes and music: war red to fire, harvest gold to blood, sickly green, and Death's pale grey. */
public final class HorsemenArenaStyles {

    private static final int[] WAR = {0xB3121A, 0xE0301E, 0xFF6A1E};
    private static final int[] FAMINE = {0xC9A45A, 0x9A7A3A, 0x8A1E14};
    private static final int[] PLAGUE = {0x9DB86A, 0x7FA03A, 0xC8D890};
    private static final int[] DEATH = {0xB8B8B0, 0x8A8A9A, 0x5A5A66, 0xEDEDE6};

    private HorsemenArenaStyles() {
    }

    public static boolean handles(int theme) {
        return theme >= ArenaTheme.WAR && theme <= ArenaTheme.DEATH;
    }

    public static int color(int theme, int phase) {
        int[] row = switch (theme) {
            case ArenaTheme.WAR -> WAR;
            case ArenaTheme.FAMINE -> FAMINE;
            case ArenaTheme.PLAGUE -> PLAGUE;
            default -> ClientHorsemen.deadWorld() ? new int[]{0x6E6E6E} : DEATH;
        };
        return row[Mth.clamp(phase - 1, 0, row.length - 1)];
    }

    public static SoundEvent music(int theme) {
        return switch (theme) {
            case ArenaTheme.WAR -> AllSounds.MUSIC_AZAZEL.get();
            case ArenaTheme.FAMINE -> AllSounds.MUSIC_LILITH.get();
            case ArenaTheme.PLAGUE -> AllSounds.MUSIC_AMARA.get();
            default -> AllSounds.MUSIC_UNCAGED.get();
        };
    }
}
