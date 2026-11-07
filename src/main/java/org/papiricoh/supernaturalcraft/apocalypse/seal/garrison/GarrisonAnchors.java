package org.papiricoh.supernaturalcraft.apocalypse.seal.garrison;

import java.util.ArrayList;
import java.util.List;

/**
 * The contract between Uriel's garrison ({@link GarrisonPlan}) and the code (v0.40): the named points of
 * {@code GarrisonPlan.plan(q).points()} the fight and Castiel rely on. Pure.
 *
 * <p>Plan space: the origin is the middle of the walled yard at floor level (y = 0, the floor block at y = -1); the gate is in
 * the north wall (-z), the chapel at the south end. {@code plan(q)} turns it q quarter turns.
 */
public final class GarrisonAnchors {

    private GarrisonAnchors() {
    }

    /** The land's top block. */
    public static final int GROUND_Y = -1;
    /** The footprint the builder clears and footings fill (plan space, inclusive). */
    public static final int MIN_X = -21, MAX_X = 21, MIN_Z = -23, MAX_Z = 21, TOP_Y = 12, BOTTOM_Y = -5;
    /** The arena round {@link #ARENA_CENTRE}: the whole yard. */
    public static final int ARENA_RADIUS = 24;
    /** A hunter this close to {@link #URIEL} at night wakes the fight. */
    public static final int WAKE_REACH = 14;

    /** Outside the gate, where a hunter walks up. */
    public static final String ENTRY = "entry";
    /** The middle of the yard (≥ 6 clear round it). */
    public static final String YARD = "yard";
    /** Where Uriel waits, before the chapel (≥ 5 clear round it). */
    public static final String URIEL = "uriel";
    /** Where Castiel comes at the end. */
    public static final String CASTIEL = "castiel";
    /** The middle of the arena's dome. */
    public static final String ARENA_CENTRE = "arena_centre";
    /** The rebels' spawn points: {@code spawn_<n>} (n from 0, at least {@link #MIN_SPAWNS}). */
    public static final String SPAWN_PREFIX = "spawn_";
    public static final int MIN_SPAWNS = 6;
    /** The gateway (inclusive box between {@code gate_min} and {@code gate_max}, air in the plan): barred through the fight. */
    public static final String GATE = "gate";

    public static String gateMin(String gate) {
        return gate + "_min";
    }

    public static String gateMax(String gate) {
        return gate + "_max";
    }

    public static String spawn(int n) {
        return SPAWN_PREFIX + n;
    }

    /** Every point name the plan must carry. */
    public static List<String> required() {
        List<String> out = new ArrayList<>(List.of(ENTRY, YARD, URIEL, CASTIEL, ARENA_CENTRE, gateMin(GATE), gateMax(GATE)));
        for (int n = 0; n < MIN_SPAWNS; n++) out.add(spawn(n));
        return out;
    }
}
