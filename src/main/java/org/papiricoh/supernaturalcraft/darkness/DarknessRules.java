package org.papiricoh.supernaturalcraft.darkness;

/**
 * Season 11's road per hunter (v0.42): pure, tested in JUnit ({@code DarknessRulesTest}). The Mark taken off lets the Darkness
 * out; Superior is found engulfed; the Cage is visited and Castiel says yes to Lucifer (Casifer); three Hands of God are taken;
 * Chuck shows himself at dawn and teaches the rite; Amara takes Casifer; the rite calls her, and at the end she and her brother
 * make peace. Amara visits at dawn between the milestones, growing up.
 */
public final class DarknessRules {

    private DarknessRules() {
    }

    /** Append only: saved by ordinal. */
    public enum Step {
        /** The Darkness is still locked away (for this hunter: they have not taken the Mark off). */
        NONE,
        /** The Mark came off and the Darkness got out (a black fog from the hunter, the scene). */
        RELEASED,
        /** The hunter reached Superior, the town the fog swallowed first. */
        SUPERIOR,
        /** The hunter stood before the Cage to ask Lucifer for help. */
        CAGE_VISITED,
        /** Castiel said yes: Lucifer rides him (Casifer walks with the hunter). */
        CASIFER,
        /** Three Hands held: Chuck came at dawn and showed who he is. */
        REVEALED,
        /** Amara took Casifer (the scene at dawn): the rite is all that is left. */
        TAKEN,
        /** Amara and Chuck made peace: the sun came back. */
        DONE;

        public static Step of(int ordinal) {
            Step[] v = values();
            return ordinal >= 0 && ordinal < v.length ? v[ordinal] : NONE;
        }

        public boolean atLeast(Step other) {
            return ordinal() >= other.ordinal();
        }
    }

    /** How many Hands Chuck waits for. */
    public static final int HANDS_FOR_CHUCK = S11Assets.HANDS_NEEDED;

    /** Days between two of Amara's visits, at least. */
    public static final int VISIT_REST_DAYS = 1;
    /** Days after the Darkness is out before Castiel comes to say the Cage is the only way left (the Cage visit opens then). */
    public static final int CAGE_AFTER_DAYS = 1;
    /** Days after Chuck's reveal before Amara takes Casifer. */
    public static final int TAKEN_AFTER_DAYS = 1;

    /** Bond limits: how the Darkness feels about the hunter (the Dean bond of the show). */
    public static final int BOND_MIN = -5, BOND_MAX = 5;

    /** How old she looks to a hunter at this point of the road (her visits). */
    public static org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets.Age ageFor(Step step, int hands) {
        if (!step.atLeast(Step.SUPERIOR)) return org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets.Age.BABY;
        if (!step.atLeast(Step.CASIFER) && hands == 0) return org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets.Age.GIRL;
        if (hands < 2 && !step.atLeast(Step.REVEALED)) return org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets.Age.TEEN;
        return org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets.Age.WOMAN;
    }

    /**
     * The visit milestone a hunter is at: one visit per milestone (the release, Superior, the Cage, each Hand, the reveal). A
     * visit is due at dawn when a milestone has not had its visit yet and the rest has passed.
     */
    public static int milestone(Step step, int hands) {
        return switch (step) {
            case NONE -> 0;
            case RELEASED -> 1;
            case SUPERIOR -> 2;
            case CAGE_VISITED, CASIFER -> 3 + Math.min(hands, HANDS_FOR_CHUCK);
            case REVEALED, TAKEN -> 3 + HANDS_FOR_CHUCK + 1;
            case DONE -> 0;
        };
    }

    /** Whether Amara visits at dawn: a milestone she has not visited ({@code visited} = the last one she did), after the rest. */
    public static boolean visitDue(Step step, int hands, int visited, long lastVisitDay, long day) {
        int m = milestone(step, hands);
        return m > 0 && m > visited && day - lastVisitDay >= VISIT_REST_DAYS;
    }

    /** Whether Castiel comes to send the hunter to the Cage (after Superior and the rest). */
    public static boolean cageDue(Step step, long sinceDay, long day) {
        return step == Step.SUPERIOR && day - sinceDay >= CAGE_AFTER_DAYS;
    }

    /** Whether Chuck comes at dawn: the Hands are held. */
    public static boolean revealDue(Step step, int handsHeld) {
        return step == Step.CASIFER && handsHeld >= HANDS_FOR_CHUCK;
    }

    /** Whether Amara takes Casifer at dawn. */
    public static boolean takenDue(Step step, long sinceDay, long day) {
        return step == Step.REVEALED && day - sinceDay >= TAKEN_AFTER_DAYS;
    }

    /** Whether the hunter may claim the Hands of God (the Darkness is out for them, and she is not yet at peace). */
    public static boolean huntsHands(Step step) {
        return step.atLeast(Step.RELEASED) && step != Step.DONE;
    }

    /** Whether the rite that calls Amara is open to the hunter. */
    public static boolean riteOpen(Step step) {
        return step.atLeast(Step.REVEALED);
    }

    public static int clampBond(int bond) {
        return Math.max(BOND_MIN, Math.min(BOND_MAX, bond));
    }

    /** Number of bits set in a claim mask. */
    public static int count(int mask) {
        return Integer.bitCount(mask & 0x1F);
    }
}
