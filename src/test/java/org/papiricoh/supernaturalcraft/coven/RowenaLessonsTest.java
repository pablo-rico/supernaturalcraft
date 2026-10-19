package org.papiricoh.supernaturalcraft.coven;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaLessons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What Rowena teaches (v0.21 P3) against the datapack: every page she sells is a Witchcraft bowl page of the tier she prices it
 * at (its {@code bowl_spell_page} entry), known to the book as a bowl spell; never a Master's page; and her offer is lowest tier first, at
 * most five, only unknown pages the hunter may learn and she trusts them with.
 */
class RowenaLessonsTest {

    private static final Path DATA = Path.of("src/main/resources/data/supernaturalcraft");

    private static JsonObject pageJson(String id) throws IOException {
        Path p = DATA.resolve("supernaturalcraft/bowl_spell_page/" + id + ".json");
        assertTrue(Files.exists(p), "no bowl_spell_page for " + id);
        return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
    }

    @Test
    void everyPageIsAWitchcraftBowlPageOfItsTier() throws IOException {
        Set<String> ids = new HashSet<>();
        JsonObject lang = JsonParser.parseString(Files.readString(Path.of("src/generated/resources/assets/supernaturalcraft/lang/en_us.json")))
                .getAsJsonObject();
        for (RowenaLessons.Page page : RowenaLessons.PAGES) {
            assertTrue(ids.add(page.id()), "she lists " + page.id() + " twice");
            JsonObject json = pageJson(page.id());
            assertEquals("witchcraft", json.get("school").getAsString(), page.id() + " is not a witchcraft page");
            assertEquals(json.get("tier").getAsInt(), page.tier(), page.id() + ": her tier and the page's disagree");
            assertTrue(page.tier() >= 1 && page.tier() <= 3, page.id() + ": her open catalogue holds no Master's page (those are MASTER_PAGES)");
            // A page teaches a spell (one or more bowl recipes): it must be a known bowl spell, by name.
            assertTrue(lang.has("bowl_spell.supernaturalcraft." + page.id()), page.id() + " is no bowl spell the book knows");
            assertTrue(page.minTrust() >= 0 && page.minTrust() <= 20, page.id() + ": trust out of range");
            assertEquals(page, RowenaLessons.page(page.id()));
        }
        assertNull(RowenaLessons.page("summon_crossroads"), "she does not teach demons' rites");
        assertEquals(5, RowenaLessons.page("familiar_bond").minTrust(), "her Familiar Bond needs five trust");
    }

    @Test
    void pricesClimbWithTheTier() {
        RowenaLessons.Price t1 = RowenaLessons.price(1), t2 = RowenaLessons.price(2), t3 = RowenaLessons.price(3);
        assertEquals(new RowenaLessons.Price(8, 0, null, 0), t1);
        assertEquals(new RowenaLessons.Price(16, 2, "minecraft:amethyst_shard", 2), t2);
        assertEquals(new RowenaLessons.Price(24, 4, "minecraft:ghast_tear", 1), t3);
        assertTrue(t1.emeralds() < t2.emeralds() && t2.emeralds() < t3.emeralds());
        assertEquals(List.of("taglocks", "hexed_ink"), List.copyOf(RowenaLessons.WARES.keySet()));
        assertEquals(3, RowenaLessons.WARES.get("taglocks")[0]);
        assertEquals(6, RowenaLessons.WARES.get("hexed_ink")[0]);
    }

    @Test
    void sheOffersAtMostFiveLowestTierFirst() {
        List<String> fresh = RowenaLessons.offer(id -> false, tier -> true, 20);
        assertEquals(RowenaLessons.OFFERED, fresh.size());
        assertEquals(List.of("curse_of_misfortune", "locate_being", "trace_the_familiar", "concealment", "break_hex"), fresh);
        int last = 0;
        for (String id : fresh) {
            int tier = RowenaLessons.page(id).tier();
            assertTrue(tier >= last, "tiers go up: " + fresh);
            last = tier;
        }
        assertTrue(RowenaLessons.OFFERED + 2 <= 8, "pages + back + leave fit the dialogue");
    }

