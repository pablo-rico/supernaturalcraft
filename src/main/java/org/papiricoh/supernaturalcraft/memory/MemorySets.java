package org.papiricoh.supernaturalcraft.memory;

import org.papiricoh.supernaturalcraft.crossroads.BossProgression;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;

/**
 * The collections gathered memories count towards, and the small gift each one gives once complete (v0.18, pure). A memory
 * counts only once gathered (touched in its staged scene), and only while it is still in the log.
 */
public final class MemorySets {

    /** Victories: share of a great enemy's blow turned aside (added to Aegis). */
    public static final float VICTORY_AEGIS = 0.03f;
    /** All Victories: extra max health (one heart). */
    public static final double ALL_VICTORIES_HEALTH = 2.0;
    /** Crossroads: days added to the term of every deal struck afterwards. */
    public static final int CROSSROADS_DAYS = 1;
    /** Cases: research takes this share of its time. */
    public static final double CASES_RESEARCH_TIME = 0.95;
    /** Kin: extra max mana. */
    public static final int KIN_MANA = 10;
    /** Sightings: extra damage to creatures already seen (never to a great enemy). */
    public static final float SIGHTING_DAMAGE = 0.05f;
    /** Companions: tamed pets this close to their hunter mend (Regeneration I). */
    public static final double COMPANION_RANGE = 16;

    public enum Set {
        /** Five great enemies' falls gathered. */
        VICTORIES("victories", 5),
        /** The fall of every enemy on the main road gathered. */
        ALL_VICTORIES("all_victories", -1),
        CROSSROADS("crossroads", 2),
        CASES("cases", 3),
        KIN("kin", 3),
        COMPANIONS("companions", 1),
        SIGHTINGS("sightings", 10);

        /** The {@link MemoryKind#set} it counts (ALL_VICTORIES counts victories its own way). */
        public final String id;
        private final int goal;

        Set(String id, int goal) {
            this.id = id;
            this.goal = goal;
        }

        /** Gathered memories it needs. */
        public int goal() {
            return this == ALL_VICTORIES ? mainRoad().size() : goal;
        }

        public String key() {
            return "memory.supernaturalcraft.set." + name().toLowerCase(Locale.ROOT);
        }
    }

    private MemorySets() {
    }

    /** The ids of the enemies on the main road (the optional side roads are not needed for All Victories). */
    public static java.util.Set<String> mainRoad() {
        java.util.Set<String> out = new HashSet<>();
        for (BossProgression.Boss b : BossProgression.Boss.values()) if (!b.optional) out.add(b.id());
        return out;
    }

    /** Gathered memories of {@code log} counting towards {@code set}. */
    public static int progress(Set set, Collection<Memory> log, java.util.Set<String> collected) {
        if (set == Set.ALL_VICTORIES) {
            java.util.Set<String> road = mainRoad(), have = new HashSet<>();
            for (Memory m : log) {
                if (m.kind() == MemoryKind.BOSS_VICTORY && collected.contains(m.id()) && road.contains(m.subject())) have.add(m.subject());
            }
            return have.size();
        }
        int n = 0;
        for (Memory m : log) if (m.kind().set.equals(set.id) && collected.contains(m.id())) n++;
        return n;
    }

    public static boolean complete(Set set, Collection<Memory> log, java.util.Set<String> collected) {
        return progress(set, log, collected) >= set.goal();
    }

    /** Every set {@code log} completes. */
    public static EnumSet<Set> completed(Collection<Memory> log, java.util.Set<String> collected) {
        EnumSet<Set> out = EnumSet.noneOf(Set.class);
        for (Set s : Set.values()) if (complete(s, log, collected)) out.add(s);
        return out;
    }

    public static EnumSet<Set> completed(MemoryLog log) {
        return completed(log.entries(), log.collected());
    }

    /** The sets {@code after} completes that {@code before} did not. */
    public static EnumSet<Set> newlyCompleted(MemoryLog before, MemoryLog after) {
        EnumSet<Set> out = completed(after);
        out.removeAll(completed(before));
        return out;
    }
}
