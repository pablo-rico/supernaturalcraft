package org.papiricoh.supernaturalcraft.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Which memories the shrines of a hunter's memory lane hold (v0.18, pure). The ones not yet gathered come first, the weightiest
 * of them first (a fallen archangel before a first sighting), the older first among equals; shrines left over show memories
 * already gathered, in turn ({@code rotation} moves the window along them, so a lane where everything is gathered changes from
 * day to day). The shrines are then filled oldest first: slot 0 stands by the gate, the last one by the house.
 */
public final class MemorySelection {

    private MemorySelection() {
    }

    /** {@link #choose(List, Set, int, int)} with no rotation. */
    public static List<Memory> choose(List<Memory> log, Set<String> collected, int n) {
        return choose(log, collected, n, 0);
    }

    /**
     * @param log       the hunter's memories (any order)
     * @param collected the ids already gathered
     * @param n         shrines to fill
     * @param rotation  how far along the gathered ones the leftover shrines start
     * @return at most {@code n} memories, oldest first (slot order)
     */
    public static List<Memory> choose(List<Memory> log, Set<String> collected, int n, int rotation) {
        if (n <= 0 || log.isEmpty()) return List.of();
        List<Memory> fresh = new ArrayList<>(), gathered = new ArrayList<>();
        for (Memory m : log) (collected.contains(m.id()) ? gathered : fresh).add(m);
        fresh.sort(Comparator.comparingInt((Memory m) -> -m.kind().weight).thenComparing(MemoryRules.CHRONOLOGICAL));
        List<Memory> out = new ArrayList<>(fresh.subList(0, Math.min(n, fresh.size())));
        int missing = n - out.size();
        if (missing > 0 && !gathered.isEmpty()) {
            gathered.sort(MemoryRules.CHRONOLOGICAL);
            int start = Math.floorMod(rotation, gathered.size());
            for (int i = 0; i < Math.min(missing, gathered.size()); i++) out.add(gathered.get((start + i) % gathered.size()));
        }
        out.sort(MemoryRules.CHRONOLOGICAL);
        return List.copyOf(out);
    }

    /** The ids of {@link #choose(List, Set, int, int)}, in slot order. */
    public static List<String> ids(List<Memory> log, Set<String> collected, int n, int rotation) {
        return choose(log, collected, n, rotation).stream().map(Memory::id).toList();
    }
}
