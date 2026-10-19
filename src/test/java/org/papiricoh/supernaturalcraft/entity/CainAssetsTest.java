package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainAssets;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cain's art (tools/artgen cain_art) against {@link CainAssets} (v0.21 P4): one detailed rig for the boss and the beekeeper at
 * home, with every bone the code reads and the three the renderer alone drives (the Mark, the First Blade, the eyes); every boss
 * clip and the beekeeper's {@code tend}/{@code talk} loops, looping or not as the contract says, at their lengths, landing in
 * time; no clip keys a renderer bone; his sounds, cinematics and spoils.
 */
class CainAssetsTest {

    private static final String PREFIX = "animation.cain.";

    @Test
    void oneDetailedRigWithEveryContractBone() throws IOException {
        Set<String> b = CovenArt.bones(CainAssets.GEO);
        assertTrue(b.size() >= CainAssets.MIN_BONES, "Cain has only " + b.size() + " bones");
        for (String bone : CainAssets.BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        for (String bone : CainAssets.PROCEDURAL) assertTrue(b.contains(bone), "missing renderer bone " + bone);
        assertEquals(List.of(CainAssets.MARK, CainAssets.FIRST_BLADE, CainAssets.EYES), CainAssets.PROCEDURAL);
        CovenArt.assertAtlas(CainAssets.GEO, CainAssets.TEXTURE);
        CovenArt.assertAtlas(CainAssets.GEO, CainAssets.GLOW);
    }

    @Test
    void everyClipExistsLoopsOrPlaysOnceAndFitsItsTiming() throws IOException {
        JsonObject a = CovenArt.anims(CainAssets.ANIM);
        Set<String> all = new HashSet<>(CainAssets.CLIPS);
        all.addAll(CainAssets.NPC_CLIPS);
        Set<String> loops = new HashSet<>(CainAssets.LOOPS);
        loops.addAll(CainAssets.NPC_CLIPS);
        for (String clip : all) {
            String name = PREFIX + clip;
            assertTrue(a.has(name), "missing clip " + name);
            boolean loop = CovenArt.loops(a.getAsJsonObject(name));
            assertEquals(loops.contains(clip), loop, name + (loop ? " must not loop" : " must loop"));
        }
        assertTrue(CainAssets.NPC_CLIPS.containsAll(List.of("tend", "talk")), "the beekeeper's own loops");
        assertEquals(160, CovenArt.ticks(a.getAsJsonObject(PREFIX + "tend")), "tend: 160 ticks among his hives");
        assertEquals(80, CovenArt.ticks(a.getAsJsonObject(PREFIX + "talk")), "talk: 80 ticks");
        assertEquals("hold_on_last_frame", CovenArt.loopMode(a.getAsJsonObject(PREFIX + "death")), "he holds his kneel");
        for (var e : CainAssets.CLIP_TICKS.entrySet()) {
            assertEquals((int) e.getValue(), CovenArt.ticks(a.getAsJsonObject(PREFIX + e.getKey())), e.getKey() + " length");
        }
        for (var e : CainAssets.HIT_TICKS.entrySet()) {
            assertTrue(e.getValue() > 0 && e.getValue() <= CainAssets.CLIP_TICKS.get(e.getKey()), e.getKey() + " lands after it ends");
        }
        assertTrue(CainAssets.CLIP_TICKS.keySet().containsAll(CainAssets.TRIGGERED), "every triggered clip has its length");
        assertTrue(CainAssets.HIT_TICKS.keySet().containsAll(CainAssets.TRIGGERED), "every triggered clip has its blow");
        assertFalse(CainAssets.TRIGGERED.contains("idle") || CainAssets.TRIGGERED.contains("walk"));
    }

    @Test
    void clipsKeyOnlyRealBonesAndNeverTheOnesTheRendererDrives() throws IOException {
        Set<String> b = CovenArt.bones(CainAssets.GEO);
        JsonObject a = CovenArt.anims(CainAssets.ANIM);
        assertFalse(a.entrySet().isEmpty());
        for (var clip : a.entrySet()) {
            assertTrue(clip.getKey().startsWith(PREFIX), "a clip named outside his prefix: " + clip.getKey());
            for (String bone : CovenArt.keyed(clip.getValue().getAsJsonObject())) {
                assertTrue(b.contains(bone), clip.getKey() + " keys an unknown bone " + bone);
                assertFalse(CainAssets.PROCEDURAL.contains(bone), clip.getKey() + " keys " + bone + ", which only the renderer may touch");
            }
        }
    }

    @Test
    void hisSoundsCinematicsAndSpoils() throws IOException {
        for (String s : CainAssets.SOUNDS) {
            assertTrue(CovenArt.soundDefined("cain." + s), "no sound cain." + s);
            assertTrue(Files.exists(CovenArt.ASSETS.resolve("sounds/cain/" + s + ".ogg")), "no file sounds/cain/" + s + ".ogg");
        }
        for (String c : CainAssets.CINEMATICS) {
            JsonObject cin = CovenArt.read("cinematics/" + c + ".json");
            assertTrue(cin.has("shots") && !cin.getAsJsonArray("shots").isEmpty(), c + " has no shots");
        }
        for (String icon : List.of("textures/item/cains_honey.png", "textures/item/cains_address.png")) {
            int[] s = CovenArt.pngSize(icon);
            assertEquals(16, s[0], icon);
            assertEquals(16, s[1], icon);
        }
    }
}
