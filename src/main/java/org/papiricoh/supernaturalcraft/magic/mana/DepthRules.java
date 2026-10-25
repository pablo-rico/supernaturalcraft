package org.papiricoh.supernaturalcraft.magic.mana;

/**
 * The rites of the well (pure): the Rite of Deepening widens the mana, the Rite of the Wellspring quickens it, as often as a
 * hunter can bear. Each kind counts on its own: its first five rites take plain offerings, the next five twice as many, and
 * from the eleventh on twice that again and a great enemy's spoil as the activator. After each rite that kind rests 1, 2, 4,
 * then 8 days (never more). Every tenth rite of either kind is a milestone with its own title.
 */
public final class DepthRules {

    private DepthRules() {
    }

    /** Max mana a Rite of Deepening adds. */
    public static final int MANA_PER_DEEPENING = 50;
    /** Share of the max mana restored each second, before any Wellspring, and what each Wellspring adds. */
    public static final float BASE_REGEN_SHARE = 0.02f, REGEN_SHARE_PER_WELLSPRING = 0.01f;
    /** Rites of one kind in each of the first two tiers. */
    public static final int RITES_PER_TIER = 5;
    /** The longest rest between two rites of one kind, in days. */
    public static final int MAX_REST_DAYS = 8;
    /** Rites (of both kinds) between two milestones, and how many milestones have a title of their own. */
    public static final int MILESTONE_EVERY = 10, TITLES = 5;
    public static final long TICKS_PER_DAY = 24000L;

    public enum Kind {
        DEEPENING, WELLSPRING;

        public String id() {
            return this == DEEPENING ? "deepening" : "wellspring";
        }
    }

    /** The tier (1–3) the next rite takes, after {@code done} rites of that kind. */
    public static int tierFor(int done) {
        return Math.min(3, 1 + Math.max(0, done) / RITES_PER_TIER);
    }

    /** Days a kind rests after its {@code done}-th rite (1, 2, 4, then 8). */
    public static int restDays(int done) {
        if (done <= 0) return 0;
        return Math.min(MAX_REST_DAYS, 1 << Math.min(done - 1, 30));
    }

    /** Mana restored per second by a bar of {@code maxMana} after {@code wellsprings} rites. */
    public static float regenPerSecond(float maxMana, int wellsprings) {
        return maxMana * (BASE_REGEN_SHARE + REGEN_SHARE_PER_WELLSPRING * Math.max(0, wellsprings));
    }

    /** Whether a rite that brings the total (both kinds) to {@code total} is a milestone. */
    public static boolean milestone(int total) {
        return total > 0 && total % MILESTONE_EVERY == 0;
    }

    /** The title (0 = none, 1–{@link #TITLES}) {@code total} rites have earned; past the last it stays the last. */
    public static int title(int total) {
        return Math.min(TITLES, Math.max(0, total) / MILESTONE_EVERY);
    }
}
