package org.papiricoh.supernaturalcraft.pact.layout;

import java.util.List;

/**
 * The contract between Crowley's lab ({@link LabPlan}) and season 6's code (v0.39): the named points of
 * {@code LabPlan.plan(q).points()} the rite, the fight and the cinematics rely on. Pure.
 *
 * <p>Plan space: the origin is the centre of the pump house's hall at floor level (y = 0, the floor block at y = -1); the hall's
 * big doors face +z, onto a concrete apron and the reservoir beyond it. {@code plan(q)} rotates by q quarter turns.
 */
public final class LabAnchors {

    private LabAnchors() {
    }

    /** The land's top block (y of the grass round the building and the reservoir's rim). */
    public static final int GROUND_Y = -1;
    /** The footprint the builder clears and footings fill (plan space, inclusive): the house, the apron, the reservoir. */
    public static final int MIN_X = -20, MAX_X = 20, MIN_Z = -16, MAX_Z = 44, TOP_Y = 14, BOTTOM_Y = -7;
    /** The arena: centred on {@link #ARENA_CENTRE}, this radius covers the hall and the reservoir's deck. */
    public static final int ARENA_RADIUS = 32;

    /** Outside the front door (the north side), where a hunter walks up. */
    public static final String ENTRY = "entry";
    /** The rite's altar: the hall's centre, a 9×9 of clear floor round it (the great circle). */
    public static final String RITE = "rite";
    /** Where Godstiel stands in phases I–IV (the far end of the hall, ≥ 6 clear round it). */
    public static final String HALL = "hall";
    /** Where the door to Purgatory opens in the air above the rite. */
    public static final String DOOR = "door";
    /** Where Crowley stands while the rite is worked (by the cages). */
    public static final String CROWLEY = "crowley";
    /** The reservoir's deck, out over the water: phase V (≥ 6 clear round it). */
    public static final String DECK = "deck";
    /** The middle of the arena's dome. */
    public static final String ARENA_CENTRE = "arena_centre";
    /** Minion spawn points: {@code spawn_hall_<n>} and {@code spawn_deck_<n>} (n from 0, at least 4 each). */
    public static final String SPAWN_PREFIX = "spawn_";
    public static final List<String> ROOMS = List.of("hall", "deck");
    public static final int MIN_SPAWNS = 4;
    /**
     * The hall's big doorway onto the apron, the inclusive box between {@code gate_doors_min} and {@code gate_doors_max} (air in
     * the plan): barred through phases I–IV, opened for the move to the reservoir.
     */
    public static final String GATE_DOORS = "gate_doors";
    /** The box of the reservoir's water (inclusive, plan space): what turns black in phase V. */
    public static final String WATER_MIN = "water_min", WATER_MAX = "water_max";

    public static String gateMin(String gate) {
        return gate + "_min";
    }

    public static String gateMax(String gate) {
        return gate + "_max";
    }

    public static String spawn(String room, int n) {
        return SPAWN_PREFIX + room + "_" + n;
    }

    /** Every point name the plan must carry. */
    public static List<String> required() {
        List<String> out = new java.util.ArrayList<>(List.of(ENTRY, RITE, HALL, DOOR, CROWLEY, DECK, ARENA_CENTRE, WATER_MIN, WATER_MAX,
                gateMin(GATE_DOORS), gateMax(GATE_DOORS)));
        for (String room : ROOMS) for (int n = 0; n < MIN_SPAWNS; n++) out.add(spawn(room, n));
        return out;
    }
}
