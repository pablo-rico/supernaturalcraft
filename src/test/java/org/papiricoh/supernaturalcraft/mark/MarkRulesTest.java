package org.papiricoh.supernaturalcraft.mark;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.allegiance.EssenceRules;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseLevels;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Mark of Cain's numbers (v0.21 P4, pure): five minutes of rest after a kill, then +0.01 a minute; a kill slakes a hundredth
 * per soul (0.02-0.3); healing cut by half the Thirst; bloodlust (x1.2 melee) from half; at full it feeds and its bearer falls on
 * death unless already a demon; the honey's quarter; the First Blade x3; a Mark taken again after it was taken off starts at
 * half; the Knights of old are owed it; and the one meter: a demon starves only from the Mark's bloodlust.
 */
class MarkRulesTest {

    private static final float EPS = 1e-5f;

    @Test
    void theThirstRestsForFiveMinutesAfterAKillThenClimbsAHundredthAMinute() {
        assertEquals(6000, MarkRules.GRACE_TICKS, "five minutes of rest");
        assertEquals(0.3f, MarkRules.rise(0.3f, 0, 1), EPS);
        assertEquals(0.3f, MarkRules.rise(0.3f, MarkRules.GRACE_TICKS, 60), EPS, "the last tick of the grace is still rest");
        // One second past the grace: a sixtieth of a hundredth.
        assertEquals(0.3f + 0.01f / 60, MarkRules.rise(0.3f, MarkRules.GRACE_TICKS + 20, 1), EPS);
        // A whole minute past it, in one step of sixty seconds: a hundredth.
        assertEquals(0.31f, MarkRules.rise(0.3f, MarkRules.GRACE_TICKS + 1200, 60), EPS);
        // Only the time past the grace counts, even in a long step that straddles it.
        assertEquals(0.3f + 0.01f / 6, MarkRules.rise(0.3f, MarkRules.GRACE_TICKS + 200, 600), EPS);
        // Second by second, an hour past the grace takes it from nothing to 0.6.
        float t = 0;
        for (int s = 1; s <= 3600; s++) t = MarkRules.rise(t, MarkRules.GRACE_TICKS + s * 20L, 1);
        assertEquals(0.6f, t, 1e-3f);
        assertEquals(1f, MarkRules.rise(0.999f, MarkRules.GRACE_TICKS * 10L, 600), EPS, "never past full");
        assertEquals(0.4f, MarkRules.rise(0.4f, MarkRules.GRACE_TICKS * 2L, 0), EPS, "no time, no thirst");
    }

    @Test
    void aKillSlakesAHundredthPerSoulWithinItsBounds() {
        // A zombie (20 health) is five souls: 0.05.
        assertEquals(CurseLevels.soulsFor(20) * 0.01f, MarkRules.relief(20), EPS);
        assertEquals(0.05f, MarkRules.relief(20), EPS);
        assertEquals(MarkRules.MIN_RELIEF, MarkRules.relief(1), EPS, "a chicken still counts for something");
        assertEquals(MarkRules.MAX_RELIEF, MarkRules.relief(1000), EPS, "a boss slakes at most 0.3");
        assertEquals(0.02f, MarkRules.MIN_RELIEF, EPS);
        assertEquals(0.3f, MarkRules.MAX_RELIEF, EPS);
        assertEquals(0.75f, MarkRules.afterKill(0.8f, 20), EPS);
        assertEquals(0f, MarkRules.afterKill(0.01f, 1000), EPS, "never below nothing");
        for (float hp = 1; hp < 2000; hp *= 1.7f) {
            float r = MarkRules.relief(hp);
            assertTrue(r >= MarkRules.MIN_RELIEF - EPS && r <= MarkRules.MAX_RELIEF + EPS, hp + " health slakes " + r);
        }
    }

    @Test
    void healingIsCutByHalfTheThirst() {
        assertEquals(1f, MarkRules.healFactor(0), EPS);
        assertEquals(0.75f, MarkRules.healFactor(0.5f), EPS);
        assertEquals(0.7f, MarkRules.healFactor(0.6f), EPS);
        assertEquals(0.5f, MarkRules.healFactor(1f), EPS);
        assertEquals(0.5f, MarkRules.healFactor(7f), EPS, "a thirst out of range is clamped");
        assertEquals(1f, MarkRules.healFactor(Float.NaN), EPS);
    }

