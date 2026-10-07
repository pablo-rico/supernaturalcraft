package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.client.render.LuciferUncagedRenderer;
import org.papiricoh.supernaturalcraft.entity.boss.uncaged.UncagedAnimations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lucifer Uncaged's and the hellhound's Java-side names against their generated GeckoLib files. */
class UncagedAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    private static Set<String> bones(String geo) throws IOException {
        Set<String> names = new HashSet<>();
        read("geo/entity/" + geo + ".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").forEach(b -> names.add(b.getAsJsonObject().get("name").getAsString()));
        return names;
    }

    @Test
    void everyAnimationJavaUsesExists() throws IOException {
        JsonObject anims = read("animations/entity/lucifer_uncaged.animation.json").getAsJsonObject("animations");
        for (String name : UncagedAnimations.TRIGGERED) assertTrue(anims.has("animation.lucifer_uncaged." + name), "missing " + name);
        for (String name : UncagedAnimations.LOOPS) assertTrue(anims.has("animation.lucifer_uncaged." + name), "missing loop " + name);
        JsonObject hound = read("animations/entity/hellhound.animation.json").getAsJsonObject("animations");
        for (String name : new String[]{"idle", "walk", "run", "bite"}) assertTrue(hound.has("animation.hellhound." + name), "hound " + name);
    }

    @Test
    void animationsOnlyTouchBonesThatExist() throws IOException {
        for (String model : new String[]{"lucifer_uncaged", "hellhound"}) {
            Set<String> bones = bones(model);
            JsonObject anims = read("animations/entity/" + model + ".animation.json").getAsJsonObject("animations");
            for (String anim : anims.keySet()) {
                for (String bone : anims.getAsJsonObject(anim).getAsJsonObject("bones").keySet()) {
                    assertTrue(bones.contains(bone), anim + " animates unknown bone " + bone);
                }
            }
        }
    }

    @Test
    void rendererBonesExist() throws IOException {
        Set<String> bones = bones("lucifer_uncaged");
        for (String b : LuciferUncagedRenderer.WINGS) assertTrue(bones.contains(b), b);
        for (String b : LuciferUncagedRenderer.CHAINS) assertTrue(bones.contains(b), b);
        assertTrue(bones.contains(LuciferUncagedRenderer.HALO));
    }

    @Test
    void texturesExist() {
        for (int p = 1; p <= 6; p++) {
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/lucifer_uncaged_p" + p + ".png")));
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/lucifer_uncaged_p" + p + "_glowmask.png")));
        }
        for (String t : new String[]{"hellhound", "hellhound_glowmask", "hellhound_haze"}) {
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/" + t + ".png")), t);
        }
        assertTrue(Files.exists(ASSETS.resolve("textures/block/hell_rift.png.mcmeta")));
    }
}
