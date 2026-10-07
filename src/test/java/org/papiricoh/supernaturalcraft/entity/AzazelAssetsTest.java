package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.azazel.AzazelAnimations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Azazel's Java-side names against his generated GeckoLib files, and the art his blocks and items need. */
class AzazelAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    @Test
    void everyAnimationJavaUsesExists() throws IOException {
        JsonObject anims = read("animations/entity/azazel.animation.json").getAsJsonObject("animations");
        for (String name : AzazelAnimations.TRIGGERED) assertTrue(anims.has("animation.azazel." + name), "missing " + name);
        for (String name : AzazelAnimations.LOOPS) assertTrue(anims.has("animation.azazel." + name), "missing loop " + name);
    }

    @Test
    void animationsOnlyTouchBonesThatExist() throws IOException {
        Set<String> bones = new HashSet<>();
        read("geo/entity/azazel.geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").forEach(b -> bones.add(b.getAsJsonObject().get("name").getAsString()));
        JsonObject anims = read("animations/entity/azazel.animation.json").getAsJsonObject("animations");
        for (String anim : anims.keySet()) {
            for (String bone : anims.getAsJsonObject(anim).getAsJsonObject("bones").keySet()) {
                assertTrue(bones.contains(bone), anim + " animates unknown bone " + bone);
            }
        }
    }

    @Test
    void texturesExist() {
        for (int p = 1; p <= 2; p++) {
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/azazel_p" + p + ".png")));
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/azazel_p" + p + "_glowmask.png")));
        }
        for (String kind : new String[]{"straight", "diagonal", "cross"}) {
            assertTrue(Files.exists(ASSETS.resolve("textures/block/colt_rail_" + kind + ".png")), kind);
            assertTrue(Files.exists(ASSETS.resolve("textures/block/colt_rail_" + kind + "_charged.png")), kind);
        }
        assertTrue(Files.exists(ASSETS.resolve("models/block/azazel_trophy.json")));
        assertTrue(Files.exists(ASSETS.resolve("textures/item/azazel_blood.png")));
        for (int i = 0; i < 4; i++) assertTrue(Files.exists(ASSETS.resolve("textures/particle/yellow_smoke_" + i + ".png")));
        for (String c : new String[]{"intro", "p2", "death"}) assertTrue(Files.exists(ASSETS.resolve("cinematics/azazel_" + c + ".json")), c);
    }
}
