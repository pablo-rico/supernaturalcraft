package org.papiricoh.supernaturalcraft.allegiance;

/**
 * Where Grace and Corruption come from and where they go (pure; the server applies them, config may scale them). Every
 * amount is a share of the bar ({@link Ranks#maxEssence}), so a King of Hell refills as fast as a Crossroads Demon.
 * <p>
 * The bar refills by itself in {@link #NATURAL_REFILL_SECONDS}. An angel standing on consecrated ground refills twice as
 * fast, praying (crouched and still) anywhere three times as fast, and praying on consecrated ground in
 * {@link #FAST_REFILL_SECONDS}. A demon's mirror: standing in fire, crouched and still anywhere in the Nether or Hell,
 * crouched and still in fire, soul fire or lava. Nothing refills a human's bar, a demon starving of a kill, anyone in
 * Amara's darkness, or anyone whose powers are suppressed. On top of that, kills of the other side, a pact with a villager.
 */
public final class EssenceRules {

    /** Slaying a creature of the other side (a demon for an angel; an angel for a demon). */
    public static final float KILL_OPPOSITE_SHARE = 0.08f;
    /** Slaying a boss of the other side (any other boss, half that). */
    public static final float KILL_BOSS_SHARE = 0.5f;
    /** A rival hunter, for a demon. */
    public static final float KILL_HUNTER_SHARE = 0.10f;
    /** A player of the other side (PvP must be on). */
    public static final float KILL_PLAYER_SHARE = 0.75f;
    /** A villager's pact (one per villager). */
    public static final float VILLAGER_PACT_SHARE = 0.20f;
    /** Seconds an untouched bar takes to refill from empty. */
    public static final int NATURAL_REFILL_SECONDS = 300;
    /** Seconds it takes praying on consecrated ground (an angel) or bathing in hellfire (a demon). */
    public static final int FAST_REFILL_SECONDS = 30;
    /** Bloodlust (Knight of Hell and up): share of the bar lost each second after {@link #BLOODLUST_GRACE_SECONDS} without a kill. */
    public static final float BLOODLUST_DRAIN_SHARE = 0.0025f;
    public static final int BLOODLUST_GRACE_SECONDS = 600;
    /** Health lost every {@link #BLOODLUST_HURT_INTERVAL} seconds while starved of a kill (and Corruption is out). */
    public static final float BLOODLUST_HURT = 1f;
    public static final int BLOODLUST_HURT_INTERVAL = 10;
    /** Amara's darkness drains this share of an angel's Grace each second in her arena. */
    public static final float AMARA_DRAIN_SHARE = 0.01f;
    /** Exorcised or banished: what is left. */
    public static final float EXPELLED_KEEPS = 0f;

    /** How a sworn player is resting, for the refill: each one a multiple of the natural rate. */
    public enum Rest {
        /** Anything else. */
        NATURAL(1),
        /** An angel standing on consecrated ground; a demon standing in fire or lava. */
        SANCTUARY(2),
        /** An angel praying away from holy ground; a demon crouched and still in the Nether or Hell. */
        DEVOTION(3),
        /** An angel praying on consecrated ground; a demon crouched and still in fire, soul fire or lava. */
        SANCTUM((float) NATURAL_REFILL_SECONDS / FAST_REFILL_SECONDS);

        public final float factor;

        Rest(float factor) {
            this.factor = factor;
        }
    }

    private EssenceRules() {
    }

    /** {@code share} of a bar of {@code max}. */
    public static float share(float share, int max) {
        return share * max;
    }

    /** Share of the bar for a kill, by the killer's side and what was slain. */
    public static float forKill(Faction killer, Kind slain) {
        if (!killer.supernatural()) return 0;
        return switch (slain) {
            case DEMON -> killer == Faction.ANGEL ? KILL_OPPOSITE_SHARE : 0;
            case ANGEL -> killer == Faction.DEMON ? KILL_OPPOSITE_SHARE : 0;
            case HUNTER -> killer == Faction.DEMON ? KILL_HUNTER_SHARE : 0;
            case BOSS -> KILL_BOSS_SHARE / 2;
            case DEMON_BOSS -> killer == Faction.ANGEL ? KILL_BOSS_SHARE : KILL_BOSS_SHARE / 2;
            case ANGEL_BOSS -> killer == Faction.DEMON ? KILL_BOSS_SHARE : KILL_BOSS_SHARE / 2;
            case OTHER -> 0;
        };
    }

    /** Share of the bar for slaying another player: only the other side's, only with PvP. */
    public static float forPlayerKill(Faction killer, Faction slain, boolean pvp) {
        return pvp && killer.supernatural() && slain == killer.opposite() ? KILL_PLAYER_SHARE : 0;
    }

    /** An angel's rest: praying (crouched and still) and/or on consecrated ground. */
    public static Rest angelRest(boolean kneeling, boolean consecrated) {
        if (kneeling) return consecrated ? Rest.SANCTUM : Rest.DEVOTION;
        return consecrated ? Rest.SANCTUARY : Rest.NATURAL;
    }

    /** A demon's rest: crouched and still, in fire or lava, in the Nether or Hell. */
    public static Rest demonRest(boolean kneeling, boolean inHellfire, boolean infernal) {
        if (kneeling) return inHellfire ? Rest.SANCTUM : infernal ? Rest.DEVOTION : Rest.NATURAL;
        return inHellfire ? Rest.SANCTUARY : Rest.NATURAL;
    }

    /** Share of the bar refilled each second resting so. */
    public static float regenShare(Rest rest) {
        return rest.factor / NATURAL_REFILL_SECONDS;
    }

    /** Nothing refills while suppressed, starving of a kill (bloodlust) or in Amara's darkness. */
    public static boolean regenBlocked(boolean suppressed, boolean starving, boolean inDarkness) {
        return suppressed || starving || inDarkness;
    }

    /** Essence refilled each second for a bar of {@code max} (none for a human or when blocked). */
    public static float regenPerSecond(Faction faction, int max, Rest rest, boolean blocked) {
        if (!faction.supernatural() || blocked) return 0;
        return share(regenShare(rest), max);
    }

    /** Whether a Knight of Hell's bloodlust bites, {@code secondsSinceKill} after the last kill. */
    public static boolean starving(Faction faction, int rank, int secondsSinceKill) {
        return faction == Faction.DEMON && rank >= 3 && secondsSinceKill > BLOODLUST_GRACE_SECONDS;
    }

    /** What was slain, as far as essence is concerned. */
    public enum Kind {
        DEMON, ANGEL, HUNTER, BOSS, DEMON_BOSS, ANGEL_BOSS, OTHER
    }
}
