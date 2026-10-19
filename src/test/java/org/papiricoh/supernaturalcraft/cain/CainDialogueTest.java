package org.papiricoh.supernaturalcraft.cain;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.datagen.cain.CainLang;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainSpoils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The talk with Cain (v0.21 P4, pure): few enough answers and "leave" always last; the fight offered only to a hunter who put
 * Lucifer back in his box and bears no Mark, and started only at night; a jar of honey a day; every node reachable and every
 * line and answer (and the offer of the Mark) written.
 */
class CainDialogueTest {

    /** Every combination of what he may know. */
    private static List<CainDialogue.Context> contexts() {
        List<CainDialogue.Context> out = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            out.add(new CainDialogue.Context((i & 1) != 0, (i & 2) != 0, (i & 4) != 0, (i & 8) != 0, (i & 16) != 0));
        }
        return out;
    }

    @Test
    void neverMoreThanEightAnswersAndLeaveIsLast() {
        for (CainDialogue.Context ctx : contexts()) {
            for (String node : CainDialogue.NODES) {
                List<String> options = CainDialogue.options(node, ctx);
                assertFalse(options.isEmpty(), node + " offers nothing");
                assertTrue(options.size() <= 8, node + ": " + options.size() + " answers");
                assertEquals(CainDialogue.LEAVE, options.getLast(), node + ": leave must be last " + options);
                assertEquals(options.size(), new HashSet<>(options).size(), node + " repeats an answer");
                assertEquals("", CainDialogue.next(node, CainDialogue.LEAVE, ctx), "leaving " + node + " ends the talk");
                assertNull(CainDialogue.next(node, "not_an_option", ctx), node + " answers what it never asked");
                for (String o : options) assertTrue(CainDialogue.next(node, o, ctx) != null, node + "." + o + " leads nowhere");
            }
            assertEquals(ctx.met() ? CainDialogue.AGAIN : CainDialogue.HELLO, CainDialogue.start(ctx));
        }
    }

    @Test
    void theFightIsOnlyForThoseWhoPutTheDevilBackAndOnlyAtNight() {
        for (CainDialogue.Context ctx : contexts()) {
            for (String node : CainDialogue.TOPIC_NODES) {
                boolean offered = CainDialogue.options(node, ctx).contains(CainDialogue.OPT_FIGHT);
                assertEquals(ctx.devilDown() && !ctx.marked(), offered, node + " in " + ctx);
                String r = CainDialogue.next(node, CainDialogue.OPT_FIGHT, ctx);
                if (!offered) assertNull(r, node + ": a fight nobody was offered starts nothing");
                else assertEquals(ctx.night() ? CainDialogue.FIGHT : CainDialogue.NOT_TONIGHT, r, node + " in " + ctx);
            }
            // Asking for the Mark: he refuses, and the only way is through him.
            if (!ctx.marked()) {
                assertEquals(CainDialogue.REFUSE, CainDialogue.next(CainDialogue.MARK, CainDialogue.OPT_ASK, ctx));
                String r = CainDialogue.next(CainDialogue.REFUSE, CainDialogue.OPT_FIGHT, ctx);
                assertEquals(!ctx.devilDown() ? CainDialogue.NOT_READY : ctx.night() ? CainDialogue.FIGHT : CainDialogue.NOT_TONIGHT, r,
                        "challenged after his refusal in " + ctx);
            } else {
                assertFalse(CainDialogue.options(CainDialogue.MARK, ctx).contains(CainDialogue.OPT_ASK), "a bearer does not ask for it");
            }
        }
    }

    @Test
    void aJarOfHoneyADay() {
        for (CainDialogue.Context ctx : contexts()) {
            assertEquals(ctx.honeyToday() ? CainDialogue.NO_HONEY : CainDialogue.HONEY,
                    CainDialogue.next(CainDialogue.AGAIN, CainDialogue.OPT_HONEY, ctx));
            assertEquals(CainDialogue.AGAIN, CainDialogue.next(CainDialogue.MARK, CainDialogue.BACK, ctx));
            assertEquals(CainDialogue.WHO, CainDialogue.next(CainDialogue.HELLO, CainDialogue.OPT_WHO, ctx));
            assertFalse(CainDialogue.options(CainDialogue.WHO, ctx).contains(CainDialogue.OPT_WHO), "he does not tell twice in a row");
        }
    }

    @Test
    void everyNodeCanBeReached() {
        Set<String> reached = new HashSet<>(List.of(CainDialogue.HELLO, CainDialogue.AGAIN));
        for (CainDialogue.Context ctx : contexts()) {
            ArrayDeque<String> todo = new ArrayDeque<>(reached);
            while (!todo.isEmpty()) {
                String node = todo.pop();
                for (String o : CainDialogue.options(node, ctx)) {
                    String next = CainDialogue.next(node, o, ctx);
                    if (next != null && CainDialogue.NODES.contains(next) && reached.add(next)) todo.push(next);
                }
            }
        }
        assertEquals(new HashSet<>(CainDialogue.NODES), reached);
    }

    @Test
    void everyLineAndAnswerIsWritten() {
        Map<String, String> lang = new HashMap<>();
        CainLang.add((k, v) -> assertNull(lang.put(k, v), "twice: " + k));
        String d = "dialogue.supernaturalcraft." + CainDialogue.ID + ".";
        for (CainDialogue.Context ctx : contexts()) {
            for (String node : CainDialogue.NODES) {
                assertTrue(lang.containsKey(d + node) && !lang.get(d + node).isBlank(), "no line for " + node);
                for (String o : CainDialogue.options(node, ctx)) {
                    String key = d + node + "." + o;
                    assertTrue(lang.containsKey(key) && lang.get(key) != null && !lang.get(key).isBlank(), "no answer " + key);
                }
            }
        }
        String o = "dialogue.supernaturalcraft." + CainSpoils.DIALOGUE + ".";
        for (String node : List.of(CainSpoils.OFFER, CainSpoils.GRAVE, CainSpoils.GRAVE_AGAIN)) {
            assertTrue(lang.containsKey(o + node), "no offer line " + node);
            for (String a : List.of(CainSpoils.TAKE, CainSpoils.REFUSE)) assertTrue(lang.containsKey(o + node + "." + a), "no answer " + node + "." + a);
        }
        for (String adv : List.of("met_cain", "first_born", "marked")) {
            assertTrue(lang.containsKey("advancement.supernaturalcraft." + adv), "no advancement title " + adv);
        }
    }
}
