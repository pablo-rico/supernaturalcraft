package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckAnimations;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckBones;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Author's art (tools/artgen/chuck_art.py, chuck_divine_art.py, author_hand_art.py, typewriter_key_art.py)
 * against the code's contracts: every clip ChuckAnimations names, every bone ChuckBones names, nothing keyed that
 * the renderer drives.
 */
class ChuckAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    private static Map<String, JsonObject> bones(String model) throws IOException {
        Map<String, JsonObject> out = new HashMap<>();
        read("geo/entity/" + model + ".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").forEach(b -> out.put(b.getAsJsonObject().get("name").getAsString(), b.getAsJsonObject()));
        return out;
    }

    private static JsonObject anims(String model) throws IOException {
        return read("animations/entity/" + model + ".animation.json").getAsJsonObject("animations");
    }

    private static List<String> with(List<String> a, String... more) {
        List<String> out = new ArrayList<>(a);
        out.addAll(List.of(more));
        return out;
    }

    private static void clipsExist(String model, String prefix, List<String> names) throws IOException {
        JsonObject anims = anims(model);
        for (String n : names) assertTrue(anims.has(prefix + n), model + " is missing " + prefix + n);
    }

    private static void clipsTouchOnlyRealBones(String model) throws IOException {
        Map<String, JsonObject> bones = bones(model);
        JsonObject anims = anims(model);
        for (String clip : anims.keySet()) {
            for (String bone : anims.getAsJsonObject(clip).getAsJsonObject("bones").keySet()) {
                assertTrue(bones.containsKey(bone), clip + " animates unknown bone " + bone);
            }
        }
    }

    @Test
    void everyClipExists() throws IOException {
        List<String> human = with(ChuckAnimations.HUMAN_LOOPS);
        human.addAll(ChuckAnimations.HUMAN_TRIGGERED);
        clipsExist("chuck", ChuckAnimations.HUMAN, human);
        List<String> divine = with(ChuckAnimations.DIVINE_LOOPS);
        divine.addAll(ChuckAnimations.DIVINE_TRIGGERED);
        clipsExist("chuck_divine", ChuckAnimations.DIVINE, divine);
        clipsExist("author_hand", ChuckAnimations.HAND, with(ChuckAnimations.HAND_TRIGGERED, "idle"));
    }

    @Test
    void clipsOnlyTouchBonesThatExist() throws IOException {
        for (String model : List.of("chuck", "chuck_divine", "author_hand")) clipsTouchOnlyRealBones(model);
    }

    @Test
    void noClipKeysWhatTheRendererDrives() throws IOException {
        JsonObject anims = anims("chuck_divine");
        for (String clip : anims.keySet()) {
            JsonObject keyed = anims.getAsJsonObject(clip).getAsJsonObject("bones");
            for (String bone : ChuckBones.PROCEDURAL) assertFalse(keyed.has(bone), clip + " keys " + bone + ", which the renderer drives");
            // The rings and core hang from `orbit`: turning or moving it would carry the weak points off their hit boxes.
            if (keyed.has("orbit")) {
                JsonObject orbit = keyed.getAsJsonObject("orbit");
                assertFalse(orbit.has("rotation") || orbit.has("position"), clip + " may only scale orbit");
            }
        }
    }

    @Test
    void contractBonesExist() throws IOException {
        Map<String, JsonObject> man = bones("chuck");
        for (String b : List.of(ChuckBones.OUTFIT_ROBE, ChuckBones.OUTFIT_FLANNEL, ChuckBones.OUTFIT_SUIT)) {
            assertTrue(man.containsKey(b), "missing " + b);
            assertEquals("root", man.get(b).get("parent").getAsString(), b + " must be a whole group under root");
        }
        assertTrue(man.containsKey(ChuckBones.GLASS));
        assertTrue(man.containsKey(ChuckBones.HEAD));
        Map<String, JsonObject> light = bones("chuck_divine");
        for (String b : ChuckBones.PROCEDURAL) assertTrue(light.containsKey(b), "missing " + b);
        for (int k = 0; k < ChuckBones.KEYS; k++) {
            assertEquals(ChuckBones.HALO, light.get(ChuckBones.key(k)).get("parent").getAsString());
        }
        assertTrue(light.size() >= 120, "the light should be the most elaborate model of the mod: " + light.size() + " bones");
        assertFalse(bones("typewriter_key").isEmpty());
    }

    @Test
    void texturesExist() {
        for (String t : List.of("chuck", "chuck_cracks", "chuck_divine", "chuck_divine_glowmask", "chuck_divine_cracks",
                "author_hand", "author_hand_glowmask", "typewriter_key")) {
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/" + t + ".png")), "missing texture " + t);
        }
    }
}
