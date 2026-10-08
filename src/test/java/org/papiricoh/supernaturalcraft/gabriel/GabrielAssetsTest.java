package org.papiricoh.supernaturalcraft.gabriel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.Channel;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielAssets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gabriel's art (tools/artgen gabriel_art, gabriel_items_art, gabriel_gui_art; tools/soundgen gabriel_sfx) against
 * {@link GabrielAssets}: every model, clip, texture (at its contract size) and sound the code loads, the bones it shows,
 * hides or reads, the costume groups, the rig's detail, and the bones no clip may key. Pure file checks.
 */
class GabrielAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");
    private static final String PREFIX = "animation.gabriel.";

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

    @Test
    void rigIsDetailedAndHasEveryContractBone() throws IOException {
        Set<String> b = bones(GabrielAssets.GEO);
        assertTrue(b.size() >= GabrielAssets.MIN_BONES, "Gabriel has only " + b.size() + " bones");
        for (String bone : GabrielAssets.BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        for (String group : GabrielAssets.COSTUME_BONES.values()) assertTrue(b.contains(group), "missing costume group " + group);
        assertEquals(Channel.Costume.values().length - 1, GabrielAssets.COSTUME_BONES.size(), "every costume but the jacket has a group");
        for (String prop : GabrielAssets.PROP_CLIPS.keySet()) assertTrue(b.contains(prop), "missing prop " + prop);
        for (String clip : GabrielAssets.PROP_CLIPS.values()) assertTrue(GabrielAssets.CLIPS.contains(clip), "unknown prop clip " + clip);
    }

    @Test
    void costumeGroupsHangOffTheRootAndHoldTheirOwnLimbs() throws IOException {
        var all = geometry(GabrielAssets.GEO).getAsJsonArray("bones");
        for (String group : GabrielAssets.COSTUME_BONES.values()) {
            int children = 0;
            for (JsonElement e : all) {
                JsonObject bone = e.getAsJsonObject();
                if (bone.get("name").getAsString().equals(group)) {
                    assertEquals("root", bone.get("parent").getAsString(), group + " must not follow a limb");
                }
                if (bone.has("parent") && bone.get("parent").getAsString().equals(group)) children++;
            }
            assertTrue(children > 0, group + " is empty");
        }
    }

    @Test
    void everyCostumeTextureFitsTheOneAtlas() throws IOException {
        for (Channel.Costume c : Channel.Costume.values()) assertAtlas(GabrielAssets.GEO, GabrielAssets.TEXTURE.formatted(c.id()));
        assertAtlas(GabrielAssets.GEO, GabrielAssets.GLOW);
    }

    @Test
    void everyClipExistsLoopsOrPlaysOnce() throws IOException {
        JsonObject a = read(GabrielAssets.ANIM).getAsJsonObject("animations");
        for (String clip : GabrielAssets.CLIPS) {
            String name = PREFIX + clip;
            assertTrue(a.has(name), "missing clip " + name);
            JsonElement loop = a.getAsJsonObject(name).get("loop");
            boolean loops = loop != null && loop.getAsJsonPrimitive().isBoolean() && loop.getAsBoolean();
            assertEquals(GabrielAssets.LOOPS.contains(clip), loops, name + (loops ? " must not loop" : " must loop"));
        }
        assertTrue(a.getAsJsonObject(PREFIX + "wings_reveal").get("loop").getAsString().equals("hold_on_last_frame"),
                "wings_reveal holds its spread");
        assertTrue(a.getAsJsonObject(PREFIX + "death").get("loop").getAsString().equals("hold_on_last_frame"), "death holds");
    }

    @Test
    void clipsOnlyKeyRealBonesAndNeverTheGroupsTheRendererHides() throws IOException {
        Set<String> b = bones(GabrielAssets.GEO);
        Set<String> forbidden = new HashSet<>(GabrielAssets.COSTUME_BONES.values());
        forbidden.add("nurse_cap");
        forbidden.add("eyes_glow");
        JsonObject a = read(GabrielAssets.ANIM).getAsJsonObject("animations");
        for (var clip : a.entrySet()) {
            JsonObject keyed = clip.getValue().getAsJsonObject().getAsJsonObject("bones");
            if (keyed == null) continue;
            for (String bone : keyed.keySet()) {
                assertTrue(b.contains(bone), clip.getKey() + " keys an unknown bone " + bone);
                assertFalse(forbidden.contains(bone), clip.getKey() + " keys " + bone + ", which only the renderer may touch");
            }
        }
    }

    @Test
    void propsAndPartyHat() throws IOException {
        assertTrue(bones(GabrielAssets.PIE_GEO).contains("pie"));
        assertAtlas(GabrielAssets.PIE_GEO, GabrielAssets.PIE_TEXTURE);
        assertTrue(bones(GabrielAssets.PARTY_HAT_GEO).contains("party_hat"));
        assertAtlas(GabrielAssets.PARTY_HAT_GEO, GabrielAssets.PARTY_HAT_TEXTURE);
        var hat = geometry(GabrielAssets.PARTY_HAT_GEO).getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("pivot");
        for (int i = 0; i < 3; i++) assertEquals(0.0, hat.get(i).getAsDouble(), 1e-6, "the hat's pivot is at its base");
    }

    @Test
    void itemsRemoteAndTrophy() throws IOException {
        for (String item : GabrielAssets.ITEMS) assertSize("textures/item/" + item + ".png", 16, 16);
        assertFalse(bones(GabrielAssets.REMOTE_GEO).isEmpty());
        assertAtlas(GabrielAssets.REMOTE_GEO, GabrielAssets.REMOTE_TEXTURE);
        assertSize(GabrielAssets.REMOTE_ICON, 16, 16);
        JsonObject trophy = read(GabrielAssets.TROPHY_MODEL);
        assertTrue(trophy.getAsJsonArray("elements").size() > 4);
        for (var t : trophy.getAsJsonObject("textures").entrySet()) {
            String ref = t.getValue().getAsString();
            String path = ref.substring(ref.indexOf(':') + 1);
            assertTrue(Files.exists(ASSETS.resolve("textures/" + path + ".png")), "missing trophy texture " + ref);
        }
    }

    @Test
    void interfaceTexturesAtTheirSizes() throws IOException {
        for (String s : GabrielAssets.SIGNS) assertSize(GabrielAssets.SIGN.formatted(s), 128, 32);
        assertSize(GabrielAssets.OSD, 128, 16);
        assertSize(GabrielAssets.STATIC, 128, 128);
        assertSize(GabrielAssets.QUIZ_PANEL, 256, 96);
        assertSize(GabrielAssets.BAR, 256, 32);
    }

    @Test
    void everySoundIsAnOgg() throws IOException {
        for (String id : GabrielAssets.SOUNDS) {
            Path p = ASSETS.resolve("sounds/gabriel/" + id + ".ogg");
            assertTrue(Files.exists(p), "missing sound " + id);
            try (InputStream in = Files.newInputStream(p)) {
                assertEquals("OggS", new String(in.readNBytes(4)), id + " is not an Ogg file");
            }
        }
    }
}
