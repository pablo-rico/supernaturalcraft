package org.papiricoh.supernaturalcraft.allegiance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The allegiance's art (tools/artgen allegiance_art + rival_hunter_art, tools/soundgen allegiance_sfx) against
 * {@link AllegianceAssets}: every model, clip, texture (at its contract size) and sound the code loads, the bones it shows,
 * hides or reads, the rival hunter's detail, and the bones no clip may key. Pure file checks.
 */
class AllegianceAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    private static JsonObject geometry(String geo) throws IOException {
        return read(geo).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    private static Set<String> bones(String geo) throws IOException {
        Set<String> out = new HashSet<>();
        geometry(geo).getAsJsonArray("bones").forEach(b -> out.add(b.getAsJsonObject().get("name").getAsString()));
        return out;
    }

    private static JsonObject anims(String file) throws IOException {
        return read(file).getAsJsonObject("animations");
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

    /** A texture drawn on a geo must match the geo's declared atlas size. */
    private static void assertAtlas(String geo, String texture) throws IOException {
        JsonObject d = geometry(geo).getAsJsonObject("description");
        assertSize(texture, d.get("texture_width").getAsInt(), d.get("texture_height").getAsInt());
    }

    private enum Mode { LOOP, HOLD, ONCE }

    private static Mode mode(JsonObject clip) {
        JsonElement loop = clip.get("loop");
        if (loop == null) return Mode.ONCE;
        if (loop.getAsJsonPrimitive().isBoolean()) return loop.getAsBoolean() ? Mode.LOOP : Mode.ONCE;
        return "hold_on_last_frame".equals(loop.getAsString()) ? Mode.HOLD : Mode.ONCE;
    }

    private static void assertClips(String file, String prefix, List<String> clips, Set<String> loops, Set<String> holds) throws IOException {
        JsonObject a = anims(file);
        for (String clip : clips) {
            String name = prefix + clip;
            assertTrue(a.has(name), file + " is missing " + name);
            Mode m = mode(a.getAsJsonObject(name));
            if (loops.contains(clip)) assertEquals(Mode.LOOP, m, name + " must loop");
            else assertFalse(m == Mode.LOOP, name + " must play once");
            if (holds.contains(clip)) assertEquals(Mode.HOLD, m, name + " must hold its last frame");
        }
    }

    private static Set<String> keyedBones(String file) throws IOException {
        Set<String> out = new HashSet<>();
        for (var e : anims(file).entrySet()) {
            JsonObject b = e.getValue().getAsJsonObject().getAsJsonObject("bones");
            if (b != null) out.addAll(b.keySet());
        }
        return out;
    }

    @Test
    void messengerModelClipsAndBones() throws IOException {
        assertTrue(bones(AllegianceAssets.MESSENGER_GEO).containsAll(AllegianceAssets.MESSENGER_BONES));
        assertAtlas(AllegianceAssets.MESSENGER_GEO, AllegianceAssets.MESSENGER_TEXTURE);
        assertAtlas(AllegianceAssets.MESSENGER_GEO, AllegianceAssets.MESSENGER_GLOW);
        assertClips(AllegianceAssets.MESSENGER_ANIM, "animation.messenger.", AllegianceAssets.MESSENGER_CLIPS, Set.of("idle"),
                Set.of("wing_spread"));
    }

    @Test
    void rivalHunterIsDetailedWithThreeLooks() throws IOException {
        Set<String> b = bones(AllegianceAssets.RIVAL_HUNTER_GEO);
        assertTrue(b.containsAll(AllegianceAssets.RIVAL_HUNTER_BONES), "rival hunter bones " + b);
        assertTrue(b.size() >= AllegianceAssets.RIVAL_HUNTER_MIN_BONES, "rival hunter has only " + b.size() + " bones");
        for (int v = 0; v < AllegianceAssets.RIVAL_HUNTER_VARIANTS; v++) {
            assertAtlas(AllegianceAssets.RIVAL_HUNTER_GEO, AllegianceAssets.RIVAL_HUNTER_TEXTURE.formatted(v));
        }
        assertClips(AllegianceAssets.RIVAL_HUNTER_ANIM, "animation.rival_hunter.", AllegianceAssets.RIVAL_HUNTER_CLIPS,
                Set.of("idle", "walk", "run"), Set.of("aim"));
    }

    @Test
    void wingsHaveBothLooksAndFreeMountRoots() throws IOException {
        assertTrue(bones(AllegianceAssets.WINGS_GEO).containsAll(AllegianceAssets.WINGS_BONES));
        for (String tex : List.of(AllegianceAssets.WINGS_SHADOW_TEXTURE, AllegianceAssets.WINGS_LIGHT_TEXTURE, AllegianceAssets.WINGS_LIGHT_GLOW)) {
            assertAtlas(AllegianceAssets.WINGS_GEO, tex);
        }
        assertClips(AllegianceAssets.WINGS_ANIM, "animation.allegiance_wings.", AllegianceAssets.WINGS_CLIPS,
                Set.of("rest", "flap", "glide"), Set.of("fold", "open"));
        Set<String> keyed = keyedBones(AllegianceAssets.WINGS_ANIM);
        for (String root : AllegianceAssets.WINGS_BONES) assertFalse(keyed.contains(root), "a clip keys the mount root " + root);
    }

    @Test
    void regaliaCloakIsProcedural() throws IOException {
        assertTrue(bones(AllegianceAssets.REGALIA_GEO).containsAll(AllegianceAssets.REGALIA_BONES));
        assertAtlas(AllegianceAssets.REGALIA_GEO, AllegianceAssets.REGALIA_TEXTURE);
        assertAtlas(AllegianceAssets.REGALIA_GEO, AllegianceAssets.REGALIA_GLOW);
        // No allegiance clip may key the cloak segments (the renderer swings them).
        for (String file : List.of(AllegianceAssets.MESSENGER_ANIM, AllegianceAssets.RIVAL_HUNTER_ANIM, AllegianceAssets.WINGS_ANIM,
                AllegianceAssets.TRUE_FORM_ANIM)) {
            Set<String> keyed = keyedBones(file);
            for (String bone : AllegianceAssets.REGALIA_BONES) {
                if (bone.startsWith("cloak_")) assertFalse(keyed.contains(bone), file + " keys " + bone);
            }
        }
    }

    @Test
    void trueFormClips() throws IOException {
        assertAtlas(AllegianceAssets.TRUE_FORM_GEO, AllegianceAssets.TRUE_FORM_TEXTURE);
        assertClips(AllegianceAssets.TRUE_FORM_ANIM, "animation.true_form.", AllegianceAssets.TRUE_FORM_CLIPS, Set.of("burn"), Set.of());
    }

    @Test
    void eyeOverlaysUseTheSkinLayout() throws IOException {
        for (String eyes : AllegianceAssets.EYES) assertSize(AllegianceAssets.EYES_TEXTURE.formatted(eyes), 64, 64);
    }

    @Test
    void interfaceTexturesAtTheirContractSizes() throws IOException {
        for (String side : List.of("angel", "demon", "hunter")) {
            assertSize(AllegianceAssets.EMBLEM.formatted(side), 32, 32);
            assertSize(AllegianceAssets.TITLE_CARD.formatted(side), 256, 64);
        }
        assertSize(AllegianceAssets.HUD_RING, 64, 32);
        assertSize(AllegianceAssets.WHEEL, 256, 256);
        for (Power p : Power.values()) assertSize(AllegianceAssets.POWER_ICON.formatted(p.id()), 24, 24);
    }

    @Test
    void itemsFireAndSounds() throws IOException {
        for (String item : AllegianceAssets.ITEMS) assertSize("textures/item/" + item + ".png", 16, 16);
        for (String fire : AllegianceAssets.FIRE_TEXTURES) {
            int[] s = pngSize("textures/block/" + fire + ".png");
            assertEquals(16, s[0], fire + " width");
            assertEquals(0, s[1] % 16, fire + " must be a strip of 16x16 frames");
            assertTrue(Files.exists(ASSETS.resolve("textures/block/" + fire + ".png.mcmeta")), fire + " has no animation");
        }
        for (String sound : AllegianceAssets.SOUNDS) {
            assertTrue(Files.exists(ASSETS.resolve("sounds/allegiance/" + sound + ".ogg")), "missing sound " + sound);
        }
    }
}
