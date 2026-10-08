package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelAnimations;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelBones;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Archangel Michael's art (tools/artgen michael_art, michael_archangel_art, host_angel_art, michael_lance_art,
 * general_armor_art, michael_items_art, michael_gui_art; tools/soundgen michael_sfx) against the michael contract: every
 * model, clip, texture, HUD texture (at its contract size) and sound the code loads, the bones it needs, and the
 * procedural bones no clip may key. Pure file checks: no registry or entity class is loaded.
 */
class MichaelAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");
    private static final Path GENERATED = Path.of("src/generated/resources/assets/supernaturalcraft");

    private static JsonObject read(Path root, String path) throws IOException {
        return JsonParser.parseString(Files.readString(root.resolve(path))).getAsJsonObject();
    }

    private static Map<String, JsonObject> bones(String geo) throws IOException {
        Map<String, JsonObject> out = new HashMap<>();
        read(ASSETS, geo).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").forEach(b -> out.put(b.getAsJsonObject().get("name").getAsString(), b.getAsJsonObject()));
        return out;
    }

    private static JsonObject anims(String file) throws IOException {
        return read(ASSETS, file).getAsJsonObject("animations");
    }

    /** Width and height from a PNG's IHDR. */
    private static int[] pngSize(Path p) throws IOException {
        try (InputStream in = Files.newInputStream(p)) {
            byte[] h = in.readNBytes(24);
            int w = ((h[16] & 0xff) << 24) | ((h[17] & 0xff) << 16) | ((h[18] & 0xff) << 8) | (h[19] & 0xff);
            int ht = ((h[20] & 0xff) << 24) | ((h[21] & 0xff) << 16) | ((h[22] & 0xff) << 8) | (h[23] & 0xff);
            return new int[]{w, ht};
        }
    }

    private record Form(String model, String prefix, List<String> clips, List<String> required) {
    }

    private static final List<Form> FORMS = List.of(
            new Form("michael", MichaelAnimations.VESSEL, MichaelAnimations.VESSEL_CLIPS, MichaelBones.VESSEL_REQUIRED),
            new Form("michael_archangel", MichaelAnimations.ARCHANGEL, MichaelAnimations.ARCHANGEL_CLIPS, MichaelBones.ARCHANGEL_REQUIRED),
            new Form("host_angel", MichaelAnimations.HOST, MichaelAnimations.HOST_CLIPS, MichaelBones.HOST_REQUIRED));

    @Test
    void everyContractClipExistsWithItsLoopMode() throws IOException {
        for (Form f : FORMS) {
            JsonObject anims = anims("animations/entity/" + f.model() + ".animation.json");
            for (String clip : f.clips()) {
                String name = f.prefix() + clip;
                assertTrue(anims.has(name), f.model() + " is missing " + name);
                JsonElement loop = anims.getAsJsonObject(name).get("loop");
                boolean loops = loop != null && loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isBoolean() && loop.getAsBoolean();
                boolean holds = loop != null && loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isString()
                        && loop.getAsString().equals("hold_on_last_frame");
                assertEquals(MichaelAnimations.LOOPS.contains(clip), loops, name + (loops ? " must not loop" : " must loop"));
                if (clip.equals("death") || clip.equals("die")) assertTrue(holds, name + " must hold its last frame");
            }
        }
        assertTrue(anims("animations/item/michael_lance.animation.json").has("animation.michael_lance.idle"));
        assertTrue(anims("animations/item/armor/general_armor.animation.json").has("animation.general_armor.idle"));
    }

    @Test
    void timedClipsHaveTheirContractLengths() throws IOException {
        JsonObject vessel = anims("animations/entity/michael.animation.json");
        assertEquals(6.0, vessel.getAsJsonObject(MichaelAnimations.VESSEL + "transform").get("animation_length").getAsDouble(), 1e-6);
        assertEquals(8.0, vessel.getAsJsonObject(MichaelAnimations.VESSEL + "death").get("animation_length").getAsDouble(), 1e-6);
        JsonObject arch = anims("animations/entity/michael_archangel.animation.json");
        assertEquals(8.0, arch.getAsJsonObject(MichaelAnimations.ARCHANGEL + "death").get("animation_length").getAsDouble(), 1e-6);
        // Every hit frame the code schedules on lies inside its clip.
        Map<String, Double> hits = Map.of("forehead_touch", 1.2, "blade_combo_1", 0.35, "lance_throw", 0.55, "lance_thrust", 0.45,
                "lance_sweep", 0.6, "dive", 0.9);
        for (var e : hits.entrySet()) {
            double len = vessel.getAsJsonObject(MichaelAnimations.VESSEL + e.getKey()).get("animation_length").getAsDouble();
            assertTrue(len > e.getValue(), e.getKey() + " ends before its hit frame");
        }
        assertTrue(arch.getAsJsonObject(MichaelAnimations.ARCHANGEL + "lance_sweep_big").get("animation_length").getAsDouble() > 0.6);
    }

    @Test
    void clipsTouchOnlyRealBonesAndNeverTheProceduralOnes() throws IOException {
        for (Form f : FORMS) {
            Map<String, JsonObject> bones = bones("geo/entity/" + f.model() + ".geo.json");
            JsonObject anims = anims("animations/entity/" + f.model() + ".animation.json");
            for (String clip : anims.keySet()) {
                for (String bone : anims.getAsJsonObject(clip).getAsJsonObject("bones").keySet()) {
                    assertTrue(bones.containsKey(bone), clip + " animates unknown bone " + bone);
                    assertFalse(MichaelBones.PROCEDURAL.contains(bone), clip + " keys " + bone + ", which the renderer drives");
                }
            }
        }
    }

    @Test
    void requiredBonesExist() throws IOException {
        for (Form f : FORMS) {
            Map<String, JsonObject> b = bones("geo/entity/" + f.model() + ".geo.json");
            for (String bone : f.required()) assertTrue(b.containsKey(bone), f.model() + " is missing bone " + bone);
        }
        Map<String, JsonObject> arch = bones("geo/entity/michael_archangel.geo.json");
        assertTrue(arch.size() >= MichaelBones.ARCHANGEL_MIN_BONES, "the archangel has only " + arch.size() + " bones");
        for (int i = 0; i < MichaelBones.HALO_SPEARS; i++) {
            String spear = MichaelBones.haloSpear(i);
            assertTrue(arch.containsKey(spear), "missing " + spear);
            assertEquals("halo_spin", arch.get(spear).get("parent").getAsString(), spear + " must turn with halo_spin");
        }
        for (String s : MichaelBones.BROKEN_SPEARS) assertTrue(arch.containsKey(s));
        for (int i = 1; i <= 4; i++) assertEquals("cape_" + (i - 1), arch.get("cape_" + i).get("parent").getAsString(), "the cape is a chain");
        for (int i = 0; i < 6; i++) assertTrue(arch.containsKey("fauld_" + i));
        assertTrue(arch.keySet().stream().filter(n -> n.startsWith("plate_")).count() >= 8, "too few plates fall in the death clip");
        for (String side : List.of("l", "r")) {
            for (int p = 1; p <= 3; p++) {
                String w = "wing_" + side + p;
                for (String part : List.of(w + "_fore", w + "_hand", w + "_p00", w + "_p11", w + "_s00", w + "_s07")) {
                    assertTrue(arch.containsKey(part), "missing " + part);
                }
            }
        }
        Map<String, JsonObject> vessel = bones("geo/entity/michael.geo.json");
        for (String side : List.of("l", "r")) {
            for (int p = 1; p <= 2; p++) {
                String wing = "wing_" + side + p;
                long feathers = vessel.keySet().stream().filter(n -> n.startsWith(wing + "_f")).count();
                assertTrue(feathers >= 16, wing + " has only " + feathers + " feathers");
            }
        }
        Map<String, JsonObject> lance = bones("geo/item/michael_lance.geo.json");
        assertTrue(lance.containsKey("lance"));
        Map<String, JsonObject> armour = bones("geo/item/armor/general_armor.geo.json");
        for (String b : List.of("armorHead", "armorBody", "armorRightArm", "armorLeftArm", "armorRightLeg", "armorLeftLeg",
                "armorRightBoot", "armorLeftBoot")) {
            assertTrue(armour.containsKey(b), "general armour is missing " + b);
        }
        for (String g : List.of("light_spear", "steel_feather")) assertTrue(Files.exists(ASSETS.resolve("geo/entity/" + g + ".geo.json")));
    }

    @Test
    void texturesExist() {
        List<String> entity = new ArrayList<>(List.of("michael", "michael_glowmask", "michael_archangel", "michael_archangel_cracked",
                "michael_archangel_glowmask", "michael_archangel_cracked_glowmask", "host_angel_0", "host_angel_1", "host_angel_2",
                "host_angel_captain", "host_angel_glowmask", "light_spear", "steel_feather"));
        for (String t : entity) assertTrue(Files.exists(ASSETS.resolve("textures/entity/" + t + ".png")), "missing texture " + t);
        for (String t : List.of("michael_lance", "michael_lance_glowmask", "michael_lance_borrowed", "michael_lance_icon", "borrowed_lance_icon",
                "michaels_grace", "general_helmet", "general_chestplate", "general_leggings", "general_boots", "armor/general_armor",
                "armor/general_armor_glowmask")) {
            assertTrue(Files.exists(ASSETS.resolve("textures/item/" + t + ".png")), "missing item texture " + t);
        }
        for (String e : List.of("vessel", "grace_favor", "heavens_mark")) {
            assertTrue(Files.exists(ASSETS.resolve("textures/mob_effect/" + e + ".png")), "missing effect icon " + e);
        }
        for (String p : List.of("steel_feather", "halo_ray")) {
            for (int i = 0; i < 4; i++) {
                assertTrue(Files.exists(ASSETS.resolve("textures/particle/" + p + "_" + i + ".png")), "missing particle " + p + "_" + i);
            }
        }
    }

    @Test
    void hudTexturesHaveTheirContractSizes() throws IOException {
        Map<String, int[]> sizes = Map.ofEntries(Map.entry("bar_frame", new int[]{256, 32}), Map.entry("bar_fill", new int[]{208, 64}),
                Map.entry("bar_wing", new int[]{64, 256}), Map.entry("feather", new int[]{16, 16}), Map.entry("enochian", new int[]{208, 16}),
                Map.entry("title_rays", new int[]{256, 256}), Map.entry("title_wings", new int[]{256, 512}),
                Map.entry("title_flourish", new int[]{256, 24}), Map.entry("yes_panel", new int[]{256, 160}), Map.entry("mark", new int[]{32, 32}),
                Map.entry("flight", new int[]{96, 16}));
        for (var e : sizes.entrySet()) {
            Path p = ASSETS.resolve("textures/gui/michael/" + e.getKey() + ".png");
            assertTrue(Files.exists(p), "missing HUD texture " + e.getKey());
            int[] wh = pngSize(p);
            assertEquals(e.getValue()[0], wh[0], e.getKey() + " width");
            assertEquals(e.getValue()[1], wh[1], e.getKey() + " height");
        }
    }

    @Test
    void trophyModelPointsAtRealTextures() throws IOException {
        JsonObject model = read(ASSETS, "models/block/michael_trophy.json");
        for (var e : model.getAsJsonObject("textures").entrySet()) {
            String ref = e.getValue().getAsString();
            String path = ref.substring(ref.indexOf(':') + 1);
            assertTrue(Files.exists(ASSETS.resolve("textures/" + path + ".png")), "michael_trophy: missing texture " + ref);
        }
    }

    @Test
    void everyMichaelSoundFileExists() throws IOException {
        Path defs = GENERATED.resolve("sounds.json");
        if (!Files.exists(defs)) return; // before the first runData
        JsonObject sounds = read(GENERATED, "sounds.json");
        int seen = 0;
        for (var e : sounds.entrySet()) {
            if (!(e.getKey().startsWith("entity.michael.") || e.getKey().startsWith("entity.host_angel.")
                    || e.getKey().equals("item.general_armor.ward") || e.getKey().equals("item.michaels_grace.flight"))) continue;
            boolean own = false;
            for (JsonElement s : e.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                String name = s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString();
                boolean event = s.isJsonObject() && s.getAsJsonObject().has("type") && s.getAsJsonObject().get("type").getAsString().equals("event");
                if (event || !name.startsWith("supernaturalcraft:")) continue;
                own = true;
                String path = name.substring(name.indexOf(':') + 1);
                assertTrue(Files.exists(ASSETS.resolve("sounds/" + path + ".ogg")), e.getKey() + ": missing sounds/" + path + ".ogg");
            }
            assertTrue(own, e.getKey() + " plays no sound of its own");
            seen++;
        }
        assertEquals(22, seen, "every Michael sound event is defined");
    }
}
