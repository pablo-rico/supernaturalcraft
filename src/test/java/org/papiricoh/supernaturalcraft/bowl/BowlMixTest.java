package org.papiricoh.supernaturalcraft.bowl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BowlMixTest {

    @Test
    void colourIsTheAverageOfTheDoses() {
        assertEquals(0xFF0000, BowlMix.mixColor(List.of(0xFF0000)));
        assertEquals(0x7F007F, BowlMix.mixColor(List.of(0xFF0000, 0x0000FF)));
        assertEquals(0xAA0055, BowlMix.mixColor(List.of(0xFF0000, 0xFF0000, 0x0000FF)), "each dose weighs the same");
        assertEquals(BowlMix.EMPTY_COLOR, BowlMix.mixColor(List.of()));
        assertEquals(0xBF003F, BowlMix.mixColor(List.of(0xFF0000, 0x0000FF), List.of(3, 1)), "a strong tint outweighs a weak one");
    }

    @Test
    void liquidsMatchAsAMultiset() {
        assertTrue(BowlMix.sameLiquids(List.of(BowlLiquid.WATER, BowlLiquid.BLOOD), List.of(BowlLiquid.BLOOD, BowlLiquid.WATER)), "any order");
        assertFalse(BowlMix.sameLiquids(List.of(BowlLiquid.WATER, BowlLiquid.WATER), List.of(BowlLiquid.WATER)), "counts matter");
        assertFalse(BowlMix.sameLiquids(List.of(BowlLiquid.WATER, BowlLiquid.WATER), List.of(BowlLiquid.WATER, BowlLiquid.HOLY_WATER)));
        assertTrue(BowlMix.sameLiquids(List.of(), List.of()));
    }

    @Test
    void liquidIdsRoundTrip() {
        for (BowlLiquid l : BowlLiquid.values()) assertEquals(l, BowlLiquid.fromId(l.id()));
        assertEquals("holy_water", BowlLiquid.HOLY_WATER.id());
        assertNull(BowlLiquid.fromId("lava"));
    }

    @Test
    void liquidHeightRisesWithDoses() {
        float empty = BowlMix.liquidHeight(0), full = BowlMix.liquidHeight(BowlContents.MAX_DOSES);
        assertEquals(1.2f / 16f, empty, 1e-6);
        assertEquals(4.4f / 16f, full, 1e-6);
        assertTrue(BowlMix.liquidHeight(2) > empty && BowlMix.liquidHeight(2) < full);
    }
}
