package org.papiricoh.supernaturalcraft.chain;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.chain.narrative.Backstory;
import org.papiricoh.supernaturalcraft.chain.narrative.Chronicle;
import org.papiricoh.supernaturalcraft.chain.narrative.Voice;
import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;
import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the chains' social life (v0.27): bonds, temperaments, a hunter's post, challenges, the front, the war. */
class ChainLifeRulesTest {
    private static Hierarchy chain(ChainFaction f) {
        Hierarchy h = new Hierarchy(f);
        Officer lord = h.recruit(ChainRank.LORD, Officer.NONE, 1, "x", 0);
        for (int i = 0; i < 2; i++) h.recruit(ChainRank.COMMANDER, lord.id, 10 + i, "x", 0);
        for (int i = 0; i < 3; i++) h.recruit(ChainRank.LIEUTENANT, Officer.NONE, 20 + i, "x", 0);
        for (int i = 0; i < 4; i++) h.recruit(ChainRank.CAPTAIN, Officer.NONE, 30 + i, "x", 0);
        return h;
    }

    // --- bonds ---------------------------------------------------------------------------------------

    @Test
    void regardIsDirectedClampedAndFadesToNothing() {
        Bonds b = new Bonds();
        var a = Bonds.Actor.of(ChainFaction.DEMON, 1);
        var c = Bonds.Actor.of(ChainFaction.ANGEL, 2);
        b.adjust(a, c, -500);
        assertEquals(Bonds.MIN, b.regard(a, c));
        assertEquals(0, b.regard(c, a));
        assertEquals(Bonds.Kind.GRUDGE, b.kind(a, c, 0));
        b.set(a, c, Bonds.FORGET + Bonds.DECAY);
        b.decay(1);
        assertEquals(Bonds.FORGET, b.regard(a, c));
        b.decay(2);
        assertTrue(b.all().isEmpty(), "a faint bond is forgotten");
    }

    @Test
    void aPactWarmsTheBondUntilItLapses() {
        Bonds b = new Bonds();
        var a = Bonds.Actor.of(ChainFaction.DEMON, 1);
        var p = Bonds.Actor.of(UUID.randomUUID());
        b.pact(a, p, 3);
        assertTrue(b.pacted(p, a, 3) && b.pacted(a, p, 2));
        assertEquals(Bonds.Kind.PACT, b.kind(a, p, 3));
        b.decay(1);
        assertEquals(Bonds.PACT_DAILY, b.regard(a, p));
        b.decay(4);
        assertFalse(b.pacted(a, p, 4));
        assertEquals(Bonds.PACT_DAILY - Bonds.DECAY, b.regard(a, p));
    }

    @Test
    void onlyTheStrongestBondsAreKept() {
        Bonds b = new Bonds();
        var a = Bonds.Actor.of(ChainFaction.DEMON, 0);
        for (int i = 1; i <= Bonds.KEEP + 50; i++) b.set(a, Bonds.Actor.of(ChainFaction.ANGEL, i), 10 + i % 80);
        b.decay(1);
        assertEquals(Bonds.KEEP, b.all().size());
        int weakestKept = b.all().stream().mapToInt(x -> Math.abs(x.regard)).min().orElseThrow();
        assertTrue(weakestKept > 10 - Bonds.DECAY);
    }

    @Test
    void actorsRoundTrip() {
        UUID id = UUID.randomUUID();
        assertEquals(id, new Bonds.Actor(Bonds.Actor.of(id).id()).uuid());
        var o = Bonds.Actor.of(ChainFaction.ANGEL, 17);
        assertEquals(ChainFaction.ANGEL, o.faction());
        assertEquals(17, o.officerId());
        assertFalse(o.player());
    }

    @Test
    void anAmbitiousUnderlingComesToHateItsSuperior() {
        Hierarchy h = chain(ChainFaction.DEMON);
        Bonds b = new Bonds();
        Officer ambitious = h.npcs().stream().filter(o -> Temperament.of(o) == Temperament.AMBITIOUS && h.superiorOf(o) != null).findFirst().orElse(null);
        if (ambitious == null) {
            ambitious = h.ofRank(ChainRank.CAPTAIN).getFirst();
            for (long seed = 0; Temperament.of(seed) != Temperament.AMBITIOUS; seed++) ambitious.nameSeed = seed + 1;
        }
        var self = Bonds.Actor.of(ambitious);
        var boss = Bonds.Actor.of(h.superiorOf(ambitious));
        for (int day = 1; day <= 60; day++) {
            b.decay(day);
            BondDrift.apply(b, h);
        }
        assertTrue(b.regard(self, boss) <= Bonds.RIVAL, "60 days under a superior it wants to replace: " + b.regard(self, boss));
    }

