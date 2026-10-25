package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.angel.AngelAssets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The angels' art (tools/artgen angel_art) against {@link AngelAssets} (v0.22): the shared rig with its bones, light locators
 * (cubeless, in the head), item bone and wings; every clip, looping or not as the contract says, keying only real bones and
 * always posing the wings (unkeyed, they would snap to their built, spread-flat pose); the smitten clip held for its length;
 * every vessel's texture and glowmask at the atlas size; the scorch and the streak.
 */
class AngelAssetsTest {

    private static Map<String, JsonObject> bones() throws IOException {
        Map<String, JsonObject> out = new HashMap<>();
        for (JsonElement b : CovenArt.geometry(AngelAssets.GEO).getAsJsonArray("bones")) {
            out.put(b.getAsJsonObject().get("name").getAsString(), b.getAsJsonObject());
        }
        return out;
    }

    @Test
    void theRig() throws IOException {
        Map<String, JsonObject> b = bones();
        for (String bone : AngelAssets.BONES) assertTrue(b.containsKey(bone), "missing bone " + bone);
        for (String loc : AngelAssets.LIGHT_LOCATORS) {
            assertTrue(b.containsKey(loc), "missing locator " + loc);
            assertEquals("head", b.get(loc).get("parent").getAsString(), loc + " must hang from the head");
            assertFalse(b.get(loc).has("cubes"), loc + " is a locator: no cubes");
        }
        assertTrue(b.containsKey(AngelAssets.WINGS), "missing the wings' root");
        assertTrue(b.containsKey(AngelAssets.RIGHT_ITEM), "missing the item bone");
        assertFalse(b.get(AngelAssets.RIGHT_ITEM).has("cubes"), "the item bone draws nothing itself");
    }

    @Test
    void theClips() throws IOException {
        JsonObject a = CovenArt.anims(AngelAssets.ANIM);
        Set<String> bones = CovenArt.bones(AngelAssets.GEO);
        for (String clip : AngelAssets.CLIPS) {
            String name = "animation.angel." + clip;
            assertTrue(a.has(name), "missing clip " + name);
            JsonObject c = a.getAsJsonObject(name);
            assertEquals(AngelAssets.LOOPS.contains(clip), CovenArt.loops(c), name + " loop mode");
            Set<String> keyed = CovenArt.keyed(c);
            for (String bone : keyed) assertTrue(bones.contains(bone), name + " keys an unknown bone " + bone);
            assertFalse(keyed.contains(AngelAssets.WINGS), name + " keys the wings' root (the renderer hides it)");
            assertFalse(keyed.contains(AngelAssets.RIGHT_ITEM), name + " keys the item bone");
            assertTrue(keyed.stream().anyMatch(k -> k.startsWith("wing_")), name + " leaves the wings in their built pose");
        }
        JsonObject smitten = a.getAsJsonObject("animation.angel.smitten");
        assertEquals("hold_on_last_frame", CovenArt.loopMode(smitten), "the smitten pose holds");
        assertEquals(AngelAssets.SMITTEN_TICKS, CovenArt.ticks(smitten), "smitten length");
        for (var e : AngelAssets.HIT_TICKS.entrySet()) {
            int len = CovenArt.ticks(a.getAsJsonObject("animation.angel." + e.getKey()));
            assertTrue(e.getValue() > 0 && e.getValue() <= len, e.getKey() + " lands after it ends");
        }
    }

    @Test
    void theTextures() throws IOException {
        for (boolean fallen : new boolean[]{false, true}) {
            int n = fallen ? AngelAssets.FALLEN_VESSELS : AngelAssets.ANGEL_VESSELS;
            for (int v = 0; v < n; v++) {
                CovenArt.assertAtlas(AngelAssets.GEO, AngelAssets.texture(fallen, v));
                CovenArt.assertAtlas(AngelAssets.GEO, AngelAssets.glowmask(fallen, v));
            }
        }
        int[] scorch = CovenArt.pngSize(AngelAssets.WING_SCORCH);
        assertEquals(2 * scorch[1], scorch[0], "the scorch is twice as wide as it is long");
        int[] streak = CovenArt.pngSize(AngelAssets.FALL_STREAK);
        assertTrue(streak[1] > streak[0], "the streak is tall");
    }
}
