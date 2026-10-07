package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckAnimations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Dean, Sam and Castiel: three models on one bone set, the shared animation file, and their skins. */
class AlliesAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");
    private static final String[] ALLIES = {"dean", "sam", "castiel"};

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    private static Set<String> bones(String who) throws IOException {
        Set<String> bones = new LinkedHashSet<>();
        read("geo/entity/hunter_" + who + ".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").forEach(b -> bones.add(b.getAsJsonObject().get("name").getAsString()));
        return bones;
    }

    @Test
    void everyAllySharesOneBoneSet() throws IOException {
        Set<String> dean = bones("dean");
        assertTrue(dean.containsAll(Set.of("root", "body", "head", "right_arm", "left_arm", "right_leg", "left_leg")), dean.toString());
        for (String who : ALLIES) assertEquals(dean, bones(who), who + " has a different bone set");
    }

    @Test
    void everyAnimationJavaUsesExists() throws IOException {
        JsonObject anims = read("animations/entity/hunter_ally.animation.json").getAsJsonObject("animations");
        assertTrue(anims.has(ChuckAnimations.ALLY + "idle"), "missing loop idle");
        for (String name : ChuckAnimations.ALLY_TRIGGERED) assertTrue(anims.has(ChuckAnimations.ALLY + name), "missing " + name);
    }

    @Test
    void animationsOnlyTouchBonesThatExist() throws IOException {
        Set<String> bones = bones("dean");
        JsonObject anims = read("animations/entity/hunter_ally.animation.json").getAsJsonObject("animations");
        assertFalse(anims.keySet().isEmpty());
        for (String anim : anims.keySet()) {
            for (String bone : anims.getAsJsonObject(anim).getAsJsonObject("bones").keySet()) {
                assertTrue(bones.contains(bone), anim + " animates unknown bone " + bone);
            }
        }
    }

    @Test
    void texturesExist() {
        for (String who : ALLIES) assertTrue(Files.exists(ASSETS.resolve("textures/entity/hunter_" + who + ".png")), who);
    }
}
