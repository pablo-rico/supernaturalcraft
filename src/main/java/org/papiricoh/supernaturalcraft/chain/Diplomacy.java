package org.papiricoh.supernaturalcraft.chain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Who is at odds with whom once guilds stand (pure, v0.29). The world's own factions keep their profile: every one of them
 * fights every other ({@link ChainFaction#hostile}), and SucroCorp fights everyone, guilds included. A guild's stance towards
 * any other faction lives here:
 * <ul>
 *     <li>hostility is declared by either side alone ({@link #declare});</li>
 *     <li>neutrality and alliance take both: one {@link #propose proposes}, the other {@link #accept accepts} (or proposes the
 *     same, which seals it).</li>
 * </ul>
 * A new guild starts hostile to the Pit and the Host (upstarts are not welcome) and neutral towards the other guilds
 * ({@link #initial}). Saved as {@link Entry} lists; a pair with no entry is at its initial stance.
 */
public final class Diplomacy {
    public enum Stance {
        HOSTILE,
        NEUTRAL,
        ALLIED
    }

    /** One stance ({@code a}, {@code b} unordered) or one standing proposal ({@code a} offers {@code b} the stance). */
    public record Entry(ChainFaction a, ChainFaction b, Stance stance) {}

    /** What became of a proposal. */
    public enum Outcome {
        /** Both sides want it: the stance holds now. */
        SEALED,
        /** It waits for the other side. */
        PROPOSED,
        /** Nothing to do: the stance already holds, or the pair cannot change (two of the world's factions, SucroCorp). */
        REFUSED
    }

    private final Map<Integer, Stance> stances = new HashMap<>();
    private final Map<Integer, Stance> proposals = new HashMap<>();

    /** The stance between two factions (a faction is allied with itself). */
    public Stance stance(ChainFaction a, ChainFaction b) {
        if (a == b) return Stance.ALLIED;
        if (!open(a, b)) return Stance.HOSTILE;
        return stances.getOrDefault(pair(a, b), initial(a, b));
    }

    public boolean hostile(ChainFaction a, ChainFaction b) {
        return stance(a, b) == Stance.HOSTILE;
    }

    /** Whether two different factions are allies (a faction's own members are not "allies" of it here). */
    public boolean allied(ChainFaction a, ChainFaction b) {
        return a != b && stance(a, b) == Stance.ALLIED;
    }

    /** A guild's stance on the day it is founded. */
    public static Stance initial(ChainFaction a, ChainFaction b) {
        return a.guild() && b.guild() ? Stance.NEUTRAL : Stance.HOSTILE;
    }

    /** Whether the pair's stance can change at all: at least one guild, and no SucroCorp. */
    public static boolean open(ChainFaction a, ChainFaction b) {
        return a != b && (a.guild() || b.guild()) && a != ChainFaction.LEVIATHAN && b != ChainFaction.LEVIATHAN;
    }

    /** {@code by} turns hostile on {@code against}, alone; every proposal between them lapses. @return whether anything changed */
    public boolean declare(ChainFaction by, ChainFaction against) {
        if (!open(by, against)) return false;
        boolean changed = stance(by, against) != Stance.HOSTILE;
        set(by, against, Stance.HOSTILE);
        return changed;
    }

    /**
     * Sets the pair's stance from outside the chain (with Nations &amp; War, two guild-nations follow their alliance and wars;
     * a vassal's stance towards its overlord is calm): every proposal between them lapses. @return whether anything changed
     */
    public boolean force(ChainFaction a, ChainFaction b, Stance stance) {
        if (!open(a, b) || stance(a, b) == stance && proposals.get(directed(a, b)) == null && proposals.get(directed(b, a)) == null) return false;
        set(a, b, stance);
        return true;
    }

    /** {@code by} offers {@code to} a calmer stance; if {@code to} already offered the same, it is sealed. */
    public Outcome propose(ChainFaction by, ChainFaction to, Stance stance) {
        if (stance == Stance.HOSTILE || !open(by, to) || stance(by, to) == stance) return Outcome.REFUSED;
        if (proposals.get(directed(to, by)) == stance) {
            set(by, to, stance);
            return Outcome.SEALED;
        }
        proposals.put(directed(by, to), stance);
        return Outcome.PROPOSED;
    }

    /** {@code by} takes the offer {@code from} made it. @return whether there was one */
    public boolean accept(ChainFaction by, ChainFaction from) {
        Stance offered = proposals.get(directed(from, by));
        if (offered == null) return false;
        set(by, from, offered);
        return true;
    }

    /** {@code by} turns the offer down (or {@code from} withdraws it). */
    public boolean refuse(ChainFaction by, ChainFaction from) {
        return proposals.remove(directed(from, by)) != null;
    }

    /** What {@code from} offers {@code to}, or null. */
    public Stance proposed(ChainFaction from, ChainFaction to) {
        return proposals.get(directed(from, to));
    }

    /** A guild is gone (its slot freed): every stance and offer it had is forgotten. */
    public void forget(ChainFaction f) {
        stances.keySet().removeIf(k -> involves(k, f));
        proposals.keySet().removeIf(k -> involves(k, f));
    }

    public void clear() {
        stances.clear();
        proposals.clear();
    }

    /** The stances that differ from {@link #initial}, as saved and synced. */
    public List<Entry> stances() {
        List<Entry> out = new ArrayList<>();
        stances.forEach((k, s) -> out.add(new Entry(first(k), second(k), s)));
        return out;
    }

    /** The standing proposals ({@code a} offers {@code b}). */
    public List<Entry> proposals() {
        List<Entry> out = new ArrayList<>();
        proposals.forEach((k, s) -> out.add(new Entry(first(k), second(k), s)));
        return out;
    }

    public void restore(List<Entry> stances, List<Entry> proposals) {
        clear();
        for (Entry e : stances) if (open(e.a(), e.b())) set(e.a(), e.b(), e.stance());
        for (Entry e : proposals) if (open(e.a(), e.b()) && e.stance() != Stance.HOSTILE) this.proposals.put(directed(e.a(), e.b()), e.stance());
    }

    private void set(ChainFaction a, ChainFaction b, Stance s) {
        proposals.remove(directed(a, b));
        proposals.remove(directed(b, a));
        if (s == initial(a, b)) stances.remove(pair(a, b));
        else stances.put(pair(a, b), s);
    }

    private static final int SHIFT = 8;

    private static int pair(ChainFaction a, ChainFaction b) {
        return a.ordinal() < b.ordinal() ? directed(a, b) : directed(b, a);
    }

    private static int directed(ChainFaction from, ChainFaction to) {
        return from.ordinal() << SHIFT | to.ordinal();
    }

    private static ChainFaction first(int key) {
        return ChainFaction.values()[key >> SHIFT];
    }

    private static ChainFaction second(int key) {
        return ChainFaction.values()[key & ((1 << SHIFT) - 1)];
    }

    private static boolean involves(int key, ChainFaction f) {
        return first(key) == f || second(key) == f;
    }
}
