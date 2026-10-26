package org.papiricoh.supernaturalcraft.chain;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HierarchyRestoreTest {
    @Test void restoresGapsAndNextIdWithoutRecruitment() {
        Hierarchy h = new Hierarchy(ChainFaction.DEMON);
        Officer lord = new Officer(7, ChainFaction.DEMON); lord.rank = ChainRank.LORD;
        Officer captain = new Officer(12, ChainFaction.DEMON); captain.rank = ChainRank.CAPTAIN; captain.superior = lord.id;
        captain.entityType = lord.entityType = "supernaturalcraft:black_eyed_demon";
        h.restore(List.of(lord, captain), 30, 20);
        assertEquals(30, h.nextId());
        assertSame(captain, h.get(12));
        assertEquals(7, captain.superior);
        assertEquals(30, h.recruit(ChainRank.SOLDIER, captain.id, 5, captain.entityType, 0).id);
        assertTrue(h.invariantsHold());
    }
    @Test void restoresReturnClockAndDoesNotReuseDeletedIds() {
        Hierarchy h = new Hierarchy(ChainFaction.ANGEL);
        Officer o = new Officer(21, ChainFaction.ANGEL); o.status = Officer.Status.RETURNING; o.returnDay = 40;
        o.entityType = "supernaturalcraft:angel";
        h.restore(List.of(o), 0, 39);
        assertFalse(h.revive(o));
        assertTrue(h.revive(o, 40));
        assertEquals(22, h.nextId());
        assertEquals(1, o.scars);
        assertTrue(h.invariantsHold());
    }
    @Test void restoresGraveyardEvictionOrder() {
        Hierarchy h = new Hierarchy(ChainFaction.DEMON);
        var dead = new java.util.ArrayList<Officer>();
        for (int id = 0; id < Hierarchy.GRAVEYARD_MAX; id++) {
            Officer o = new Officer(id, ChainFaction.DEMON); o.status = Officer.Status.DEAD; dead.add(o);
        }
        h.restore(dead, 20, 1);
        Officer o = h.recruit(ChainRank.SOLDIER, Officer.NONE, 0, "supernaturalcraft:black_eyed_demon", 0);
        h.kill(o, true, 0);
        assertNull(h.get(0));
        assertEquals(Hierarchy.GRAVEYARD_MAX, h.all().size());
        assertTrue(h.invariantsHold());
    }
}
