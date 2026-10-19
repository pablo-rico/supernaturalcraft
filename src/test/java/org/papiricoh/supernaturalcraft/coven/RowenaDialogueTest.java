package org.papiricoh.supernaturalcraft.coven;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaDialogue;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaLessons;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.datagen.coven.CovenLang;

import java.util.ArrayDeque;
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
 * The talk with Rowena (v0.21 P3, pure): few enough answers, "leave" always last, every node reachable and written. P6, the ending
 * ({@link RowenaDialogue#ENDING_NODES}): the Book is spoken of only by a hunter who holds it after Abaddon and has not chosen; the
 * choice is final (no "book" afterwards); a keeper meets only hostility; a giver may ask how to call the Grand Coven until the
 * Grand Witch falls, and then (bearing the Mark) to have it taken off.
 */
class RowenaDialogueTest {

    private static final List<String> ALL_PAGES = RowenaLessons.PAGES.stream().map(RowenaLessons.Page::id).toList();
    private static final List<RowenaDialogue.Context> CONTEXTS = List.of(
            new RowenaDialogue.Context(false, false, false, RowenaLessons.offer(id -> false, t -> true, 0)),
            new RowenaDialogue.Context(true, false, false, List.of()),
            new RowenaDialogue.Context(true, true, false, RowenaLessons.offer(id -> false, t -> true, 20)),
            new RowenaDialogue.Context(true, true, true, List.of("familiar_bond")));

    @Test
    void neverMoreThanEightAnswersAndLeaveIsLast() {
        for (RowenaDialogue.Context ctx : CONTEXTS) {
            for (String node : RowenaDialogue.NODES) {
                List<String> options = RowenaDialogue.options(node, ctx);
                assertFalse(options.isEmpty(), node + " offers nothing");
                assertTrue(options.size() <= 8, node + ": " + options.size() + " answers");
                assertEquals(RowenaDialogue.LEAVE, options.getLast(), node + ": leave must be last " + options);
                assertEquals(options.size(), new HashSet<>(options).size(), node + " repeats an answer");
                assertEquals("", RowenaDialogue.next(node, RowenaDialogue.LEAVE, ctx), "leaving " + node + " ends the talk");
                assertNull(RowenaDialogue.next(node, "not_an_option", ctx), node + " answers what it never asked");
            }
        }
    }

    @Test
    void cainOnlyOnceTheDevilWentDown() {
        RowenaDialogue.Context early = CONTEXTS.get(1);
        for (String node : RowenaDialogue.TOPIC_NODES) {
            assertFalse(RowenaDialogue.options(node, early).contains(RowenaDialogue.OPT_CAIN), node + " speaks of Cain too soon");
            assertNull(RowenaDialogue.next(node, RowenaDialogue.OPT_CAIN, early));
        }
        RowenaDialogue.Context late = CONTEXTS.get(2), told = CONTEXTS.get(3);
        assertEquals(RowenaDialogue.CLUE, RowenaDialogue.next(RowenaDialogue.AGAIN, RowenaDialogue.OPT_CAIN, late));
        assertEquals(RowenaDialogue.CLUE_AGAIN, RowenaDialogue.next(RowenaDialogue.AGAIN, RowenaDialogue.OPT_CAIN, told));
    }

    @Test
    void lessonsAndWaresGoToTheTill() {
        RowenaDialogue.Context ctx = CONTEXTS.getFirst();
        assertEquals(RowenaDialogue.HELLO, RowenaDialogue.start(ctx));
        assertEquals(RowenaDialogue.AGAIN, RowenaDialogue.start(CONTEXTS.get(1)));
        assertEquals(RowenaDialogue.LESSONS, RowenaDialogue.next(RowenaDialogue.HELLO, RowenaDialogue.OPT_LESSONS, ctx));
        assertEquals(RowenaDialogue.NO_LESSONS, RowenaDialogue.next(RowenaDialogue.AGAIN, RowenaDialogue.OPT_LESSONS, CONTEXTS.get(1)));
        for (String page : ctx.offer()) {
            assertEquals(RowenaDialogue.BUY_PAGE + page, RowenaDialogue.next(RowenaDialogue.LESSONS, page, ctx));
        }
        assertNull(RowenaDialogue.next(RowenaDialogue.LESSONS, "familiar_bond", ctx), "a page she does not offer cannot be bought");
        for (String ware : RowenaLessons.WARES.keySet()) {
            assertEquals(RowenaDialogue.BUY_WARE + ware, RowenaDialogue.next(RowenaDialogue.WARES, ware, ctx));
        }
        assertEquals(RowenaDialogue.AGAIN, RowenaDialogue.next(RowenaDialogue.WARES, RowenaDialogue.BACK, ctx));
    }

    @Test
    void everyNodeCanBeReached() {
        Set<String> reached = new HashSet<>(List.of(RowenaDialogue.HELLO, RowenaDialogue.AGAIN));
        // The server answers purchases with these.
        reached.addAll(List.of(RowenaDialogue.BOUGHT, RowenaDialogue.TOO_POOR, RowenaDialogue.SOLD));
        for (RowenaDialogue.Context ctx : CONTEXTS) {
            ArrayDeque<String> todo = new ArrayDeque<>(reached);
            while (!todo.isEmpty()) {
                String node = todo.pop();
                for (String o : RowenaDialogue.options(node, ctx)) {
                    String next = RowenaDialogue.next(node, o, ctx);
                    if (next != null && RowenaDialogue.NODES.contains(next) && reached.add(next)) todo.push(next);
                }
            }
        }
        assertEquals(new HashSet<>(RowenaDialogue.NODES), reached);
    }

    @Test
    void everyLineAndAnswerIsWrittenWithItsPrice() {
        Map<String, String> lang = new HashMap<>();
        CovenLang.add((k, v) -> assertNull(lang.put(k, v), "twice: " + k));
        String d = "dialogue.supernaturalcraft." + RowenaDialogue.ID + ".";
        RowenaDialogue.Context every = new RowenaDialogue.Context(true, true, false, ALL_PAGES);
        for (String node : RowenaDialogue.NODES) {
            assertTrue(lang.containsKey(d + node) && !lang.get(d + node).isBlank(), "no line for " + node);
            for (String o : RowenaDialogue.options(node, every)) assertTrue(lang.containsKey(d + node + "." + o), "no answer " + node + "." + o);
        }
        for (RowenaLessons.Page p : RowenaLessons.PAGES) {
            String text = lang.get(d + RowenaDialogue.LESSONS + "." + p.id());
            assertTrue(text.contains(RowenaLessons.price(p.tier()).emeralds() + " emeralds"), p.id() + " shows its price: " + text);
        }
        for (var w : RowenaLessons.WARES.entrySet()) {
            String text = lang.get(d + RowenaDialogue.WARES + "." + w.getKey());
            assertTrue(text.contains(w.getValue()[0] + " emeralds"), w.getKey() + " shows its price: " + text);
        }
    }

    // --- the ending (P6) --------------------------------------------------------------------------------------------------

    private static RowenaDialogue.Context ending(boolean ready, RowenaRelation.Book book, boolean witchBeaten, boolean marked) {
        return new RowenaDialogue.Context(true, true, true, List.of("compel"), new RowenaDialogue.Ending(ready, book, witchBeaten, marked));
    }

    private static final RowenaDialogue.Context READY = ending(true, RowenaRelation.Book.NONE, false, true),
            NOT_READY = ending(false, RowenaRelation.Book.NONE, false, true),
            GIVEN = ending(false, RowenaRelation.Book.GIVEN, false, true),
            GIVEN_WON = ending(false, RowenaRelation.Book.GIVEN, true, true),
            GIVEN_WON_CLEAN = ending(false, RowenaRelation.Book.GIVEN, true, false),
            KEPT = ending(false, RowenaRelation.Book.KEPT, false, true),
            KEPT_WON = ending(false, RowenaRelation.Book.KEPT, true, true);
    private static final List<RowenaDialogue.Context> ENDINGS = List.of(READY, NOT_READY, GIVEN, GIVEN_WON, GIVEN_WON_CLEAN, KEPT, KEPT_WON,
            // A giver who holds a second Book (won again from Abaddon) is not asked again: the choice is made.
            ending(true, RowenaRelation.Book.GIVEN, false, true), ending(true, RowenaRelation.Book.KEPT, false, true));

    @Test
    void theEndingNodesAreHersAndNeverRepeatTheShopsNodes() {
        assertEquals(List.of(RowenaDialogue.BOOK_ASK, RowenaDialogue.BOOK_GIVEN, RowenaDialogue.BOOK_KEPT, RowenaDialogue.HOSTILE,
                RowenaDialogue.COVEN, RowenaDialogue.UNMARKED, RowenaDialogue.NEEDS_BLOOD), RowenaDialogue.ENDING_NODES);
        assertEquals(RowenaDialogue.NODES.size() + RowenaDialogue.ENDING_NODES.size(), new HashSet<>(RowenaDialogue.ALL_NODES).size(),
                "no node is both the shop's and the ending's");
        assertFalse(RowenaDialogue.ALL_NODES.contains(RowenaDialogue.UNMARK), "unmark is settled by the server, never spoken");
        for (RowenaDialogue.Context ctx : ENDINGS) {
            for (String node : RowenaDialogue.ALL_NODES) {
                List<String> options = RowenaDialogue.options(node, ctx);
                assertTrue(options.size() <= 8, node + ": " + options.size() + " answers");
                assertEquals(RowenaDialogue.LEAVE, options.getLast(), node + ": leave must be last " + options);
                assertEquals(options.size(), new HashSet<>(options).size(), node + " repeats an answer");
                assertEquals("", RowenaDialogue.next(node, RowenaDialogue.LEAVE, ctx));
            }
        }
    }

    @Test
    void theBookIsSpokenOfOnlyOnceAndOnlyByOneWhoHoldsItAfterAbaddon() {
        assertEquals(RowenaDialogue.BOOK_ASK, RowenaDialogue.next(RowenaDialogue.AGAIN, RowenaDialogue.OPT_BOOK, READY));
        assertEquals(List.of(RowenaDialogue.OPT_GIVE, RowenaDialogue.OPT_REFUSE, RowenaDialogue.BACK, RowenaDialogue.LEAVE),
                RowenaDialogue.options(RowenaDialogue.BOOK_ASK, READY), "give, keep, think it over, or go");
        assertEquals(RowenaDialogue.BOOK_GIVEN, RowenaDialogue.next(RowenaDialogue.BOOK_ASK, RowenaDialogue.OPT_GIVE, READY));
        assertEquals(RowenaDialogue.BOOK_KEPT, RowenaDialogue.next(RowenaDialogue.BOOK_ASK, RowenaDialogue.OPT_REFUSE, READY));
        assertEquals(RowenaDialogue.AGAIN, RowenaDialogue.next(RowenaDialogue.BOOK_ASK, RowenaDialogue.BACK, READY), "thinking it over is no choice");
        for (RowenaDialogue.Context ctx : ENDINGS) {
            boolean may = ctx.ending().bookReady() && ctx.ending().book() == RowenaRelation.Book.NONE;
            for (String node : RowenaDialogue.TOPIC_NODES) {
                assertEquals(may, RowenaDialogue.options(node, ctx).contains(RowenaDialogue.OPT_BOOK),
                        node + " offers the Book to " + ctx.ending());
            }
            assertEquals(may, RowenaDialogue.options(RowenaDialogue.BOOK_ASK, ctx).contains(RowenaDialogue.OPT_GIVE), "give, " + ctx.ending());
            assertEquals(may, RowenaDialogue.next(RowenaDialogue.BOOK_ASK, RowenaDialogue.OPT_REFUSE, ctx) != null, "keep, " + ctx.ending());
        }
    }

    @Test
    void aKeeperMeetsOnlyHerHostility() {
        for (RowenaDialogue.Context ctx : List.of(KEPT, KEPT_WON)) {
            assertEquals(RowenaDialogue.HOSTILE, RowenaDialogue.start(ctx), "she greets a keeper with hostility");
            assertEquals(List.of(RowenaDialogue.LEAVE), RowenaDialogue.options(RowenaDialogue.HOSTILE, ctx), "and nothing else to say");
            for (String node : RowenaDialogue.TOPIC_NODES) {
                assertEquals(List.of(RowenaDialogue.LEAVE), RowenaDialogue.options(node, ctx), node + ": no lessons, wares or favours for a keeper");
            }
            assertNull(RowenaDialogue.next(RowenaDialogue.AGAIN, RowenaDialogue.OPT_LESSONS, ctx), "no lessons for a keeper");
            assertNull(RowenaDialogue.next(RowenaDialogue.AGAIN, RowenaDialogue.OPT_UNMARK, ctx), "nor the cure");
        }
        assertEquals(List.of(RowenaDialogue.LEAVE), RowenaDialogue.options(RowenaDialogue.BOOK_KEPT, READY), "refusing her ends the talk");
    }

    @Test
    void aGiverMayAskForTheCovenThenForTheCure() {
        assertEquals(RowenaDialogue.AGAIN, RowenaDialogue.start(GIVEN), "an ally is greeted as one");
        for (String node : List.of(RowenaDialogue.AGAIN, RowenaDialogue.BOOK_GIVEN, RowenaDialogue.WHO)) {
            assertTrue(RowenaDialogue.options(node, GIVEN).contains(RowenaDialogue.OPT_COVEN), node + ": how to call the Grand Coven");
            assertFalse(RowenaDialogue.options(node, GIVEN).contains(RowenaDialogue.OPT_UNMARK), node + ": no cure before the Grand Witch falls");
        }
        assertEquals(RowenaDialogue.COVEN, RowenaDialogue.next(RowenaDialogue.AGAIN, RowenaDialogue.OPT_COVEN, GIVEN));
        assertFalse(RowenaDialogue.options(RowenaDialogue.COVEN, GIVEN).contains(RowenaDialogue.OPT_COVEN), "not asked twice in a row");
        assertTrue(RowenaDialogue.options(RowenaDialogue.COVEN, GIVEN).contains(RowenaDialogue.OPT_LESSONS), "the shop stays open to an ally");
        for (String node : List.of(RowenaDialogue.AGAIN, RowenaDialogue.UNMARKED, RowenaDialogue.NEEDS_BLOOD)) {
            assertFalse(RowenaDialogue.options(node, GIVEN_WON).contains(RowenaDialogue.OPT_COVEN), node + ": the Coven is beaten");
        }
        assertEquals(RowenaDialogue.UNMARK, RowenaDialogue.next(RowenaDialogue.AGAIN, RowenaDialogue.OPT_UNMARK, GIVEN_WON));
        assertEquals(RowenaDialogue.UNMARK, RowenaDialogue.next(RowenaDialogue.NEEDS_BLOOD, RowenaDialogue.OPT_UNMARK, GIVEN_WON),
                "asked again with the blood in hand");
        assertFalse(RowenaDialogue.options(RowenaDialogue.AGAIN, GIVEN_WON_CLEAN).contains(RowenaDialogue.OPT_UNMARK), "no Mark, no cure");
        assertFalse(RowenaDialogue.options(RowenaDialogue.AGAIN, NOT_READY).contains(RowenaDialogue.OPT_UNMARK), "no cure before the choice");
        assertFalse(RowenaDialogue.options(RowenaDialogue.AGAIN, NOT_READY).contains(RowenaDialogue.OPT_COVEN), "no Coven before the choice");
    }

    @Test
    void everyEndingNodeCanBeReached() {
        Set<String> reached = new HashSet<>();
        for (RowenaDialogue.Context ctx : ENDINGS) {
            reached.add(RowenaDialogue.start(ctx));
            ArrayDeque<String> todo = new ArrayDeque<>(List.of(RowenaDialogue.start(ctx), RowenaDialogue.AGAIN));
            Set<String> seen = new HashSet<>(todo);
            while (!todo.isEmpty()) {
                String node = todo.pop();
                for (String o : RowenaDialogue.options(node, ctx)) {
                    String next = RowenaDialogue.next(node, o, ctx);
                    if (RowenaDialogue.UNMARK.equals(next)) {
                        // The server answers the cure with one of these.
                        for (String n : List.of(RowenaDialogue.UNMARKED, RowenaDialogue.NEEDS_BLOOD)) if (seen.add(n)) todo.push(n);
                    } else if (next != null && RowenaDialogue.ALL_NODES.contains(next) && seen.add(next)) {
                        todo.push(next);
                    }
                }
            }
            reached.addAll(seen);
        }
        assertTrue(reached.containsAll(RowenaDialogue.ENDING_NODES), "unreached: " + RowenaDialogue.ENDING_NODES.stream().filter(n -> !reached.contains(n)).toList());
    }

    @Test
    void everyEndingLineAndAnswerIsWritten() {
        Map<String, String> lang = new HashMap<>();
        CovenLang.add((k, v) -> assertNull(lang.put(k, v), "twice: " + k));
        String d = "dialogue.supernaturalcraft." + RowenaDialogue.ID + ".";
        for (String node : RowenaDialogue.ALL_NODES) {
            assertTrue(lang.containsKey(d + node) && !lang.get(d + node).isBlank(), "no line for " + node);
            for (RowenaDialogue.Context ctx : ENDINGS) {
                for (String o : RowenaDialogue.options(node, ctx)) assertTrue(lang.containsKey(d + node + "." + o), "no answer " + node + "." + o);
            }
        }
        assertTrue(lang.get(d + RowenaDialogue.BOOK_ASK + "." + RowenaDialogue.OPT_GIVE).contains("Final")
                && lang.get(d + RowenaDialogue.BOOK_ASK + "." + RowenaDialogue.OPT_REFUSE).contains("Final"), "the choice says it is final");
    }
}
