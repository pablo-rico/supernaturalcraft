package org.papiricoh.supernaturalcraft.gabriel;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.Channel;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielBalance;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.QuizBank;
import org.papiricoh.supernaturalcraft.trickster.PrankRules;
import org.papiricoh.supernaturalcraft.trickster.TricksterLedger;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gabriel's pure rules (v0.14): channels, thresholds, the laugh track, the monitor, the quiz deal, the pranks. */
class GabrielRulesTest {

    @Test
    void oneChannelPerPhaseInTheSeriesOrder() {
        assertSame(Channel.SITCOM, Channel.ofPhase(1));
        assertSame(Channel.GAME_SHOW, Channel.ofPhase(2));
        assertSame(Channel.HOSPITAL, Channel.ofPhase(3));
        assertSame(Channel.COMMERCIAL, Channel.ofPhase(4));
        assertSame(Channel.COMMERCIAL, Channel.ofPhase(9), "clamped");
        assertEquals(GabrielBalance.PHASES, Channel.values().length);
        for (Channel c : Channel.values()) assertTrue(c.costume != Channel.Costume.JACKET, "the jacket is his own, never a channel's");
    }

    @Test
    void aQuarterOfHisHealthEachPhase() {
        assertEquals(0.75f, GabrielBalance.threshold(1), 1e-6);
        assertEquals(0.5f, GabrielBalance.threshold(2), 1e-6);
        assertEquals(0.25f, GabrielBalance.threshold(3), 1e-6);
        assertEquals(1500f, (float) GabrielBalance.BASE_HEALTH * GabrielBalance.healthScale(2.5, 0.5, 1), 1e-3);
    }

    @Test
    void theLaughTrackStartsLitAndGoesDark() {
        assertTrue(GabrielBalance.laughing(0));
        assertTrue(GabrielBalance.laughing(GabrielBalance.LAUGH_TICKS - 1));
        assertFalse(GabrielBalance.laughing(GabrielBalance.LAUGH_TICKS));
        assertTrue(GabrielBalance.laughing(GabrielBalance.LAUGH_TICKS + GabrielBalance.QUIET_TICKS));
    }

    @Test
    void theMonitorQuickensAndTheBeatHasAWindow() {
        assertEquals(GabrielBalance.BEAT_SLOWEST, GabrielBalance.beatPeriod(0));
        assertEquals(GabrielBalance.BEAT_FASTEST, GabrielBalance.beatPeriod(1));
        assertTrue(GabrielBalance.onBeat(0, 20));
        assertTrue(GabrielBalance.onBeat(GabrielBalance.BEAT_WINDOW, 20));
        assertTrue(GabrielBalance.onBeat(20 - GabrielBalance.BEAT_WINDOW, 20), "just before the next beep counts");
        assertFalse(GabrielBalance.onBeat(10, 20));
    }

    @Test
    void theQuizDealsEachAnswerToOnePlatform() {
        Random r = new Random(7);
        for (int i = 0; i < 50; i++) {
            int[] dealt = QuizBank.deal(r);
            int[] sorted = dealt.clone();
            Arrays.sort(sorted);
            assertArrayEquals(new int[]{0, 1, 2}, sorted);
            assertEquals(0, dealt[QuizBank.rightPlatform(dealt)]);
            assertArrayEquals(dealt, QuizBank.unpack(QuizBank.pack(dealt)));
        }
    }

    @Test
    void oneHarmlessPrankADayAfterLucifer() {
        TricksterLedger fresh = TricksterLedger.NONE;
        assertFalse(PrankRules.due(false, fresh, 3), "not before Lucifer falls");
        assertTrue(PrankRules.due(true, fresh, 3));
        TricksterLedger once = fresh.sighted(3);
        assertFalse(PrankRules.due(true, once, 3), "one a day");
        assertTrue(PrankRules.due(true, once, 4));
        assertFalse(once.sighted(4).onHisTrail());
        assertTrue(once.sighted(4).sighted(5).onHisTrail(), "the third one puts you on his trail");
        Random r = new Random(1);
        for (int i = 0; i < 100; i++) {
            PrankRules.Prank p = PrankRules.pick(r, false, false, false);
            assertTrue(p == PrankRules.Prank.CANDY_WRAPPER || p == PrankRules.Prank.LAUGH_TRACK, "only what needs nothing around");
        }
    }
}
