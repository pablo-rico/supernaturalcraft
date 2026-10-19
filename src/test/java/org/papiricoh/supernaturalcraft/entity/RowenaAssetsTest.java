package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaAnimations;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaAssets;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaCinematics;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rowena's art (tools/artgen rowena_art) against {@link RowenaAssets} (v0.21): one rig for the boss and the woman at home; a
 * detailed model with every bone the code reads; every boss and NPC clip, looping or not as the contract says, at its length,
 * landing in time; the open grimoire shown only while she recites or reads (and hidden in every other clip, since GeckoLib puts
 * an unkeyed bone back to rest); the closed grimoire never keyed (the renderer hides it); her sounds and spoils. P6, the boss in
 * use: the clips the code triggers, her cameras as long as the code waits for them, her bar, titles and messages written.
 */
class RowenaAssetsTest {

    private static final String PREFIX = "animation.rowena.";

    @Test
    void oneDetailedRigWithEveryContractBone() throws IOException {
        Set<String> b = CovenArt.bones(RowenaAssets.GEO);
        assertTrue(b.size() >= RowenaAssets.MIN_BONES, "Rowena has only " + b.size() + " bones");
        for (String bone : RowenaAssets.BONES) assertTrue(b.contains(bone), "missing bone " + bone);
        for (String prop : RowenaAssets.PROPS.keySet()) assertTrue(b.contains(prop), "missing prop " + prop);
        for (String bone : RowenaAssets.PROCEDURAL) assertTrue(b.contains(bone), "missing renderer bone " + bone);
        CovenArt.assertAtlas(RowenaAssets.GEO, RowenaAssets.TEXTURE);
        CovenArt.assertAtlas(RowenaAssets.GEO, RowenaAssets.GLOW);
    }

    @Test
    void everyClipExistsLoopsOrPlaysOnceAndFitsItsTiming() throws IOException {
        JsonObject a = CovenArt.anims(RowenaAssets.ANIM);
        Set<String> all = new HashSet<>(RowenaAssets.CLIPS);
        all.addAll(RowenaAssets.NPC_CLIPS);
        Set<String> loops = new HashSet<>(RowenaAssets.LOOPS);
        loops.addAll(RowenaAssets.NPC_LOOPS);
        for (String clip : all) {
            String name = PREFIX + clip;
            assertTrue(a.has(name), "missing clip " + name);
            boolean loop = CovenArt.loops(a.getAsJsonObject(name));
            assertEquals(loops.contains(clip), loop, name + (loop ? " must not loop" : " must loop"));
        }
        assertEquals("hold_on_last_frame", CovenArt.loopMode(a.getAsJsonObject(PREFIX + "death")), "she holds her death pose");
        for (var e : RowenaAssets.CLIP_TICKS.entrySet()) {
            assertEquals((int) e.getValue(), CovenArt.ticks(a.getAsJsonObject(PREFIX + e.getKey())), e.getKey() + " length");
        }
        for (var e : RowenaAssets.HIT_TICKS.entrySet()) {
            assertTrue(e.getValue() <= RowenaAssets.CLIP_TICKS.get(e.getKey()), e.getKey() + " lands after it ends");
        }
        assertTrue(RowenaAssets.CLIP_TICKS.keySet().containsAll(RowenaAssets.TRIGGERED), "every triggered boss clip has its length");
    }

    @Test
    void theOpenGrimoireShowsOnlyWhileSheRecitesOrReads() throws IOException {
        JsonObject a = CovenArt.anims(RowenaAssets.ANIM);
        for (var prop : RowenaAssets.PROPS.entrySet()) {
            List<String> shown = prop.getValue();
            for (String clip : shown) assertTrue(a.has(PREFIX + clip), prop.getKey() + " shows in an unknown clip " + clip);
            for (var clip : a.entrySet()) {
                String name = clip.getKey().substring(PREFIX.length());
                Set<Double> scales = CovenArt.scales(clip.getValue().getAsJsonObject(), prop.getKey());
                if (shown.contains(name)) {
                    assertTrue(scales.stream().allMatch(s -> s >= 0.5), clip.getKey() + " hides " + prop.getKey() + " while it should show: " + scales);
                } else {
                    assertFalse(scales.isEmpty(), clip.getKey() + " must scale " + prop.getKey() + " away (GeckoLib shows an unkeyed bone)");
                    assertTrue(scales.stream().allMatch(s -> s <= 0.05), clip.getKey() + " shows " + prop.getKey() + ": " + scales);
                }
            }
        }
    }

    @Test
    void clipsKeyOnlyRealBonesAndNeverTheOnesTheRendererHides() throws IOException {
        Set<String> b = CovenArt.bones(RowenaAssets.GEO);
        JsonObject a = CovenArt.anims(RowenaAssets.ANIM);
        for (var clip : a.entrySet()) {
            for (String bone : CovenArt.keyed(clip.getValue().getAsJsonObject())) {
                assertTrue(b.contains(bone), clip.getKey() + " keys an unknown bone " + bone);
                assertFalse(RowenaAssets.PROCEDURAL.contains(bone), clip.getKey() + " keys " + bone + ", which only the renderer may touch");
            }
        }
    }

