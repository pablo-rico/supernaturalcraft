package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity;
import org.papiricoh.supernaturalcraft.entity.witch.WitchAssets;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The witches' art (tools/artgen witch_art, familiar_art, victims_art) against {@link WitchAssets} (v0.21 P3): every rig's model,
 * texture at its atlas size, glow mask and clips; the bones the code reads; loops loop and the rest play once; every action
 * lands within its clip; clips key only real bones and never the renderer's props; every sound is defined.
 */
class WitchAssetsTest {

    private static String prefix(String rig) {
        return "animation." + rig + ".";
    }

    @Test
    void everyRigHasItsFilesAtTheirAtlasSize() throws IOException {
        assertEquals(5, WitchAssets.RIGS.size());
        for (String rig : WitchAssets.RIGS) {
            CovenArt.assertAtlas(WitchAssets.geo(rig), WitchAssets.texture(rig));
            CovenArt.assertAtlas(WitchAssets.geo(rig), WitchAssets.glow(rig));
            CovenArt.anims(WitchAssets.anim(rig));
        }
        for (String v : WitchAssets.FAMILIAR_VARIANTS) assertTrue(WitchAssets.RIGS.contains(WitchAssets.familiarRig(v)), v);
    }

    @Test
    void everyBoneTheCodeReadsAndEveryPropIsThere() throws IOException {
        for (String rig : WitchAssets.RIGS) {
            Set<String> bones = CovenArt.bones(WitchAssets.geo(rig));
            for (String bone : WitchAssets.BONES.get(rig)) assertTrue(bones.contains(bone), rig + " is missing bone " + bone);
            String prop = WitchAssets.PROPS.get(rig);
            if (prop != null) assertTrue(bones.contains(prop), rig + " is missing its prop " + prop);
        }
    }

    @Test
    void everyClipExistsAndLoopsOrPlaysOnce() throws IOException {
        for (String rig : WitchAssets.RIGS) {
            JsonObject a = CovenArt.anims(WitchAssets.anim(rig));
            for (String clip : WitchAssets.CLIPS.get(rig)) {
                String name = prefix(rig) + clip;
                assertTrue(a.has(name), "missing clip " + name);
                boolean loops = CovenArt.loops(a.getAsJsonObject(name));
                assertEquals(WitchAssets.LOOPS.get(rig).contains(clip), loops, name + (loops ? " must not loop" : " must loop"));
            }
            assertTrue(WitchAssets.CLIPS.get(rig).containsAll(WitchAssets.LOOPS.get(rig)), rig + ": a loop that is no clip");
            for (String t : WitchAssets.triggered(rig)) assertFalse(WitchAssets.LOOPS.get(rig).contains(t));
        }
    }

    @Test
    void everyActionLandsBeforeItsClipEnds() throws IOException {
        for (String rig : WitchAssets.RIGS) {
            JsonObject a = CovenArt.anims(WitchAssets.anim(rig));
            for (Map.Entry<String, Integer> hit : WitchAssets.HIT_TICKS.entrySet()) {
                String name = prefix(rig) + hit.getKey();
                if (!a.has(name)) continue;
                int length = CovenArt.ticks(a.getAsJsonObject(name));
                assertTrue(hit.getValue() > 0 && hit.getValue() < length, name + " lands at " + hit.getValue() + " of " + length + " ticks");
            }
        }
        for (String clip : new String[]{"cast", "hex", "teleport"}) assertTrue(WitchAssets.HIT_TICKS.containsKey(clip), clip + " has its moment");
        assertTrue(WitchAssets.HIT_TICKS.containsKey("steal") && WitchAssets.HIT_TICKS.containsKey("attack"));
        // The freed villager stands up as itself no later than its clip ends.
        JsonObject victim = CovenArt.anims(WitchAssets.anim(WitchAssets.HEXED_VILLAGER));
        int freed = CovenArt.ticks(victim.getAsJsonObject(prefix(WitchAssets.HEXED_VILLAGER) + "freed"));
        assertTrue(HexedVillagerEntity.FREE_TICKS <= freed, "freed lasts " + freed + " ticks, the swap comes at " + HexedVillagerEntity.FREE_TICKS);
    }

    @Test
    void clipsKeyOnlyRealBonesAndNeverTheRenderersProps() throws IOException {
        for (String rig : WitchAssets.RIGS) {
            Set<String> bones = CovenArt.bones(WitchAssets.geo(rig));
            String prop = WitchAssets.PROPS.get(rig);
            JsonObject a = CovenArt.anims(WitchAssets.anim(rig));
            for (var clip : a.entrySet()) {
                assertTrue(clip.getKey().startsWith(prefix(rig)), rig + ": a clip of another rig " + clip.getKey());
                for (String bone : CovenArt.keyed(clip.getValue().getAsJsonObject())) {
                    assertTrue(bones.contains(bone), clip.getKey() + " keys an unknown bone " + bone);
                    assertFalse(bone.equals(prop), clip.getKey() + " keys " + bone + ", which only the renderer shows");
                }
            }
        }
    }

    @Test
    void everySoundIsDefined() throws IOException {
        for (String s : WitchAssets.SOUNDS) assertTrue(CovenArt.soundDefined("witch." + s), "no sound witch." + s);
    }
}
