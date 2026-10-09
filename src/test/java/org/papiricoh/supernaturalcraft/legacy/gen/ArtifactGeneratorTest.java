package org.papiricoh.supernaturalcraft.legacy.gen;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.legacy.LegacyAssets;
import org.papiricoh.supernaturalcraft.legacy.artifact.ArtifactData;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactGeneratorTest {

    @Test
    void rollsAreDeterministicAndInRange() {
        int[] byRarity = new int[4];
        Set<String> names = new HashSet<>();
        for (long seed = 0; seed < 4000; seed++) {
            ArtifactData a = ArtifactGenerator.roll(seed * 31 + 7, 3);
            assertEquals(a, ArtifactGenerator.roll(seed * 31 + 7, 3));
            assertTrue(LegacyAssets.ARTIFACT_FORMS.contains(a.form()), a.form());
            assertTrue(a.rarity() >= 0 && a.rarity() <= 3);
            assertTrue(a.boons().size() >= 1 && a.boons().size() <= 2);
            assertEquals(a.boons().size(), new HashSet<>(a.boons()).size(), "boons repeat");
            assertTrue(ArtifactGenerator.BOONS.containsAll(a.boons()));
            assertTrue(a.curse().isEmpty() || ArtifactGenerator.CURSES.contains(a.curse()));
            if (a.rarity() >= 2) assertEquals(2, a.boons().size());
            assertFalse(a.identified());
            assertFalse(a.name().isBlank());
            byRarity[a.rarity()]++;
            names.add(a.name());
        }
        assertTrue(byRarity[0] > byRarity[1] && byRarity[1] > byRarity[2] && byRarity[2] > byRarity[3] && byRarity[3] > 0);
        assertTrue(names.size() > 50, "names vary");
    }

    @Test
    void maxRarityCaps() {
        for (long seed = 0; seed < 2000; seed++) {
            assertEquals(0, ArtifactGenerator.roll(seed, 0).rarity());
            assertTrue(ArtifactGenerator.roll(seed, 1).rarity() <= 1);
        }
    }
}
