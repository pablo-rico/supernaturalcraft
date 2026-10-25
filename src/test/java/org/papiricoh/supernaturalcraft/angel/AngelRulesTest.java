package org.papiricoh.supernaturalcraft.angel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AngelRulesTest {

    @Test
    void onlyAnAngelBladeBitesInFull() {
        assertEquals(10f, AngelRules.incoming(10f, true, false));
        assertEquals(10f * AngelRules.MUNDANE_SHARE, AngelRules.incoming(10f, false, false), 1e-6);
        assertEquals(10f, AngelRules.incoming(10f, false, true), "what bypasses invulnerability (the void, /kill) still lands");
        assertTrue(AngelRules.MUNDANE_SHARE <= 0.25f, "without a blade it should take several times the work");
    }

    @Test
    void anAngelBladeSmitesAngelsButNeverBosses() {
        assertTrue(AngelRules.smites(true, false, true));
        assertFalse(AngelRules.smites(true, true, true), "a boss dies its own death");
        assertFalse(AngelRules.smites(true, false, false), "any other weapon kills it plainly");
        assertFalse(AngelRules.smites(false, false, true), "a demon is no angel");
    }
}
