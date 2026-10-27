package org.papiricoh.supernaturalcraft.chain;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;
import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** v0.28: the chains are no longer two (the Leviathans of SucroCorp join the Pit and the Host). */
class ThreeFactionsTest {

    private static final Conclave.RecruitSource SOURCE = (f, b) -> new Conclave.Recruit("test:" + f.id() + "_" + b.name(), 0);

    @Test
    void everyFactionFightsEveryOther() {
        for (ChainFaction f : ChainFaction.values()) {
            assertEquals(ChainFaction.values().length - 1, f.rivals().size());
            assertFalse(f.rivals().contains(f));
            for (ChainFaction g : f.rivals()) assertTrue(ChainFaction.hostile(f, g));
        }
        assertFalse(ChainFaction.LEVIATHAN.talks());
        assertFalse(ChainFaction.LEVIATHAN.joinable());
        assertNull(ChainFaction.ofSide(ChainFaction.Side.NONE));
        assertEquals(ChainFaction.DEMON, ChainFaction.ofSide(ChainFaction.Side.HELL));
    }

    @Test
    void theFirstTwoSeatsNeverMoveAndTheThirdStandsApart() {
        for (long seed : new long[]{1, 42, -7, 123456789}) {
            SeatSite.Site demon = SeatSite.of(seed, ChainFaction.DEMON), angel = SeatSite.of(seed, ChainFaction.ANGEL),
                    board = SeatSite.of(seed, ChainFaction.LEVIATHAN);
            // Before v0.28 the Host's seat was the Pit's turned half round: it still is.
            assertEquals(Math.PI, gap(bearing(demon), bearing(angel)), 1e-3);
            assertEquals(ChainFaction.LEVIATHAN.seatOffset(), gap(bearing(demon), bearing(board)), 1e-3);
            double r = Math.hypot(board.x(), board.z());
            assertTrue(r >= SeatSite.MIN_DISTANCE && r <= SeatSite.MAX_DISTANCE, "radius " + r);
        }
    }

    @Test
    void aLateFactionTakesItsShareWithoutEmptyingAnyone() {
        Front f = Front.initial(Map.of(ChainFaction.DEMON, new SeatSite.Site(4000, 0), ChainFaction.ANGEL, new SeatSite.Site(-4000, 0)));
        assertEquals(0, f.held(ChainFaction.LEVIATHAN));
        int taken = f.admit(ChainFaction.LEVIATHAN, new SeatSite.Site(0, 4000));
        assertEquals(Front.SECTORS / 3, taken);
        assertEquals(taken, f.held(ChainFaction.LEVIATHAN));
        assertEquals(ChainFaction.LEVIATHAN, f.owner(Front.sector(0, 1000)), "the sectors round its own seat");
        assertTrue(f.held(ChainFaction.DEMON) > 0 && f.held(ChainFaction.ANGEL) > 0);
        assertEquals(0, f.admit(ChainFaction.LEVIATHAN, new SeatSite.Site(0, 4000)), "admitted once");
    }

    @Test
    void pressureFromAnyRivalFlipsASectorAndTheHolderClearsIt() {
        Front f = Front.initial(Map.of(ChainFaction.DEMON, new SeatSite.Site(4000, 0), ChainFaction.ANGEL, new SeatSite.Site(-4000, 0)));
        f.admit(ChainFaction.LEVIATHAN, new SeatSite.Site(0, 4000));
        int sector = Front.sector(1000, 10);
        assertEquals(ChainFaction.DEMON, f.owner(sector));
        assertFalse(f.push(sector, ChainFaction.LEVIATHAN, Front.FLIP - 1));
        assertFalse(f.push(sector, ChainFaction.ANGEL, Front.FLIP - 1));
        assertEquals(Front.FLIP - 1, f.pressure(sector, ChainFaction.LEVIATHAN));
        assertFalse(f.push(sector, ChainFaction.DEMON, 1), "a holder's win");
        assertEquals(0, f.pressure(sector, ChainFaction.LEVIATHAN), "clears every rival's pressure");
        assertTrue(f.push(sector, ChainFaction.LEVIATHAN, Front.FLIP));
        assertEquals(ChainFaction.LEVIATHAN, f.owner(sector));
        Front back = Front.restore(f.owners(), f.scores());
        for (int i = 0; i < Front.SECTORS; i++) assertEquals(f.owner(i), back.owner(i));
    }

    @Test
    void aBeheadedFactionsGroundGoesToItsNeighbours() {
        Front f = Front.initial(Map.of(ChainFaction.DEMON, new SeatSite.Site(4000, 0), ChainFaction.ANGEL, new SeatSite.Site(-4000, 0)));
        f.admit(ChainFaction.LEVIATHAN, new SeatSite.Site(0, 4000));
        int demon = f.held(ChainFaction.DEMON), angel = f.held(ChainFaction.ANGEL), board = f.held(ChainFaction.LEVIATHAN);
        assertEquals(board, f.release(ChainFaction.LEVIATHAN));
        assertEquals(0, f.held(ChainFaction.LEVIATHAN));
        assertEquals(Front.SECTORS, f.held(ChainFaction.DEMON) + f.held(ChainFaction.ANGEL));
        assertTrue(f.held(ChainFaction.DEMON) > demon && f.held(ChainFaction.ANGEL) > angel, "both neighbours gain");
    }

