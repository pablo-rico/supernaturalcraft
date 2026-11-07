package org.papiricoh.supernaturalcraft.apocalypse.seal;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The plagues' ledger (v0.40): bounded, one entry per block (the first original kept), reverted by time, in batches, by area. */
class PlagueLedgerTest {

    static final String WATER = "minecraft:water[level=0]", BLOOD = "supernaturalcraft:blood_pool";

    @Test
    void oneEntryPerBlockKeepsTheFirstOriginal() {
        PlagueLedger l = new PlagueLedger();
        long p = PlagueLedger.pack(10, 64, -3);
        assertTrue(l.add(p, WATER, BLOOD, 100));
        assertFalse(l.add(p, BLOOD, "minecraft:air", 200), "a block changed twice is remembered once");
        assertEquals(WATER, l.get(p).original());
        assertEquals(1, l.size());
    }

    @Test
    void itIsBounded() {
        PlagueLedger l = new PlagueLedger();
        for (int i = 0; i < PlagueLedger.CAP; i++) assertTrue(l.add(PlagueLedger.pack(i, 0, 0), WATER, BLOOD, 0));
        assertTrue(l.full());
        assertFalse(l.add(PlagueLedger.pack(-1, 0, 0), WATER, BLOOD, 0), "nothing past the cap (so nothing is changed)");
        assertEquals(PlagueLedger.CAP, l.size());
    }

    @Test
    void dueOnlyWhenItsTimeComesAndInBatches() {
        PlagueLedger l = new PlagueLedger();
        for (int i = 0; i < 100; i++) l.add(PlagueLedger.pack(i, 0, 0), WATER, BLOOD, i < 80 ? 50 : 500);
        assertTrue(l.due(49, PlagueLedger.REVERT_PER_TICK).isEmpty(), "not before its time");
        List<PlagueLedger.Entry> first = l.due(50, PlagueLedger.REVERT_PER_TICK);
        assertEquals(PlagueLedger.REVERT_PER_TICK, first.size(), "at most a batch a tick");
        assertEquals(80 - PlagueLedger.REVERT_PER_TICK, l.due(50, PlagueLedger.REVERT_PER_TICK).size());
        assertEquals(20, l.size(), "the later ones wait");
        assertEquals(20, l.take(PlagueLedger.REVERT_PER_TICK).size(), "the echo takes everything");
        assertTrue(l.isEmpty());
    }

    @Test
    void hastenByArea() {
        PlagueLedger l = new PlagueLedger();
        l.add(PlagueLedger.pack(0, 70, 0), WATER, BLOOD, 1000);
        l.add(PlagueLedger.pack(5, 70, 5), WATER, BLOOD, 1000);
        l.add(PlagueLedger.pack(200, 70, 0), WATER, BLOOD, 1000);
        assertEquals(2, l.hasten(0, 0, 10, 10));
        assertEquals(2, l.due(10, 64).size());
        assertEquals(1, l.size(), "the far one is untouched");
        l.hastenAll(10);
        assertEquals(1, l.due(10, 64).size());
    }

    @Test
    void onlyAnUntouchedBlockGoesBack() {
        PlagueLedger.Entry e = new PlagueLedger.Entry(0, WATER, BLOOD, 0);
        assertTrue(PlagueLedger.untouched(e, BLOOD));
        assertFalse(PlagueLedger.untouched(e, "minecraft:cobblestone"), "a hunter built over it: their work stays");
    }

    @Test
    void packingMatchesBlockPos() {
        for (int[] p : new int[][]{{0, 0, 0}, {-30_000_000, -64, 29_999_999}, {12345, 319, -6789}, {-1, -1, -1}}) {
            long l = PlagueLedger.pack(p[0], p[1], p[2]);
            assertEquals(net.minecraft.core.BlockPos.asLong(p[0], p[1], p[2]), l);
            assertEquals(p[0], PlagueLedger.unpackX(l));
            assertEquals(p[1], PlagueLedger.unpackY(l));
            assertEquals(p[2], PlagueLedger.unpackZ(l));
        }
    }
}
