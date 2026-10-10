package org.papiricoh.supernaturalcraft.client.heaven;

import java.util.ArrayList;
import java.util.List;

/**
 * What Zachariah's docket shows (v0.18, pure): the attacks already written ({@code HeavenFxPayload.FORETOLD}, ids comma
 * separated) and the one revision a phase ({@code REVISION}): the entry is struck through, and after the given ticks its new
 * text takes its place, marked as revised until the docket is written again.
 */
public final class DocketView {

    /** One line: its attack id, whether it is being struck through (and what replaces it), whether it was revised. */
    public record Entry(String id, String replacement, int strikeLeft, boolean revised) {
        public boolean striking() {
            return strikeLeft > 0;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    /** Ticks since the docket was last written (for its ink to appear). */
    private int age;

    /** The docket as written now: ids, comma separated (blank = no docket). */
    public void foretell(String ids) {
        entries.clear();
        age = 0;
        if (ids == null || ids.isBlank()) return;
        for (String s : ids.split(",")) {
            String id = s.trim();
            if (!id.isEmpty()) entries.add(new Entry(id, "", 0, false));
        }
    }

    /** Entry {@code index} is struck through and becomes {@code replacement} in {@code ticks}. */
    public void revise(int index, String replacement, int ticks) {
        if (index < 0 || index >= entries.size()) return;
        Entry e = entries.get(index);
        if (ticks <= 0) entries.set(index, new Entry(replacement.trim(), "", 0, true));
        else entries.set(index, new Entry(e.id(), replacement.trim(), ticks, false));
    }

    public void tick() {
        age++;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (!e.striking()) continue;
            int left = e.strikeLeft() - 1;
            entries.set(i, left <= 0 ? new Entry(e.replacement().isEmpty() ? e.id() : e.replacement(), "", 0, true)
                    : new Entry(e.id(), e.replacement(), left, false));
        }
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public boolean empty() {
        return entries.isEmpty();
    }

    public int age() {
        return age;
    }

    public void clear() {
        entries.clear();
        age = 0;
    }
}
