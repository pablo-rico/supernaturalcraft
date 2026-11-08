package org.papiricoh.supernaturalcraft.hands;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.mob.ChittersRules;
import org.papiricoh.supernaturalcraft.hands.mob.HandsMobRules;
import org.papiricoh.supernaturalcraft.hands.reliquary.ReliquarySites;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Hands slice's pure rules (v0.42): the Chitters' cycle, the wild spawns' gates, the claim record, the reliquaries' sites. */
class HandsRulesTest {

    @Test
    void chittersWakeForAFewDaysInEveryCycle() {
        for (long d = 0; d < ChittersRules.FIRST; d++) assertFalse(ChittersRules.awake(d), "asleep in the world's first weeks, day " + d);
        int awake = 0;
        for (long d = ChittersRules.FIRST; d < ChittersRules.FIRST + ChittersRules.CYCLE * 10L; d++) if (ChittersRules.awake(d)) awake++;
        assertEquals(ChittersRules.AWAKE_DAYS * 10, awake, "awake days per ten cycles");
        assertTrue(ChittersRules.awake(ChittersRules.FIRST));
        assertTrue(ChittersRules.awake(ChittersRules.FIRST + ChittersRules.AWAKE_DAYS - 1));
        assertFalse(ChittersRules.awake(ChittersRules.FIRST + ChittersRules.AWAKE_DAYS));
        assertTrue(ChittersRules.awake(ChittersRules.FIRST + ChittersRules.CYCLE));
        for (long d = 0; d < 200; d++) {
            long n = ChittersRules.nextWaking(d);
            assertTrue(n >= d && ChittersRules.awake(n), "next waking from " + d + " is " + n);
            for (long k = d; k < n; k++) assertFalse(ChittersRules.awake(k), "no waking skipped before " + n);
        }
    }

    @Test
    void chittersRiseOnlyDeepInThePitchDark() {
        long day = ChittersRules.FIRST;
        assertTrue(HandsSpawnRules.chitters(true, day, ChittersRules.MAX_Y - 1, 0, 0));
        assertFalse(HandsSpawnRules.chitters(false, day, 10, 0, 0), "not before season 11 begins");
        assertFalse(HandsSpawnRules.chitters(true, day + ChittersRules.AWAKE_DAYS, 10, 0, 0), "not while asleep");
        assertFalse(HandsSpawnRules.chitters(true, day, ChittersRules.MAX_Y, 0, 0), "not up near the surface");
        assertFalse(HandsSpawnRules.chitters(true, day, 10, 1, 0), "not under the sky");
        assertFalse(HandsSpawnRules.chitters(true, day, 10, 0, 3), "not by a torch");
        for (double r = 0; r < 1; r += 0.01) {
            int n = ChittersRules.packSize(r);
            assertTrue(n >= ChittersRules.PACK_MIN && n <= ChittersRules.PACK_MAX);
        }
        assertEquals(ChittersRules.PACK_MAX, ChittersRules.packSize(0.999));
    }

    @Test
    void theBansheeComesOnlyWithTheDarknessByNightWhereSheHaunts() {
        assertTrue(HandsSpawnRules.banshee(true, true, true));
        assertFalse(HandsSpawnRules.banshee(false, true, true), "not while the sun is whole");
        assertFalse(HandsSpawnRules.banshee(true, false, true), "not by day");
        assertFalse(HandsSpawnRules.banshee(true, true, false), "not out of her swamps and dark woods");
    }

    @Test
    void theClaimRecordKeepsEachHandOnce() {
        HandClaims c = HandClaims.EMPTY;
        assertEquals(5, c.unclaimed().size());
        c = c.with(S11Assets.Hand.ROD_OF_AARON).with(S11Assets.Hand.ROD_OF_AARON);
        assertEquals(1, c.count());
        assertTrue(c.has(S11Assets.Hand.ROD_OF_AARON));
        assertFalse(c.unclaimed().contains(S11Assets.Hand.ROD_OF_AARON));
        for (S11Assets.Hand h : S11Assets.Hand.values()) c = c.with(h);
        assertEquals(HandClaims.ALL, c.mask());
        assertTrue(c.unclaimed().isEmpty());
    }

