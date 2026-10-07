package org.papiricoh.supernaturalcraft.reward;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.reward.colt.ColtAnimations;
import org.papiricoh.supernaturalcraft.reward.colt.ColtReload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** colt_art.py and the Java that drives the Colt must agree on bones, clips and timings. */
class ColtAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    private static Set<String> bones() throws IOException {
        Set<String> out = new HashSet<>();
        read("geo/item/the_colt.geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").forEach(b -> out.add(b.getAsJsonObject().get("name").getAsString()));
        return out;
    }

    private static JsonObject anims() throws IOException {
        return read("animations/item/the_colt.animation.json").getAsJsonObject("animations");
    }

    @Test
    void everyBoneTheJavaPosesExists() throws IOException {
        Set<String> bones = bones();
        for (String b : new String[]{"root", "spin", "cylinder", "hammer", "trigger", "loading_lever", "muzzle_flash", "hand_r",
                "hand_l", "loose_round", "engraving"}) {
            assertTrue(bones.contains(b), "missing bone " + b);
        }
        for (int j = 0; j < ColtItem.CAPACITY; j++) {
            assertTrue(bones.contains("chamber_" + j) && bones.contains("round_" + j), "missing chamber " + j);
        }
    }

    @Test
    void everyClipTheItemTriggersExists() throws IOException {
        JsonObject anims = anims();
        assertNotNull(anims.get(ColtAnimations.PREFIX + ColtAnimations.IDLE_COCKED));
        assertNotNull(anims.get(ColtAnimations.PREFIX + ColtAnimations.IDLE_UNCOCKED));
        for (String name : ColtAnimations.triggerables(ColtItem.CAPACITY)) {
            assertNotNull(anims.get(ColtAnimations.PREFIX + name), "missing animation " + name);
        }
    }

    @Test
    void reloadClipsLastAsLongAsTheReload() throws IOException {
        JsonObject anims = anims();
        for (int n = 1; n <= ColtItem.CAPACITY; n++) {
            double len = anims.getAsJsonObject(ColtAnimations.PREFIX + ColtAnimations.reload(n)).get("animation_length").getAsDouble();
            assertEquals(ColtReload.total(n) / 20.0, len, 1e-3, "reload_" + n);
        }
    }

    @Test
    void noClipKeysAProceduralBone() throws IOException {
        JsonObject anims = anims();
        for (String name : anims.keySet()) {
            for (String bone : anims.getAsJsonObject(name).getAsJsonObject("bones").keySet()) {
                for (String p : ColtAnimations.PROCEDURAL) {
                    assertFalse(p.endsWith("_") ? bone.startsWith(p) : bone.equals(p), name + " keys the procedural bone " + bone);
                }
            }
        }
    }

    @Test
    void playerAnimationsUseOnlyTheLibrarysBones() throws IOException {
        Set<String> allowed = Set.of("head", "body", "torso", "right_arm", "left_arm", "right_leg", "left_leg");
        JsonObject anims = read("player_animations/colt.json").getAsJsonObject("animations");
        for (String name : new String[]{"colt_dry", "colt_inspect"}) assertNotNull(anims.get(name), name);
        for (int n = 1; n <= ColtItem.CAPACITY; n++) assertNotNull(anims.get("colt_reload_" + n), "colt_reload_" + n);
        for (String name : anims.keySet()) {
            for (String bone : anims.getAsJsonObject(name).getAsJsonObject("bones").keySet()) {
                assertTrue(allowed.contains(bone), name + " animates " + bone + ", which PlayerAnimationLib does not have");
            }
        }
    }
}
