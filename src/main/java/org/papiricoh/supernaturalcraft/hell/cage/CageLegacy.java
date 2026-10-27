package org.papiricoh.supernaturalcraft.hell.cage;

import java.util.Set;

/**
 * What the Cage's first builder (before v0.30's plan) put into worlds that already have it, so a rebuild can clear it before
 * writing {@link CagePlan} (pure). It wrote only these blocks, all within {@link #OLD_REACH} of the origin horizontally and
 * between {@link #OLD_MIN_Y} and {@link #OLD_MAX_Y}: the island, bridges, Cage and chains out of the Cage's own blocks, and
 * the gates' walls out of hellstone bricks (only in the four gate boxes, {@link #oldGate}; elsewhere hellstone bricks are
 * Hell's own).
 */
public final class CageLegacy {

    private CageLegacy() {
    }

    public static final int OLD_REACH = 100, OLD_MIN_Y = 56, OLD_MAX_Y = 232;
    /** The old builder's gates: from this far out to {@link #OLD_GATE_END}, four blocks either side of the spoke, these heights. */
    public static final int OLD_GATE_START = 70, OLD_GATE_END = 94, OLD_GATE_HALF = 4, OLD_GATE_MIN_Y = 95, OLD_GATE_MAX_Y = 109;

    /** Every block id the old builder placed anywhere in its extent. */
    public static final Set<String> OLD_IDS = Set.of("supernaturalcraft:cage_bars", "supernaturalcraft:cage_frame",
            "supernaturalcraft:cage_seal", "supernaturalcraft:cage_chain", "supernaturalcraft:abyssal_flagstone",
            "supernaturalcraft:abyssal_bedrock", "supernaturalcraft:enochian_pillar", "supernaturalcraft:hellfire_brazier",
            "supernaturalcraft:cage_ritual_stone");
    public static final String OLD_GATE_BRICKS = "supernaturalcraft:hellstone_bricks";

    /** Whether (x, y, z) lies within the old structure's extent. */
    public static boolean inOldExtent(int x, int y, int z) {
        return Math.abs(x) <= OLD_REACH && Math.abs(z) <= OLD_REACH && y >= OLD_MIN_Y && y <= OLD_MAX_Y;
    }

    /** Whether (x, y, z) lies in one of the old gates' boxes (along an axis, out from the Pit's wall). */
    public static boolean oldGate(int x, int y, int z) {
        if (y < OLD_GATE_MIN_Y || y > OLD_GATE_MAX_Y) return false;
        int ax = Math.abs(x), az = Math.abs(z);
        return az <= OLD_GATE_HALF && ax >= OLD_GATE_START && ax <= OLD_GATE_END
                || ax <= OLD_GATE_HALF && az >= OLD_GATE_START && az <= OLD_GATE_END;
    }

    /** Whether a block with this id at (x, y, z) may be the old builder's (so a rebuild may erase it). */
    public static boolean erasable(String blockId, int x, int y, int z) {
        if (!inOldExtent(x, y, z)) return false;
        return OLD_IDS.contains(blockId) || OLD_GATE_BRICKS.equals(blockId) && oldGate(x, y, z);
    }
}