    @Test
    void anOldSignedFrontReadsAsTwoFactionsPressure() {
        byte[] owners = new byte[Front.SECTORS];
        int[] old = new int[Front.SECTORS];
        old[2] = 4;
        old[5] = -3;
        Front f = Front.restore(owners, old);
        assertEquals(4, f.pressure(2, ChainFaction.DEMON));
        assertEquals(3, f.pressure(5, ChainFaction.ANGEL));
        assertEquals(0, f.pressure(5, ChainFaction.LEVIATHAN));
    }

    @Test
    void warPairsTwoDifferentAwakeFactions() {
        assertNull(War.pair(new GenSeed.Rng(1), List.of(ChainFaction.DEMON)));
        EnumSet<ChainFaction> seen = EnumSet.noneOf(ChainFaction.class);
        for (int i = 0; i < 60; i++) {
            ChainFaction[] p = War.pair(new GenSeed.Rng(i), List.of(ChainFaction.values()));
            assertNotEquals(p[0], p[1]);
            seen.add(p[0]);
            seen.add(p[1]);
        }
        assertEquals(EnumSet.allOf(ChainFaction.class), seen);
        assertEquals(ChainFaction.LEVIATHAN, War.winner(ChainFaction.ANGEL, 2, ChainFaction.LEVIATHAN, 5));
    }

    @Test
    void threeChainsKeepTheirShapeAndSkirmishOnlyAcrossCamps() {
        Map<ChainFaction, Hierarchy> chains = new EnumMap<>(ChainFaction.class);
        for (ChainFaction f : ChainFaction.values()) chains.put(f, HierarchyTest.full(f));
        boolean across = false;
        for (long day = 1; day <= 200; day++) {
            for (Conclave.ChainEvent e : Conclave.run(chains, 31, day, EnumSet.noneOf(ChainFaction.class), true, SOURCE)) {
                if (e.type() != Conclave.EventType.SKIRMISH) continue;
                Officer a = chains.get(e.faction()).get(e.officerId()), b = chains.get(e.otherFaction()).get(e.otherId());
                assertNotEquals(a.camp(), b.camp(), "a skirmish is fought across camps");
                if (e.faction() == ChainFaction.LEVIATHAN || e.otherFaction() == ChainFaction.LEVIATHAN) across = true;
            }
            for (Hierarchy h : chains.values()) assertTrue(h.invariantsHold(), "day " + day + " " + h.faction());
        }
        assertTrue(across, "the Leviathans skirmish too");
        for (Officer o : chains.get(ChainFaction.LEVIATHAN).all()) assertEquals(ChainFaction.Branch.LOYAL, o.branch, "no Leviathan falls");
    }

    @Test
    void theBossOnTheThroneIsOutOfTheDicesHands() {
        Hierarchy board = HierarchyTest.full(ChainFaction.LEVIATHAN);
        Officer lord = board.lord();
        lord.entityType = Officer.BOSS_TYPE;
        assertTrue(lord.boss() && lord.held() && !lord.hunter());
        assertFalse(board.npcs().contains(lord));
        assertEquals(ChainNames.BOSS_NAME, ChainNames.name(lord));
        Map<ChainFaction, Hierarchy> chains = new EnumMap<>(ChainFaction.class);
        chains.put(ChainFaction.LEVIATHAN, board);
        for (long day = 1; day <= 120; day++) {
            Conclave.run(chains, 9, day, EnumSet.noneOf(ChainFaction.class), true, SOURCE);
            assertSame(lord, board.lord(), "day " + day);
            assertTrue(lord.alive());
        }
    }

    @Test
    void sucroCorpGoesByItsOwnTitles() {
        assertEquals("chain.supernaturalcraft.rank.leviathan.lord", ChainRank.LORD.titleKey(ChainFaction.LEVIATHAN));
        assertEquals("chain.supernaturalcraft.rank.lord", ChainRank.LORD.titleKey(ChainFaction.DEMON));
        assertEquals("chain.supernaturalcraft.rank.captain", ChainRank.titleKey("angel", "CAPTAIN"));
    }

    private static double bearing(SeatSite.Site s) {
        return Math.atan2(s.z(), s.x());
    }

    /** The angle from {@code a} round to {@code b}, in [0, 2π). */
    private static double gap(double a, double b) {
        return Math.floorMod(Math.round((b - a) * 1e6), Math.round(2 * Math.PI * 1e6)) / 1e6;
    }
}
