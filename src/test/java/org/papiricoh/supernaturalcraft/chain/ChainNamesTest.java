package org.papiricoh.supernaturalcraft.chain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ChainNamesTest {
    @Test void namesAreStableVariedAndAvoidExistingCharacters() {
        Set<String> banned = Set.of("lucifer", "azazel", "lilith", "metatron", "michael", "gabriel", "raphael", "naomi", "zachariah",
                "cain", "abaddon", "rowena", "amara", "chuck", "castiel", "crowley", "chorus", "uncaged", "death", "war", "famine", "pestilence", "grandwitch");
        for (ChainFaction faction : ChainFaction.values()) {
            Set<String> names = new HashSet<>();
            for (long seed = -5000; seed < 5000; seed++) {
                String name = ChainNames.name(faction, seed);
                assertEquals(name, ChainNames.name(faction, seed));
                // SucroCorp's people have a first name and a surname.
                assertTrue(name.matches(faction == ChainFaction.LEVIATHAN ? "[A-Z][a-z]+ [A-Z][a-z]+" : "[A-Z][a-z]+"), name);
                for (String part : name.toLowerCase(Locale.ROOT).split(" ")) assertFalse(banned.contains(part), name);
                names.add(name);
            }
            assertTrue(names.size() > (faction == ChainFaction.LEVIATHAN ? 300 : 1000), "Only " + names.size() + " distinct names");
        }
    }

    @Test void fallingChangesEpithetButNotIdentity() {
        Officer o = new Officer(1, ChainFaction.ANGEL);
        o.nameSeed = 123;
        String name = ChainNames.name(o);
        assertEquals("of_the_host", ChainNames.epithet(o.faction, o.branch, o.rank));
        o.branch = ChainFaction.Branch.FALLEN;
        assertEquals(name, ChainNames.name(o));
        assertEquals("the_fallen", ChainNames.epithet(o.faction, o.branch, o.rank));
        assertEquals("of_the_pit", ChainNames.epithet(ChainFaction.DEMON, ChainFaction.Branch.LOYAL, ChainRank.LORD));
    }
}
