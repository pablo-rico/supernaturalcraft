package org.papiricoh.supernaturalcraft.bowl.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindingMathTest {

    @Test
    void insideStaysPut() {
        assertNull(BindingMath.pullBack(0, 64, 0, 3, 64, 0, 4, false));
        assertNull(BindingMath.pullBack(0, 64, 0, 0, 64, 4, 4, false), "the edge itself is inside");
    }

    @Test
    void outsideIsPulledToJustInsideTheEdge() {
        double[] p = BindingMath.pullBack(10, 64, 10, 20, 64, 10, 4, false);
        assertArrayEquals(new double[]{10 + 4 - BindingMath.INSET, 64, 10}, p, 1e-9);
        assertFalse(BindingMath.outside(10, 64, 10, p[0], p[1], p[2], 4, false), "pulled back inside");
    }

    @Test
    void walkersKeepTheirHeightFlyersDoNot() {
        // Straight above the bowl: inside a cylinder, outside a sphere.
        assertNull(BindingMath.pullBack(0, 64, 0, 0, 74, 0, 4, false));
        double[] up = BindingMath.pullBack(0, 64, 0, 0, 74, 0, 4, true);
        assertArrayEquals(new double[]{0, 64 + 4 - BindingMath.INSET, 0}, up, 1e-9);
        double[] diag = BindingMath.pullBack(0, 64, 0, 6, 70, 8, 4, false);
        assertArrayEquals(new double[]{3.5 * 0.6, 70, 3.5 * 0.8}, diag, 1e-9);
    }

    @Test
    void outsideIsStrict() {
        assertTrue(BindingMath.outside(0, 0, 0, 4.01, 0, 0, 4, true));
        assertFalse(BindingMath.outside(0, 0, 0, 4, 0, 0, 4, true));
    }
}
