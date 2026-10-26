package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Feuds inside a chain (pure): about once every {@link #DAYS} days two officers of one faction fight it out, an underling
 * challenging its superior or two rivals of one rank. While the throne stands vacant they flare up ({@link #VACANT_CHANCE},
 * always with a Commander in them) and the Commanders' wins are their claims to the throne ({@link #heir}).
 *
 * <p>A challenger that beats a higher-ranked foe takes its rank and gives it its own; any other winner gains a level. The
 * loser loses a level and gains a scar, and one feud in {@link #DEATH_CHANCE} kills it (it returns days later, as from any
 * death that is not a proper kill).
 */
public final class Feud {

    public static final int DAYS = 3;
    public static final double CHANCE = 1.0 / DAYS, VACANT_CHANCE = Math.min(1, 3.0 / DAYS), DEATH_CHANCE = 0.25;
    /** The interregnum lasts this many days, at least and at most. */
    public static final int INTERREGNUM_MIN = 3, INTERREGNUM_MAX = 5;

    public record Pair(Officer challenger, Officer defender) {
    }

    public record Outcome(Officer winner, Officer loser, boolean swapped, boolean died) {
    }

    private Feud() {
    }

    /** Whether today's roll starts a feud. */
    public static boolean rolls(GenSeed.Rng rng, boolean vacant) {
        return rng.chance(vacant ? VACANT_CHANCE : CHANCE);
    }

    public static int interregnumDays(GenSeed.Rng rng) {
        return rng.range(INTERREGNUM_MIN, INTERREGNUM_MAX);
    }

    /**
     * Who feuds, or null if nobody can. During an interregnum it is two Commanders (or a Lieutenant challenging the only one);
     * otherwise an underling and its superior, or two rivals of one rank and branch. A pair may include a post a hunter holds:
     * the server turns that one into a {@link Challenge} the hunter must answer.
     */
    public static Pair pick(Hierarchy h, boolean vacant, GenSeed.Rng rng) {
        List<Officer> commanders = h.ofRank(ChainRank.COMMANDER);
        if (vacant) {
            if (commanders.size() >= 2) {
                Officer a = rng.pick(commanders);
                List<Officer> rivals = commanders.stream().filter(o -> o != a).toList();
                return new Pair(a, rng.pick(rivals));
            }
            if (commanders.isEmpty()) return null;
            List<Officer> lieutenants = h.ofRank(ChainRank.LIEUTENANT);
            return lieutenants.isEmpty() ? null : new Pair(rng.pick(lieutenants), commanders.getFirst());
        }
        List<Pair> upward = new ArrayList<>(), rivals = new ArrayList<>();
        for (Officer o : h.alive()) {
            Officer superior = h.superiorOf(o);
            if (superior != null && superior.alive()) upward.add(new Pair(o, superior));
            for (Officer r : h.alive())
                if (r.id > o.id && r.rank == o.rank && r.branch == o.branch) rivals.add(new Pair(o, r));
        }
        if (upward.isEmpty() && rivals.isEmpty()) return null;
        boolean up = rivals.isEmpty() || !upward.isEmpty() && rng.chance(0.5);
        return rng.pick(up ? upward : rivals);
    }

    /** Settles a feud in the records. The winner may be forced (a fight seen to its end in the world). */
    public static Outcome resolve(Hierarchy h, Pair p, GenSeed.Rng rng, long day, Officer forcedWinner) {
        Officer winner = forcedWinner != null ? forcedWinner : ChainRules.challengeWinner(p.challenger(), p.defender(), rng);
        Officer loser = winner == p.challenger() ? p.defender() : p.challenger();
        // Only a living loser gives up its rank: a dead one's post is already empty (swapping with it would overfill a rank).
        boolean swapped = winner == p.challenger() && loser.alive() && p.challenger().rank.ordinal() < p.defender().rank.ordinal();
        if (swapped) {
            ChainRank low = p.challenger().rank;
            p.challenger().rank = p.defender().rank;
            p.defender().rank = low;
            p.challenger().log("promoted:" + p.challenger().rank.name());
            p.defender().log("demoted:" + low.name());
        } else {
            ChainRules.levelOnPlayerKill(winner);
        }
        loser.level = Math.max(0, loser.level - 1);
        loser.scars++;
        winner.log("feud:won:" + loser.id);
        loser.log("feud:lost:" + winner.id);
        boolean died = loser.alive() && rng.chance(DEATH_CHANCE);
        if (died) h.kill(loser, false, day + ChainRules.returnDelayDays(rng));
        h.reassignOrphans();
        return new Outcome(winner, loser, swapped, died);
    }

    /**
     * Who takes the empty throne when the interregnum ends: the Commander with most claims (wins in feuds since the Lord
     * died), then the higher level, then the lower id; with no Commander, the highest-ranked officer of the loyal branch.
     * Posts held by hunters are passed over.
     */
    public static Officer heir(Hierarchy h, Map<Integer, Integer> claims) {
        Comparator<Officer> order = Comparator.<Officer>comparingInt(o -> claims.getOrDefault(o.id, 0))
                .thenComparingInt(o -> o.level).thenComparing(Comparator.<Officer>comparingInt(o -> o.id).reversed());
        // A hunter holding a post never inherits the throne: they must win it in a duel.
        List<Officer> commanders = h.ofRank(ChainRank.COMMANDER).stream().filter(o -> !o.held()).toList();
        if (!commanders.isEmpty()) return commanders.stream().max(order).orElseThrow();
        return h.npcs().stream().filter(o -> o.branch == ChainFaction.Branch.LOYAL)
                .max(Comparator.<Officer>comparingInt(o -> o.rank.ordinal()).thenComparing(order)).orElse(null);
    }
}
