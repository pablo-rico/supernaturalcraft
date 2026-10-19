package org.papiricoh.supernaturalcraft.coven;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.coven.ending.GrandWitchDialogue;
import org.papiricoh.supernaturalcraft.datagen.witchboss.WitchBossLang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Grand Witch at peace (v0.21 P6, pure): she owes one thing to one hunter only, the keeper of the Book who killed Rowena for
 * her, and only while they bear the Mark; to anyone else she has nothing to say. Every line and answer is written.
 */
class GrandWitchDialogueTest {

    private static final GrandWitchDialogue.Context VICTOR = new GrandWitchDialogue.Context(true, true),
            CLEAN = new GrandWitchDialogue.Context(true, false), STRANGER = new GrandWitchDialogue.Context(false, true),
            NOBODY = new GrandWitchDialogue.Context(false, false);
    private static final List<GrandWitchDialogue.Context> ALL = List.of(VICTOR, CLEAN, STRANGER, NOBODY);

    @Test
    void sheOwesOnlyHerChampion() {
        assertEquals(GrandWitchDialogue.HELLO, GrandWitchDialogue.start(VICTOR));
        assertEquals(GrandWitchDialogue.NOT_MARKED, GrandWitchDialogue.start(CLEAN), "the debt is paid once the Mark is off");
        assertEquals(GrandWitchDialogue.STRANGER, GrandWitchDialogue.start(STRANGER), "a stranger, Marked or not");
        assertEquals(GrandWitchDialogue.STRANGER, GrandWitchDialogue.start(NOBODY));
        assertEquals(GrandWitchDialogue.UNMARK, GrandWitchDialogue.next(GrandWitchDialogue.HELLO, GrandWitchDialogue.OPT_UNMARK, VICTOR));
        for (String node : List.of(GrandWitchDialogue.NEEDS_BLOOD, GrandWitchDialogue.NEEDS_BOOK)) {
            assertEquals(GrandWitchDialogue.UNMARK, GrandWitchDialogue.next(node, GrandWitchDialogue.OPT_UNMARK, VICTOR), "asked again from " + node);
        }
        for (GrandWitchDialogue.Context ctx : List.of(CLEAN, STRANGER, NOBODY)) {
            for (String node : GrandWitchDialogue.NODES) {
                assertFalse(GrandWitchDialogue.options(node, ctx).contains(GrandWitchDialogue.OPT_UNMARK), node + " offers the cure to " + ctx);
                assertNull(GrandWitchDialogue.next(node, GrandWitchDialogue.OPT_UNMARK, ctx));
            }
        }
        for (String node : List.of(GrandWitchDialogue.STRANGER, GrandWitchDialogue.UNMARKED, GrandWitchDialogue.NOT_MARKED)) {
            assertEquals(List.of(GrandWitchDialogue.LEAVE), GrandWitchDialogue.options(node, VICTOR), node + " has nothing more to offer");
        }
    }

    @Test
    void leaveIsAlwaysLastAndEndsTheTalk() {
        for (GrandWitchDialogue.Context ctx : ALL) {
            for (String node : GrandWitchDialogue.NODES) {
                List<String> o = GrandWitchDialogue.options(node, ctx);
                assertEquals(GrandWitchDialogue.LEAVE, o.getLast(), node);
                assertEquals("", GrandWitchDialogue.next(node, GrandWitchDialogue.LEAVE, ctx));
                assertNull(GrandWitchDialogue.next(node, "not_an_option", ctx));
            }
        }
        assertFalse(GrandWitchDialogue.NODES.contains(GrandWitchDialogue.UNMARK), "the server answers unmark, she never says it");
    }

    @Test
    void everyLineAndAnswerIsWritten() {
        Map<String, String> lang = new HashMap<>();
        WitchBossLang.add((k, v) -> assertNull(lang.put(k, v), "twice: " + k));
        String d = "dialogue.supernaturalcraft." + GrandWitchDialogue.ID + ".";
        for (String node : GrandWitchDialogue.NODES) {
            assertTrue(lang.containsKey(d + node) && !lang.get(d + node).isBlank(), "no line for " + node);
            for (GrandWitchDialogue.Context ctx : ALL) {
                for (String o : GrandWitchDialogue.options(node, ctx)) assertTrue(lang.containsKey(d + node + "." + o), "no answer " + node + "." + o);
            }
        }
    }
}
