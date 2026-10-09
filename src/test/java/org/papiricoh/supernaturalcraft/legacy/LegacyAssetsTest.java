package org.papiricoh.supernaturalcraft.legacy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.legacy.research.ArchiveLore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Men of Letters' art (tools/artgen legacy_art, legacy_items_art; tools/soundgen legacy_sfx) against {@link LegacyAssets}:
 * every creature's rig, texture and clip, the bones the renderers toggle, the sounds, the item and block art, and the Archive's
 * fixed pages (generated) for every lore topic and every great enemy. Pure file checks.
 */
class LegacyAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");
    private static final Path GENERATED = Path.of("src/generated/resources/assets/supernaturalcraft");

    private static JsonObject read(Path p) throws IOException {
        assertTrue(Files.exists(p), "missing " + p);
        return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
    }

    private static Set<String> bones(String creature) throws IOException {
        JsonObject geo = read(ASSETS.resolve("geo/entity/" + creature + ".geo.json")).getAsJsonArray("minecraft:geometry").get(0)
                .getAsJsonObject();
        Set<String> out = new HashSet<>();
        for (JsonElement b : geo.getAsJsonArray("bones")) out.add(b.getAsJsonObject().get("name").getAsString());
        return out;
    }

    @Test
    void everyCreatureHasItsRigTextureAndClips() throws IOException {
        for (String c : LegacyAssets.CREATURES) {
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/" + c + ".png")), "no texture for " + c);
            assertTrue(bones(c).size() >= 20, c + " needs a rig worth animating: " + bones(c).size() + " bones");
            JsonObject anims = read(ASSETS.resolve("animations/entity/" + c + ".animation.json")).getAsJsonObject("animations");
            for (String clip : LegacyAssets.CLIPS.get(c)) {
                String name = "animation." + c + "." + clip;
                assertTrue(anims.has(name), c + " has no clip " + clip);
                JsonObject a = anims.getAsJsonObject(name);
                boolean loops = a.has("loop") && a.get("loop").isJsonPrimitive() && a.get("loop").getAsJsonPrimitive().isBoolean()
                        && a.get("loop").getAsBoolean();
                assertEquals(LegacyAssets.LOOPS.get(c).contains(clip), loops, c + "." + clip + " loops: " + loops);
            }
            assertTrue(LegacyAssets.CLIPS.get(c).containsAll(LegacyAssets.LOOPS.get(c)), c + ": a loop that is not a clip");
        }
    }

    @Test
    void theToggledBonesExist() throws IOException {
        for (var e : LegacyAssets.TOGGLED_BONES.entrySet()) {
            Set<String> bones = bones(e.getKey());
            for (String b : e.getValue()) assertTrue(bones.contains(b), e.getKey() + " has no bone " + b);
        }
        // The werewolf's two forms each carry a whole body; the shapeshifter's shed skin covers its hands too.
        Set<String> wolf = bones("werewolf");
        for (String b : List.of("h_body", "h_head", "w_body", "w_head", "w_jaw")) assertTrue(wolf.contains(b), "werewolf has no " + b);
        Set<String> shifter = bones("shapeshifter");
        for (String b : List.of("shed_skin_body", "shed_skin_right_hand", "shed_skin_left_hand")) {
            assertTrue(shifter.contains(b), "shapeshifter has no " + b);
        }
        assertTrue(bones("vampire").contains("fang_row"), "the vampire's fangs slide on fang_row");
        assertTrue(bones("henry_winchester").containsAll(List.of("hat", "briefcase", "right_index")), "Henry tips his hat and points");
    }

    @Test
    void everySoundHasItsFile() {
        for (String s : LegacyAssets.SOUNDS) {
            assertTrue(Files.exists(ASSETS.resolve("sounds/legacy/" + s + ".ogg")), "no sound file for " + s);
        }
    }

    @Test
    void theItemsAndBlocksHaveTheirArt() {
        for (String form : LegacyAssets.ARTIFACT_FORMS) {
            assertTrue(Files.exists(ASSETS.resolve("textures/item/artifact_" + form + ".png")), "no icon for the " + form);
            assertTrue(Files.exists(ASSETS.resolve("textures/item/artifact_" + form + "_aura.png")), "no halo for the " + form);
        }
        for (String topic : LegacyAssets.NOTE_TOPICS) {
            assertTrue(Files.exists(ASSETS.resolve("textures/item/field_notes_" + topic + ".png")), "no icon for " + topic + " notes");
        }
        for (String item : List.of("bunker_key", "case_file", "dead_mans_blood", "men_of_letters_ring", "spellwrights_spectacles",
                "henrys_case", "aquarian_star", "bunker_door")) {
            assertTrue(Files.exists(ASSETS.resolve("textures/item/" + item + ".png")), "no icon for " + item);
        }
        for (String block : List.of("research_desk", "map_table", "archive_shelf", "men_of_letters_emblem")) {
            assertTrue(Files.exists(ASSETS.resolve("models/block/" + block + ".json")), "no model for " + block);
        }
        for (String tex : List.of("block/bunker_door_top", "block/bunker_door_bottom", "map/decorations/bunker", "map/decorations/case_site",
                "models/armor/spellwrights_spectacles_layer_1")) {
            assertTrue(Files.exists(ASSETS.resolve("textures/" + tex + ".png")), "no texture " + tex);
        }
    }

    @Test
    void theArchiveHasAPageForEveryLoreTopicAndEveryEnemy() {
        Path entries = GENERATED.resolve("journal/entries");
        assertTrue(Files.isDirectory(entries), "no journal entries: run runData");
        assertFalse(ArchiveLore.ALL.isEmpty());
        for (ArchiveLore.Lore lore : ArchiveLore.ALL) {
            assertTrue(Files.exists(entries.resolve("archive_lore_" + lore.id() + ".json")), "no Archive page for lore " + lore.id());
        }
        for (BossProgression.Boss boss : BossProgression.Boss.values()) {
            assertTrue(Files.exists(entries.resolve("archive_boss_" + boss.id() + ".json")), "no Archive page for " + boss);
        }
    }
}
