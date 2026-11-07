package org.papiricoh.supernaturalcraft.purgatory;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickBalance;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveAssets;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveBalance;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Eve's numbers (v0.39): phases without gaps, pressure that rises, and blows in her tier's budget. */
class EveBalanceTest {

    @Test
    void phasesSplitHerHealthWithoutGaps() {
        assertEquals(4, EveBalance.PHASES);
        float last = 1f;
        for (int p = 1; p < EveBalance.PHASES; p++) {
            float t = EveBalance.threshold(p);
            assertEquals(last - 1f / EveBalance.PHASES, t, 1e-6f, "phase " + p + " ends a quarter further down");
            last = t;
        }
        assertEquals(0f, EveBalance.threshold(EveBalance.PHASES));
    }

    @Test
    void thePressureRisesEachPhase() {
        for (int p = 2; p <= EveBalance.PHASES; p++) {
            assertTrue(EveBalance.attackGap(p) < EveBalance.attackGap(p - 1), "phase " + p + " attacks sooner");
        }
        assertTrue(EveBalance.LAST_SWARMS > 1, "two lanes of roaches in the last phase");
    }

    @Test
    void herBlowsAreInHerTiersBudget() {
        assertEquals(4, ProgressionScale.of(BossProgression.Boss.EVE).tier(), "she is a tier-4 boss");
        float heaviest = 0, lightest = Float.MAX_VALUE;
        for (float b : EveBalance.BLOWS) {
            heaviest = Math.max(heaviest, b);
            lightest = Math.min(lightest, b);
        }
        // Her tier's neighbours on the road: Dick Roman's bite and Raphael's snap.
        float ref = Math.max(DickBalance.BITE_DAMAGE, RaphaelBalance.SNAP_DAMAGE);
        assertTrue(heaviest <= ref, "her heaviest blow " + heaviest + " stays within her tier's (" + ref + ")");
        assertTrue(heaviest >= 0.75f * ref, "and is not much softer than theirs: " + heaviest);
        assertTrue(lightest >= 8f, "no blow is a tickle: " + lightest);
        // The swarm bites in short beats: each beat alone is far under a blow.
        assertTrue(EveBalance.SWARM_DAMAGE * 3 < EveBalance.LASH_DAMAGE);
        assertTrue(EveBalance.CHILD_HEALTH < 40 && EveBalance.CHILD_DAMAGE < 8, "her children are frail");
    }

    @Test
    void herWindupsLetTheClipsLand() {
        assertTrue(EveBalance.LASH_WINDUP >= EveBalance.cue("lash"));
        assertTrue(EveBalance.GRASP_WINDUP >= EveBalance.cue("root_grasp"));
        assertTrue(EveBalance.BIRTH_WINDUP >= EveBalance.cue("birth"));
        assertTrue(EveBalance.SWARM_WINDUP >= EveBalance.cue("roach_swarm"));
        assertTrue(EveBalance.CALL_WINDUP >= EveBalance.cue("call"));
        assertTrue(EveBalance.THORN_WINDUP >= EveBalance.cue("thorn_ring"));
        assertTrue(EveBalance.FIELD_WINDUP >= EveBalance.cue("root_field"));
        assertEquals(EveAssets.CLIP_TICKS.get("death"), EveBalance.DEATH_TICKS, "her death lasts its clip");
        assertTrue(EveBalance.THORN_INNER < EveBalance.THORN_OUTER);
    }
}
