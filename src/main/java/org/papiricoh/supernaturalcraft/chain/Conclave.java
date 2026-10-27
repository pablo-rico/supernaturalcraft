package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Daily resolution; the server owns last-run-day tracking, persistence and destabilization timers. Posts held by hunters
 * are never moved by the dice: no struggle, betrayal, skirmish, Fall or promotion picks them. Every day, before the dice,
 * gaps are filled by promotion from the throne down ({@code climb}); recruits only ever come in at the bottom.
 */
public final class Conclave {
    public enum EventType { REVIVE, POWER_STRUGGLE, RECRUIT, PROMOTE, BETRAYAL, FALL, SKIRMISH }
    /** For combat, officer is the winner and other is the loser; ids are local to their faction. */
    public record ChainEvent(EventType type, ChainFaction faction, int officerId,
                             ChainFaction otherFaction, int otherId, ChainRank rank, long day,
                             boolean lordKilled) {}
    public record Recruit(String entityType, int variant) {
        public Recruit { Objects.requireNonNull(entityType); }
    }
    @FunctionalInterface
    public interface RecruitSource {
        Recruit recruit(ChainFaction faction, ChainFaction.Branch branch);
    }

    private Conclave() {}

    /** Call once per world day. A deterministic RecruitSource is required for deterministic output. */
    public static List<ChainEvent> run(Hierarchy demons, Hierarchy angels, long worldSeed, long day,
                                       boolean demonsDestabilized, boolean angelsDestabilized, RecruitSource source) {
        return run(demons, angels, worldSeed, day, demonsDestabilized, angelsDestabilized, true, source);
    }

    /** {@code fallenOpen}: whether an angel may Fall yet (the fallen walk the Earth only after Metatron). */
    public static List<ChainEvent> run(Hierarchy demons, Hierarchy angels, long worldSeed, long day,
                                       boolean demonsDestabilized, boolean angelsDestabilized, boolean fallenOpen, RecruitSource source) {
        if (demons.faction() != ChainFaction.DEMON || angels.faction() != ChainFaction.ANGEL)
            throw new IllegalArgumentException("Expected demon and angel hierarchies, in that order");
        Map<ChainFaction, Hierarchy> chains = new EnumMap<>(ChainFaction.class);
        chains.put(ChainFaction.DEMON, demons);
        chains.put(ChainFaction.ANGEL, angels);
        Set<ChainFaction> paused = EnumSet.noneOf(ChainFaction.class);
        if (demonsDestabilized) paused.add(ChainFaction.DEMON);
        if (angelsDestabilized) paused.add(ChainFaction.ANGEL);
        return run(chains, worldSeed, day, paused, fallenOpen, source);
    }