    @Test
    void grudgesBreakOutOnlyPastTheThresholdAndNeverUnderAPact() {
        Hierarchy h = chain(ChainFaction.ANGEL);
        Bonds b = new Bonds();
        Officer x = h.ofRank(ChainRank.CAPTAIN).getFirst(), y = h.ofRank(ChainRank.LIEUTENANT).getFirst();
        b.set(Bonds.Actor.of(x), Bonds.Actor.of(y), Bonds.GRUDGE + 1);
        assertTrue(rolls(b, h, 0).isEmpty());
        b.set(Bonds.Actor.of(x), Bonds.Actor.of(y), Bonds.MIN);
        assertTrue(rolls(b, h, 0).stream().anyMatch(o -> o.holder() == x && o.target() == y));
        b.pact(Bonds.Actor.of(x), Bonds.Actor.of(y), 5);
        assertTrue(rolls(b, h, 3).isEmpty());
    }

    /** Some day within many tries the officer's temperament lets the grudge out. */
    private static java.util.List<BondDrift.Outbreak> rolls(Bonds b, Hierarchy h, long day) {
        for (int i = 0; i < 200; i++) {
            var out = BondDrift.grudges(b, h, new GenSeed.Rng(i), day);
            if (!out.isEmpty()) return out;
        }
        return java.util.List.of();
    }

    // --- temperament and backstory -------------------------------------------------------------------

    @Test
    void temperamentIsStableAndEveryOneOccurs() {
        Map<Temperament, Integer> seen = new EnumMap<>(Temperament.class);
        for (long seed = 0; seed < 600; seed++) {
            assertEquals(Temperament.of(seed), Temperament.of(seed));
            seen.merge(Temperament.of(seed), 1, Integer::sum);
        }
        assertEquals(Temperament.values().length, seen.size());
        for (int n : seen.values()) assertTrue(n > 600 / Temperament.values().length / 2, "spread: " + seen);
    }

    @Test
    void aFallenAngelKeepsItsPastAndAddsItsFall() {
        var keys = Backstory.keys(Voice.FALLEN, 42);
        assertEquals(4, keys.size());
        assertTrue(keys.getFirst().startsWith(Backstory.PREFIX + "angel."));
        assertTrue(keys.getLast().startsWith(Backstory.PREFIX + "fallen.fall."));
        assertEquals(keys, Backstory.keys(Voice.FALLEN, 42));
    }

    @Test
    void chronicleVariantsAreStableAndAllUsed() {
        var used = new HashSet<String>();
        for (int day = 0; day < 50; day++) {
            assertEquals(Chronicle.key("feud", day, 3), Chronicle.key("feud", day, 3));
            used.add(Chronicle.key("feud", day, 3));
        }
        assertEquals(Chronicle.VARIANTS, used.size());
    }

    // --- a hunter's post ------------------------------------------------------------------------------

    @Test
    void factionRankCapsTheHuntersPostAndTheThroneIsNeverGiven() {
        assertNull(Membership.cap(0));
        assertEquals(ChainRank.SOLDIER, Membership.cap(1));
        assertEquals(ChainRank.COMMANDER, Membership.cap(4));
        assertFalse(Membership.within(ChainRank.LORD, ChainRank.COMMANDER));
        Hierarchy h = chain(ChainFaction.DEMON);
        UUID me = UUID.randomUUID();
        Officer post = Membership.join(h, me, "Hunter", ChainRank.LIEUTENANT, 7);
        assertNotNull(post);
        // Three Lieutenants already: the hunter is sworn in at the highest free rank below.
        assertEquals(ChainRank.CAPTAIN, post.rank);
        assertTrue(h.invariantsHold());
        assertNull(Membership.join(h, me, "Hunter", ChainRank.SOLDIER, 7), "one post per hunter");
        assertFalse(Membership.promote(h, post, ChainRank.CAPTAIN), "capped");
        h.remove(post);
        assertNull(h.heldBy(me));
        assertTrue(h.invariantsHold());
    }

    @Test
    void theDiceNeverMoveAHuntersPost() {
        Hierarchy demons = chain(ChainFaction.DEMON), angels = chain(ChainFaction.ANGEL);
        Officer post = Membership.join(angels, UUID.randomUUID(), "Hunter", ChainRank.SOLDIER, 9);
        for (long day = 1; day < 200; day++) {
            Conclave.run(demons, angels, 77, day, false, false, (f, b) -> new Conclave.Recruit("x", 0));
            assertTrue(post.alive(), "day " + day);
            assertEquals(ChainFaction.Branch.LOYAL, post.branch);
        }
        assertFalse(Feud.heir(angels, Map.of()) == post);
    }

