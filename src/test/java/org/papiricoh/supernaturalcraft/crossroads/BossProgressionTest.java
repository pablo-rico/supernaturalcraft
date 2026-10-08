package org.papiricoh.supernaturalcraft.crossroads;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossProgressionTest {

    @Test
    void namesTheFirstEnemyNotYetBeaten() {
        Set<String> done = new HashSet<>();
        assertSame(Boss.AZAZEL, BossProgression.next(done::contains));
        done.add("main/yellow_eyed");
        assertSame(Boss.LILITH, BossProgression.next(done::contains));
        done.add("main/lucifer_rising");
        done.add("main/devil_went_down");
        // The three Horsemen come after Lucifer, in any order the book lists them.
        assertSame(Boss.WAR, BossProgression.next(done::contains));
        done.add("main/war");
        done.add("main/famine");
        done.add("main/pestilence");
        assertSame(Boss.BROKEN_CHORUS, BossProgression.next(done::contains));
        done.add("main/silence_falls");
        done.add("main/scribe_of_god");
        done.add("main/dawn");
        // Death, offered their rings, just before the Cage opens.
        assertSame(Boss.DEATH, BossProgression.next(done::contains));
        done.add("main/pale_rider");
        assertSame(Boss.LUCIFER_UNCAGED, BossProgression.next(done::contains));
        done.add("main/back_in_the_box");
        // Michael, the end of Heaven's road, is the last before the Author.
        assertSame(Boss.MICHAEL, BossProgression.next(done::contains));
        done.add("main/sword_of_heaven");
        assertSame(Boss.CHUCK, BossProgression.next(done::contains));
        // A gap earlier in the order wins over later kills.
        assertSame(Boss.AZAZEL, BossProgression.next(Set.of("main/devil_went_down", "main/dawn")::contains));
    }

    @Test
    void nothingLeftAfterTheLast() {
        Set<String> all = new HashSet<>();
        for (Boss b : Boss.values()) all.add(b.advancement);
        assertNull(BossProgression.next(all::contains));
    }

    @Test
    void theAuthorWaitsForAllFourHorsemen() {
        Set<String> all = new HashSet<>();
        for (Boss b : Boss.values()) if (b != Boss.CHUCK) all.add(b.advancement);
        org.junit.jupiter.api.Assertions.assertTrue(BossProgression.allBeforeChuck(all::contains));
        all.remove("main/pale_rider");
        org.junit.jupiter.api.Assertions.assertFalse(BossProgression.allBeforeChuck(all::contains));
    }

    @Test
    void anOptionalBossIsNeverNextNorAskedForByTheAuthor() {
        assertTrue(Boss.GABRIEL.optional);
        assertTrue(Boss.RAPHAEL.optional);
        Set<String> all = new HashSet<>();
        for (Boss b : Boss.values()) if (b != Boss.CHUCK && !b.optional) all.add(b.advancement);
        assertTrue(BossProgression.allBeforeChuck(all::contains), "Gabriel is a side road");
        Set<String> lucifer = Set.of("main/yellow_eyed", "main/lucifer_rising", "main/devil_went_down");
        assertSame(Boss.WAR, BossProgression.next(lucifer::contains), "after Lucifer the next is War, not Gabriel");
        Set<String> horsemen = new HashSet<>(lucifer);
        horsemen.addAll(Set.of("main/war", "main/famine", "main/pestilence"));
        assertSame(Boss.BROKEN_CHORUS, BossProgression.next(horsemen::contains), "after the Horsemen the Chorus, not Raphael");
    }

    @Test
    void keys() {
        assertEquals("book.supernaturalcraft.crossroads.lucifer_uncaged", Boss.LUCIFER_UNCAGED.key());
    }
}
