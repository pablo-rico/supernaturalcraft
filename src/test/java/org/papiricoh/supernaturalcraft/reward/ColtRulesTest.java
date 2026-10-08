package org.papiricoh.supernaturalcraft.reward;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.client.colt.ColtCylinder;
import org.papiricoh.supernaturalcraft.client.colt.RecoilSpring;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.reward.colt.ColtReload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Colt's pure rules: exact boss damage, the reload's clock, the cylinder, the recoil spring. */
class ColtRulesTest {

    @Test
    void exactDamageSkipsMultiplierAndCapButNeverTheFloor() {
        assertEquals(60, BossDamage.scaleAndCap(true, 60, 0.5f, 40), 1e-6);
        assertEquals(15, BossDamage.scaleAndCap(false, 60, 0.25f, 40), 1e-6);
        assertEquals(40, BossDamage.scaleAndCap(false, 60, 1.25f, 40), 1e-6);
        assertEquals(30, BossDamage.clampToFloor(780, 60, 750), 1e-6);
        assertEquals(0, BossDamage.clampToFloor(750, 60, 750), 1e-6);
        assertEquals(60, BossDamage.clampToFloor(1000, 60, 750), 1e-6);
    }

    @Test
    void aRoundTakesFivePercentOfAGreatEnemyButOnlyTheHardCapOfTheAuthor() {
        float hard = org.papiricoh.supernaturalcraft.balance.ProgressionScale.DEFAULT_HARD_CAP;
        assertEquals(250, BossDamage.coltCap(5_000, false, BossDamage.DEFAULT_COLT_SHARE, hard), 1e-3, "Azazel");
        assertEquals(3250, BossDamage.coltCap(65_000, false, BossDamage.DEFAULT_COLT_SHARE, hard), 1e-2, "Michael");
        assertEquals(1500, BossDamage.coltCap(100_000, true, BossDamage.DEFAULT_COLT_SHARE, hard), 1e-2, "the Author: his hard cap");
        assertTrue(BossDamage.coltCap(45_000, false, BossDamage.DEFAULT_COLT_SHARE, hard) > 45_000 * hard, "the Colt alone passes the hard cap");
    }

    @Test
    void roundsAreSeatedOnTheirTicks() {
        assertEquals(ColtReload.INTRO + ColtReload.SEAT, ColtReload.insertTick(0));
        assertEquals(ColtReload.INTRO + ColtReload.PER * 5 + ColtReload.OUTRO, ColtReload.total(5));
        assertEquals(0, ColtReload.seatedBy(100, 3, 100 + ColtReload.insertTick(0) - 1));
        assertEquals(1, ColtReload.seatedBy(100, 3, 100 + ColtReload.insertTick(0)));
        assertEquals(3, ColtReload.seatedBy(100, 3, 100 + 10_000));
        assertTrue(ColtReload.insertTick(4) < ColtReload.total(5), "the last round goes in before the gun closes");
    }

    @Test
    void loadedRoundsSitInTheChambersComingUp() {
        // Chamber 3 under the hammer, two rounds: chambers 3 and 4 are loaded.
        assertTrue(ColtReload.roundVisible(3, 3, 2, 5));
        assertTrue(ColtReload.roundVisible(4, 3, 2, 5));
        assertFalse(ColtReload.roundVisible(0, 3, 2, 5));
        assertTrue(ColtReload.roundVisible(0, 4, 2, 5), "wraps past the last chamber");
        for (int j = 0; j < 5; j++) assertFalse(ColtReload.roundVisible(j, 2, 0, 5));
    }

    @Test
    void theCylinderOnlyTurnsForward() {
        long gun = 424242L;
        assertEquals(4 * ColtCylinder.STEP, ColtCylinder.angle(gun, 4, 0), 1e-3);
        ColtCylinder.angle(gun, 0, 1);
        float later = ColtCylinder.angle(gun, 0, 1 + ColtCylinder.TURN_TICKS);
        assertEquals(5 * ColtCylinder.STEP, later, 1e-3, "chamber 4 to 0 is one more step, not four back");
    }

    @Test
    void theSpringKicksOvershootsAndSettles() {
        RecoilSpring s = new RecoilSpring(260, 0.5);
        s.kick(-7);
        double peak = 0;
        for (int i = 0; i < 120; i++) {
            s.step(1 / 60.0);
            peak = Math.min(peak, s.value());
            assertFalse(Double.isNaN(s.value()));
        }
        assertEquals(-7, peak, 0.6, "the kick should peak where it was aimed");
        assertTrue(Math.abs(s.value()) < 0.05, "it should have settled after two seconds: " + s.value());
        s.kick(3);
        s.step(10);
        assertFalse(Double.isNaN(s.value()), "a huge frame must not blow it up");
    }
}
