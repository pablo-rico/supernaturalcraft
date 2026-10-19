package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchAssets;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchBalance;
import org.papiricoh.supernaturalcraft.entity.witch.CovenWitchEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Grand Witch's numbers (v0.21 P6, pure): three gapless thirds; her coven's shield only in phase I and only while one still
 * channels; an interruption shorter than a coven witch's own shield takes to rise; windups long enough for their clips; the mass
 * taglock's follow-and-hold and Rowena's green circles; the Coven Staff.
 */
class GrandWitchBalanceTest {

    @Test
    void threePhasesOfAThirdEachWithNoGap() {
        assertEquals(3, GrandWitchBalance.PHASES);
        for (int p = 1; p <= GrandWitchBalance.PHASES; p++) {
            assertEquals(1f / 3f, GrandWitchBalance.threshold(p - 1) - GrandWitchBalance.threshold(p), 1e-6, "phase " + p + " is a third");
        }
        assertEquals(0f, GrandWitchBalance.threshold(3), 1e-6);
        assertTrue(GrandWitchBalance.attackGap(3) < GrandWitchBalance.attackGap(2) && GrandWitchBalance.attackGap(2) < GrandWitchBalance.attackGap(1),
                "her coven does the work at first; she quickens after");
    }

    @Test
    void theShieldIsTheCovensAndOnlyInPhaseOne() {
        assertEquals(3, GrandWitchBalance.CHANNELERS);
        for (int phase = 1; phase <= 3; phase++) {
            for (int channeling = 0; channeling <= GrandWitchBalance.CHANNELERS; channeling++) {
                assertEquals(phase == 1 && channeling > 0, GrandWitchBalance.shielded(phase, channeling), "phase " + phase + ", " + channeling + " channelling");
            }
        }
        assertFalse(GrandWitchBalance.shielded(1, 0), "the last channeler stopped: the shield falls");
        assertEquals(240, GrandWitchBalance.INTERRUPT_TICKS, "a broken channeler stops for twelve seconds");
        assertTrue(GrandWitchBalance.INTERRUPT_TICKS < CovenWitchEntity.SHIELD_REGEN, "she channels again before her own shield is back");
        assertTrue(GrandWitchBalance.CHANNEL_DISTANCE > GrandWitchBalance.INFERNO_RADIUS, "her channelers stand out of her inferno");
    }

    @Test
    void windupsEndOnTheClipsMoments() {
        Map<String, Integer> windups = Map.of("frost_hex", GrandWitchBalance.FROST_WINDUP, "inferno", GrandWitchBalance.INFERNO_WINDUP,
                "crush", GrandWitchBalance.CRUSH_WINDUP, "mass_taglock", GrandWitchBalance.MASS_FOLLOW + GrandWitchBalance.MASS_HOLD,
                "smoke_veil", GrandWitchBalance.VEIL_WINDUP);
        for (var e : windups.entrySet()) {
            int cue = GrandWitchBalance.cue(e.getKey());
            assertEquals(GrandWitchAssets.HIT_TICKS.get(e.getKey()).intValue(), cue, e.getKey());
            assertTrue(e.getValue() >= cue, e.getKey() + ": the clip must start inside the windup");
        }
        assertTrue(GrandWitchBalance.DEATH_TICKS >= GrandWitchAssets.HIT_TICKS.get("death"));
    }

    @Test
    void theMassTaglockAndRowenasCircles() {
        assertEquals(40, GrandWitchBalance.MASS_FOLLOW);
        assertTrue(GrandWitchBalance.MASS_HOLD >= 10, "the circle stops long enough to step into a green one");
        assertEquals(2, GrandWitchBalance.SAFE_CIRCLES);
        assertTrue(GrandWitchBalance.spared(0) && GrandWitchBalance.spared(GrandWitchBalance.SAFE_RADIUS), "inside a green circle");
        assertFalse(GrandWitchBalance.spared(GrandWitchBalance.SAFE_RADIUS + 0.01), "just outside it");
        assertFalse(GrandWitchBalance.spared(Double.MAX_VALUE), "no circle at all");
        assertTrue(GrandWitchBalance.SAFE_RADIUS > GrandWitchBalance.MASS_RADIUS, "a green circle is wider than the red one");
        assertTrue(GrandWitchBalance.SAFE_REACH > GrandWitchBalance.SAFE_RADIUS + 1, "a circle is drawn near, not under, the hunter");
        assertTrue(GrandWitchBalance.SAFE_REACH * 20 / 4.3 < GrandWitchBalance.MASS_FOLLOW + GrandWitchBalance.MASS_HOLD + 40,
                "the farthest circle is within a sprint's reach before the curse lands");
    }

    @Test
    void theCovenStaff() {
        assertEquals(0.8f, GrandWitchBalance.STAFF_WITCHCRAFT_MANA, 1e-6);
        assertEquals(1.25f, GrandWitchBalance.STAFF_WITCHCRAFT_POTENCY, 1e-6);
        assertTrue(GrandWitchBalance.STAFF_DAMAGE > 0 && GrandWitchBalance.STAFF_RANGE >= 16);
        assertTrue(GrandWitchBalance.STAFF_COOLDOWN > 0 && GrandWitchBalance.STAFF_CURSE_TICKS > 0);
        assertEquals(100, GrandWitchBalance.ALLY_BOLT_EVERY, "Rowena throws a bolt every five seconds");
    }
}
