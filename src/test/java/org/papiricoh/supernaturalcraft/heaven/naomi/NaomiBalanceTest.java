package org.papiricoh.supernaturalcraft.heaven.naomi;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.NaomiAnimations;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.NaomiBalance;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.arena.ReprogrammingRoomLayout;
import org.papiricoh.supernaturalcraft.heaven.HeavenAssets;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Naomi's numbers (v0.18): halves, the strap's lane and the most isolated hunter, the lance, the wipe's gaps, guards, tests, console. */
class NaomiBalanceTest {

    @Test
    void twoPhasesOfAHalfEach() {
        assertEquals(0.5f, NaomiBalance.threshold(1), 1e-6);
        assertEquals(0f, NaomiBalance.threshold(2), 1e-6);
        assertTrue(NaomiBalance.attackGap(2) < NaomiBalance.attackGap(1), "the red lights hurry her");
        assertTrue(NaomiBalance.ARENA_RADIUS > ReprogrammingRoomLayout.RADIUS, "the arena holds her whole room");
    }

    @Test
    void theStrapFindsTheMostIsolatedHunter() {
        assertEquals(-1, NaomiBalance.mostIsolated(new double[0][]));
        assertEquals(0, NaomiBalance.mostIsolated(new double[][]{{3, 4}}), "a lone hunter");
        // Two together, one alone at the far end.
        assertEquals(2, NaomiBalance.mostIsolated(new double[][]{{0, 0}, {1, 0}, {12, 5}}));
        assertEquals(0, NaomiBalance.mostIsolated(new double[][]{{-20, 0}, {1, 0}, {3, 0}}));
    }

    @Test
    void theStrapLineCatchesOnlyWhoStaysOnIt() {
        // A chair at (0, 0), the hunter seen at (10, 0).
        assertTrue(NaomiBalance.onLane(10, 0, 0, 0, 10, 0, 1.1), "still where they stood");
        assertTrue(NaomiBalance.onLane(6, 1.0, 0, 0, 10, 0, 1.1), "a step aside is not enough");
        assertFalse(NaomiBalance.onLane(6, 1.6, 0, 0, 10, 0, 1.1), "a sidestep clears it");
        assertTrue(NaomiBalance.onLane(10.8, 0, 0, 0, 10, 0, 1.1), "just past the end");
        assertFalse(NaomiBalance.onLane(13, 0, 0, 0, 10, 0, 1.1), "well past the end");
        assertFalse(NaomiBalance.onLane(-2, 0, 0, 0, 10, 0, 1.1), "behind the chair");
    }

    @Test
    void thePhaseTwoLanceStrikesThreeTimesEachShownFirst() {
        assertEquals(1, NaomiBalance.lunges(1));
        assertEquals(3, NaomiBalance.lunges(2));
        assertEquals(1, NaomiBalance.lanceActive(1));
        assertEquals(1 + 2 * NaomiBalance.LANCE_GAP, NaomiBalance.lanceActive(2));
        assertTrue(NaomiBalance.LANCE_WARN < NaomiBalance.LANCE_GAP, "each next lunge is shown before it lands");
        assertTrue(NaomiBalance.LANCE_WARN >= 10, "and shown long enough to step out");
    }

    @Test
    void theWipeSweepsOutAndItsGapsAreLanes() {
        assertEquals(1, NaomiBalance.wipeRadius(0), 1e-9);
        assertEquals(NaomiBalance.WIPE_RADIUS, NaomiBalance.wipeRadius(NaomiBalance.WIPE_TICKS), 1e-9);
        assertEquals(NaomiBalance.WIPE_RADIUS, NaomiBalance.wipeRadius(NaomiBalance.WIPE_TICKS + 10), 1e-9);
        List<Float> gaps = NaomiBalance.wipeGaps(3, 0);
        assertEquals(List.of(0f, 120f, 240f), gaps);
        // Yaw 0 is south (+z): a hunter 6 south of her, on the lane.
        assertTrue(NaomiBalance.inGap(0, 6, gaps, 1.4), "on the south lane");
        assertTrue(NaomiBalance.inGap(1.2, 6, gaps, 1.4), "at its edge");
        assertFalse(NaomiBalance.inGap(2, 6, gaps, 1.4), "beside it");
        assertFalse(NaomiBalance.inGap(0, -6, gaps, 1.4), "north is not a gap");
        assertFalse(NaomiBalance.inGap(0, 0.2, gaps, 1.4), "not at her feet");
        // Yaw 120: -sin(120°), cos(120°) = (-0.866, -0.5).
        assertTrue(NaomiBalance.inGap(-0.866 * 8, -0.5 * 8, gaps, 1.4), "on the second lane");
    }

