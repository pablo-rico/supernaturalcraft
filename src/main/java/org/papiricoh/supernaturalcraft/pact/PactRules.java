package org.papiricoh.supernaturalcraft.pact;

/**
 * Season 6's rules (v0.39), pure: where a hunter stands in Castiel's pact with the King of Hell, which Alphas they have brought
 * down, and when Castiel comes back. Tested in JUnit ({@code PactRulesTest}); {@link Pacts} applies them.
 *
 * <p>The road: Raphael falls → Castiel comes at dawn ({@link Step#CALLED}) → he sends the hunter to a crossroads, where Crowley
 * waits ({@link Step#MET_CROWLEY}) → Crowley wants the three Alphas ({@link Step#HUNTING}; they come at night, one at a time)
 * → all three down, Crowley raises his lab and Castiel's door is ready ({@link Step#RITE_READY}) → the door opens, Castiel
 * betrays Crowley and swallows Purgatory ({@link Step#BETRAYED}: the fight) → beaten, he kneels and the hunter forgives
 * ({@link Step#FORGIVEN}) or condemns him ({@link Step#CONDEMNED}).
 */
public final class PactRules {

    private PactRules() {
    }

    /** Where a hunter stands. Saved by ordinal: append only. */
    public enum Step {
        NONE, CALLED, MET_CROWLEY, HUNTING, RITE_READY, BETRAYED, FORGIVEN, CONDEMNED;

        public static Step of(int ordinal) {
            Step[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
        }

        /** Castiel has been beaten: the pact is over, one way or the other. */
        public boolean decided() {
            return this == FORGIVEN || this == CONDEMNED;
        }

        /** At or past {@code other} on the road (a decided pact is past every step). */
        public boolean atLeast(Step other) {
            return ordinal() >= other.ordinal();
        }
    }

    /** The three firsts Crowley wants. Saved as bits of a mask by ordinal: append only. */
    public enum Alpha {
        VAMPIRE, WEREWOLF, SHAPESHIFTER;

        public int bit() {
            return 1 << ordinal();
        }

        /** The entity path the purgatory package registers for this Alpha. */
        public String entity() {
            return "alpha_" + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Every Alpha down. */
    public static final int ALL_ALPHAS = (1 << Alpha.values().length) - 1;

    /** Days Castiel lets pass between two of his visits (he is busy with a war). */
    public static final int VISIT_REST_DAYS = 1;
    /** Days Crowley waits for a hunter Castiel sent him before he comes to them himself, at midnight (no crossroads called). */
    public static final int CROWLEY_PATIENCE_DAYS = 3;

    /** Whether Crowley, tired of waiting, comes to a hunter sent to him on {@code since}. */
    public static boolean crowleyImpatient(Step step, long day, long since) {
        return (step == Step.CALLED || step == Step.MET_CROWLEY) && day >= since + CROWLEY_PATIENCE_DAYS;
    }

    public static boolean has(int mask, Alpha a) {
        return (mask & a.bit()) != 0;
    }

    public static int with(int mask, Alpha a) {
        return mask | a.bit();
    }

    public static int count(int mask) {
        return Integer.bitCount(mask & ALL_ALPHAS);
    }

    /** Whether {@code a} may come for a hunter at {@code step} who has brought down {@code mask}. */
    public static boolean hunts(Step step, int mask, Alpha a) {
        return step == Step.HUNTING && !has(mask, a);
    }

    /** The step after an Alpha falls: {@link Step#RITE_READY} once all three are down, else unchanged. */
    public static Step afterAlpha(Step step, int mask) {
        return step == Step.HUNTING && (mask & ALL_ALPHAS) == ALL_ALPHAS ? Step.RITE_READY : step;
    }

    /** Whether Castiel may visit at dawn on {@code day} (he last came on {@code lastVisit}). */
    public static boolean visitDue(long day, long lastVisit) {
        return day >= lastVisit + VISIT_REST_DAYS;
    }
}
