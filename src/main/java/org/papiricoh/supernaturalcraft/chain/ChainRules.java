package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

/** Pure balance decisions. Randomness is supplied explicitly, never taken from a global stream. */
public final class ChainRules {
    public static final double MAX_HEALTH_MULTIPLIER = 4.0;
    public static final double MAX_DAMAGE_MULTIPLIER = 2.5;
    public enum DailyEvent { NONE, POWER_STRUGGLE, RECRUIT, BETRAYAL, FALL, SKIRMISH }
    public record State(ChainFaction faction, boolean vacancies, boolean loyalOfficers, boolean destabilized) {
        /** {@code vacancies}: room where a recruit may come in (the rest is filled by promotion, not by the dice). */
        public static State of(Hierarchy hierarchy, boolean destabilized) {
            return new State(hierarchy.faction(), Conclave.RECRUIT_RANKS.stream().anyMatch(r -> hierarchy.vacancies().get(r) > 0),
                    hierarchy.alive().stream().anyMatch(o -> o.branch == ChainFaction.Branch.LOYAL), destabilized);
        }
    }

    private ChainRules() {}

    public static GenSeed.Rng rng(long worldSeed, int officerId, long day) {
        return new GenSeed.Rng(GenSeed.mix(GenSeed.mix(worldSeed ^ 0x434841494EL)
                ^ GenSeed.mix(officerId + 1L) ^ GenSeed.mix(day + 0x9E3779B97F4A7C15L)));
    }

    public static double healthMultiplier(ChainRank rank, int level) {
        double base = switch (rank) { case SOLDIER -> 1.25; case CAPTAIN -> 1.75; case LIEUTENANT -> 2.25; case COMMANDER -> 2.6; case LORD -> 3.0; };
        return Math.min(MAX_HEALTH_MULTIPLIER, base * (1 + .05 * Math.max(0, level)));
    }

    public static double damageMultiplier(ChainRank rank, int level) {
        return Math.min(MAX_DAMAGE_MULTIPLIER, (1.1 + .2 * rank.ordinal()) * (1 + .05 * Math.max(0, level)));
    }

    /** Minimum escort size; use the RNG overload for the upper end of each rank's range. */
    public static int escortSize(ChainRank rank) { return rank.ordinal(); }
    public static int escortSize(ChainRank rank, GenSeed.Rng rng) {
        return rank == ChainRank.SOLDIER ? 0 : escortSize(rank) + rng.nextInt(2);
    }
    public static int levelOnPlayerKill(int level) { return level == Integer.MAX_VALUE ? level : Math.max(0, level) + 1; }
    public static int levelOnPlayerKill(Officer o) { return o.level = levelOnPlayerKill(o.level); }
    public static boolean shouldPromoteOnKill(Officer o, GenSeed.Rng rng) {
        return o.alive() && o.rank != ChainRank.LORD && rng.chance(Math.min(.65, .15 + .05 * Math.max(0, o.level)));
    }
    public static Officer challengeWinner(Officer challenger, Officer defender, GenSeed.Rng rng) {
        double advantage = .06 * ((double) challenger.level - defender.level)
                + .12 * (challenger.rank.ordinal() - defender.rank.ordinal());
        return rng.chance(Math.clamp(.5 + advantage, .1, .9)) ? challenger : defender;
    }
    public static boolean returns(boolean properKill, GenSeed.Rng rng) { return !properKill && rng.chance(.5); }
    public static int returnDelayDays(GenSeed.Rng rng) { return rng.range(2, 5); }
    public static int vengeanceDelayDays(ChainRank rank) { return Math.max(1, 3 - rank.ordinal()); }
    public static int destabilizeDays() { return 5; }
    public static int ambushIntervalDays(int heat) { return Math.max(1, 7 - Math.min(6, Math.max(0, heat))); }

    public static DailyEvent pickDailyEvent(GenSeed.Rng rng, State state) {
        if (state.destabilized()) return DailyEvent.NONE;
        int choice = rng.weighted(20, 12, state.vacancies() ? 50 : 0, 8,
                state.faction().hasFallen() && state.loyalOfficers() ? 6 : 0, 12);
        return DailyEvent.values()[choice];
    }
}
