package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAnimations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Java side names animations and bones; the generated GeckoLib files must contain them. */
class LuciferAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    private static Set<String> bones() throws IOException {
        Set<String> names = new HashSet<>();
        read("geo/entity/lucifer.geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").forEach(b -> names.add(b.getAsJsonObject().get("name").getAsString()));
        return names;
    }

    @Test
    void everyAnimationJavaUsesExists() throws IOException {
        JsonObject anims = read("animations/entity/lucifer.animation.json").getAsJsonObject("animations");
        for (String name : LuciferAnimations.TRIGGERED) {
            assertTrue(anims.has("animation.lucifer." + name), "missing triggered animation " + name);
        }
        for (String name : LuciferAnimations.LOOPS) {
            assertTrue(anims.has("animation.lucifer." + name), "missing loop " + name);
        }
    }

    @Test
    void animationsOnlyTouchBonesThatExist() throws IOException {
        Set<String> bones = bones();
        JsonObject anims = read("animations/entity/lucifer.animation.json").getAsJsonObject("animations");
        for (String anim : anims.keySet()) {
            for (String bone : anims.getAsJsonObject(anim).getAsJsonObject("bones").keySet()) {
                assertTrue(bones.contains(bone), anim + " animates unknown bone " + bone);
            }
        }
    }

    @Test
    void rendererBonesExist() throws IOException {
        Set<String> bones = bones();
        for (String b : List.of("head", "halo", "wing_r1", "wing_l1", "wing_r2", "wing_l2", "wing_r3", "wing_l3")) {
            assertTrue(bones.contains(b), "renderer toggles missing bone " + b);
        }
    }

    @Test
    void everyPhaseHasATextureAndGlowmask() {
        for (int p = 1; p <= 4; p++) {
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/lucifer_p" + p + ".png")), "missing texture p" + p);
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/lucifer_p" + p + "_glowmask.png")), "missing glowmask p" + p);
        }
    }
}
