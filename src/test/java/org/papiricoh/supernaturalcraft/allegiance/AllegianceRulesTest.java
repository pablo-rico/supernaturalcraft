package org.papiricoh.supernaturalcraft.allegiance;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerRules;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AllegianceRulesTest {

    @Test
    void everyoneStartsAFreeHuman() {
        Allegiance a = Allegiance.HUMAN;
        assertTrue(a.isHuman());
        assertFalse(a.committed());
        assertTrue(a.mayChoose(0));
        assertEquals(0, a.maxEssence());
    }

    @Test
    void ranksAndEssenceAreClamped() {
        Allegiance angel = Allegiance.HUMAN.convert(Faction.ANGEL);
        assertEquals(1, angel.rank());
        assertEquals(50, angel.essence());
        assertEquals(4, angel.withRank(9).rank());
        assertEquals(100, angel.addEssence(500).essence());
        assertEquals(0, angel.addEssence(-500).essence());
        assertEquals(250, angel.withRank(4).addEssence(1000).essence());
        assertEquals(3, Allegiance.HUMAN.withRank(4).rank(), "a hunter tops out at Legend");
    }

    @Test
    void aCureLeavesAHumanWhoMustWait() {
        Allegiance demon = Allegiance.HUMAN.convert(Faction.DEMON).withToll(2).withRank(3);
        Allegiance cured = demon.cured(1000 + CureProgress.CHOOSE_AGAIN_TICKS);
        assertTrue(cured.isHuman());
        assertEquals(0, cured.rank());
        assertEquals(0, cured.tollHearts());
        assertFalse(cured.mayChoose(1000));
        assertTrue(cured.mayChoose(1000 + CureProgress.CHOOSE_AGAIN_TICKS));
    }

    @Test
    void rankAdvancementsNameEachRoad() {
        assertEquals("main/angel_2", Ranks.advancement(Faction.ANGEL, 2));
        assertEquals("main/demon_4", Ranks.advancement(Faction.DEMON, 4));
        assertEquals("main/hunter_1", Ranks.advancement(Faction.HUMAN, 1));
        assertNull(Ranks.advancement(Faction.ANGEL, 0));
        assertEquals("IV", Ranks.roman(4));
    }

    @Test
    void killsFeedOnlyTheOtherSide() {
        assertTrue(EssenceRules.forKill(Faction.ANGEL, EssenceRules.Kind.DEMON) > 0);
        assertEquals(0, EssenceRules.forKill(Faction.ANGEL, EssenceRules.Kind.ANGEL));
        assertTrue(EssenceRules.forKill(Faction.DEMON, EssenceRules.Kind.HUNTER) > 0);
        assertEquals(0, EssenceRules.forKill(Faction.HUMAN, EssenceRules.Kind.DEMON));
        assertEquals(0, EssenceRules.forPlayerKill(Faction.ANGEL, Faction.DEMON, false), "no PvP, no reward");
        assertTrue(EssenceRules.forPlayerKill(Faction.ANGEL, Faction.DEMON, true) > 0);
        assertEquals(0, EssenceRules.forPlayerKill(Faction.ANGEL, Faction.ANGEL, true));
        assertTrue(EssenceRules.sanctuary(true, true) > EssenceRules.sanctuary(true, false));
        assertEquals(0, EssenceRules.sanctuary(false, true));
        assertFalse(EssenceRules.starving(Faction.DEMON, 2, 100000), "only a Knight thirsts");
        assertTrue(EssenceRules.starving(Faction.DEMON, 3, EssenceRules.BLOODLUST_GRACE_SECONDS + 1));
    }

    @Test
    void theCureTakesThreeDifferentNights() {
        long n0 = CureProgress.nightIndex(13000), n1 = CureProgress.nightIndex(13000 + 24000);
        assertEquals(CureProgress.nightIndex(13000), CureProgress.nightIndex(23999), "one night runs past midnight");
        assertTrue(CureProgress.canAdvance(0, -1, n0));
        assertFalse(CureProgress.canAdvance(1, n0, n0), "twice in one night does nothing");
        assertTrue(CureProgress.canAdvance(1, n0, n1));
        assertFalse(CureProgress.canAdvance(CureProgress.NIGHTS, n0, n1 + 5));
        assertTrue(CureProgress.complete(CureProgress.NIGHTS));
    }

    @Test
    void theMessengerComesAtDawnAndReturnsWhenTurnedAway() {
        Allegiance a = Allegiance.HUMAN;
        assertFalse(MessengerSchedule.visits(a, 24000), "not before Azazel");
        Allegiance due = MessengerSchedule.azazelSlain(a, 14000);
        assertFalse(MessengerSchedule.visits(due, 18000), "not at midnight");
        assertTrue(MessengerSchedule.visits(due, 24000 + 200), "the next dawn");
        Allegiance declined = MessengerSchedule.declined(due, 24000 + 200);
        assertFalse(MessengerSchedule.visits(declined, 2 * 24000 + 200));
        assertTrue(MessengerSchedule.visits(declined, (1 + MessengerSchedule.RETURN_DAYS) * 24000L + 200));
        assertFalse(MessengerSchedule.visits(MessengerSchedule.heeded(due), 5 * 24000L), "heeded, he only comes when called");
        assertFalse(MessengerSchedule.visits(due.convert(Faction.DEMON), 24000 + 200), "never to a demon");
        assertSame(due, MessengerSchedule.azazelSlain(due, 99999), "a second Azazel does not reschedule him");
    }

    @Test
    void powersBelongToOneSideAndRankUp() {
        Set<String> ids = new HashSet<>();
        for (Power p : Power.values()) {
            assertTrue(ids.add(p.id()));
            assertTrue(p.minRank >= 1 && p.minRank <= p.faction.maxRank(), p.id());
            assertTrue(p.passive == (p.cost == 0 && p.cooldown == 0), p.id() + ": passives cost nothing, actives something");
            if (p.faction == Faction.HUMAN) assertTrue(p.passive, "hunters have no wheel");
        }
        assertTrue(Power.wheel(Faction.ANGEL, 1).contains(Power.TELEPORT));
        assertFalse(Power.wheel(Faction.ANGEL, 1).contains(Power.SMITE));
        assertTrue(Power.wheel(Faction.ANGEL, 4).contains(Power.HOST_SQUAD));
        assertFalse(Power.wheel(Faction.DEMON, 4).contains(Power.TELEPORT));
        assertTrue(Power.wheel(Faction.HUMAN, 3).isEmpty());
        for (Faction f : Faction.values()) assertTrue(Power.wheel(f, f.maxRank()).size() <= 8, "the wheel holds 8 at most");
    }

    @Test
    void castingChecksSideRankEssenceCooldownAndSuppression() {
        Allegiance angel = Allegiance.HUMAN.convert(Faction.ANGEL);
        assertEquals(PowerRules.Verdict.OK, PowerRules.canCast(angel, Power.TELEPORT, 0, false));
        assertEquals(PowerRules.Verdict.NOT_YOURS, PowerRules.canCast(angel, Power.SMOKE, 0, false));
        assertEquals(PowerRules.Verdict.RANK_TOO_LOW, PowerRules.canCast(angel, Power.SMITE, 0, false));
        assertEquals(PowerRules.Verdict.PASSIVE, PowerRules.canCast(angel.withRank(2), Power.WINGS, 0, false));
        assertEquals(PowerRules.Verdict.COOLING_DOWN, PowerRules.canCast(angel, Power.TELEPORT, 5, false));
        assertEquals(PowerRules.Verdict.NO_ESSENCE, PowerRules.canCast(angel.withEssence(0), Power.TELEPORT, 0, false));
        assertEquals(PowerRules.Verdict.SUPPRESSED, PowerRules.canCast(angel, Power.TELEPORT, 0, true));
        assertFalse(PowerRules.passiveActive(angel.withRank(2), Power.WINGS, true));
        assertTrue(PowerRules.passiveActive(angel.withRank(2), Power.WINGS, false));
    }

    @Test
    void requirementsAskSideRankAndCooldown() {
        Allegiance angel = Allegiance.HUMAN.convert(Faction.ANGEL);
        assertNull(AllegianceRequirement.of(Faction.ANGEL, 1).check(angel, 0));
        assertNotNull(AllegianceRequirement.of(Faction.ANGEL, 2).check(angel, 0));
        assertNotNull(AllegianceRequirement.of(Faction.DEMON, 1).check(angel, 0));
        AllegianceRequirement free = new AllegianceRequirement(Faction.HUMAN, java.util.Optional.empty(), java.util.Optional.empty(),
                java.util.Optional.of(false));
        assertNull(free.check(Allegiance.HUMAN, 0));
        assertNotNull(free.check(angel.cured(500), 100), "a cured player must wait");
        assertNull(free.check(angel.cured(500), 500));
    }

    @Test
    void eachRankRiteIsAffordableAtTheRankBeforeIt() throws java.io.IOException {
        assertEquals(0, Ranks.manaBonus(Faction.HUMAN, 0));
        assertEquals(100, Ranks.manaBonus(Faction.ANGEL, 4));
        assertEquals(75, Ranks.manaBonus(Faction.HUMAN, 9), "a hunter tops out at Legend");
        String[][] rites = {{"seraph_ascension", "angel", "1"}, {"archangel_ascension", "angel", "2"}, {"usurp_the_host", "angel", "3"},
                {"prince_of_hell", "demon", "1"}, {"mark_of_cain", "demon", "2"}, {"usurp_the_throne", "demon", "3"},
                {"veterans_vigil", "human", "1"}, {"legend_of_the_road", "human", "2"}};
        java.util.regex.Pattern cost = java.util.regex.Pattern.compile("\"mana_cost\":\\s*(\\d+)");
        for (String[] r : rites) {
            String json = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/data/supernaturalcraft/recipe/ritual/" + r[0] + ".json"));
            java.util.regex.Matcher m = cost.matcher(json);
            assertTrue(m.find(), r[0]);
            Faction f = Faction.valueOf(r[1].toUpperCase(java.util.Locale.ROOT));
            float max = 100 + Ranks.manaBonus(f, Integer.parseInt(r[2]));
            float price = Integer.parseInt(m.group(1)) * (f == Faction.HUMAN ? 0.75f : 1f);
            assertTrue(price <= max, r[0] + " costs " + price + " but a rank " + r[2] + " holds " + max);
        }
    }
}
