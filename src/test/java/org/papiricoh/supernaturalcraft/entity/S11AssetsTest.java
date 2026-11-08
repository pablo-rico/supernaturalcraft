package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Season 11's art (v0.42) against {@link S11Assets}: the mobs' rigs and clips, Casifer, the fogged overlay, the Hands, the blocks. */
class S11AssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    @Test
    void everyMobHasItsRigClipsAndTextures() throws IOException {
        for (S11Assets.Rig rig : S11Assets.MOBS) {
            Set<String> bones = new HashSet<>();
            for (JsonElement b : read(rig.geo()).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
                bones.add(b.getAsJsonObject().get("name").getAsString());
            }
            JsonObject anims = read(rig.anim()).getAsJsonObject("animations");
            for (List<String> clips : List.of(rig.loops(), rig.triggered())) {
                for (String clip : clips) {
                    String name = "animation." + rig.rig() + "." + clip;
                    assertTrue(anims.has(name), rig.rig() + " lacks " + clip);
                    JsonObject a = anims.getAsJsonObject(name);
                    if (!a.has("bones")) continue;
                    for (Map.Entry<String, JsonElement> e : a.getAsJsonObject("bones").entrySet()) {
                        assertTrue(bones.contains(e.getKey()), rig.rig() + "." + clip + " animates unknown bone " + e.getKey());
                    }
                }
            }
            assertTrue(Files.exists(ASSETS.resolve(rig.texturePath())), "missing " + rig.texturePath());
            if (rig.glow()) assertTrue(Files.exists(ASSETS.resolve(rig.glowPath())), "missing " + rig.glowPath());
        }
    }

    @Test
    void casiferWearsCastielsLayout() throws IOException {
        Path cas = ASSETS.resolve(S11Assets.CASIFER_TEXTURE);
        assertTrue(Files.exists(cas) && Files.exists(ASSETS.resolve(S11Assets.CASIFER_GLOW)), "missing Casifer");
        assertArrayEquals(AmaraFormAssetsTest.pngSize(ASSETS.resolve("textures/entity/castiel.png")), AmaraFormAssetsTest.pngSize(cas),
                "Casifer must share Castiel's UV layout");
        assertArrayEquals(new int[]{64, 64}, AmaraFormAssetsTest.pngSize(ASSETS.resolve(S11Assets.FOGGED_VILLAGER_OVERLAY)));
    }

    private static boolean block(String id) {
        Path dir = ASSETS.resolve("textures/block");
        return Files.exists(dir.resolve(id + ".png")) || Files.exists(dir.resolve(id + "_side.png"));
    }

    @Test
    void theHandsAndTheirBlocksAreDrawn() {
        for (S11Assets.Hand h : S11Assets.Hand.values()) {
            assertTrue(Files.exists(ASSETS.resolve("textures/item/" + h.item + ".png")), "missing " + h.item);
        }
        assertTrue(block(S11Assets.HAND_ALTAR), "missing the altar");
        S11Assets.RELIQUARY_BLOCKS.values().forEach(ids -> ids.forEach(id -> assertTrue(block(id), "missing block " + id)));
        S11Assets.NEST_BLOCKS.forEach(id -> assertTrue(block(id), "missing block " + id));
    }
}
