package org.papiricoh.supernaturalcraft.magic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.bowl.page.SpellCatalog;
import org.papiricoh.supernaturalcraft.magic.school.School;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The schools in the shipped data (v0.21), read as plain JSON: every effect sigil, bowl spell and rite names a real school, and
 * every bowl recipe can be learnt from a registered {@code bowl_spell_page} (its group's or its own id's).
 */
class SchoolDataTest {

    private static final String NS = "supernaturalcraft";
    private static final Path DATA = Path.of("src/main/resources/data/" + NS);
    private static final Path SIGILS = DATA.resolve(NS + "/sigil");
    private static final Path PAGES = DATA.resolve(NS + "/bowl_spell_page");
    private static final Path BOWL = DATA.resolve("recipe/bowl_spell");
    private static final Path RITUALS = DATA.resolve("recipe/ritual");

    private static Map<String, JsonObject> read(Path dir) throws IOException {
        Map<String, JsonObject> out = new TreeMap<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                String name = p.getFileName().toString();
                out.put(name.substring(0, name.length() - 5), JsonParser.parseString(Files.readString(p)).getAsJsonObject());
            }
        }
        assertFalse(out.isEmpty(), "nothing in " + dir.toAbsolutePath());
        return out;
    }

    /** The school a file names (as {@code SchoolCodecs.CODEC} reads it), or null if the field is missing or unknown. */
    private static School school(JsonObject json) {
        if (!json.has("school")) return null;
        String id = json.get("school").getAsString();
        School s = School.fromId(id);
        return s.isNone() && !"none".equalsIgnoreCase(id) ? null : s;
    }

    @Test
    void theSchoolsAndTheirIds() {
        assertEquals(List.of(School.ENOCHIAN, School.INFERNAL, School.WITCHCRAFT, School.HUNTER), School.SCHOOLS);
        for (School s : School.values()) assertEquals(s, School.fromId(s.id().toUpperCase()), s + " by id");
        assertEquals(School.NONE, School.fromId("necromancy"));
        assertEquals(School.NONE, School.fromId(null));
        assertEquals(School.NONE, School.byOrdinal(99), "a newer school on an older client");
    }

    @Test
    void everyEffectSigilBelongsToASchool() throws IOException {
        List<String> problems = new ArrayList<>();
        read(SIGILS).forEach((id, json) -> {
            boolean effect = "effect".equals(json.get("kind").getAsString());
            if (json.has("school") && school(json) == null) problems.add(id + ": unknown school " + json.get("school"));
            if (effect && (school(json) == null || school(json).isNone())) problems.add(id + ": an effect needs a school");
            int tier = json.has("tier") ? json.get("tier").getAsInt() : 1;
            if (tier < 1 || tier > 4) problems.add(id + ": tier " + tier);
        });
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void theOldSigilsKeepTheirPlannedSchools() throws IOException {
        Map<String, JsonObject> sigils = read(SIGILS);
        Map<String, School> planned = Map.of("smite", School.ENOCHIAN, "mend", School.ENOCHIAN, "banishing", School.ENOCHIAN,
                "hellfire", School.INFERNAL, "frost", School.WITCHCRAFT, "repel", School.HUNTER, "reveal", School.HUNTER,
                "exorcise", School.HUNTER, "snare", School.HUNTER,
                // The plan's one exception among the forms: the Ward is a circle of Heaven's tongue.
                "ward", School.ENOCHIAN);
        planned.forEach((id, s) -> assertEquals(s, school(sigils.get(id)), id));
        for (String form : List.of("touch", "bolt", "burst", "empower", "extend", "widen", "echo")) {
            School s = school(sigils.get(form));
            assertTrue(s == null || s.isNone(), form + " is a form or modifier: it belongs to no school");
        }
    }

    @Test
    void everyRiteAndBowlSpellBelongsToASchool() throws IOException {
        List<String> problems = new ArrayList<>();
        read(RITUALS).forEach((id, json) -> {
            School s = school(json);
            if (s == null || s.isNone()) problems.add("ritual/" + id + ": school " + json.get("school"));
        });
        read(BOWL).forEach((id, json) -> {
            School s = school(json);
            if (s == null || s.isNone()) problems.add("bowl_spell/" + id + ": school " + json.get("school"));
        });
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyPageIsWellFormed() throws IOException {
        List<String> problems = new ArrayList<>();
        read(PAGES).forEach((id, json) -> {
            School s = school(json);
            if (json.has("school") && (s == null || s.isNone())) problems.add(id + ": school " + json.get("school"));
            int tier = json.has("tier") ? json.get("tier").getAsInt() : 1;
            if (tier < 1 || tier > 4) problems.add(id + ": tier " + tier);
            int weight = json.has("weight") ? json.get("weight").getAsInt() : 10;
            if (weight < 0 || weight > 1000) problems.add(id + ": weight " + weight);
        });
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyBowlRecipeHasAPageToLearnItFrom() throws IOException {
        Set<String> pages = read(PAGES).keySet();
        List<String> problems = new ArrayList<>();
        Set<String> used = new TreeSet<>();
        read(BOWL).forEach((file, json) -> {
            String group = json.has("spell") ? json.get("spell").getAsString() : NS + ":" + file;
            String groupPath = group.startsWith(NS + ":") ? group.substring(NS.length() + 1) : null;
            boolean byGroup = groupPath != null && pages.contains(groupPath);
            boolean byOwn = pages.contains(file);
            if (!byGroup && !byOwn) problems.add(file + ": neither its group " + group + " nor its own id has a bowl_spell_page");
            if (byGroup) used.add(groupPath);
            if (byOwn) used.add(file);
        });
        assertTrue(problems.isEmpty(), String.join("\n", problems));
        Set<String> idle = new TreeSet<>(pages);
        idle.removeAll(used);
        assertTrue(idle.isEmpty(), "pages that teach no recipe: " + idle);
    }

    @Test
    void theCatalogMirrorsThePages() throws IOException {
        assertEquals(new TreeSet<>(read(PAGES).keySet()), new TreeSet<>(SpellCatalog.PAGES), "SpellCatalog.PAGES vs the page files");
        assertTrue(SpellCatalog.PAGES.containsAll(SpellCatalog.SPELLS), "every group has its page");
    }
}
