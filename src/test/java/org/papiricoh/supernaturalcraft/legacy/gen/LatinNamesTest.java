package org.papiricoh.supernaturalcraft.legacy.gen;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatinNamesTest {

    @Test
    void namesAreDeterministicAndNeverRepeatInASequence() {
        long salt = GenSeed.of(42, new UUID(1, 2), "formula", 0);
        Set<String> seen = new HashSet<>();
        int n = LatinNames.space() * 2 + 50;
        for (int i = 0; i < n; i++) {
            String name = LatinNames.name(salt, i);
            assertEquals(name, LatinNames.name(salt, i));
            assertTrue(seen.add(name), "repeated name " + name + " at " + i);
        }
    }

    @Test
    void differentHuntersGetDifferentSequences() {
        long a = GenSeed.of(42, new UUID(1, 2), "formula", 0), b = GenSeed.of(42, new UUID(3, 4), "formula", 0);
        int same = 0;
        for (int i = 0; i < 20; i++) if (LatinNames.name(a, i).equals(LatinNames.name(b, i))) same++;
        assertTrue(same < 5);
        assertNotEquals(GenSeed.of(1, new UUID(1, 2), "rite", 0), GenSeed.of(2, new UUID(1, 2), "rite", 0));
    }

    @Test
    void incantationsArePlainLowercaseLatin() {
        for (long s = 0; s < 500; s++) {
            String inc = LatinNames.incantation(s * 7919, (int) (s % 6));
            assertTrue(inc.matches("[a-z ]+"), inc);
            int words = inc.split(" ").length;
            assertTrue(words >= 3 && words <= 10, inc);
            assertEquals(inc, LatinNames.incantation(s * 7919, (int) (s % 6)));
        }
    }

    @Test
    void romanNumerals() {
        assertEquals("II", LatinNames.roman(2));
        assertEquals("XIV", LatinNames.roman(14));
        assertEquals("MCMXCIX", LatinNames.roman(1999));
    }
}
