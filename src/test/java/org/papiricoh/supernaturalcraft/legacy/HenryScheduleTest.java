package org.papiricoh.supernaturalcraft.legacy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Henry's calls ({@link LegacySchedule}) and his talk ({@link HenryDialogue}). */
class HenryScheduleTest {

    private static final long DAY = 24000;

    @Test
    void heComesTheDawnAfterLucifer() {
        Legacy none = Legacy.NONE;
        assertFalse(LegacySchedule.visits(none, 5 * DAY), "not before Lucifer falls");
        Legacy due = LegacySchedule.luciferBeaten(none, 5 * DAY + 6000);
        assertFalse(LegacySchedule.visits(due, 5 * DAY + 7000), "not the same day");
        assertFalse(LegacySchedule.visits(due, 6 * DAY + 6000), "not at noon");
        assertTrue(LegacySchedule.visits(due, 6 * DAY - 500), "at the next dawn");
        assertTrue(LegacySchedule.visits(due, 9 * DAY + 100), "or any dawn after");
        assertEquals(due, LegacySchedule.luciferBeaten(due, 20 * DAY), "beating him again changes nothing");
    }

    @Test
    void turnedAwayHeComesBackLaterAndNeverAfterJoining() {
        Legacy due = LegacySchedule.luciferBeaten(Legacy.NONE, 0);
        Legacy no = LegacySchedule.declined(due, DAY + 200);
        assertEquals(Legacy.HENRY_DECLINED, no.henry());
        assertFalse(LegacySchedule.visits(no, 2 * DAY + 200));
        assertTrue(LegacySchedule.visits(no, (1 + LegacyRules.HENRY_RETURN_DAYS) * DAY + 200));
        Legacy joined = no.withRank(1).withHenry(Legacy.HENRY_JOINED, -1);
        assertFalse(LegacySchedule.visits(joined, 50 * DAY));
        assertEquals(joined, LegacySchedule.declined(joined, 0));
    }

    @Test
    void theTalkGoesWhereTheAnswersLead() {
        assertEquals(HenryDialogue.Stage.OFFER, HenryDialogue.start(new HenryDialogue.Context(false, false, false, -1)));
        assertEquals(HenryDialogue.Stage.OFFER_AGAIN, HenryDialogue.start(new HenryDialogue.Context(false, true, false, -1)));
        assertEquals(HenryDialogue.Stage.WAR_ROOM, HenryDialogue.start(new HenryDialogue.Context(true, false, false, -1)));
        assertEquals(HenryDialogue.Stage.CASE_OPEN, HenryDialogue.start(new HenryDialogue.Context(true, false, true, 1)));
        assertEquals(HenryDialogue.Stage.CASE_SOLVED, HenryDialogue.start(new HenryDialogue.Context(true, false, false, 2)));
        assertEquals(HenryDialogue.Stage.CASE_LOST, HenryDialogue.start(new HenryDialogue.Context(true, false, false, 3)));
        assertEquals(HenryDialogue.Stage.WELCOME, HenryDialogue.next(HenryDialogue.Stage.OFFER, HenryDialogue.ACCEPT));
        assertEquals(HenryDialogue.Stage.FAREWELL, HenryDialogue.next(HenryDialogue.Stage.OFFER_AGAIN, HenryDialogue.DECLINE));
        assertEquals(HenryDialogue.Stage.CASE_GIVEN, HenryDialogue.next(HenryDialogue.Stage.WAR_ROOM, HenryDialogue.NEW_CASE));
        assertNull(HenryDialogue.next(HenryDialogue.Stage.WELCOME, HenryDialogue.CLOSE));
        assertEquals(HenryDialogue.Stage.CASE_OPEN, HenryDialogue.next(HenryDialogue.Stage.CASE_OPEN, HenryDialogue.NEW_CASE), "refused");
        assertEquals(HenryDialogue.Stage.OFFER, HenryDialogue.next(HenryDialogue.Stage.OFFER, HenryDialogue.NEW_CASE), "refused");
    }

    @Test
    void everyStageHasLinesAndAWayOut() {
        for (HenryDialogue.Stage s : HenryDialogue.Stage.values()) {
            assertTrue(s.lines >= 1 && !s.choices().isEmpty(), s + "");
            assertEquals(s, HenryDialogue.Stage.of(s.ordinal()));
            assertEquals("legacy.supernaturalcraft.henry." + s.id() + ".1", HenryDialogue.line(s, 1));
        }
        assertNull(HenryDialogue.Stage.of(HenryDialogue.CLOSED));
        assertEquals(List.of(HenryDialogue.ACCEPT, HenryDialogue.DECLINE), HenryDialogue.Stage.OFFER.choices());
        assertEquals("legacy.supernaturalcraft.henry.choice.new_case", HenryDialogue.choiceKey(HenryDialogue.NEW_CASE));
    }

    @Test
    void everyLineIsWritten() {
        java.util.Map<String, String> lang = new java.util.HashMap<>();
        org.papiricoh.supernaturalcraft.datagen.legacy.LegacyServerLang.add(lang::put);
        for (HenryDialogue.Stage s : HenryDialogue.Stage.values()) {
            for (int n = 1; n <= s.lines; n++) assertTrue(lang.containsKey(HenryDialogue.line(s, n)), "missing " + HenryDialogue.line(s, n));
            assertFalse(lang.containsKey(HenryDialogue.line(s, s.lines + 1)), "an unread line in " + s);
            for (byte c : s.choices()) assertTrue(lang.containsKey(HenryDialogue.choiceKey(c)), "missing " + HenryDialogue.choiceKey(c));
        }
        for (String sc : org.papiricoh.supernaturalcraft.legacy.cases.CaseGenerator.SCENARIOS) {
            assertTrue(lang.containsKey("legacy.supernaturalcraft.case.scenario." + sc), sc);
        }
        for (String t : org.papiricoh.supernaturalcraft.legacy.cases.CaseGenerator.TWISTS) {
            assertTrue(lang.containsKey("legacy.supernaturalcraft.case.twist." + (t.isEmpty() ? "none" : t)), t);
        }
    }
}
