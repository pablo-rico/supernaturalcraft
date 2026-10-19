package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchAnimations;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchAssets;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchCinematics;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Grand Witch's art (tools/artgen grand_witch_art) against {@link GrandWitchAssets} (v0.21): a detailed rig with her staff
 * and its crystal, every clip looping or not as the contract says, at its length, landing in time; the Coven Staff's model,
 * texture and icon; P6, the boss in use: her sounds and cameras (as long as the code waits), the clips the code triggers (hers,
 * the Grand Witch at peace's), her bar and titles; the Coven Staff's clips, glow and item model.
 */
class GrandWitchAssetsTest {

    private static final String PREFIX = "animation.grand_witch.";

    @Test
    void aDetailedRigWithEveryContractBone() throws IOException {
        Set<String> b = CovenArt.bones(GrandWitchAssets.GEO);
        assertTrue(b.size() >= GrandWitchAssets.MIN_BONES, "the Grand Witch has only " + b.size() + " bones");
        for (String bone : GrandWitchAssets.BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        CovenArt.assertAtlas(GrandWitchAssets.GEO, GrandWitchAssets.TEXTURE);
        CovenArt.assertAtlas(GrandWitchAssets.GEO, GrandWitchAssets.GLOW);
    }

    @Test
    void everyClipExistsLoopsOrPlaysOnceAndFitsItsTiming() throws IOException {
        JsonObject a = CovenArt.anims(GrandWitchAssets.ANIM);
        for (String clip : GrandWitchAssets.CLIPS) {
            String name = PREFIX + clip;
            assertTrue(a.has(name), "missing clip " + name);
            boolean loop = CovenArt.loops(a.getAsJsonObject(name));
            assertEquals(GrandWitchAssets.LOOPS.contains(clip), loop, name + (loop ? " must not loop" : " must loop"));
        }
        assertEquals("hold_on_last_frame", CovenArt.loopMode(a.getAsJsonObject(PREFIX + "death")), "she holds her death pose");
        for (var e : GrandWitchAssets.CLIP_TICKS.entrySet()) {
            assertEquals((int) e.getValue(), CovenArt.ticks(a.getAsJsonObject(PREFIX + e.getKey())), e.getKey() + " length");
        }
        for (var e : GrandWitchAssets.HIT_TICKS.entrySet()) {
            assertTrue(e.getValue() <= GrandWitchAssets.CLIP_TICKS.get(e.getKey()), e.getKey() + " lands after it ends");
        }
        assertTrue(GrandWitchAssets.CLIP_TICKS.keySet().containsAll(GrandWitchAssets.TRIGGERED), "every triggered clip has its length");
        Set<String> bones = CovenArt.bones(GrandWitchAssets.GEO);
        for (var clip : a.entrySet()) {
            for (String bone : CovenArt.keyed(clip.getValue().getAsJsonObject())) {
                assertTrue(bones.contains(bone), clip.getKey() + " keys an unknown bone " + bone);
            }
        }
    }

    @Test
    void theCovenStaff() throws IOException {
        Set<String> staff = CovenArt.bones(GrandWitchAssets.STAFF_GEO);
        assertTrue(staff.contains("staff") && staff.contains("crystal"), "the staff and its crystal: " + staff);
        CovenArt.assertAtlas(GrandWitchAssets.STAFF_GEO, GrandWitchAssets.STAFF_TEXTURE);
        int[] icon = CovenArt.pngSize(GrandWitchAssets.STAFF_ICON);
        assertEquals(16, icon[0]);
        assertEquals(16, icon[1]);
        JsonObject anims = CovenArt.anims("animations/item/coven_staff.animation.json");
        assertTrue(anims.size() > 0, "the staff has its clips");
    }

    // --- the boss in use (P6) ---------------------------------------------------------------------------------------------

    @Test
    void herSoundsCamerasAndWords() throws IOException {
        for (String s : GrandWitchAssets.SOUNDS) {
            assertTrue(CovenArt.soundDefined("grand_witch." + s), "no sound grand_witch." + s);
            assertTrue(Files.exists(CovenArt.ASSETS.resolve("sounds/grand_witch/" + s + ".ogg")), "no file sounds/grand_witch/" + s + ".ogg");
        }
        Map<String, Integer> lengths = Map.of("grand_witch_intro", GrandWitchCinematics.INTRO_TICKS, "grand_witch_p2", GrandWitchCinematics.BREAK_TICKS,
                "grand_witch_p3", GrandWitchCinematics.LAST_TICKS, "grand_witch_death", GrandWitchCinematics.DEATH_TICKS);
        assertEquals(Set.copyOf(GrandWitchAssets.CINEMATICS), lengths.keySet());
        for (var e : lengths.entrySet()) {
            JsonObject cin = CovenArt.read("cinematics/" + e.getKey() + ".json");
            int total = 0;
            for (var shot : cin.getAsJsonArray("shots")) total += shot.getAsJsonObject().get("duration").getAsInt();
            assertEquals((int) e.getValue(), total, e.getKey() + ": the camera's length");
        }
        assertEquals(GrandWitchEntity.INTRO_TICKS, GrandWitchCinematics.INTRO_TICKS);
        JsonObject lang = JsonParser.parseString(Files.readString(Path.of("src/generated/resources/assets/supernaturalcraft/lang/en_us.json")))
                .getAsJsonObject();
        for (int phase = 1; phase <= GrandWitchEntity.MAX_PHASE; phase++) {
            assertTrue(lang.has("entity.supernaturalcraft.grand_witch.bar.phase" + phase), "her bar's name in phase " + phase);
            assertTrue(lang.has("title.supernaturalcraft.grand_witch.phase" + phase), "her title card in phase " + phase);
        }
        for (String key : List.of("entity.supernaturalcraft.grand_witch", "entity.supernaturalcraft.grand_witch_npc",
                "item.supernaturalcraft.coven_staff", "block.supernaturalcraft.grand_witch_trophy",
                "message.supernaturalcraft.grand_witch.shield_down", "message.supernaturalcraft.grand_witch.rowena_arrives",
                "message.supernaturalcraft.grand_witch.rowena_thanks", "message.supernaturalcraft.grand_witch.arrives",
                "message.supernaturalcraft.grand_witch.busy", "message.supernaturalcraft.book_choice.given",
                "message.supernaturalcraft.book_choice.kept", "message.supernaturalcraft.unmark.done",
                "message.supernaturalcraft.unmark.not_marked", "message.supernaturalcraft.unmark.no_survivor",
                "message.supernaturalcraft.unmark.needs_book", "message.supernaturalcraft.unmark.needs_blood",
                "message.supernaturalcraft.unmark.no_survivor_near", "message.supernaturalcraft.ritual.needs_mark",
                "message.supernaturalcraft.book_of_the_damned.coven_waits", "bowl_spell.supernaturalcraft.unmark",
                "jei.supernaturalcraft.effect.supernaturalcraft.call_the_grand_coven", "cinematic.supernaturalcraft.grand_witch.victory.title")) {
            assertTrue(lang.has(key) && !lang.get(key).getAsString().isBlank(), "no text for " + key);
        }
    }

    @Test
    void theCodeTriggersOnlyClipsThatExist() throws IOException {
        JsonObject a = CovenArt.anims(GrandWitchAssets.ANIM);
        assertEquals(GrandWitchAssets.TRIGGERED, GrandWitchAnimations.TRIGGERED);
        assertEquals(List.of("frost_hex", "inferno", "crush", "mass_taglock", "smoke_veil", "death"), GrandWitchAssets.TRIGGERED,
                "her one-shot clips (at peace she uses smoke_veil and inferno too)");
        for (String clip : GrandWitchAnimations.TRIGGERED) assertTrue(a.has(PREFIX + clip), "the code triggers a missing clip " + clip);
        assertTrue(CovenArt.loops(a.getAsJsonObject(PREFIX + "shield")), "the shield loop runs while her coven channels");
        assertEquals(GrandWitchAssets.HIT_TICKS.keySet(), GrandWitchAssets.CLIP_TICKS.keySet());
    }

    @Test
    void theCovenStaffInHand() throws IOException {
        JsonObject anims = CovenArt.anims("animations/item/coven_staff.animation.json");
        String idle = "animation.coven_staff." + GrandWitchAnimations.STAFF_IDLE, cast = "animation.coven_staff." + GrandWitchAnimations.STAFF_CAST;
        assertTrue(anims.has(idle) && anims.has(cast), "the staff's idle and cast: " + anims.keySet());
        assertTrue(CovenArt.loops(anims.getAsJsonObject(idle)), "its crystal idles in a loop");
        assertFalse(CovenArt.loops(anims.getAsJsonObject(cast)), "its cast plays once");
        Set<String> staff = CovenArt.bones(GrandWitchAssets.STAFF_GEO);
        for (var clip : anims.entrySet()) {
            for (String bone : CovenArt.keyed(clip.getValue().getAsJsonObject())) assertTrue(staff.contains(bone), clip.getKey() + " keys " + bone);
        }
        CovenArt.assertAtlas(GrandWitchAssets.STAFF_GEO, "textures/item/coven_staff_glowmask.png");
        assertTrue(Files.exists(Path.of("src/generated/resources/assets/supernaturalcraft/models/item/coven_staff.json")), "no item model for the staff");
        assertTrue(Files.exists(Path.of("src/generated/resources/assets/supernaturalcraft/models/item/rowenas_grimoire.json")), "no item model for the grimoire");
        String profiles = Files.readString(Path.of("src/main/resources/data/supernaturalcraft/data_maps/item/weapon_profile.json"));
        assertTrue(profiles.contains("supernaturalcraft:coven_staff"), "the staff has its weapon profile");
        assertTrue(profiles.contains("supernaturalcraft:rowenas_grimoire"), "the grimoire has its weapon profile");
    }
}