    /**
     * One day for every chain in {@code chains} (keyed by its faction; a chain that is asleep or shaken goes in
     * {@code destabilized}, where nothing but revivals happens and no skirmish reaches it).
     */
    public static List<ChainEvent> run(Map<ChainFaction, Hierarchy> chains, long worldSeed, long day,
                                       Set<ChainFaction> destabilized, boolean fallenOpen, RecruitSource source) {
        chains.forEach((f, h) -> {
            if (h.faction() != f) throw new IllegalArgumentException("Hierarchy of " + h.faction() + " keyed as " + f);
        });
        Objects.requireNonNull(source);
        List<ChainEvent> events = new ArrayList<>();
        for (Hierarchy h : chains.values()) {
            h.advanceTo(day);
            for (Officer o : h.all()) if (h.revive(o)) events.add(event(EventType.REVIVE, o, null, day, false));
        }
        for (Hierarchy h : chains.values())
            if (!destabilized.contains(h.faction())) climb(h, ChainRules.rng(worldSeed, -20 - h.faction().ordinal(), day), day, events);
        for (Hierarchy h : chains.values()) {
            Set<ChainFaction> paused = EnumSet.noneOf(ChainFaction.class);
            for (ChainFaction f : chains.keySet()) if (destabilized.contains(f) || lordDied(events, f)) paused.add(f);
            GenSeed.Rng rng = ChainRules.rng(worldSeed, -2 - h.faction().ordinal(), day);
            ChainRules.State state = ChainRules.State.of(h, paused.contains(h.faction()));
            if (!fallenOpen) state = new ChainRules.State(state.faction(), state.vacancies(), false, state.destabilized());
            ChainRules.DailyEvent pick = ChainRules.pickDailyEvent(rng, state);
            switch (pick) {
                case NONE -> { }
                case RECRUIT -> recruit(h, source, rng, day, events);
                case POWER_STRUGGLE -> struggle(h, rng, day, events);
                case BETRAYAL -> betray(h, rng, day, events);
                case FALL -> {
                    List<Officer> loyal = h.npcs().stream().filter(o -> o.branch == ChainFaction.Branch.LOYAL).toList();
                    if (!loyal.isEmpty()) {
                        Officer o = rng.pick(loyal);
                        if (h.fall(o)) events.add(event(EventType.FALL, o, null, day, false));
                    }
                }
                case SKIRMISH -> skirmish(chains, h.faction(), paused, rng, day, events);
            }
        }
        for (Hierarchy h : chains.values()) h.reassignOrphans();
        for (Hierarchy h : chains.values())
            if (!h.invariantsHold()) throw new IllegalStateException("Invalid chain after conclave");
        return List.copyOf(events);
    }

    private static boolean lordDied(List<ChainEvent> events, ChainFaction faction) {
        return events.stream().anyMatch(e -> e.lordKilled() && e.otherFaction() == faction);
    }

    private static ChainEvent event(EventType type, Officer officer, Officer other, long day, boolean lordKilled) {
        return new ChainEvent(type, officer.faction, officer.id, other == null ? null : other.faction,
                other == null ? Officer.NONE : other.id, officer.rank, day, lordKilled);
    }

    /**
     * The ladder: every day each rank with room, from the throne down, takes the most seasoned officer of the rank below
     * (one a day per rank, so a gap climbs down the chain over a few days). Nobody is ever made above {@link #RECRUIT_RANKS}:
     * a gap high up is filled by promotion, and new blood comes in only at the bottom ({@link #recruit}).
     */
    private static void climb(Hierarchy h, GenSeed.Rng rng, long day, List<ChainEvent> events) {
        for (int r = ChainRank.values().length - 1; r > 0; r--) {
            ChainRank rank = ChainRank.values()[r];
            if (h.vacancies().get(rank) == 0) continue;
            List<Officer> below = h.ofRank(rank.down()).stream().filter(o -> !o.held()).toList();
            if (below.isEmpty()) continue;
            int best = below.stream().mapToInt(o -> o.level).max().orElse(0);
            Officer o = rng.pick(below.stream().filter(x -> x.level == best).toList());
            if (h.promote(o)) events.add(event(EventType.PROMOTE, o, null, day, false));
        }
    }

    /** The only ranks a recruit comes in at (I and II): above them, posts are earned by promotion ({@link #climb}). */
    static final List<ChainRank> RECRUIT_RANKS = List.of(ChainRank.SOLDIER, ChainRank.CAPTAIN);

    private static void recruit(Hierarchy h, RecruitSource source, GenSeed.Rng rng, long day, List<ChainEvent> events) {
        for (ChainRank rank : RECRUIT_RANKS) {
            if (h.vacancies().get(rank) == 0) continue;
            Recruit recruit = source.recruit(h.faction(), ChainFaction.Branch.LOYAL);
            if (recruit == null) return;
            Officer o = h.recruit(rank, Officer.NONE, rng.nextLong(), recruit.entityType(), recruit.variant());
            if (o != null) events.add(event(EventType.RECRUIT, o, null, day, false));
            return;
        }
    }

