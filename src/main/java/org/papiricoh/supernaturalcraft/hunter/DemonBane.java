package org.papiricoh.supernaturalcraft.hunter;

/** A weapon that hits demons harder. The multiplier is applied in {@link CombatEvents}. */
public interface DemonBane {

    float demonDamageMultiplier();

    /** Whether demons slain by this weapon always drop their blood. */
    default boolean harvestsBlood() {
        return false;
    }
}
