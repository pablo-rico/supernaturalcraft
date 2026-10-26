package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ChainSavedDataTest {
    @Test void everyOfficerFieldAndWorldQueueRoundTrips() {
        var data = new ChainSavedData();
        UUID victim = UUID.fromString("00000000-0000-0000-0000-000000000001");
        for (var f : ChainFaction.values()) {
            Hierarchy h = data.hierarchy(f);
            String type = f == ChainFaction.DEMON ? "supernaturalcraft:black_eyed_demon" : "supernaturalcraft:angel";
            Officer lord = h.recruit(ChainRank.LORD, Officer.NONE, 987654321, type, 1);
            Officer subordinate = h.recruit(ChainRank.CAPTAIN, lord.id, -22, type, 2);
            subordinate.level = 7; subordinate.scars = 3; subordinate.recordKill(victim); subordinate.recordKill(victim);
            subordinate.log("promoted:CAPTAIN");
            subordinate.sighted("minecraft:the_nether", -812, 4031, 11);
            Officer returning = h.recruit(ChainRank.SOLDIER, subordinate.id, 43, type, 0);
            if (f == ChainFaction.ANGEL) h.fall(returning);
            h.kill(returning, false, 40);
            Officer dead = h.recruit(ChainRank.SOLDIER, subordinate.id, 44, type, 0); h.kill(dead, true, 0);
            data.destabilize(f, 35);
            data.schedule(new ChainSavedData.Vengeance(f, lord.id, victim, 33));
            data.log(new ChainSavedData.History(12, "promote", f, subordinate.id, "detail"));
        }
        data.lastConclaveDay(12);
        CompoundTag saved = data.save(new CompoundTag(), null);
        var loaded = ChainSavedData.load(saved, null);
        assertEquals(saved, loaded.save(new CompoundTag(), null));
        for (var f : ChainFaction.values()) assertTrue(loaded.hierarchy(f).invariantsHold());
        assertEquals(2, loaded.vengeance().size());
        Officer seen = loaded.hierarchy(ChainFaction.DEMON).ofRank(ChainRank.CAPTAIN).getFirst();
        assertEquals("minecraft:the_nether", seen.seenDimension);
        assertEquals(-812, seen.seenX); assertEquals(4031, seen.seenZ); assertEquals(11, seen.seenDay);
        assertEquals(-1, loaded.hierarchy(ChainFaction.DEMON).lord().seenDay, "Never seen stays never seen");
    }
    @Test void globalHistoryIsBoundedAndMutationsDirtyTheSave() {
        var data = new ChainSavedData(); data.setDirty(false);
        long before = data.revision();
        for (int i = 0; i < 80; i++) data.log(new ChainSavedData.History(i, "recruit", ChainFaction.DEMON, i, ""));
        assertTrue(data.isDirty()); assertTrue(data.revision() > before);
        assertEquals(50, data.history().size()); assertEquals(30, data.history().getFirst().day());
        data.reset(); assertTrue(data.history().isEmpty()); assertTrue(data.vengeance().isEmpty()); assertEquals(-1, data.lastConclaveDay());
    }
}
