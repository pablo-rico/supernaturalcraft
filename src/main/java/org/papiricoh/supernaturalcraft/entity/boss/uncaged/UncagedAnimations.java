package org.papiricoh.supernaturalcraft.entity.boss.uncaged;

import java.util.List;

/**
 * Lucifer Uncaged's clips by short name ("animation.lucifer_uncaged." + name). Must match
 * tools/artgen/uncaged_art.py; LuciferUncagedAssetsTest checks the generated file.
 */
public final class UncagedAnimations {

    public static final List<String> TRIGGERED = List.of(
            "emerge", "death", "transform_2", "transform_3", "transform_4", "transform_5", "transform_6",
            "snap", "fling", "grasp", "summon", "wing_sweep", "rain", "fissure", "leap",
            "cage", "beam", "illusion", "collapse", "storm", "judgement", "smite_charge", "drain", "teleport",
            "chain_lash", "shackle_pull", "pillars", "hounds", "frozen_prison", "abyssal_pull", "falling_stars", "tempest", "supernova");

    public static final List<String> LOOPS = List.of("idle", "idle_fly", "walk", "wings_idle", "wings_fly", "chained");

    private UncagedAnimations() {
    }
}
