package org.papiricoh.supernaturalcraft.apocalypse.art;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.apocalypse.ApocalypseAssets;

import java.io.IOException;

import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertSize;

/** The Apocalypse's shared art (tools/artgen/blood_art.py, apocalypse_particles_art.py) against {@link ApocalypseAssets}. */
class ApocalypseAssetsTest {

    @Test
    void theShackleAndTheRings() throws IOException {
        assertSize(ApocalypseAssets.SHACKLE, 16, 16);
        for (String p : ApocalypseAssets.PARTICLES) assertSize("textures/particle/" + p + ".png", 16, 16);
    }
}
