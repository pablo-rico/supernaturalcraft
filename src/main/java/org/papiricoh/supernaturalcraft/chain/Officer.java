package org.papiricoh.supernaturalcraft.chain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One named officer of a chain of command (pure, mutable; {@link Hierarchy} owns and edits it). It is a record, not an entity:
 * the server materializes it as a mob when an event calls for it, and the record stays the source of truth.
 * <ul>
 *     <li>{@code superior}: id of the officer it answers to, or {@link #NONE} (the Lord, or an orphan waiting to be reassigned).</li>
 *     <li>{@code nameSeed}: {@link ChainNames} turns it into the officer's name.</li>
 *     <li>{@code kills}: players it has killed (UUID → count), shown in the book.</li>
 *     <li>{@code returnDay}: for {@link Status#RETURNING}, the {@code WorldClock.day} it comes back on.</li>
 * </ul>
 */
public final class Officer {

    public static final int NONE = -1;
    /** History lines kept per officer (oldest dropped). */
    public static final int HISTORY_MAX = 8;

    public enum Status {
        ALIVE,
        RETURNING,
        DEAD
    }

    public final int id;
    public final ChainFaction faction;
    public ChainFaction.Branch branch = ChainFaction.Branch.LOYAL;
    public ChainRank rank = ChainRank.SOLDIER;
    public int level;
    public int superior = NONE;
    public long nameSeed;
    /** The mob it was born from, as a registry id ("supernaturalcraft:black_eyed_demon"); kept as text so this stays pure. */
    public String entityType;
    /** The vessel / skin variant of that mob. */
    public int variant;
    public Status status = Status.ALIVE;
    public long returnDay;
    /** Times it came back from a death that was not done properly (each one leaves a scar on its look). */
    public int scars;
    public final Map<UUID, Integer> kills = new HashMap<>();
    /** Translation keys + args are built by the server; here each entry is an opaque line id ("promoted:12", "fell", …). */
    public final Deque<String> history = new ArrayDeque<>();

    public Officer(int id, ChainFaction faction) {
        this.id = id;
        this.faction = faction;
    }

    public boolean alive() {
        return status == Status.ALIVE;
    }

    public int killsOf(UUID player) {
        return kills.getOrDefault(player, 0);
    }

    public void recordKill(UUID player) {
        kills.merge(player, 1, Integer::sum);
    }

    public void log(String line) {
        history.addLast(line);
        while (history.size() > HISTORY_MAX) history.removeFirst();
    }

    /** The side it fights for: demons and fallen angels are on Hell's side. */
    public boolean hellSide() {
        return faction == ChainFaction.DEMON || branch == ChainFaction.Branch.FALLEN;
    }
}
