package org.papiricoh.supernaturalcraft.british.safehouse;

import java.util.List;

/**
 * The contract between Kelly Kline's safe house ({@link SafehousePlan}) and season 12's code (v0.41): the named points of
 * {@code SafehousePlan.plan(q).points()} Dagon's fight, the birth scene and the rematch rite rely on. Pure.
 *
 * <p>Plan space: the origin is the middle of the clearing in front of the cabin at ground level (y = 0, the ground block at
 * y = -1); the cabin's porch and front door face -z (north), onto the clearing. {@code plan(q)} rotates by q quarter turns.
 */
public final class SafehouseAnchors {

    private SafehouseAnchors() {
    }

    /** The land's top block (the clearing). */
    public static final int GROUND_Y = -1;
    /** The footprint the builder clears and footings fill (plan space, inclusive): the cabin, its porch and the clearing. */
    public static final int MIN_X = -20, MAX_X = 20, MIN_Z = -20, MAX_Z = 20, TOP_Y = 14, BOTTOM_Y = -4;
    /** The arena round {@link #ARENA_CENTRE}: the clearing and the cabin. */
    public static final int ARENA_RADIUS = 20;

    /** At the clearing's edge, where a hunter walks in. */
    public static final String ENTRY = "entry";
    /** Before the cabin's porch steps, where Dagon stands guard (≥ 4 clear ground round it). */
    public static final String GUARD = "guard";
    /** The middle of the arena's dome (the clearing). */
    public static final String ARENA_CENTRE = "arena_centre";
    /** A clear 9×9 of ground in the clearing where the rematch's great circle is drawn ({@code summon_dagon} answers near it). */
    public static final String RITE = "rite";
    /** Beside Kelly's cot in the nursery corner, where she lies for the birth (standable, inside). */
    public static final String COT = "cot";
    /** Inside the cabin, a few steps from the cot, where Lucifer stands to reach for the child (standable). */
    public static final String ROURKE = "rourke";
    /** Behind Lucifer, out in the clearing near the porch, where the rift to the other world tears open (≥ 3 clear up). */
    public static final String RIFT = "rift";
    /** The cabin's front doorway (inclusive box, air in the plan). */
    public static final String GATE_DOOR = "gate_door";

    public static String gateMin(String gate) {
        return gate + "_min";
    }

    public static String gateMax(String gate) {
        return gate + "_max";
    }

    /** Every point name the plan must carry. */
    public static List<String> required() {
        return List.of(ENTRY, GUARD, ARENA_CENTRE, RITE, COT, ROURKE, RIFT, gateMin(GATE_DOOR), gateMax(GATE_DOOR));
    }
}