    @Test
    void neverWhatTheyKnowNorAboveTheirRankNorBeforeSheTrustsThem() {
        Set<String> known = Set.of("curse_of_misfortune", "locate_being", "trace_the_familiar", "concealment");
        List<String> next = RowenaLessons.offer(known::contains, tier -> true, 0);
        assertFalse(next.stream().anyMatch(known::contains), "known pages are not offered: " + next);
        assertTrue(next.stream().allMatch(id -> RowenaLessons.page(id).tier() == 2), "the second tier comes next: " + next);
        assertTrue(next.stream().allMatch(id -> RowenaLessons.page(id).minTrust() == 0), "untrusted, only the open pages: " + next);
        List<String> apprentice = RowenaLessons.offer(id -> false, tier -> tier <= 1, 20);
        assertEquals(List.of("curse_of_misfortune", "locate_being", "trace_the_familiar", "concealment"), apprentice, "rank limits the tier");
        assertTrue(RowenaLessons.offer(id -> !"familiar_bond".equals(id), tier -> true, 4).isEmpty(), "Familiar Bond waits for trust 5");
        assertEquals(List.of("familiar_bond"), RowenaLessons.offer(id -> !"familiar_bond".equals(id), tier -> true, 5));
        assertTrue(RowenaLessons.offer(id -> true, tier -> true, 20).isEmpty(), "nothing left to teach");
        assertNotNull(RowenaLessons.offer(id -> false, tier -> false, 20));
        assertTrue(RowenaLessons.offer(id -> false, tier -> false, 20).isEmpty(), "nothing for one who may learn nothing");
    }

    // --- her Master's pages (P6) -------------------------------------------------------------------------------------------

    @Test
    void herMastersPageIsTheCureAndOnlyForHerAlly() throws IOException {
        assertEquals(List.of(new RowenaLessons.Page("unmark", 4, 0)), RowenaLessons.MASTER_PAGES);
        JsonObject lang = JsonParser.parseString(Files.readString(Path.of("src/generated/resources/assets/supernaturalcraft/lang/en_us.json")))
                .getAsJsonObject();
        for (RowenaLessons.Page page : RowenaLessons.MASTER_PAGES) {
            assertNull(RowenaLessons.PAGES.stream().filter(p -> p.id().equals(page.id())).findAny().orElse(null), page.id() + " is in both lists");
            JsonObject json = pageJson(page.id());
            assertEquals("witchcraft", json.get("school").getAsString());
            assertEquals(4, json.get("tier").getAsInt(), page.id() + " is a Master's page");
            assertEquals(0, json.get("weight").getAsInt(), page.id() + " is never found in a chest: only she teaches it");
            assertTrue(lang.has("bowl_spell.supernaturalcraft." + page.id()), page.id() + " is no bowl spell the book knows");
            assertEquals(page, RowenaLessons.page(page.id()));
        }
        assertEquals(new RowenaLessons.Price(32, 8, "minecraft:echo_shard", 1), RowenaLessons.price(4));
        assertTrue(lang.get("dialogue.supernaturalcraft.rowena.lessons.unmark").getAsString().contains("32 emeralds"), "the cure shows its price");
        // Everything else known: the cure only for her ally (master), only at the fourth tier, and she still asks no trust.
        Predicate<String> knowsTheRest = id -> !"unmark".equals(id);
        assertTrue(RowenaLessons.offer(knowsTheRest, tier -> true, 20).isEmpty(), "never to a stranger");
        assertTrue(RowenaLessons.offer(knowsTheRest, tier -> true, 20, false).isEmpty());
        assertEquals(List.of("unmark"), RowenaLessons.offer(knowsTheRest, tier -> true, 0, true), "to her ally");
        assertTrue(RowenaLessons.offer(knowsTheRest, tier -> tier <= 3, 20, true).isEmpty(), "not above the ally's Witchcraft rank");
        assertTrue(RowenaLessons.offer(id -> true, tier -> true, 20, true).isEmpty(), "not twice");
        // A fresh ally sees the lowest tiers first: the Master's page waits at the end of the list.
        List<String> fresh = RowenaLessons.offer(id -> false, tier -> true, 20, true);
        assertEquals(RowenaLessons.offer(id -> false, tier -> true, 20), fresh, "the first five are the same pages");
        assertFalse(fresh.contains("unmark"));
    }
}
