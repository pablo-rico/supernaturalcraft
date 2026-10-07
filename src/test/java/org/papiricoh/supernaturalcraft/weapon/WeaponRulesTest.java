package org.papiricoh.supernaturalcraft.weapon;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseLevels;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseState;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponRulesTest {

    private static final WeaponProfile MELEE_T3 = new WeaponProfile(3, 3, WeaponProfile.Kind.MELEE, false, false);
    private static final WeaponProfile CATALYST_T4 = new WeaponProfile(4, 4, WeaponProfile.Kind.CATALYST, false, true);

    @Test
    void curseLevelsFollowSoulThresholds() {
        assertEquals(1, CurseLevels.levelFor(0));
        assertEquals(1, CurseLevels.levelFor(19));
        assertEquals(2, CurseLevels.levelFor(20));
        assertEquals(4, CurseLevels.levelFor(150));
        assertEquals(5, CurseLevels.levelFor(10_000));
    }

    @Test
    void feedingRaisesSoulsAndSatiationButCaps() {
        CurseState s = new CurseState(0, 90, java.util.Optional.empty(), 0).fed(CurseLevels.soulsFor(20));
        assertEquals(5, s.souls());
        assertEquals(CurseLevels.MAX_SATIATION, s.satiation());
        assertEquals(0, CurseState.FRESH.hungrier().hungrier().satiation() - (CurseLevels.MAX_SATIATION - 2));
    }

    @Test
    void runeFitsAndStackingLimits() {
        RuneSet set = RuneSet.EMPTY;
        assertNull(set.rejection(Rune.EDGE, MELEE_T3));
        assertEquals("wrong_kind", set.rejection(Rune.RESONANCE, MELEE_T3));
        assertEquals("wrong_kind", set.rejection(Rune.EDGE, CATALYST_T4));
        assertNull(set.rejection(Rune.SANCTITY, CATALYST_T4), "SANCTITY fits anything");
        set = set.with(Rune.EDGE).with(Rune.EDGE);
        assertEquals("too_many", set.rejection(Rune.EDGE, MELEE_T3));
        set = set.with(Rune.LEECH);
        assertEquals("full", set.rejection(Rune.FROST, MELEE_T3));
        assertEquals("tier", RuneSet.EMPTY.rejection(Rune.VOID, MELEE_T3));
    }

    @Test
    void purgeLosesExactlyOneRune() {
        RuneSet set = new RuneSet(List.of(Rune.EDGE, Rune.LEECH, Rune.FROST));
        RandomSource random = RandomSource.create(7);
        for (int i = 0; i < 20; i++) {
            List<Rune> back = set.survivorsOfPurge(random);
            assertEquals(2, back.size());
            assertTrue(set.runes().containsAll(back));
        }
        assertTrue(RuneSet.EMPTY.survivorsOfPurge(random).isEmpty());
    }

    @Test
    void boundWeaponsKnowTheirMaster() {
        java.util.UUID a = java.util.UUID.randomUUID(), b = java.util.UUID.randomUUID();
        CurseState s = CurseState.FRESH.boundTo(a);
        assertTrue(s.ownedBy(a));
        assertFalse(s.ownedBy(b));
        assertTrue(CurseState.FRESH.ownedBy(b), "an unbound weapon answers to anyone");
    }
}
