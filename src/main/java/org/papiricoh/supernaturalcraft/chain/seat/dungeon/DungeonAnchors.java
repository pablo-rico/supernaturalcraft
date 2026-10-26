package org.papiricoh.supernaturalcraft.chain.seat.dungeon;

import java.util.List;

/**
 * The contract between the Chain's strongholds (the pure plans of {@code DungeonCatalog}: the Pit Castle of the demons, the
 * Monastery of the Host) and the code that builds and garrisons them ({@code SeatWorld}, {@code OfficerSpawner}). Pure.
 *
 * <p>A plan is centred on its origin (0,0,0 = the ground level at the site; +z is the gate side, the throne is to the north),
 * fits in {@link #RADIUS} around it, from {@code -DEPTH} to {@code +HEIGHT}, and holds no block entities. Every anchor is a
 * standable, headroomed feet cell reachable on foot from {@link #ENTRANCE}.
 */
public final class DungeonAnchors {

    private DungeonAnchors() {
    }

    public static final int RADIUS = 36, HEIGHT = 34, DEPTH = 14;

    /** The throne (the Lord stands before it). */
    public static final String LORD = "lord";
    /** Either side of the throne. */
    public static final List<String> COMMANDERS = List.of("commander_1", "commander_2");
    /** Outside the gate, on the approach (where the stronghold's title shows). */
    public static final String ENTRANCE = "entrance";
    /** Where {@code /supernatural chain seat … tp} sets a hunter down: just inside the gate, out of the guards' first line. */
    public static final String ARRIVAL = "arrival";
    /** The unnamed garrison: {@code guard_<room>_<n>}, one mob each; 24 to 40 per plan, every room has at least two. */
    public static final String GUARD_PREFIX = "guard_";
    /** Treasure chests in the vault (placed with a loot table after the plan is written): {@code vault_<n>}, 2 to 4. */
    public static final String VAULT_PREFIX = "vault_";
    /** The throne room's centre, for the boundary of the court (the Lord's guard does not leave it). */
    public static final String THRONE_ROOM = "throne_room";

    public static String guard(String room, int n) {
        return GUARD_PREFIX + room + "_" + n;
    }

    public static String vault(int n) {
        return VAULT_PREFIX + n;
    }

    /** The room a guard anchor belongs to, or null if the name is not a guard's. */
    public static String roomOf(String anchor) {
        if (!anchor.startsWith(GUARD_PREFIX)) return null;
        int last = anchor.lastIndexOf('_');
        return last <= GUARD_PREFIX.length() ? null : anchor.substring(GUARD_PREFIX.length(), last);
    }
}
