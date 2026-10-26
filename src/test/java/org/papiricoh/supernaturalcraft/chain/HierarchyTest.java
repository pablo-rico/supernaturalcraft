package org.papiricoh.supernaturalcraft.chain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HierarchyTest {
    static Officer add(Hierarchy h, ChainRank rank) { return h.recruit(rank, Officer.NONE, h.nextId(), "test:mob", 0); }
    static Hierarchy full(ChainFaction faction) {
        Hierarchy h = new Hierarchy(faction);
        for (ChainRank rank : ChainRank.values()) for (int i = 0; i < rank.cap(); i++) assertNotNull(add(h, rank));
        return h;
    }

    @Test void capsAndFailedOperationsAreAtomic() {
        Hierarchy h = full(ChainFaction.DEMON);
        for (ChainRank rank : ChainRank.values()) {
            assertNull(add(h, rank));
            assertEquals(0, h.vacancies().get(rank));
        }
        Officer soldier = h.ofRank(ChainRank.SOLDIER).getFirst();
        assertFalse(h.promote(soldier));
        assertFalse(h.demote(soldier));
        assertFalse(h.demote(h.lord()));
        assertEquals(ChainRank.totalCap(), h.nextId());
        assertTrue(h.invariantsHold());
        assertFalse(h.promote(new Officer(soldier.id, h.faction())));
    }

    @Test void deathReparentsAndPromotionsRepairEdges() {
        Hierarchy h = new Hierarchy(ChainFaction.DEMON);
        Officer lord = add(h, ChainRank.LORD), captain = add(h, ChainRank.CAPTAIN);
        Officer soldier = h.recruit(ChainRank.SOLDIER, captain.id, 4, "test:mob", 0);
        h.kill(captain, false, 10);
        assertSame(lord, h.superiorOf(soldier));
        assertEquals(6, h.vacancies().get(ChainRank.CAPTAIN));
        assertFalse(h.revive(captain, 9));
        assertTrue(h.revive(captain, 10));
        assertEquals(1, captain.scars);
        assertEquals(1, captain.level);
        assertFalse(h.revive(captain, 11));
        assertTrue(h.promote(soldier));
        assertTrue(h.demote(lord));
        assertTrue(h.invariantsHold());
    }

    @Test void returnsFallbackOneRankOrWaitAndDeadNeverReturn() {
        Hierarchy h = full(ChainFaction.ANGEL);
        Officer lord = h.lord();
        h.kill(lord, false, 5);
        add(h, ChainRank.LORD);
        assertFalse(h.revive(lord, 5));
        Officer commander = h.ofRank(ChainRank.COMMANDER).getFirst();
        h.kill(commander, true, 0);
        assertTrue(h.revive(lord, 6));
        assertEquals(ChainRank.COMMANDER, lord.rank);
        assertFalse(h.revive(commander, 100));
        assertTrue(h.invariantsHold());
    }

    @Test void graveyardKeepsDeathOrderRatherThanBirthOrder() {
        Hierarchy h = new Hierarchy(ChainFaction.DEMON);
        Officer oldest = add(h, ChainRank.SOLDIER);
        for (int i = 0; i < 25; i++) h.kill(add(h, ChainRank.SOLDIER), true, 0);
        h.kill(oldest, true, 0);
        assertEquals(20, h.all().size());
        assertSame(oldest, h.get(oldest.id));
        assertNull(h.get(6));
        assertNotNull(h.get(7));
        assertTrue(h.invariantsHold());
    }

    @Test void fallsShareCapsButNeverReportAcrossBranches() {
        Hierarchy h = full(ChainFaction.ANGEL);
        Officer captain = h.ofRank(ChainRank.CAPTAIN).getFirst();
        Officer soldier = h.ofRank(ChainRank.SOLDIER).getFirst();
        assertTrue(h.fall(soldier));
        assertEquals(Officer.NONE, soldier.superior);
        assertTrue(h.fall(captain));
        assertSame(captain, h.superiorOf(soldier));
        assertFalse(h.fall(captain));
        assertEquals(ChainRank.totalCap(), h.alive().size());
        assertTrue(h.invariantsHold());
        h.kill(captain, true, 0);
        assertEquals(Officer.NONE, soldier.superior);
        assertTrue(h.invariantsHold());
        Hierarchy demons = full(ChainFaction.DEMON);
        assertFalse(demons.fall(demons.lord()));
    }

    @Test void orphanAssignmentUsesLeastLoadedAndPreservesValidLinks() {
        Hierarchy h = new Hierarchy(ChainFaction.DEMON);
        Officer a = add(h, ChainRank.CAPTAIN), b = add(h, ChainRank.CAPTAIN);
        for (int i = 0; i < 6; i++) add(h, ChainRank.SOLDIER);
        assertEquals(3, h.subordinates(a.id).size());
        assertEquals(3, h.subordinates(b.id).size());
        Officer child = h.subordinates(a.id).getFirst();
        child.superior = b.id;
        h.reassignOrphans();
        assertEquals(b.id, child.superior);
        a.superior = child.id;
        assertFalse(h.invariantsHold());
        h.reassignOrphans();
        assertTrue(h.invariantsHold());
    }
}
