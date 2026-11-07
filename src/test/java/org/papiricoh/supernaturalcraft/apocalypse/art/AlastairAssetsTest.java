package org.papiricoh.supernaturalcraft.apocalypse.art;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.alastair.AlastairAssets;

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

/** Alastair's art (tools/artgen/alastair_art.py, alastair_gui_art.py) against {@link AlastairAssets}. */
class AlastairAssetsTest {

    @Test
    void heMeetsTheContract() throws IOException {
        assertBones(AlastairAssets.GEO, AlastairAssets.BONES, AlastairAssets.MIN_BONES);
        Set<String> b = bones(AlastairAssets.GEO);
        for (String bone : AlastairAssets.OPTIONAL_BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        assertAtlas(AlastairAssets.GEO, AlastairAssets.TEXTURE);
        assertAtlas(AlastairAssets.GEO, AlastairAssets.GLOW);
        // The renderer shows and hides the scalpel and the eyes: no clip keys them.
        assertClips(AlastairAssets.ANIM, "animation.alastair.", AlastairAssets.CLIPS, AlastairAssets.LOOPS, AlastairAssets.CLIP_TICKS,
                List.of("scalpel", "eyes"));
        assertClipBonesExist(AlastairAssets.GEO, AlastairAssets.ANIM);
        assertHits(AlastairAssets.HIT_TICKS, AlastairAssets.CLIP_TICKS);
        for (String clip : AlastairAssets.TRIGGERED) assertTrue(AlastairAssets.CLIP_TICKS.containsKey(clip), clip + " has a length");
    }

    @Test
    void theRackAndTheHook() throws IOException {
        String rack = "geo/entity/" + AlastairAssets.RACK + ".geo.json";
        String rackAnim = "animations/entity/" + AlastairAssets.RACK + ".animation.json";
        assertBones(rack, List.of("frame", "chains", "cuffs"), 3);
        assertAtlas(rack, "textures/entity/" + AlastairAssets.RACK + ".png");
        assertClips(rackAnim, "animation." + AlastairAssets.RACK + ".", AlastairAssets.RACK_CLIPS, List.of("idle"),
                AlastairAssets.RACK_CLIP_TICKS, List.of());
        assertClipBonesExist(rack, rackAnim);
        String hook = "geo/entity/" + AlastairAssets.HOOK + ".geo.json";
        assertAtlas(hook, "textures/entity/" + AlastairAssets.HOOK + ".png");
        assertClipBonesExist(hook, "animations/entity/" + AlastairAssets.HOOK + ".animation.json");
        // The thrown scalpel uses vanilla's arrow layout.
        assertSize(AlastairAssets.THROWN_SCALPEL, 32, 32);
    }

    @Test
    void spoilsAndBar() throws IOException {
        assertSize(AlastairAssets.SCALPEL_ITEM, 16, 16);
        assertBlockModel(AlastairAssets.TROPHY_MODEL);
        assertSize(AlastairAssets.BAR_SHEET, 256, 128);
    }
}
