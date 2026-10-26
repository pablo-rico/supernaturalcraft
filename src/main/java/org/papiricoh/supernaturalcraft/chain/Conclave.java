package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Daily resolution; the server owns last-run-day tracking, persistence and destabilization timers. Posts held by hunters
 * are never moved by the dice: no struggle, betrayal, skirmish, Fall or promotion picks them.
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
        Objects.requireNonNull(source);
        List<ChainEvent> events = new ArrayList<>();
        for (Hierarchy h : List.of(demons, angels)) {
            h.advanceTo(day);
            for (Officer o : h.all()) if (h.revive(o)) events.add(event(EventType.REVIVE, o, null, day, false));
        }
        for (Hierarchy h : List.of(demons, angels)) {
            boolean demonPaused = demonsDestabilized || lordDied(events, ChainFaction.DEMON);
            boolean angelPaused = angelsDestabilized || lordDied(events, ChainFaction.ANGEL);
            GenSeed.Rng rng = ChainRules.rng(worldSeed, -2 - h.faction().ordinal(), day);
            ChainRules.State state = ChainRules.State.of(h, h == demons ? demonPaused : angelPaused);
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
                case SKIRMISH -> {
                    if (!demonPaused && !angelPaused) skirmish(demons, angels, rng, day, events);
                }
            }
        }
        demons.reassignOrphans();
        angels.reassignOrphans();
        if (!demons.invariantsHold() || !angels.invariantsHold())
            throw new IllegalStateException("Invalid chain after conclave");
        return List.copyOf(events);
    }

    private static boolean lordDied(List<ChainEvent> events, ChainFaction faction) {
        return events.stream().anyMatch(e -> e.lordKilled() && e.otherFaction() == faction);
    }

    private static ChainEvent event(EventType type, Officer officer, Officer other, long day, boolean lordKilled) {
        return new ChainEvent(type, officer.faction, officer.id, other == null ? null : other.faction,
                other == null ? Officer.NONE : other.id, officer.rank, day, lordKilled);
    }

    private static void recruit(Hierarchy h, RecruitSource source, GenSeed.Rng rng, long day, List<ChainEvent> events) {
        for (ChainRank rank : ChainRank.values()) {
            if (h.vacancies().get(rank) == 0) continue;
            List<Officer> below = rank == ChainRank.SOLDIER ? List.of() : h.ofRank(rank.down()).stream().filter(o -> !o.held()).toList();
            if (!below.isEmpty()) {
                Officer o = rng.pick(below);
                if (h.promote(o)) events.add(event(EventType.PROMOTE, o, null, day, false));
            } else {
                Recruit recruit = source.recruit(h.faction(), ChainFaction.Branch.LOYAL);
                if (recruit == null) return;
                Officer o = h.recruit(rank, Officer.NONE, rng.nextLong(), recruit.entityType(), recruit.variant());
                if (o != null) events.add(event(EventType.RECRUIT, o, null, day, false));
            }
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

    private static void skirmish(Hierarchy demons, Hierarchy angels, GenSeed.Rng rng, long day, List<ChainEvent> events) {
        List<Officer> hell = new ArrayList<>(demons.npcs());
        hell.addAll(angels.npcs().stream().filter(Officer::hellSide).toList());
        List<Officer[]> choices = new ArrayList<>();
        for (Officer attacker : hell) for (Officer defender : angels.npcs()) {
            if (!defender.hellSide() && Math.abs(attacker.rank.ordinal() - defender.rank.ordinal()) <= 1)
                choices.add(new Officer[]{attacker, defender});
        }
        if (choices.isEmpty()) return;
        Officer[] pair = rng.pick(choices);
        Officer winner = ChainRules.challengeWinner(pair[0], pair[1], rng);
        Officer loser = winner == pair[0] ? pair[1] : pair[0];
        boolean lordKilled = loser.rank == ChainRank.LORD;
        die(loser.faction == ChainFaction.DEMON ? demons : angels, loser, rng, day);
        ChainRules.levelOnPlayerKill(winner);
        if (ChainRules.shouldPromoteOnKill(winner, rng))
            (winner.faction == ChainFaction.DEMON ? demons : angels).promote(winner);
        events.add(event(EventType.SKIRMISH, winner, loser, day, lordKilled));
    }

    private static void die(Hierarchy h, Officer o, GenSeed.Rng rng, long day) {
        boolean returning = ChainRules.returns(false, rng);
        h.kill(o, !returning, returning ? day + ChainRules.returnDelayDays(rng) : 0);
    }
}
