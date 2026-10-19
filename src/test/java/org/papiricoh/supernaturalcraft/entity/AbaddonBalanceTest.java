package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonAssets;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonBalance;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abaddon's numbers (v0.21 P5, pure): three gapless thirds, quickening; her share of a blow (35 %, in full for the First Blade, a
 * Marked hunter or a trap; doubled while stunned); the Brand's rings and gaps; windups long enough for their clips; possession
 * and the trap's timings.
 */
class AbaddonBalanceTest {

    @Test
    void threePhasesOfAThirdEach() {
        assertEquals(2f / 3f, AbaddonBalance.threshold(1), 1e-6);
        assertEquals(1f / 3f, AbaddonBalance.threshold(2), 1e-6);
        assertEquals(0f, AbaddonBalance.threshold(3), 1e-6);
        assertTrue(AbaddonBalance.attackGap(3) < AbaddonBalance.attackGap(2) && AbaddonBalance.attackGap(2) < AbaddonBalance.attackGap(1));
    }

    @Test
    void herShareOfABlow() {
        assertEquals(0.35f, AbaddonBalance.share(false, false), 1e-6, "an ordinary blow: 35 %");
        assertEquals(1f, AbaddonBalance.share(true, false), 1e-6, "the First Blade, the Mark, a trap: all of it");
        assertEquals(0.7f, AbaddonBalance.share(false, true), 1e-6, "stunned: twice the share");
        assertEquals(2f, AbaddonBalance.share(true, true), 1e-6);
        assertEquals(100, AbaddonBalance.STUN_TICKS);
        assertEquals(120, AbaddonBalance.TRAPPED_TICKS);
        assertEquals(600, AbaddonBalance.TRAP_COOLDOWN);
        assertEquals(320, AbaddonBalance.POSSESS_TICKS);
    }

    @Test
    void theBrandsRingsAndGaps() {
        float[] r = AbaddonBalance.BRAND_RINGS;
        assertEquals(3, r.length);
        assertEquals(4.5f, r[0], 1e-6);
        assertEquals(8.5f, r[1], 1e-6);
        assertEquals(12.5f, r[2], 1e-6);
        for (int i = 0; i < r.length; i++) {
            assertEquals(i, AbaddonBalance.brandRing(r[i]), "on ring " + i);
            assertEquals(i, AbaddonBalance.brandRing(r[i] + AbaddonBalance.BRAND_BAND - 0.01), "its outer edge");
            assertEquals(i, AbaddonBalance.brandRing(r[i] - AbaddonBalance.BRAND_BAND + 0.01), "its inner edge");
            assertEquals(-1, AbaddonBalance.brandRing(r[i] + AbaddonBalance.BRAND_BAND + 0.05), "past it");
        }
        assertEquals(-1, AbaddonBalance.brandRing(0), "at her feet");
        assertEquals(-1, AbaddonBalance.brandRing((r[0] + r[1]) / 2), "between the rings");
        assertTrue(AbaddonBalance.brandHits(1, AbaddonBalance.BRAND_GAP_RADIUS + 0.01), "on a ring, out of the gap");
        assertFalse(AbaddonBalance.brandHits(1, AbaddonBalance.BRAND_GAP_RADIUS), "in a gap");
        assertFalse(AbaddonBalance.brandHits(-1, 99), "off the rings");
        for (int i = 1; i < r.length; i++) {
            assertTrue(r[i] - r[i - 1] > 2 * AbaddonBalance.BRAND_BAND + 1, "a safe band between ring " + (i - 1) + " and " + i);
        }
        assertTrue(AbaddonBalance.BRAND_GAP_RADIUS * 2 > AbaddonBalance.BRAND_BAND, "a gap spans its ring");
    }

    @Test
    void windupsEndOnTheClipsMoments() {
        Map<String, Integer> windups = Map.of("hellfire_sweep", AbaddonBalance.SWEEP_WINDUP, "smoke_out", AbaddonBalance.SMOKE_WINDUP,
                "telekinetic_pin", AbaddonBalance.PIN_WINDUP, "brand", AbaddonBalance.BRAND_WINDUP,
                "herald_mark", AbaddonBalance.MARK_FOLLOW + AbaddonBalance.MARK_HOLD);
        for (var e : windups.entrySet()) {
            int cue = AbaddonBalance.cue(e.getKey());
            assertEquals(AbaddonAssets.HIT_TICKS.get(e.getKey()).intValue(), cue);
            assertTrue(e.getValue() >= cue, e.getKey() + ": the clip must start inside the windup");
        }
        assertTrue(AbaddonBalance.MARK_HOLD >= 10, "the mark holds still long enough to step out");
        assertTrue(AbaddonBalance.PIN_TICKS <= 60, "a pin is short");
    }
}
