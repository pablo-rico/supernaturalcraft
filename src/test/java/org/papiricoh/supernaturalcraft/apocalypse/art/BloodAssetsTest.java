package org.papiricoh.supernaturalcraft.apocalypse.art;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.apocalypse.blood.BloodAssets;

import java.io.IOException;

import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertSize;

/** The demon blood's art (tools/artgen/blood_art.py, ruby_art.py, apocalypse_particles_art.py) against {@link BloodAssets}. */
class BloodAssetsTest {

    @Test
    void theBloodMeetsTheContract() throws IOException {
        for (String id : BloodAssets.POWER_ICONS) assertSize("textures/gui/allegiance/power/" + id + ".png", 24, 24);
        assertSize(BloodAssets.HUD_SHEET, 128, 64);
        assertSize(BloodAssets.VIGNETTE, 256, 256);
        assertSize(BloodAssets.EFFECT_ICON, 18, 18);
        for (String path : BloodAssets.DOOR) assertSize(path, 16, 16);
        assertSize(BloodAssets.FLASK, 16, 16);
        for (String p : BloodAssets.PARTICLES) assertSize("textures/particle/" + p + ".png", 16, 16);
    }
}
