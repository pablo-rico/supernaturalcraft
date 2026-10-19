package org.papiricoh.supernaturalcraft.entity.boss.rowena;

import java.util.List;

/**
 * Rowena's boss clips by short name ({@code animation.rowena.<name>}), as the code plays them. The full contract (lengths,
 * moments, bones) is {@link RowenaAssets}; a {@code RowenaAssetsTest} checks the generated files.
 */
public final class RowenaAnimations {

    /** Clips played once on the {@code action} controller ({@code death} holds). */
    public static final List<String> TRIGGERED = RowenaAssets.TRIGGERED;
    /** The {@code base} controller's loops: idle/walk, {@code levitate} in phase III, {@code recite} while she recites. */
    public static final List<String> LOOPS = RowenaAssets.LOOPS;
    /** The clips her moments land on, in ticks from their start ({@link RowenaAssets#HIT_TICKS}). */
    public static final java.util.Map<String, Integer> HIT_TICKS = RowenaAssets.HIT_TICKS;

    private RowenaAnimations() {
    }
}
