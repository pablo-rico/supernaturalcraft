package org.papiricoh.supernaturalcraft.coven;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.coven.ending.WitchBossSlot;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The witch boss slot (v0.21 P6, pure): one fight after Abaddon in two optional variants, each with its own kill advancement and
 * entity; the Book's choice decides which a hunter's road leads to (the giver fights the Grand Witch, the keeper Rowena), only that
 * variant counts as "their" boss and only it may be called by them; either variant fills the slot; the choice's and the unmarking's
 * advancements exist.
 */
class WitchBossSlotTest {

    private static final Path ADVANCEMENTS = Path.of("src/generated/resources/data/supernaturalcraft/advancement");

    @Test
    void twoOptionalVariantsRightAfterAbaddon() {
        List<Boss> order = List.of(Boss.values());
        int abaddon = order.indexOf(Boss.ABADDON);
        assertEquals(Boss.ROWENA, order.get(abaddon + 1), "Rowena right after Abaddon");
        assertEquals(Boss.GRAND_WITCH, order.get(abaddon + 2), "then the Grand Witch");
        assertTrue(Boss.ROWENA.optional && Boss.GRAND_WITCH.optional, "the witch slot blocks nothing");
        assertEquals("main/rowena_defeated", Boss.ROWENA.advancement);
        assertEquals("main/grand_witch_defeated", Boss.GRAND_WITCH.advancement);
        assertEquals(Boss.ROWENA, Boss.byEntity("rowena"));
        assertEquals(Boss.GRAND_WITCH, Boss.byEntity("grand_witch"));
        assertNull(Boss.byEntity("rowena_npc"), "Rowena at home is no boss");
        assertNull(Boss.byEntity("grand_witch_npc"), "nor the Grand Witch at peace");
        for (Boss b : List.of(Boss.ROWENA, Boss.GRAND_WITCH)) {
            ProgressionScale.BossStats s = ProgressionScale.of(b);
            assertEquals(3, s.tier(), b + " is a tier-3 enemy");
            assertTrue(s.targetHits() > ProgressionScale.of(Boss.ABADDON).targetHits(), b + " is past Abaddon on the curve");
            assertTrue(s.targetHits() < ProgressionScale.of(Boss.BROKEN_CHORUS).targetHits(), b + " is short of the Chorus");
            assertTrue(Files.isRegularFile(ADVANCEMENTS.resolve(b.advancement + ".json")), "no advancement " + b.advancement);
        }
    }

    @Test
    void theChoiceDecidesTheRoad() {
        assertEquals(Boss.GRAND_WITCH, WitchBossSlot.bossFor(RowenaRelation.Book.GIVEN), "the giver fights the Grand Witch");
        assertEquals(Boss.ROWENA, WitchBossSlot.bossFor(RowenaRelation.Book.KEPT), "the keeper fights Rowena");
        assertNull(WitchBossSlot.bossFor(RowenaRelation.Book.NONE), "no road before the choice");
        for (RowenaRelation.Book book : RowenaRelation.Book.values()) {
            for (Boss b : List.of(Boss.ROWENA, Boss.GRAND_WITCH)) {
                assertEquals(WitchBossSlot.bossFor(book) == b, WitchBossSlot.maySummon(book, b), book + " may summon " + b);
            }
            assertFalse(WitchBossSlot.maySummon(book, Boss.ABADDON), "the slot never calls another boss");
        }
    }

    @Test
    void onlyYourOwnRoadsBossCountsForYouButEitherFillsTheSlot() {
        Set<String> rowena = Set.of(Boss.ROWENA.advancement), witch = Set.of(Boss.GRAND_WITCH.advancement), none = Set.of();
        assertTrue(WitchBossSlot.beaten(RowenaRelation.Book.KEPT, rowena::contains));
        assertFalse(WitchBossSlot.beaten(RowenaRelation.Book.KEPT, witch::contains), "a keeper who only saw the Grand Witch fall");
        assertTrue(WitchBossSlot.beaten(RowenaRelation.Book.GIVEN, witch::contains));
        assertFalse(WitchBossSlot.beaten(RowenaRelation.Book.GIVEN, rowena::contains), "a giver who only saw Rowena fall");
        assertFalse(WitchBossSlot.beaten(RowenaRelation.Book.NONE, p -> true), "nothing is beaten before the choice");
        assertFalse(WitchBossSlot.beaten(RowenaRelation.Book.GIVEN, none::contains));
        assertTrue(WitchBossSlot.slotBeaten(rowena::contains));
        assertTrue(WitchBossSlot.slotBeaten(witch::contains));
        assertFalse(WitchBossSlot.slotBeaten(none::contains));
        assertFalse(WitchBossSlot.slotBeaten(Boss.ABADDON.advancement::equals), "Abaddon is not the slot");
    }

    @Test
    void theChoicesAndTheUnmarkingAreAdvancements() {
        assertEquals("main/gave_the_book", WitchBossSlot.GAVE);
        assertEquals("main/kept_the_book", WitchBossSlot.KEPT);
        assertEquals("main/unmarked", WitchBossSlot.UNMARKED);
        for (String a : List.of(WitchBossSlot.GAVE, WitchBossSlot.KEPT, WitchBossSlot.UNMARKED)) {
            assertTrue(Files.isRegularFile(ADVANCEMENTS.resolve(a + ".json")), "no advancement " + a);
        }
        assertEquals(List.of("NONE", "GIVEN", "KEPT"), List.of(RowenaRelation.Book.values()).stream().map(Enum::name).toList(),
                "the Book's fates only append (saved by name, but kept in order)");
        assertEquals(BossProgression.Boss.values().length, Set.of(Boss.values()).size());
    }

    @Test
    void theChoiceIsSavedAndOlderSavesHaveMadeNone() {
        RowenaRelation kept = RowenaRelation.NONE.meet().trusted(7).withBook(RowenaRelation.Book.KEPT).buy("compel");
        com.google.gson.JsonElement json = RowenaRelation.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, kept).getOrThrow();
        assertEquals("kept", json.getAsJsonObject().get("book").getAsString());
        assertEquals(kept, RowenaRelation.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow());
        com.google.gson.JsonObject old = com.google.gson.JsonParser.parseString("{\"trust\": 3, \"met\": true}").getAsJsonObject();
        RowenaRelation before = RowenaRelation.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, old).getOrThrow();
        assertEquals(RowenaRelation.Book.NONE, before.book(), "a P3 save has made no choice");
        assertEquals(3, before.trust());
        assertEquals(RowenaRelation.Book.GIVEN, before.withBook(RowenaRelation.Book.GIVEN).trusted(5).book());
        assertEquals(8, before.withBook(RowenaRelation.Book.GIVEN).trusted(5).trust(), "giving the Book earns five trust");
        assertEquals(RowenaRelation.MAX_TRUST, before.trusted(99).trust(), "trust is capped");
    }
}
