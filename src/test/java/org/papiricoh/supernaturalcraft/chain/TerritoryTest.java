package org.papiricoh.supernaturalcraft.chain;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;

import static org.junit.jupiter.api.Assertions.*;

class TerritoryTest {

    @Test
    void onlyTheLordAndItsCommandersHoldCourt() {
        assertFalse(ChainRank.LIEUTENANT.seated());
        assertTrue(ChainRank.COMMANDER.seated() && ChainRank.LORD.seated());
    }

    @Test
    void theRanksThatHoldCourtAreAtTheirSeat() {
        for (long seed : new long[]{0, 42, -7_000_000_123L})
            for (ChainFaction f : ChainFaction.values())
                for (ChainRank rank : ChainRank.values())
                    if (rank.seated()) assertEquals(SeatSite.of(seed, f), Territory.of(seed, f, 5, rank), rank + " of " + f);
    }

    @Test
    void soldiersRoamNearTheCentreAndCaptainsTowardTheirSeat() {
        for (long seed = 0; seed < 20; seed++)
            for (ChainFaction f : ChainFaction.values()) {
                SeatSite.Site seat = SeatSite.of(seed, f);
                double seatAngle = Math.atan2(seat.z(), seat.x());
                for (int id = 0; id < 30; id++) {
                    SeatSite.Site s = Territory.of(seed, f, id, ChainRank.SOLDIER);
                    double r = Math.hypot(s.x(), s.z());
                    assertTrue(r >= Territory.SOLDIER_MIN - 1 && r <= Territory.SOLDIER_MAX + 1, "soldier at " + r);
                    SeatSite.Site c = Territory.of(seed, f, id, ChainRank.CAPTAIN);
                    double rc = Math.hypot(c.x(), c.z());
                    assertTrue(rc >= Territory.CAPTAIN_MIN - 1 && rc <= Territory.CAPTAIN_MAX + 1, "captain at " + rc);
                    double off = Math.abs(Math.IEEEremainder(Math.atan2(c.z(), c.x()) - seatAngle, Math.PI * 2));
                    assertTrue(off <= Territory.CAPTAIN_SPREAD + 1e-3, "captain off its seat's bearing by " + off);
                    SeatSite.Site l = Territory.of(seed, f, id, ChainRank.LIEUTENANT);
                    double rl = Math.hypot(l.x(), l.z());
                    assertTrue(rl >= Territory.LIEUTENANT_MIN - 1 && rl <= Territory.LIEUTENANT_MAX + 1, "lieutenant at " + rl);
                    double offL = Math.abs(Math.IEEEremainder(Math.atan2(l.z(), l.x()) - seatAngle, Math.PI * 2));
                    assertTrue(offL <= Territory.CAPTAIN_SPREAD + 1e-3, "lieutenant off its seat's bearing by " + offL);
                }
            }
    }

    @Test
    void aTerritoryIsStableAndOnlyAPromotionMovesIt() {
        Officer o = new Officer(9, ChainFaction.DEMON);
        o.rank = ChainRank.SOLDIER;
        SeatSite.Site first = Territory.of(123, o);
        o.level = 6;
        o.scars = 2;
        o.sighted("minecraft:overworld", 1, 2, 3);
        assertEquals(first, Territory.of(123, o));
        o.rank = ChainRank.CAPTAIN;
        assertNotEquals(first, Territory.of(123, o));
        assertNotEquals(first, Territory.of(123, ChainFaction.DEMON, 10, ChainRank.SOLDIER), "officers have their own territories");
    }

    @Test
    void strongholdCandidatesStayOnTheRingAndStartAtTheSeat() {
        for (long seed : new long[]{0, 42, -9}) for (ChainFaction f : ChainFaction.values()) {
            var list = SeatSite.candidates(seed, f);
            assertEquals(SeatSite.of(seed, f), list.getFirst());
            assertEquals(SeatSite.ALTERNATIVES + 1, new java.util.HashSet<>(list).size(), "every candidate is a new place");
            for (SeatSite.Site s : list) {
                double r = Math.hypot(s.x(), s.z());
                assertTrue(r >= SeatSite.MIN_DISTANCE && r <= SeatSite.MAX_DISTANCE, "candidate off the ring at " + r);
            }
        }
    }
}
