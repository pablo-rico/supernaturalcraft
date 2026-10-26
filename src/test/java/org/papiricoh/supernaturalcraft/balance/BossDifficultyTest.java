package org.papiricoh.supernaturalcraft.balance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossDifficultyTest {

    private static final List<BossDifficulty> EASIEST_FIRST = List.of(BossDifficulty.PEACEFUL, BossDifficulty.EASY,
            BossDifficulty.NORMAL, BossDifficulty.HARD, BossDifficulty.HARDCORE);

    @Test
    void vanillaDifficultyAndHardcoreMapToTheTable() {
        assertSame(BossDifficulty.PEACEFUL, BossDifficulty.of(0, false));
        assertSame(BossDifficulty.EASY, BossDifficulty.of(1, false));
        assertSame(BossDifficulty.NORMAL, BossDifficulty.of(2, false));
        assertSame(BossDifficulty.HARD, BossDifficulty.of(3, false));
        for (int id = 0; id <= 3; id++) assertSame(BossDifficulty.HARDCORE, BossDifficulty.of(id, true), "hardcore wins at " + id);
    }

    @Test
    void normalChangesNothing() {
        BossDifficulty n = BossDifficulty.NORMAL;
        assertEquals(1f, n.health());
        assertEquals(1f, n.damage());
        assertEquals(1f, n.windup());
        assertEquals(1f, n.gap());
        assertEquals(1f, n.recover());
        for (int base = 0; base <= 6; base++) assertEquals(base, n.minions(base));
    }

    @Test
    void eachStepIsHarderThanTheLast() {
        for (int i = 1; i < EASIEST_FIRST.size(); i++) {
            BossDifficulty easier = EASIEST_FIRST.get(i - 1), harder = EASIEST_FIRST.get(i);
            String step = i + "";
            assertTrue(harder.health() > easier.health(), "health " + step);
            assertTrue(harder.damage() > easier.damage(), "damage " + step);
            assertTrue(harder.windup() < easier.windup(), "telegraphs " + step);
            assertTrue(harder.gap() < easier.gap(), "pauses " + step);
            assertTrue(harder.recover() < easier.recover(), "punish windows " + step);
            assertTrue(harder.minions(4) >= easier.minions(4), "minions " + step);
        }
    }

    @Test
    void peacefulHasNoMinionsAndTheOthersKeepAtLeastOne() {
        assertEquals(0, BossDifficulty.PEACEFUL.minions(5));
        assertEquals(1, BossDifficulty.EASY.minions(1));
        assertEquals(1, BossDifficulty.EASY.minions(2));
        assertEquals(2, BossDifficulty.EASY.minions(3));
        assertEquals(3, BossDifficulty.HARD.minions(2));
        assertEquals(3, BossDifficulty.HARDCORE.minions(2));
        for (BossDifficulty d : EASIEST_FIRST) assertEquals(0, d.minions(0));
    }

    @Test
    void theSavedIdRoundTripsAndOldSavesAreNormal() {
        for (BossDifficulty d : EASIEST_FIRST) assertSame(d, BossDifficulty.byId(d.id()));
        assertSame(BossDifficulty.NORMAL, BossDifficulty.byId(-1));
        assertSame(BossDifficulty.NORMAL, BossDifficulty.byId(99));
    }

    @Test
    void scaledTicksNeverReachZero() {
        assertEquals(1, BossDifficulty.ticks(1, 0.1f));
        assertEquals(34, BossDifficulty.ticks(40, 0.85f));
        assertEquals(64, BossDifficulty.ticks(40, 1.6f));
    }
}
