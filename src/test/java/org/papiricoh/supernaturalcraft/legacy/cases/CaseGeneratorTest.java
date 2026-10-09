package org.papiricoh.supernaturalcraft.legacy.cases;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cases: deterministic, within their caps, a scenario the monster haunts, twists by tier, a site on the ring. */
class CaseGeneratorTest {

    @Test
    void sameInputsSameCase() {
        assertEquals(CaseGenerator.roll(9, 1, 2, 3, 2, 400, 1500), CaseGenerator.roll(9, 1, 2, 3, 2, 400, 1500));
        Set<CaseGenerator.Plan> plans = new HashSet<>();
        for (int i = 0; i < 20; i++) plans.add(CaseGenerator.roll(9, 1, 2, i, 2, 400, 1500));
        assertTrue(plans.size() >= 15, "cases differ from one index to the next");
    }

    @Test
    void everyCaseKeepsItsRules() {
        Set<String> monsters = new HashSet<>(), scenarios = new HashSet<>(), twists = new HashSet<>();
        for (int rank = 0; rank <= 6; rank++) {
            for (int i = 0; i < 200; i++) {
                CaseGenerator.Plan p = CaseGenerator.roll(1234L * rank, i * 7L, -i, i, rank, 400, 1500);
                assertTrue(p.tier() >= 1 && p.tier() <= 5, "tier " + p.tier());
                assertEquals(CaseGenerator.tier(rank), p.tier());
                CaseGenerator.Monster m = CaseGenerator.MONSTERS.stream().filter(x -> x.id().equals(p.monster())).findFirst().orElseThrow();
                assertTrue(m.scenarios().contains(p.scenario()), p.monster() + " doesn't haunt " + p.scenario());
                assertTrue(CaseGenerator.SCENARIOS.contains(p.scenario()));
                assertTrue(CaseGenerator.twists(p.tier()).contains(p.twist()), "twist " + p.twist() + " at tier " + p.tier());
                assertTrue(p.count() >= 1 && CaseGenerator.creatures(p) <= CaseGenerator.MAX_COUNT, "creatures " + CaseGenerator.creatures(p));
                assertEquals(p.twist().equals("named_leader"), !p.leader().isEmpty());
                assertEquals(p.twist().equals("second_monster"), !p.extra().isEmpty());
                if (!p.extra().isEmpty()) assertTrue(!p.extra().equals(p.monster()), "a different second monster");
                double d = Math.hypot(p.dx(), p.dz());
                assertTrue(d >= 399 && d <= 1501, "site " + d + " blocks away");
                monsters.add(p.monster());
                scenarios.add(p.scenario());
                twists.add(p.twist());
            }
        }
        assertEquals(CaseGenerator.MONSTERS.size(), monsters.size(), "every monster turns up");
        assertEquals(CaseGenerator.SCENARIOS.size(), scenarios.size(), "every scenario turns up");
        assertEquals(CaseGenerator.TWISTS.size(), twists.size(), "every twist turns up");
    }

    @Test
    void lowTiersAreGentle() {
        for (int i = 0; i < 300; i++) {
            CaseGenerator.Plan p = CaseGenerator.roll(5, 6, 7, i, 1, 400, 1500);
            assertTrue(p.twist().isEmpty() || p.twist().equals("named_leader"), "tier I twist " + p.twist());
        }
    }

    @Test
    void theLeaderAndTheExtraDoNotDependOnTheRing() {
        for (int i = 0; i < 50; i++) {
            CaseGenerator.Plan a = CaseGenerator.roll(3, 4, 5, i, 5, 400, 1500), b = CaseGenerator.roll(3, 4, 5, i, 5, 0, 1);
            assertEquals(a.monster(), b.monster());
            assertEquals(a.leader(), b.leader());
            assertEquals(a.extra(), b.extra());
            assertEquals(a.count(), b.count());
        }
    }

    @Test
    void setPiecesStayInsideTheirRadiusWithoutBlockEntities() {
        for (String s : CaseGenerator.SCENARIOS) {
            var cells = CaseLayout.piece(s);
            assertTrue(cells.size() >= 8, s + " has a set piece");
            Set<String> seen = new HashSet<>();
            for (CaseLayout.Cell c : cells) {
                assertTrue(Math.hypot(c.x(), c.z()) <= CaseLayout.RADIUS, s + ": " + c + " too far");
                assertTrue(c.y() >= -2 && c.y() <= 8, s + ": " + c);
                assertTrue(seen.add(c.x() + "," + c.y() + "," + c.z()), s + ": twice at " + c);
                for (String bad : new String[]{"chest", "barrel", "bed", "campfire", "sign", "spawner", "furnace"}) {
                    assertTrue(!c.state().contains(bad), s + ": " + c.state() + " has a block entity");
                }
            }
            assertTrue(CaseLayout.lairs(s).length >= CaseGenerator.MAX_COUNT, s + " has a lair for each creature");
        }
    }
}
