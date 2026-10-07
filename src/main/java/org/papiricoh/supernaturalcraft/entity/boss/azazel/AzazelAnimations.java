package org.papiricoh.supernaturalcraft.entity.boss.azazel;

import java.util.List;

/**
 * Azazel's clips by short name ("animation.azazel." + name). Must match tools/artgen/azazel_art.py;
 * AzazelAssetsTest checks the generated file.
 */
public final class AzazelAnimations {

    public static final List<String> TRIGGERED = List.of(
            "emerge", "death", "transform_2", "trapped",
            "shove", "hurl", "drag", "gaze", "fire", "possess", "smoke_dash", "blink");

    /** Loops the shared controllers ask for (the wing loops are empty: he has none). */
    public static final List<String> LOOPS = List.of("idle", "walk", "idle_fly", "wings_idle", "wings_fly");

    private AzazelAnimations() {
    }
}
