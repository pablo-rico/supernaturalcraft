package org.papiricoh.supernaturalcraft.legacy.gen;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiteGeneratorTest {

    @Test
    void deterministicSafeAndUniquelyNamed() {
        for (int hunter = 0; hunter < 6; hunter++) {
            long salt = GenSeed.of(77L * hunter, new UUID(hunter, 5), "rite", 0);
            Set<String> names = new HashSet<>();
            for (int i = 0; i < 120; i++) {
                int tier = RiteGenerator.tierOf(i);
                RiteGenerator.Spec s = RiteGenerator.generate(salt, i, tier, 0);
                assertEquals(s, RiteGenerator.generate(salt, i, tier, 0));
                assertEquals(s.name(), RiteGenerator.generate(salt, i, tier, 3).name(), "a re-roll keeps the name");
                assertTrue(names.add(s.name()), "unique " + s.name());
                assertTrue(s.liquids().size() >= 1 && s.liquids().size() <= 3);
                assertTrue(RiteGenerator.LIQUIDS.containsAll(s.liquids()));
                assertTrue(s.ingredients().size() >= 2 && s.ingredients().size() <= 5, s.ingredients().toString());
                assertEquals(s.ingredients().size(), new HashSet<>(s.ingredients()).size());
                List<String> known = RiteGenerator.INGREDIENTS.stream().map(RiteGenerator.Ingredient::item).toList();
                assertTrue(known.containsAll(s.ingredients()));
                assertTrue(s.incantation().matches("[a-z ]+"));
                assertTrue(s.manaCost() > 0 && s.manaCost() <= RiteGenerator.MAX_MANA);
                JsonObject e = s.effect();
                String type = e.get("type").getAsString();
                if (type.equals("supernaturalcraft:apply_effect")) {
                    String effect = e.get("effect").getAsString();
                    RiteGenerator.Boon boon = RiteGenerator.BOONS.stream().filter(b -> b.effect().equals(effect)).findFirst().orElseThrow();
                    assertTrue(boon.minTier() <= tier);
                    int d = e.get("duration").getAsInt();
                    assertTrue(d >= boon.minTicks() * 0.7 && d <= boon.maxTicks(), "duration " + d);
                    assertTrue(e.get("amplifier").getAsInt() <= boon.maxAmp());
                    assertTrue(tier >= 4 || e.get("amplifier").getAsInt() == 0);
                    assertTrue(e.get("radius").getAsDouble() <= 8);
                } else {
                    assertTrue(RiteGenerator.WARDS.contains(type), type);
                    assertTrue(e.get("radius").getAsDouble() <= RiteGenerator.MAX_RADIUS);
                }
            }
        }
    }

    @Test
    void tiersClimbTwoAtATime() {
        assertEquals(1, RiteGenerator.tierOf(0));
        assertEquals(1, RiteGenerator.tierOf(1));
        assertEquals(2, RiteGenerator.tierOf(2));
        assertEquals(5, RiteGenerator.tierOf(8));
        assertEquals(5, RiteGenerator.tierOf(500));
    }
}
