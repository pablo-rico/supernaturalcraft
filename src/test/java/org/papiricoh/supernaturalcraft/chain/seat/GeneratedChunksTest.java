package org.papiricoh.supernaturalcraft.chain.seat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedChunksTest {

    @Test
    void aChunkIsSavedWhenItsHeaderEntryIsSet() {
        byte[] header = new byte[GeneratedChunks.HEADER];
        // Chunk (3, 2) of its region: entry 3 + 2 * 32, offset bytes 0..2 and the sector count in byte 3.
        header[4 * (3 + 2 * 32) + 3] = 1;
        assertTrue(GeneratedChunks.saved(header, 3, 2));
        assertTrue(GeneratedChunks.saved(header, 3 + 32 * 7, 2 - 32 * 4), "any region, same local chunk");
        assertTrue(GeneratedChunks.saved(header, -29, -30), "negative chunks wrap into the region");
        assertFalse(GeneratedChunks.saved(header, 2, 3));
        assertFalse(GeneratedChunks.saved(null, 3, 2), "no region file, nothing generated");
    }
}
