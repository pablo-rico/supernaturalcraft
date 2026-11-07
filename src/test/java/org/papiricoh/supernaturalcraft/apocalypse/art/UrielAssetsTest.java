package org.papiricoh.supernaturalcraft.apocalypse.art;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.uriel.UrielAssets;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertAtlas;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertBlockModel;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertBones;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertClipBonesExist;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertClips;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertHits;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertSize;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.bones;

/** Uriel's art (tools/artgen/uriel_art.py) against {@link UrielAssets}. */
class UrielAssetsTest {

    @Test
    void heMeetsTheContract() throws IOException {
        assertBones(UrielAssets.GEO, UrielAssets.BONES, UrielAssets.MIN_BONES);
        Set<String> b = bones(UrielAssets.GEO);
        for (String bone : UrielAssets.OPTIONAL_BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        assertAtlas(UrielAssets.GEO, UrielAssets.TEXTURE);
        assertAtlas(UrielAssets.GEO, UrielAssets.GLOW);
        // The renderer shows and hides the wings (with all under them) and the eyes: no clip keys those two bones.
        assertClips(UrielAssets.ANIM, "animation.uriel.", UrielAssets.CLIPS, UrielAssets.LOOPS, UrielAssets.CLIP_TICKS,
                List.of("wings", "eyes"));
        assertClipBonesExist(UrielAssets.GEO, UrielAssets.ANIM);
        assertHits(UrielAssets.HIT_TICKS, UrielAssets.CLIP_TICKS);
    }

    @Test
    void bustAndBar() throws IOException {
        assertBlockModel(UrielAssets.TROPHY_MODEL);
        assertSize(UrielAssets.BAR_SHEET, 256, 128);
    }
}
