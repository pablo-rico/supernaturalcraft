package org.papiricoh.supernaturalcraft.chain;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FeudTest {

    /** A full chain: 1 Lord, 2 Commanders, 3 Lieutenants, 6 Captains, 10 Soldiers. */
    private static Hierarchy full(ChainFaction f) {
        Hierarchy h = new Hierarchy(f);
        for (int r = ChainRank.LORD.ordinal(); r >= 0; r--) {
            ChainRank rank = ChainRank.values()[r];
            for (int i = 0; i < rank.cap(); i++) h.recruit(rank, Officer.NONE, i * 7L + r, "supernaturalcraft:black_eyed_demon", 0).level = r;
        }
        h.reassignOrphans();
        return h;
    }

    @Test
    void feudsComeAboutEveryThreeDaysAndThriceAsOftenWithTheThroneEmpty() {
        int calm = 0, vacant = 0, days = 6000;
        for (int d = 0; d < days; d++) {
            if (Feud.rolls(ChainRules.rng(1, -30, d), false)) calm++;
            if (Feud.rolls(ChainRules.rng(1, -31, d), true)) vacant++;
        }
        assertEquals(days / 3.0, calm, days * 0.03);
        assertTrue(vacant >= calm * 2.7, "vacant " + vacant + " vs calm " + calm);
    }

    @Test
    void duringAnInterregnumEveryFeudHasACommander() {
        Hierarchy h = full(ChainFaction.DEMON);
        h.kill(h.lord(), false, 10);
        h.sealThrone(true);
        for (long d = 0; d < 200; d++) {
            Feud.Pair p = Feud.pick(h, true, ChainRules.rng(2, 0, d));
            assertNotNull(p);
            assertTrue(p.challenger().rank == ChainRank.COMMANDER || p.defender().rank == ChainRank.COMMANDER);
            assertNotSame(p.challenger(), p.defender());
        }
    }

    @Test
    void everyOutcomeKeepsTheChainValid() {
        for (long seed = 0; seed < 300; seed++) {
            Hierarchy h = full(ChainFaction.ANGEL);
            GenSeed.Rng rng = ChainRules.rng(seed, 0, seed);
            for (int day = 0; day < 10; day++) {
                Feud.Pair p = Feud.pick(h, false, rng);
                if (p == null) break;
                int scars = p.challenger().scars + p.defender().scars;
                Feud.Outcome o = Feud.resolve(h, p, rng, day, null);
                assertEquals(scars + 1, p.challenger().scars + p.defender().scars, "the loser takes a scar");
                assertTrue(h.invariantsHold(), "seed " + seed + " day " + day);
                if (o.swapped()) assertTrue(o.winner().rank.ordinal() > o.loser().rank.ordinal(), "a usurper rises");
            }
        }
    }

    @Test
    void aChallengerThatBeatsItsMasterTakesItsRank() {
        Hierarchy h = new Hierarchy(ChainFaction.DEMON);
        Officer captain = h.recruit(ChainRank.CAPTAIN, Officer.NONE, 1, "x", 0);
        Officer soldier = h.recruit(ChainRank.SOLDIER, captain.id, 2, "x", 0);
        Feud.Outcome o = Feud.resolve(h, new Feud.Pair(soldier, captain), ChainRules.rng(0, 0, 0), 0, soldier);
        assertTrue(o.swapped());
        assertEquals(ChainRank.CAPTAIN, soldier.rank);
        assertEquals(ChainRank.SOLDIER, captain.rank);
        assertTrue(h.invariantsHold());
    }

    @Test
    void aSealedThroneCannotBeFilled() {
        Hierarchy h = full(ChainFaction.DEMON);
        Officer lord = h.lord();
        h.kill(lord, false, 0);
        h.sealThrone(true);
        Officer commander = h.ofRank(ChainRank.COMMANDER).getFirst();
        assertFalse(h.promote(commander), "no promotion to an empty, sealed throne");
        assertNull(h.recruit(ChainRank.LORD, Officer.NONE, 9, "x", 0), "no recruit either");
        h.kill(h.ofRank(ChainRank.COMMANDER).getLast(), true, 0);
        assertTrue(h.revive(lord, 5));
        assertEquals(ChainRank.COMMANDER, lord.rank, "a returning Lord does not take back a sealed throne");
        assertNull(h.lord());
        h.sealThrone(false);
        assertTrue(h.promote(commander));
    }

    @Test
    void theHeirIsTheCommanderWithMostClaims() {
        Hierarchy h = full(ChainFaction.DEMON);
        h.kill(h.lord(), false, 0);
        var commanders = h.ofRank(ChainRank.COMMANDER);
        Officer a = commanders.get(0), b = commanders.get(1);
        a.level = 9;
        Map<Integer, Integer> claims = new HashMap<>();
        claims.put(b.id, 2);
        claims.put(a.id, 1);
        assertSame(b, Feud.heir(h, claims), "wins outweigh levels");
        claims.put(a.id, 2);
        assertSame(a, Feud.heir(h, claims), "a tie goes to the higher level");
        h.kill(a, true, 0);
        h.kill(b, true, 0);
        assertEquals(ChainRank.LIEUTENANT, Feud.heir(h, claims).rank, "with no Commander the highest rank inherits");
    }

    @Test
    void theInterregnumLastsThreeToFiveDays() {
        for (long d = 0; d < 500; d++) {
            int days = Feud.interregnumDays(ChainRules.rng(3, 0, d));
            assertTrue(days >= Feud.INTERREGNUM_MIN && days <= Feud.INTERREGNUM_MAX, "" + days);
        }
    }
}
