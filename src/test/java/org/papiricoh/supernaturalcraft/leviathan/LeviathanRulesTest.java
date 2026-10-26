package org.papiricoh.supernaturalcraft.leviathan;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.dick.BoardroomGround;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickBalance;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseGenerator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Leviathans' pure rules, Dick Roman's thresholds, his boardroom's plan and the village cases (v0.25). */
class LeviathanRulesTest {

    @Test
    void ordinaryHarmIsCutAndBoraxLandsWhole() {
        assertEquals(10 * LeviathanRules.DAMAGE_TAKEN, LeviathanRules.damageTaken(10, false), 1e-6);
        assertEquals(10, LeviathanRules.damageTaken(10, true), 1e-6);
        assertTrue(LeviathanRules.regen(false) > 0);
        assertEquals(0, LeviathanRules.regen(true));
    }

    @Test
    void onlyASharpBlowAtHalfHealthTakesTheHeadOff() {
        assertTrue(LeviathanRules.decapitates(true, 20, 40));
        assertTrue(LeviathanRules.decapitates(true, -5, 40));
        assertFalse(LeviathanRules.decapitates(true, 21, 40));
        assertFalse(LeviathanRules.decapitates(false, 5, 40));
    }

    @Test
    void aHeadIsLostOnlyFarAndLongEnough() {
        double far = (LeviathanRules.HEAD_LOST_RANGE + 1) * (LeviathanRules.HEAD_LOST_RANGE + 1);
        assertTrue(LeviathanRules.headLost(far, LeviathanRules.HEAD_LOST_TICKS));
        assertFalse(LeviathanRules.headLost(far, LeviathanRules.HEAD_LOST_TICKS - 1));
        assertFalse(LeviathanRules.headLost(4, 10_000));
        assertTrue(LeviathanRules.rejoins(1));
        assertFalse(LeviathanRules.rejoins(4));
    }

    @Test
    void aVillageIsNeverEmptiedNorOverrun() {
        assertTrue(LeviathanRules.infiltrates(0, 5, 0));
        assertFalse(LeviathanRules.infiltrates(LeviathanRules.MAX_PER_VILLAGE, 10, 0));
        assertFalse(LeviathanRules.infiltrates(0, LeviathanRules.MIN_VILLAGERS_LEFT, 0));
        assertFalse(LeviathanRules.infiltrates(0, 10, LeviathanRules.INFILTRATION_CHANCE));
    }

    @Test
    void dicksPhasesFollowEachOtherWithoutGaps() {
        float last = 1f;
        for (int p = 1; p <= DickBalance.PHASES; p++) {
            assertTrue(DickBalance.threshold(p) < last, "phase " + p);
            last = DickBalance.threshold(p);
        }
        assertEquals(0f, DickBalance.threshold(DickBalance.PHASES));
    }

    @Test
    void theBoardroomFitsItsBudgetAndLeavesFloorForTheFlood() {
        List<ArenaCell> plan = BoardroomGround.plan(16);
        assertTrue(plan.size() <= BoardroomGround.BUDGET);
        Set<String> cells = new HashSet<>();
        int flood = 0;
        for (ArenaCell c : plan) {
            assertTrue(cells.add(c.dx() + "," + c.dy() + "," + c.dz()), "one block per cell");
            assertTrue(c.dx() * c.dx() + c.dz() * c.dz() < 16 * 16, "inside the arena");
            if (c.block().equals(BoardroomGround.FLOOR_AIR)) flood++;
        }
        assertTrue(flood > 300, "the Flood has floor to cover: " + flood);
        assertTrue(plan.stream().noneMatch(c -> c.dy() == 1 && c.dx() * c.dx() + c.dz() * c.dz() <= 4 && !c.block().equals(BoardroomGround.FLOOR_AIR)),
                "nothing stands at the centre");
    }

    @Test
    void halfTheVillageCasesAreTheLeviathansOnceTheyCome() {
        int theirs = 0, villages = 0;
        for (int i = 0; i < 400; i++) {
            CaseGenerator.Plan plan = CaseGenerator.roll(42, 7, 9, i, 3, 100, 300);
            CaseGenerator.Plan closed = CaseGenerator.withLeviathans(plan, false);
            assertEquals(plan, closed, "nothing changes before Raphael falls");
            CaseGenerator.Plan open = CaseGenerator.withLeviathans(plan, true);
            assertEquals(open, CaseGenerator.withLeviathans(plan, true), "deterministic");
            if (open.monster().equals(CaseGenerator.LEVIATHAN)) {
                theirs++;
                assertEquals("village", open.scenario());
            }
            if (plan.scenario().equals("village")) villages++;
        }
        assertTrue(theirs > 0 && theirs < villages, theirs + " of " + villages);
    }
}
