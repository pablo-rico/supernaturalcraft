package org.papiricoh.supernaturalcraft.british;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.british.BritishRules.Choice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BritishRulesTest {

    @Test
    void choicesAreSavedByOrdinalAndOnlyAppended() {
        assertEquals(Choice.NONE, Choice.of(0));
        assertEquals(Choice.JOINED, Choice.of(1));
        assertEquals(Choice.REFUSED, Choice.of(2));
        assertEquals(Choice.BETRAYED, Choice.of(3));
        assertEquals(Choice.NONE, Choice.of(-1));
        assertEquals(Choice.NONE, Choice.of(99));
    }

    @Test
    void aHunterWhoJoinedIsWrittenOffAfterTheirDays() {
        assertFalse(BritishRules.betrayalDue(Choice.JOINED, 10, 10 + BritishRules.JOIN_DAYS - 1));
        assertTrue(BritishRules.betrayalDue(Choice.JOINED, 10, 10 + BritishRules.JOIN_DAYS));
        assertFalse(BritishRules.betrayalDue(Choice.REFUSED, 10, 100), "only those who joined are betrayed");
        assertFalse(BritishRules.betrayalDue(Choice.BETRAYED, 10, 100), "and only once");
    }

    @Test
    void ketchHuntsTheWrittenOffUntilHeIsBeaten() {
        assertTrue(BritishRules.hunted(Choice.REFUSED, false));
        assertTrue(BritishRules.hunted(Choice.BETRAYED, false));
        assertFalse(BritishRules.hunted(Choice.JOINED, false), "not while they are London's");
        assertFalse(BritishRules.hunted(Choice.NONE, false));
        assertFalse(BritishRules.hunted(Choice.REFUSED, true), "beaten, he hunts no more");
        assertFalse(BritishRules.huntDue(5, 5 + BritishRules.HUNT_REST_DAYS - 1));
        assertTrue(BritishRules.huntDue(5, 5 + BritishRules.HUNT_REST_DAYS));
    }

    @Test
    void ketchHuntsByNightAndHessComesOnlyAfterAmara() {
        assertTrue(BritishRules.night(18_000));
        assertFalse(BritishRules.night(6_000));
        assertTrue(BritishRules.night(24_000L * 7 + 18_000), "every night");
        assertTrue(BritishRules.ultimatumDue(true, Choice.NONE));
        assertFalse(BritishRules.ultimatumDue(false, Choice.NONE), "not before Amara");
        assertFalse(BritishRules.ultimatumDue(true, Choice.REFUSED), "once");
    }
}
