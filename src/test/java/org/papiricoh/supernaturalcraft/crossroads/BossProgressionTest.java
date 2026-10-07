package org.papiricoh.supernaturalcraft.crossroads;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class BossProgressionTest {

    @Test
    void namesTheFirstEnemyNotYetBeaten() {
        Set<String> done = new HashSet<>();
        assertSame(Boss.AZAZEL, BossProgression.next(done::contains));
        done.add("main/yellow_eyed");
        assertSame(Boss.LILITH, BossProgression.next(done::contains));
        done.add("main/lucifer_rising");
        done.add("main/devil_went_down");
        assertSame(Boss.BROKEN_CHORUS, BossProgression.next(done::contains));
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
    void keys() {
        assertEquals("book.supernaturalcraft.crossroads.lucifer_uncaged", Boss.LUCIFER_UNCAGED.key());
    }
}
