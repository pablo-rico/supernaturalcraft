package org.papiricoh.supernaturalcraft.chain.narrative;

import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.List;

/**
 * The book's chronicle (pure): every {@code ChainSavedData.History} key has {@link #VARIANTS} lines of prose,
 * {@code chain.supernaturalcraft.chronicle.<key>.<n>}, picked by the day and the officer so a line reads the same each time
 * the book opens. Arguments: {@code %1$s} the officer the entry is about, {@code %2$s} the other party (an officer or a hunter;
 * empty if none), {@code %3$s} a detail (a rank, a sector of the front, a number of days; empty if none).
 */
public final class Chronicle {
    public static final int VARIANTS = 3;
    public static final String PREFIX = "chain.supernaturalcraft.chronicle.";

    /** Every history key the server logs. Append only. */
    public static final List<String> KEYS = List.of(
            // v0.25–0.26
            "founded", "kill", "slain", "defeated", "revive", "power_struggle", "recruit", "promote", "betrayal", "fall",
            "skirmish", "vacant", "crowned", "feud", "feud_usurp", "feud_death",
            // v0.27: bonds
            "rivalry", "grudge", "pact", "pact_broken", "insult",
            // duels and challenges
            "challenge", "duel_won", "duel_yield", "duel_death", "challenge_hunter", "coward", "hunter_duel_won",
            "hunter_duel_lost", "throne_duel",
            // between the sides
            "skirmish_seen", "vow", "champion_won", "sector_taken", "war_declared", "war_won",
            // hunters in the chain
            "sworn", "player_promoted", "player_demoted", "player_left", "sponsored", "task_done", "order");

    private Chronicle() {
    }

    public static String key(String historyKey, int variant) {
        return PREFIX + historyKey + "." + variant;
    }

    /** The line for one entry. */
    public static String key(String historyKey, long day, int officerId) {
        return key(historyKey, (int) Math.floorMod(GenSeed.mix(day * 31 + officerId), (long) VARIANTS));
    }
}
