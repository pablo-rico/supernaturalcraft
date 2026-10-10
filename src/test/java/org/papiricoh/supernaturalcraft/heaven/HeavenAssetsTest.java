package org.papiricoh.supernaturalcraft.heaven;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

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
 * v0.18's 3D art (tools/artgen naomi_art, zachariah_art, ash_art, chair_art, the GeckoLib half of heaven_items_art and the
 * heaven_guard/clerk_angel looks of host_angel_art) against {@link HeavenAssets}: every model, texture (at its atlas size),
 * clip and bone the code relies on, the rigs' detail, clip lengths and hit ticks, the props and the busts. Pure file checks.
 */
class HeavenAssetsTest {

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

    private static JsonObject clips(String anim) throws IOException {
        return read(anim).getAsJsonObject("animations");
    }

    /** Every clip exists; exactly the loops loop; lengths match the tick table when there is one. */
    private static void assertClips(String anim, String prefix, List<String> clips, List<String> loops, Map<String, Integer> ticks)
            throws IOException {
        JsonObject a = clips(anim);
        for (String clip : clips) {
            String name = prefix + clip;
            assertTrue(a.has(name), "missing clip " + name);
            JsonElement loop = a.getAsJsonObject(name).get("loop");
            boolean loops_ = loop != null && loop.getAsJsonPrimitive().isBoolean() && loop.getAsBoolean();
            assertEquals(loops.contains(clip), loops_, name + (loops_ ? " must not loop" : " must loop"));
        }
        if (ticks != null) {
            assertEquals(Set.copyOf(clips), ticks.keySet(), prefix + " tick table covers every clip");
            for (var e : ticks.entrySet()) {
                double length = a.getAsJsonObject(prefix + e.getKey()).get("animation_length").getAsDouble();
                assertEquals(e.getValue(), (int) Math.round(length * 20), e.getKey() + " length");
            }
        }
    }

    /** Clips key only bones of the rig. */
    private static void assertKeysRealBones(String geo, String anim) throws IOException {
        Set<String> b = bones(geo);
        for (var clip : clips(anim).entrySet()) {
            JsonObject keyed = clip.getValue().getAsJsonObject().getAsJsonObject("bones");
            if (keyed == null) continue;
            for (String bone : keyed.keySet()) assertTrue(b.contains(bone), clip.getKey() + " keys an unknown bone " + bone);
        }
    }

    private static void assertHitsInside(Map<String, Integer> hits, Map<String, Integer> ticks) {
        for (var e : hits.entrySet()) {
            assertTrue(ticks.containsKey(e.getKey()), "hit for an unknown clip " + e.getKey());
            assertTrue(e.getValue() <= ticks.get(e.getKey()), e.getKey() + " lands after it ends");
        }
    }

    private static void assertProps(Set<String> bones, Map<String, List<String>> props, List<String> clips) {
        for (var e : props.entrySet()) {
            assertTrue(bones.contains(e.getKey()), "missing prop " + e.getKey());
            for (String c : e.getValue()) assertTrue(clips.contains(c), "prop " + e.getKey() + " names unknown clip " + c);
        }
    }