    @Test
    void eachReliquaryKnowsItsGround() {
        int sea = 63;
        // The wreck: deep, even water only.
        assertNotNull(ReliquarySites.site(S11Assets.Hand.DELPHINES_HAND, new int[]{40, 41, 39, 40, 42}, sea, 1));
        assertNull(ReliquarySites.site(S11Assets.Hand.DELPHINES_HAND, new int[]{58, 58, 58, 58, 58}, sea, 1), "too shallow");
        assertNull(ReliquarySites.site(S11Assets.Hand.DELPHINES_HAND, new int[]{40, 30, 40, 50, 40}, sea, 1), "too broken");
        // The bunker: high up, its back into the rising side.
        ReliquarySites.Site b = ReliquarySites.site(S11Assets.Hand.NAIL_OF_THE_CROSS, new int[]{110, 100, 104, 120, 101}, sea, 1);
        assertNotNull(b);
        assertEquals(0, b.q(), "into the south side, unturned");
        b = ReliquarySites.site(S11Assets.Hand.NAIL_OF_THE_CROSS, new int[]{110, 125, 104, 100, 101}, sea, 1);
        assertNotNull(b);
        assertEquals(2, b.q(), "into the north side, two turns");
        assertEquals(1, ReliquarySites.site(S11Assets.Hand.NAIL_OF_THE_CROSS, new int[]{110, 100, 104, 100, 130}, sea, 1).q(), "west");
        assertEquals(3, ReliquarySites.site(S11Assets.Hand.NAIL_OF_THE_CROSS, new int[]{110, 100, 130, 100, 101}, sea, 1).q(), "east");
        assertNull(ReliquarySites.site(S11Assets.Hand.NAIL_OF_THE_CROSS, new int[]{70, 60, 80, 60, 60}, sea, 1), "not low down");
        assertNull(ReliquarySites.site(S11Assets.Hand.NAIL_OF_THE_CROSS, new int[]{110, 110, 111, 109, 110}, sea, 1), "not on a flat top");
        // The church and the temple on dry level ground; the monastery terraced into a high slope.
        assertNotNull(ReliquarySites.site(S11Assets.Hand.APOSTLES_CHALICE, new int[]{70, 71, 69, 70, 72}, sea, 1));
        assertNull(ReliquarySites.site(S11Assets.Hand.APOSTLES_CHALICE, new int[]{70, 80, 69, 70, 72}, sea, 1));
        assertNull(ReliquarySites.site(S11Assets.Hand.ROD_OF_AARON, new int[]{62, 63, 62, 62, 63}, sea, 1), "not at the water's edge");
        ReliquarySites.Site m = ReliquarySites.site(S11Assets.Hand.TRUMPET_OF_JERICHO, new int[]{100, 96, 104, 99, 101}, sea, 1);
        assertNotNull(m);
        assertEquals(105, m.y(), "the terrace on the highest ground, footings below");
    }

    @Test
    void seasonElevenTakesSomeCasesWithoutShiftingTheRolls() {
        var gen = org.papiricoh.supernaturalcraft.legacy.cases.CaseGenerator.class;
        int banshees = 0, chitters = 0, woods = 0, mines = 0;
        for (int i = 0; i < 400; i++) {
            var plan = org.papiricoh.supernaturalcraft.legacy.cases.CaseGenerator.roll(1234L, 5L, 7L, i, 3, 200, 600);
            var closed = org.papiricoh.supernaturalcraft.legacy.cases.CaseGenerator.withSeasonEleven(plan, false);
            assertEquals(plan, closed, "nothing changes before season 11");
            var open = org.papiricoh.supernaturalcraft.legacy.cases.CaseGenerator.withSeasonEleven(plan, true);
            assertEquals(plan.dx(), open.dx());
            assertEquals(plan.dz(), open.dz());
            assertEquals(plan.scenario(), open.scenario());
            if (plan.scenario().equals("night_woods")) woods++;
            if (plan.scenario().equals("mine")) mines++;
            if (open.monster().equals(org.papiricoh.supernaturalcraft.legacy.cases.CaseGenerator.BANSHEE)) {
                banshees++;
                assertEquals("night_woods", plan.scenario());
            }
            if (open.monster().equals(org.papiricoh.supernaturalcraft.legacy.cases.CaseGenerator.CHITTER)) {
                chitters++;
                assertEquals("mine", plan.scenario());
                assertTrue(open.count() >= 2, "Chitters come as a pack");
            }
            if (!plan.scenario().equals("night_woods") && !plan.scenario().equals("mine")) assertEquals(plan, open);
        }
        assertTrue(banshees > 0 && banshees < woods, "some night-woods cases, not all: " + banshees + "/" + woods);
        assertTrue(chitters > 0 && chitters < mines, "some mine cases, not all: " + chitters + "/" + mines);
        assertNotNull(gen);
    }

    @Test
    void theCreaturesAreNoBosses() {
        assertTrue(HandsMobRules.OFFICER_HEALTH < 100 && HandsMobRules.BANSHEE_HEALTH < 100);
        assertTrue(HandsMobRules.hitChance(HandsMobRules.SHOT_MAX) < HandsMobRules.hitChance(HandsMobRules.SHOT_MIN), "the farther, the more it misses");
        assertTrue(HandsMobRules.GOLD_MULTIPLIER > 1 && HandsMobRules.OTHER_MULTIPLIER < 1);
    }
}
