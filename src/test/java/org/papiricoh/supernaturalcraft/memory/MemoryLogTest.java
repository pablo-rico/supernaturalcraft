package org.papiricoh.supernaturalcraft.memory;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The memory log ({@link MemoryLog}) and what becomes a memory ({@link MemoryRules}). */
class MemoryLogTest {

    private static Memory sight(int i) {
        return MemoryRules.sighting("supernaturalcraft:mob_" + i, i, 0);
    }

    @Test
    void appendsOnceById() {
        Memory m = MemoryRules.boss(BossProgression.Boss.AZAZEL, 10, 1000);
        MemoryLog log = MemoryLog.EMPTY.with(m);
        assertSame(log, log.with(MemoryRules.boss(BossProgression.Boss.AZAZEL, 99, 2000)), "the same moment is remembered once");
        assertEquals(1, log.entries().size());
        assertEquals(m, log.find("boss:azazel"));
        assertFalse(log.isCollected("boss:azazel"));
        assertTrue(log.withCollected("boss:azazel").isCollected("boss:azazel"));
    }

    @Test
    void survivesItsCodec() {
        MemoryLog log = MemoryLog.EMPTY.with(MemoryRules.boss(BossProgression.Boss.LUCIFER, 5, 6))
                .with(MemoryRules.deal(0, "upgrade", "1", true, 7, 8)).withCollected("deal:0").withBackfilled();
        JsonElement json = MemoryLog.CODEC.encodeStart(JsonOps.INSTANCE, log).getOrThrow();
        MemoryLog back = MemoryLog.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(log.entries(), back.entries());
        assertEquals(log.collected(), back.collected());
        assertTrue(back.backfilled());
    }

    @Test
    void aFullLogDropsTheLightestUngatheredFirst() {
        MemoryLog log = MemoryLog.EMPTY.with(MemoryRules.boss(BossProgression.Boss.AZAZEL, 1, 1));
        for (int i = 0; i < MemoryLog.CAP - 1; i++) log = log.with(sight(i));
        log = log.withCollected("seen:supernaturalcraft:mob_0");
        assertEquals(MemoryLog.CAP, log.entries().size());
        log = log.with(MemoryRules.boss(BossProgression.Boss.LILITH, 2, 2));
        assertEquals(MemoryLog.CAP, log.entries().size(), "never past the cap");
        assertTrue(log.has("boss:azazel"), "a victory is never pushed out by sightings");
        assertTrue(log.has("boss:lilith"));
        assertTrue(log.has("seen:supernaturalcraft:mob_0"), "a gathered sighting stays while ungathered ones remain");
        assertFalse(log.has("seen:supernaturalcraft:mob_1"), "the oldest ungathered sighting went");
    }

    @Test
    void advancementsBecomeMemories() {
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            Memory m = MemoryRules.fromAdvancement(b.advancement, 1, 2);
            assertNotNull(m, b.advancement);
            assertEquals(MemoryKind.BOSS_VICTORY, m.kind());
            assertEquals(b.id(), m.subject());
            assertEquals("boss:" + b.id(), m.id());
        }
        assertEquals("lilith", MemoryRules.fromAdvancement("main/lucifer_rising", 0, 0).subject(), "Lilith's fall is Lucifer rising");
        Memory angel = MemoryRules.fromAdvancement("main/angel_3", 0, 0);
        assertEquals(MemoryKind.ASCENSION, angel.kind());
        assertEquals("angel", angel.subject());
        assertEquals(3, angel.variant());
        assertEquals("rank:hunter_2", MemoryRules.fromAdvancement("main/hunter_2", 0, 0).id());
        assertEquals("rank:demon_4", MemoryRules.fromAdvancement("main/demon_4", 0, 0).id());
        Memory legacy = MemoryRules.fromAdvancement("main/legacy_5", 0, 0);
        assertEquals(MemoryKind.LEGACY_RANK, legacy.kind());
        assertEquals(5, legacy.variant());
        assertEquals(MemoryKind.HEEDED_CALL, MemoryRules.fromAdvancement("main/heeded_the_call", 0, 0).kind());
        assertNull(MemoryRules.fromAdvancement("main/deal_with_the_devil", 0, 0));
        assertNull(MemoryRules.fromAdvancement("main/angel_x", 0, 0));
        Set<String> ids = new HashSet<>();
        for (String path : MemoryRules.advancements()) {
            Memory m = MemoryRules.fromAdvancement(path, 0, 0);
            assertNotNull(m, path);
            assertTrue(ids.add(m.id()), "one id per advancement: " + path);
        }
    }

    @Test
    void otherMomentsGetStableIds() {
        Memory deal = MemoryRules.deal(2, "upgrade", "1", true, 0, 0);
        assertEquals("deal:2", deal.id());
        assertEquals("wild", deal.detail());
        assertEquals(1, deal.variant());
        assertEquals(0, MemoryRules.deal(0, "knowledge", "", false, 0, 0).variant());
        assertEquals("bowl", MemoryRules.deal(0, "knowledge", "nonsense", false, 0, 0).detail());
        assertEquals(MemoryKind.CASE_SOLVED, MemoryRules.caseClosed(4, "supernaturalcraft:vampire", "barn", true, 0, 0).kind());
        assertEquals(MemoryKind.CASE_LOST, MemoryRules.caseClosed(4, "supernaturalcraft:vampire", "barn", false, 0, 0).kind());
        assertEquals("case:4", MemoryRules.caseClosed(4, "x", "y", false, 0, 0).id());
        assertEquals("pet:abc", MemoryRules.petLost("abc", "minecraft:wolf", "Rumsfeld", 0, 0).id());
        assertEquals("prey:minecraft:zombie", MemoryRules.prey("minecraft:zombie", 30, 0, 0).id());
    }

    @Test
    void theStoryIsToldOldestFirst() {
        Memory a = new Memory("a", MemoryKind.FIRST_SIGHTING, "", "", 0, 0, 0);
        Memory b = new Memory("b", MemoryKind.BOSS_VICTORY, "", "", 0, 500, 0);
        Memory c = new Memory("c", MemoryKind.CASE_SOLVED, "", "", 100, 500, 0);
        Memory d = new Memory("d", MemoryKind.CROSSROADS_DEAL, "", "", 0, 900, 0);
        assertEquals(List.of(a, b, c, d), MemoryRules.chronological(List.of(d, c, b, a)));
        assertEquals(1_000_000 - 20 * 50, MemoryRules.estimateRealTime(80, 100, 1_000_000), "a game time a second ago");
        assertEquals(1_000_000, MemoryRules.estimateRealTime(0, 100, 1_000_000), "an unknown time is now");
    }
}
