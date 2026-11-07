package org.papiricoh.supernaturalcraft.apocalypse.art;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyAssets;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertAtlas;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertBones;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertClipBonesExist;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertClips;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertHits;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.assertSize;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.bones;
import static org.papiricoh.supernaturalcraft.apocalypse.art.ArtFiles.read;

/** Ruby's art (tools/artgen/ruby_art.py) against {@link RubyAssets}. */
class RubyAssetsTest {

    @Test
    void sheMeetsTheContract() throws IOException {
        assertBones(RubyAssets.GEO, RubyAssets.BONES, RubyAssets.MIN_BONES);
        Set<String> b = bones(RubyAssets.GEO);
        for (String bone : RubyAssets.OPTIONAL_BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        assertAtlas(RubyAssets.GEO, RubyAssets.TEXTURE);
        assertAtlas(RubyAssets.GEO, RubyAssets.GLOW);
        // The renderer toggles the knife, its sheathed hilt and the eyes: no clip keys them.
        assertClips(RubyAssets.ANIM, "animation.ruby.", RubyAssets.CLIPS, RubyAssets.LOOPS, RubyAssets.CLIP_TICKS,
                List.of("knife", "sheath", "sheath_knife", "eyes"));
        assertClipBonesExist(RubyAssets.GEO, RubyAssets.ANIM);
        assertHits(RubyAssets.HIT_TICKS, RubyAssets.CLIP_TICKS);
        assertSize(RubyAssets.FLASK_ITEM, 16, 16);
    }

    /** The flask shows only in {@code give}: every loop scales it away, {@code give} brings it back. */
    @Test
    void theFlaskShowsOnlyWhenGiven() throws IOException {
        JsonObject a = read(RubyAssets.ANIM).getAsJsonObject("animations");
        for (String loop : RubyAssets.LOOPS) {
            JsonObject bones = a.getAsJsonObject("animation.ruby." + loop).getAsJsonObject("bones");
            assertTrue(bones.has("flask") && bones.getAsJsonObject("flask").has("scale"), loop + " hides the flask");
        }
        JsonObject give = a.getAsJsonObject("animation.ruby.give").getAsJsonObject("bones");
        assertTrue(give.has("flask") && give.getAsJsonObject("flask").has("scale"), "give shows the flask");
    }
}