    @Test
    void aSwapMendsTheTree() {
        Hierarchy h = chain(ChainFaction.DEMON);
        Officer post = Membership.join(h, UUID.randomUUID(), "Hunter", ChainRank.COMMANDER, 3);
        assertEquals(ChainRank.CAPTAIN, post.rank, "Commanders and Lieutenants full: the first free rank below");
        Officer lord = h.lord();
        Membership.swap(h, post, lord);
        assertSame(post, h.lord());
        assertEquals(ChainRank.CAPTAIN, lord.rank);
        assertTrue(h.invariantsHold());
    }

    @Test
    void aFeudWonOverADeadRivalDoesNotOverfillARank() {
        Hierarchy h = chain(ChainFaction.DEMON);
        Officer captain = h.ofRank(ChainRank.CAPTAIN).getFirst(), lieutenant = h.ofRank(ChainRank.LIEUTENANT).getFirst();
        h.kill(lieutenant, true, 0);
        // A new Lieutenant fills the dead one's post before the feud is settled.
        h.recruit(ChainRank.LIEUTENANT, Officer.NONE, 99, "x", 0);
        Feud.resolve(h, new Feud.Pair(captain, lieutenant), new GenSeed.Rng(1), 0, captain);
        assertEquals(ChainRank.CAPTAIN, captain.rank);
        assertTrue(h.invariantsHold());
    }

    @Test
    void anOverfilledTreeIsMended() {
        Hierarchy h = chain(ChainFaction.ANGEL);
        h.ofRank(ChainRank.CAPTAIN).getFirst().rank = ChainRank.LIEUTENANT;
        assertFalse(h.invariantsHold());
        assertTrue(h.repair());
        assertTrue(h.invariantsHold());
        assertFalse(h.repair());
    }

    // --- challenges ----------------------------------------------------------------------------------

    @Test
    void aChallengesDeadlineRunsOnlyWithPlay() {
        Challenge c = new Challenge(0, Challenge.Kind.OFFICER_HUNTER, Bonds.Actor.of(ChainFaction.DEMON, 1),
                Bonds.Actor.of(UUID.randomUUID()), true, 0);
        c.tickOnline(Challenge.DEADLINE_TICKS - 1);
        assertFalse(c.lapsed());
        c.tickOnline(1);
        assertTrue(c.lapsed());
        c.state = Challenge.State.STAGED;
        assertFalse(c.lapsed());
        assertFalse(Challenge.lethal(true));
    }

    // --- the front and the war -----------------------------------------------------------------------

    @Test
    void theFrontSplitsBetweenTheSeatsAndMovesWithVictories() {
        Front f = Front.initial(new SeatSite.Site(4000, 0), new SeatSite.Site(-4000, 0));
        assertEquals(ChainFaction.DEMON, f.owner(Front.sector(1000, 10)));
        assertEquals(ChainFaction.ANGEL, f.owner(Front.sector(-1000, 10)));
        assertEquals(Front.SECTORS, f.held(ChainFaction.DEMON) + f.held(ChainFaction.ANGEL));
        int sector = Front.sector(-1000, 10);
        assertFalse(f.push(sector, ChainFaction.DEMON, Front.FLIP - 1));
        assertTrue(f.push(sector, ChainFaction.DEMON, 1));
        assertEquals(ChainFaction.DEMON, f.owner(sector));
        Front back = Front.restore(f.owners(), f.scores());
        for (int i = 0; i < Front.SECTORS; i++) assertEquals(f.owner(i), back.owner(i));
    }

    @Test
    void aSideNeverLosesItsLastSector() {
        Front f = Front.initial(new SeatSite.Site(4000, 0), new SeatSite.Site(-4000, 0));
        for (int i = 0; i < Front.SECTORS; i++) f.push(i, ChainFaction.DEMON, Front.FLIP * 2);
        assertEquals(1, f.held(ChainFaction.ANGEL));
    }

    @Test
    void anOfficerBelowCourtRankRoamsItsOwnSidesSectors() {
        Front f = Front.initial(new SeatSite.Site(4000, 0), new SeatSite.Site(-4000, 0));
        Hierarchy h = chain(ChainFaction.ANGEL);
        for (Officer o : h.npcs()) {
            if (o.rank.seated()) continue;
            var site = Territory.of(5, o, new SeatSite.Site(-4000, 0), f);
            assertEquals(ChainFaction.ANGEL, f.owner(Front.sector(site.x(), site.z())), o.rank + " " + site);
        }
    }

    @Test
    void warIsRolledOnlyAfterItsIntervalAndScoredFairly() {
        assertFalse(War.rolls(new GenSeed.Rng(1), 10, 0));
        boolean any = false;
        for (int i = 0; i < 50 && !any; i++) any = War.rolls(new GenSeed.Rng(i), War.INTERVAL, 0);
        assertTrue(any);
        assertNull(War.winner(3, 3));
        assertEquals(ChainFaction.ANGEL, War.winner(1, 4));
        int d = War.days(new GenSeed.Rng(3));
        assertTrue(d >= War.MIN_DAYS && d <= War.MAX_DAYS);
    }
}
