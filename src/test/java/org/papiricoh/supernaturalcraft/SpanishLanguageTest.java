package org.papiricoh.supernaturalcraft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the hand-written locale without loading any Minecraft classes. */
class SpanishLanguageTest {
    private static final Path EN = Path.of("src/generated/resources/assets/supernaturalcraft/lang/en_us.json");
    private static final Path ES = Path.of("src/main/resources/assets/supernaturalcraft/lang/es_es.json");
    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:[1-6]\\$)?s|%%");
    private static final int REQUIRED_COVERAGE = 5453;

    private static JsonObject read(Path path) throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            assertTrue(entry.getValue().isJsonPrimitive()
                    && entry.getValue().getAsJsonPrimitive().isString(),
                    () -> path + ": non-string value at " + entry.getKey());
        }
        return json;
    }

    private static Map<String, Integer> placeholders(String text) {
        Map<String, Integer> counts = new HashMap<>();
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) counts.merge(matcher.group(), 1, Integer::sum);
        return counts;
    }

    @Test
    void spanishParsesAsAStringDictionary() throws IOException {
        assertTrue(read(ES).size() > 0);
    }

    @Test
    void spanishHasNoStaleKeys() throws IOException {
        JsonObject english = read(EN), spanish = read(ES);
        TreeSet<String> stale = new TreeSet<>(spanish.keySet());
        stale.removeAll(english.keySet());
        assertTrue(stale.isEmpty(), () -> "Stale Spanish keys: " + stale);
    }

    @Test
    void sharedKeysKeepTheirPlaceholdersAndLineBreaks() throws IOException {
        JsonObject english = read(EN), spanish = read(ES);
        for (String key : spanish.keySet()) {
            if (!english.has(key)) continue;
            String source = english.get(key).getAsString();
            String translated = spanish.get(key).getAsString();
            assertEquals(placeholders(source), placeholders(translated), key);
            assertEquals(source.chars().filter(c -> c == '\n').count(),
                    translated.chars().filter(c -> c == '\n').count(),
                    () -> "Line breaks: " + key);
        }
    }

    @Test
    void spanishCoversTheOriginalEnglishKeySet() throws IOException {
        JsonObject english = read(EN), spanish = read(ES);
        TreeSet<String> missing = new TreeSet<>(english.keySet());
        missing.removeAll(spanish.keySet());
        missing.forEach(key -> System.out.println("Missing es_es translation: " + key));
        int translated = english.size() - missing.size();
        // Future English additions are printed above without raising this baseline.
        assertTrue(translated >= REQUIRED_COVERAGE,
                () -> "Spanish coverage: " + translated + "/" + english.size()
                        + "; required: " + REQUIRED_COVERAGE);
    }
}
