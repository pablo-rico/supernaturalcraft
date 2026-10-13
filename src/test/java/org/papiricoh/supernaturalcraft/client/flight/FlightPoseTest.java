package org.papiricoh.supernaturalcraft.client.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightPoseTest {

    private static FlightPose settled(double horizontal, double vertical, float yaw) {
        FlightPose p = new FlightPose();
        for (int i = 0; i < 80; i++) p.tick(true, horizontal, vertical, yaw);
        return p;
    }

    /** The pose averaged over one wingbeat (the bob and the sway cancel out). */
    private static float pitch(FlightPose p) {
        float sum = 0;
        for (int i = 0; i < FlightPose.BEAT_TICKS; i++) {
            p.tick(true, lastH, lastV, 0);
            sum += p.frame(0).bodyPitch();
        }
        return sum / FlightPose.BEAT_TICKS;
    }

    private static double lastH, lastV;

    private static float meanPitch(double h, double v) {
        lastH = h;
        lastV = v;
        return pitch(settled(h, v, 0));
    }

    @Test
    void onTheGroundThePlayerKeepsTheirOwnPose() {
        FlightPose p = new FlightPose();
        assertFalse(p.active());
        p.tick(false, 0.2, 0, 0);
        assertSame(FlightPose.Frame.REST, p.frame(1));
    }

    @Test
    void hoveringLeansALittleCruisingLiesAlongTheFlight() {
        float hover = meanPitch(0, 0), cruise = meanPitch(0.7, 0);
        assertEquals(FlightPose.HOVER_PITCH, hover, 1.5f);
        assertEquals(FlightPose.CRUISE_PITCH, cruise, 1.5f);
        assertTrue(meanPitch(0.7, -0.4) > cruise, "a dive is steeper");
        assertTrue(meanPitch(0.7, 0.4) < cruise, "a climb is more upright");
        assertTrue(meanPitch(0.7, -2) <= FlightPose.MAX_PITCH);
    }

    @Test
    void theHeadKeepsLookingAhead() {
        FlightPose.Frame f = settled(0.7, 0, 0).frame(0);
        assertTrue(f.headPitch() < -40, "the head turns back up: " + f.headPitch());
        assertTrue(-f.headPitch() <= FlightPose.NECK);
    }

    @Test
    void itBanksIntoTurnsOnlyAtSpeed() {
        assertTrue(settled(0.7, 0, 6).frame(0).bodyRoll() > 10, "a right turn banks right");
        assertTrue(settled(0.7, 0, -6).frame(0).bodyRoll() < -10, "a left turn banks left");
        assertEquals(0, settled(0, 0, 6).frame(0).bodyRoll(), 1e-3, "hovering does not bank");
        assertEquals(0, settled(0.7, 0, 359).frame(0).bodyRoll(), 5, "a step through north is a small turn");
    }

    @Test
    void landingEasesBackAndStops() {
        FlightPose p = settled(0.7, 0, 0);
        p.tick(false, 0, 0, 0);
        assertTrue(p.active(), "still easing out");
        float mid = p.frame(1).blend();
        assertTrue(mid > 0 && mid < 1);
        for (int i = 0; i < 60; i++) p.tick(false, 0, 0, 0);
        assertFalse(p.active(), "settled back");
    }

    @Test
    void theLegsDangleOutOfStepWhileHovering() {
        FlightPose p = settled(0, 0, 0);
        boolean apart = false;
        for (int i = 0; i < 40; i++) {
            p.tick(true, 0, 0, 0);
            FlightPose.Frame f = p.frame(0);
            if (Math.abs(f.rightLegBack() - f.leftLegBack()) > 4) apart = true;
            assertTrue(f.rightLegBack() > 0 && f.leftLegBack() > 0, "they hang back, never kick forward");
        }
        assertTrue(apart);
    }
}
