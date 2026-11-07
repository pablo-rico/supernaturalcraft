package org.papiricoh.supernaturalcraft.apocworld.camp;

import java.util.ArrayList;
import java.util.List;

/**
 * The contract between Michael's outposts ({@link CampPlan}: a prisoner camp or a road checkpoint) and the code that peoples them
 * (v0.41). Pure. Plan space: the origin is the outpost's middle on the ground (y = 0, the ground block at y = -1); its gate faces
 * -z (north). {@code plan(kind, q)} rotates by q quarter turns.
 */
public final class CampAnchors {

    private CampAnchors() {
    }

    public static final int GROUND_Y = -1;
    /** The footprint (plan space, inclusive, either kind). */
    public static final int MIN_X = -15, MAX_X = 15, MIN_Z = -16, MAX_Z = 15, TOP_Y = 9, BOTTOM_Y = -2;

    /** Outside the gate, where a hunter walks up. */
    public static final String ENTRY = "entry";
    /** Where its soldiers stand: {@code guard_<n>}, n from 0 ({@link #MIN_GUARDS}). */
    public static final String GUARD_PREFIX = "guard_";
    public static final int MIN_GUARDS = 3;
    /** A camp's cages: the prisoner inside ({@code prisoner_<n>}) and its lock ({@code lock_<n>}), n from 0 ({@link #CAGES}). */
    public static final String PRISONER_PREFIX = "prisoner_", LOCK_PREFIX = "lock_";
    public static final int CAGES = 3;

    public static String guard(int n) {
        return GUARD_PREFIX + n;
    }

    public static String prisoner(int n) {
        return PRISONER_PREFIX + n;
    }

    public static String lock(int n) {
        return LOCK_PREFIX + n;
    }

    /** Every point name a plan of {@code kind} must carry. */
    public static List<String> required(CampPlan.Kind kind) {
        List<String> out = new ArrayList<>(List.of(ENTRY));
        for (int n = 0; n < MIN_GUARDS; n++) out.add(guard(n));
        if (kind == CampPlan.Kind.CAMP) {
            for (int n = 0; n < CAGES; n++) {
                out.add(prisoner(n));
                out.add(lock(n));
            }
        }
        return out;
    }
}
