package org.papiricoh.supernaturalcraft.memory;

import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What becomes a memory, and under which id (v0.18, pure). Every way into the log (live events, the hooks, the backfill of an
 * old save) builds its memories here, so the same moment always gets the same id and is never remembered twice.
 */
public final class MemoryRules {

    /** First sightings remembered at most (the rest of the bestiary stays in the book). */
    public static final int MAX_SIGHTINGS = 40;
    /** Favourite prey remembered at most, and the kills a creature needs to be one. */
    public static final int MAX_PREY = 5, PREY_KILLS = 10;
    /** Milliseconds per game tick, to guess when a moment only known by its game time happened. */
    public static final long MILLIS_PER_TICK = 50;

    private static final Pattern RANK = Pattern.compile("(angel|demon|hunter)_(\\d)");
    private static final Pattern LEGACY = Pattern.compile("legacy_(\\d)");

    /** Oldest first: by real time, then game time (a memory with neither sorts first, as the earliest). Stable. */
    public static final Comparator<Memory> CHRONOLOGICAL = Comparator.comparingLong(Memory::realTime).thenComparingLong(Memory::gameTime);

    private MemoryRules() {
    }

    /**
     * The memory one of the mod's advancements stands for, or null.
     *
     * @param path the advancement's path in the mod's namespace ({@code main/yellow_eyed})
     */
    public static @Nullable Memory fromAdvancement(String path, long gameTime, long realTime) {
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            if (b.advancement.equals(path)) return boss(b, gameTime, realTime);
        }
        if (!path.startsWith("main/")) return null;
        String name = path.substring(5);
        if (name.equals("heeded_the_call")) {
            return new Memory("call:messenger", MemoryKind.HEEDED_CALL, "messenger", "", gameTime, realTime, 0);
        }
        Matcher rank = RANK.matcher(name);
        if (rank.matches()) return rank(rank.group(1), Integer.parseInt(rank.group(2)), gameTime, realTime);
        Matcher legacy = LEGACY.matcher(name);
        if (legacy.matches()) return legacyRank(Integer.parseInt(legacy.group(1)), gameTime, realTime);
        return null;
    }

    /** Every advancement path {@link #fromAdvancement} knows (bosses, ranks, the call, the Men of Letters). */
    public static List<String> advancements() {
        List<String> out = new ArrayList<>();
        for (BossProgression.Boss b : BossProgression.Boss.values()) out.add(b.advancement);
        out.add("main/heeded_the_call");
        for (int i = 1; i <= 4; i++) out.add("main/angel_" + i);
        for (int i = 1; i <= 4; i++) out.add("main/demon_" + i);
        for (int i = 1; i <= 3; i++) out.add("main/hunter_" + i);
        for (int i = 1; i <= 5; i++) out.add("main/legacy_" + i);
        return out;
    }

    public static Memory boss(BossProgression.Boss boss, long gameTime, long realTime) {
        return new Memory("boss:" + boss.id(), MemoryKind.BOSS_VICTORY, boss.id(), "", gameTime, realTime, 0);
    }

    /** @param side {@code angel}, {@code demon} or {@code hunter} */
    public static Memory rank(String side, int rank, long gameTime, long realTime) {
        return new Memory("rank:" + side + "_" + rank, MemoryKind.ASCENSION, side, "", gameTime, realTime, rank);
    }

    public static Memory legacyRank(int rank, long gameTime, long realTime) {
        return new Memory("legacy:" + rank, MemoryKind.LEGACY_RANK, "men_of_letters", "", gameTime, realTime, rank);
    }

    /**
     * The {@code n}-th deal a hunter made ({@code n} = the deals already remembered).
     *
     * @param arg the wish's argument; a number becomes the memory's variant
     */
    public static Memory deal(int n, String wish, String arg, boolean wild, long gameTime, long realTime) {
        int variant = 0;
        try {
            variant = arg == null || arg.isEmpty() ? 0 : Integer.parseInt(arg.trim());
        } catch (NumberFormatException ignored) {
        }
        return new Memory("deal:" + n, MemoryKind.CROSSROADS_DEAL, wish == null ? "" : wish, wild ? "wild" : "bowl", gameTime, realTime, variant);
    }

    public static Memory caseClosed(int index, String monster, String scenario, boolean solved, long gameTime, long realTime) {
        return new Memory("case:" + index, solved ? MemoryKind.CASE_SOLVED : MemoryKind.CASE_LOST, monster, scenario, gameTime, realTime, index);
    }

    /** @param pet the pet's UUID as a string */
    public static Memory petLost(String pet, String type, String name, long gameTime, long realTime) {
        return new Memory("pet:" + pet, MemoryKind.PET_LOST, type, name == null ? "" : name, gameTime, realTime, 0);
    }

    public static Memory sighting(String type, long gameTime, long realTime) {
        return new Memory("seen:" + type, MemoryKind.FIRST_SIGHTING, type, "", gameTime, realTime, 0);
    }

    public static Memory prey(String type, int kills, long gameTime, long realTime) {
        return new Memory("prey:" + type, MemoryKind.FAVOURITE_PREY, type, "", gameTime, realTime, kills);
    }

    /** Memories of {@code kind} in {@code log}. */
    public static int count(Collection<Memory> log, MemoryKind kind) {
        int n = 0;
        for (Memory m : log) if (m.kind() == kind) n++;
        return n;
    }

    /** When something that happened at {@code gameTime} probably happened, by the clock, given the time now. */
    public static long estimateRealTime(long gameTime, long nowGame, long nowReal) {
        if (gameTime <= 0 || gameTime > nowGame) return nowReal;
        return Math.max(1, nowReal - (nowGame - gameTime) * MILLIS_PER_TICK);
    }

    /** {@code memories} oldest first (a copy). */
    public static List<Memory> chronological(Collection<Memory> memories) {
        List<Memory> out = new ArrayList<>(memories);
        out.sort(CHRONOLOGICAL);
        return out;
    }
}