    @Test
    void twoGuardsWardHer() {
        assertEquals(1f, NaomiBalance.ward(0));
        assertEquals(1f, NaomiBalance.ward(1));
        assertEquals(0.6f, NaomiBalance.ward(2), 1e-6);
        assertEquals(0.6f, NaomiBalance.ward(3), 1e-6);
        assertEquals(2, NaomiBalance.guards(1));
        assertEquals(3, NaomiBalance.guards(2));
        assertTrue(NaomiBalance.guards(2) <= ReprogrammingRoomLayout.GUARD_SPAWNS.size(), "an alcove for each");
    }

    @Test
    void theTestPassesOnlyCleanAndInTime() {
        assertTrue(NaomiBalance.testPassed(0, false, 100, 300));
        assertFalse(NaomiBalance.testPassed(1, false, 100, 300), "a hostile left");
        assertFalse(NaomiBalance.testPassed(0, true, 100, 300), "a kneeler touched");
        assertFalse(NaomiBalance.testPassed(0, false, 301, 300), "too late");
        assertTrue(NaomiBalance.kneelers(2) + NaomiBalance.hostiles(2) <= ReprogrammingRoomLayout.COPY_SPOTS.size(), "a spot for every copy");
    }

    @Test
    void theConsoleHealsAShareASecondUpToItsCap() {
        float max = 30_000f;
        assertEquals(120f, NaomiBalance.recalibrationSecond(max, 0), 1e-3, "0.4% a second");
        assertEquals(60f, NaomiBalance.recalibrationSecond(max, 1440f), 1e-3, "the last of the 5%");
        assertEquals(0f, NaomiBalance.recalibrationSecond(max, 1500f), 1e-3, "nothing past 5%");
        assertTrue(NaomiBalance.RECAL_TICKS >= 12.5 * 20, "long enough to give it all if nobody strikes the console");
        assertEquals(6, NaomiBalance.CONSOLE_HITS);
    }

    @Test
    void everyClipTheCodePlaysIsInTheArtContract() {
        for (String clip : NaomiAnimations.LOOPS) assertTrue(HeavenAssets.NAOMI_CLIPS.contains(clip), clip);
        for (String clip : NaomiAnimations.TRIGGERED) assertTrue(HeavenAssets.NAOMI_CLIPS.contains(clip), clip);
        for (String clip : NaomiAnimations.LOOPS) assertTrue(HeavenAssets.NAOMI_LOOPS.contains(clip), clip + " loops");
        assertTrue(HeavenAssets.CHAIR_CLIPS.contains(NaomiAnimations.CHAIR_IDLE));
        for (String clip : NaomiAnimations.CHAIR_TRIGGERED) assertTrue(HeavenAssets.CHAIR_CLIPS.contains(clip), clip);
        assertTrue(HeavenAssets.NAOMI_BONES.contains("drill"), "the drill bone the renderer shows");
    }

    @Test
    void everyWindupEndsOnItsClipsMoment() {
        assertEquals(HeavenAssets.NAOMI_HIT_TICKS.get("palm_strike"), NaomiBalance.PALM_WINDUP);
        assertEquals(HeavenAssets.NAOMI_HIT_TICKS.get("restraint"), NaomiBalance.RESTRAINT_WINDUP);
        assertEquals(HeavenAssets.NAOMI_HIT_TICKS.get("strap_in"), NaomiBalance.STRAP_CUE);
        assertEquals(HeavenAssets.NAOMI_HIT_TICKS.get("drill_lance"), NaomiBalance.LANCE_WINDUP);
        assertEquals(HeavenAssets.NAOMI_HIT_TICKS.get("drill_lance"), NaomiBalance.LANCE_WARN);
        assertEquals(HeavenAssets.NAOMI_HIT_TICKS.get("wipe"), NaomiBalance.WIPE_WINDUP);
        assertEquals(HeavenAssets.NAOMI_HIT_TICKS.get("call_guards"), NaomiBalance.GUARDS_WINDUP);
        assertEquals(HeavenAssets.NAOMI_HIT_TICKS.get("test"), NaomiBalance.TEST_WINDUP);
        assertEquals(HeavenAssets.NAOMI_HIT_TICKS.get("death"), NaomiBalance.DEATH_LIGHT);
        assertEquals(HeavenAssets.NAOMI_CLIP_TICKS.get("death"), NaomiBalance.DEATH_TICKS);
        assertEquals(HeavenAssets.NAOMI_CLIP_TICKS.get("emerge"), NaomiBalance.EMERGE_TICKS);
        assertTrue(NaomiBalance.STRAP_WINDUP >= NaomiBalance.STRAP_CUE, "the strap's line is up before its clip");
    }
}
