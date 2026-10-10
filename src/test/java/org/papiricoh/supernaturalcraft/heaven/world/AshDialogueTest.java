package org.papiricoh.supernaturalcraft.heaven.world;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.datagen.heaven.HeavenServerLang;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.AshDialogue;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What Ash tells a hunter at the Roadhouse (v0.18). */
class AshDialogueTest {

    private static AshDialogue.Progress progress(boolean hasPlot, int memories, int gathered, int naomi, int zachariah, boolean home,
                                                 boolean rested, String next, boolean welcome, int visitable) {
        return new AshDialogue.Progress(false, hasPlot, memories, gathered, 3, naomi, zachariah, home, rested, next, welcome, visitable);
    }

    private static List<String> keys(AshDialogue.Progress p) {
        return AshDialogue.hints(p, 0).stream().map(AshDialogue.Line::key).toList();
    }

    private static String hint(String name) {
        return AshDialogue.PREFIX + "hint." + name;
    }

    @Test
    void withoutAHeavenHeSaysHowToGetOne() {
        List<String> k = keys(progress(false, 0, 0, 0, 0, false, false, null, false, 0));
        assertEquals(hint("no_plot"), k.getFirst());
    }

    @Test
    void theWingNeedsMemories() {
        List<AshDialogue.Line> lines = AshDialogue.hints(progress(true, 5, 1, 0, 0, false, false, null, true, 0), 0);
        assertEquals(hint("wing_sealed"), lines.getFirst().key());
        assertEquals(List.of("2"), lines.getFirst().args());
        assertTrue(lines.stream().anyMatch(l -> l.key().equals(hint("memories")) && l.args().equals(List.of("4"))));
    }

    @Test
    void thenNaomiThenZachariahThenTheHearth() {
        assertEquals(hint("naomi"), keys(progress(true, 3, 3, 0, 0, false, false, null, true, 0)).getFirst());
        assertEquals(hint("zachariah"), keys(progress(true, 3, 3, 1, 0, false, false, null, true, 0)).getFirst());
        assertEquals(hint("hearth"), keys(progress(true, 3, 3, 1, 1, true, false, null, true, 0)).getFirst());
        assertEquals(hint("homecoming"), keys(progress(true, 3, 3, 1, 1, true, true, null, true, 0)).getFirst());
        assertFalse(keys(progress(true, 3, 3, 1, 1, true, true, null, true, 0)).contains(hint("naomi")));
    }

    @Test
    void theNextBossIsNamedByItsKey() {
        List<AshDialogue.Line> lines = AshDialogue.all(progress(true, 3, 3, 1, 1, true, true,
                "book.supernaturalcraft.crossroads.amara.name", true, 0));
        AshDialogue.Line next = lines.stream().filter(l -> l.key().equals(hint("next_boss"))).findFirst().orElseThrow();
        assertEquals(List.of("@book.supernaturalcraft.crossroads.amara.name"), next.args());
    }

    @Test
    void visitsOrTheWelcome() {
        assertTrue(AshDialogue.all(progress(true, 3, 3, 1, 1, true, true, null, false, 2)).stream()
                .anyMatch(l -> l.key().equals(hint("visits")) && l.args().equals(List.of("2"))));
        assertTrue(AshDialogue.all(progress(true, 3, 3, 1, 1, true, true, null, false, 0)).stream()
                .anyMatch(l -> l.key().equals(hint("welcome"))));
        assertFalse(AshDialogue.all(progress(true, 3, 3, 1, 1, true, true, null, true, 0)).stream()
                .anyMatch(l -> l.key().equals(hint("welcome"))));
    }

    @Test
    void neverTooManyTheMostPressingFirstAndTheRestTurn() {
        AshDialogue.Progress busy = progress(true, 9, 1, 0, 0, false, false, "book.supernaturalcraft.crossroads.amara.name", false, 4);
        List<AshDialogue.Line> all = AshDialogue.all(busy);
        assertTrue(all.size() > AshDialogue.MAX_HINTS);
        var seen = new HashSet<String>();
        for (int r = 0; r < 8; r++) {
            List<AshDialogue.Line> h = AshDialogue.hints(busy, r);
            assertEquals(AshDialogue.MAX_HINTS, h.size());
            assertEquals(all.getFirst(), h.getFirst());
            assertEquals(h.size(), new HashSet<>(h).size(), "a hint repeats");
            h.forEach(l -> seen.add(l.key()));
        }
        assertEquals(all.size(), seen.size(), "asking again should bring every hint round");
    }

    @Test
    void withNothingToTellHeRambles() {
        AshDialogue.Progress done = new AshDialogue.Progress(false, true, 3, 3, 3, 1, 1, false, false, null, true, 0);
        List<AshDialogue.Line> h = AshDialogue.hints(done, 5);
        assertEquals(1, h.size());
        assertTrue(h.getFirst().key().startsWith(AshDialogue.PREFIX + "ramble."));
    }

    @Test
    void greetings() {
        AshDialogue.Progress first = new AshDialogue.Progress(true, false, 0, 0, 3, 0, 0, false, false, null, false, 0);
        assertEquals(AshDialogue.PREFIX + "greet.first", AshDialogue.greeting(first, 0));
        AshDialogue.Progress again = progress(true, 0, 0, 0, 0, false, false, null, false, 0);
        assertTrue(AshDialogue.keys().contains(AshDialogue.greeting(again, 7)));
    }

    @Test
    void everyLineHasItsText() {
        for (String key : AshDialogue.keys()) assertTrue(HeavenServerLang.ASH_LINES.containsKey(key), "no English for " + key);
        for (String name : AshDialogue.HINTS) assertTrue(AshDialogue.keys().contains(hint(name)));
    }
}
