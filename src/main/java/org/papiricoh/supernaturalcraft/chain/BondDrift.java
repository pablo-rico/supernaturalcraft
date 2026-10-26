package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.ArrayList;
import java.util.List;

/**
 * How the officers' regard moves on its own each day (pure), after {@link Bonds#decay}: each officer's temperament leans it
 * towards or against its superior and its peers ({@link Temperament#superiorDrift}, {@link Temperament#peerDrift}, times
 * {@link #SCALE}). Then a grudge may break out ({@link #grudges}): a feud with the one it hates if they serve the same chain,
 * a betrayal of a superior it hates.
 */
public final class BondDrift {
    /** Stronger than {@link Bonds#DECAY}, so a temperament's lean outlasts the daily fading (net 1 × SCALE − DECAY a day). */
    public static final int SCALE = 3;

    private BondDrift() {
    }

    public static void apply(Bonds bonds, Hierarchy h) {
        for (Officer o : h.npcs()) {
            Temperament t = Temperament.of(o);
            Bonds.Actor self = Bonds.Actor.of(o);
            Officer superior = h.superiorOf(o);
            if (superior != null && t.superiorDrift() != 0) bonds.adjust(self, Bonds.Actor.of(superior), t.superiorDrift() * SCALE);
            if (t.peerDrift() == 0) continue;
            for (Officer peer : h.ofRank(o.rank))
                if (peer != o && peer.branch == o.branch) bonds.adjust(self, Bonds.Actor.of(peer), t.peerDrift() * SCALE);
        }
    }

    /** A grudge between two officers of one chain that boils over today. */
    public record Outbreak(Officer holder, Officer target) {
    }

    /**
     * The grudges that break out today: an officer whose regard for another of its chain is at {@link Bonds#GRUDGE} or below
     * (and with whom it has no pact) rolls its temperament's {@link Temperament#challengeChance}. Strongest grudge first.
     */
    public static List<Outbreak> grudges(Bonds bonds, Hierarchy h, GenSeed.Rng rng, long day) {
        List<Outbreak> out = new ArrayList<>();
        for (Officer o : h.npcs()) {
            Bonds.Actor self = Bonds.Actor.of(o);
            Officer worst = null;
            int low = Bonds.GRUDGE + 1;
            for (Officer other : h.alive()) {
                if (other == o) continue;
                Bonds.Actor them = Bonds.Actor.of(other);
                int r = bonds.regard(self, them);
                if (r < low && !bonds.pacted(self, them, day)) { low = r; worst = other; }
            }
            if (worst != null && rng.chance(Temperament.of(o).challengeChance())) out.add(new Outbreak(o, worst));
        }
        out.sort((a, b) -> Integer.compare(bonds.regard(Bonds.Actor.of(a.holder()), Bonds.Actor.of(a.target())),
                bonds.regard(Bonds.Actor.of(b.holder()), Bonds.Actor.of(b.target()))));
        return out;
    }
}
