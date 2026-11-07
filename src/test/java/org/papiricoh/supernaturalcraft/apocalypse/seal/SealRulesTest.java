package org.papiricoh.supernaturalcraft.apocalypse.seal;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The seals' pure rules (v0.40): the count, the delay cap, the stages, the event caps and outcomes, the kinds. */
class SealRulesTest {

    @Test
    void aSealBreaksADayAfterTheLastPlusADayPerEventHeld() {
        long last = 5 * SealRules.DAY;
        assertEquals(last + SealRules.DAY, SealRules.nextBreak(last, 0));
        assertEquals(last + 3 * SealRules.DAY, SealRules.nextBreak(last, 2));
        assertFalse(SealRules.breaksNow(true, 3, last + SealRules.DAY - 1, last, 0), "not before its day");
        assertTrue(SealRules.breaksNow(true, 3, last + SealRules.DAY, last, 0), "on its day");
        assertFalse(SealRules.breaksNow(true, 3, last + SealRules.DAY, last, 1), "an event held pushes it a day");
        assertFalse(SealRules.breaksNow(false, 3, last + 9 * SealRules.DAY, last, 0), "nothing before the count starts");
    }

    @Test
    void thePassiveCountStopsAtElevenForLilith() {
        assertEquals(SealRules.TOTAL - 1, SealRules.PASSIVE_MAX);
        assertFalse(SealRules.breaksNow(true, SealRules.PASSIVE_MAX, Long.MAX_VALUE / 2, 0, 0), "the twelfth is Lilith's death");
        assertTrue(SealRules.breaksNow(true, SealRules.PASSIVE_MAX - 1, 10 * SealRules.DAY, 0, 0));
    }

    @Test
    void theDelayIsCapped() {
        int won = 0;
        for (int i = 0; i < 20; i++) if (SealRules.canDelay(won, 3)) won++;
        assertEquals(SealRules.DELAY_CAP, won, "no more than the cap over the whole Apocalypse");
        assertFalse(SealRules.canDelay(0, SealRules.PASSIVE_MAX), "nothing to delay with only Lilith left");
    }

    @Test
    void daysLeftRoundUp() {
        assertEquals(1, SealRules.daysLeft(0, 1));
        assertEquals(1, SealRules.daysLeft(0, SealRules.DAY));
        assertEquals(2, SealRules.daysLeft(0, SealRules.DAY + 1));
        assertEquals(0, SealRules.daysLeft(5, 5));
        assertEquals(0, SealRules.daysLeft(5, -1));
    }

    @Test
    void stagesEveryThreeSeals() {
        assertEquals(0, SealRules.stage(2, false));
        assertEquals(1, SealRules.stage(3, false));
        assertEquals(SealRules.ANGEL_STAGE, SealRules.stage(6, false));
        assertEquals(3, SealRules.stage(11, false));
        assertEquals(SealRules.MAX_STAGE, SealRules.stage(SealRules.TOTAL, false));
        assertEquals(SealRules.MAX_STAGE, SealRules.stage(0, true), "risen is the last stage");
        assertEquals(1, SealRules.effectiveStage(SealRules.TOTAL, true, true), "the echo is faint");
        assertTrue(SealRules.omenStrength(12, true, true) < SealRules.omenStrength(12, true, false));
        assertEquals(1f, SealRules.omenStrength(12, true, false), 1e-6);
    }

    @Test
    void eventsComeOnlyToChapterTwoWithinTheirCaps() {
        assertTrue(SealRules.chapterTwo(true, false));
        assertFalse(SealRules.chapterTwo(true, true));
        assertFalse(SealRules.chapterTwo(false, false));
        long now = 100_000;
        assertTrue(SealRules.mayCall(true, 0, 0, 0, now, -1, 3, false));
        assertFalse(SealRules.mayCall(false, 0, 0, 0, now, -1, 3, false), "not past Lilith or before Azazel");
        assertFalse(SealRules.mayCall(true, SealRules.MAX_PER_PLAYER, 0, 0, now, -1, 3, false), "one at a time per hunter");
        assertFalse(SealRules.mayCall(true, 0, SealRules.MAX_PER_WORLD, 0, now, -1, 3, false), "three in the world");
        assertFalse(SealRules.mayCall(true, 0, 0, SealRules.MAX_PER_DAY, now, -1, 3, false), "two a day per hunter");
        assertFalse(SealRules.mayCall(true, 0, 0, 0, now, now - SealRules.REST_TICKS + 1, 3, false), "a rest between calls");
        assertTrue(SealRules.mayCall(true, 0, 0, 0, now, now - SealRules.REST_TICKS, 3, false));
        assertFalse(SealRules.mayCall(true, 0, 0, 0, now, -1, SealRules.PASSIVE_MAX, false), "nothing left to break");
        assertFalse(SealRules.mayCall(true, 0, 0, 0, now, -1, 3, true), "Lucifer has risen");
        assertTrue(SealRules.MAX_PER_DAY >= 1 && SealRules.MAX_PER_DAY <= 2);
    }

