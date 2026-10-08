package org.papiricoh.supernaturalcraft.entity.boss;

/**
 * A great enemy whose blows are capped by {@link BossDamage} (v0.15). Every boss implements it; the final
 * clamp ({@link BossDamage#onFinalDamage}) and the health guard ({@link BossHealthGuard}) read it.
 */
public interface CappedBoss {

    /** True max health (vanilla max health times {@link #healthScale()}). */
    float trueMaxHealth();

    /** True health per point of vanilla health. */
    float healthScale();

    /**
     * Vanilla health no blow may take this boss below right now: the current phase's threshold, or 1 in the
     * last phase (where its own pipeline starts the death). 0 lets a blow kill it outright.
     */
    float vanillaFloor();

    /**
     * Its health has just been set by something trusted (its own code, a command, a test): the {@link BossHealthGuard}
     * takes it as it is instead of turning the change into a capped blow.
     */
    void acceptHealth();

    /** The most one exact blow (a Colt round) may take, in true health: {@link BossDamage#coltCap}. */
    default float exactCap() {
        return BossDamage.coltCap(trueMaxHealth(), false);
    }
}
