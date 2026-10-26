package org.papiricoh.supernaturalcraft.chain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the officers of both chains, and the hunters they deal with, think of one another (pure, owned by
 * {@link ChainSavedData}). Regard is directed (A may loathe B, who barely notices) and runs from {@link #MIN} to {@link #MAX};
 * a pact is mutual and lasts until a day. Events push it ({@link #STOLEN_PROMOTION}, {@link #LOST_FEUD}, …), every day it
 * drifts back towards nothing ({@link #decay}), and past {@link #GRUDGE} it breaks out into a challenge or a betrayal.
 * A hunter's standing with an officer is that officer's regard for them.
 */
public final class Bonds {
    public static final int MIN = -100, MAX = 100;
    /** Thresholds: a rival, a grudge (it wants satisfaction), a loyal friend. */
    public static final int RIVAL = -40, GRUDGE = -70, LOYAL = 50;
    /** Event weights. */
    public static final int STOLEN_PROMOTION = -20, LOST_FEUD = -30, BETRAYED = -50, HELPED = 20, PACT_DAILY = 15, INSULT = -25,
            KILLED_BY = -60, SPARED = 15, TASK_DONE = 25, COWARD = -20;
    /** Below this a bond is forgotten; at most {@link #KEEP} are kept (the strongest). */
    public static final int FORGET = 3, KEEP = 400, DECAY = 2;
    /** Only bonds this strong (or a pact) reach the client. */
    public static final int NOTABLE = 30;

    /** One side of a bond: an officer ({@code "demon:12"}) or a hunter ({@code "player:<uuid>"}). */
    public record Actor(String id) {
        public static Actor of(Officer o) {
            return o.player != null ? of(o.player) : new Actor(o.faction.id() + ":" + o.id);
        }

        public static Actor of(ChainFaction f, int id) {
            return new Actor(f.id() + ":" + id);
        }

        public static Actor of(UUID player) {
            return new Actor("player:" + player);
        }

        public boolean player() {
            return id.startsWith("player:");
        }

        public UUID uuid() {
            return player() ? UUID.fromString(id.substring(7)) : null;
        }

        /** The officer's faction, or null for a hunter. */
        public ChainFaction faction() {
            return player() ? null : ChainFaction.byId(id.substring(0, id.indexOf(':')));
        }

        /** The officer's id, or {@link Officer#NONE} for a hunter. */
        public int officerId() {
            return player() ? Officer.NONE : Integer.parseInt(id.substring(id.indexOf(':') + 1));
        }
    }

    public enum Kind { NONE, LOYAL, RIVAL, GRUDGE, PACT }

    /** Mutable: regard of {@code from} for {@code to}, and the last day of their pact (-1: none). */
    public static final class Bond {
        public final Actor from, to;
        public int regard;
        public long pactUntil = -1;

        public Bond(Actor from, Actor to) {
            this.from = from;
            this.to = to;
        }
    }

    private record Key(Actor from, Actor to) {
    }

    private final Map<Key, Bond> bonds = new HashMap<>();

    public int regard(Actor from, Actor to) {
        Bond b = bonds.get(new Key(from, to));
        return b == null ? 0 : b.regard;
    }

    public Bond bond(Actor from, Actor to) {
        return bonds.computeIfAbsent(new Key(from, to), k -> new Bond(from, to));
    }

    /** Pushes {@code from}'s regard for {@code to}. @return the new regard */
    public int adjust(Actor from, Actor to, int delta) {
        if (from.equals(to) || delta == 0) return regard(from, to);
        Bond b = bond(from, to);
        b.regard = Math.clamp(b.regard + delta, MIN, MAX);
        return b.regard;
    }

    public void set(Actor from, Actor to, int regard) {
        if (!from.equals(to)) bond(from, to).regard = Math.clamp(regard, MIN, MAX);
    }

    /** Both ways. */
    public void mutual(Actor a, Actor b, int delta) {
        adjust(a, b, delta);
        adjust(b, a, delta);
    }

    public void pact(Actor a, Actor b, long untilDay) {
        bond(a, b).pactUntil = untilDay;
        bond(b, a).pactUntil = untilDay;
    }

    public void breakPact(Actor a, Actor b) {
        Bond x = bonds.get(new Key(a, b)), y = bonds.get(new Key(b, a));
        if (x != null) x.pactUntil = -1;
        if (y != null) y.pactUntil = -1;
    }

    public boolean pacted(Actor a, Actor b, long day) {
        Bond x = bonds.get(new Key(a, b));
        return x != null && x.pactUntil >= day;
    }

    public static Kind kind(int regard, boolean pact) {
        if (pact) return Kind.PACT;
        if (regard <= GRUDGE) return Kind.GRUDGE;
        if (regard <= RIVAL) return Kind.RIVAL;
        return regard >= LOYAL ? Kind.LOYAL : Kind.NONE;
    }

    public Kind kind(Actor from, Actor to, long day) {
        return kind(regard(from, to), pacted(from, to, day));
    }

    public List<Bond> all() {
        return List.copyOf(bonds.values());
    }

    /** What {@code from} thinks of everyone it has an opinion on. */
    public List<Bond> of(Actor from) {
        return bonds.values().stream().filter(b -> b.from.equals(from)).toList();
    }

    /** The bonds worth showing: strong regard or a pact still running. */
    public List<Bond> notable(long day, int limit) {
        return bonds.values().stream().filter(b -> Math.abs(b.regard) >= NOTABLE || b.pactUntil >= day)
                .sorted(Comparator.comparingInt((Bond b) -> -Math.abs(b.regard))).limit(limit).toList();
    }

    /**
     * One day passes: pacts warm their bond, other regard eases towards zero by {@link #DECAY}, faint bonds and lapsed pacts
     * are forgotten and only the {@link #KEEP} strongest are kept.
     */
    public void decay(long day) {
        for (Bond b : bonds.values()) {
            if (b.pactUntil >= day) b.regard = Math.min(MAX, b.regard + PACT_DAILY);
            else {
                if (b.pactUntil >= 0) b.pactUntil = -1;
                b.regard -= Integer.signum(b.regard) * Math.min(DECAY, Math.abs(b.regard));
            }
        }
        bonds.values().removeIf(b -> Math.abs(b.regard) < FORGET && b.pactUntil < 0);
        if (bonds.size() > KEEP) {
            List<Bond> weakest = new ArrayList<>(bonds.values());
            weakest.sort(Comparator.comparingInt((Bond b) -> b.pactUntil >= 0 ? Integer.MAX_VALUE : Math.abs(b.regard)));
            for (Bond b : weakest.subList(0, bonds.size() - KEEP)) bonds.remove(new Key(b.from, b.to));
        }
    }

    /** Forgets everything said by or about someone gone for good. */
    public void forget(Actor a) {
        bonds.values().removeIf(b -> b.from.equals(a) || b.to.equals(a));
    }

    /** Forgets one chain entirely (a reset). */
    public void forget(ChainFaction f) {
        bonds.values().removeIf(b -> f.equals(b.from.faction()) || f.equals(b.to.faction()));
    }

    public void clear() {
        bonds.clear();
    }

    /** Restores a saved bond. */
    public void restore(Actor from, Actor to, int regard, long pactUntil) {
        Bond b = bond(from, to);
        b.regard = Math.clamp(regard, MIN, MAX);
        b.pactUntil = pactUntil;
    }
}
