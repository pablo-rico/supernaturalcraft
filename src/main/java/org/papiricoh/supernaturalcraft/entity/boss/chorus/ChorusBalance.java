package org.papiricoh.supernaturalcraft.entity.boss.chorus;

/**
 * The numbers of the fight against the Broken Chorus, pure so they can be unit-tested.
 *
 * <p>Its health is the sum of what is left of its parts: four faces, six wings, twelve eyes and
 * the core, broken in that order. Every hit lands on a part and comes off the boss's health as
 * well, so the bar always tells the truth and no hit can skip a phase.
 */
public final class ChorusBalance {

    public static final float FACE_POOL = 100, WING_POOL = 70, EYE_POOL = 30;
    /** Of the base health, what is left for the core after the parts. */
    public static final float BASE_PARTS = 4 * FACE_POOL + 6 * WING_POOL + 12 * EYE_POOL;
    public static final float HIT_CAP = 40;
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

    /** The core's share of a base health (never less than a tenth of it). */
    public static float corePool(double baseHealth) {
        return (float) Math.max(baseHealth * 0.1, baseHealth - BASE_PARTS);
    }

    /** Scale for the part pools when the configured health differs from the default 1600. */
    public static float partScale(double baseHealth) {
        return (float) Math.min(1.0, baseHealth * 0.9 / BASE_PARTS);
    }

    /** What a hit takes from a part (and from the boss): boosted, capped, never more than the part has. */
    public static float partDamage(float amount, boolean holy, boolean kneeling, float partHealth) {
        return partDamage(amount, holy, kneeling, false, partHealth);
    }

    /** As above; exact damage (the Colt) skips the boosts and the cap but still empties only this part. */
    public static float partDamage(float amount, boolean holy, boolean kneeling, boolean exact, float partHealth) {
        float d = exact ? amount : Math.min(amount * (holy ? HOLY : 1) * (kneeling ? KNEELING : 1), HIT_CAP);
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
