package org.papiricoh.supernaturalcraft.crossroads;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal.State;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms.Event;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms.Wish;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DealTermsTest {

    private static final long DAY = DealTerms.DAY;

    @Test
    void termsDependOnTheWish() {
        assertEquals(5, Wish.UPGRADE.days);
        assertEquals(7, Wish.RECOVER.days);
        assertEquals(10, Wish.RARE_ITEM.days);
        assertEquals(10, Wish.KNOWLEDGE.days);
        assertEquals(1000 + 5 * DAY, DealTerms.dueAt(1000, Wish.UPGRADE));
        assertEquals(7 * DAY, DealTerms.dueAt(0, Wish.RECOVER));
    }

    @Test
    void wishIdsRoundTrip() {
        for (Wish w : Wish.values()) {
            assertSame(w, Wish.byId(w.id()));
            for (int a = 0; a < w.variants; a++) {
                assertTrue(w.validArg(a));
                assertEquals(DealTerms.keyOf(w.clause(a)), w.key(a));
            }
            assertFalse(w.validArg(w.variants));
            assertFalse(w.validArg(-1));
        }
        assertNull(Wish.byId("eternal_youth"));
        assertEquals("deal.supernaturalcraft.wish.upgrade.1", Wish.UPGRADE.key(1));
    }

    @Test
    void dueDaysAndTheLastDay() {
        long due = DealTerms.dueAt(0, Wish.UPGRADE);
        assertFalse(DealTerms.due(due - 1, due));
        assertTrue(DealTerms.due(due, due));
        assertEquals(5, DealTerms.daysLeft(0, due));
        assertEquals(5, DealTerms.daysLeft(1, due));
        assertEquals(1, DealTerms.daysLeft(due - 1, due));
        assertEquals(0, DealTerms.daysLeft(due, due));
        assertFalse(DealTerms.lastDay(due - DAY - 1, due));
        assertTrue(DealTerms.lastDay(due - DAY, due));
        assertTrue(DealTerms.lastDay(due - 1, due));
        assertFalse(DealTerms.lastDay(due, due), "once due it is the hunt, not the omen");
        assertEquals(24, DealTerms.hoursLeft(due - DAY, due));
        assertEquals(1, DealTerms.hoursLeft(due - 1, due));
        assertEquals(0, DealTerms.hoursLeft(due + 5, due));
    }

    @Test
    void packsAreThreeToFive() {
        Set<Integer> sizes = new HashSet<>();
        for (int roll = -50; roll < 50; roll++) {
            int n = DealTerms.packSize(roll);
            assertTrue(n >= 3 && n <= 5, "pack of " + n);
            sizes.add(n);
        }
        assertEquals(Set.of(3, 4, 5), sizes);
        assertTrue(DealTerms.packSize(Integer.MIN_VALUE) >= 3);
    }

    @Test
    void survivingTakesTwoMinutes() {
        assertFalse(DealTerms.survived(1000 + 2399, 1000));
        assertTrue(DealTerms.survived(1000 + 2400, 1000));
        assertFalse(DealTerms.survived(1000, 1200), "a hunt that has not started yet is not survived");
    }

    @Test
    void upgradesHaveACeiling() {
        assertTrue(DealTerms.canUpgrade(0, 0, 0));
        assertTrue(DealTerms.canUpgrade(8, 0, 0));
        assertFalse(DealTerms.canUpgrade(10, 0, 0));
        assertTrue(DealTerms.canUpgrade(10, 75, 1));
        assertFalse(DealTerms.canUpgrade(0, 100, 1));
    }

    @Test
    void theDemonReturnsOnALaterNight() {
        long dusk = 13000, lateNight = 22000, nextNoon = DAY + 6000, nextDusk = DAY + 13000;
        assertEquals(DealTerms.nightIndex(dusk), DealTerms.nightIndex(lateNight));
        assertEquals(DealTerms.nightIndex(dusk), DealTerms.nightIndex(DAY + 1000), "dawn still belongs to the same night");
        assertFalse(DealTerms.demonReturns(true, lateNight, dusk), "not the same night");
        assertFalse(DealTerms.demonReturns(false, nextNoon, dusk), "not by day");
        assertTrue(DealTerms.demonReturns(true, nextDusk, dusk));
        assertTrue(DealTerms.demonReturns(true, 3 * DAY + 14000, 0), "a demon gone long ago returns too");
    }

    @Test
    void penalties() {
        assertSame(DealTerms.Penalty.LOSE_UPGRADE, DealTerms.penalty(Wish.UPGRADE));
        assertSame(DealTerms.Penalty.SOULLESS, DealTerms.penalty(Wish.RECOVER));
        assertSame(DealTerms.Penalty.SOULLESS, DealTerms.penalty(Wish.RARE_ITEM));
        assertSame(DealTerms.Penalty.SOULLESS, DealTerms.penalty(Wish.KNOWLEDGE));
        assertEquals(72000, DealTerms.SOULLESS_TICKS);
    }

    @Test
    void stateMachine() {
        // Sealing: only without an outstanding debt.
        assertSame(State.OPEN, DealTerms.next(State.NONE, Event.SEAL, true));
        assertSame(State.OPEN, DealTerms.next(State.FREE, Event.SEAL, true));
        assertSame(State.OPEN, DealTerms.next(State.COLLECTED, Event.SEAL, true));
        assertSame(State.COLLECTING, DealTerms.next(State.COLLECTING, Event.SEAL, true));
        // Due: the hounds come, even for a demon still being hunted.
        assertSame(State.COLLECTING, DealTerms.next(State.OPEN, Event.DUE, false));
        assertSame(State.COLLECTING, DealTerms.next(State.HUNTED, Event.DUE, false));
        assertSame(State.FREE, DealTerms.next(State.FREE, Event.DUE, false));
        // Surviving the hunt.
        assertSame(State.FREE, DealTerms.next(State.COLLECTING, Event.HOUNDS_SLAIN, false));
        assertSame(State.FREE, DealTerms.next(State.COLLECTING, Event.SURVIVED, false));
        assertSame(State.OPEN, DealTerms.next(State.OPEN, Event.HOUNDS_SLAIN, true));
        // Breaking the deal, and killing the demon in time.
        assertSame(State.HUNTED, DealTerms.next(State.OPEN, Event.BREAK, true));
        assertSame(State.OPEN, DealTerms.next(State.OPEN, Event.BREAK, false), "too late to break it");
        assertSame(State.COLLECTING, DealTerms.next(State.COLLECTING, Event.BREAK, true));
        assertSame(State.FREE, DealTerms.next(State.HUNTED, Event.DEMON_SLAIN, true));
        assertSame(State.HUNTED, DealTerms.next(State.HUNTED, Event.DEMON_SLAIN, false), "after the due date it is the hounds");
        assertSame(State.OPEN, DealTerms.next(State.OPEN, Event.DEMON_SLAIN, true));
        // Dying: only with the hounds out does it cost the soul.
        assertSame(State.COLLECTED, DealTerms.next(State.COLLECTING, Event.DIED, false));
        assertSame(State.OPEN, DealTerms.next(State.OPEN, Event.DIED, true));
        assertSame(State.HUNTED, DealTerms.next(State.HUNTED, Event.DIED, true));
        // Active states.
        assertTrue(DealTerms.active(State.OPEN));
        assertTrue(DealTerms.active(State.COLLECTING));
        assertTrue(DealTerms.active(State.HUNTED));
        assertFalse(DealTerms.active(State.NONE));
        assertFalse(DealTerms.active(State.FREE));
        assertFalse(DealTerms.active(State.COLLECTED));
    }

    // --- Wild bargains (v0.18) -------------------------------------------------------------------------------------------------

    @Test
    void wildWishesAreWildOnly() {
        Set<Wish> wild = EnumSet.noneOf(Wish.class);
        for (Wish w : Wish.values()) if (w.wildOnly) wild.add(w);
        assertEquals(EnumSet.of(Wish.ASCEND, Wish.TROPHY, Wish.REVIVE, Wish.UNCURSE), wild);
        assertEquals(8, Wish.ASCEND.days);
        assertEquals(10, Wish.TROPHY.days);
        assertEquals(7, Wish.REVIVE.days);
        assertEquals(6, Wish.UNCURSE.days);
        assertEquals(DealTerms.TROPHY_BOSSES.size(), Wish.TROPHY.variants);
        assertEquals(DealTerms.Affliction.values().length, Wish.UNCURSE.variants);
        assertEquals(4, Wish.UNCURSE.variants);
        for (Wish w : wild) assertSame(DealTerms.Penalty.SOULLESS, DealTerms.penalty(w), "a wild wish costs the soul's warmth");
    }

    @Test
    void ordinaryTermsAreUntouched() {
        for (Wish w : Wish.values()) {
            for (double f : new double[]{0.1, 0.5, 1.0}) assertEquals(w.days, DealTerms.daysFor(w, false, f), "a bowl deal keeps its term");
        }
        assertEquals(1000 + 5 * DAY, DealTerms.dueAt(1000, 5));
        assertEquals(DealTerms.dueAt(1000, Wish.KNOWLEDGE), DealTerms.dueAt(1000, Wish.KNOWLEDGE.days));
        for (int roll = -20; roll < 20; roll++) assertEquals(DealTerms.packSize(roll), DealTerms.packSize(roll, false));
        assertEquals(DealTerms.SURVIVE_TICKS, DealTerms.surviveTicks(false));
        assertFalse(DealTerms.soulClausePreTicked(false));
    }

    @Test
    void wildTermsAreShorter() {
        assertEquals(3, DealTerms.daysFor(Wish.UPGRADE, true, 0.5), "half of five, rounded up");
        assertEquals(5, DealTerms.daysFor(Wish.RARE_ITEM, true, 0.5));
        assertEquals(4, DealTerms.daysFor(Wish.ASCEND, true, 0.5));
        assertEquals(5, DealTerms.daysFor(Wish.TROPHY, true, 0.5));
        assertEquals(4, DealTerms.daysFor(Wish.REVIVE, true, 0.5));
        assertEquals(3, DealTerms.daysFor(Wish.UNCURSE, true, 0.5));
        assertEquals(1, DealTerms.daysFor(Wish.UPGRADE, true, 0.1), "never less than a day");
        assertEquals(10, DealTerms.daysFor(Wish.KNOWLEDGE, true, 1.0), "a factor of one changes nothing");
        assertEquals(0, DealTerms.daysFor(Wish.CONVERT, true, 0.5), "settled at once stays settled at once");
        for (Wish w : Wish.values()) assertTrue(DealTerms.daysFor(w, true, 0.5) <= w.days);
        assertEquals(5, DealTerms.daysFor(Wish.UPGRADE, true, 0.5, 2), "a memory's grace comes on top");
        assertEquals(7, DealTerms.daysFor(Wish.UPGRADE, false, 0.5, 2));
        assertEquals(5, DealTerms.daysFor(Wish.UPGRADE, false, 0.5, -3), "never less than the term");
        assertEquals(0, DealTerms.daysFor(Wish.CONVERT, false, 0.5, 2), "nothing to add to a wish settled at once");
    }

    @Test
    void wildHuntsAreHarder() {
        Set<Integer> sizes = new HashSet<>();
        for (int roll = -50; roll < 50; roll++) sizes.add(DealTerms.packSize(roll, true));
        assertEquals(Set.of(5, 6, 7), sizes);
        assertTrue(DealTerms.packSize(Integer.MIN_VALUE, true) >= 5);
        assertEquals(3600, DealTerms.surviveTicks(true));
        assertFalse(DealTerms.survived(1000 + 3599, 1000, true));
        assertTrue(DealTerms.survived(1000 + 3600, 1000, true));
        assertTrue(DealTerms.survived(1000 + 2400, 1000, false));
        assertTrue(DealTerms.soulClausePreTicked(true));
    }

    @Test
    void ascensionIsCapped() {
        assertEquals(1, DealTerms.ascendCap(1, 5), "a hunter's own tier");
        assertEquals(4, DealTerms.ascendCap(5, 5), "never past IV");
        assertEquals(3, DealTerms.ascendCap(5, 3), "nor past the weapon's own limit");
        assertEquals(0, DealTerms.ascendCap(5, 0), "nothing that cannot ascend");
        assertTrue(DealTerms.canAscend(0, 1, 5));
        assertFalse(DealTerms.canAscend(1, 1, 5));
        assertTrue(DealTerms.canAscend(3, 5, 5));
        assertFalse(DealTerms.canAscend(4, 5, 5));
    }

    @Test
    void trophiesOnlyForTheBeaten() {
        assertTrue(DealTerms.trophyOffer(b -> false).isEmpty());
        assertEquals(List.of(DealTerms.TROPHY_BOSSES.indexOf(Boss.AZAZEL)), DealTerms.trophyOffer(b -> b == Boss.AZAZEL));
        Set<Boss> beaten = EnumSet.of(Boss.AZAZEL, Boss.LILITH, Boss.LUCIFER, Boss.WAR, Boss.METATRON);
        List<Integer> offer = DealTerms.trophyOffer(beaten::contains);
        assertEquals(DealTerms.TROPHY_OFFERS, offer.size(), "no more than three at once");
        assertEquals(List.of(Boss.METATRON, Boss.WAR, Boss.LUCIFER), offer.stream().map(DealTerms::trophyBoss).toList(),
                "the furthest along the road first");
        assertEquals(DealTerms.TROPHY_BOSSES.size(), Set.copyOf(DealTerms.TROPHY_BOSSES).size(), "each boss once");
        assertFalse(DealTerms.TROPHY_BOSSES.contains(Boss.CHUCK), "the Author leaves no bust");
        assertNull(DealTerms.trophyBoss(-1));
        assertNull(DealTerms.trophyBoss(DealTerms.TROPHY_BOSSES.size()));
        // The order is a save format: the first entries never move.
        assertSame(Boss.AZAZEL, DealTerms.trophyBoss(0));
        assertSame(Boss.MICHAEL, DealTerms.trophyBoss(14));
    }

    @Test
    void afflictionsRoundTrip() {
        for (DealTerms.Affliction a : DealTerms.Affliction.values()) assertSame(a, DealTerms.Affliction.of(a.ordinal()));
        assertNull(DealTerms.Affliction.of(4));
        assertNull(DealTerms.Affliction.of(-1));
        assertEquals("heavens_mark", DealTerms.Affliction.HEAVENS_MARK.id());
    }

    @Test
    void theCrossroadsAnswersOnceANight() {
        long dusk = 13000, lateNight = 22000, nextDusk = DAY + 13000;
        assertTrue(DealTerms.wildAnswers(true, dusk, CrossroadsDeal.NEVER), "the first box is always answered");
        assertFalse(DealTerms.wildAnswers(false, 6000, CrossroadsDeal.NEVER), "but never by day");
        long spent = DealTerms.nightIndex(dusk);
        assertFalse(DealTerms.wildAnswers(true, lateNight, spent), "once a night");
        assertTrue(DealTerms.wildAnswers(true, nextDusk, spent), "the next night it answers again");
    }
}
