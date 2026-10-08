package org.papiricoh.supernaturalcraft.allegiance;

/**
 * Where Grace and Corruption come from and where they go (pure; the server applies them, config may scale the gains).
 * Grace: slaying demons, praying (crouched and still on consecrated ground) and, slowly, just standing on it. Corruption:
 * slaying angels and rival hunters, and pacts with villagers. A kill of the other side's player (only where PvP is on)
 * is worth the most. Nothing regenerates on its own otherwise.
 */
public final class EssenceRules {

    /** Slaying a creature of the other side (a demon for an angel; an angel for a demon). */
    public static final float KILL_OPPOSITE = 12f;
    /** Slaying a boss of the other side (or any boss, half that). */
    public static final float KILL_BOSS = 80f;
    /** A rival hunter, for a demon. */
    public static final float KILL_HUNTER = 15f;
    /** A player of the other side (PvP must be on). */
    public static final float KILL_PLAYER = 120f;
    /** Each second of prayer on consecrated ground. */
    public static final float PRAY_PER_SECOND = 3f;
    /** Each second near consecrated ground (not praying). */
    public static final float SANCTUARY_PER_SECOND = 0.4f;
    /** A villager's pact (one per villager). */
    public static final float VILLAGER_PACT = 30f;
    /** Bloodlust (Knight of Hell and up): Corruption lost each second after {@link #BLOODLUST_GRACE_SECONDS} without a kill. */
    public static final float BLOODLUST_DRAIN_PER_SECOND = 0.5f;
    public static final int BLOODLUST_GRACE_SECONDS = 600;
    /** Health lost every {@link #BLOODLUST_HURT_INTERVAL} seconds while starved of a kill (and Corruption is out). */
    public static final float BLOODLUST_HURT = 1f;
    public static final int BLOODLUST_HURT_INTERVAL = 10;
    /** Amara's darkness drains an angel's Grace each second in her arena. */
    public static final float AMARA_DRAIN_PER_SECOND = 1.5f;
    /** Exorcised or banished: what is left. */
    public static final float EXPELLED_KEEPS = 0f;

    private EssenceRules() {
    }

    /** Essence for a kill, by the killer's side and what was slain. */
    public static float forKill(Faction killer, Kind slain) {
        if (!killer.supernatural()) return 0;
        return switch (slain) {
            case DEMON -> killer == Faction.ANGEL ? KILL_OPPOSITE : 0;
            case ANGEL -> killer == Faction.DEMON ? KILL_OPPOSITE : 0;
            case HUNTER -> killer == Faction.DEMON ? KILL_HUNTER : 0;
            case BOSS -> KILL_BOSS / 2;
            case DEMON_BOSS -> killer == Faction.ANGEL ? KILL_BOSS : KILL_BOSS / 2;
            case ANGEL_BOSS -> killer == Faction.DEMON ? KILL_BOSS : KILL_BOSS / 2;
            case OTHER -> 0;
        };
    }

    /** Essence for slaying another player: only the other side's, only with PvP. */
    public static float forPlayerKill(Faction killer, Faction slain, boolean pvp) {
        return pvp && killer.supernatural() && slain == killer.opposite() ? KILL_PLAYER : 0;
    }

    /** Grace per second for an angel: praying on consecrated ground, near it, or nothing. */
    public static float sanctuary(boolean onConsecrated, boolean praying) {
        if (!onConsecrated) return 0;
        return praying ? PRAY_PER_SECOND : SANCTUARY_PER_SECOND;
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
