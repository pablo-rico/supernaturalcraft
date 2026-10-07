package org.papiricoh.supernaturalcraft.entity.boss.chorus;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** The Hymn: the chant only the Choir Bells can break. (Its attack is in {@link ChorusAttacks}.) */
public final class Hymn {

    private Hymn() {
    }

    /** A bell rang while the Chorus may be singing. */
    static void onBell(ChorusEntity boss, int note, @Nullable Player ringer) {
        if (boss.scheduler().current() instanceof ChorusAttacks.TheHymn hymn) hymn.bell(boss, note, ringer);
    }
}