    @Test
    void theRiteTakesHalfADay() {
        float p = 0;
        for (int t = 0; t < SealRules.EVENT_TICKS; t += 20) p += SealRules.riteStep(20, true);
        assertEquals(SealRules.RITE_MAX, p, 0.01f);
        assertEquals(12_000, SealRules.EVENT_TICKS);
        assertEquals(SealRules.RITE_MAX, SealRules.wardSlain(SealRules.RITE_MAX - 1), 1e-6, "never past the top");
        assertTrue(SealRules.wardSlain(10) > 10);
    }

    @Test
    void outcomes() {
        assertEquals(SealRules.Outcome.RUNNING, SealRules.outcome(50, 2, 3, 3));
        assertEquals(SealRules.Outcome.HELD, SealRules.outcome(50, 0, 3, 3), "every driver gone");
        assertEquals(SealRules.Outcome.HELD, SealRules.outcome(50, 0, 0, 0), "a kind with no wards");
        assertEquals(SealRules.Outcome.BROKEN, SealRules.outcome(50, 2, 0, 3), "every ward slain");
        assertEquals(SealRules.Outcome.BROKEN, SealRules.outcome(SealRules.RITE_MAX, 0, 3, 3), "the demons win ties");
    }

    @Test
    void thirteenKindsInFourFamilies() {
        assertEquals(13, SealKind.values().length);
        Set<SealFamily> fams = new HashSet<>();
        for (SealKind k : SealKind.values()) {
            fams.add(k.family);
            assertTrue(k.wards || k.focus, k + " needs something the hunters can break or save");
            assertEquals(k, SealKind.byId(k.id()));
            assertEquals(k, SealKind.byOrdinal(k.ordinal()));
        }
        assertEquals(SealFamily.values().length, fams.size());
        for (SealFamily f : SealFamily.values()) {
            int n = SealKind.of(f).size();
            assertTrue(n >= 2 && n <= 4, f + " has " + n + " kinds");
        }
        assertEquals(SealKind.values()[0], SealKind.pick(0));
        assertEquals(SealKind.values()[SealKind.values().length - 1], SealKind.pick(0.99999));
        assertEquals(SealKind.WITNESSES, SealKind.byOrdinal(-1));
    }

    @Test
    void urielAfterTwoAngelSeals() {
        assertEquals(2, SealRules.URIEL_AFTER);
        SealRecord r = SealRecord.EMPTY.held(SealKind.SLAIN_ANGELS).held(SealKind.WITNESSES).held(SealKind.GARRISON);
        assertEquals(3, r.defended());
        assertEquals(2, r.angelsDefended());
        assertTrue(r.heldKind(SealKind.WITNESSES) && !r.heldKind(SealKind.SWARMS));
        assertTrue(SealEnvoy.due(r));
        assertFalse(SealEnvoy.due(r.withEnvoy(2)), "heard him out");
        assertEquals(1, SealRecord.EMPTY.called(10, 3).calledOn(3));
        assertEquals(2, SealRecord.EMPTY.called(10, 3).called(20, 3).calledOn(3));
        assertEquals(1, SealRecord.EMPTY.called(10, 3).called(20, 4).calledOn(4), "a new day");
        assertEquals(0, SealRecord.EMPTY.called(10, 3).calledOn(5));
    }

    @Test
    void theTwelveSealsHaveNames() {
        for (int n = 1; n <= SealRules.TOTAL; n++) assertTrue(SealWorld.sealName(n).endsWith("." + n));
        assertEquals(SealWorld.sealName(1), SealWorld.sealName(0));
        assertEquals(SealWorld.sealName(SealRules.TOTAL), SealWorld.sealName(99));
    }
}
