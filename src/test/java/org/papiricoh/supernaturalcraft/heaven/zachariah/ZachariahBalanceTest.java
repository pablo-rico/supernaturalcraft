package org.papiricoh.supernaturalcraft.heaven.zachariah;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahAnimations;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahBalance;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.arena.ZachariahOfficeLayout;
import org.papiricoh.supernaturalcraft.heaven.HeavenAssets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Zachariah's numbers (v0.18): quarters, the docket's length, the wrap, the stamp, the notice, the smite's ring, his clips. */
class ZachariahBalanceTest {

    @Test
    void fourPhasesOfAQuarterEach() {
        assertEquals(0.75f, ZachariahBalance.threshold(1), 1e-6);
        assertEquals(0.5f, ZachariahBalance.threshold(2), 1e-6);
        assertEquals(0.25f, ZachariahBalance.threshold(3), 1e-6);
        assertEquals(0f, ZachariahBalance.threshold(4), 1e-6);
        for (int p = 2; p <= 4; p++) {
            assertTrue(ZachariahBalance.attackGap(p) < ZachariahBalance.attackGap(p - 1), "the paperwork piles up faster in " + p);
        }
        assertEquals(50_000f, ProgressionScale.of(BossProgression.Boss.ZACHARIAH).trueHealth(), 1e-3, "fifty thousand on the curve");
    }

    @Test
    void theDocketOpensInTheThirdPhase() {
        assertEquals(0, ZachariahBalance.docketSize(1));
        assertEquals(0, ZachariahBalance.docketSize(2));
        assertEquals(3, ZachariahBalance.docketSize(3));
        assertEquals(5, ZachariahBalance.docketSize(4));
        assertEquals(60, ZachariahBalance.FORETOLD_LEAD);
        assertEquals(20, ZachariahBalance.REVISION_WARNING);
    }

    @Test
    void theWrapBringsYouBackInFromTheOtherSide() {
        int w = ZachariahOfficeLayout.WRAP_WINDOW, s = ZachariahOfficeLayout.WRAP_SHIFT;
        assertEquals(0, ZachariahBalance.wrapShift(0));
        assertEquals(0, ZachariahBalance.wrapShift(w), "the window's edge is still inside");
        assertEquals(-s, ZachariahBalance.wrapShift(w + 0.1));
        assertEquals(s, ZachariahBalance.wrapShift(-w - 0.1));
        assertEquals(0, ZachariahBalance.wrapShift(w + s + 5), "far outside the office: nothing");
        double after = w + 0.1 - s;
        assertTrue(Math.abs(after) <= w, "one shift lands back inside the window");
        assertEquals(0, s % ZachariahOfficeLayout.TILE, "a whole number of tiles");
        assertEquals(3.0, ZachariahBalance.fold(3.0), 1e-9);
        assertEquals(30.0 - s, ZachariahBalance.fold(30.0), 1e-9);
        assertEquals(-30.0 + s, ZachariahBalance.fold(-30.0), 1e-9);
        assertTrue(ZachariahBalance.ARENA_RADIUS >= Math.ceil(w * Math.sqrt(2)), "the arena reaches the window's corners");
        assertTrue(ZachariahBalance.ARENA_RADIUS >= Math.ceil(ZachariahOfficeLayout.RADIUS * Math.sqrt(2)), "and the whole square office");
    }

    @Test
    void theStampIsAThreeByThreeSquare() {
        assertTrue(ZachariahBalance.underStamp(0, 0));
        assertTrue(ZachariahBalance.underStamp(1.4, -1.4), "a corner");
        assertFalse(ZachariahBalance.underStamp(1.6, 0));
        assertFalse(ZachariahBalance.underStamp(0, -1.6));
    }

    @Test
    void aTerminationNoticeIsSplitBetweenThoseWhoShareIt() {
        assertEquals(8f, ZachariahBalance.terminationEach(20f, 1), 1e-6, "two fifths of their health alone");
        assertEquals(4f, ZachariahBalance.terminationEach(20f, 2), 1e-6);
        assertEquals(2f, ZachariahBalance.terminationEach(20f, 4), 1e-6);
        assertEquals(8f, ZachariahBalance.terminationEach(20f, 0), 1e-6, "nobody is never less than one");
    }

    @Test
    void theSmiteOfHeavenSparesOnlyItsRing() {
        assertTrue(ZachariahBalance.smiteHits(4, 0), "near him, on a line");
        assertFalse(ZachariahBalance.smiteHits(10, 0), "in the ring");
        assertTrue(ZachariahBalance.smiteHits(15, 1), "beyond the ring, on a line");
        assertFalse(ZachariahBalance.smiteHits(15, 2), "between two lines");
        assertFalse(ZachariahBalance.smiteHits(ZachariahBalance.SMITE_LENGTH + 1, 0), "past the line's end");
        assertFalse(ZachariahBalance.smiteHits(-2, 0), "behind the line's start");
    }

    @Test
    void everyClipHePlaysIsInTheArt() {
        for (String clip : ZachariahAnimations.USED) {
            assertTrue(HeavenAssets.ZACHARIAH_CLIPS.contains(clip), "clip " + clip + " is not in HeavenAssets.ZACHARIAH_CLIPS");
        }
        for (String hold : ZachariahAnimations.HOLDS) assertTrue(ZachariahAnimations.TRIGGERED.contains(hold), hold + " is triggered");
        assertFalse(ZachariahAnimations.TRIGGERED.contains("idle"), "loops are not triggered");
    }
}
