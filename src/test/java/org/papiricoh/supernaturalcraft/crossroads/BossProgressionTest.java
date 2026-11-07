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
        // v0.40, season 4: Alastair at the warehouse (Uriel is a side road), then Lilith, the last seal.
        assertSame(Boss.ALASTAIR, BossProgression.next(done::contains));
        done.add("main/on_the_head_of_a_pin");
        assertSame(Boss.LILITH, BossProgression.next(done::contains));
        done.add("main/lucifer_rising");
        // Season 5: the three Horsemen, in any order the book lists them, then Death, then Lucifer.
        assertSame(Boss.WAR, BossProgression.next(done::contains));
        done.add("main/war");
        done.add("main/famine");
        done.add("main/pestilence");
        assertSame(Boss.DEATH, BossProgression.next(done::contains));
        done.add("main/pale_rider");
        assertSame(Boss.LUCIFER, BossProgression.next(done::contains), "season 5 closes with Lucifer");
        done.add("main/devil_went_down");
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
        // The Darkness right after the Word.
        assertSame(Boss.AMARA, BossProgression.next(done::contains));
        done.add("main/dawn");
        // v0.41, seasons 12-14 (Ketch, Hess and Asmodeus are side roads): Dagon, both Michaels, Jack, the Shadow.
        assertSame(Boss.DAGON, BossProgression.next(done::contains));
        done.add("main/all_along_the_watchtower");
        assertSame(Boss.AU_MICHAEL, BossProgression.next(done::contains));
        done.add("main/let_the_good_times_roll");
        assertSame(Boss.MICHAEL_VESSEL, BossProgression.next(done::contains));
        done.add("main/the_spear");
        assertSame(Boss.SOULLESS_JACK, BossProgression.next(done::contains));
        done.add("main/moriah");
        assertSame(Boss.SHADOW, BossProgression.next(done::contains));
        done.add("main/despair");
        // Back in the Box, the bridge from season 14 to 15: the Cage, and Michael.
        assertSame(Boss.LUCIFER_UNCAGED, BossProgression.next(done::contains));
        done.add("main/back_in_the_box");
        assertSame(Boss.MICHAEL, BossProgression.next(done::contains));
        done.add("main/sword_of_heaven");
        // Michael is the last before the Author.
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
        assertTrue(Boss.URIEL.optional);
        Set<String> all = new HashSet<>();
        for (Boss b : Boss.values()) if (b != Boss.CHUCK && !b.optional) all.add(b.advancement);
        assertTrue(BossProgression.allBeforeChuck(all::contains), "Gabriel is a side road");
        Set<String> lilith = Set.of("main/yellow_eyed", "main/on_the_head_of_a_pin", "main/lucifer_rising");
        assertSame(Boss.WAR, BossProgression.next(lilith::contains), "after Lilith the next is War, not Gabriel");
        assertSame(Boss.ALASTAIR, BossProgression.next(Set.of("main/yellow_eyed")::contains), "after Azazel Alastair, not Uriel");
        Set<String> horsemen = new HashSet<>(lilith);
        horsemen.addAll(Set.of("main/war", "main/famine", "main/pestilence"));
        assertSame(Boss.DEATH, BossProgression.next(horsemen::contains), "after the Horsemen Death, not Raphael");
        horsemen.add("main/pale_rider");
        assertSame(Boss.LUCIFER, BossProgression.next(horsemen::contains), "after Death Lucifer, not Raphael");
    }

    @Test
    void keys() {
        assertEquals("book.supernaturalcraft.crossroads.lucifer_uncaged", Boss.LUCIFER_UNCAGED.key());
    }
}
