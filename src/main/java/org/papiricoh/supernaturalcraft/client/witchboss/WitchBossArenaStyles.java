package org.papiricoh.supernaturalcraft.client.witchboss;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * The witch bosses' dome colours and music (v0.21, P6).
 * <ul>
 *   <li>Rowena's house: emerald witch-fire (at home), the magenta of her hexes and her familiar's theft (the Familiar and the
 *   Hexed), the violet of her recitation.</li>
 *   <li>The Grand Coven: the silver-lilac of the coven's shield, the plum of the matriarch's hexes, the crimson of every taglock at
 *   once.</li>
 * </ul>
 * Music: each her own ({@code music.rowena}, {@code music.grand_witch}) for the whole fight.
 */
public final class WitchBossArenaStyles {

    private static final int[] ROWENA = {0x3FB868, 0xD03C8C, 0x9B4FD8};
    private static final int[] COVEN = {0xB8A4F0, 0x8A3AA8, 0xC8203A};

    private WitchBossArenaStyles() {
    }

    public static boolean handles(int theme) {
        return theme == ArenaTheme.ROWENA_HOUSE || theme == ArenaTheme.COVEN;
    }

    public static int color(int theme, int phase) {
        int[] row = theme == ArenaTheme.ROWENA_HOUSE ? ROWENA : COVEN;
        return row[Mth.clamp(phase - 1, 0, row.length - 1)];
    }

    public static SoundEvent music(int theme) {
        return theme == ArenaTheme.ROWENA_HOUSE ? AllSounds.MUSIC_ROWENA.get() : AllSounds.MUSIC_GRAND_WITCH.get();
    }
}
