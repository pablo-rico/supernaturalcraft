package org.papiricoh.supernaturalcraft.entity.boss.chorus;

import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;

/**
 * The numbers of the fight against the Broken Chorus, pure so they can be unit-tested.
 *
 * <p>Its health is the sum of what is left of its parts: four faces, six wings, twelve eyes and
 * the core, broken in that order. Every hit lands on a part and comes off the boss's health as
 * well, so the bar always tells the truth and no hit can skip a phase. The pools below are the
 * shape for {@link #REFERENCE_HEALTH}; the power curve's true health (v0.15) scales them all.
 */
public final class ChorusBalance {

    public static final float FACE_POOL = 100, WING_POOL = 70, EYE_POOL = 30;
    /** The health the pools above are drawn for (the core holds what the parts leave of it). */
    public static final float REFERENCE_HEALTH = 1600;
    /** Of the reference health, what the parts hold; the core has the rest. */
    public static final float BASE_PARTS = 4 * FACE_POOL + 6 * WING_POOL + 12 * EYE_POOL;
    public static final float HOLY = 1.25f, KNEELING = 1.5f;
    /** How directly a player must look at a charging eye to take its full judgment (cosine). */
    public static final double GAZE_DOT = 0.85;
    public static final float HYMN_DAMAGE = 12, HYMN_PER_ECHO = 0.15f;

    public enum Gaze { NONE, BURN, FULL }

    private ChorusBalance() {
    }

    /** Health scaled for the number of challengers. */
    public static float scaled(double base, double perExtra, int challengers) {
        return (float) (base * (1 + perExtra * (Math.max(1, challengers) - 1)));
    }

    /** The core's share of a true health (never less than a tenth of it). */
    public static float corePool(double baseHealth) {
        return (float) Math.max(baseHealth * 0.1, baseHealth - BASE_PARTS * partScale(baseHealth));
    }

    /** Scale for the part pools for a true health of {@code health} (1 at {@link #REFERENCE_HEALTH}). */
    public static float partScale(double health) {
        return (float) (health / REFERENCE_HEALTH);
    }

    /** What a blow on a part is multiplied by: holy, and a kneeling choir. */
    public static float boost(boolean holy, boolean kneeling) {
        return (holy ? HOLY : 1) * (kneeling ? KNEELING : 1);
    }

    /**
     * What a hit takes from a part (and from the boss) of a choir with {@code trueMax} true health: boosted, soft-capped
     * (the default curve; the game uses its config), never more than the part has.
     */
    public static float partDamage(float amount, boolean holy, boolean kneeling, float partHealth, float trueMax) {
        return partDamage(amount, holy, kneeling, false, partHealth, trueMax);
    }

    /** As above; exact damage (the Colt) skips the boosts and the soft cap up to the Colt's own cap, and empties only this part. */
    public static float partDamage(float amount, boolean holy, boolean kneeling, boolean exact, float partHealth, float trueMax) {
        float d = exact ? Math.min(amount, BossDamage.coltCap(trueMax, false, BossDamage.DEFAULT_COLT_SHARE, ProgressionScale.DEFAULT_HARD_CAP))
                : ProgressionScale.softCap(amount * boost(holy, kneeling), trueMax);
        return Math.max(0, Math.min(d, partHealth));
    }

    /**
     * An eye's judgment at the end of its charge: in its sight and looking into it, the full
     * blow; seen but looking away, a burn; out of its sight, nothing.
     */
    public static Gaze gaze(boolean lineOfSight, double lookDot) {
        if (!lineOfSight) return Gaze.NONE;
        return lookDot > GAZE_DOT ? Gaze.FULL : Gaze.BURN;
    }

    public static float hymnDamage(int echoes) {
        return HYMN_DAMAGE * (1 + HYMN_PER_ECHO * echoes);
    }
}
