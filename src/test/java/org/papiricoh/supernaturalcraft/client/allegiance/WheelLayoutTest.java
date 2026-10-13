package org.papiricoh.supernaturalcraft.client.allegiance;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;

import static org.junit.jupiter.api.Assertions.*;

class WheelLayoutTest {

    @Test
    void upToEightTheWheelKeepsItsSize() {
        for (int n = 1; n <= WheelLayout.COMPACT; n++) assertEquals(82, WheelLayout.of(n).outer());
        assertTrue(WheelLayout.of(WheelLayout.COMPACT + 1).outer() > 82);
    }

    @Test
    void everyIconHasRoomOnTheRing() {
        for (int n = 1; n <= Power.WHEEL_MAX; n++) {
            WheelLayout l = WheelLayout.of(n);
            assertTrue(l.chord(n) >= WheelLayout.ICON + 8, n + " slices: icons overlap");
            // Icon and its cost line sit inside the ring (the art's inner edge to the rim).
            assertTrue(l.iconR() - WheelLayout.ICON / 2f >= l.ringInner(true), n + " slices: icon falls into the hub");
            assertTrue(l.iconR() + WheelLayout.ICON / 2f + 8 <= l.outer(), n + " slices: icon runs off the rim");
        }
    }

    @Test
    void eachSliceIsFoundWhereItIsDrawn() {
        for (int n = 1; n <= Power.WHEEL_MAX; n++) {
            WheelLayout l = WheelLayout.of(n);
            for (int i = 0; i < n; i++) {
                double a = WheelLayout.mid(i, n);
                assertEquals(i, l.slice(Math.sin(a) * l.iconR(), -Math.cos(a) * l.iconR(), n), n + " slices, slice " + i);
            }
            assertEquals(-1, l.slice(0, 0, n), "the hub selects nothing");
        }
    }
}
