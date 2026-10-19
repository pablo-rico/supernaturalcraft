package org.papiricoh.supernaturalcraft.magic.mastery;

import java.util.Collection;
import java.util.Locale;

/**
 * Magical mastery (v0.21), pure so JUnit can check it ({@code MasteryRulesTest}). Each school trains on its own: casting a sigil
 * gives a fifth of the mana spent as XP, a bowl spell twice that, a rite a third of its mana. XP climbs through three
 * {@link Rank ranks}; each makes that school's magic cheaper and lets its harder pages be learnt (tier 2 needs an Apprentice,
 * tier 3 an Adept, a Master's sigil or spell a Master). Morningstar's Spark (Lucifer's grace taken in) counts as Adept in every
 * school for learning. Knowledge already learnt is never taken away.
 *
 * <p>Max mana: the player gets the mana bonus of their <b>highest</b> rank in any school, not a sum over schools, so mastery adds
 * at most {@code Rank.MASTER.manaBonus} (+20).
 */
public final class MasteryRules {

    private MasteryRules() {
    }

    /** The ranks, lowest first. Their order is part of the comparisons: only append. */
    public enum Rank {
        NONE(0, 0f, 0),
        APPRENTICE(150, 0.05f, 5),
        ADEPT(600, 0.10f, 10),
        MASTER(1800, 0.15f, 20);

        /** XP needed. */
        public final int xp;
        /** Share of a cost in this school saved ({@code magic.ArcanaCost}). */
        public final float discount;
        /** Extra max mana (only the highest rank's counts). */
        public final int manaBonus;

        Rank(int xp, float discount, int manaBonus) {
            this.xp = xp;
            this.discount = discount;
            this.manaBonus = manaBonus;
        }

        /** {@code none}, {@code apprentice}, {@code adept}, {@code master} (lang {@code mastery.supernaturalcraft.<id>}). */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean atLeast(Rank other) {
            return ordinal() >= other.ordinal();
        }

        /** The next rank, or null at {@link #MASTER}. */
        public Rank next() {
            Rank[] all = values();
            return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
        }
    }

    /** XP is capped here (a float keeps the fractions of small casts). */
    public static final float MAX_XP = 1_000_000f;
    /** Mana per XP point for a sigil. */
    public static final float SIGIL_MANA_PER_XP = 5f;
    /** A bowl spell counts this many times a sigil's XP for the same mana. */
    public static final float BOWL_MULTIPLIER = 2f;
    /** Mana per XP point for a rite. */
    public static final float RITUAL_MANA_PER_XP = 3f;

    /** XP for a sigil that cost {@code mana}. */
    public static float sigilXp(float mana) {
        return Math.max(0, mana) / SIGIL_MANA_PER_XP;
    }

    /** XP for a bowl spell that cost {@code mana}. */
    public static float bowlXp(float mana) {
        return sigilXp(mana) * BOWL_MULTIPLIER;
    }

    /** XP for a rite that cost {@code mana}. */
    public static float ritualXp(float mana) {
        return Math.max(0, mana) / RITUAL_MANA_PER_XP;
    }

    /** The rank {@code xp} reaches. */
    public static Rank rank(float xp) {
        Rank out = Rank.NONE;
        for (Rank r : Rank.values()) if (xp >= r.xp) out = r;
        return out;
    }

    /** XP added, capped at {@link #MAX_XP} (never below 0). */
    public static float add(float xp, float gain) {
        return Math.max(0, Math.min(MAX_XP, xp + Math.max(0, gain)));
    }

    /** How far {@code xp} is from its rank to the next (0-1; 1 at Master). */
    public static float progress(float xp) {
        Rank r = rank(xp);
        Rank next = r.next();
        if (next == null) return 1f;
        return Math.max(0, Math.min(1, (xp - r.xp) / (float) (next.xp - r.xp)));
    }

    /** The rank a page of {@code tier} asks for: 1 none, 2 Apprentice, 3 Adept, 4 (a Master's) Master. */
    public static Rank requiredRank(int tier) {
        if (tier <= 1) return Rank.NONE;
        if (tier == 2) return Rank.APPRENTICE;
        if (tier == 3) return Rank.ADEPT;
        return Rank.MASTER;
    }

    /** The rank that counts for learning: Morningstar's Spark lifts anything below Adept to Adept. */
    public static Rank forLearning(Rank rank, boolean spark) {
        return spark && !rank.atLeast(Rank.ADEPT) ? Rank.ADEPT : rank;
    }

    /** Whether a page of {@code tier} can be learnt with {@code rank} in its school (and the Spark or not). */
    public static boolean mayLearn(int tier, Rank rank, boolean spark) {
        return forLearning(rank, spark).atLeast(requiredRank(tier));
    }

    /** The highest of {@code ranks} ({@link Rank#NONE} if empty). */
    public static Rank highest(Collection<Rank> ranks) {
        Rank out = Rank.NONE;
        for (Rank r : ranks) if (r.atLeast(out)) out = r;
        return out;
    }

    /** Extra max mana for these ranks (one per school): the highest rank's bonus only. */
    public static int manaBonus(Collection<Rank> ranks) {
        return highest(ranks).manaBonus;
    }
}
