package org.papiricoh.supernaturalcraft.apocalypse.blood;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodRulesTest {

    @Test
    void aDoseFillsTheBodyAndHooksEachSideAtItsOwnPace() {
        BloodData h = BloodRules.drink(BloodData.EMPTY, Faction.HUMAN);
        BloodData d = BloodRules.drink(BloodData.EMPTY, Faction.DEMON);
        BloodData a = BloodRules.drink(BloodData.EMPTY, Faction.ANGEL);
        assertEquals(BloodRules.DOSE_BLOOD, h.blood(), 1e-6);
        assertEquals(1, h.doses());
        assertTrue(d.dependency() < h.dependency() && h.dependency() < a.dependency(), "demons slowest, angels fastest");
        assertEquals(h.dependency() * 2, a.dependency(), 1e-6);
        BloodData full = BloodData.EMPTY;
        for (int i = 0; i < 20; i++) full = BloodRules.drink(full, Faction.ANGEL);
        assertEquals(1f, full.blood(), 1e-6, "the body holds no more than full");
        assertEquals(1f, full.dependency(), 1e-6);
    }

    @Test
    void thePowersOpenByLifetimeDosesAndOnlyWithBloodInTheBody() {
        assertEquals(0, BloodRules.tier(0));
        assertEquals(1, BloodRules.tier(1));
        assertEquals(1, BloodRules.tier(5));
        assertEquals(2, BloodRules.tier(6));
        assertEquals(3, BloodRules.tier(15));
        assertEquals(3, BloodRules.tier(99));
        assertEquals(0, BloodRules.activeTier(new BloodData(0f, 0.5f, 20, 0)), "a dry body has no powers");
        assertEquals(3, BloodRules.activeTier(new BloodData(0.1f, 0.5f, 20, 0)));
        // One power per tier, each costing no more than a body can hold.
        for (int t = 1; t <= 3; t++) assertEquals(t, Power.blood(t).size());
        for (Power p : Power.blood(3)) assertTrue(BloodRules.cost(p.cost) <= 1f, p.id());
        assertTrue(Power.blood(3).stream().allMatch(p -> p.blood && !p.passive));
    }

    @Test
    void theBloodFadesByTheMinuteAndTheNeedByTheDay() {
        assertEquals(0.3f, BloodRules.fade(0.35f, 1), 1e-6);
        assertEquals(0f, BloodRules.fade(0.35f, 60), 1e-6);
        assertEquals(0.48f, BloodRules.fadeDays(0.5f, 1), 1e-6);
        assertEquals(0.5f, BloodRules.fadeDays(0.5f, 0), 1e-6);
        assertTrue(BloodRules.fadeDays(1f, 10) > 0.5f, "the need outlasts a week: the Panic Room is the cure");
    }

    @Test
    void withdrawalComesWithADryBodyAndTheNeedOnIt() {
        assertFalse(BloodRules.withdrawal(new BloodData(0.2f, 0.9f, 3, 0)), "not while the blood holds");
        assertFalse(BloodRules.withdrawal(new BloodData(0f, BloodRules.WITHDRAWAL, 3, 0)));
        BloodData dry = new BloodData(0f, 0.8f, 3, 0);
        assertTrue(BloodRules.withdrawal(dry));
        assertTrue(BloodRules.severe(dry));
        assertEquals(1f, BloodRules.holyTaken(dry, Faction.HUMAN), 1e-6, "a hunter does not burn for it");
        assertEquals(1.4f, BloodRules.holyTaken(dry, Faction.DEMON), 1e-6);
        assertEquals(1.4f, BloodRules.holyTaken(dry, Faction.ANGEL), 1e-6);
        assertEquals(0f, BloodRules.essenceDrainPerSecond(dry, Faction.HUMAN), 1e-6);
        assertTrue(BloodRules.essenceDrainPerSecond(dry, Faction.DEMON) > 0);
        assertTrue(BloodRules.essenceDrainPerSecond(dry, Faction.ANGEL) > 0);
    }
}