    @Test
    void naomi() throws IOException {
        Set<String> b = bones(HeavenAssets.NAOMI_GEO);
        assertTrue(b.size() >= HeavenAssets.NAOMI_MIN_BONES, "Naomi has only " + b.size() + " bones");
        for (String bone : HeavenAssets.NAOMI_BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        assertProps(b, HeavenAssets.NAOMI_PROP_CLIPS, HeavenAssets.NAOMI_CLIPS);
        assertAtlas(HeavenAssets.NAOMI_GEO, HeavenAssets.NAOMI_TEXTURE);
        assertAtlas(HeavenAssets.NAOMI_GEO, HeavenAssets.NAOMI_GLOW);
        assertClips(HeavenAssets.NAOMI_ANIM, "animation.naomi.", HeavenAssets.NAOMI_CLIPS, HeavenAssets.NAOMI_LOOPS,
                HeavenAssets.NAOMI_CLIP_TICKS);
        assertHitsInside(HeavenAssets.NAOMI_HIT_TICKS, HeavenAssets.NAOMI_CLIP_TICKS);
        assertKeysRealBones(HeavenAssets.NAOMI_GEO, HeavenAssets.NAOMI_ANIM);
        // The drill's bit spins inside its drill; the lab coat is one group under the root.
        assertEquals("drill", parent(HeavenAssets.NAOMI_GEO, "drill_bit"));
        assertEquals("root", parent(HeavenAssets.NAOMI_GEO, "coat"));
        assertEquals("hold_on_last_frame", clips(HeavenAssets.NAOMI_ANIM).getAsJsonObject("animation.naomi.death").get("loop").getAsString());
    }

    @Test
    void zachariah() throws IOException {
        Set<String> b = bones(HeavenAssets.ZACHARIAH_GEO);
        assertTrue(b.size() >= HeavenAssets.ZACHARIAH_MIN_BONES, "Zachariah has only " + b.size() + " bones");
        for (String bone : HeavenAssets.ZACHARIAH_BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        assertProps(b, HeavenAssets.ZACHARIAH_PROP_CLIPS, HeavenAssets.ZACHARIAH_CLIPS);
        assertAtlas(HeavenAssets.ZACHARIAH_GEO, HeavenAssets.ZACHARIAH_TEXTURE);
        assertAtlas(HeavenAssets.ZACHARIAH_GEO, HeavenAssets.ZACHARIAH_GLOW);
        assertClips(HeavenAssets.ZACHARIAH_ANIM, "animation.zachariah.", HeavenAssets.ZACHARIAH_CLIPS, HeavenAssets.ZACHARIAH_LOOPS,
                HeavenAssets.ZACHARIAH_CLIP_TICKS);
        assertHitsInside(HeavenAssets.ZACHARIAH_HIT_TICKS, HeavenAssets.ZACHARIAH_CLIP_TICKS);
        assertKeysRealBones(HeavenAssets.ZACHARIAH_GEO, HeavenAssets.ZACHARIAH_ANIM);
        // Six wings, all under the one group the renderer hides.
        for (String side : new String[]{"r", "l"}) {
            for (int pair = 1; pair <= 3; pair++) assertEquals("wings", parent(HeavenAssets.ZACHARIAH_GEO, "wing_" + side + pair));
        }
        // No clip keys the palm light (the renderer shows it).
        for (var clip : clips(HeavenAssets.ZACHARIAH_ANIM).entrySet()) {
            JsonObject keyed = clip.getValue().getAsJsonObject().getAsJsonObject("bones");
            assertTrue(keyed == null || !keyed.has("palm_light"), clip.getKey() + " keys palm_light");
        }
    }

    @Test
    void ash() throws IOException {
        Set<String> b = bones(HeavenAssets.ASH_GEO);
        assertTrue(b.size() >= HeavenAssets.ASH_MIN_BONES, "Ash has only " + b.size() + " bones");
        for (String bone : HeavenAssets.ASH_BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        assertAtlas(HeavenAssets.ASH_GEO, HeavenAssets.ASH_TEXTURE);
        assertClips(HeavenAssets.ASH_ANIM, "animation.ash.", HeavenAssets.ASH_CLIPS, HeavenAssets.ASH_LOOPS, null);
        assertKeysRealBones(HeavenAssets.ASH_GEO, HeavenAssets.ASH_ANIM);
        // The props switch by scale in the loops: the glass only while he wipes it.
        JsonObject a = clips(HeavenAssets.ASH_ANIM);
        assertTrue(a.getAsJsonObject("animation.ash.idle").getAsJsonObject("bones").getAsJsonObject("glass").has("scale"),
                "idle hides the glass by scale");
    }

    @Test
    void chair() throws IOException {
        Set<String> b = bones(HeavenAssets.CHAIR_GEO);
        for (String bone : HeavenAssets.CHAIR_BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        assertAtlas(HeavenAssets.CHAIR_GEO, HeavenAssets.CHAIR_TEXTURE);
        assertAtlas(HeavenAssets.CHAIR_GEO, HeavenAssets.CHAIR_GLOW);
        assertClips(HeavenAssets.CHAIR_ANIM, "animation.reprogramming_chair.", HeavenAssets.CHAIR_CLIPS, HeavenAssets.CHAIR_LOOPS,
                HeavenAssets.CHAIR_CLIP_TICKS);
        assertKeysRealBones(HeavenAssets.CHAIR_GEO, HeavenAssets.CHAIR_ANIM);
    }

    @Test
    void hostLooks() throws IOException {
        for (String tex : List.of(HeavenAssets.GUARD_TEXTURE, HeavenAssets.GUARD_GLOW, HeavenAssets.CLERK_TEXTURE, HeavenAssets.CLERK_GLOW)) {
            assertAtlas(HeavenAssets.HOST_GEO, tex);
        }
        JsonObject a = clips(HeavenAssets.HOST_ANIM);
        for (String clip : HeavenAssets.HOST_HEAVEN_CLIPS) assertTrue(a.has("animation.host_angel." + clip), "missing host clip " + clip);
    }

    @Test
    void weaponsIconsAndBusts() throws IOException {
        assertTrue(bones(HeavenAssets.DRILL_GEO).containsAll(List.of("drill", "bit")), "the drill and its spinning bit");
        assertTrue(bones(HeavenAssets.BLADE_GEO).containsAll(List.of("blade", "engraving")), "the blade and its engraving");
        assertAtlas(HeavenAssets.DRILL_GEO, HeavenAssets.DRILL_TEXTURE);
        assertAtlas(HeavenAssets.DRILL_GEO, HeavenAssets.DRILL_GLOW);
        assertAtlas(HeavenAssets.BLADE_GEO, HeavenAssets.BLADE_TEXTURE);
        assertAtlas(HeavenAssets.BLADE_GEO, HeavenAssets.BLADE_GLOW);
        assertClips(HeavenAssets.DRILL_ANIM, "animation.naomis_drill.", HeavenAssets.DRILL_CLIPS, List.of("idle"), null);
        assertClips(HeavenAssets.BLADE_ANIM, "animation.zachariahs_blade.", HeavenAssets.BLADE_CLIPS, List.of("idle"), null);
        for (String icon : List.of("naomis_drill_icon", "zachariahs_blade_icon")) {
            int[] s = pngSize("textures/item/" + icon + ".png");
            assertEquals(16, s[0], icon);
            assertEquals(16, s[1], icon);
        }
        for (String bust : List.of(HeavenAssets.NAOMI_TROPHY_MODEL, HeavenAssets.ZACHARIAH_TROPHY_MODEL)) {
            JsonObject model = read(bust);
            assertTrue(model.getAsJsonArray("elements").size() > 6, bust + " has its parts");
            for (var tex : model.getAsJsonObject("textures").entrySet()) {
                String id = tex.getValue().getAsString();
                if (!id.startsWith("supernaturalcraft:")) continue;
                assertTrue(Files.exists(ASSETS.resolve("textures/" + id.substring("supernaturalcraft:".length()) + ".png")), "missing " + id);
            }
        }
    }

    private static String parent(String geo, String bone) throws IOException {
        for (JsonElement e : geometry(geo).getAsJsonArray("bones")) {
            JsonObject b = e.getAsJsonObject();
            if (b.get("name").getAsString().equals(bone)) return b.has("parent") ? b.get("parent").getAsString() : null;
        }
        throw new AssertionError("no bone " + bone);
    }
}
