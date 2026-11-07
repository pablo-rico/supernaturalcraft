package org.papiricoh.supernaturalcraft.allegiance;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerBossDamage;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;

import static org.junit.jupiter.api.Assertions.*;

/** v0.38: faction powers against bosses are priced in blows of the reference weapon at the boss's tier. */
class PowerBossDamageTest {

    /** Most reference blows a power may deal a boss in a minute of casting it off cooldown. */
    private static final float MAX_HITS_PER_MINUTE = 10f;
    /** Most of a boss's designed blows one cast may take (bosses of a tier at least the power's rank: where it is met). */
    private static final float MAX_SHARE_PER_CAST = 0.06f;

    @Test
    void aBlowIsTheReferenceWeaponAtTheBossTier() {
        for (int tier = 1; tier <= ProgressionScale.MAX_TIER; tier++) {
            assertEquals(PowerBossDamage.SMITE * ProgressionScale.referenceHit(tier),
                    PowerBossDamage.damage(Power.SMITE, tier, Power.SMITE.minRank), 1e-3, "tier " + tier);
        }
    }

    @Test
    void itGrowsWithTheBossNotTheCaster() {
        for (Power p : Power.values()) {
            if (PowerBossDamage.hits(p) <= 0) continue;
            for (int tier = 2; tier <= ProgressionScale.MAX_TIER; tier++) {
                assertTrue(PowerBossDamage.damage(p, tier, p.minRank) > PowerBossDamage.damage(p, tier - 1, p.minRank), p + " tier " + tier);
            }
        }
    }

    @Test
    void rankAddsALittle() {
        assertEquals(1f, PowerBossDamage.rankBonus(2, 2), 1e-6);
        assertEquals(1f, PowerBossDamage.rankBonus(1, 2), 1e-6, "never below the power's own rank");
        assertEquals(1f + 2 * PowerBossDamage.RANK_BONUS, PowerBossDamage.rankBonus(4, 2), 1e-6);
    }

    @Test
    void noPowerOutpacesTheWeapon() {
        for (Power p : Power.values()) {
            float cast = PowerBossDamage.hitsPerCast(p);
            if (cast <= 0) continue;
            assertTrue(p.cooldown > 0, p + " strikes bosses and needs a cooldown");
            float perMinute = cast * 1200f / p.cooldown * PowerBossDamage.rankBonus(p.faction.maxRank(), p.minRank);
            assertTrue(perMinute <= MAX_HITS_PER_MINUTE, p + " deals " + perMinute + " reference blows a minute");
        }
    }

    @Test
    void oneCastIsASliverOfAnyBoss() {
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            ProgressionScale.BossStats stats = ProgressionScale.of(b);
            for (Power p : Power.values()) {
                if (stats.tier() < p.minRank) continue;
                float cast = PowerBossDamage.hitsPerCast(p) * PowerBossDamage.rankBonus(p.faction.maxRank(), p.minRank);
                assertTrue(cast <= stats.targetHits() * MAX_SHARE_PER_CAST, p + " takes too much of " + b + " in one cast");
            }
        }
    }
}
