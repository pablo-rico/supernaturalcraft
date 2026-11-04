package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.chain.ChainEvents;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;

/**
 * Who stands with Crowley (v0.35): himself, his companion form, his suits, the possessed patients, and the hellhounds and
 * crossroads demons he calls into the fight. His people and the Pit's (the chain's black-eyed demons) are rivals: they fight
 * on sight ({@link #rivals}).
 */
public final class CrowleyRetinue {

    private CrowleyRetinue() {
    }

    /** One of Crowley's own (never hurt by, nor targeted by, his people). */
    public static boolean ours(Entity e) {
        return e instanceof CrowleyEntity || e instanceof CrowleyCompanionEntity || e instanceof CrowleySuitEntity
                || e instanceof PossessedPatientEntity
                || e instanceof HellhoundEntity h && h.getPersistentData().getBoolean(CrowleyEntity.CALLED_TAG)
                || e instanceof CrossroadsDemonEntity d && d.getPersistentData().getBoolean(CrowleyEntity.CALLED_TAG);
    }

    /** A demon of the Pit (the chain's DEMON faction) his suits pick a fight with, and who picks one with them. */
    public static boolean rivals(LivingEntity e) {
        return !ours(e) && Kin.isDemon(e) && !(e instanceof net.minecraft.world.entity.player.Player)
                && ChainEvents.factionOf(e) == ChainFaction.DEMON;
    }
}
