package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraHudSheet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Darkness's v0.42 art against {@link AmaraAssets}: both rigs, every age, the HUD sheet, sprites and cinematics. */
class AmaraFormAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    private static List<String> bones(String geo) throws IOException {
        List<String> names = new ArrayList<>();
        for (JsonElement b : read(geo).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            names.add(b.getAsJsonObject().get("name").getAsString());
        }
        return names;
    }

    static int[] pngSize(Path p) throws IOException {
        try (InputStream in = Files.newInputStream(p)) {
            ByteBuffer b = ByteBuffer.wrap(in.readNBytes(24));
            return new int[]{b.getInt(16), b.getInt(20)};
        }
    }

    @Test
    void theFormRigHasEveryBoneTheCodeNames() throws IOException {
        List<String> bones = bones(AmaraAssets.FORM_GEO);
        assertTrue(bones.size() >= AmaraAssets.FORM_MIN_BONES, "only " + bones.size() + " bones");
        for (String b : AmaraAssets.FORM_BONES) assertTrue(bones.contains(b), "missing bone " + b);
        for (String b : AmaraAssets.TOGGLED) assertTrue(bones.contains(b), "missing toggled bone " + b);
        assertEquals(bones.size(), new HashSet<>(bones).size(), "duplicate bone names");
    }

    @Test
    void everyFormClipExistsWithItsLength() throws IOException {
        Set<String> bones = new HashSet<>(bones(AmaraAssets.FORM_GEO));
        JsonObject anims = read(AmaraAssets.FORM_ANIM).getAsJsonObject("animations");
        for (String clip : AmaraAssets.FORM_CLIPS) {
            String name = "animation." + AmaraAssets.RIG_FORM + "." + clip;
            assertTrue(anims.has(name), "missing clip " + clip);
            JsonObject a = anims.getAsJsonObject(name);
            Integer ticks = AmaraAssets.CLIP_TICKS.get(clip);
            if (ticks != null) {
                double len = a.get("animation_length").getAsDouble();
                assertTrue(Math.abs(len - ticks / 20.0) <= 0.06, clip + " lasts " + len + "s, not " + ticks / 20.0);
            }
            if (AmaraAssets.FORM_LOOPS.contains(clip)) {
                assertTrue(a.has("loop") && a.get("loop").getAsJsonPrimitive().isBoolean() && a.get("loop").getAsBoolean(), clip + " must loop");
            }
            if (!a.has("bones")) continue;
            for (Map.Entry<String, JsonElement> e : a.getAsJsonObject("bones").entrySet()) {
                assertTrue(bones.contains(e.getKey()), clip + " animates unknown bone " + e.getKey());
                // The renderer scales a child's head itself, every frame.
                if (e.getKey().equals("head")) assertFalse(e.getValue().getAsJsonObject().has("scale"), clip + " scales the head");
            }
        }
    }

    @Test
    void everyAgeHasItsTextures() throws IOException {
        int[] size = null;
        for (AmaraAssets.Age age : AmaraAssets.Age.values()) {
            Path tex = ASSETS.resolve(age.texturePath()), glow = ASSETS.resolve(age.glowPath());
            assertTrue(Files.exists(tex), "missing " + tex);
            assertTrue(Files.exists(glow), "missing " + glow);
            int[] s = pngSize(tex);
            if (size == null) size = s;
            assertEquals(size[0], s[0], age + " width");
            assertEquals(size[1], s[1], age + " height");
            assertEquals(s[0], pngSize(glow)[0], age + " glowmask width");
        }
    }

    @Test
    void theMassKeepsItsFrozenBonesAndClips() throws IOException {
        List<String> bones = bones(AmaraAssets.MASS_GEO);
        for (String b : AmaraAssets.MASS_FROZEN) assertTrue(bones.contains(b), "mass lost bone " + b);
        JsonObject anims = read(AmaraAssets.MASS_ANIM).getAsJsonObject("animations");
        List<String> wanted = new ArrayList<>(AmaraAssets.MASS_LOOPS);
        wanted.addAll(AmaraAssets.MASS_TRIGGERED);
        for (String clip : wanted) assertTrue(anims.has("animation.amara." + clip), "mass lacks clip " + clip);
    }

    @Test
    void theHudSheetSpritesAndCinematicsExist() throws IOException {
        Path sheet = ASSETS.resolve(AmaraAssets.HUD_SHEET);
        assertTrue(Files.exists(sheet), "missing HUD sheet");
        int[] s = pngSize(sheet);
        assertEquals(AmaraHudSheet.WIDTH, s[0]);
        assertEquals(AmaraHudSheet.HEIGHT, s[1]);
        for (String p : AmaraAssets.PARTICLES) assertTrue(Files.exists(ASSETS.resolve("textures/particle/" + p + ".png")), "missing particle " + p);
        for (String i : AmaraAssets.ITEMS) assertTrue(Files.exists(ASSETS.resolve("textures/item/" + i + ".png")), "missing item " + i);
        for (String c : AmaraAssets.CINEMATICS) {
            JsonObject j = read("cinematics/" + c + ".json");
            assertTrue(j.getAsJsonArray("shots").size() >= 3, c + " should be a long, multi-shot sequence");
        }
    }
}
