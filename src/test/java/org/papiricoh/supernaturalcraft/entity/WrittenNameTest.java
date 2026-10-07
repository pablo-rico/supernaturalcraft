package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.ritual.WrittenName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrittenNameTest {

    @Test
    void anyCaseAnywhereAcrossPages() {
        assertTrue(WrittenName.holds(List.of("Metatron"), null, "Metatron"));
        assertTrue(WrittenName.holds(List.of("I call upon m e t a t r o n!"), null, "Metatron"));
        assertTrue(WrittenName.holds(List.of("hear me, meta", "tron, scribe"), null, "Metatron"), "split over two pages");
        assertTrue(WrittenName.holds(List.of(), "METATRON's book", "Metatron"), "the title counts");
        assertFalse(WrittenName.holds(List.of("Metatr0n"), null, "Metatron"));
        assertFalse(WrittenName.holds(List.of(""), "", "Metatron"));
    }
}
