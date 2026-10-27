package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.List;

/**
 * Which mobs serve each chain (pure, registry ids): the Lord is always the plain vessel of its side (a black-eyed demon, an
 * angel), the rest are drawn by weight from the side's rank and file; the fallen branch has its own. Escorts and the
 * strongholds' garrisons draw from the same pools.
 */
public final class ChainRoster {

    public record Pick(String type, int weight) {
    }

    public static final String BLACK_EYED = "supernaturalcraft:black_eyed_demon", ANGEL = "supernaturalcraft:angel",
            FALLEN_ANGEL = "supernaturalcraft:fallen_angel", GRIGORI = "supernaturalcraft:grigori";

    public static final List<Pick> PIT = List.of(new Pick(BLACK_EYED, 4), new Pick("supernaturalcraft:demon_brute", 2),
            new Pick("supernaturalcraft:sulfur_priest", 2), new Pick("supernaturalcraft:demon_flayer", 2),
            new Pick("supernaturalcraft:demon_tormentor", 2));
    public static final List<Pick> HOST = List.of(new Pick(ANGEL, 4), new Pick("supernaturalcraft:angel_templar", 3),
            new Pick("supernaturalcraft:angel_herald", 2), new Pick("supernaturalcraft:angel_choirmaster", 1),
            new Pick("supernaturalcraft:angel_dominion", 1));
    public static final List<Pick> FALLEN = List.of(new Pick(FALLEN_ANGEL, 3), new Pick(GRIGORI, 2));
    /** v0.28: SucroCorp. Its first Lord is the boss {@link Officer#BOSS_TYPE} (see {@code ChainGates}); his heirs wear the suit. */
    public static final String EXECUTIVE = "supernaturalcraft:leviathan_executive", GUARD = "supernaturalcraft:leviathan_guard",
            CHEMIST = "supernaturalcraft:leviathan_chemist";
    public static final List<Pick> BOARD = List.of(new Pick(GUARD, 4), new Pick(CHEMIST, 3), new Pick(EXECUTIVE, 2));

    /** v0.29: the hunter a guild may hire (no chain of the world fields one). */
    public static final String HUNTER = "supernaturalcraft:rival_hunter";

    private ChainRoster() {
    }

    public static List<Pick> pool(ChainFaction faction, ChainFaction.Branch branch) {
        if (faction == ChainFaction.DEMON) return PIT;
        if (faction == ChainFaction.LEVIATHAN) return BOARD;
        return branch == ChainFaction.Branch.FALLEN ? FALLEN : HOST;
    }

    /** The mob a new officer of {@code rank} wears. */
    public static String recruit(ChainFaction faction, ChainFaction.Branch branch, ChainRank rank, GenSeed.Rng rng) {
        if (rank == ChainRank.LORD) return lordType(faction, branch);
        return draw(pool(faction, branch), rng);
    }

    public static String lordType(ChainFaction faction, ChainFaction.Branch branch) {
        return switch (faction) {
            case DEMON -> BLACK_EYED;
            case LEVIATHAN -> EXECUTIVE;
            case ANGEL -> branch == ChainFaction.Branch.FALLEN ? FALLEN_ANGEL : ANGEL;
            // A guild's throne is a hunter's: its record is never materialized.
            case GUILD_1, GUILD_2, GUILD_3, GUILD_4 -> HUNTER;
        };
    }

    public static String draw(List<Pick> pool, GenSeed.Rng rng) {
        int total = pool.stream().mapToInt(Pick::weight).sum(), roll = rng.nextInt(total);
        for (Pick p : pool) if ((roll -= p.weight()) < 0) return p.type();
        return pool.getLast().type();
    }

    /**
     * What an officer's record shows itself as: its own type while that type serves its branch, else its branch's plain vessel
     * (an angel that fell keeps the look it had only if the fallen use it). The Lord always wears its side's plain vessel.
     */
    public static String materialize(ChainFaction faction, ChainFaction.Branch branch, ChainRank rank, String recorded) {
        // A guild's officers came to it as they were: a hired demon stays a demon.
        if (faction.guild()) return recorded;
        if (rank == ChainRank.LORD) return lordType(faction, branch);
        for (Pick p : pool(faction, branch)) if (p.type().equals(recorded)) return recorded;
        return lordType(faction, branch);
    }
}
