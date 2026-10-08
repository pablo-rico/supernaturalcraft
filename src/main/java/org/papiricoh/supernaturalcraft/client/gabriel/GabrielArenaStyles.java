package org.papiricoh.supernaturalcraft.client.gabriel;

import net.minecraft.sounds.SoundEvent;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.Channel;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/** TV Land's dome colour and music (v0.14): each channel its own jingle, looped while it is on the air. */
public final class GabrielArenaStyles {

    /** Dome colour per channel: the sitcom's warm living room, the game show's neon, the hospital's teal, the ad's white. */
    private static final int[] COLORS = {0xFFC27A, 0xFF4FD8, 0x5FE0C8, 0xFFFFFF};

    private GabrielArenaStyles() {
    }

    public static int color(int phase) {
        return COLORS[Channel.ofPhase(phase).ordinal()];
    }

    public static SoundEvent music(int phase) {
        return switch (Channel.ofPhase(phase)) {
            case SITCOM -> AllSounds.GABRIEL_JINGLE_SITCOM.get();
            case GAME_SHOW -> AllSounds.GABRIEL_JINGLE_GAME_SHOW.get();
            case HOSPITAL -> AllSounds.GABRIEL_JINGLE_HOSPITAL.get();
            case COMMERCIAL -> AllSounds.GABRIEL_JINGLE_COMMERCIAL.get();
        };
    }
}
