package org.papiricoh.supernaturalcraft.heaven.zachariah;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.Docket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "It was already written" (v0.18): the docket is honest, but for one revision a phase. */
class DocketTest {

    /** A docket that writes a, b, c, d, e... in turn. */
    private static Docket written(int capacity, Iterator<String> source) {
        Docket d = new Docket();
        d.open(capacity);
        d.fill("", prev -> source.next());
        return d;
    }

    private static Iterator<String> letters() {
        return new Iterator<>() {
            char c = 'a';

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public String next() {
                return String.valueOf(c++);
            }
        };
    }

    @Test
    void whatIsAnnouncedIsWhatComes() {
        Iterator<String> src = letters();
        Docket d = written(3, src);
        assertEquals(List.of("a", "b", "c"), d.entries());
        assertEquals("a,b,c", d.text());
        List<String> announced = new ArrayList<>(d.entries());
        List<String> done = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            done.add(d.next());
            d.fill(done.getLast(), prev -> src.next());
            assertEquals(3, d.size(), "kept full");
            if (announced.size() < 9) announced.add(d.entries().getLast());
        }
        assertEquals(announced.subList(0, 6), done, "the docket never lies without a revision");
        assertEquals(6, d.taken());
    }

    @Test
    void oneRevisionAPhaseStruckThroughBeforeItChanges() {
        Docket d = written(5, letters());
        assertTrue(d.revise(2, "x", 120));
        assertEquals("c", d.entries().get(2), "struck through, but not yet changed");
        assertFalse(d.revise(3, "y", 130), "only one a phase");
        assertFalse(d.tick(119), "not before its time");
        assertTrue(d.tick(120));
        assertEquals(List.of("a", "b", "x", "d", "e"), d.entries());
        assertNull(d.pending());
        assertFalse(d.revise(1, "z", 200), "still used this phase");
        d.open(5);
        d.fill("", prev -> "q" + d.size());
        assertTrue(d.revise(1, "z", 200), "a new phase, a new revision");
    }

    @Test
    void aRevisionIsNeverIntoTheSameAttackNorOutOfRange() {
        Docket d = written(3, letters());
        assertFalse(d.revise(1, "b", 10), "into itself");
        assertFalse(d.revise(3, "x", 10), "out of range");
        assertFalse(d.revise(-1, "x", 10));
        assertFalse(d.revised(), "none of those count");
    }

    @Test
    void takingTheHeadLandsItsPendingRevisionFirst() {
        Docket d = written(3, letters());
        assertTrue(d.revise(1, "x", 1000));
        assertEquals("a", d.next());
        assertEquals(0, d.pending().index(), "the revision follows its entry up the queue");
        assertEquals("x", d.next(), "taken before its time: the amendment lands first");
        assertNull(d.pending());
    }

    @Test
    void aClosedDocketIsEmpty() {
        Docket d = new Docket();
        d.open(0);
        assertFalse(d.active());
        assertFalse(d.fill("", prev -> "a"));
        assertNull(d.next());
        assertEquals("", d.text());
    }
}
