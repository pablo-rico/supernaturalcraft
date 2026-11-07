package org.papiricoh.supernaturalcraft.apocalypse.ruby;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RubyRulesTest {

    @Test
    void sheComesAfterAzazelAndStopsAtLilith() {
        assertNull(RubyRules.opens(RubyRules.Step.NONE, "human", false, false, false), "not before Azazel");
        assertSame(RubyRules.MEET, RubyRules.opens(RubyRules.Step.NONE, "human", true, false, false));
        assertSame(RubyRules.MEET_ANGEL, RubyRules.opens(RubyRules.Step.NONE, "angel", true, false, false));
        assertSame(RubyRules.MEET_DEMON, RubyRules.opens(RubyRules.Step.NONE, "demon", true, false, false));
        assertSame(RubyRules.OFFER, RubyRules.opens(RubyRules.Step.MET, "human", true, false, false));
        assertNull(RubyRules.opens(RubyRules.Step.ALASTAIR, "human", true, false, false), "nothing to say until Alastair falls");
        assertSame(RubyRules.LILITH, RubyRules.opens(RubyRules.Step.ALASTAIR, "human", true, true, false));
        assertNull(RubyRules.opens(RubyRules.Step.LILITHS_WAY, "human", true, true, false));
        assertNull(RubyRules.opens(RubyRules.Step.NONE, "human", true, true, true), "after Lilith, no more visits");
    }

    @Test
    void silenceIsTheSafeAnswer() {
        for (RubyRules.Line l : RubyRules.all()) {
            String last = l.options().getLast();
            RubyRules.Turn t = RubyRules.answer(l.node(), last);
            assertTrue(t.act() != RubyRules.Act.TAKE_FLASK, l.node() + ": silence never takes her blood");
        }
        assertEquals(RubyRules.Act.LEAVE, RubyRules.answer("meet_angel", "leave").act());
        assertSame(RubyRules.OFFER, RubyRules.answer("meet_demon", "listen").next());
        assertEquals(RubyRules.Act.TAKE_FLASK, RubyRules.answer("offer", "take").act());
        assertEquals(RubyRules.Act.NO_FLASK, RubyRules.answer("offer", "refuse").act());
        assertEquals(RubyRules.Act.LILITHS_WAY, RubyRules.answer("lilith", "why").act());
    }

    @Test
    void theStepsAreASaveFormat() {
        String[] order = {"NONE", "MET", "ALASTAIR", "LILITHS_WAY", "BETRAYAL_PENDING", "DONE"};
        for (int i = 0; i < order.length; i++) assertEquals(order[i], RubyRules.Step.values()[i].name());
        assertSame(RubyRules.Step.NONE, RubyRules.Step.of(-1));
        assertSame(RubyRules.Step.NONE, RubyRules.Step.of(99));
        assertTrue(RubyRules.owedReveal(RubyRules.Step.NONE) && !RubyRules.owedReveal(RubyRules.Step.DONE));
        Set<String> nodes = new HashSet<>();
        for (RubyRules.Line l : RubyRules.all()) assertTrue(nodes.add(l.node()), "node twice: " + l.node());
    }
}
