package org.papiricoh.supernaturalcraft.author;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The talk at the typewriter: everything reachable, "I'm ready" always at hand, no dead ends. */
class AuthorDialogueTest {

    private static final List<AuthorDialogue.Context> CONTEXTS = List.of(AuthorDialogue.Context.FIRST,
            new AuthorDialogue.Context(true, false, false, false), new AuthorDialogue.Context(true, false, true, false),
            new AuthorDialogue.Context(true, false, false, true), new AuthorDialogue.Context(true, true, true, true));

    private static Set<String> reachable(AuthorDialogue.Context ctx) {
        Set<String> seen = new HashSet<>();
        ArrayDeque<String> todo = new ArrayDeque<>(List.of(AuthorDialogue.start(ctx)));
        while (!todo.isEmpty()) {
            String node = todo.poll();
            if (!seen.add(node)) continue;
            for (String o : AuthorDialogue.options(node, ctx)) {
                String next = AuthorDialogue.next(node, o, ctx);
                assertNotNull(next, node + " offers " + o + " but does not accept it");
                if (!next.isEmpty() && !next.equals(AuthorDialogue.BEGIN)) todo.add(next);
            }
        }
        return seen;
    }

    @Test
    void everyTopicCanBeReached() {
        Set<String> all = new HashSet<>();
        for (AuthorDialogue.Context ctx : CONTEXTS) {
            Set<String> r = reachable(ctx);
            for (String t : AuthorDialogue.TOPICS) {
                if (t.equals(AuthorDialogue.DRAFT) && !ctx.victor() || t.equals("story")) continue;
                assertTrue(r.contains(t), t + " unreachable for " + ctx);
            }
            assertTrue(r.contains(AuthorDialogue.story(ctx)), "the hunter's story for " + ctx);
            assertTrue(r.contains(AuthorDialogue.READY));
            all.addAll(r);
        }
        assertEquals(new HashSet<>(AuthorDialogue.nodes()), all, "every node is said to someone");
    }

    @Test
    void readyIsAlwaysOfferedAndNothingIsADeadEnd() {
        for (AuthorDialogue.Context ctx : CONTEXTS) {
            for (String node : reachable(ctx)) {
                List<String> options = AuthorDialogue.options(node, ctx);
                assertTrue(options.size() >= 2, node + " offers too little");
                assertTrue(options.contains(AuthorDialogue.READY) || options.contains(AuthorDialogue.BEGIN), node + " without ready");
                assertTrue(options.size() <= 16, node + ": more options than the payload carries");
                assertTrue(AuthorDialogue.LINES.getOrDefault(node, 0) > 0, node + " says nothing");
            }
        }
    }

    @Test
    void everyLineAndOptionHasItsText() {
        java.util.Map<String, String> lang = new java.util.HashMap<>();
        org.papiricoh.supernaturalcraft.datagen.chuck.AuthorWorldLang.add((k, v) -> assertNull(lang.put(k, v), "twice: " + k));
        String d = "dialogue.supernaturalcraft.author.";
        for (var e : AuthorDialogue.LINES.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) assertTrue(lang.containsKey(d + e.getKey() + "." + i), "no line " + e.getKey() + "." + i);
            assertFalse(lang.containsKey(d + e.getKey() + "." + e.getValue()), e.getKey() + " has more lines than LINES says");
        }
        Set<String> options = new HashSet<>(AuthorDialogue.TOPICS);
        options.addAll(List.of(AuthorDialogue.READY, AuthorDialogue.BEGIN, AuthorDialogue.NOT_YET, AuthorDialogue.LEAVE));
        for (String o : options) assertTrue(lang.containsKey(d + "option." + o), "no option " + o);
        for (String b : List.of("azazel", "lilith", "lucifer", "broken_chorus", "metatron", "amara", "lucifer_uncaged",
                "title", "start", "nothing", "unfinished", "waiting", "expected")) {
            assertTrue(lang.containsKey("typewriter.supernaturalcraft.page." + b), "no page line " + b);
        }
    }

    @Test
    void theRematchIsOnlyForVictors() {
        assertFalse(AuthorDialogue.options("who", AuthorDialogue.Context.FIRST).contains(AuthorDialogue.DRAFT));
        assertTrue(AuthorDialogue.options("who", new AuthorDialogue.Context(true, true, false, false)).contains(AuthorDialogue.DRAFT));
        assertNull(AuthorDialogue.next("who", AuthorDialogue.DRAFT, AuthorDialogue.Context.FIRST), "a draft cannot be forced");
        assertEquals(AuthorDialogue.BEGIN, AuthorDialogue.next(AuthorDialogue.READY, AuthorDialogue.BEGIN, AuthorDialogue.Context.FIRST));
        assertEquals("", AuthorDialogue.next("hello", AuthorDialogue.LEAVE, AuthorDialogue.Context.FIRST));
        assertNull(AuthorDialogue.next("hello", AuthorDialogue.BEGIN, AuthorDialogue.Context.FIRST), "begin only after ready");
    }
}
