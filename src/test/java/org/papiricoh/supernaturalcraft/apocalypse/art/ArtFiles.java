package org.papiricoh.supernaturalcraft.apocalypse.art;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
 * Shared file checks for the Apocalypse's art tests (v0.40): models, clips and their lengths, atlases, sprites, block models.
 * Pure file checks against the contracts ({@code *Assets}).
 */
final class ArtFiles {

    private ArtFiles() {
    }

    static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

    static JsonObject read(String path) throws IOException {
        Path p = ASSETS.resolve(path);
        assertTrue(Files.exists(p), "missing " + path);
        return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
    }

    static JsonObject geometry(String geo) throws IOException {
        return read(geo).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    static Set<String> bones(String geo) throws IOException {
        Set<String> out = new HashSet<>();
        geometry(geo).getAsJsonArray("bones").forEach(b -> out.add(b.getAsJsonObject().get("name").getAsString()));
        return out;
    }

    static int[] pngSize(String path) throws IOException {
        Path p = ASSETS.resolve(path);
        assertTrue(Files.exists(p), "missing " + path);
        try (InputStream in = Files.newInputStream(p)) {
            byte[] h = in.readNBytes(24);
            int w = ((h[16] & 0xff) << 24) | ((h[17] & 0xff) << 16) | ((h[18] & 0xff) << 8) | (h[19] & 0xff);
            int ht = ((h[20] & 0xff) << 24) | ((h[21] & 0xff) << 16) | ((h[22] & 0xff) << 8) | (h[23] & 0xff);
            return new int[]{w, ht};
        }
    }

    static void assertSize(String path, int w, int h) throws IOException {
        int[] s = pngSize(path);
        assertEquals(w, s[0], path + " width");
        assertEquals(h, s[1], path + " height");
    }

    /** The texture's size is the geo's atlas size. */
    static void assertAtlas(String geo, String texture) throws IOException {
        JsonObject d = geometry(geo).getAsJsonObject("description");
        int[] s = pngSize(texture);
        assertEquals(d.get("texture_width").getAsInt(), s[0], texture + " width");
        assertEquals(d.get("texture_height").getAsInt(), s[1], texture + " height");
    }

    static void assertBones(String geo, List<String> required, int min) throws IOException {
        Set<String> b = bones(geo);
        assertTrue(b.size() >= min, geo + " has only " + b.size() + " bones");
        for (String bone : required) assertTrue(b.contains(bone), geo + " is missing bone " + bone);
    }

    /**
     * Every clip exists, loops when it should, has its contract length; no clip keys a bone of {@code untouchable} (the
     * renderer moves or toggles those) at all.
     */
    static void assertClips(String anim, String prefix, List<String> clips, List<String> loops, Map<String, Integer> ticks,
                            List<String> untouchable) throws IOException {
        JsonObject a = read(anim).getAsJsonObject("animations");
        for (String clip : clips) {
            String name = prefix + clip;
            assertTrue(a.has(name), "missing clip " + name);
            JsonObject c = a.getAsJsonObject(name);
            JsonElement loop = c.get("loop");
            if (loops.contains(clip)) assertTrue(loop != null && loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isBoolean()
                    && loop.getAsBoolean(), name + " must loop");
            else assertTrue(loop == null || !loop.isJsonPrimitive() || !loop.getAsJsonPrimitive().isBoolean() || !loop.getAsBoolean(),
                    name + " must play once");
            if (ticks.containsKey(clip)) assertEquals(ticks.get(clip) / 20.0, c.get("animation_length").getAsDouble(), 0.051, name + " length");
            JsonObject bones = c.getAsJsonObject("bones");
            if (bones == null) continue;
            for (String bone : untouchable) assertTrue(!bones.has(bone), name + " keys " + bone + ", which the renderer drives");
        }
    }

    /** Every clip's bones exist in the model. */
    static void assertClipBonesExist(String geo, String anim) throws IOException {
        Set<String> b = bones(geo);
        JsonObject a = read(anim).getAsJsonObject("animations");
        for (String name : a.keySet()) {
            JsonObject bones = a.getAsJsonObject(name).getAsJsonObject("bones");
            if (bones == null) continue;
            for (String bone : bones.keySet()) assertTrue(b.contains(bone), name + " keys missing bone " + bone);
        }
    }

    /** Each moment lands inside its clip. */
    static void assertHits(Map<String, Integer> hits, Map<String, Integer> lengths) {
        for (Map.Entry<String, Integer> e : hits.entrySet()) {
            Integer len = lengths.get(e.getKey());
            assertTrue(len != null, e.getKey() + " has a hit tick but no length");
            assertTrue(e.getValue() >= 0 && e.getValue() < len, e.getKey() + "'s moment (" + e.getValue() + ") lands inside it (" + len + ")");
        }
    }

    /** A vanilla block model whose every texture variable points at a texture that exists. */
    static void assertBlockModel(String model) throws IOException {
        JsonObject m = read(model);
        assertTrue(m.has("elements") || m.has("parent"), model + " has no elements");
        JsonObject tex = m.getAsJsonObject("textures");
        assertTrue(tex != null, model + " has no textures");
        for (String k : tex.keySet()) {
            String v = tex.get(k).getAsString();
            if (v.startsWith("#") || v.startsWith("minecraft:")) continue;
            String path = v.substring(v.indexOf(':') + 1);
            assertTrue(Files.exists(ASSETS.resolve("textures/" + path + ".png")), model + " points at missing texture " + v);
        }
    }
}
