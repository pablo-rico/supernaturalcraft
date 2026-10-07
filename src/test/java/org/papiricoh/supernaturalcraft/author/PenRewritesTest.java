package org.papiricoh.supernaturalcraft.author;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Pen's rewrites: never a boss or a player, always within a tier, cycles that close. */
class PenRewritesTest {

    @Test
    void bossesAndPlayersAreNeverRewritten() {
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            String id = "supernaturalcraft:" + b.entity;
            assertNull(PenRewrites.rewrite(id, false), b + " is in a tier");
        }
        assertNull(PenRewrites.rewrite("minecraft:player", false));
        assertNull(PenRewrites.rewrite("minecraft:wither", false));
        assertNull(PenRewrites.rewrite("minecraft:zombie", true), "anything tagged a boss");
        for (List<String> tier : PenRewrites.TIERS) for (String id : tier) assertTrue(!PenRewrites.NEVER.contains(id), id);
    }

    @Test
    void aCreatureBecomesAnotherOfItsTier() {
        Set<String> all = new HashSet<>();
        for (List<String> tier : PenRewrites.TIERS) {
            assertTrue(tier.size() >= 2);
            for (String id : tier) {
                assertTrue(all.add(id), id + " is in two tiers");
                String to = PenRewrites.rewrite(id, false);
                assertNotEquals(id, to);
                assertTrue(tier.contains(to), id + " left its tier for " + to);
            }
        }
        assertNull(PenRewrites.rewrite("minecraft:unknown_thing", false));
    }

    @Test
    void biomesAndSkiesCycle() {
        String b = PenRewrites.BIOMES.getFirst();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < PenRewrites.BIOMES.size(); i++) {
            assertTrue(seen.add(b));
            b = PenRewrites.nextBiome(b);
        }
        assertEquals(PenRewrites.BIOMES.getFirst(), b);
        assertEquals(PenRewrites.BIOMES.getFirst(), PenRewrites.nextBiome("minecraft:the_void"));
        assertEquals(PenRewrites.Sky.RAIN, PenRewrites.Sky.of(false, false, false).next());
        assertEquals(PenRewrites.Sky.STORM, PenRewrites.Sky.of(true, false, false).next());
        assertEquals(PenRewrites.Sky.CLEAR_NIGHT, PenRewrites.Sky.of(true, true, false).next());
        assertEquals(PenRewrites.Sky.CLEAR_DAY, PenRewrites.Sky.of(false, false, true).next());
        assertEquals(PenRewrites.MODES, PenRewrites.COOLDOWN.length);
    }
}
