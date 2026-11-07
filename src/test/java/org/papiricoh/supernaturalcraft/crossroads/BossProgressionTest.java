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
        // v0.38, chapters: Death, offered their rings, closes the Apocalypse...
        assertSame(Boss.DEATH, BossProgression.next(done::contains));
        done.add("main/pale_rider");
        // v0.39, season 6: the war in Heaven, the new God, the Leviathans and Purgatory.
        assertSame(Boss.RAPHAEL, BossProgression.next(done::contains));
        done.add("main/free_to_be_you_and_me");
        assertSame(Boss.GODSTIEL, BossProgression.next(done::contains));
        done.add("main/the_man_who_knew_too_much");
        assertSame(Boss.DICK_ROMAN, BossProgression.next(done::contains));
        done.add("main/survival_of_the_fittest");
        assertSame(Boss.EVE, BossProgression.next(done::contains));
        done.add("main/mommy_dearest");
        // ...and the Word of God opens with the Chorus and Metatron.
        assertSame(Boss.BROKEN_CHORUS, BossProgression.next(done::contains));
        done.add("main/silence_falls");
        assertSame(Boss.METATRON, BossProgression.next(done::contains));
        done.add("main/scribe_of_god");
        // Back to the Cage, and Michael.
        assertSame(Boss.LUCIFER_UNCAGED, BossProgression.next(done::contains));
        done.add("main/back_in_the_box");
        assertSame(Boss.MICHAEL, BossProgression.next(done::contains));
        done.add("main/sword_of_heaven");
        // The Darkness is the last before the Author.
        assertSame(Boss.AMARA, BossProgression.next(done::contains));
        done.add("main/dawn");
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
        assertTrue(Boss.CROWLEY.optional);
        Set<String> all = new HashSet<>();
        for (Boss b : Boss.values()) if (b != Boss.CHUCK && !b.optional) all.add(b.advancement);
        assertTrue(BossProgression.allBeforeChuck(all::contains), "Gabriel is a side road");
        Set<String> lucifer = Set.of("main/yellow_eyed", "main/lucifer_rising", "main/devil_went_down");
        assertSame(Boss.WAR, BossProgression.next(lucifer::contains), "after Lucifer the next is War, not Gabriel");
        Set<String> horsemen = new HashSet<>(lucifer);
        horsemen.addAll(Set.of("main/war", "main/famine", "main/pestilence"));
        assertSame(Boss.DEATH, BossProgression.next(horsemen::contains), "after the Horsemen Death, not Raphael");
    }

    @Test
    void keys() {
        assertEquals("book.supernaturalcraft.crossroads.lucifer_uncaged", Boss.LUCIFER_UNCAGED.key());
    }
}
