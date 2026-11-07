package org.papiricoh.supernaturalcraft.british.compound;

import java.util.List;

/**
 * The contract between the British Men of Letters' compound ({@link CompoundPlan}) and season 12's code (v0.41): the named points
 * of {@code CompoundPlan.plan(q).points()} Hess's fight, her agents and the rematch rite rely on. Pure.
 *
 * <p>Plan space: the origin is the centre of the manor's war room at floor level (y = 0, the floor block at y = -1); the manor's
 * front door faces -z (north), onto a gravel drive behind iron railings. {@code plan(q)} rotates by q quarter turns.
 */
public final class CompoundAnchors {

    private CompoundAnchors() {
    }

    /** The land's top block (the lawn and the drive round the manor). */
    public static final int GROUND_Y = -1;
    /** The footprint the builder clears and footings fill (plan space, inclusive): the manor, its drive and its railings. */
    public static final int MIN_X = -24, MAX_X = 24, MIN_Z = -30, MAX_Z = 20, TOP_Y = 18, BOTTOM_Y = -4;
    /** The arena round {@link #ARENA_CENTRE}: the whole manor and its forecourt. */
    public static final int ARENA_RADIUS = 26;

    /** Outside the gate in the railings, where a hunter walks up the drive. */
    public static final String ENTRY = "entry";
    /** Where Hess stands at the head of the war room's table (≥ 5 clear floor round it). */
    public static final String HESS = "hess";
    /** The middle of the arena's dome. */
    public static final String ARENA_CENTRE = "arena_centre";
    /**
     * A clear 9×9 of floor in the war room or the forecourt where the rematch's great circle is drawn ({@code summon_hess} only
     * answers near it).
     */
    public static final String RITE = "rite";
    /** Where her agents stand guard and come in from: {@code agent_<n>}, n from 0 ({@link #AGENTS}). */
    public static final String AGENT_PREFIX = "agent_";
    public static final int AGENTS = 6;
    /** The manor's front doorway (inclusive box, air in the plan): barred by the fight. */
    public static final String GATE_FRONT = "gate_front";

    public static String agent(int n) {
        return AGENT_PREFIX + n;
    }

    public static String gateMin(String gate) {
        return gate + "_min";
    }

    public static String gateMax(String gate) {
        return gate + "_max";
    }

    /** Every point name the plan must carry. */
    public static List<String> required() {
        List<String> out = new java.util.ArrayList<>(List.of(ENTRY, HESS, ARENA_CENTRE, RITE, gateMin(GATE_FRONT), gateMax(GATE_FRONT)));
        for (int n = 0; n < AGENTS; n++) out.add(agent(n));
        return out;
    }
}
