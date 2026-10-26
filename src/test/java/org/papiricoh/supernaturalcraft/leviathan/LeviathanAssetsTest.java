package org.papiricoh.supernaturalcraft.leviathan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickAssets;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanAssets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Leviathans' and Dick Roman's art (tools/artgen leviathan_art, dick_roman_art, leviathan_items_art) against
 * {@link LeviathanAssets} and {@link DickAssets}: every model, clip (and its length), texture at its atlas size and sprite the
 * code loads, the bones it shows, hides or reads, and that no clip scales a bone the renderer toggles. Pure file checks.
 */
class LeviathanAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

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

    private static void assertAtlas(String geo, String texture) throws IOException {
        JsonObject d = geometry(geo).getAsJsonObject("description");
        int[] s = pngSize(texture);
        assertEquals(d.get("texture_width").getAsInt(), s[0], texture + " width");
        assertEquals(d.get("texture_height").getAsInt(), s[1], texture + " height");
    }

    /** Every clip exists, loops when it should, lasts what the code waits for, and never scales a toggled bone. */
    private static void assertClips(String anim, String prefix, List<String> clips, List<String> loops, Map<String, Integer> ticks,
                                    List<String> toggled) throws IOException {
        JsonObject a = read(anim).getAsJsonObject("animations");
        for (String clip : clips) {
            String name = prefix + clip;
            assertTrue(a.has(name), "missing clip " + name);
            JsonObject c = a.getAsJsonObject(name);
            JsonElement loop = c.get("loop");
            if (loops.contains(clip)) assertTrue(loop != null && loop.isJsonPrimitive() && loop.getAsBoolean(), name + " must loop");
            if (ticks.containsKey(clip)) {
                assertEquals(ticks.get(clip) / 20.0, c.get("animation_length").getAsDouble(), 0.051, name + " length");
            }
            JsonObject bones = c.getAsJsonObject("bones");
            if (bones == null) continue;
            for (String bone : toggled) {
                JsonObject b = bones.getAsJsonObject(bone);
                assertTrue(b == null || !b.has("scale") || bone.equals("pen"), name + " scales " + bone + ", which the renderer toggles");
            }
        }
    }

    @Test
    void theLeviathanAndItsHeadMeetTheContract() throws IOException {
        Set<String> b = bones(LeviathanAssets.GEO);
        assertTrue(b.size() >= LeviathanAssets.MIN_BONES, "the Leviathan has only " + b.size() + " bones");
        for (String bone : LeviathanAssets.BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        assertTrue(bones(LeviathanAssets.HEAD_GEO).containsAll(LeviathanAssets.HEAD_BONES), "the head's bones");
        assertAtlas(LeviathanAssets.GEO, LeviathanAssets.TEXTURE);
        assertAtlas(LeviathanAssets.HEAD_GEO, LeviathanAssets.HEAD_TEXTURE);
        assertClips(LeviathanAssets.ANIM, "animation.leviathan.", LeviathanAssets.CLIPS, LeviathanAssets.LOOPS, LeviathanAssets.CLIP_TICKS,
                LeviathanAssets.TOGGLED);
        assertClips(LeviathanAssets.HEAD_ANIM, "animation.leviathan_head.", LeviathanAssets.HEAD_CLIPS, List.of("idle", "crawl"), Map.of(),
                List.of());
    }

    @Test
    void dickRomanMeetsTheContract() throws IOException {
        Set<String> b = bones(DickAssets.GEO);
        assertTrue(b.size() >= DickAssets.MIN_BONES, "Dick Roman has only " + b.size() + " bones");
        for (String bone : DickAssets.BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        assertAtlas(DickAssets.GEO, DickAssets.TEXTURE);
        assertClips(DickAssets.ANIM, "animation.dick_roman.", DickAssets.CLIPS, DickAssets.LOOPS, DickAssets.CLIP_TICKS, DickAssets.TOGGLED);
        for (String clip : DickAssets.HIT_TICKS.keySet()) {
            assertTrue(DickAssets.HIT_TICKS.get(clip) < DickAssets.CLIP_TICKS.get(clip), clip + "'s blow lands inside it");
        }
    }

    @Test
    void spritesDecalsAndIconsExist() throws IOException {
        for (String item : LeviathanAssets.ITEMS) assertEquals(16, pngSize("textures/item/" + item + ".png")[0], item);
        assertEquals(16, pngSize(LeviathanAssets.GOO)[0]);
        assertEquals(16, pngSize(LeviathanAssets.GOO_DRIED)[0]);
        for (String effect : LeviathanAssets.EFFECTS) assertEquals(18, pngSize("textures/mob_effect/" + effect + ".png")[0], effect);
    }
}
