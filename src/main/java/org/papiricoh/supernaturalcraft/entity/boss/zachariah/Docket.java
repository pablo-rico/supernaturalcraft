package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * "It was already written" (v0.18, pure): from phase III Zachariah announces his next attacks, and keeps to them. The docket is
 * a queue of attack ids kept {@link #capacity()} long; {@link #next()} takes the head, and that is the attack he makes. It is
 * honest but for one revision a phase: an entry is struck through ({@link #revise}) and, once its warning has run out
 * ({@link #tick}), becomes another attack. If the head is taken while its revision is still pending, the revision lands first
 * (Heaven's paperwork never loses an amendment).
 */
public final class Docket {

    /** A pending revision: entry {@code index} becomes {@code replacement} at {@code due}. */
    public record Revision(int index, String replacement, long due) {
    }

    private final List<String> entries = new ArrayList<>();
    private int capacity;
    private boolean revised;
    private int taken;
    private Revision pending;

    /** A new phase's docket of {@code capacity} entries (0 closes it): empty, and its revision unused. */
    public void open(int capacity) {
        this.capacity = Math.max(0, capacity);
        entries.clear();
        revised = false;
        taken = 0;
        pending = null;
    }

    public int capacity() {
        return capacity;
    }

    public boolean active() {
        return capacity > 0;
    }

    /** What is written, head first (a copy). */
    public List<String> entries() {
        return List.copyOf(entries);
    }

    public int size() {
        return entries.size();
    }

    /** Foretold attacks taken this phase. */
    public int taken() {
        return taken;
    }

    /** Whether this phase's revision has been used. */
    public boolean revised() {
        return revised;
    }

    public Revision pending() {
        return pending;
    }

    /**
     * Writes entries until the docket is full; {@code pick} is given the id written just before (the last entry, or
     * {@code lastTaken} for the first) and returns the next. @return whether anything was written
     */
    public boolean fill(String lastTaken, Function<String, String> pick) {
        boolean wrote = false;
        while (active() && entries.size() < capacity) {
            String before = entries.isEmpty() ? lastTaken : entries.getLast();
            String id = pick.apply(before);
            if (id == null || id.isEmpty()) break;
            entries.add(id);
            wrote = true;
        }
        return wrote;
    }

    /** Takes the head (applying its revision first if one is pending on it). @return the attack id, or null if empty */
    public String next() {
        if (entries.isEmpty()) return null;
        if (pending != null && pending.index() == 0) apply();
        String head = entries.removeFirst();
        taken++;
        if (pending != null) pending = new Revision(pending.index() - 1, pending.replacement(), pending.due());
        return head;
    }

    /**
     * Strikes through entry {@code index}: at {@code due} it becomes {@code replacement}. Once a phase, and never into the same
     * attack. @return whether the revision was written
     */
    public boolean revise(int index, String replacement, long due) {
        if (revised || pending != null || index < 0 || index >= entries.size() || replacement == null
                || replacement.equals(entries.get(index))) {
            return false;
        }
        revised = true;
        pending = new Revision(index, replacement, due);
        return true;
    }

    /** Lands a pending revision once due. @return whether the docket changed */
    public boolean tick(long now) {
        if (pending == null || now < pending.due()) return false;
        apply();
        return true;
    }

    private void apply() {
        if (pending.index() >= 0 && pending.index() < entries.size()) entries.set(pending.index(), pending.replacement());
        pending = null;
    }

    /** The docket as the client reads it (ids, comma separated). */
    public String text() {
        return String.join(",", entries);
    }
}
