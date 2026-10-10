package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaletteTest {

    private static Map<String, Integer> sample(Brush b, int n) {
        Map<String, Integer> out = new HashMap<>();
        for (int i = 0; i < n; i++) out.merge(b.at(i % 101, i / 101 % 37, i / 3737), 1, Integer::sum);
        return out;
    }

    @Test
    void mixesHonourTheirWeights() {
        Map<String, Integer> m = sample(Palette.mix(1, "minecraft:stone", 3, "minecraft:andesite", 1), 40_000);
        double share = m.get("minecraft:stone") / 40_000.0;
        assertTrue(Math.abs(share - 0.75) < 0.02, "stone share " + share);
        Map<String, Integer> p = sample(Palette.patches(2, 4, "minecraft:stone", 1, "minecraft:andesite", 1, "minecraft:tuff", 2), 40_000);
        double tuff = p.get("minecraft:tuff") / 40_000.0;
        assertTrue(Math.abs(tuff - 0.5) < 0.08, "clumped picks keep their shares too: " + tuff);
    }

    @Test
    void gradientsRunBottomToTop() {
        Brush g = Palette.gradientY(0, 9, 0.2, 3, "minecraft:cobblestone", "minecraft:stone_bricks", "minecraft:calcite");
        int bottom = 0, top = 0;
        for (int x = 0; x < 40; x++) {
            for (int z = 0; z < 40; z++) {
                if (g.at(x, 0, z).equals("minecraft:cobblestone")) bottom++;
                if (g.at(x, 9, z).equals("minecraft:calcite")) top++;
            }
        }
        assertEquals(1600, bottom, "the bottom row is all the first band");
        assertEquals(1600, top, "the top row is all the last band");
        Set<String> middle = new TreeSet<>();
        for (int x = 0; x < 40; x++) middle.add(g.at(x, 3, 0));
        assertTrue(middle.size() >= 2, "boundaries are dithered: " + middle);
    }

    @Test
    void weatheringFollowsWear() {
        Brush fresh = Palette.weathered(Palette.STONE_BRICK_WEAR, Palette.Field.constant(0), 0.1, 5);
        Brush ruined = Palette.weathered(Palette.STONE_BRICK_WEAR, Palette.Field.constant(1), 0.1, 5);
        Map<String, Integer> a = sample(fresh, 5000), b = sample(ruined, 5000);
        assertTrue(a.getOrDefault("minecraft:stone_bricks", 0) > 4500, "no wear, mostly pristine: " + a);
        assertTrue(b.getOrDefault("minecraft:stone_bricks", 0) < 500, "full wear, rarely pristine: " + b);
        assertTrue(b.getOrDefault("minecraft:moss_block", 0) + b.getOrDefault("minecraft:mossy_cobblestone", 0) > 2000, "and mostly ruined: " + b);
    }

    @Test
    void brushesAreDeterministic() {
        Brush b = Palette.patches(9, 3, "minecraft:stone", 1, "minecraft:andesite", 1);
        for (int i = 0; i < 200; i++) assertEquals(b.at(i, i * 3, -i), b.at(i, i * 3, -i));
    }

    @Test
    void everySchemeUsesReadableStates() {
        BlockIds ids = BlockIds.vanilla();
        for (Palette.Scheme s : List.of(Palette.HEAVEN_MARBLE, Palette.LAWRENCE_HOUSE, Palette.ROADHOUSE, Palette.CLINIC, Palette.OFFICE,
                Palette.DUST_ROAD, Palette.CLOUD)) {
            for (Brush b : List.of(s.wall(), s.plinth(), s.accent(), s.floor(), s.ground())) {
                for (int x = -20; x < 20; x++) for (int y = -8; y < 14; y++) assertNull(ids.check(b.at(x, y, x * 3 - y)), s.name());
            }
            for (Family f : List.of(s.trim(), s.roof())) {
                assertNull(ids.check(f.block()), s.name());
                if (f.hasStairs()) assertNull(ids.check(f.stairs(Dir.NORTH, true)), s.name());
                assertNull(ids.check(f.slabTop()), s.name());
            }
            assertNull(ids.check(St.pane(s.glass())), s.name());
            assertNull(ids.check(s.light()), s.name());
            assertNull(ids.check(s.wood().planks()), s.name());
        }
    }
}
