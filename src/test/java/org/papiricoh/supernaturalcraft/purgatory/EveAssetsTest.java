package org.papiricoh.supernaturalcraft.purgatory;

import org.papiricoh.supernaturalcraft.entity.boss.eve.EveAssets;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.papiricoh.supernaturalcraft.purgatory.ArtFiles.ASSETS;
import static org.papiricoh.supernaturalcraft.purgatory.ArtFiles.assertAtlas;
import static org.papiricoh.supernaturalcraft.purgatory.ArtFiles.assertClips;
import static org.papiricoh.supernaturalcraft.purgatory.ArtFiles.bones;
import static org.papiricoh.supernaturalcraft.purgatory.ArtFiles.pngSize;

/** Eve's art (tools/artgen/eve_art.py) against {@link EveAssets}: model, clips and lengths, atlas, bones, toggled bones never scaled. */
class EveAssetsTest {

    @Test
    void eveMeetsTheContract() throws IOException {
        Set<String> b = bones(EveAssets.GEO);
        assertTrue(b.size() >= EveAssets.MIN_BONES, "Eve has only " + b.size() + " bones");
        for (String bone : EveAssets.BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        assertAtlas(EveAssets.GEO, EveAssets.TEXTURE);
        assertAtlas(EveAssets.GEO, EveAssets.GLOWMASK);
        assertClips(EveAssets.ANIM, "animation.eve.", EveAssets.CLIPS, EveAssets.LOOPS, EveAssets.CLIP_TICKS, EveAssets.TOGGLED);
        for (String clip : EveAssets.HIT_TICKS.keySet()) {
            assertTrue(EveAssets.HIT_TICKS.get(clip) < EveAssets.CLIP_TICKS.get(clip), clip + "'s blow lands inside it");
        }
    }
}
