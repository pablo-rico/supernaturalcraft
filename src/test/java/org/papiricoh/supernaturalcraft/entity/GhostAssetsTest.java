package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostAnimations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ghost's and the graves' Java-side names against their generated art. */
class GhostAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    private static Set<String> bones() throws IOException {
        Set<String> names = new HashSet<>();
        read("geo/entity/ghost.geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").forEach(b -> names.add(b.getAsJsonObject().get("name").getAsString()));
        return names;
    }

    @Test
    void everyClipJavaPlaysExists() throws IOException {
        JsonObject anims = read("animations/entity/ghost.animation.json").getAsJsonObject("animations");
        for (String name : GhostAnimations.LOOPS) assertTrue(anims.has(GhostAnimations.clip(name)), "missing loop " + name);
        for (String name : GhostAnimations.TRIGGERED) assertTrue(anims.has(GhostAnimations.clip(name)), "missing " + name);
    }

    @Test
    void theRigHasTheBonesJavaNeeds() throws IOException {
        Set<String> bones = bones();
        for (String b : GhostAnimations.BONES) assertTrue(bones.contains(b), "missing bone " + b);
    }

    @Test
    void animationsOnlyTouchBonesThatExist() throws IOException {
        Set<String> bones = bones();
        JsonObject anims = read("animations/entity/ghost.animation.json").getAsJsonObject("animations");
        for (String anim : anims.keySet()) {
            JsonObject a = anims.getAsJsonObject(anim);
            if (!a.has("bones")) continue;
            for (String bone : a.getAsJsonObject("bones").keySet()) assertTrue(bones.contains(bone), anim + " animates unknown bone " + bone);
        }
    }

    @Test
    void texturesAndBlockModelsExist() {
        for (String t : new String[]{"textures/entity/ghost.png", "textures/entity/ghost_glowmask.png",
                "textures/block/grave_soil.png", "textures/block/grave_soil_side.png",
                "models/block/grave_bones.json", "models/block/grave_bones_salted.json", "models/block/grave_bones_rested.json",
                "models/block/grave_headstone.json", "textures/item/ectoplasm.png", "textures/item/grave_dirt.png"}) {
            assertTrue(Files.exists(ASSETS.resolve(t)), "missing " + t);
        }
    }
}
