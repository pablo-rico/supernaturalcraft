package org.papiricoh.supernaturalcraft.entity.horsemen;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarFury;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarFuryTest {

    @Test
    void blowsFeedItAndStandardsKeepFeedingIt() {
        WarFury f = new WarFury();
        f.onHurt(20);
        assertEquals(20 * WarFury.PER_DAMAGE, f.value(), 1e-4);
        float before = f.value();
        f.tickSecond(4);
        assertEquals(before + 4 * WarFury.PER_STANDARD, f.value(), 1e-4);
    }

    @Test
    void breakingAStandardTakesItDown() {
        WarFury f = new WarFury();
        f.set(50);
        f.standardBroken();
        assertEquals(50 - WarFury.STANDARD_BROKEN, f.value(), 1e-4);
        f.set(5);
        f.standardBroken();
        assertEquals(0, f.value(), 1e-4, "never below zero");
    }

    @Test
    void withNoStandardsItCools() {
        WarFury f = new WarFury();
        f.set(10);
        f.tickSecond(0);
        assertTrue(f.value() < 10);
    }

    @Test
    void itIsCappedAndScalesDamageAndSpeed() {
        WarFury f = new WarFury();
        for (int i = 0; i < 100; i++) f.betrayal();
        assertEquals(WarFury.MAX, f.value(), 1e-4);
        assertEquals(1 + WarFury.MAX_DAMAGE_BONUS, f.damageMultiplier(), 1e-4);
        assertEquals(1 + WarFury.MAX_SPEED_BONUS, f.speedMultiplier(), 1e-4);
        f.set(0);
        assertEquals(1, f.damageMultiplier(), 1e-4);
    }
}
