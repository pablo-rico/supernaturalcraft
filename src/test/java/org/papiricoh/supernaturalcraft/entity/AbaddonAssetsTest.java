package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonAssets;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abaddon's art (tools/artgen abaddon_art, abaddon_retinue_art) against {@link AbaddonAssets} (v0.21 P5): her detailed rig and
 * her retinue's, every clip looping or not as the contract says, at its length, landing in time; the Brand on her palm keyed by
 * the {@code brand} clip alone; the cleaver's model and clips; the Book's and cleaver's icons; her sounds and cinematics.
 */
class AbaddonAssetsTest {

    private static void clips(String rig, List<String> clips, List<String> loops) throws IOException {
        JsonObject a = CovenArt.anims(AbaddonAssets.anim(rig));
        Set<String> bones = CovenArt.bones(AbaddonAssets.geo(rig));
        for (String clip : clips) {
            String name = "animation." + rig + "." + clip;
            assertTrue(a.has(name), "missing clip " + name);
            assertEquals(loops.contains(clip), CovenArt.loops(a.getAsJsonObject(name)), name + (loops.contains(clip) ? " must loop" : " must not loop"));
        }
        for (var clip : a.entrySet()) {
            for (String bone : CovenArt.keyed(clip.getValue().getAsJsonObject())) assertTrue(bones.contains(bone), clip.getKey() + " keys an unknown bone " + bone);
        }
        CovenArt.assertAtlas(AbaddonAssets.geo(rig), AbaddonAssets.texture(rig));
        CovenArt.assertAtlas(AbaddonAssets.geo(rig), AbaddonAssets.glow(rig));
    }

    @Test
    void herRigAndClips() throws IOException {
        Set<String> b = CovenArt.bones(AbaddonAssets.GEO);
        assertTrue(b.size() >= AbaddonAssets.MIN_BONES, "Abaddon has only " + b.size() + " bones");
        for (String bone : AbaddonAssets.BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        assertTrue(b.contains(AbaddonAssets.BRAND), "missing the Brand on her palm");
        clips("abaddon", AbaddonAssets.CLIPS, AbaddonAssets.LOOPS);
        JsonObject a = CovenArt.anims(AbaddonAssets.ANIM);
        assertEquals("hold_on_last_frame", CovenArt.loopMode(a.getAsJsonObject("animation.abaddon.death")), "she holds her death");
        for (var e : AbaddonAssets.CLIP_TICKS.entrySet()) {
            assertEquals((int) e.getValue(), CovenArt.ticks(a.getAsJsonObject("animation.abaddon." + e.getKey())), e.getKey() + " length");
        }
        for (var e : AbaddonAssets.HIT_TICKS.entrySet()) {
            assertTrue(e.getValue() > 0 && e.getValue() <= AbaddonAssets.CLIP_TICKS.get(e.getKey()), e.getKey() + " lands after it ends");
        }
        assertTrue(AbaddonAssets.CLIP_TICKS.keySet().containsAll(AbaddonAssets.TRIGGERED), "every triggered clip has its length");
        for (var clip : a.entrySet()) {
            boolean keys = CovenArt.keyed(clip.getValue().getAsJsonObject()).contains(AbaddonAssets.BRAND);
            assertEquals(clip.getKey().equals("animation.abaddon.brand"), keys, clip.getKey() + (keys ? " keys the Brand" : " must key the Brand"));
        }
    }

    @Test
    void herRetinue() throws IOException {
        for (String rig : List.of(AbaddonAssets.KNIGHT, AbaddonAssets.HERALD)) {
            Set<String> b = CovenArt.bones(AbaddonAssets.geo(rig));
            for (String bone : AbaddonAssets.RETINUE_BONES.get(rig)) assertTrue(b.contains(bone), rig + " misses bone " + bone);
            clips(rig, AbaddonAssets.RETINUE_CLIPS.get(rig), AbaddonAssets.RETINUE_LOOPS.get(rig));
            JsonObject a = CovenArt.anims(AbaddonAssets.anim(rig));
            for (var e : AbaddonAssets.RETINUE_CLIP_TICKS.entrySet()) {
                String name = "animation." + rig + "." + e.getKey();
                if (!a.has(name)) continue;
                assertEquals((int) e.getValue(), CovenArt.ticks(a.getAsJsonObject(name)), name + " length");
                assertTrue(AbaddonAssets.RETINUE_HIT_TICKS.get(e.getKey()) <= e.getValue(), name + " lands after it ends");
            }
        }
        // The knight's blade rests where it is drawn: no clip keys it.
        JsonObject k = CovenArt.anims(AbaddonAssets.anim(AbaddonAssets.KNIGHT));
        for (var clip : k.entrySet()) assertFalse(CovenArt.keyed(clip.getValue().getAsJsonObject()).contains("blade"), clip.getKey() + " keys the blade");
    }

    @Test
    void herSpoilsSoundsAndCinematics() throws IOException {
        for (String icon : List.of(AbaddonAssets.BOOK_OF_THE_DAMNED, AbaddonAssets.CLEAVER_ICON)) {
            int[] s = CovenArt.pngSize(icon);
            assertEquals(16, s[0], icon);
            assertEquals(16, s[1], icon);
        }
        CovenArt.assertAtlas(AbaddonAssets.CLEAVER_GEO, AbaddonAssets.CLEAVER_TEXTURE);
        CovenArt.assertAtlas(AbaddonAssets.CLEAVER_GEO, AbaddonAssets.CLEAVER_GLOW);
        JsonObject c = CovenArt.anims(AbaddonAssets.CLEAVER_ANIM);
        for (String clip : AbaddonAssets.CLEAVER_CLIPS) assertTrue(c.has("animation.knights_cleaver." + clip), "cleaver misses " + clip);
        assertTrue(CovenArt.loops(c.getAsJsonObject("animation.knights_cleaver.idle")), "the veins pulse in a loop");
        CovenArt.read(AbaddonAssets.TROPHY_MODEL);
        for (String s : AbaddonAssets.SOUNDS) assertTrue(CovenArt.soundDefined("abaddon." + s), "no sound abaddon." + s);
        for (String cin : AbaddonAssets.CINEMATICS) {
            JsonObject j = CovenArt.read("cinematics/" + cin + ".json");
            assertTrue(j.has("shots") && !j.getAsJsonArray("shots").isEmpty(), cin + " has no shots");
        }
    }
}
