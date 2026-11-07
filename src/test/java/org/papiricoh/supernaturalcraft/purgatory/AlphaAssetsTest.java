package org.papiricoh.supernaturalcraft.purgatory;

import org.papiricoh.supernaturalcraft.entity.alpha.AlphaAssets;
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

/** The Alphas' art (tools/artgen/alphas_art.py) against {@link AlphaAssets}: models, clips and lengths, atlases, bones. */
class AlphaAssetsTest {

    @Test
    void theAlphasMeetTheContract() throws IOException {
        for (String id : AlphaAssets.CREATURES) {
            Set<String> b = bones(AlphaAssets.geo(id));
            assertTrue(b.size() >= AlphaAssets.MIN_BONES.get(id), id + " has only " + b.size() + " bones");
            for (String bone : AlphaAssets.BONES.get(id)) assertTrue(b.contains(bone), id + " misses bone " + bone);
            assertAtlas(AlphaAssets.geo(id), AlphaAssets.texture(id));
            assertAtlas(AlphaAssets.geo(id), AlphaAssets.glowmask(id));
            assertClips(AlphaAssets.anim(id), "animation." + id + ".", AlphaAssets.CLIPS.get(id), AlphaAssets.LOOPS.get(id),
                    AlphaAssets.CLIP_TICKS.get(id), AlphaAssets.TOGGLED.get(id));
            for (Map.Entry<String, Integer> e : AlphaAssets.CLIP_TICKS.get(id).entrySet()) {
                Integer hit = AlphaAssets.HIT_TICKS.get(e.getKey());
                if (hit != null) assertTrue(hit < e.getValue(), id + "'s " + e.getKey() + " lands inside it");
            }
        }
    }
}
