package org.papiricoh.supernaturalcraft.nephilim;

/**
 * Seasons 12-14 (v0.41), pure: where a hunter stands on the road from Amara to the Author. The steps only go forward and are
 * saved by ordinal, so they are only ever appended. The British choice and the Apocalypse World's records live in their own
 * slices' attachments ({@code british.British}, {@code apocworld.AUStanding}); this is the main road.
 */
public final class NephilimRules {

    private NephilimRules() {
    }

    public enum Step {
        /** Not yet on this road (Amara stands, or the British have not come). */
        NONE,
        /** Dr. Hess has come with the British ultimatum (whatever the answer). */
        ULTIMATUM,
        /** The nephilim's trail is in hand (the rite gave the compass to Kelly's safe house). */
        TRACING,
        /** Dagon fell, Kelly died and Jack was born: he walks with the hunter, and the first rift has opened. */
        BORN,
        /** The hunter has been to the Apocalypse World. */
        IN_AU,
        /** AU Michael was beaten at his fortress and fled through the rift into this world (Jack burned his grace). */
        MICHAEL_FLED,
        /** Michael has made his offer (yes or no: {@link Nephilim#answer()}). */
        OFFERED,
        /** Michael's vessel was beaten (thrown out of the hunter's head, or brought down in the open). */
        VESSEL_DOWN,
        /** Jack traded his soul away and left: he will come back without it. */
        SOULLESS,
        /** Soulless Jack fell, and went into the Empty. */
        JACK_FELL,
        /** The Shadow fell in the Empty: the road leads on to the Author. */
        DONE;

        public static Step of(int ordinal) {
            Step[] v = values();
            return ordinal >= 0 && ordinal < v.length ? v[ordinal] : NONE;
        }

        public boolean atLeast(Step s) {
            return ordinal() >= s.ordinal();
        }

        /** Whether Jack walks with the hunter as a companion at this step. */
        public boolean jackWalks() {
            return atLeast(BORN) && !atLeast(SOULLESS);
        }
    }

    /** Michael's offer: not yet asked, yes, no ({@link Nephilim#answer()}). Silence is no. */
    public static final int UNASKED = 0, YES = 1, NO = 2;

    /** Max mana a nephilim's grace adds, taken in (once per hunter). */
    public static final float GRACE_MANA = 25f;

    /** Days the hunter rests between one visit (Hess, Michael, Jack's leaving) and the next. */
    public static final int VISIT_REST_DAYS = 1;

    /** Whether a visitor whose last visit was on {@code lastVisitDay} may come on {@code today} (both in days). */
    public static boolean visitDue(long lastVisitDay, long today) {
        return today - lastVisitDay >= VISIT_REST_DAYS;
    }
}
