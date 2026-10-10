package org.papiricoh.supernaturalcraft.entity.boss.naomi;

import org.papiricoh.supernaturalcraft.allegiance.Faction;

/**
 * The reprogramming chair's struggle (v0.18, pure): how many jump presses break the straps and how long a hunter has before
 * the drill comes down, how fast the server believes presses arrive, what a friend's blows and the drill do. The client mashes
 * jump and reports every few ticks ({@code ChairStrugglePayload}); {@link Budget} is what the server accepts of it.
 */
public final class ChairRules {

    /** Presses to break free in phase 1 and 2, and the ticks before the drill. */
    public static final int PRESSES_P1 = 18, PRESSES_P2 = 24, TICKS_P1 = 80, TICKS_P2 = 70;
    /** A human's will is their own (fewer presses); an angel was made to sit (more). */
    public static final float HUMAN = 0.75f, ANGEL = 1.25f;
    /** The most presses a second the server believes. */
    public static final int MAX_PER_SECOND = 12;
    /** The most credit the server holds for a late packet (half a second of presses). */
    public static final float MAX_CREDIT = MAX_PER_SECOND / 2f;
    /** Blows on the chair by a friend that cut the straps. */
    public static final int ALLY_HITS = 3;
    /** The drill, if it comes down: this share of the hunter's max health as Divine Wrath. */
    public static final float DRILL_SHARE = 0.06f;
    /** ...then the hunter is conditioned this long, and she heals this share of her true max. */
    public static final int FAIL_CONDITIONED = 160;
    public static final float FAIL_HEAL = 0.005f;

    private ChairRules() {
    }

    /** Presses needed in {@code phase} for a hunter of {@code faction}. */
    public static int presses(int phase, Faction faction) {
        int base = phase >= 2 ? PRESSES_P2 : PRESSES_P1;
        float mult = switch (faction) {
            case HUMAN -> HUMAN;
            case ANGEL -> ANGEL;
            case DEMON -> 1f;
        };
        return Math.max(1, Math.round(base * mult));
    }

    /** Ticks before the drill in {@code phase}. */
    public static int ticks(int phase) {
        return phase >= 2 ? TICKS_P2 : TICKS_P1;
    }

    /** Whether {@code presses} so far are enough. */
    public static boolean breaksFree(int presses, int needed) {
        return presses >= needed;
    }

    /** What the drill takes from a hunter of {@code maxHealth} ({@code demon}: it bites deeper). */
    public static float drillDamage(float maxHealth, boolean demon) {
        return maxHealth * DRILL_SHARE * (demon ? NaomiBalance.DEMON_DRILL : 1f);
    }

    /**
     * The presses the server accepts: credit fills at {@link #MAX_PER_SECOND} a second (up to {@link #MAX_CREDIT}) and each
     * report spends it. A hunter strapped in starts with none, so no window of time ever counts much more than the rate.
     */
    public static final class Budget {
        private float credit;
        private long last;

        public Budget(long now) {
            this.last = now;
        }

        /** Of {@code reported} presses at {@code now}, how many count. */
        public int accept(int reported, long now) {
            long elapsed = Math.max(0, now - last);
            last = Math.max(last, now);
            credit = Math.min(MAX_CREDIT, credit + elapsed * MAX_PER_SECOND / 20f);
            int ok = Math.max(0, Math.min(reported, (int) Math.floor(credit + 1e-4f)));
            credit -= ok;
            return ok;
        }
    }
}
