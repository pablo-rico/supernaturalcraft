package org.papiricoh.supernaturalcraft.entity.boss.lucifer;

import java.util.List;

/**
 * One-shot animations the server triggers, by short name ("animation.lucifer." + name). Must
 * match tools/artgen/lucifer_art.py; LuciferAnimationsTest checks the generated file.
 */
public final class LuciferAnimations {

    public static final List<String> TRIGGERED = List.of(
            "emerge", "death", "transform_2", "transform_3", "transform_4",
            "snap", "fling", "grasp", "summon", "wing_sweep", "rain", "fissure", "leap",
            "cage", "beam", "illusion", "collapse", "storm", "judgement", "smite_charge", "drain", "teleport");

    public static final List<String> LOOPS = List.of("idle", "idle_fly", "walk", "wings_idle", "wings_fly");

    private LuciferAnimations() {
    }
}
