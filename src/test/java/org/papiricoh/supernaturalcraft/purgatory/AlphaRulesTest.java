package org.papiricoh.supernaturalcraft.purgatory;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.entity.alpha.AlphaRules;
import org.papiricoh.supernaturalcraft.pact.PactRules;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Alphas' rules (v0.39): rare night visits, one at a time, a small pack, and a real fight at Death's tier. */
class AlphaRulesTest {

    @Test
    void theyComeRarelyAtNightAndOneAtATime() {
        assertTrue(AlphaRules.CHANCE > 0 && AlphaRules.CHANCE < 0.05f, "rare per check: " + AlphaRules.CHANCE);
        // Over a night (10 minutes of checks) one is likelier to come than not, but not certain.
        int checks = 10 * 60 * 20 / AlphaRules.CHECK_TICKS;
        double come = 1 - Math.pow(1 - AlphaRules.CHANCE, checks);
        assertTrue(come > 0.3 && come < 0.9, "chance an Alpha comes in a night: " + come);
        assertTrue(AlphaRules.comes(0, true, 0));
        assertFalse(AlphaRules.comes(0, false, 0), "never by day");
        assertFalse(AlphaRules.comes(0, true, AlphaRules.MAX_ALIVE), "one at a time");
        assertFalse(AlphaRules.comes(AlphaRules.CHANCE, true, 0));
        assertEquals(1, AlphaRules.MAX_ALIVE);
    }

    @Test
    void onlyAnAlphaStillHuntingComes() {
        assertNull(AlphaRules.pick(List.of(), 0.5));
        assertEquals(PactRules.Alpha.WEREWOLF, AlphaRules.pick(List.of(PactRules.Alpha.WEREWOLF), 0.99));
        List<PactRules.Alpha> two = List.of(PactRules.Alpha.VAMPIRE, PactRules.Alpha.SHAPESHIFTER);
        assertEquals(PactRules.Alpha.VAMPIRE, AlphaRules.pick(two, 0.1));
        assertEquals(PactRules.Alpha.SHAPESHIFTER, AlphaRules.pick(two, 0.99));
        assertEquals(PactRules.Alpha.SHAPESHIFTER, AlphaRules.pick(two, 1.0));
    }

    @Test
    void eachComesWithASmallPack() {
        for (PactRules.Alpha a : PactRules.Alpha.values()) {
            for (double r = 0; r < 1; r += 0.05) {
                int n = AlphaRules.pack(a, r);
                assertTrue(n >= 1 && n <= 3, a + " pack " + n);
            }
        }
    }

    @Test
    void aHunterAtDeathsTierNeedsARealFight() {
        assertEquals(ProgressionScale.of(org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss.DEATH).tier(), AlphaRules.tier());
        float blade = ProgressionScale.bladeHit(AlphaRules.tier());
        for (PactRules.Alpha a : PactRules.Alpha.values()) {
            float hp = AlphaRules.health(a);
            assertTrue(hp <= 1000f, a + " stays under the attribute's cap: " + hp);
            float swings = hp / (blade * AlphaRules.damageScale(a));
            assertEquals(AlphaRules.hits(a), swings, 0.01f, a + " takes its blows of the ascended Angel Blade");
            assertTrue(swings >= 20 && swings <= 40, a + " takes " + swings + " blows of the ascended Angel Blade");
            // Far tougher than its children (the legacy vampire 40, werewolf 50, shapeshifter 36) at the same blade.
            assertTrue(hp / AlphaRules.damageScale(a) > 40 * 10, a + ": " + hp);
            assertTrue(AlphaRules.melee(a) >= 10 && AlphaRules.melee(a) <= 16, a + " hits hard but not like a great enemy");
        }
        assertTrue(AlphaRules.SLAM_DAMAGE <= 16 && AlphaRules.LUNGE_DAMAGE <= 16 && AlphaRules.LEAP_DAMAGE <= 16);
    }

    @Test
    void theyStepOutOfTheDarkALittleWayOff() {
        assertTrue(AlphaRules.MIN_DISTANCE >= 16 && AlphaRules.MAX_DISTANCE > AlphaRules.MIN_DISTANCE && AlphaRules.MAX_DISTANCE <= 64);
        assertTrue(AlphaRules.SLAIN_REACH >= 16 && AlphaRules.BAR_REACH >= AlphaRules.SLAIN_REACH);
    }
}
