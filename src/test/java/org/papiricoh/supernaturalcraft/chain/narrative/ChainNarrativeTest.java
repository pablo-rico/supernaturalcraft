package org.papiricoh.supernaturalcraft.chain.narrative;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.chain.Bonds;
import org.papiricoh.supernaturalcraft.chain.ChainTask;
import org.papiricoh.supernaturalcraft.chain.Temperament;
import org.papiricoh.supernaturalcraft.chain.talk.TalkOption;
import org.papiricoh.supernaturalcraft.datagen.chain.ChainNarrativeLang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Narrative coverage and locale contracts, without loading the game or its registries. */
class ChainNarrativeTest {
    private static final Path SPANISH = Path.of("src/main/resources/assets/supernaturalcraft/lang/es_es.json");
    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:[1-6]\\$)?s|%%");
    // Like ChainNames' exclusion list, but for prose: ordinary words such as war and death remain legal.
    private static final Pattern CANONICAL = Pattern.compile(
            "\\b(?:lucifer|castiel|crowley|dean|sam|bobby|michael|gabriel|raphael|metatron|azazel|lilith|"
                    + "cain|abaddon|rowena|amara|chuck|naomi|zachariah|nemesis)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static Map<String, String> english() {
        Map<String, String> lines = new TreeMap<>();
        ChainNarrativeLang.add((key, text) -> {
            assertNull(lines.putIfAbsent(key, text), () -> "Duplicate English key: " + key);
            assertTrue(!text.isBlank(), () -> "Empty English line: " + key);
        });
        return lines;
    }

    private static void contains(Map<String, String> lines, String key) {
        assertTrue(lines.containsKey(key), () -> "Missing English key: " + key);
    }

    @Test
    void everyVoiceAndSituationHasAllItsDeclaredLines() {
        Map<String, String> lines = english();
        Set<String> expected = new HashSet<>();
        for (Voice voice : Voice.values()) {
            for (Situation situation : Situation.values()) {
                int shared = VoiceLines.count(voice, situation, null);
                assertTrue(shared >= 1, () -> voice + "/" + situation + " has no shared lines");
                for (int n = 0; n < shared; n++) expected.add(voice.key(situation, null, n));
                for (Temperament temperament : Temperament.values()) {
                    int count = VoiceLines.count(voice, situation, temperament);
                    assertTrue(count >= 0, () -> "Negative count: " + voice + "/" + situation + "/" + temperament);
                    for (int n = 0; n < count; n++) expected.add(voice.key(situation, temperament, n));
                }
            }
        }
        expected.forEach(key -> contains(lines, key));
        Set<String> actual = new TreeSet<>();
        lines.keySet().stream().filter(key -> key.startsWith(Voice.PREFIX)).forEach(actual::add);
        assertEquals(new TreeSet<>(expected), actual, "Voice counts and written lines must agree in both directions");
    }

    @Test
    void everyBackstoryFragmentExists() {
        Map<String, String> lines = english();
        for (Voice voice : Voice.values()) {
            for (Backstory.Piece piece : Backstory.Piece.values()) {
                // Fallen officers retain the angel's origin, vessel and rise, then add their own fall.
                if ((voice == Voice.FALLEN) != (piece == Backstory.Piece.FALL)) continue;
                for (int n = 0; n < Backstory.COUNT; n++) contains(lines, Backstory.key(voice, piece, n));
            }
        }
    }

    @Test
    void everyChronicleEventHasAllItsVariants() {
        Map<String, String> lines = english();
        for (String event : Chronicle.KEYS) {
            for (int n = 0; n < Chronicle.VARIANTS; n++) contains(lines, Chronicle.key(event, n));
        }
    }

    @Test
    void everyChoiceTemperamentBondAndTaskHasALabel() {
        Map<String, String> lines = english();
        for (TalkOption option : TalkOption.values()) {
            contains(lines, option.key());
            assertTrue(lines.get(option.key()).length() <= 32, () -> "Long button label: " + option);
        }
        for (Temperament temperament : Temperament.values()) {
            String key = "chain.supernaturalcraft.temperament." + temperament.id();
            contains(lines, key);
            contains(lines, key + ".desc");
        }
        for (Bonds.Kind kind : Bonds.Kind.values()) {
            if (kind != Bonds.Kind.NONE) {
                contains(lines, "chain.supernaturalcraft.bond." + kind.name().toLowerCase(Locale.ROOT));
            }
        }
        for (ChainTask.Kind kind : ChainTask.Kind.values()) {
            contains(lines, "chain.supernaturalcraft.task." + kind.id());
        }
        contains(lines, "chain.supernaturalcraft.sector");
        contains(lines, "tooltip.supernaturalcraft.gauntlet_of_ash");
        contains(lines, "tooltip.supernaturalcraft.seal_of_summons");
    }

    @Test
    void narrativeDoesNotNameCanonicalCharacters() {
        english().forEach((key, text) -> assertTrue(!CANONICAL.matcher(text).find(),
                () -> "Forbidden character name or term in " + key + ": " + text));
    }

    private static Map<String, Integer> placeholders(String text) {
        Map<String, Integer> counts = new HashMap<>();
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) counts.merge(matcher.group(), 1, Integer::sum);
        return counts;
    }

    @Test
    void spanishCoversEveryNarrativeKeyAndKeepsFormatting() throws IOException {
        JsonObject spanish = JsonParser.parseString(Files.readString(SPANISH)).getAsJsonObject();
        Map<String, String> lines = english();
        Set<String> missing = new TreeSet<>(lines.keySet());
        missing.removeAll(spanish.keySet());
        assertTrue(missing.isEmpty(), () -> "Merge es_narrative.json; missing Spanish keys: " + missing);
        for (Map.Entry<String, String> entry : lines.entrySet()) {
            String key = entry.getKey();
            assertTrue(spanish.get(key).isJsonPrimitive()
                    && spanish.get(key).getAsJsonPrimitive().isString(), () -> "Non-string Spanish value: " + key);
            String translated = spanish.get(key).getAsString();
            assertEquals(placeholders(entry.getValue()), placeholders(translated), key);
            assertEquals(entry.getValue().chars().filter(c -> c == '\n').count(),
                    translated.chars().filter(c -> c == '\n').count(), () -> "Line breaks: " + key);
            assertTrue(!CANONICAL.matcher(translated).find(), () -> "Forbidden name in Spanish: " + key);
        }
    }
}
