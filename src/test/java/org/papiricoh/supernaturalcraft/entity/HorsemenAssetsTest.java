package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

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
 * The Four Horsemen's art (tools/artgen steed_art, war_art, famine_art, pestilence_art, death_art, reaper_art,
 * horsemen_items_art; tools/soundgen horsemen_sfx) against the art/code contract: every model, texture and clip the code
 * loads, the groups it shows and hides (never keyed by a clip), and the files the datagen points at. Pure file checks:
 * no registry or entity class is loaded.
 */
class HorsemenAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");
    private static final List<String> HORSEMEN = List.of("war", "famine", "pestilence", "death");
    private static final List<String> COMMON = List.of("idle", "walk", "intro", "transition", "death", "mount", "mounted_idle",
            "mounted_gallop", "mounted_charge");
    private static final List<String> LOOPS = List.of("idle", "walk", "mounted_idle", "mounted_gallop", "wheel_idle", "drain");
    private static final Map<String, List<String>> OWN = Map.of(
            "war", List.of("sword_combo", "sword_heavy", "parried", "rage_roar", "illusion_cast", "mounted_sweep"),
            "famine", List.of("wheel_idle", "stand_up", "devour", "grab", "drain", "mounted_grab"),
            "pestilence", List.of("cough", "cough_cone", "swarm_call", "cloud_cast", "mounted_spray"),
            "death", List.of("cane_strike", "scythe_reap", "scythe_throw", "summon_reapers", "shadow_step", "world_flip", "mounted_reap"),
            "reaper", List.of("idle", "walk", "attack"),
            "horseman_steed", List.of("idle", "walk", "gallop", "rear"),
            "war_standard", List.of("idle"));
    /** Groups the code shows and hides: whole groups that no clip may key. */
    private static final List<String> TOGGLED = List.of("steed", "wheelchair", "cane", "scythe", "saddle", "sword");

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

    private static List<String> clips(String model) {
        List<String> out = new ArrayList<>();
        if (HORSEMEN.contains(model)) out.addAll(COMMON);
        out.addAll(OWN.get(model));
        return out;
    }

    private static String parent(Map<String, JsonObject> bones, String bone) {
        JsonElement p = bones.get(bone).get("parent");
        return p == null ? null : p.getAsString();
    }

    @Test
    void everyContractClipExists() throws IOException {
        for (String model : OWN.keySet()) {
            JsonObject anims = anims(model);
            for (String clip : clips(model)) {
                String name = "animation." + model + "." + clip;
                assertTrue(anims.has(name), model + " is missing " + name);
                JsonElement loop = anims.getAsJsonObject(name).get("loop");
                boolean loops = loop != null && loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isBoolean() && loop.getAsBoolean();
                boolean shouldLoop = LOOPS.contains(clip) || model.equals("reaper") && !clip.equals("attack")
                        || model.equals("horseman_steed") && !clip.equals("rear") || model.equals("war_standard");
                assertEquals(shouldLoop, loops, name + (shouldLoop ? " must loop" : " must play once"));
            }
        }
    }

    @Test
    void clipsTouchOnlyRealBonesAndNeverTheToggledGroups() throws IOException {
        for (String model : OWN.keySet()) {
            Map<String, JsonObject> bones = bones(model);
            JsonObject anims = anims(model);
            for (String clip : anims.keySet()) {
                for (String bone : anims.getAsJsonObject(clip).getAsJsonObject("bones").keySet()) {
                    assertTrue(bones.containsKey(bone), clip + " animates unknown bone " + bone);
                    assertFalse(TOGGLED.contains(bone), clip + " keys " + bone + ", which the code shows and hides");
                }
            }
        }
    }

    @Test
    void contractBonesExist() throws IOException {
        for (String h : HORSEMEN) {
            Map<String, JsonObject> b = bones(h);
            for (String bone : List.of("root", "rider", "body", "head", "right_hand", "left_hand", "steed", "steed_body", "saddle")) {
                assertTrue(b.containsKey(bone), h + " is missing bone " + bone);
            }
            assertEquals("root", parent(b, "steed"), h + ": the steed must be a whole group under root");
            assertEquals("root", parent(b, "rider"), h + ": the rider must be a whole group under root");
        }
        assertTrue(bones("war").containsKey("sword"));
        Map<String, JsonObject> famine = bones("famine");
        assertEquals("root", parent(famine, "wheelchair"), "the wheelchair must be a whole group under root");
        assertTrue(famine.containsKey("mouth"));
        Map<String, JsonObject> death = bones("death");
        assertTrue(death.containsKey("cane") && death.containsKey("scythe") && death.containsKey("scythe_spin"));
        Map<String, JsonObject> steed = bones("horseman_steed");
        assertTrue(steed.containsKey("saddle") && steed.containsKey("steed_head"));
        assertTrue(bones("reaper").containsKey("head"));
        assertTrue(bones("war_standard").containsKey("banner"));
    }

    @Test
    void texturesExist() {
        List<String> textures = new ArrayList<>();
        for (String h : HORSEMEN) {
            textures.add(h);
            textures.add(h + "_glowmask");
            textures.add("horseman_steed_" + h);
            textures.add("horseman_steed_" + h + "_glowmask");
        }
        textures.addAll(List.of("reaper", "reaper_glowmask", "war_standard", "war_standard_glowmask"));
        for (String t : textures) assertTrue(Files.exists(ASSETS.resolve("textures/entity/" + t + ".png")), "missing texture " + t);
        assertTrue(Files.exists(ASSETS.resolve("textures/item/antidote_vial.png")));
        assertTrue(Files.exists(ASSETS.resolve("textures/mob_effect/plague.png")));
        for (String p : List.of("fly", "plague_spore", "soul_wisp")) {
            for (int i = 0; i < 4; i++) {
                assertTrue(Files.exists(ASSETS.resolve("textures/particle/" + p + "_" + i + ".png")), "missing particle " + p + "_" + i);
            }
        }
    }

    @Test
    void trophyModelsPointAtRealTextures() throws IOException {
        for (String h : HORSEMEN) {
            JsonObject model = read("models/block/" + h + "_trophy.json");
            for (var e : model.getAsJsonObject("textures").entrySet()) {
                String ref = e.getValue().getAsString();
                String path = ref.substring(ref.indexOf(':') + 1);
                assertTrue(Files.exists(ASSETS.resolve("textures/" + path + ".png")), h + "_trophy: missing texture " + ref);
            }
        }
    }

    @Test
    void soundsExist() {
        List<String> sounds = new ArrayList<>(List.of("war_death", "war_rage", "war_parry", "famine_death", "famine_devour", "famine_hunger",
                "pestilence_death", "pestilence_hurt_1", "death_death", "death_reap", "death_limbo_bell", "death_world_flip",
                "reaper_attack", "fly_buzz"));
        for (int i = 1; i <= 3; i++) {
            sounds.add("war_clash_" + i);
            sounds.add("pestilence_cough_" + i);
        }
        for (int i = 1; i <= 2; i++) {
            for (String h : HORSEMEN) sounds.add(h + "_ambient_" + i);
            for (String h : List.of("war", "famine", "death")) sounds.add(h + "_hurt_" + i);
            for (String s : List.of("death_tick_", "reaper_whisper_", "steed_neigh_", "steed_gallop_")) sounds.add(s + i);
        }
        for (String s : sounds) assertTrue(Files.exists(ASSETS.resolve("sounds/horsemen/" + s + ".ogg")), "missing sound " + s);
    }
}
