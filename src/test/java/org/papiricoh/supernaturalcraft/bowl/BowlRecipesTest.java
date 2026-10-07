package org.papiricoh.supernaturalcraft.bowl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.bowl.page.SpellCatalog;
import org.papiricoh.supernaturalcraft.datagen.lang.BowlLang;
import org.papiricoh.supernaturalcraft.datagen.lang.ContentLang;
import org.papiricoh.supernaturalcraft.datagen.lang.CrossroadsLang;
import org.papiricoh.supernaturalcraft.datagen.lang.GhostLang;
import org.papiricoh.supernaturalcraft.datagen.lang.SpellLang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The hand-written bowl spell recipes, read as plain JSON: well-formed, within limits, one per mix. */
class BowlRecipesTest {

    private static final Path DIR = Path.of("src/main/resources/data/supernaturalcraft/recipe/bowl_spell");
    private static final String NS = "supernaturalcraft";
    /** Nothing may cost more than the most mana anyone can have. */
    private static final float MAX_MANA = 175;
    /** As BowlContents (copied: that class drags in Minecraft codecs). */
    private static final int MAX_DOSES = 4, MAX_ITEMS = 8;

    private static Map<String, JsonObject> recipes() throws IOException {
        Map<String, JsonObject> out = new TreeMap<>();
        try (Stream<Path> files = Files.list(DIR)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                String name = p.getFileName().toString();
                out.put(name.substring(0, name.length() - 5), JsonParser.parseString(Files.readString(p)).getAsJsonObject());
            }
        }
        return out;
    }

    /** The spell a recipe teaches: its "spell" field, or else its file name. */
    private static String spell(String file, JsonObject json) {
        return json.has("spell") ? json.get("spell").getAsString() : NS + ":" + file;
    }

    private static List<String> liquids(JsonObject json) {
        List<String> out = new ArrayList<>();
        if (json.has("liquids")) json.getAsJsonArray("liquids").forEach(e -> out.add(e.getAsString()));
        return out;
    }

    private static List<String> ingredients(JsonObject json) {
        List<String> out = new ArrayList<>();
        if (!json.has("ingredients")) return out;
        for (JsonElement e : json.getAsJsonArray("ingredients")) {
            JsonObject o = e.getAsJsonObject();
            out.add(o.has("item") ? o.get("item").getAsString() : o.has("tag") ? "#" + o.get("tag").getAsString() : o.toString());
        }
        return out;
    }

    @Test
    void everyRecipeIsWellFormed() throws IOException {
        Map<String, JsonObject> all = recipes();
        assertFalse(all.isEmpty(), "no bowl spell recipes found in " + DIR.toAbsolutePath());
        for (var e : all.entrySet()) {
            String file = e.getKey();
            JsonObject json = e.getValue();
            assertEquals(NS + ":bowl_spell", json.get("type").getAsString(), file + ": type");
            List<String> liquids = liquids(json);
            assertTrue(liquids.size() <= MAX_DOSES, file + ": more than " + MAX_DOSES + " liquids");
            for (String l : liquids) assertNotNull(BowlLiquid.fromId(l), file + ": unknown liquid " + l);
            List<String> ingredients = ingredients(json);
            assertTrue(ingredients.size() <= MAX_ITEMS, file + ": more than " + MAX_ITEMS + " ingredients");
            assertFalse(liquids.isEmpty() && ingredients.isEmpty(), file + ": an empty bowl cannot be a recipe");
            String incantation = json.get("incantation").getAsString();
            assertTrue(incantation.trim().split("\\s+").length >= 3, file + ": the incantation needs at least three words");
            assertTrue(Recitation.letters(incantation) > 0, file + ": the incantation has no letters");
            float mana = json.has("mana_cost") ? json.get("mana_cost").getAsFloat() : 0;
            assertTrue(mana >= 0 && mana <= MAX_MANA, file + ": mana cost " + mana);
            assertTrue(json.has("effect") && json.getAsJsonObject("effect").has("type"), file + ": effect type");
        }
    }

    @Test
    void theRecipesTeachExactlyTheNineSpells() throws IOException {
        Set<String> taught = new HashSet<>();
        recipes().forEach((file, json) -> taught.add(spell(file, json)));
        Set<String> expected = new HashSet<>();
        SpellCatalog.SPELLS.forEach(s -> expected.add(NS + ":" + s));
        assertEquals(9, expected.size());
        assertEquals(expected, taught);
    }

    @Test
    void noTwoRecipesAskForTheSameBowl() throws IOException {
        Map<String, String> seen = new HashMap<>();
        recipes().forEach((file, json) -> {
            List<String> l = new ArrayList<>(liquids(json));
            List<String> i = new ArrayList<>(ingredients(json));
            l.sort(null);
            i.sort(null);
            String key = l + " | " + i;
            String other = seen.put(key, file);
            assertTrue(other == null, file + " and " + other + " both ask for " + key);
        });
    }

    @Test
    void everySpellHasANameAndADescription() {
        Map<String, String> lang = new HashMap<>();
        List<String> duplicates = new ArrayList<>();
        java.util.function.BiConsumer<String, String> add = (k, v) -> {
            if (lang.put(k, v) != null) duplicates.add(k);
        };
        BowlLang.add(add);
        GhostLang.add(add);
        org.papiricoh.supernaturalcraft.datagen.chuck.AuthorWorldLang.add(add);
        CrossroadsLang.add(add);
        SpellLang.add(add);
        ContentLang.add(add);
        assertTrue(duplicates.isEmpty(), "lang keys defined twice: " + duplicates);
        for (String s : SpellCatalog.SPELLS) {
            assertTrue(lang.containsKey("bowl_spell." + NS + "." + s), "no name for " + s);
            assertTrue(lang.containsKey("bowl_spell." + NS + "." + s + ".desc"), "no description for " + s);
        }
    }
}
