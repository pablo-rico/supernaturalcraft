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
 *     <li>{@code seen*}: where and when it was last in the world (materialized, or killing a hunter); {@code seenDay} -1 if never.</li>
 *     <li>{@code player}: set when the post is held by a hunter of the side (v0.27). Such a record is never materialized, and
 *     {@code entityType} is {@link #PLAYER_TYPE}.</li>
 *     <li>{@code met}: how many times each hunter has crossed its path (so it can say it remembers them).</li>
 * </ul>
 */
public final class Officer {

    public static final int NONE = -1;
    /** History lines kept per officer (oldest dropped). */
    public static final int HISTORY_MAX = 8;
    /** The {@link #entityType} of a post held by a hunter. */
    public static final String PLAYER_TYPE = "minecraft:player";

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
    public String seenDimension = "";
    public int seenX, seenZ;
    public long seenDay = -1;
    public final Map<UUID, Integer> kills = new HashMap<>();
    public final Map<UUID, Integer> met = new HashMap<>();
    /** The hunter holding this post, or null for an officer of flesh (or ichor). */
    public UUID player;
    public String playerName = "";
    /** Translation keys + args are built by the server; here each entry is an opaque line id ("promoted:12", "fell", …). */
    public final Deque<String> history = new ArrayDeque<>();

    public Officer(int id, ChainFaction faction) {
        this.id = id;
        this.faction = faction;
    }

    /** The boss who first sits on the Leviathans' throne (v0.28): never drawn by the dice, met only in his penthouse. */
    public static final String BOSS_TYPE = "supernaturalcraft:dick_roman";

    /**
     * Whether the post is out of the dice's and the stages' hands: a hunter holds it ({@link #hunter()}) or a boss sits in it
     * ({@link #boss()}). Neither is ever materialized by the spawner nor moved by a conclave or a feud.
     */
    public boolean held() {
        return player != null || boss();
    }

    /** Whether a hunter holds this post. */
    public boolean hunter() {
        return player != null;
    }

    /** Whether a boss sits in this post (the Leviathans' first Lord). */
    public boolean boss() {
        return BOSS_TYPE.equals(entityType);
    }

    public int metWith(UUID player) {
        return met.getOrDefault(player, 0);
    }

    public void meet(UUID player) {
        met.merge(player, 1, Integer::sum);
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

    public void sighted(String dimension, int x, int z, long day) {
        seenDimension = dimension;
        seenX = x;
        seenZ = z;
        seenDay = day;
    }

    public void log(String line) {
        history.addLast(line);
        while (history.size() > HISTORY_MAX) history.removeFirst();
    }

    /** The side it fights for: demons and fallen angels are on Hell's side. */
    public boolean hellSide() {
        return camp() == ChainFaction.DEMON;
    }

    /** The faction it fights for on the front and in a war: its own, but a fallen angel fights for the demons. */
    public ChainFaction camp() {
        return branch == ChainFaction.Branch.FALLEN ? ChainFaction.DEMON : faction;
    }
}
