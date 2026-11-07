package org.papiricoh.supernaturalcraft.apocworld.fortress;

import java.util.List;

/**
 * The contract between Michael's fortress ({@link FortressPlan}) and his fight (v0.41): the named points of
 * {@code FortressPlan.plan(q).points()} the fight, the rift and the cinematics rely on. Pure.
 *
 * <p>Plan space: the origin is the middle of the fortress's open court (y = 0, its floor block at y = -1). The gate faces -z
 * (north) onto the approach road; his throne stands on a dais at the court's south end, under the keep's black spire.
 * {@code plan(q)} rotates by q quarter turns.
 */
public final class FortressAnchors {

    private FortressAnchors() {
    }

    /** The court's floor level. */
    public static final int GROUND_Y = -1;
    /** The footprint the builder clears and footings fill (plan space, inclusive). */
    public static final int MIN_X = -34, MAX_X = 34, MIN_Z = -44, MAX_Z = 36, TOP_Y = 46, BOTTOM_Y = -6;

    /** On the approach road, where a hunter walks up. */
    public static final String ENTRY = "entry";
    /** Where he stands before his throne (the fight begins when a hunter comes close). */
    public static final String THRONE = "throne";
    /** The middle of the arena's dome. */
    public static final String ARENA_CENTRE = "arena_centre";
    /** Where the rift he flees through opens (an alcove in the keep behind his throne). */
    public static final String RIFT = "rift";
    /** Where his soldiers come in: {@code spawn_<n>}, n from 0 ({@link #MIN_SPAWNS}). */
    public static final String SPAWN_PREFIX = "spawn_";
    public static final int MIN_SPAWNS = 4;
    /** The gate's opening (inclusive box, air in the plan): barred by the fight. */
    public static final String GATE = "gate";

    public static String spawn(int n) {
        return SPAWN_PREFIX + n;
    }

    public static String gateMin(String gate) {
        return gate + "_min";
    }

    public static String gateMax(String gate) {
        return gate + "_max";
    }

    /** Every point name the plan must carry. */
    public static List<String> required() {
        List<String> out = new java.util.ArrayList<>(List.of(ENTRY, THRONE, ARENA_CENTRE, RIFT, gateMin(GATE), gateMax(GATE)));
        for (int n = 0; n < MIN_SPAWNS; n++) out.add(spawn(n));
        return out;
    }
}
