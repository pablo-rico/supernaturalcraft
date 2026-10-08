package org.papiricoh.supernaturalcraft.balance;

import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The whole power curve of the mod in one place (v0.15, pure, tested in JUnit): how much true health each
 * great enemy has, how hard it hits, which Ascension Shard it leaves, how far a weapon's Ascension multiplies
 * it, and the soft cap every boss puts on a single blow.
 *
 * <p>The scale ends at an endgame on par with Draconic Evolution's old Chaos Guardian: the Author has about
 * 100 000 health and no blow, however large, takes more than {@link #DEFAULT_HARD_CAP} of it, so even a
 * weapon from another mod needs dozens of hits. Config ({@code SNConfig} section {@code balance}) scales and
 * tunes these; nothing here reads it.
 */
public final class ProgressionScale {

    private ProgressionScale() {
    }

    // --- Tiers --------------------------------------------------------------------------------------------------------

    /** Highest Ascension (and shard) tier. */
    public static final int MAX_TIER = 5;

    /**
     * One great enemy's numbers.
     *
     * @param trueHealth       true health with one challenger (before config and the per-player bonus)
     * @param tier             the stage of the curve it belongs to (1-5; the Author is 5 too)
     * @param damageMultiplier what every attack it makes is multiplied by
     * @param shardTier        the Ascension Shard it leaves (0 = none)
     */
    public record BossStats(float trueHealth, int tier, float damageMultiplier, int shardTier) {
    }

    private static final Map<Boss, BossStats> STATS = new EnumMap<>(Boss.class);

    static {
        STATS.put(Boss.AZAZEL, new BossStats(5_000, 1, 1.5f, 2));
        STATS.put(Boss.LILITH, new BossStats(8_000, 1, 2.0f, 2));
        STATS.put(Boss.LUCIFER, new BossStats(15_000, 2, 3.0f, 3));
        STATS.put(Boss.GABRIEL, new BossStats(18_000, 2, 3.5f, 3));
        STATS.put(Boss.WAR, new BossStats(20_000, 3, 4.0f, 3));
        STATS.put(Boss.FAMINE, new BossStats(20_000, 3, 4.0f, 3));
        STATS.put(Boss.PESTILENCE, new BossStats(20_000, 3, 4.0f, 3));
        STATS.put(Boss.BROKEN_CHORUS, new BossStats(28_000, 4, 4.5f, 4));
        STATS.put(Boss.METATRON, new BossStats(40_000, 4, 5.0f, 4));
        STATS.put(Boss.AMARA, new BossStats(45_000, 4, 5.5f, 4));
        STATS.put(Boss.DEATH, new BossStats(45_000, 4, 5.0f, 4));
        STATS.put(Boss.LUCIFER_UNCAGED, new BossStats(65_000, 5, 7.0f, 5));
        STATS.put(Boss.MICHAEL, new BossStats(65_000, 5, 7.0f, 5));
        STATS.put(Boss.CHUCK, new BossStats(100_000, 5, 8.0f, 0));
    }

    public static BossStats of(Boss boss) {
        return STATS.get(boss);
    }

    /** True health for {@code players} challengers: {@code base * (1 + perExtra * (players - 1))}. */
    public static float healthFor(float base, int players, float perExtraPlayer) {
        return base * (1f + perExtraPlayer * Math.max(0, players - 1));
    }

    // --- The soft cap on a single blow --------------------------------------------------------------------------------

    /** Share of a boss's true max health above which a blow keeps only {@link #DEFAULT_EXCESS_KEEP} of the excess. */
    public static final float DEFAULT_SOFT_CAP = 0.01f;
    /** Share of a boss's true max health no single blow may ever take. */
    public static final float DEFAULT_HARD_CAP = 0.015f;
    /** What is kept of a blow above the soft cap. */
    public static final float DEFAULT_EXCESS_KEEP = 0.3f;

    /**
     * What is left of a blow of {@code amount} true damage once a boss with {@code maxTrue} true max health
     * caps it: unchanged up to {@code soft * maxTrue}, then {@code keep} of the excess, never more than
     * {@code hard * maxTrue}. Continuous and non-decreasing in {@code amount}.
     */
    public static float softCap(float amount, float maxTrue, float soft, float hard, float keep) {
        if (amount <= 0) return 0;
        float s = soft * maxTrue, h = Math.max(s, hard * maxTrue);
        float capped = amount <= s ? amount : s + (amount - s) * keep;
        return Math.min(capped, h);
    }

    public static float softCap(float amount, float maxTrue) {
        return softCap(amount, maxTrue, DEFAULT_SOFT_CAP, DEFAULT_HARD_CAP, DEFAULT_EXCESS_KEEP);
    }

    /** The most one blow can take from a boss with {@code maxTrue} true max health. */
    public static float hardCap(float maxTrue, float hard) {
        return hard * maxTrue;
    }

    // --- What bosses deal ---------------------------------------------------------------------------------------------

    /** Share of every boss blow dealt as Divine Wrath (ignores armour, enchantments, effects and shields). */
    public static final float DEFAULT_DIVINE_FRACTION = 0.15f;

    /** A boss blow of {@code total} split into its ordinary part and its Divine Wrath. */
    public record Split(float ordinary, float divine) {
    }

    public static Split split(float total, float divineFraction) {
        float d = Math.max(0, total) * divineFraction;
        return new Split(Math.max(0, total) - d, d);
    }

    // --- Ascension ----------------------------------------------------------------------------------------------------

    /** Multiplier on a weapon's base damage by Ascension level (0 = never ascended). */
    private static final float[] ASCENSION = {1f, 6f, 15f, 30f, 55f, 90f};

    public static float ascensionMultiplier(int level) {
        return ASCENSION[clampTier(level)];
    }

    /** Whether a shard of {@code shardTier} ascends a weapon now at {@code level}: only the next tier does. */
    public static boolean shardFits(int level, int shardTier) {
        return level < MAX_TIER && shardTier == level + 1;
    }

    // --- The hunter's tier (spells and powers against bosses) ---------------------------------------------------------

    /**
     * A hunter's tier from the great enemies they have beaten: the highest shard tier those enemies leave,
     * never below 1 (the first shard is made by a rite before any boss). Spells and faction powers hit
     * bosses with {@link #ascensionMultiplier} of it.
     *
     * @param beaten whether the advancement with this path (e.g. {@code main/yellow_eyed}) is done
     */
    public static int playerTier(Predicate<String> beaten) {
        int tier = 1;
        for (Boss b : Boss.values()) {
            if (beaten.test(b.advancement)) tier = Math.max(tier, STATS.get(b).shardTier());
        }
        if (beaten.test(Boss.CHUCK.advancement)) tier = MAX_TIER;
        return tier;
    }

    // --- The hunter's defence -----------------------------------------------------------------------------------------

    /** Hearts of max health the first victory over a great enemy gives (optional ones give half, the Author none). */
    public static final int DEFAULT_VITALITY_HEARTS = 2;

    public static int vitalityHearts(Boss boss, int heartsPerBoss) {
        if (boss == Boss.CHUCK) return 0;
        return boss.optional ? Math.max(1, heartsPerBoss / 2) : heartsPerBoss;
    }

    /** Aegis (share of boss damage turned aside, Divine Wrath included) by armour Ascension level, per full set. */
    private static final float[] ARMOR_AEGIS = {0f, 0.05f, 0.10f, 0.15f, 0.20f, 0.30f};
    /** Aegis never turns aside more than this, whatever its sources. */
    public static final float MAX_AEGIS = 0.6f;

    public static float armorAegis(int level) {
        return ARMOR_AEGIS[clampTier(level)];
    }

    /** Aegis from every source, capped at {@link #MAX_AEGIS}. */
    public static float totalAegis(float... sources) {
        float sum = 0;
        for (float s : sources) sum += Math.max(0, s);
        return Math.min(MAX_AEGIS, sum);
    }

    /** What is left of a boss blow once {@code aegis} turns part of it aside. */
    public static float applyAegis(float amount, float aegis) {
        return amount * (1f - Math.min(MAX_AEGIS, Math.max(0, aegis)));
    }

    private static int clampTier(int level) {
        return Math.max(0, Math.min(MAX_TIER, level));
    }
}
