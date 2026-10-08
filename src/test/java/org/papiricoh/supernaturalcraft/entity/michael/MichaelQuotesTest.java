package org.papiricoh.supernaturalcraft.entity.michael;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.datagen.michael.MichaelLang;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelQuotes;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MichaelQuotesTest {

    @Test
    void everyLineHeSaysExists() {
        Map<String, String> lang = new HashMap<>();
        MichaelLang.add(lang::put);
        assertEquals(MichaelLang.QUOTES, MichaelQuotes.LINES);
        assertEquals(MichaelLang.QUOTES_AFTER_UNCAGED, MichaelQuotes.UNCAGED_LINES);
        for (boolean uncaged : new boolean[]{false, true}) {
            for (int phase = 1; phase <= 6; phase++) {
                String key = MichaelQuotes.onPhase(phase, uncaged);
                assertTrue(lang.containsKey(key), "missing " + key);
            }
            for (int roll = 0; roll < 200; roll++) {
                String key = MichaelQuotes.any(roll, uncaged);
                assertTrue(lang.containsKey(key), "missing " + key);
                if (!uncaged) assertTrue(!key.contains(".uncaged."), "his brother is still free: " + key);
            }
        }
        assertEquals("message.supernaturalcraft.michael.quote.0", MichaelQuotes.onPhase(1, false), "he arrives asking for your yes");
        assertTrue(MichaelQuotes.onPhase(5, true).contains(".uncaged."), "after Uncaged, the Archangel speaks of the Cage");
    }
}
