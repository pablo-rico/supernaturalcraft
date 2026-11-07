package org.papiricoh.supernaturalcraft.apocalypse.warehouse;

import java.util.List;

/**
 * The contract between the warehouse ({@link WarehousePlan}) and Alastair's fight (v0.40): the named points of
 * {@code WarehousePlan.plan(q).points()} the trap, the fight and the cinematics rely on. Pure.
 *
 * <p>Plan space: the origin is the centre of the warehouse floor (y = 0, the floor block at y = -1), under the devil's trap the
 * angels chained him in; the loading doors face -z (north) onto a gravel yard. {@code plan(q)} rotates by q quarter turns.
 */
public final class WarehouseAnchors {

    private WarehouseAnchors() {
    }

    /** The land's top block (the yard round the building). */
    public static final int GROUND_Y = -1;
    /** The footprint the builder clears and footings fill (plan space, inclusive): the shed and its yard. */
    public static final int MIN_X = -20, MAX_X = 20, MIN_Z = -24, MAX_Z = 16, TOP_Y = 14, BOTTOM_Y = -4;
    /** The arena round {@link #ARENA_CENTRE}: the whole shed. */
    public static final int ARENA_RADIUS = 22;

    /** Outside the loading doors, where a hunter walks up. */
    public static final String ENTRY = "entry";
    /** The middle of the devil's trap (painted on the floor by the builder, 3×3), where Alastair kneels chained. */
    public static final String TRAP = "trap";
    /** The middle of the arena's dome. */
    public static final String ARENA_CENTRE = "arena_centre";
    /** Where the angels who hold him stand: {@code angel_<n>}, n from 0 ({@link #ANGELS}). */
    public static final String ANGEL_PREFIX = "angel_";
    public static final int ANGELS = 4;
    /** Where his demons climb out in phase III: {@code spawn_<n>}, n from 0 ({@link #MIN_SPAWNS}). */
    public static final String SPAWN_PREFIX = "spawn_";
    public static final int MIN_SPAWNS = 4;
    /** The loading doors' doorway (inclusive box, air in the plan): barred by the fight. */
    public static final String GATE_DOORS = "gate_doors";

    public static String angel(int n) {
        return ANGEL_PREFIX + n;
    }

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
        List<String> out = new java.util.ArrayList<>(List.of(ENTRY, TRAP, ARENA_CENTRE, gateMin(GATE_DOORS), gateMax(GATE_DOORS)));
        for (int n = 0; n < ANGELS; n++) out.add(angel(n));
        for (int n = 0; n < MIN_SPAWNS; n++) out.add(spawn(n));
        return out;
    }
}
