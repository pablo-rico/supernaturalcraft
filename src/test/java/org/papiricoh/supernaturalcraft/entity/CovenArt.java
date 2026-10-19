package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** File helpers shared by the v0.21 coven asset tests (witches, Rowena, the Grand Witch). Pure file reads. */
final class CovenArt {

    static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");
    private static final Path SOUNDS = Path.of("src/generated/resources/assets/supernaturalcraft/sounds.json");

    private CovenArt() {
    }

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

    static JsonObject anims(String anim) throws IOException {
        return read(anim).getAsJsonObject("animations");
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

    /** The texture is the size the geometry's UVs were laid out for. */
    static void assertAtlas(String geo, String texture) throws IOException {
        JsonObject d = geometry(geo).getAsJsonObject("description");
        int[] s = pngSize(texture);
        assertEquals(d.get("texture_width").getAsInt(), s[0], texture + " width");
        assertEquals(d.get("texture_height").getAsInt(), s[1], texture + " height");
    }

    static boolean loops(JsonObject clip) {
        JsonElement loop = clip.get("loop");
        return loop != null && loop.getAsJsonPrimitive().isBoolean() && loop.getAsBoolean();
    }

    static String loopMode(JsonObject clip) {
        JsonElement loop = clip.get("loop");
        return loop == null ? "once" : loop.getAsString();
    }

    static int ticks(JsonObject clip) {
        return (int) Math.round(clip.get("animation_length").getAsDouble() * 20);
    }

    /** The bones {@code clip} keys (empty if none). */
    static Set<String> keyed(JsonObject clip) {
        JsonObject b = clip.getAsJsonObject("bones");
        return b == null ? Set.of() : b.keySet();
    }

    /** Whether the generated sounds.json defines {@code event}. */
    static boolean soundDefined(String event) throws IOException {
        assertTrue(Files.exists(SOUNDS), "no generated sounds.json: run runData");
        return JsonParser.parseString(Files.readString(SOUNDS)).getAsJsonObject().has(event);
    }

    /** Every scale keyframe {@code bone} has in {@code clip} (each component), or none if it is not scaled there. */
    static Set<Double> scales(JsonObject clip, String bone) {
        Set<Double> out = new HashSet<>();
        JsonObject b = clip.getAsJsonObject("bones");
        if (b == null || !b.has(bone) || !b.getAsJsonObject(bone).has("scale")) return out;
        JsonElement scale = b.getAsJsonObject(bone).get("scale");
        collect(scale, out);
        return out;
    }

    private static void collect(JsonElement e, Set<Double> out) {
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) out.add(e.getAsDouble());
        else if (e.isJsonArray()) e.getAsJsonArray().forEach(x -> collect(x, out));
        else if (e.isJsonObject()) {
            for (var en : e.getAsJsonObject().entrySet()) {
                if (en.getKey().equals("easing") || en.getKey().equals("lerp_mode")) continue;
                collect(en.getValue(), out);
            }
        }
    }
}
