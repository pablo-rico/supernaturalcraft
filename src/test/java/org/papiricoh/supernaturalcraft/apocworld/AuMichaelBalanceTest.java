package org.papiricoh.supernaturalcraft.apocworld;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.apocworld.army.AuArmyRules;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelAssets;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelBalance;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AU Michael's numbers (v0.41): four phases without gaps, readable windups that hold the art's moments, a flight in order; his army's. */
class AuMichaelBalanceTest {

    @Test
    void fourPhasesWithoutGaps() {
        assertEquals(4, AuMichaelBalance.PHASES);
        float last = 1f;
        for (int p = 1; p <= AuMichaelBalance.PHASES; p++) {
            float t = AuMichaelBalance.threshold(p);
            assertTrue(t < last, "phase " + p + " ends below where it began");
            last = t;
        }
        assertEquals(0f, AuMichaelBalance.threshold(AuMichaelBalance.PHASES));
        assertTrue(AuMichaelBalance.attackGap(1) > AuMichaelBalance.attackGap(4), "he quickens");
        assertTrue(AuMichaelBalance.aerial(4) && !AuMichaelBalance.aerial(3), "only the last phase is in the air");
        assertTrue(!AuMichaelBalance.wings(1) && AuMichaelBalance.wings(2), "his wings open at the second");
    }

    @Test
    void everyWindupLeavesTimeToReadTheGroundAndHoldsItsClipsMoment() {
        Map<String, Integer> windups = Map.of("blade_slash", AuMichaelBalance.SLASH_WINDUP, "blade_combo", AuMichaelBalance.COMBO_WINDUP,
                "smite", Math.min(AuMichaelBalance.SMITE_WINDUP, AuMichaelBalance.RAIN_WINDUP), "wing_beat", AuMichaelBalance.GALE_WINDUP,
                "command", AuMichaelBalance.COMMAND_WINDUP, "lightning", Math.min(AuMichaelBalance.BOLT_WINDUP, AuMichaelBalance.LANCE_WINDUP));
        for (var e : windups.entrySet()) {
            assertTrue(e.getValue() >= 16, "a windup of " + e.getValue() + " ticks is too short to step out of");
            assertTrue(e.getValue() >= AuMichaelBalance.cue(e.getKey()), e.getKey() + "'s moment must land by the windup's end");
            assertTrue(AuMichaelAssets.CLIPS.contains(e.getKey()), e.getKey() + " is one of his clips");
        }
        for (var e : AuMichaelAssets.HIT_TICKS.entrySet()) {
            Integer len = AuMichaelAssets.CLIP_TICKS.get(e.getKey());
            assertTrue(len != null && e.getValue() < len, e.getKey() + "'s moment falls inside its clip");
        }
    }

    @Test
    void hisFlightRunsInOrderAndMatchesTheClip() {
        assertTrue(0 < AuMichaelBalance.JACK_AT && AuMichaelBalance.JACK_AT < AuMichaelBalance.TURN_AT
                && AuMichaelBalance.TURN_AT < AuMichaelBalance.GONE_AT && AuMichaelBalance.GONE_AT <= AuMichaelBalance.FLEE_TICKS);
        assertEquals((int) AuMichaelAssets.HIT_TICKS.get("rift_flee"), AuMichaelBalance.GONE_AT - AuMichaelBalance.TURN_AT,
                "he is gone at the rift_flee clip's moment");
        assertTrue(AuMichaelBalance.FLEE_RIFT_TICKS >= 20 * 60, "the rift he leaves stays long enough to follow him home");
        assertTrue(AuMichaelBalance.MAX_SOLDIERS >= AuMichaelBalance.SOLDIERS_PER_CALL);
    }

    @Test
    void hisArmysPatrolsStayFewAndSmall() {
        for (double roll = 0; roll < 1; roll += 0.01) {
            int n = AuArmyRules.patrolSize(roll);
            assertTrue(n >= AuArmyRules.PATROL_MIN && n <= AuArmyRules.PATROL_MAX, "a squad of " + n);
        }
        assertEquals(AuArmyRules.PATROL_MAX, AuArmyRules.patrolSize(0.9999));
        assertTrue(!AuArmyRules.patrolComes(AuArmyRules.PATROL_CAP, 0), "none while enough already stand round the hunter");
        assertTrue(AuArmyRules.patrolComes(0, 0) && !AuArmyRules.patrolComes(0, AuArmyRules.PATROL_CHANCE), "only by chance");
        assertTrue(AuArmyRules.PATROL_MIN_DIST > 16, "they come from out of sight");
    }
}
