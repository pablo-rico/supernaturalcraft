package org.papiricoh.supernaturalcraft.balance;

import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;

/**
 * {@link ProgressionScale} as the server's config tunes it (v0.15): what code in game asks for. Pure numbers
 * and their tests stay in {@code ProgressionScale}.
 */
public final class Balance {

    private Balance() {
    }

    /** A great enemy's true health with one challenger, config included. */
    public static float bossHealth(Boss boss) {
        return ProgressionScale.trueHealth(boss) * SNConfig.BOSS_HEALTH_MULTIPLIER.get().floatValue();
    }

    /** What every attack of a great enemy is multiplied by, config included. */
    public static float bossDamage(Boss boss) {
        return ProgressionScale.of(boss).damageMultiplier() * SNConfig.BOSS_DAMAGE_MULTIPLIER.get().floatValue();
    }

    /** Optional modpack safety; zero leaves an ordinary blow unchanged. */
    public static float perHitLimit(float amount, float maxTrue) {
        float fraction = SNConfig.PER_HIT_CAP_FRACTION.get().floatValue();
        return fraction > 0 ? Math.min(amount, fraction * maxTrue) : amount;
    }

    public static ProgressionScale.Split split(float total) {
        return ProgressionScale.split(total, SNConfig.DIVINE_FRACTION.get().floatValue());
    }

    /** Multiplier of a weapon at Ascension {@code level} (and of a hunter's spells on bosses at that tier), config included. */
    public static float ascension(int level) {
        float m = ProgressionScale.ascensionMultiplier(level);
        return 1f + (m - 1f) * SNConfig.PLAYER_DAMAGE_MULTIPLIER.get().floatValue();
    }

    public static int vitalityHearts(Boss boss) {
        return ProgressionScale.vitalityHearts(boss, SNConfig.VITALITY_HEARTS.get());
    }
}
