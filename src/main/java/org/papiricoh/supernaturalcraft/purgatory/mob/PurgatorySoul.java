package org.papiricoh.supernaturalcraft.purgatory.mob;

import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveEntity;

/**
 * One of Purgatory's souls (v0.39): a monster that died and went on hunting here. Souls never turn on each other or on Eve, and
 * answer her call; everyone else is prey.
 */
public interface PurgatorySoul {

    /** Whether a soul would hunt {@code e} (any living thing but its own kind and the Mother). */
    static boolean prey(LivingEntity e) {
        return !(e instanceof PurgatorySoul) && !(e instanceof EveEntity) && e.isAlive()
                && !(e instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator()));
    }
}
