package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAnimations;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the Java side of the Darkness names must exist in her generated GeckoLib files. */
class AmaraAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    private static Set<String> bones() throws IOException {
        Set<String> names = new HashSet<>();
        read("geo/entity/amara.geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").forEach(b -> names.add(b.getAsJsonObject().get("name").getAsString()));
        return names;
    }

    @Test
    void everyAnimationJavaUsesExists() throws IOException {
        JsonObject anims = read("animations/entity/amara.animation.json").getAsJsonObject("animations");
        List<String> wanted = new ArrayList<>(AmaraAnimations.TRIGGERED);
        wanted.addAll(List.of("idle", "idle_open", "form_idle"));
        for (String name : wanted) assertTrue(anims.has("animation.amara." + name), "missing animation " + name);
    }

    @Test
    void animationsOnlyTouchBonesThatExist() throws IOException {
        Set<String> bones = bones();
        JsonObject anims = read("animations/entity/amara.animation.json").getAsJsonObject("animations");
        for (String anim : anims.keySet()) {
            for (String bone : anims.getAsJsonObject(anim).getAsJsonObject("bones").keySet()) {
                assertTrue(bones.contains(bone), anim + " animates unknown bone " + bone);
            }
        }
    }

    @Test
    void rendererBonesExist() throws IOException {
        Set<String> bones = bones();
        List<String> wanted = new ArrayList<>(List.of("mass", "form", "core"));
        for (int i = 0; i < AmaraEntity.ANCHORS; i++) wanted.add("ring_" + i);
        for (int i = 0; i < AmaraEntity.TENTACLES; i++) {
            wanted.add("tentacle_" + i);
            wanted.add("cyst_" + i);
        }
        for (String b : wanted) assertTrue(bones.contains(b), "renderer moves or toggles missing bone " + b);
    }
}
