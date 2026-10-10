package org.papiricoh.supernaturalcraft.client.lucifer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.network.LuciferFxPayload;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** The Cage's HUD: its textures against the contract, its glyphs against every title and name, the bars' story. */
class LuciferGuiTest {

    private static final Path GUI = Path.of("src/main/resources/assets/supernaturalcraft/textures/gui/lucifer");
    private static final Path LANG = Path.of("src/generated/resources/assets/supernaturalcraft/lang/en_us.json");

    /** A PNG's width and height, from its header. */
    private static int[] size(String name) throws IOException {
        Path file = GUI.resolve(name + ".png");
        assertTrue(Files.exists(file), "missing " + file);
        try (InputStream in = Files.newInputStream(file); DataInputStream data = new DataInputStream(in)) {
            data.skipNBytes(16);
            return new int[]{data.readInt(), data.readInt()};
        }
    }

    private static void assertSize(String name, int w, int h) throws IOException {
        assertArrayEquals(new int[]{w, h}, size(name), name);
    }

    private static JsonObject glyphs() throws IOException {
        return JsonParser.parseString(Files.readString(GUI.resolve("glyphs.json"))).getAsJsonObject();
    }

    @Test
    void texturesMatchTheContract() throws IOException {
        assertSize("bar_frame", LuciferGui.FRAME_W, LuciferGui.FRAME_H);
        assertSize("bar_runes", LuciferGui.FRAME_W, LuciferGui.FRAME_H);
        assertSize("bar_bars", LuciferGui.BAR_W * CageBars.State.values().length, LuciferGui.BAR_H);
        assertSize("debris", 32, 8);
        assertSize("title_seal", 256, 256);
        assertSize("title_motifs", 512, 384);
        assertSize("title_chain", 256, 24);
        for (boolean uncaged : new boolean[]{false, true}) {
            for (int p = 1; p <= LuciferGui.maxPhase(uncaged); p++) {
                assertSize("fill_" + LuciferGui.style(uncaged, p).fill(), LuciferGui.FILL_W, LuciferGui.FILL_H * LuciferGui.FILL_FRAMES);
            }
        }
        JsonObject g = glyphs();
        int count = g.get("chars").getAsString().length();
        for (String atlas : new String[]{"large", "small"}) {
            JsonObject a = g.getAsJsonObject(atlas);
            int cw = a.getAsJsonArray("cell").get(0).getAsInt(), ch = a.getAsJsonArray("cell").get(1).getAsInt(), cols = a.get("cols").getAsInt();
            assertSize("glyphs_" + atlas, cw * cols, ch * ((count + cols - 1) / cols));
            assertEquals(count, a.getAsJsonArray("advance").size(), atlas);
        }
    }

    @Test
    void theGlyphsWriteEveryTitleAndName() throws IOException {
        String chars = glyphs().get("chars").getAsString();
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        List<String> texts = new ArrayList<>();
        for (boolean uncaged : new boolean[]{false, true}) {
            int max = LuciferGui.maxPhase(uncaged);
            List<Integer> cards = new ArrayList<>(List.of(LuciferFxPayload.TITLE_INTRO, LuciferFxPayload.TITLE_VICTORY));
            for (int p = 2; p <= max; p++) cards.add(p);
            for (int which : cards) {
                String key = LuciferGui.titleKey(uncaged, which) + ".title";
                assertTrue(lang.has(key), "no title " + key);
                texts.add(lang.get(key).getAsString());
            }
            for (int p = 1; p <= max; p++) {
                String key = (uncaged ? LuciferHud.UNCAGED_PREFIX : LuciferHud.LUCIFER_PREFIX) + ".phase" + p;
                assertTrue(lang.has(key), "no bar name " + key);
                texts.add(lang.get(key).getAsString());
                texts.add(LuciferGui.roman(p));
            }
        }
        for (String text : texts) {
            for (char c : text.toUpperCase(Locale.ROOT).toCharArray()) {
                assertTrue(chars.indexOf(c) >= 0, "no glyph for '" + c + "' in \"" + text + "\"");
            }
        }
    }

    @Test
    void barKeysAreReadBack() {
        assertEquals(Boolean.FALSE, LuciferHud.variant(LuciferHud.LUCIFER_PREFIX + ".phase2"));
        assertEquals(Boolean.TRUE, LuciferHud.variant(LuciferHud.UNCAGED_PREFIX + ".phase5"));
        assertNull(LuciferHud.variant("entity.supernaturalcraft.michael.bar.phase1"));
        assertNull(LuciferHud.variant("entity.supernaturalcraft.lucifer.phase1"));
        assertEquals(5, LuciferHud.phaseOf(LuciferHud.UNCAGED_PREFIX + ".phase5"));
        assertEquals(-1, LuciferHud.phaseOf("entity.supernaturalcraft.lucifer.bar"));
    }

    @Test
    void theCageClosesOnLucifer() {
        assertEquals(0, CageBars.whole(false, 1));
        assertEquals(CageBars.COUNT, CageBars.whole(false, CageBars.phases(false)));
        for (int p = 2; p <= CageBars.phases(false); p++) {
            assertTrue(CageBars.whole(false, p) > CageBars.whole(false, p - 1), "the Cage closes further in phase " + p);
            for (int i = 0; i < CageBars.COUNT; i++) {
                if (CageBars.state(false, p - 1, i) != null) assertNotNull(CageBars.state(false, p, i), "bar " + i + " lifted in phase " + p);
            }
        }
    }

    @Test
    void theCageBreaksUnderLuciferUncaged() {
        assertEquals(CageBars.COUNT, CageBars.whole(true, 1));
        assertEquals(0, CageBars.whole(true, CageBars.phases(true)));
        assertEquals(LuciferGui.maxPhase(true), CageBars.phases(true));
        assertEquals(LuciferGui.maxPhase(false), CageBars.phases(false));
        for (int p = 2; p <= CageBars.phases(true); p++) {
            assertTrue(CageBars.whole(true, p) <= CageBars.whole(true, p - 1), "bars grow back in phase " + p);
            for (int i = 0; i < CageBars.COUNT; i++) {
                CageBars.State was = CageBars.state(true, p - 1, i), now = CageBars.state(true, p, i);
                if (was == null || !was.whole()) assertTrue(now == null || !now.whole(), "bar " + i + " whole again in phase " + p);
            }
        }
    }
}
