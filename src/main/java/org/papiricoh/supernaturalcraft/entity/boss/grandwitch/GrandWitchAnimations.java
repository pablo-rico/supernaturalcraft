package org.papiricoh.supernaturalcraft.entity.boss.grandwitch;

import java.util.List;

/**
 * The Grand Witch's clips by short name ({@code animation.grand_witch.<name>}), as the code plays them. The full contract
 * (lengths, moments, bones) is {@link GrandWitchAssets}; a {@code GrandWitchAssetsTest} checks the generated files.
 */
public final class GrandWitchAnimations {

    /** Clips played once on the {@code action} controller ({@code death} holds). */
    public static final List<String> TRIGGERED = GrandWitchAssets.TRIGGERED;
    /** The {@code base} controller's loops: idle/walk, and {@code shield} while her coven channels into her. */
    public static final List<String> LOOPS = GrandWitchAssets.LOOPS;
    public static final java.util.Map<String, Integer> HIT_TICKS = GrandWitchAssets.HIT_TICKS;
    /** The Coven Staff ({@code animation.coven_staff.<name>}): {@code idle} loops, {@code cast} plays on its spell. */
    public static final String STAFF_IDLE = "idle", STAFF_CAST = "cast";

    private GrandWitchAnimations() {
    }
}
