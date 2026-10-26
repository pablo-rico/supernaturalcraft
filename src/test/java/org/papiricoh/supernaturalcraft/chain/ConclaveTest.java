package org.papiricoh.supernaturalcraft.chain;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConclaveTest {
    private static final Conclave.RecruitSource SOURCE = (f, b) -> new Conclave.Recruit("test:" + f.id() + "_" + b.name(), 2);

    @Test void fiveHundredDaysAreDeterministicAndAlwaysRespectCaps() {
        assertEquals(simulate(42), simulate(42));
        assertNotEquals(simulate(42), simulate(43));
    }

    private List<String> simulate(long seed) {
        Hierarchy demons = HierarchyTest.full(ChainFaction.DEMON), angels = HierarchyTest.full(ChainFaction.ANGEL);
        GenSeed.Rng external = new GenSeed.Rng(seed);
        List<String> trace = new ArrayList<>();
        EnumSet<Conclave.EventType> seen = EnumSet.noneOf(Conclave.EventType.class);
        long demonUntil = 0, angelUntil = 0;
        for (long day = 1; day <= 500; day++) {
            for (Hierarchy h : List.of(demons, angels)) {
                if (!h.alive().isEmpty() && external.chance(.35)) {
                    Officer victim = external.pick(h.alive());
                    if (victim.rank == ChainRank.LORD) {
                        if (h == demons) demonUntil = day + ChainRules.destabilizeDays();
                        else angelUntil = day + ChainRules.destabilizeDays();
                    }
                    h.kill(victim, external.chance(.5), day + 2);
                }
                if (external.chance(.3)) h.recruit(ChainRank.SOLDIER, Officer.NONE, external.nextLong(), "test:recruit", external.nextInt(5));
            }
            List<Conclave.ChainEvent> events = Conclave.run(demons, angels, seed, day, day < demonUntil, day < angelUntil, SOURCE);
            for (var event : events) {
                seen.add(event.type());
                if (event.lordKilled()) {
                    if (event.otherFaction() == ChainFaction.DEMON) demonUntil = day + ChainRules.destabilizeDays();
                    else angelUntil = day + ChainRules.destabilizeDays();
                }
            }
            trace.add(events.toString());
            for (Hierarchy h : List.of(demons, angels)) {
                assertTrue(h.invariantsHold(), "Day " + day + " " + h.faction());
                assertTrue(h.alive().size() <= 20);
                for (ChainRank rank : ChainRank.values()) assertTrue(h.ofRank(rank).size() <= rank.cap());
                trace.add(snapshot(h));
            }
        }
        assertTrue(seen.containsAll(EnumSet.allOf(Conclave.EventType.class)), seen.toString());
        return trace;
    }

    private String snapshot(Hierarchy h) {
        StringBuilder result = new StringBuilder(h.faction() + ":" + h.nextId());
        for (Officer o : h.all()) result.append('|').append(o.id).append(':').append(o.rank).append(':').append(o.branch)
                .append(':').append(o.status).append(':').append(o.level).append(':').append(o.scars).append(':').append(o.superior)
                .append(':').append(o.returnDay).append(':').append(o.nameSeed).append(':').append(o.entityType).append(':').append(o.variant)
                .append(':').append(o.history);
        return result.toString();
    }

    @Test void destabilizationSuppressesEventsButNotDueReturns() {
        Hierarchy demons = HierarchyTest.full(ChainFaction.DEMON), angels = HierarchyTest.full(ChainFaction.ANGEL);
        Officer o = demons.ofRank(ChainRank.SOLDIER).getFirst();
        demons.kill(o, false, 10);
        assertTrue(Conclave.run(demons, angels, 5, 9, true, true, SOURCE).isEmpty());
        List<Conclave.ChainEvent> events = Conclave.run(demons, angels, 5, 10, true, true, SOURCE);
        assertEquals(1, events.size());
        assertEquals(Conclave.EventType.REVIVE, events.getFirst().type());
        assertEquals(o.id, events.getFirst().officerId());
        assertTrue(o.alive());
    }

    @Test void recruitmentStartsLowAndPromotesBeforeCreatingHigherOfficers() {
        for (int seed = 0; seed < 100; seed++) {
            Hierarchy demons = new Hierarchy(ChainFaction.DEMON), angels = new Hierarchy(ChainFaction.ANGEL);
            for (int i = 0; i < 10; i++) HierarchyTest.add(demons, ChainRank.SOLDIER);
            var events = Conclave.run(demons, angels, seed, 1, false, true, SOURCE);
            if (events.stream().anyMatch(e -> e.type() == Conclave.EventType.PROMOTE)) {
                assertEquals(10, demons.all().size());
                assertEquals(9, demons.ofRank(ChainRank.SOLDIER).size());
                assertEquals(1, demons.ofRank(ChainRank.CAPTAIN).size());
                assertTrue(demons.invariantsHold());
                return;
            }
        }
        fail("No promotion event occurred");
    }

    @Test void emptyChainsCanGrowAndRecruitSourceCanDecline() {
        Hierarchy demons = new Hierarchy(ChainFaction.DEMON), angels = new Hierarchy(ChainFaction.ANGEL);
        for (int day = 1; day <= 50; day++) {
            assertTrue(Conclave.run(demons, angels, 0, day, false, false, (f, b) -> null).isEmpty());
        }
        assertTrue(demons.all().isEmpty());
        for (int day = 51; day <= 150; day++) Conclave.run(demons, angels, 0, day, false, false, SOURCE);
        assertFalse(demons.alive().isEmpty());
        assertFalse(angels.alive().isEmpty());
        assertTrue(demons.invariantsHold());
        assertTrue(angels.invariantsHold());
    }

    @Test void fullRankChallengesAndFallenSkirmishesAreResolved() {
        boolean struggle = false, fallenCombat = false;
        for (int seed = 0; seed < 300; seed++) {
            Hierarchy demons = new Hierarchy(ChainFaction.DEMON), angels = HierarchyTest.full(ChainFaction.ANGEL);
            Officer fallen = angels.ofRank(ChainRank.CAPTAIN).getFirst();
            angels.fall(fallen);
            var events = Conclave.run(demons, angels, seed, 1, false, false, SOURCE);
            for (var event : events) {
                struggle |= event.type() == Conclave.EventType.POWER_STRUGGLE;
                if (event.type() == Conclave.EventType.SKIRMISH && event.faction() == ChainFaction.ANGEL
                        && event.otherFaction() == ChainFaction.ANGEL) {
                    fallenCombat = true;
                    assertTrue(event.officerId() == fallen.id || event.otherId() == fallen.id);
                    assertFalse(angels.get(event.otherId()).alive());
                }
            }
            assertTrue(demons.invariantsHold());
            assertTrue(angels.invariantsHold());
        }
        assertTrue(struggle);
        assertTrue(fallenCombat);
    }
}