    @Test
    void bloodlustFromHalfFullAtOneAndTheBlowsLandHarder() {
        assertFalse(MarkRules.bloodlust(0.49f));
        assertTrue(MarkRules.bloodlust(0.5f));
        assertEquals(1f, MarkRules.meleeFactor(0.49f), EPS);
        assertEquals(1.2f, MarkRules.meleeFactor(0.5f), EPS);
        assertEquals(1.2f, MarkRules.meleeFactor(1f), EPS);
        assertFalse(MarkRules.full(0.99f));
        assertTrue(MarkRules.full(1f));
        assertTrue(MarkRules.bloodlustStarts(0.49f, 0.5f));
        assertFalse(MarkRules.bloodlustStarts(0.5f, 0.6f), "it starts once");
        assertFalse(MarkRules.bloodlustStarts(0.6f, 0.4f));
        assertTrue(MarkRules.pulses(0.19f, 0.2f), "it throbs past each tenth");
        assertFalse(MarkRules.pulses(0.2f, 0.25f));
        assertFalse(MarkRules.pulses(0.3f, 0.1f), "never as it falls");
        assertEquals(1f, MarkRules.FEED_HURT, EPS);
        assertEquals(10, MarkRules.FEED_INTERVAL);
    }

    @Test
    void atFullThirstHumansAndAngelsFallButADemonHasNothingLeftToLose() {
        assertTrue(MarkRules.fallsOnDeath(Faction.HUMAN, 1f));
        assertTrue(MarkRules.fallsOnDeath(Faction.ANGEL, 1f));
        assertFalse(MarkRules.fallsOnDeath(Faction.DEMON, 1f));
        assertFalse(MarkRules.fallsOnDeath(Faction.HUMAN, 0.99f), "only at full");
        assertEquals(0.5f, MarkRules.AFTER_FALL, EPS);
    }

    @Test
    void theHoneyTheBladeAndAMarkTakenAgain() {
        assertEquals(0.25f, MarkRules.HONEY_RELIEF, EPS);
        assertEquals(3f, MarkRules.FIRST_BLADE, EPS);
        assertEquals(0f, MarkRules.retakenThirst(false), EPS, "the first time it starts quiet");
        assertEquals(0.5f, MarkRules.retakenThirst(true), EPS, "it remembers the way");
        MarkData fresh = MarkData.regranted(MarkData.NONE, 100, Optional.empty());
        assertTrue(fresh.marked() && fresh.thirst() == 0f && fresh.lastKill() == 100 && !fresh.everRemoved());
        MarkData off = fresh.withThirst(0.9f).removed();
        assertFalse(off.marked());
        assertEquals(0f, off.thirst(), EPS, "taken off, it thirsts no more");
        assertTrue(off.everRemoved(), "but it is remembered");
        UUID cain = UUID.randomUUID();
        MarkData again = MarkData.regranted(off, 500, Optional.of(cain));
        assertTrue(again.marked() && again.everRemoved() && again.grantedBy().equals(Optional.of(cain)));
        assertEquals(MarkRules.RETAKEN_THIRST, again.thirst(), EPS, "taken again, it comes back at half");
        assertEquals(1f, new MarkData(true, 4f, 0, null).thirst(), EPS, "the record clamps");
        assertTrue(new MarkData(true, 0, 0, null).grantedBy().isEmpty());
    }

    @Test
    void theKnightsOfOldBearTheMarkAndItIsTheOneMeter() {
        assertTrue(MarkRules.owedTheMark(Faction.DEMON, 3, false));
        assertTrue(MarkRules.owedTheMark(Faction.DEMON, 4, false), "a King too");
        assertFalse(MarkRules.owedTheMark(Faction.DEMON, 3, true), "once");
        assertFalse(MarkRules.owedTheMark(Faction.DEMON, 2, false), "a Prince is not");
        assertFalse(MarkRules.owedTheMark(Faction.ANGEL, 4, false));
        assertFalse(MarkRules.owedTheMark(Faction.HUMAN, 3, false));
        // A demon's Corruption starves only from the Mark's bloodlust.
        assertFalse(EssenceRules.starving(false, 1f), "no Mark, no bloodlust");
        assertFalse(EssenceRules.starving(true, 0.49f));
        assertTrue(EssenceRules.starving(true, 0.5f));
    }
}
