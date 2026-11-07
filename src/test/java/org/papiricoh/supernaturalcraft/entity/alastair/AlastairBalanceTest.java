package org.papiricoh.supernaturalcraft.entity.alastair;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.alastair.AlastairAssets;
import org.papiricoh.supernaturalcraft.entity.boss.alastair.AlastairBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlastairBalanceTest {

    @Test
    void threePhasesWithoutGaps() {
        assertEquals(3, AlastairBalance.PHASES);
        float last = 1f;
        for (int p = 1; p <= AlastairBalance.PHASES; p++) {
            float t = AlastairBalance.threshold(p);
            assertTrue(t < last, "phase " + p + " ends below where it began");
            last = t;
        }
        assertEquals(0f, AlastairBalance.threshold(AlastairBalance.PHASES));
        assertTrue(AlastairBalance.attackGap(1) > AlastairBalance.attackGap(3), "he quickens");
    }

    @Test
    void everyWindupLeavesTimeToReadTheGroundAndEveryClipItsMoment() {
        for (int w : new int[]{AlastairBalance.BOUND_LASH_WINDUP, AlastairBalance.SLASH_WINDUP, AlastairBalance.THROW_WINDUP,
                AlastairBalance.HOOK_WINDUP, AlastairBalance.LASH_WINDUP, AlastairBalance.SMOKE_WINDUP, AlastairBalance.RACK_WINDUP,
                AlastairBalance.SUMMON_WINDUP}) {
            assertTrue(w >= 12, "a windup of " + w + " ticks is too short to step out of");
        }
        for (var e : AlastairAssets.HIT_TICKS.entrySet()) {
            Integer len = AlastairAssets.CLIP_TICKS.get(e.getKey());
            assertTrue(len != null && e.getValue() < len, e.getKey() + "'s moment falls inside its clip");
            assertTrue(AlastairAssets.CLIPS.contains(e.getKey()), e.getKey() + " is one of his clips");
        }
        assertTrue(AlastairBalance.BOUND_MUNDANE < 1f, "bound, only holy things truly hurt him");
    }
}
