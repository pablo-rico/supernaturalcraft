package org.papiricoh.supernaturalcraft.purgatory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.papiricoh.supernaturalcraft.entity.alpha.AlphaAssets;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveAssets;

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
 * Shared file checks for Purgatory's art tests. Purgatory's art (tools/artgen eve_art, alphas_art, purgatory_*_art) against {@link EveAssets}, {@link AlphaAssets} and
 * {@link PurgatoryAssets}: every model, clip (and its length), texture at its atlas size and sprite the code loads, the bones it
 * shows, hides or reads, and that no clip scales a bone the renderer toggles. Pure file checks.
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

    static void assertAtlas(String geo, String texture) throws IOException {
        JsonObject d = geometry(geo).getAsJsonObject("description");
        int[] s = pngSize(texture);
        assertEquals(d.get("texture_width").getAsInt(), s[0], texture + " width");
        assertEquals(d.get("texture_height").getAsInt(), s[1], texture + " height");
    }

    static void assertClips(String anim, String prefix, List<String> clips, List<String> loops, Map<String, Integer> ticks,
                                    List<String> toggled) throws IOException {
        JsonObject a = read(anim).getAsJsonObject("animations");
        for (String clip : clips) {
            String name = prefix + clip;
            assertTrue(a.has(name), "missing clip " + name);
            JsonObject c = a.getAsJsonObject(name);
            JsonElement loop = c.get("loop");
            if (loops.contains(clip)) assertTrue(loop != null && loop.isJsonPrimitive() && loop.getAsBoolean(), name + " must loop");
            if (ticks.containsKey(clip)) assertEquals(ticks.get(clip) / 20.0, c.get("animation_length").getAsDouble(), 0.051, name + " length");
            JsonObject bones = c.getAsJsonObject("bones");
            if (bones == null) continue;
            for (String bone : toggled) {
                JsonObject b = bones.getAsJsonObject(bone);
                assertTrue(b == null || !b.has("scale"), name + " scales " + bone + ", which the renderer toggles");
            }
        }
    }
}
