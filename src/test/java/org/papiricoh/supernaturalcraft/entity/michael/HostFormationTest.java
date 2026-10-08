package org.papiricoh.supernaturalcraft.entity.michael;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostFormation;
import org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostFormation.Order;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostFormationTest {

    @Test
    void everySlotIsItsOwn() {
        for (Order order : Order.values()) {
            for (int size = 1; size <= 10; size++) {
                List<double[]> slots = HostFormation.slots(order, size);
                assertEquals(size, slots.size());
                for (int i = 0; i < size; i++) {
                    for (int j = i + 1; j < size; j++) {
                        double dx = slots.get(i)[0] - slots.get(j)[0], dz = slots.get(i)[1] - slots.get(j)[1];
                        assertTrue(dx * dx + dz * dz >= 1.0, order + " x" + size + ": slots " + i + " and " + j + " overlap");
                    }
                }
            }
        }
    }

    @Test
    void theCaptainLeadsTheWallAndTheWedge() {
        double[] wall = HostFormation.slots(Order.SHIELD_WALL, 7).get(0);
        assertEquals(0, wall[1], 1e-9, "the captain stands in the front rank");
        assertTrue(Math.abs(wall[0]) <= HostFormation.SPACING / 2 + 1e-9, "...at its centre");
        double[] wedge = HostFormation.slots(Order.CHARGE, 8).get(0);
        for (double[] s : HostFormation.slots(Order.CHARGE, 8)) assertTrue(s[1] <= wedge[1], "the captain is the wedge's point");
        for (double[] s : HostFormation.slots(Order.ENCIRCLE, 8).subList(1, 8)) {
            assertEquals(HostFormation.RING, Math.hypot(s[0], s[1]), 1e-6, "the ring is round the foe");
        }
    }

    @Test
    void theCompanyBreaksWhenItsCaptainFalls() {
        HostFormation f = new HostFormation();
        f.give(Order.CHARGE);
        assertNotNull(f.slot(3, 7));
        assertTrue(f.speed() > 1);
        f.captainFalls();
        assertTrue(f.disordered());
        assertNull(f.slot(3, 7), "no formation without a captain");
        assertEquals(HostFormation.DISORDER_SPEED, f.speed(), 1e-6);
        f.give(Order.SHIELD_WALL);
        assertEquals(Order.CHARGE, f.order(), "a broken company takes no more orders");
    }

    @Test
    void ordersKeepChanging() {
        for (Order last : Order.values()) {
            for (double d : new double[]{3, 7, 10, 20}) {
                for (int foes = 1; foes <= 3; foes++) assertNotEquals(last, HostFormation.next(last, d, foes));
            }
        }
        assertEquals(Order.CHARGE, HostFormation.next(Order.SHIELD_WALL, 20, 2));
        assertEquals(Order.ENCIRCLE, HostFormation.next(Order.SHIELD_WALL, 5, 1));
    }
}