    private static void struggle(Hierarchy h, GenSeed.Rng rng, long day, List<ChainEvent> events) {
        List<Officer> challengers = h.npcs().stream().filter(o -> h.superiorOf(o) != null && !h.superiorOf(o).held()).toList();
        if (challengers.isEmpty()) return;
        Officer challenger = rng.pick(challengers), defender = h.superiorOf(challenger);
        Officer winner = ChainRules.challengeWinner(challenger, defender, rng);
        Officer loser = winner == challenger ? defender : challenger;
        ChainRank oldRank = loser.rank;
        if (winner == challenger) {
            ChainRank vacant = challenger.rank;
            if (rng.chance(.5)) {
                // Swapping into the challenger's old post is an atomic demotion, even when both ranks are full.
                defender.rank = vacant;
                defender.log("demoted:" + vacant.name());
            } else die(h, defender, rng, day);
            challenger.rank = oldRank;
            challenger.log("promoted:" + oldRank.name());
        } else if (!rng.chance(.5) || !h.demote(loser)) die(h, loser, rng, day);
        ChainRules.levelOnPlayerKill(winner);
        h.reassignOrphans();
        events.add(event(EventType.POWER_STRUGGLE, winner, loser, day, oldRank == ChainRank.LORD && !loser.alive()));
    }

    private static void betray(Hierarchy h, GenSeed.Rng rng, long day, List<ChainEvent> events) {
        List<Officer[]> choices = new ArrayList<>();
        for (Officer o : h.npcs()) for (Officer parent : h.alive()) {
            if (parent.id != o.superior && parent.rank.ordinal() > o.rank.ordinal() && parent.branch == o.branch)
                choices.add(new Officer[]{o, parent});
        }
        if (choices.isEmpty()) return;
        Officer[] pair = rng.pick(choices);
        pair[0].superior = pair[1].id;
        pair[0].log("betrayed:" + pair[1].id);
        events.add(event(EventType.BETRAYAL, pair[0], pair[1], day, false));
    }

    /** A skirmish {@code camp} starts against any other camp that is not paused: officers fight for their camp. */
    private static void skirmish(Map<ChainFaction, Hierarchy> chains, ChainFaction camp, Set<ChainFaction> paused,
                                 GenSeed.Rng rng, long day, List<ChainEvent> events) {
        if (paused.contains(camp)) return;
        List<Officer> ours = new ArrayList<>(), theirs = new ArrayList<>();
        for (Hierarchy h : chains.values()) for (Officer o : h.npcs()) {
            if (o.camp() == camp) ours.add(o);
            else if (!paused.contains(o.camp()) && !paused.contains(o.faction)) theirs.add(o);
        }
        List<Officer[]> choices = new ArrayList<>();
        for (Officer attacker : ours) for (Officer defender : theirs) {
            if (Math.abs(attacker.rank.ordinal() - defender.rank.ordinal()) <= 1)
                choices.add(new Officer[]{attacker, defender});
        }
        if (choices.isEmpty()) return;
        Officer[] pair = rng.pick(choices);
        Officer winner = ChainRules.challengeWinner(pair[0], pair[1], rng);
        Officer loser = winner == pair[0] ? pair[1] : pair[0];
        boolean lordKilled = loser.rank == ChainRank.LORD;
        die(chains.get(loser.faction), loser, rng, day);
        ChainRules.levelOnPlayerKill(winner);
        if (ChainRules.shouldPromoteOnKill(winner, rng))
            chains.get(winner.faction).promote(winner);
        events.add(event(EventType.SKIRMISH, winner, loser, day, lordKilled));
    }

    private static void die(Hierarchy h, Officer o, GenSeed.Rng rng, long day) {
        boolean returning = ChainRules.returns(false, rng);
        h.kill(o, !returning, returning ? day + ChainRules.returnDelayDays(rng) : 0);
    }
}
