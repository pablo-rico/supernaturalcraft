package org.papiricoh.supernaturalcraft.hands.reliquary;

import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;

/**
 * Where each kind of reliquary may stand (v0.42), pure: judged from five ground heights (the middle and four points
 * {@link #SAMPLE} blocks off to the north, east, south and west), the sea level, and the structure's seed for its turn.
 * <ul>
 *   <li>The U-boat: deep water ({@link #UBOAT_DEPTH} over the floor at least), a fairly even floor.</li>
 *   <li>The bunker: high on a mountain ({@link #BUNKER_HEIGHT} over the sea), its door facing down the slope, into a side that
 *   rises at least {@link #BUNKER_SLOPE}.</li>
 *   <li>The church and the temple: dry, fairly level ground.</li>
 *   <li>The monastery: up in the snow, on a slope it can be terraced into (its terrace stands on footings).</li>
 * </ul>
 */
public final class ReliquarySites {

    public static final int SAMPLE = 14;
    public static final int UBOAT_DEPTH = 12, UBOAT_RELIEF = 6;
    public static final int BUNKER_HEIGHT = 30, BUNKER_SLOPE = 4;
    public static final int LEVEL_RELIEF = 4, TEMPLE_RELIEF = 5;
    public static final int MONASTERY_HEIGHT = 25, MONASTERY_RELIEF = 10;

    private ReliquarySites() {
    }

    /** A site: the plan's origin row (y = 0, the first open row) and its turn. */
    public record Site(int y, int q) {
    }

    /**
     * @param tops the ground's top block at the middle, north, east, south and west (ocean floor for the wreck)
     * @return the site, or null if the kind may not stand here
     */
    public static @Nullable Site site(S11Assets.Hand kind, int[] tops, int sea, long seed) {
        if (tops.length != 5) return null;
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int t : tops) {
            lo = Math.min(lo, t);
            hi = Math.max(hi, t);
        }
        int mid = tops[0], q = ReliquaryPlans.turnOf(seed);
        return switch (kind) {
            case DELPHINES_HAND -> mid <= sea - UBOAT_DEPTH && hi - lo <= UBOAT_RELIEF ? new Site(mid + 1, q) : null;
            case NAIL_OF_THE_CROSS -> {
                if (mid < sea + BUNKER_HEIGHT) yield null;
                // Into the side that rises most: south is the plan's +z unturned; west one turn, north two, east three.
                int best = 1;
                for (int i = 2; i <= 4; i++) if (tops[i] > tops[best]) best = i;
                if (tops[best] < mid + BUNKER_SLOPE) yield null;
                int turn = switch (best) {
                    case 3 -> 0;  // south
                    case 4 -> 1;  // west
                    case 1 -> 2;  // north
                    default -> 3; // east
                };
                yield new Site(mid + 1, turn);
            }
            case APOSTLES_CHALICE -> lo > sea && hi - lo <= LEVEL_RELIEF ? new Site(mid + 1, q) : null;
            case ROD_OF_AARON -> lo > sea && hi - lo <= TEMPLE_RELIEF ? new Site(mid + 1, q) : null;
            case TRUMPET_OF_JERICHO -> mid >= sea + MONASTERY_HEIGHT && hi - lo <= MONASTERY_RELIEF ? new Site(hi + 1, q) : null;
        };
    }
}