    @Test
    void herSpoilsAndSounds() throws IOException {
        int[] icon = CovenArt.pngSize(RowenaAssets.GRIMOIRE_ICON);
        assertEquals(16, icon[0]);
        assertEquals(16, icon[1]);
        int[] card = CovenArt.pngSize("textures/item/rowenas_calling_card.png");
        assertEquals(16, card[0]);
        for (String s : RowenaAssets.SOUNDS) assertTrue(CovenArt.soundDefined("rowena." + s), "no sound rowena." + s);
    }

    // --- the boss in use (P6) ---------------------------------------------------------------------------------------------

    @Test
    void theBossPlaysOnlyHerOwnClipsAndHoldsHerDeath() throws IOException {
        JsonObject a = CovenArt.anims(RowenaAssets.ANIM);
        assertEquals(RowenaAssets.TRIGGERED, RowenaAnimations.TRIGGERED);
        assertEquals(RowenaAssets.LOOPS, RowenaAnimations.LOOPS);
        assertEquals(List.of("fling", "ignis", "glacies", "summon_victims", "teleport", "death"), RowenaAssets.TRIGGERED,
                "the boss's one-shot clips (and Rowena at the hunters' side: fling, glacies, teleport)");
        for (String clip : RowenaAnimations.TRIGGERED) assertTrue(a.has(PREFIX + clip), "the code triggers a missing clip " + clip);
        for (String loop : RowenaAnimations.LOOPS) assertTrue(CovenArt.loops(a.getAsJsonObject(PREFIX + loop)), loop + " must loop");
        assertEquals(RowenaAssets.HIT_TICKS.keySet(), RowenaAssets.CLIP_TICKS.keySet(), "every timed clip has a moment and a length");
    }

    @Test
    void herCamerasLastWhatTheCodeWaitsForAndHerWordsAreWritten() throws IOException {
        Map<String, Integer> lengths = Map.of("rowena_intro", RowenaCinematics.INTRO_TICKS, "rowena_p2", RowenaCinematics.BREAK_TICKS,
                "rowena_p3", RowenaCinematics.LAST_TICKS, "rowena_death", RowenaCinematics.DEATH_TICKS);
        assertEquals(Set.copyOf(RowenaAssets.CINEMATICS), lengths.keySet());
        for (var e : lengths.entrySet()) {
            JsonObject cin = CovenArt.read("cinematics/" + e.getKey() + ".json");
            int total = 0;
            for (var shot : cin.getAsJsonArray("shots")) total += shot.getAsJsonObject().get("duration").getAsInt();
            assertEquals((int) e.getValue(), total, e.getKey() + ": the camera's length");
        }
        assertEquals(RowenaEntity.INTRO_TICKS, RowenaCinematics.INTRO_TICKS, "she emerges for as long as her intro camera runs");
        for (String s : RowenaAssets.SOUNDS) {
            assertTrue(Files.exists(CovenArt.ASSETS.resolve("sounds/rowena/" + s + ".ogg")), "no file sounds/rowena/" + s + ".ogg");
        }
        JsonObject lang = JsonParser.parseString(Files.readString(Path.of("src/generated/resources/assets/supernaturalcraft/lang/en_us.json")))
                .getAsJsonObject();
        for (int phase = 1; phase <= RowenaEntity.MAX_PHASE; phase++) {
            assertTrue(lang.has("entity.supernaturalcraft.rowena.bar.phase" + phase), "her bar's name in phase " + phase);
            assertTrue(lang.has("title.supernaturalcraft.rowena.phase" + phase), "her title card in phase " + phase);
        }
        for (String key : List.of("entity.supernaturalcraft.rowena", "entity.supernaturalcraft.rowena_ally", "entity.supernaturalcraft.recitation_bowl",
                "item.supernaturalcraft.rowenas_grimoire", "block.supernaturalcraft.rowena_trophy", "tooltip.supernaturalcraft.rowenas_grimoire",
                "message.supernaturalcraft.rowena.recitation", "message.supernaturalcraft.rowena.recitation_broken",
                "message.supernaturalcraft.rowena.victim_killed", "message.supernaturalcraft.rowena.victim_freed",
                "message.supernaturalcraft.rowena.challenge.not_kept", "message.supernaturalcraft.rowena.challenge.not_here",
                "message.supernaturalcraft.rowena.challenge.not_night", "message.supernaturalcraft.rowena.challenge.busy",
                "message.supernaturalcraft.rowenas_grimoire.empty", "message.supernaturalcraft.rowenas_grimoire.forgotten",
                "cinematic.supernaturalcraft.rowena.victory.title")) {
            assertTrue(lang.has(key) && !lang.get(key).getAsString().isBlank(), "no text for " + key);
        }
    }
}
