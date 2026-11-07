package org.papiricoh.supernaturalcraft.pact;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.pact.PactDialogue.Act;
import org.papiricoh.supernaturalcraft.pact.PactRules.Alpha;
import org.papiricoh.supernaturalcraft.pact.PactRules.Step;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Season 6's road (v0.39): the steps, the Alphas, Castiel's and Crowley's talks. */
class PactRulesTest {

    @Test
    void theStepsKeepTheirOrdinals() {
        // Saved by ordinal: append only.
        assertEquals(List.of("NONE", "CALLED", "MET_CROWLEY", "HUNTING", "RITE_READY", "BETRAYED", "FORGIVEN", "CONDEMNED"),
                java.util.Arrays.stream(Step.values()).map(Enum::name).toList());
        assertSame(Step.NONE, Step.of(-1));
        assertSame(Step.NONE, Step.of(99));
        assertTrue(Step.FORGIVEN.decided() && Step.CONDEMNED.decided());
        assertFalse(Step.BETRAYED.decided());
        assertTrue(Step.RITE_READY.atLeast(Step.HUNTING));
        assertFalse(Step.CALLED.atLeast(Step.HUNTING));
    }

    @Test
    void theAlphasComeOneAtATimeUntilAllThreeAreDown() {
        int mask = 0;
        for (Alpha a : Alpha.values()) {
            assertFalse(PactRules.hunts(Step.CALLED, mask, a), "not before Crowley's errand");
            assertTrue(PactRules.hunts(Step.HUNTING, mask, a));
        }
        Set<Integer> bits = new HashSet<>();
        for (Alpha a : Alpha.values()) {
            assertTrue(bits.add(a.bit()), "one bit each");
            assertSame(Step.HUNTING, PactRules.afterAlpha(Step.HUNTING, mask));
            mask = PactRules.with(mask, a);
            assertFalse(PactRules.hunts(Step.HUNTING, mask, a), a + " never twice");
        }
        assertEquals(PactRules.ALL_ALPHAS, mask);
        assertEquals(3, PactRules.count(mask));
        assertSame(Step.RITE_READY, PactRules.afterAlpha(Step.HUNTING, mask));
        assertSame(Step.BETRAYED, PactRules.afterAlpha(Step.BETRAYED, mask), "only the errand moves on");
        assertEquals("alpha_shapeshifter", Alpha.SHAPESHIFTER.entity());
    }

    @Test
    void castielRestsAndCrowleyGetsImpatient() {
        assertFalse(PactRules.visitDue(10, 10));
        assertTrue(PactRules.visitDue(10 + PactRules.VISIT_REST_DAYS, 10));
        assertFalse(PactRules.crowleyImpatient(Step.CALLED, 5 + PactRules.CROWLEY_PATIENCE_DAYS - 1, 5));
        assertTrue(PactRules.crowleyImpatient(Step.CALLED, 5 + PactRules.CROWLEY_PATIENCE_DAYS, 5));
        assertFalse(PactRules.crowleyImpatient(Step.HUNTING, 100, 5), "only while he waits for them");
    }

    @Test
    void castielSaysEachThingOnce() {
        assertSame(PactDialogue.CALL, PactDialogue.castielOpens(Step.NONE, 0, 0));
        assertNull(PactDialogue.castielOpens(Step.CALLED, 0, 0), "he has sent you to Crowley");
        assertNull(PactDialogue.castielOpens(Step.HUNTING, 0, 0), "nothing until an Alpha is down");
        int one = PactRules.with(0, Alpha.VAMPIRE);
        assertSame(PactDialogue.DOUBT, PactDialogue.castielOpens(Step.HUNTING, one, 0));
        assertNull(PactDialogue.castielOpens(Step.HUNTING, one, PactDialogue.SCENE_DOUBT));
        assertNull(PactDialogue.castielOpens(Step.RITE_READY, PactRules.ALL_ALPHAS, 0), "not before Crowley hands over the lab");
        assertSame(PactDialogue.READY, PactDialogue.castielOpens(Step.RITE_READY, PactRules.ALL_ALPHAS, PactDialogue.SCENE_LAB));
        assertNull(PactDialogue.castielOpens(Step.RITE_READY, PactRules.ALL_ALPHAS, PactDialogue.SCENE_LAB | PactDialogue.SCENE_READY));
        assertNull(PactDialogue.castielOpens(Step.FORGIVEN, PactRules.ALL_ALPHAS, 0));
    }

    @Test
    void theAnswersLeadWhereTheySay() {
        assertSame(PactDialogue.PLAN, PactDialogue.castiel("call", "listen").next());
        assertSame(Act.CASTIEL_REFUSE, PactDialogue.castiel("call", "refuse").act());
        assertSame(Act.CASTIEL_HELP, PactDialogue.castiel("plan", "help").act());
        assertSame(Act.CASTIEL_REFUSE, PactDialogue.castiel("plan", "refuse").act());
        assertEquals("question_reply", PactDialogue.castiel("doubt", "question").next().node());
        assertSame(Act.SCENE_DONE, PactDialogue.castiel("ready", "warn").act());
        assertSame(PactDialogue.MEET, PactDialogue.crowleyOpens(Step.CALLED, 0));
        assertSame(PactDialogue.DEAL, PactDialogue.crowley("meet", "listen").next());
        assertSame(Act.CROWLEY_DEAL, PactDialogue.crowley("deal", "deal").act());
        assertSame(Act.CROWLEY_LEAVE, PactDialogue.crowley("deal", "refuse").act());
        assertSame(PactDialogue.LAB, PactDialogue.crowleyOpens(Step.RITE_READY, 0));
        assertNull(PactDialogue.crowleyOpens(Step.RITE_READY, PactDialogue.SCENE_LAB));
        assertSame(Act.CROWLEY_LAB, PactDialogue.crowley("lab", "go").act());
        assertSame(Act.FORGIVE, PactDialogue.fate("forgive"));
        assertSame(Act.CONDEMN, PactDialogue.fate("condemn"));
    }

    @Test
    void silenceIsAlwaysTheSafeAnswer() {
        // The shared dialogue answers the last option for a hunter who says nothing.
        for (String d : List.of(PactDialogue.CASTIEL, PactDialogue.CROWLEY, PactDialogue.FATE)) {
            for (PactDialogue.Line line : PactDialogue.all(d)) assertFalse(line.options().isEmpty(), d + "." + line.node());
        }
        assertEquals("refuse", PactDialogue.PLAN.options().getLast(), "silence never signs you up");
        assertEquals("refuse", PactDialogue.DEAL.options().getLast());
        assertEquals("condemn", PactDialogue.FATE_LINE.options().getLast());
    }
}
