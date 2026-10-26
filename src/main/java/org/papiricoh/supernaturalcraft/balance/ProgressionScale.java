package org.papiricoh.supernaturalcraft.balance;

import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The whole power curve of the mod in one place (pure, tested in JUnit): how much true health each great enemy
 * has, how hard it hits, which Ascension Shard it leaves and how far a weapon's Ascension multiplies it.
 *
 * <p>v0.24: bosses are designed by damage. Each one has a number of {@linkplain BossStats#targetHits target hits}:
 * blows of the {@linkplain #referenceHit reference weapon} (the Angel Blade ascended to the boss's tier, before the
 * arsenal's x2) it takes at Normal with one challenger; the real, doubled blade needs about half. Its true health follows from that ({@link #trueHealth}). There is no per-hit cap
 * any more: crits, runes, bane and better blades shorten the fight, and only the phase floor (no blow skips a
 * phase) and the Colt's own cap ({@code BossDamage.coltCap}) limit a single blow. Config ({@code SNConfig} section
 * {@code balance}) and the world's difficulty ({@link BossDifficulty}) scale these; nothing here reads them.
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
     * @param targetHits       reference melee blows with one challenger before config
     * @param tier             the stage of the curve it belongs to (1-5; the Author is 5 too)
     * @param damageMultiplier what every attack it makes is multiplied by
     * @param shardTier        the Ascension Shard it leaves (0 = none)
     */
    public record BossStats(int targetHits, int tier, float damageMultiplier, int shardTier) {
    }

    private static final Map<Boss, BossStats> STATS = new EnumMap<>(Boss.class);

    static {
        STATS.put(Boss.AZAZEL, new BossStats(35, 1, 1.1f, 2));
        STATS.put(Boss.LILITH, new BossStats(45, 1, 1.4f, 2));
        STATS.put(Boss.LUCIFER, new BossStats(55, 2, 2.4f, 3));
        STATS.put(Boss.GABRIEL, new BossStats(55, 2, 2.8f, 3));
        STATS.put(Boss.CAIN, new BossStats(55, 2, 2.8f, 3));
        STATS.put(Boss.WAR, new BossStats(65, 3, 4.0f, 3));
        STATS.put(Boss.FAMINE, new BossStats(65, 3, 4.0f, 3));
        STATS.put(Boss.PESTILENCE, new BossStats(65, 3, 4.0f, 3));
        STATS.put(Boss.RAPHAEL, new BossStats(68, 3, 4.5f, 4));
        STATS.put(Boss.DICK_ROMAN, new BossStats(70, 3, 4.6f, 4));
        STATS.put(Boss.ABADDON, new BossStats(67, 3, 4.5f, 4));
        STATS.put(Boss.ROWENA, new BossStats(70, 3, 4.5f, 4));
        STATS.put(Boss.GRAND_WITCH, new BossStats(70, 3, 4.5f, 4));
        STATS.put(Boss.BROKEN_CHORUS, new BossStats(80, 4, 4.5f, 4));
        STATS.put(Boss.METATRON, new BossStats(85, 4, 5.0f, 4));
        STATS.put(Boss.NAOMI, new BossStats(82, 4, 5.0f, 4));
        STATS.put(Boss.ZACHARIAH, new BossStats(90, 4, 5.5f, 4));
        STATS.put(Boss.AMARA, new BossStats(88, 4, 5.5f, 4));
        STATS.put(Boss.DEATH, new BossStats(88, 4, 5.0f, 4));
        STATS.put(Boss.LUCIFER_UNCAGED, new BossStats(100, 5, 7.0f, 5));
        STATS.put(Boss.MICHAEL, new BossStats(100, 5, 7.0f, 5));
        STATS.put(Boss.CHUCK, new BossStats(130, 5, 8.0f, 0));
    }

    public static BossStats of(Boss boss) {
        return STATS.get(boss);
    }

    /** True health for {@code players} challengers: {@code base * (1 + perExtra * (players - 1))}. */
    public static float healthFor(float base, int players, float perExtraPlayer) {
        return base * (1f + perExtraPlayer * Math.max(0, players - 1));
    }

    /**
     * The blow boss health is measured in: the Angel Blade ascended to {@code tier}, as it was before the arsenal's
     * damage was doubled ({@link WeaponDamage#WEAPON_DAMAGE_SCALE}), plus the player's own point. The doubling is
     * the hunter's edge, not the bosses': the real blade ({@link #bladeHit}) needs about half the target hits.
     */
    public static float referenceHit(int tier) {
        return 1f + WeaponDamage.angelBladeBaseModifier() * ascensionMultiplier(tier);
    }

    /** The real Angel Blade's ascended melee blow (damage scale included), plus the player's own point. */
    public static float bladeHit(int tier) {
        return 1f + WeaponDamage.angelBladeModifier() * ascensionMultiplier(tier);
    }

    /**
     * How much of the Angel Blade's demon-bane a boss's health accounts for: {@link #EXPECTED_BANE} for the bosses
     * in {@code #supernaturalcraft:demons} (what {@code CombatEvents} counts; Lucifer is tagged an archangel and the
     * Horsemen are neither), 1 for the rest. Only half the bane's x3 is priced in, so the right blade still halves
     * the fight while spells, powers and other blades are not left facing a triple sponge.
     */
    public static float bane(Boss boss) {
        return switch (boss) {
            case AZAZEL, LILITH, CAIN, ABADDON -> EXPECTED_BANE;
            default -> 1f;
        };
    }

    /** Share of {@link WeaponDamage#ANGEL_BLADE_BANE} a demon boss's health assumes. */
    public static final float EXPECTED_BANE = 1.5f;

    /** True health with one challenger, before config and difficulty: target hits x reference hit x bane, to 100. */
    public static float trueHealth(Boss boss) {
        BossStats stats = of(boss);
        return Math.round(stats.targetHits() * referenceHit(stats.tier()) * bane(boss) / 100f) * 100f;
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
