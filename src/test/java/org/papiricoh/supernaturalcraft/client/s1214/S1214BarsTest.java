package org.papiricoh.supernaturalcraft.client.s1214;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The v0.41 boss bars' key convention and sheets. */
class S1214BarsTest {

    @Test
    void recognisesEachBossByItsKey() {
        for (String rig : S1214Bars.RIGS) {
            assertEquals(rig, S1214Bars.rigOf("entity.supernaturalcraft." + rig + ".bar"));
            assertEquals(rig, S1214Bars.rigOf("entity.supernaturalcraft." + rig + ".bar.phase2"));
        }
    }

    @Test
    void ignoresOtherBossesAndLookalikes() {
        assertNull(S1214Bars.rigOf("entity.supernaturalcraft.michael.bar.phase1"));
        assertNull(S1214Bars.rigOf("entity.supernaturalcraft.alastair.bar.phase1"));
        assertNull(S1214Bars.rigOf("entity.supernaturalcraft.shadow"));
        assertNull(S1214Bars.rigOf("entity.supernaturalcraft.shadow.barrier"));
        assertNull(S1214Bars.rigOf("entity.supernaturalcraft.dagon_minion.bar"));
        assertNull(S1214Bars.rigOf(null));
    }

    @Test
    void readsThePhase() {
        assertEquals(1, S1214Bars.phase("entity.supernaturalcraft.hess.bar"));
        assertEquals(1, S1214Bars.phase("entity.supernaturalcraft.hess.bar.phase1"));
        assertEquals(3, S1214Bars.phase("entity.supernaturalcraft.hess.bar.phase3"));
        assertEquals(12, S1214Bars.phase("entity.supernaturalcraft.shadow.bar.phase12"));
        assertEquals(1, S1214Bars.phase("entity.supernaturalcraft.shadow.bar.phase"));
        assertEquals(1, S1214Bars.phase("entity.supernaturalcraft.shadow.bar.phase0"));
    }

    @Test
    void everySheetIsThere() throws IOException {
        for (String rig : S1214Bars.RIGS) {
            Path p = Path.of("src/main/resources/assets/supernaturalcraft").resolve(S1214Bars.sheet(rig));
            assertTrue(Files.exists(p), "missing " + p);
            try (InputStream in = Files.newInputStream(p)) {
                byte[] h = in.readNBytes(24);
                int w = ((h[16] & 0xff) << 24) | ((h[17] & 0xff) << 16) | ((h[18] & 0xff) << 8) | (h[19] & 0xff);
                int ht = ((h[20] & 0xff) << 24) | ((h[21] & 0xff) << 16) | ((h[22] & 0xff) << 8) | (h[23] & 0xff);
                assertEquals(256, w, rig + " sheet width");
                assertEquals(128, ht, rig + " sheet height");
            }
        }
    }
}
