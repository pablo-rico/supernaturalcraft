package org.papiricoh.supernaturalcraft.apocalypse.blood;

import org.papiricoh.supernaturalcraft.allegiance.Faction;

/**
 * Demon blood's numbers (v0.40, pure, tested in JUnit by {@code BloodRulesTest}). Anyone may drink it; the powers are the
 * same for every side, the dependency is not.
 * <ul>
 *   <li><b>Blood in the body</b> (0-1): each dose adds {@link #DOSE_BLOOD}; it fades {@link #FADE_PER_MINUTE} a minute. The
 *   powers are paid in it ({@link #cost}).</li>
 *   <li><b>Tier</b> (0-3): the lifetime doses ({@link #TIER_DOSES}) unlock the push, then the exorcism, then the raised hand;
 *   a tier only works while there is blood in the body.</li>
 *   <li><b>Dependency</b> (0-1): each dose adds {@link #DOSE_DEPENDENCY} × the side's {@link #riseFactor} (a human ×1, a demon
 *   ×0.5, an angel ×2) and it fades only {@link #FADE_PER_DAY} a day: the Panic Room is the cure ({@link #PANIC_DAYS}).</li>
 *   <li><b>Withdrawal</b>: with the body dry and the dependency past {@link #WITHDRAWAL}. A human weakens, slows and sees
 *   things; a demon's Corruption drains; an angel's Grace is corrupted (it refills at half the pace). Demons and angels in
 *   withdrawal take {@link #holyTaken} from holy things.</li>
 * </ul>
 */
public final class BloodRules {

    /** Blood one dose puts in the body. */
    public static final float DOSE_BLOOD = 0.35f;
    /** Blood that fades each minute. */
    public static final float FADE_PER_MINUTE = 0.05f;
    /** Dependency one dose adds before the side's factor. */
    public static final float DOSE_DEPENDENCY = 0.12f;
    /** Dependency that fades each day on its own. */
    public static final float FADE_PER_DAY = 0.02f;
    /** Past this dependency, a dry body goes into withdrawal. */
    public static final float WITHDRAWAL = 0.3f;
    /** Past this, withdrawal is severe (a human's visions and nausea). */
    public static final float SEVERE = 0.6f;
    /** Lifetime doses that open tiers 1, 2 and 3. */
    public static final int[] TIER_DOSES = {1, 6, 15};
    /** Blood a power costs per point of its listed cost. */
    public static final float COST_PER_POINT = 0.01f;
    /** Share of a demon's maximum Corruption one dose refills. */
    public static final float DEMON_REFILL = 0.25f;
    /** Days locked in the Panic Room that cure the dependency (counted in day-time ticks: sleeping there counts). */
    public static final int PANIC_DAYS = 1;
    public static final long DAY_TICKS = 24000L;
    /** Boss blows of the raised hand against Alastair, Lilith and Ruby, over its usual share (Sam's own victories). */
    public static final float GREATER_DEMON_BONUS = 2f;

    private BloodRules() {
    }

    public static float clamp(float v) {
        return Float.isNaN(v) ? 0f : Math.max(0f, Math.min(1f, v));
    }

    /** How fast {@code side} gets hooked. */
    public static float riseFactor(Faction side) {
        return switch (side) {
            case DEMON -> 0.5f;
            case ANGEL -> 2f;
            default -> 1f;
        };
    }

    /** The data after one dose. */
    public static BloodData drink(BloodData d, Faction side) {
        return new BloodData(clamp(d.blood() + DOSE_BLOOD), clamp(d.dependency() + DOSE_DEPENDENCY * riseFactor(side)),
                d.doses() + 1, d.lastDay(), d.panicDoor(), d.panicUntil());
    }

    /** The power tier {@code doses} lifetime doses have reached (0-3). */
    public static int tier(int doses) {
        int t = 0;
        for (int need : TIER_DOSES) if (doses >= need) t++;
        return t;
    }

    /** The tier a drinker can use now: none with a dry body. */
    public static int activeTier(BloodData d) {
        return d.blood() > 0 ? tier(d.doses()) : 0;
    }

    /** Blood a power of listed cost {@code cost} takes. */
    public static float cost(int cost) {
        return cost * COST_PER_POINT;
    }

    /** Blood left after {@code minutes} of fading. */
    public static float fade(float blood, float minutes) {
        return clamp(blood - FADE_PER_MINUTE * minutes);
    }

    /** The dependency after {@code days} days of the slow fade. */
    public static float fadeDays(float dependency, long days) {
        return days <= 0 ? dependency : clamp(dependency - FADE_PER_DAY * days);
    }

    /** Whether the body is in withdrawal. */
    public static boolean withdrawal(BloodData d) {
        return d.blood() <= 0 && d.dependency() > WITHDRAWAL;
    }

    public static boolean severe(BloodData d) {
        return withdrawal(d) && d.dependency() > SEVERE;
    }

    /** The multiplier on holy damage a demon or angel in withdrawal takes. */
    public static float holyTaken(BloodData d, Faction side) {
        if (side == Faction.HUMAN || !withdrawal(d)) return 1f;
        return 1f + d.dependency() / 2f;
    }

    /**
     * A side's essence drain each second in withdrawal, as a share of its maximum: a demon's Corruption runs out over
     * ~five minutes at full dependency; an angel's Grace refills at half pace (half of the five-minute refill is drained).
     */
    public static float essenceDrainPerSecond(BloodData d, Faction side) {
        if (!withdrawal(d)) return 0f;
        return switch (side) {
            case DEMON -> d.dependency() / 300f;
            case ANGEL -> 0.5f / 300f;
            default -> 0f;
        };
    }
}
