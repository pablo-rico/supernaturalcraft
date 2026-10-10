package org.papiricoh.supernaturalcraft.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A hunter's memories (v0.18, attachment {@code MEMORY_LOG}, survives death): an append-only timeline (at most {@link #CAP},
 * deduplicated by {@link Memory#id}), the ones they have gathered in their Heaven, and whether the one-time backfill from an old
 * save has run. Immutable and pure.
 * <p>Created by the foundations; owned by the memory work (it may add fields with {@code optionalFieldOf}).
 */
public record MemoryLog(List<Memory> entries, Set<String> collected, boolean backfilled) {

    public static final int CAP = 256;
    public static final MemoryLog EMPTY = new MemoryLog(List.of(), Set.of(), false);

    public static final Codec<MemoryLog> CODEC = RecordCodecBuilder.create(i -> i.group(
            Memory.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(MemoryLog::entries),
            Codec.STRING.listOf().<Set<String>>xmap(LinkedHashSet::new, List::copyOf).optionalFieldOf("collected", Set.of())
                    .forGetter(MemoryLog::collected),
            Codec.BOOL.optionalFieldOf("backfilled", false).forGetter(MemoryLog::backfilled)
    ).apply(i, MemoryLog::new));

    public boolean has(String id) {
        for (Memory m : entries) if (m.id().equals(id)) return true;
        return false;
    }

    /**
     * This log with {@code m} appended, unless one with its id is already there. Past {@link #CAP} the least of them goes: the
     * oldest of the lightest kind not yet gathered (or, if every one of that weight is gathered, the oldest of the lightest), so
     * a great victory is never pushed out by a crowd of sightings.
     */
    public MemoryLog with(Memory m) {
        if (has(m.id())) return this;
        List<Memory> out = new ArrayList<>(entries);
        out.add(m);
        while (out.size() > CAP) out.remove(evictee(out, collected));
        return new MemoryLog(List.copyOf(out), collected, backfilled);
    }

    private static int evictee(List<Memory> list, Set<String> collected) {
        int lightest = Integer.MAX_VALUE;
        for (Memory m : list) lightest = Math.min(lightest, m.kind().weight);
        int firstOfWeight = -1;
        for (int i = 0; i < list.size(); i++) {
            Memory m = list.get(i);
            if (m.kind().weight != lightest) continue;
            if (!collected.contains(m.id())) return i;
            if (firstOfWeight < 0) firstOfWeight = i;
        }
        return firstOfWeight;
    }

    /** The memory with this id, or null. */
    public Memory find(String id) {
        for (Memory m : entries) if (m.id().equals(id)) return m;
        return null;
    }

    public boolean isCollected(String id) {
        return collected.contains(id);
    }

    public MemoryLog withCollected(String id) {
        Set<String> out = new LinkedHashSet<>(collected);
        out.add(id);
        return new MemoryLog(entries, out, backfilled);
    }

    public MemoryLog withBackfilled() {
        return new MemoryLog(entries, collected, true);
    }
}
