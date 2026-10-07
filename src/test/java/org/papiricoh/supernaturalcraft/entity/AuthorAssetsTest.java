package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Author's art beyond Chuck himself: ink echoes, items, blocks and the typewriter model, particles, and sounds. */
class AuthorAssetsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/supernaturalcraft");
    private static final Path GENERATED = Path.of("src/generated/resources/assets/supernaturalcraft");

    /** Each ink echo and the boss texture it is recoloured from (tools/artgen/ink_echo_art.py). */
    private static final Map<String, String> ECHOES = Map.of(
            "lucifer", "lucifer_p1", "azazel", "azazel_p1", "lilith", "lilith_p1", "metatron", "metatron_p1",
            "lucifer_uncaged", "lucifer_uncaged_p1", "amara", "amara", "broken_chorus", "broken_chorus");

    /** Width and height from a PNG's IHDR chunk. */
    private static int[] size(Path png) throws IOException {
        try (InputStream in = Files.newInputStream(png); DataInputStream data = new DataInputStream(in)) {
            data.skipNBytes(16);
            return new int[]{data.readInt(), data.readInt()};
        }
    }

    private static void exists(String path) {
        assertTrue(Files.exists(ASSETS.resolve(path)), "missing " + path);
    }

    @Test
    void inkEchoesMatchTheirBossTexture() throws IOException {
        for (var e : ECHOES.entrySet()) {
            Path ink = ASSETS.resolve("textures/entity/ink/" + e.getKey() + ".png");
            assertTrue(Files.exists(ink), "missing ink echo " + e.getKey());
            exists("textures/entity/ink/" + e.getKey() + "_glowmask.png");
            assertArrayEquals(size(ASSETS.resolve("textures/entity/" + e.getValue() + ".png")), size(ink), e.getKey() + " size");
        }
    }

    @Test
    void itemsBlocksAndParticlesHaveArt() {
        for (String item : new String[]{"the_end_manuscript", "authors_pen", "sams_amulet"}) exists("textures/item/" + item + ".png");
        exists("textures/map/decorations/author_cabin.png");
        for (String block : new String[]{"page_block", "ink_block", "burning_ink"}) exists("textures/block/" + block + ".png");
        for (int i = 0; i < 8; i++) exists("textures/particle/ink_letter_" + i + ".png");
        for (int i = 0; i < 4; i++) {
            exists("textures/particle/page_scrap_" + i + ".png");
            exists("textures/particle/golden_mote_" + i + ".png");
        }
    }

    @Test
    void typewriterModelTexturesExist() throws IOException {
        JsonObject model = JsonParser.parseString(Files.readString(ASSETS.resolve("models/block/typewriter.json"))).getAsJsonObject();
        assertTrue(model.getAsJsonArray("elements").size() > 20, "the typewriter should be a real model");
        for (Map.Entry<String, JsonElement> t : model.getAsJsonObject("textures").entrySet()) {
            String ref = t.getValue().getAsString();
            assertTrue(ref.startsWith("supernaturalcraft:block/"), ref);
            exists("textures/" + ref.substring("supernaturalcraft:".length()) + ".png");
        }
    }

    @Test
    void everyAuthorSoundFileExists() throws IOException {
        JsonObject sounds = JsonParser.parseString(Files.readString(GENERATED.resolve("sounds.json"))).getAsJsonObject();
        int files = 0;
        for (Map.Entry<String, JsonElement> event : sounds.entrySet()) {
            for (JsonElement s : event.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                String name = s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString();
                boolean isEvent = s.isJsonObject() && s.getAsJsonObject().has("type")
                        && "event".equals(s.getAsJsonObject().get("type").getAsString());
                if (isEvent || !name.startsWith("supernaturalcraft:chuck/")) continue;
                exists("sounds/" + name.substring("supernaturalcraft:".length()) + ".ogg");
                files++;
            }
        }
        assertTrue(files >= 20, "expected the Author's sounds in sounds.json, found " + files);
        assertEquals(true, sounds.has("music.chuck"));
    }
}
