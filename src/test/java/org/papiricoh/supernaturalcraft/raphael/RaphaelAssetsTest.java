package org.papiricoh.supernaturalcraft.raphael;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAssets;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelBalance;

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
 * Raphael's art (tools/artgen raphael_art, raphael_items_art, the garrison in host_angel_art; tools/soundgen raphael_sfx)
 * against {@link RaphaelAssets}: every model, clip, texture (at its atlas size) and sound the code loads, the bones it shows,
 * hides or reads, the rig's detail, and the bones no clip may key. Pure file checks.
 */
class RaphaelAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");
    private static final String PREFIX = "animation.raphael.";

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

    @Test
    void rigIsDetailedAndHasEveryContractBone() throws IOException {
        Set<String> b = bones(RaphaelAssets.GEO);
        assertTrue(b.size() >= RaphaelAssets.MIN_BONES, "Raphael has only " + b.size() + " bones");
        for (String bone : RaphaelAssets.BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        for (String bone : RaphaelAssets.VEIN_BONES) assertTrue(b.contains(bone), "missing vein shell " + bone);
        for (String prop : RaphaelAssets.PROPS.keySet()) assertTrue(b.contains(prop), "missing prop " + prop);
        for (String clip : RaphaelAssets.PROPS.values()) assertTrue(RaphaelAssets.CLIPS.contains(clip), "unknown prop clip " + clip);
        // Two pairs of wings under the one group.
        for (String w : new String[]{"wing_r1", "wing_l1", "wing_r2", "wing_l2"}) assertTrue(b.contains(w), "missing wing " + w);
    }

    @Test
    void wingsHangOffTheirGroup() throws IOException {
        for (JsonElement e : geometry(RaphaelAssets.GEO).getAsJsonArray("bones")) {
            JsonObject bone = e.getAsJsonObject();
            String name = bone.get("name").getAsString();
            if (name.matches("wing_[rl][12]")) assertEquals("wings", bone.get("parent").getAsString(), name);
        }
    }

    @Test
    void texturesFitTheAtlas() throws IOException {
        assertAtlas(RaphaelAssets.GEO, RaphaelAssets.TEXTURE);
        assertAtlas(RaphaelAssets.GEO, RaphaelAssets.GLOW);
        assertAtlas(RaphaelAssets.GARRISON_GEO, RaphaelAssets.GARRISON_TEXTURE);
        assertAtlas(RaphaelAssets.STAFF_GEO, RaphaelAssets.STAFF_TEXTURE);
        int[] icon = pngSize(RaphaelAssets.STAFF_ICON);
        assertEquals(16, icon[0]);
        assertEquals(16, icon[1]);
        int[] slick = pngSize(RaphaelAssets.OIL_SLICK);
        assertEquals(16, slick[0]);
    }

    @Test
    void everyClipExistsLoopsOrPlaysOnceAndFitsItsTiming() throws IOException {
        JsonObject a = read(RaphaelAssets.ANIM).getAsJsonObject("animations");
        for (String clip : RaphaelAssets.CLIPS) {
            String name = PREFIX + clip;
            assertTrue(a.has(name), "missing clip " + name);
            JsonElement loop = a.getAsJsonObject(name).get("loop");
            boolean loops = loop != null && loop.getAsJsonPrimitive().isBoolean() && loop.getAsBoolean();
            assertEquals(RaphaelAssets.LOOPS.contains(clip), loops, name + (loops ? " must not loop" : " must loop"));
        }
        for (String held : new String[]{"wings_reveal", "death"}) {
            assertEquals("hold_on_last_frame", a.getAsJsonObject(PREFIX + held).get("loop").getAsString(), held + " holds");
        }
        for (var e : RaphaelAssets.CLIP_TICKS.entrySet()) {
            double length = a.getAsJsonObject(PREFIX + e.getKey()).get("animation_length").getAsDouble();
            assertEquals(e.getValue(), (int) Math.round(length * 20), e.getKey() + " length");
        }
        for (var e : RaphaelAssets.HIT_TICKS.entrySet()) {
            assertTrue(e.getValue() <= RaphaelAssets.CLIP_TICKS.get(e.getKey()), e.getKey() + " lands after it ends");
        }
        // The smite's hand closes after exactly the windup the fight waits.
        assertEquals(RaphaelBalance.SMITE_WINDUP, (int) RaphaelAssets.HIT_TICKS.get("smite"));
        JsonObject fingers = a.getAsJsonObject(PREFIX + "smite").getAsJsonObject("bones").getAsJsonObject("right_fingers")
                .getAsJsonObject("rotation");
        assertTrue(fingers.has(String.valueOf(RaphaelBalance.SMITE_WINDUP / 20.0)), "the smite keys its fist at the windup");
    }

    @Test
    void clipsOnlyKeyRealBonesAndNeverTheOnesTheRendererDrives() throws IOException {
        Set<String> b = bones(RaphaelAssets.GEO);
        Set<String> forbidden = new HashSet<>(RaphaelAssets.PROCEDURAL);
        JsonObject a = read(RaphaelAssets.ANIM).getAsJsonObject("animations");
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
    void staffTrophyAndSounds() throws IOException {
        Set<String> staff = bones(RaphaelAssets.STAFF_GEO);
        assertTrue(staff.contains("staff") && staff.contains("crystal") && staff.contains("arcs"));
        JsonObject anims = read("animations/item/raphaels_stormcaller.animation.json").getAsJsonObject("animations");
        for (String clip : new String[]{"idle", "zap", "heal"}) assertTrue(anims.has("animation.raphaels_stormcaller." + clip), clip);
        JsonObject trophy = read(RaphaelAssets.TROPHY_MODEL);
        assertTrue(trophy.getAsJsonArray("elements").size() > 6, "the bust has its parts");
        for (String sound : RaphaelAssets.SOUNDS) {
            assertTrue(Files.exists(ASSETS.resolve("sounds/raphael/" + sound + ".ogg")), "missing sound " + sound);
        }
    }
}
