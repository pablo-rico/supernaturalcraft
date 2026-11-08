package org.papiricoh.supernaturalcraft.hands.reliquary;

import org.papiricoh.supernaturalcraft.darkness.S11Assets;

import java.util.ArrayList;
import java.util.List;

/**
 * The contract between the five reliquaries' plans ({@link ReliquaryPlans}) and the code that builds and peoples them (v0.42).
 * Pure. Plan space: the origin is the reliquary's entrance on the ground (y = 0 the first open layer, the ground block at y = -1);
 * its way in faces -z (north) before the plan is turned by q quarter turns.
 */
public final class ReliquaryAnchors {

    private ReliquaryAnchors() {
    }

    /** Outside the way in, where a hunter walks (or swims) up. */
    public static final String ENTRY = "entry";
    /** The altar's cell: air in the plan, with a floor under it; the builder puts the altar (a block entity) there. */
    public static final String ALTAR = "altar";
    /** Before the altar, where the hunter stands to take the Hand. */
    public static final String KNEEL = "kneel";
    /** Loot chests ({@code chest_<n>}, n from 0): air in the plan, the builder puts the chest. */
    public static final String CHEST_PREFIX = "chest_";
    /** Where a reliquary's sleepers lie at generation ({@code guard_<n>}; only the Thule ones have them). */
    public static final String GUARD_PREFIX = "guard_";

    public static String chest(int n) {
        return CHEST_PREFIX + n;
    }

    public static String guard(int n) {
        return GUARD_PREFIX + n;
    }

    /** A plan's footprint (plan space, inclusive, before turning). */
    public record Bounds(int minX, int maxX, int minZ, int maxZ, int bottomY, int topY) {
        /** The farthest any cell lies from the origin on the ground plane, in any turn. */
        public int reach() {
            return Math.max(Math.max(-minX, maxX), Math.max(-minZ, maxZ));
        }
    }

    public static Bounds bounds(S11Assets.Hand kind) {
        return switch (kind) {
            case DELPHINES_HAND -> new Bounds(-22, 22, -11, 11, -2, 12);
            case NAIL_OF_THE_CROSS -> new Bounds(-11, 11, -12, 46, -12, 8);
            case APOSTLES_CHALICE -> new Bounds(-13, 13, -15, 17, -9, 16);
            case ROD_OF_AARON -> new Bounds(-12, 12, -20, 20, -10, 7);
            case TRUMPET_OF_JERICHO -> new Bounds(-15, 15, -17, 18, -2, 15);
        };
    }

    /** How many loot chests a kind has. */
    public static int chests(S11Assets.Hand kind) {
        return switch (kind) {
            case DELPHINES_HAND, ROD_OF_AARON, APOSTLES_CHALICE -> 2;
            case NAIL_OF_THE_CROSS -> 3;
            case TRUMPET_OF_JERICHO -> 2;
        };
    }

    /** Every point name a plan of {@code kind} must carry. */
    public static List<String> required(S11Assets.Hand kind) {
        List<String> out = new ArrayList<>(List.of(ENTRY, ALTAR, KNEEL));
        for (int n = 0; n < chests(kind); n++) out.add(chest(n));
        return out;
    }
}
