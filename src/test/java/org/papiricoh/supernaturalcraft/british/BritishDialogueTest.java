package org.papiricoh.supernaturalcraft.british;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.british.BritishDialogue.Act;
import org.papiricoh.supernaturalcraft.british.BritishDialogue.Line;
import org.papiricoh.supernaturalcraft.british.BritishDialogue.Turn;
import org.papiricoh.supernaturalcraft.british.BritishRules.Choice;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BritishDialogueTest {

    @Test
    void hessOpensOnceAndTalksToTheMenOfLettersOfTheirBunker() {
        assertSame(BritishDialogue.OFFER, BritishDialogue.opens(Choice.NONE, false));
        assertSame(BritishDialogue.OFFER_LETTERS, BritishDialogue.opens(Choice.NONE, true));
        for (Choice c : new Choice[]{Choice.JOINED, Choice.REFUSED, Choice.BETRAYED}) assertNull(BritishDialogue.opens(c, false));
    }

    @Test
    void silenceIsTheRefusalEverywhereItMatters() {
        for (Line l : BritishDialogue.all()) {
            String quiet = BritishDialogue.silence(l);
            Turn t = BritishDialogue.answer(l.node(), quiet);
            assertFalse(t.act() == Act.JOIN, l.node() + ": silence never joins");
        }
        assertEquals(Act.REFUSE, BritishDialogue.answer("offer", BritishDialogue.silence(BritishDialogue.OFFER)).act());
        assertEquals(Act.REFUSE, BritishDialogue.answer("terms", BritishDialogue.silence(BritishDialogue.TERMS)).act());
    }

    @Test
    void joiningTakesTheTermsAndEitherWaySheLetsSlipTheNephilim() {
        Turn listen = BritishDialogue.answer("offer", "terms");
        assertSame(BritishDialogue.TERMS, listen.next());
        assertEquals(Act.NONE, listen.act(), "hearing the terms is not saying yes");
        Turn join = BritishDialogue.answer("terms", "join");
        assertEquals(Act.JOIN, join.act());
        assertSame(BritishDialogue.SLIP_JOINED, join.next());
        Turn refuse = BritishDialogue.answer("offer_letters", "refuse");
        assertEquals(Act.REFUSE, refuse.act());
        assertSame(BritishDialogue.SLIP_REFUSED, refuse.next());
        assertNull(BritishDialogue.answer("slip_joined", "go").next());
        assertEquals(Act.DONE, BritishDialogue.answer("slip_refused", "go").act());
    }

    @Test
    void everyNodeIsReachableAndHasAnswers() {
        Set<String> nodes = new HashSet<>();
        for (Line l : BritishDialogue.all()) {
            assertTrue(nodes.add(l.node()), "one node per name: " + l.node());
            assertFalse(l.options().isEmpty(), l.node() + " has answers");
            for (String o : l.options()) {
                Turn t = BritishDialogue.answer(l.node(), o);
                assertNotNull(t);
                if (t.next() != null) assertTrue(BritishDialogue.all().contains(t.next()), l.node() + "." + o + " leads to a known node");
            }
        }
    }
}
