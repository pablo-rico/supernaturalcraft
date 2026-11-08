package org.papiricoh.supernaturalcraft.dyingsun;

/**
 * The dying sun's numbers (v0.42, season 11): pure. Once the first hunter of a world lets the Darkness out, the sun dies a little
 * every day for the whole server, without end, until someone brings Amara to peace; then it comes back for good. {@code d} is the
 * darkness 0..1 everywhere below. {@code DyingSunRulesTest} pins the shape.
 */
public final class DyingSunRules {

    private DyingSunRules() {
    }

    public static final byte NONE = 0, DYING = 1, RESTORED = 2;

    /** How dark the sun gets by itself: it nears this and never reaches it (only Amara's phase VI takes it to 1). */
    public static final float CEILING = 0.85f;
    /** Days for the darkness to cover half of {@link #CEILING}. */
    public static final float HALF_DAYS = 4f;
    /** The client hears of a change only every 1/STEPS of darkness. */
    public static final int STEPS = 64;
    /** Above this the undead stop burning and monsters may spawn in daylight. */
    public static final float UNDEAD_SPARED = 0.4f, DAY_SPAWNS = 0.5f;

    /** @return the darkness after {@code days} (fractional) since the sun began to die; 0 before */
    public static float darkness(double days) {
        if (days <= 0) return 0f;
        return (float) (CEILING * days / (days + HALF_DAYS));
    }

    /** Phase VI: from the world's {@code base} to 1 as the countdown runs from 1 to 0. */
    public static float fightDarkness(float base, float countdown) {
        float t = Math.max(0f, Math.min(1f, 1f - countdown));
        return base + (1f - base) * t;
    }

    /** Extra weight for monsters in the spawn lists, as a multiplier of their own (1 = none). */
    public static float spawnBonus(float d) {
        return 1f + 2f * d;
    }

    /** The chance a crop's growth tick is refused. */
    public static float cropDenyChance(float d) {
        return 0.6f * d;
    }

    public static boolean undeadSpared(float d) {
        return d >= UNDEAD_SPARED;
    }

    public static boolean daySpawns(float d) {
        return d >= DAY_SPAWNS;
    }

    /** The darkness rounded to the step the client is told of. */
    public static float quantise(float d) {
        return Math.round(Math.max(0f, Math.min(1f, d)) * STEPS) / (float) STEPS;
    }
}
