package org.papiricoh.supernaturalcraft.asylum.layout;

import java.util.List;

/**
 * The contract between the asylum's blueprint ({@code AsylumPlan}, by ART) and Crowley's code (v0.35): the named points and
 * zones of {@code AsylumPlan.plan(q).points()/zones()} the fight, the case and the garrison rely on. Pure.
 *
 * <p>Plan space: origin at the building's centre on the ground floor's level (y = 0), the façade (portico, balcony, steep
 * front stairs) facing +z; basement below 0. Unrotated footprint about {@link #WIDTH} × {@link #DEPTH} plus the stairs.
 * {@code plan(q)} rotates by q quarter turns like {@code RowenaHouseLayout}.
 */
public final class AsylumAnchors {

    private AsylumAnchors() {
    }

    public static final int WIDTH = 50, DEPTH = 40;
    /**
     * The terrain pad's level in plan space: the ground floor stands this far above the land (the steep front stairs climb
     * it). Everything above the pad in the footprint is cleared before the plan is written; below it the plan must carry
     * explicit air cells for every interior space (the basement), so the earth is carved only where it should be.
     */
    public static final int GROUND_Y = -4;
    /** The footprint the builder clears and footings fill (plan space, inclusive): the building, its stairs and a margin. */
    public static final int MIN_X = -28, MAX_X = 28, MIN_Z = -24, MAX_Z = 34, TOP_Y = 22, BOTTOM_Y = -12;
    /** The arena's box around the plan's origin (half extents and vertical span): the whole building. */
    public static final int ARENA_HALF_X = 30, ARENA_HALF_Z = 32, ARENA_BELOW = 12, ARENA_ABOVE = 22;

    /** Foot of the front stairs (outside). */
    public static final String ENTRY = "entry";
    /** Inside the front door: a hunter with the case crossing it starts the fight. */
    public static final String HALL_THRESHOLD = "hall_threshold";
    /** Where Crowley stands in each phase's room (floor level, ≥ 9×9 clear around it). */
    public static final String HALL = "hall", CELLS = "cells", THRONE = "throne";
    /** The throne's seat (Crowley sits here; the rematch rite's altar faces it). */
    public static final String THRONE_SEAT = "throne_seat";
    /** The ritual altar spot of the rematch rite in the throne room (a free floor cell in front of the dais). */
    public static final String RITE = "rite";
    /** Minion spawn points: {@code spawn_hall_<n>}, {@code spawn_cells_<n>}, {@code spawn_throne_<n>} (n from 0, at least 4 each). */
    public static final String SPAWN_PREFIX = "spawn_";
    public static final List<String> ROOMS = List.of("hall", "cells", "throne");
    public static final int MIN_SPAWNS = 4;

    /**
     * Gates: doorways the fight fills with bars while closed and empties to open, each the inclusive box between the points
     * {@code <gate>_min} and {@code <gate>_max} (air in the plan). {@code gate_front} (the front door: shut for the whole
     * fight), {@code gate_cells} (hall → cell wing: opens at phase 2), {@code gate_throne} (to the basement stairs and the
     * throne room: opens at phase 3).
     */
    public static final String GATE_FRONT = "gate_front", GATE_CELLS = "gate_cells", GATE_THRONE = "gate_throne";
    public static final List<String> GATES = List.of(GATE_FRONT, GATE_CELLS, GATE_THRONE);

    public static String gateMin(String gate) {
        return gate + "_min";
    }

    public static String gateMax(String gate) {
        return gate + "_max";
    }

    /** Every point name the plan must carry. */
    public static List<String> required() {
        List<String> out = new java.util.ArrayList<>(List.of(ENTRY, HALL_THRESHOLD, HALL, CELLS, THRONE, THRONE_SEAT, RITE));
        for (String room : ROOMS) for (int n = 0; n < MIN_SPAWNS; n++) out.add(spawn(room, n));
        for (String g : GATES) {
            out.add(gateMin(g));
            out.add(gateMax(g));
        }
        return out;
    }

    public static String spawn(String room, int n) {
        return SPAWN_PREFIX + room + "_" + n;
    }
}
