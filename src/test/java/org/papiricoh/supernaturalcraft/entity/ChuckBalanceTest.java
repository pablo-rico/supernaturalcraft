package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorRules;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckBalance;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckWindows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChuckBalanceTest {

    @Test
    void fifthsOfHisHealth() {
        assertEquals(0.8f, ChuckBalance.threshold(1), 1e-6);
        assertEquals(0.2f, ChuckBalance.threshold(4), 1e-6);
        assertEquals(0f, ChuckBalance.threshold(5), 1e-6);
        assertEquals(100f, ChuckBalance.band(1, 500), 1e-4);
        assertEquals(100f, ChuckBalance.band(5, 500), 1e-4);
    }

    @Test
    void aHundredThousandSoloAndHalfAgainPerHunter() {
        // v0.15: the end of the power curve.
        assertEquals(100_000f, ChuckBalance.health(0.5, 1), 1e-2);
        assertEquals(150_000f, ChuckBalance.health(0.5, 2), 1e-2);
        assertEquals(100_000f, ChuckBalance.health(0.5, 0), 1e-2, "nobody counts as one");
    }

    @Test
    void pagesAndWeakPointsScaleWithHim() {
        assertEquals(1200f, ChuckBalance.targetHealth(true, 100_000), 1e-2);
        assertEquals(640f, ChuckBalance.targetHealth(false, 100_000), 1e-2);
        assertEquals(2, (int) Math.ceil(ChuckBalance.PAGE_SHARE / ChuckBalance.TARGET_HIT_SHARE), "a page takes two capped blows");
    }

    @Test
    void contradictionsCrackAFixedShare() {
        assertEquals(12.5f, ChuckBalance.crack(500), 1e-4);
        assertTrue(1 / ChuckBalance.CRACK_SHARE >= 6, "chapter 5 should take several contradictions");
    }

    @Test
    void progressThroughABand() {
        assertEquals(0f, ChuckBalance.progress(4, 200, 500), 1e-6);
        assertEquals(0.5f, ChuckBalance.progress(4, 150, 500), 1e-6);
        assertEquals(1f, ChuckBalance.progress(4, 50, 500), 1e-6);
    }

    @Test
    void gapsShortenAndSnapsHurry() {
        for (int p = 2; p <= 5; p++) {
            assertTrue(ChuckBalance.attackGap(p) < ChuckBalance.attackGap(p - 1));
            assertTrue(ChuckBalance.snapCountdown(p) <= ChuckBalance.snapCountdown(p - 1));
        }
        assertTrue(ChuckBalance.snapCountdown(5) >= 30, "a snap must leave time to leave the frame");
    }

    @Test
    void threeOrFourPagesWithStaggeredShields() {
        assertEquals(3, ChuckBalance.pages(1));
        assertEquals(4, ChuckBalance.pages(3));
        // At any moment, some page is open within a short while.
        for (int t = 0; t < 400; t += 10) {
            boolean soon = false;
            for (int dt = 0; dt <= ChuckBalance.SHIELD_ON && !soon; dt += 10) {
                for (int i = 0; i < 3; i++) soon |= !ChuckBalance.shielded(i, t + dt);
            }
            assertTrue(soon, "no page opens soon after " + t);
        }
    }

    @Test
    void rulesGrowWithTheChapters() {
        assertEquals(0, ChuckBalance.rules(1), "he rewrites nothing in Eden");
        assertEquals(0, ChuckBalance.pickRule(1, 0, 3));
        for (int p = 2; p <= 5; p++) {
            for (int roll = 0; roll < 10; roll++) {
                int rule = ChuckBalance.pickRule(p, AuthorRules.GRAVITY_LOW, roll);
                assertTrue((ChuckBalance.rules(p) & rule) != 0, "a rule not allowed in " + p);
                assertNotEquals(AuthorRules.GRAVITY_LOW, rule, "the same rule twice");
            }
        }
        assertTrue(AuthorRules.has(ChuckBalance.rules(5), AuthorRules.FLOOR_LAVA));
    }

    @Test
    void theBarRefillsAndComesBack() {
        assertEquals(0.3f, ChuckBalance.refillLie(0.3f, 0), 1e-6);
        assertEquals(1f, ChuckBalance.refillLie(0.3f, ChuckBalance.REFILL_TICKS / 2), 1e-6);
        assertEquals(0.3f, ChuckBalance.refillLie(0.3f, ChuckBalance.REFILL_TICKS), 1e-6);
        float prev = 0;
        for (int t = 0; t <= ChuckBalance.REFILL_TICKS / 4; t++) {
            float v = ChuckBalance.refillLie(0.3f, t);
            assertTrue(v >= prev);
            prev = v;
        }
    }

    @Test
    void snapFrameAndSweepingLines() {
        assertTrue(ChuckBalance.inSnapFrame(4.4, -4.4, 4.5), "corners are inside a square frame");
        assertFalse(ChuckBalance.inSnapFrame(4.6, 0, 4.5));
        assertTrue(ChuckBalance.lineHits(true, 0, 1.8), "standing in a low line");
        assertFalse(ChuckBalance.lineHits(true, 0.9, 1.8), "jumping over it");
        assertTrue(ChuckBalance.lineHits(false, 0, 1.8), "standing in a high line");
        assertFalse(ChuckBalance.lineHits(false, 0, 1.5), "kneeling under it");
    }

    @Test
    void whiteRises() {
        assertEquals(0.25f, ChuckBalance.whiteness(0), 1e-6);
        assertEquals(1f, ChuckBalance.whiteness(1), 1e-6);
        assertFalse(ChuckBalance.echoes(2));
        assertTrue(ChuckBalance.echoes(3));
    }

    @Test
    void windowsOpenAndLengthen() {
        ChuckWindows w = new ChuckWindows();
        assertFalse(w.isOpen(0));
        w.open(100, 120);
        assertTrue(w.isOpen(219));
        assertFalse(w.isOpen(220));
        w.open(110, 20);
        assertTrue(w.isOpen(219), "a shorter window never cuts a longer one");
        w.close();
        assertFalse(w.isOpen(150));
    }

    @Test
    void ringsBreakAndRepair() {
        ChuckWindows.Rings rings = new ChuckWindows.Rings();
        assertEquals(ChuckWindows.Break.NOTHING, rings.breakNode(0, 0, 0, 600));
        assertEquals(ChuckWindows.Break.NOTHING, rings.breakNode(0, 0, 0, 600), "a broken point breaks once");
        assertEquals(ChuckWindows.Break.NOTHING, rings.breakNode(0, 1, 0, 600));
        assertEquals(ChuckWindows.Break.RING, rings.breakNode(0, 2, 10, 600));
        assertTrue(rings.ringBroken(0));
        assertTrue(rings.repairDue(500).isEmpty());
        assertEquals(java.util.List.of(0), rings.repairDue(610));
        assertFalse(rings.ringBroken(0), "repaired");
        for (int r = 0; r < 4; r++) {
            for (int n = 0; n < 3; n++) {
                ChuckWindows.Break b = rings.breakNode(r, n, 1000, 600);
                if (r == 3 && n == 2) assertEquals(ChuckWindows.Break.ALL, b, "the last point of the last ring exposes the core");
            }
        }
        assertTrue(rings.repairDue(5000).isEmpty(), "with the core exposed, nothing repairs on its own");
        assertEquals(1f, rings.brokenShare(), 1e-6);
        rings.reset();
        assertEquals(0, rings.ringsBroken());
    }
}
