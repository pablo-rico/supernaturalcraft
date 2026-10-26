package org.papiricoh.supernaturalcraft.client.leviathan;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Dick Roman's boardroom's dome colour and music (v0.25). The dome reads as the room's air: the cold white of a corner office,
 * brushed steel as the maw opens, a gunmetal with oil-blue in it while he hides among copies, and the black of the goo with its
 * oil-blue sheen in the Flood. Music: provisional, vanilla's Deep Dark track (there is no {@code music.dick_roman} yet).
 */
public final class DickArenaStyles {

    private static final int[] COLORS = {0xE4ECF2, 0x8C9AA8, 0x34465C, 0x0C1A2C};

    private DickArenaStyles() {
    }

    public static int color(int phase) {
        return COLORS[Mth.clamp(phase - 1, 0, COLORS.length - 1)];
    }

    public static SoundEvent music() {
        return SoundEvents.MUSIC_BIOME_DEEP_DARK.value();
    }
}
