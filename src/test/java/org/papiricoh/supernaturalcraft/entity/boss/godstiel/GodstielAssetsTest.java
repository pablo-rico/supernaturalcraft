package org.papiricoh.supernaturalcraft.entity.boss.godstiel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Season 6's art (tools/artgen godstiel_art, godstiel_gui_art, season6_items_art) against {@link GodstielAssets}: the
 * Castiel/Godstiel rig and its bones (with the parts of each toggled set), the three skins and the glowmask at the atlas
 * size, every clip at its length and loop mode with its blow inside it, no clip scaling a bone the renderer toggles, the HUD
 * sheet, the sprites, the armour layer, the Blade of the New God and Crowley's King of Hell skin. Pure file checks.
 */
class GodstielAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");
    private static final String PREFIX = "animation." + GodstielAssets.RIG + ".";

    private static JsonObject read(String path) throws IOException {
        Path p = ASSETS.resolve(path);
        assertTrue(Files.exists(p), "missing " + path);
        return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
    }

    private static JsonObject geometry(String geo) throws IOException {
        return read(geo).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    private static Set<String> bones(String geo) throws IOException {
        Set<String> out = new HashSet<>();
        geometry(geo).getAsJsonArray("bones").forEach(b -> out.add(b.getAsJsonObject().get("name").getAsString()));
        return out;
    }

    private static int[] pngSize(String path) throws IOException {
        Path p = ASSETS.resolve(path);
        assertTrue(Files.exists(p), "missing " + path);
        try (InputStream in = Files.newInputStream(p)) {
            byte[] h = in.readNBytes(24);
            int w = ((h[16] & 0xff) << 24) | ((h[17] & 0xff) << 16) | ((h[18] & 0xff) << 8) | (h[19] & 0xff);
            int ht = ((h[20] & 0xff) << 24) | ((h[21] & 0xff) << 16) | ((h[22] & 0xff) << 8) | (h[23] & 0xff);
            return new int[]{w, ht};
        }
    }

    private static void assertSize(String path, int w, int h) throws IOException {
        int[] s = pngSize(path);
        assertEquals(w, s[0], path + " width");
        assertEquals(h, s[1], path + " height");
    }

    private static void assertAtlas(String geo, String texture) throws IOException {
        JsonObject d = geometry(geo).getAsJsonObject("description");
        assertSize(texture, d.get("texture_width").getAsInt(), d.get("texture_height").getAsInt());
    }

    private static List<String> toggledWithParts() {
        List<String> out = new ArrayList<>(GodstielAssets.TOGGLED);
        GodstielAssets.TOGGLED_PARTS.values().forEach(out::addAll);
        return out;
    }

    @Test
    void theRigMeetsTheContract() throws IOException {
        Set<String> b = bones(GodstielAssets.GEO);
        assertTrue(b.size() >= GodstielAssets.MIN_BONES, "Castiel has only " + b.size() + " bones");
        for (String bone : GodstielAssets.BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        for (String bone : GodstielAssets.TOGGLED) assertTrue(b.contains(bone), "missing toggled bone " + bone);
        for (var e : GodstielAssets.TOGGLED_PARTS.entrySet()) {
            assertTrue(GodstielAssets.TOGGLED.contains(e.getKey()), e.getKey() + " has parts but is not toggled");
            for (String part : e.getValue()) assertTrue(b.contains(part), "missing part " + part + " of " + e.getKey());
        }
        for (String tex : List.of(GodstielAssets.TEXTURE_NPC, GodstielAssets.TEXTURE, GodstielAssets.GLOW, GodstielAssets.TEXTURE_SOAKED,
                GodstielAssets.GLOW_SOAKED)) {
            assertAtlas(GodstielAssets.GEO, tex);
        }
    }

    @Test
    void everyClipAtItsLengthWithItsBlowInside() throws IOException {
        JsonObject a = read(GodstielAssets.ANIM).getAsJsonObject("animations");
        List<String> toggled = toggledWithParts();
        for (String clip : GodstielAssets.CLIPS) {
            String name = PREFIX + clip;
            assertTrue(a.has(name), "missing clip " + name);
            JsonObject c = a.getAsJsonObject(name);
            JsonElement loop = c.get("loop");
            boolean loops = loop != null && loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isBoolean() && loop.getAsBoolean();
            assertEquals(GodstielAssets.LOOPS.contains(clip), loops, name + " loop mode");
            if (GodstielAssets.CLIP_TICKS.containsKey(clip)) {
                assertEquals(GodstielAssets.CLIP_TICKS.get(clip) / 20.0, c.get("animation_length").getAsDouble(), 0.026, name + " length");
            }
            JsonObject bones = c.getAsJsonObject("bones");
            if (bones == null) continue;
            for (String bone : toggled) {
                JsonObject bo = bones.getAsJsonObject(bone);
                assertTrue(bo == null || !bo.has("scale"), name + " scales " + bone + ", which the renderer toggles");
            }
        }
        for (var e : GodstielAssets.HIT_TICKS.entrySet()) {
            Integer len = GodstielAssets.CLIP_TICKS.get(e.getKey());
            assertTrue(len != null, e.getKey() + " has a blow but no length");
            assertTrue(e.getValue() > 0 && e.getValue() < len, e.getKey() + "'s blow lands inside it");
        }
    }

    @Test
    void theHudSpritesAndArmour() throws IOException {
        assertSize(GodstielAssets.BAR_SHEET, 256, 128);
        for (String item : GodstielAssets.ITEMS) assertSize("textures/item/" + item + ".png", 16, 16);
        for (String particle : GodstielAssets.PARTICLES) assertSize("textures/particle/" + particle + ".png", 16, 16);
        assertSize(GodstielAssets.COAT_ARMOR, 64, 32);
        int[] crowley = pngSize("textures/entity/crowley.png");
        assertSize(GodstielAssets.CROWLEY_KING_TEXTURE, crowley[0], crowley[1]);
    }

    @Test
    void theBladeOfTheNewGod() throws IOException {
        Set<String> b = bones(GodstielAssets.BLADE_GEO);
        for (String bone : List.of("blade", "grip", "light")) assertTrue(b.contains(bone), "the blade is missing " + bone);
        assertAtlas(GodstielAssets.BLADE_GEO, GodstielAssets.BLADE_TEXTURE);
        assertAtlas(GodstielAssets.BLADE_GEO, "textures/item/" + GodstielAssets.BLADE + "_glowmask.png");
        assertSize("textures/item/" + GodstielAssets.BLADE + "_icon.png", 16, 16);
        JsonObject a = read(GodstielAssets.BLADE_ANIM).getAsJsonObject("animations");
        for (String clip : GodstielAssets.BLADE_CLIPS) {
            String name = "animation." + GodstielAssets.BLADE + "." + clip;
            assertTrue(a.has(name), "missing clip " + name);
            JsonObject bones = a.getAsJsonObject(name).getAsJsonObject("bones");
            assertTrue(bones != null && bones.has("light"), name + " leaves the light unposed");
        }
    }
}
