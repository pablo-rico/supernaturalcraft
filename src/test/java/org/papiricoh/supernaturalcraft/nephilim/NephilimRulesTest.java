package org.papiricoh.supernaturalcraft.nephilim;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules.Step;
import org.papiricoh.supernaturalcraft.nephilim.jack.JackRules;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NephilimRulesTest {

    @Test
    void stepsAreSavedByOrdinalAndOnlyAppended() {
        // A world's saved steps are ordinals: the order of the first ones may never change.
        List<String> fixed = List.of("NONE", "ULTIMATUM", "TRACING", "BORN", "IN_AU", "MICHAEL_FLED", "OFFERED", "VESSEL_DOWN", "SOULLESS",
                "JACK_FELL", "DONE");
        for (int i = 0; i < fixed.size(); i++) assertEquals(fixed.get(i), Step.values()[i].name());
        assertEquals(Step.NONE, Step.of(-1));
        assertEquals(Step.NONE, Step.of(999));
        assertEquals(Step.BORN, Step.of(Step.BORN.ordinal()));
    }

    @Test
    void jackWalksFromHisBirthUntilHeTradesHisSoul() {
        for (Step s : Step.values()) {
            boolean walks = s.ordinal() >= Step.BORN.ordinal() && s.ordinal() < Step.SOULLESS.ordinal();
            assertEquals(walks, s.jackWalks(), s.name());
        }
    }

    @Test
    void jackGrowsUntilHeSpendsHisGraceOnMichael() {
        assertEquals(0f, JackRules.strike(Step.TRACING));
        assertTrue(JackRules.strike(Step.IN_AU) > JackRules.strike(Step.BORN), "the Apocalypse World makes him stronger");
        assertTrue(JackRules.graced(Step.IN_AU));
        assertFalse(JackRules.graced(Step.MICHAEL_FLED), "his grace is spent driving Michael back");
        assertTrue(JackRules.strike(Step.OFFERED) < JackRules.strike(Step.BORN), "graceless, he is only a boy");
        assertFalse(JackRules.shields(Step.VESSEL_DOWN));
        assertEquals(0f, JackRules.strike(Step.SOULLESS), "once soulless he no longer walks with the hunter");
        assertEquals("born", JackRules.mood(Step.BORN));
        assertEquals("au", JackRules.mood(Step.IN_AU));
        assertEquals("graceless", JackRules.mood(Step.OFFERED));
    }

    @Test
    void bloodOnceADayAndVisitsAfterARest() {
        assertTrue(JackRules.bloodDue(-100, 0));
        assertFalse(JackRules.bloodDue(5, 5));
        assertTrue(JackRules.bloodDue(5, 6));
        assertFalse(NephilimRules.visitDue(10, 10));
        assertTrue(NephilimRules.visitDue(10, 10 + NephilimRules.VISIT_REST_DAYS));
    }

    @Test
    void theNewRoadSitsBetweenAmaraAndTheAuthorWithItsSideRoadsOptional() {
        List<BossProgression.Boss> order = List.of(BossProgression.Boss.values());
        int amara = order.indexOf(BossProgression.Boss.AMARA), chuck = order.indexOf(BossProgression.Boss.CHUCK);
        for (BossProgression.Boss b : List.of(BossProgression.Boss.KETCH, BossProgression.Boss.HESS, BossProgression.Boss.DAGON,
                BossProgression.Boss.ASMODEUS, BossProgression.Boss.AU_MICHAEL, BossProgression.Boss.MICHAEL_VESSEL,
                BossProgression.Boss.SOULLESS_JACK, BossProgression.Boss.SHADOW)) {
            int i = order.indexOf(b);
            assertTrue(i > amara && i < chuck, b + " is between Amara and the Author");
        }
        assertTrue(BossProgression.Boss.KETCH.optional && BossProgression.Boss.HESS.optional && BossProgression.Boss.ASMODEUS.optional);
        assertFalse(BossProgression.Boss.DAGON.optional || BossProgression.Boss.AU_MICHAEL.optional || BossProgression.Boss.MICHAEL_VESSEL.optional
                || BossProgression.Boss.SOULLESS_JACK.optional || BossProgression.Boss.SHADOW.optional);
        // The Author waits for the new main road, not for its side roads.
        assertFalse(BossProgression.allBeforeChuck(a -> !a.equals(BossProgression.Boss.SHADOW.advancement)));
        assertTrue(BossProgression.allBeforeChuck(a -> !a.equals(BossProgression.Boss.KETCH.advancement)));
    }

    @Test
    void migrationGrantsTheMainRoadInOrderWithoutItsSideRoads() {
        List<String> road = NephilimMigration.MAIN_ROAD;
        assertTrue(road.contains(BossProgression.Boss.DAGON.advancement));
        assertTrue(road.contains(BossProgression.Boss.SHADOW.advancement));
        assertFalse(road.contains(BossProgression.Boss.KETCH.advancement));
        assertFalse(road.contains(BossProgression.Boss.ASMODEUS.advancement));
        assertTrue(road.indexOf(BossProgression.Boss.DAGON.advancement) < road.indexOf(BossProgression.Boss.SHADOW.advancement));
    }
}
