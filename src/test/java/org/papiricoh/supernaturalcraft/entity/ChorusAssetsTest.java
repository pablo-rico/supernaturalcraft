package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusAnimations;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusGeometry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Broken Chorus's model and its hit boxes come from two places (chorus_art.py and
 * ChorusGeometry); here they must agree, or the boss would be struck where it is not drawn.
 */
class ChorusAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");
    /** Bones the renderer drives each frame: no animation may key them. */
    private static final Set<String> PROCEDURAL_PREFIXES = Set.of("body_turn", "wheel_", "heads", "face_");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    private static Map<String, JsonObject> bones() throws IOException {
        Map<String, JsonObject> out = new HashMap<>();
        read("geo/entity/broken_chorus.geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").forEach(b -> out.put(b.getAsJsonObject().get("name").getAsString(), b.getAsJsonObject()));
        return out;
    }

    private static float[] vec(JsonObject o, String key) {
        JsonArray a = o.getAsJsonArray(key);
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    @Test
    void eyesSitWhereTheHitBoxesThinkTheyDo() throws IOException {
        Map<String, JsonObject> bones = bones();
        for (int i = 0; i < ChorusGeometry.EYES; i++) {
            JsonObject eye = bones.get("eye_" + i);
            assertNotNull(eye, "missing eye_" + i);
            int w = ChorusGeometry.wheelOf(i);
            assertEquals("wheel_" + ChorusGeometry.WHEEL_NAMES[w], eye.get("parent").getAsString());
            assertEquals(Math.toDegrees(ChorusGeometry.eyeTheta(i)), vec(eye, "rotation")[0], 1e-3, "eye_" + i + " angle");
            assertEquals(ChorusGeometry.CORE_Y, vec(eye, "pivot")[1], 1e-4);
            // The eyeball's centre: EYE_OUT outside the wheel's rim.
            JsonObject ball = bones.get("eye_" + i + "_ball").getAsJsonArray("cubes").get(0).getAsJsonObject();
            float[] o = vec(ball, "origin"), s = vec(ball, "size");
            assertEquals(ChorusGeometry.CORE_Y + ChorusGeometry.WHEEL_RADIUS[w] + ChorusGeometry.EYE_OUT, o[1] + s[1] / 2, 1e-3,
                    "eye_" + i + " height on its wheel");
            assertEquals(0, o[0] + s[0] / 2, 1e-3);
            assertEquals(0, o[2] + s[2] / 2, 1e-3);
        }
        for (String w : ChorusGeometry.WHEEL_NAMES) {
            assertEquals(ChorusGeometry.CORE_Y, vec(bones.get("wheel_" + w), "pivot")[1], 1e-4);
            assertFalse(bones.get("wheel_" + w).has("rotation"), "wheel_" + w + " must not have a rest rotation");
        }
    }

    @Test
    void facesAndWingsMatchTheGeometry() throws IOException {
        Map<String, JsonObject> bones = bones();
        for (int i = 0; i < ChorusGeometry.FACES; i++) {
            String n = ChorusGeometry.FACE_NAMES[i];
            float[] pivot = vec(bones.get("face_" + n + "_intact"), "pivot");
            for (int k = 0; k < 3; k++) assertEquals(ChorusGeometry.FACE_CENTRES[i][k], pivot[k], 1e-4, "face " + n);
        }
        for (int i = 0; i < ChorusGeometry.WINGS; i++) {
            JsonObject wing = bones.get("wing_" + ChorusGeometry.WING_NAMES[i]);
            assertNotNull(wing, "missing wing " + ChorusGeometry.WING_NAMES[i]);
            float[] pivot = vec(wing, "pivot");
            for (int k = 0; k < 3; k++) assertEquals(ChorusGeometry.SHOULDERS[i][k], pivot[k], 1e-4, "shoulder " + i);
            assertFalse(wing.has("rotation"), "wing roots are posed in code, not in the model");
        }
    }

    @Test
    void animationsLeaveTheDrivenBonesAlone() throws IOException {
        Map<String, JsonObject> bones = bones();
        JsonObject anims = read("animations/entity/broken_chorus.animation.json").getAsJsonObject("animations");
        for (String name : ChorusAnimations.TRIGGERED) assertTrue(anims.has("animation.broken_chorus." + name), "missing " + name);
        for (String name : List.of("idle", "final_idle")) assertTrue(anims.has("animation.broken_chorus." + name), "missing " + name);
        for (String anim : anims.keySet()) {
            for (String bone : anims.getAsJsonObject(anim).getAsJsonObject("bones").keySet()) {
                assertTrue(bones.containsKey(bone), anim + " animates unknown bone " + bone);
                boolean driven = PROCEDURAL_PREFIXES.stream().anyMatch(bone::startsWith) && !bone.endsWith("_jaw")
                        || bone.matches("eye_\\d+(_lid_[ab])?") || bone.matches("wing_(top|mid|low)_[lr]");
                assertFalse(driven, anim + " keys " + bone + ", which the renderer drives");
            }
        }
    }

    @Test
    void rendererBonesExist() throws IOException {
        Map<String, JsonObject> bones = bones();
        for (String b : List.of("wheels", "fragments", "core_shell", "core_light", "heads", "torso", "halo")) {
            assertTrue(bones.containsKey(b), "missing " + b);
        }
        for (String w : ChorusGeometry.WING_NAMES) {
            for (String suffix : List.of("", "_stump", "_fb0", "_fb1", "_fb2")) assertTrue(bones.containsKey("wing_" + w + suffix));
        }
        for (int i = 0; i < ChorusGeometry.EYES; i++) {
            for (String suffix : List.of("_ball", "_lid_a", "_lid_b", "_socket")) assertTrue(bones.containsKey("eye_" + i + suffix));
        }
    }
}
