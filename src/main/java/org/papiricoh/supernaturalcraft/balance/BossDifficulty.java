package org.papiricoh.supernaturalcraft.balance;

/**
 * How the world's difficulty shapes a fight with a great enemy (v0.24, pure, tested in JUnit). A fight takes the
 * difficulty in force when it starts and keeps it to the end ({@code entity.boss.DifficultyLock}), so turning the
 * world to Peaceful halfway through changes nothing.
 *
 * <p>Peaceful is a story mode: bosses still come (vanilla would make them vanish), with half their health, a
 * third of their bite, slow telegraphs and no minions. Hardcore is Hard and then some.
 *
 * @param health  multiplier on true health
 * @param damage  multiplier on every attack the boss makes
 * @param windup  multiplier on each attack's telegraph (lower = less time to react)
 * @param gap     multiplier on the pause between attacks
 * @param recover multiplier on the punish window after each attack
 * @param minions multiplier on how many minions an attack calls (0 = none)
 */
public record BossDifficulty(float health, float damage, float windup, float gap, float recover, float minions) {

    public static final BossDifficulty PEACEFUL = new BossDifficulty(0.5f, 0.3f, 1.4f, 1.6f, 1.5f, 0f);
    public static final BossDifficulty EASY = new BossDifficulty(0.75f, 0.65f, 1.2f, 1.3f, 1.25f, 0.5f);
    public static final BossDifficulty NORMAL = new BossDifficulty(1f, 1f, 1f, 1f, 1f, 1f);
    public static final BossDifficulty HARD = new BossDifficulty(1.3f, 1.3f, 0.85f, 0.8f, 0.85f, 1.25f);
    public static final BossDifficulty HARDCORE = new BossDifficulty(1.5f, 1.5f, 0.8f, 0.7f, 0.8f, 1.5f);

    /** In the order they are saved ({@link #id}). */
    private static final BossDifficulty[] ALL = {PEACEFUL, EASY, NORMAL, HARD, HARDCORE};

    /**
     * The settings for vanilla difficulty {@code difficultyId} (0 Peaceful … 3 Hard, as {@code Difficulty.getId()}),
     * or Hardcore when the world is.
     */
    public static BossDifficulty of(int difficultyId, boolean hardcore) {
        if (hardcore) return HARDCORE;
        return ALL[Math.max(0, Math.min(3, difficultyId))];
    }

    /** Saved form: 0 Peaceful … 4 Hardcore. */
    public int id() {
        for (int i = 0; i < ALL.length; i++) if (ALL[i] == this) return i;
        return 2;
    }

    /** Back from {@link #id}; anything unknown (an older save) is Normal. */
    public static BossDifficulty byId(int id) {
        return id >= 0 && id < ALL.length ? ALL[id] : NORMAL;
    }

    public boolean peaceful() {
        return this == PEACEFUL;
    }

    /** How many of {@code base} minions come: none in Peaceful, never fewer than one otherwise (if any were due). */
    public int minions(int base) {
        if (base <= 0 || minions <= 0) return 0;
        return Math.max(1, (int) Math.ceil(base * minions - 1e-4f));
    }

    /** {@code ticks} scaled by {@code factor}, at least one. */
    public static int ticks(int ticks, float factor) {
        return Math.max(1, Math.round(ticks * factor));
    }
}
