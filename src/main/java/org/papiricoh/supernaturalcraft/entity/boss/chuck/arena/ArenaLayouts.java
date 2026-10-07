package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import org.papiricoh.supernaturalcraft.entity.boss.chuck.Chapter;

import java.util.LinkedHashMap;
import java.util.Map;

/** The five arenas, one per chapter, built for an arena's radius and seed (and cached: a plan never changes). Pure. */
public final class ArenaLayouts {

    /** Cells a plan may hold per column of the arena (the arena remembers 90 000 positions for the whole fight). */
    public static final int CELLS_PER_COLUMN = 6;
    private static final int CACHE = 12;

    private record Key(Chapter chapter, int radius, long seed) {
    }

    private static final Map<Key, ArenaPlan> PLANS = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, ArenaPlan> eldest) {
            return size() > CACHE;
        }
    };

    private ArenaLayouts() {
    }

    /** The most cells a plan of an arena of {@code radius} may hold. */
    public static int budget(int radius) {
        return (int) Math.round(CELLS_PER_COLUMN * Math.PI * radius * radius);
    }

    /** The plan of a chapter's arena. */
    public static synchronized ArenaPlan plan(Chapter chapter, int radius, long seed) {
        return PLANS.computeIfAbsent(new Key(chapter, radius, seed), k -> build(chapter, radius, seed));
    }

    private static ArenaPlan build(Chapter chapter, int radius, long seed) {
        return switch (chapter) {
            case EDEN -> EdenLayout.plan(radius, seed);
            case HELL -> HellLayout.plan(radius, seed);
            case STORM -> StormLayout.plan(radius, seed);
            case LIBRARY -> LibraryLayout.plan(radius, seed);
            case BLANK -> BlankPageLayout.plan(radius, seed);
        };
    }
}
