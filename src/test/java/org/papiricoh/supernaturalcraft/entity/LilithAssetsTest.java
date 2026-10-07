package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithAnimations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lilith's Java-side names against his generated GeckoLib files, and the art his blocks and items need. */
class LilithAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    @Test
    void everyAnimationJavaUsesExists() throws IOException {
        JsonObject anims = read("animations/entity/lilith.animation.json").getAsJsonObject("animations");
        for (String name : LilithAnimations.TRIGGERED) assertTrue(anims.has("animation.lilith." + name), "missing " + name);
        for (String name : LilithAnimations.LOOPS) assertTrue(anims.has("animation.lilith." + name), "missing loop " + name);
    }

    @Test
    void animationsOnlyTouchBonesThatExist() throws IOException {
        Set<String> bones = new HashSet<>();
        read("geo/entity/lilith.geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").forEach(b -> bones.add(b.getAsJsonObject().get("name").getAsString()));
        JsonObject anims = read("animations/entity/lilith.animation.json").getAsJsonObject("animations");
        for (String anim : anims.keySet()) {
            for (String bone : anims.getAsJsonObject(anim).getAsJsonObject("bones").keySet()) {
                assertTrue(bones.contains(bone), anim + " animates unknown bone " + bone);
            }
        }
    }

    @Test
    void texturesExist() {
        for (int p = 1; p <= 3; p++) {
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/lilith_p" + p + ".png")));
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/lilith_p" + p + "_glowmask.png")));
        }
        for (int c = 0; c <= 2; c++) {
            for (String half : new String[]{"lower", "upper"}) {
                assertTrue(Files.exists(ASSETS.resolve("models/block/cracked_headstone_" + half + "_" + c + ".json")), half + c);
            }
        }
        assertTrue(Files.exists(ASSETS.resolve("models/block/lilith_trophy.json")));
        for (String item : new String[]{"last_seal", "hound_whistle"}) assertTrue(Files.exists(ASSETS.resolve("textures/item/" + item + ".png")), item);
        for (int i = 0; i < 4; i++) assertTrue(Files.exists(ASSETS.resolve("textures/particle/white_light_" + i + ".png")));
        for (String c : new String[]{"intro", "p2", "p3", "death"}) assertTrue(Files.exists(ASSETS.resolve("cinematics/lilith_" + c + ".json")), c);
    }
}
