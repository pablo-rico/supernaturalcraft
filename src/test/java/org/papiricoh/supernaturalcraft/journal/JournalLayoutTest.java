package org.papiricoh.supernaturalcraft.journal;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.journal.JournalLayout.Measured;
import org.papiricoh.supernaturalcraft.journal.JournalLayout.Slice;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalLayoutTest {

    private static Measured lines(int n) {
        return Measured.text(Collections.nCopies(n, 10));
    }

    @Test
    void textFlowsOnToTheNextFaceBetweenLines() {
        // 100 high, title 20 + gap 6: 7 lines fit on the first face, 10 on each after.
        var faces = JournalLayout.layout(List.of(lines(25)), 100, 20, 6);
        assertEquals(3, faces.size());
        assertEquals(List.of(new Slice(0, 0, 6, 26)), faces.get(0), "the title's face, spaced from the title");
        assertEquals(List.of(new Slice(0, 7, 16, 0)), faces.get(1), "a face never starts with a gap");
        assertEquals(List.of(new Slice(0, 17, 24, 0)), faces.get(2));
    }

    @Test
    void everyLineIsLaidOnceInOrder() {
        var faces = JournalLayout.layout(List.of(lines(7), Measured.fixed(30), lines(13), lines(4)), 80, 0, 4);
        int[] next = new int[4];
        int lastBlock = 0;
        for (var face : faces) {
            for (Slice s : face) {
                assertTrue(s.block() >= lastBlock, "blocks stay in order");
                lastBlock = s.block();
                assertEquals(next[s.block()], s.first(), "no line skipped or repeated");
                next[s.block()] = s.last() + 1;
            }
        }
        assertEquals(7, next[0]);
        assertEquals(13, next[2]);
        assertEquals(4, next[3]);
    }

    @Test
    void aPictureNeverSplits() {
        // 5 lines (50) + gap + a 60 picture do not fit in 100: the picture moves whole.
        var faces = JournalLayout.layout(List.of(lines(5), Measured.fixed(60), lines(2)), 100, 0, 6);
        assertEquals(2, faces.size());
        assertEquals(List.of(new Slice(0, 0, 4, 0)), faces.get(0));
        assertEquals(List.of(new Slice(1, 0, 0, 0), new Slice(2, 0, 1, 66)), faces.get(1));
        for (var face : faces) {
            for (Slice s : face) {
                if (s.block() == 1) assertTrue(s.y() + 60 <= 100, "the picture is whole on its face");
            }
        }
    }

    @Test
    void aPictureThatFitsStaysWithTheText() {
        var faces = JournalLayout.layout(List.of(lines(3), Measured.fixed(40)), 100, 0, 6);
        assertEquals(1, faces.size());
        assertEquals(List.of(new Slice(0, 0, 2, 0), new Slice(1, 0, 0, 36)), faces.get(0));
    }

    @Test
    void anOversizePictureGetsAFaceOfItsOwn() {
        var faces = JournalLayout.layout(List.of(lines(2), Measured.fixed(150), lines(1)), 100, 10, 6);
        assertEquals(3, faces.size());
        assertEquals(List.of(new Slice(0, 0, 1, 16)), faces.get(0));
        assertEquals(List.of(new Slice(1, 0, 0, 0)), faces.get(1), "alone, even though it overflows");
        assertEquals(List.of(new Slice(2, 0, 0, 0)), faces.get(2), "what follows starts a new face");
    }

    @Test
    void aPictureTooTallForTheTitlesFaceMovesOn() {
        var faces = JournalLayout.layout(List.of(Measured.fixed(90)), 100, 30, 6);
        assertEquals(2, faces.size());
        assertTrue(faces.get(0).isEmpty(), "the first face keeps only the title");
        assertEquals(List.of(new Slice(0, 0, 0, 0)), faces.get(1));
    }

    @Test
    void anEmptyEntryIsOneFaceWithItsTitle() {
        var faces = JournalLayout.layout(List.of(), 100, 20, 6);
        assertEquals(1, faces.size());
        assertTrue(faces.getFirst().isEmpty());
        assertEquals(1, JournalLayout.layout(List.of(Measured.text(List.of())), 100, 0, 6).size(), "an empty paragraph adds nothing");
    }

    @Test
    void theFirstBlockSitsUnderTheTitle() {
        var faces = JournalLayout.layout(List.of(Measured.fixed(20)), 100, 0, 6);
        assertEquals(List.of(new Slice(0, 0, 0, 0)), faces.getFirst(), "no title, no gap");
    }
}
