package org.papiricoh.supernaturalcraft.apocalypse.art;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealAssets;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.ASSETS;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertAtlas;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertBlockModel;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertBones;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertClipBonesExist;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertClips;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertSize;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.pngSize;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.read;

/** The seals' art (tools/artgen/seal_art.py, seal_sky_art.py) against {@link SealAssets}. */
class SealAssetsTest {

    private static final Map<String, Integer> VARIANTS = Map.of(SealAssets.WITNESS, SealAssets.WITNESS_VARIANTS,
            SealAssets.REBEL, SealAssets.REBEL_VARIANTS);

    @Test
    void mobsMeetTheContract() throws IOException {
        for (String rig : List.of(SealAssets.WITNESS, SealAssets.REAPER, SealAssets.REBEL)) {
            String geo = SealAssets.geo(rig), anim = SealAssets.anim(rig);
            assertBones(geo, SealAssets.BONES.get(rig), SealAssets.BONES.get(rig).size());
            assertClips(anim, "animation." + rig + ".", SealAssets.CLIPS.get(rig), SealAssets.LOOPS.get(rig), SealAssets.CLIP_TICKS,
                    List.of());
            assertClipBonesExist(geo, anim);
            Integer variants = VARIANTS.get(rig);
            if (variants == null) {
                assertAtlas(geo, SealAssets.texture(rig));
                assertAtlas(geo, SealAssets.glow(rig));
            } else {
                for (int n = 0; n < variants; n++) {
                    assertAtlas(geo, SealAssets.texture(rig, n));
                    assertAtlas(geo, SealAssets.glow(rig, n));
                }
                assertTrue(!Files.exists(ASSETS.resolve(SealAssets.texture(rig))), rig + " has variants only, no plain texture");
            }
        }
    }

    @Test
    void rebelWingsAreNeverScaled() throws IOException {
        JsonObject a = read(SealAssets.anim(SealAssets.REBEL)).getAsJsonObject("animations");
        for (String name : a.keySet()) {
            JsonObject bones = a.getAsJsonObject(name).getAsJsonObject("bones");
            if (bones == null || !bones.has("wings")) continue;
            assertTrue(!bones.getAsJsonObject("wings").has("scale"), name + " scales the wings the renderer toggles");
        }
    }

    @Test
    void momentsLandInsideTheirClips() {
        for (Map.Entry<String, Integer> e : SealAssets.HIT_TICKS.entrySet()) {
            Integer len = SealAssets.CLIP_TICKS.get(e.getKey());
            assertTrue(len != null, e.getKey() + " has a moment but no length");
            assertTrue(e.getValue() >= 0 && e.getValue() < len, e.getKey() + "'s moment lands inside it");
            assertTrue(SealAssets.CLIPS.values().stream().anyMatch(c -> c.contains(e.getKey())), e.getKey() + " is some mob's clip");
        }
    }

    @Test
    void blocksItemsSkyAndSheet() throws IOException {
        assertBlockModel(SealAssets.HEX_FOCUS_MODEL);
        int[] pool = pngSize(SealAssets.BLOOD_POOL);
        assertTrue(pool[0] == 16 && pool[1] % 16 == 0, "blood_pool is a strip of 16x16 frames");
        assertTrue(Files.exists(ASSETS.resolve(SealAssets.BLOOD_POOL + ".mcmeta")), "blood_pool is animated");
        assertSize(SealAssets.WING_SCORCH, 32, 32);
        for (String item : SealAssets.ITEMS) assertSize(item, 16, 16);
        assertSize(SealAssets.RED_MOON, 128, 64);
        assertSize(SealAssets.BLOOD_SKY, 256, 256);
        assertSize(SealAssets.GUI_SHEET, 256, 128);
        for (String p : SealAssets.PARTICLES) assertSize("textures/particle/" + p + ".png", 16, 16);
    }
}
