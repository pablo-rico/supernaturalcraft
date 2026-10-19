package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.coven.layout.RowenaHouseLayout;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaAssets;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaBalance;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaCinematics;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rowena's numbers (v0.21 P6, pure): three gapless thirds, quickening; windups long enough for their clips; the familiar's curse
 * cadence; her victims' heal and cost; the recitation (three bowls of three blows inside her house's arena, a warning before the
 * hex, twelve per bowl left whole, the stun ×1.5); the Grimoire's minute.
 */
class RowenaBalanceTest {

    @Test
    void threePhasesOfAThirdEachWithNoGap() {
        assertEquals(3, RowenaBalance.PHASES);
        assertEquals(1f, RowenaBalance.threshold(0), 1e-6);
        assertEquals(2f / 3f, RowenaBalance.threshold(1), 1e-6);
        assertEquals(1f / 3f, RowenaBalance.threshold(2), 1e-6);
        assertEquals(0f, RowenaBalance.threshold(3), 1e-6);
        assertEquals(0f, RowenaBalance.threshold(4), 1e-6, "past the last phase there is no floor");
        for (int p = 1; p <= RowenaBalance.PHASES; p++) {
            assertEquals(1f / 3f, RowenaBalance.threshold(p - 1) - RowenaBalance.threshold(p), 1e-6, "phase " + p + " is a third");
        }
        assertTrue(RowenaBalance.attackGap(3) < RowenaBalance.attackGap(2) && RowenaBalance.attackGap(2) < RowenaBalance.attackGap(1),
                "she quickens");
        assertTrue(RowenaBalance.attackGap(3) >= 20, "but leaves room to breathe");
    }

    @Test
    void windupsEndOnTheClipsMoments() {
        Map<String, int[]> windups = Map.of("fling", new int[]{RowenaBalance.FLING_WINDUP}, "ignis", new int[]{RowenaBalance.IGNIS_WINDUP},
                "glacies", new int[]{RowenaBalance.GLACIES_WINDUP}, "summon_victims", new int[]{RowenaBalance.VICTIMS_WINDUP, RowenaBalance.FAMILIAR_WINDUP},
                "teleport", new int[]{RowenaBalance.SMOKE_WINDUP});
        for (var e : windups.entrySet()) {
            int cue = RowenaBalance.cue(e.getKey());
            assertEquals(RowenaAssets.HIT_TICKS.get(e.getKey()).intValue(), cue, e.getKey());
            for (int w : e.getValue()) assertTrue(w >= cue, e.getKey() + ": the clip must start inside the windup (" + w + " < " + cue + ")");
            assertTrue(e.getValue()[0] >= 15, e.getKey() + ": a windup long enough to read");
        }
        assertEquals(0, RowenaBalance.cue("recite"), "a loop has no moment");
        assertTrue(RowenaBalance.DEATH_TICKS >= RowenaAssets.HIT_TICKS.get("death"), "her death clip lands before she is gone");
    }

    @Test
    void theFamiliarsCurseAndTheHexed() {
        assertEquals(120, RowenaBalance.HOMING_EVERY, "a curse down the stolen lock every six seconds");
        assertTrue(RowenaBalance.FAMILIAR_AGAIN > RowenaBalance.HOMING_EVERY, "killing the raven buys more than one curse's time");
        assertTrue(RowenaBalance.HOMING_DAMAGE > 0 && RowenaBalance.HOMING_CURSE_TICKS > 0);
        assertEquals(0.04f, RowenaBalance.VICTIM_HEAL, 1e-6, "a victim killed heals her 4 %");
        assertEquals(0.03f, RowenaBalance.VICTIM_FREED, 1e-6, "one freed costs her 3 %");
        assertTrue(RowenaBalance.VICTIM_HEAL * RowenaBalance.MAX_VICTIMS < 1f / RowenaBalance.PHASES, "killing them all never undoes a phase");
        assertTrue(RowenaBalance.VICTIMS_PER_CALL <= RowenaBalance.MAX_VICTIMS);
    }

    @Test
    void theRecitation() {
        assertEquals(300, RowenaBalance.RECITE_TICKS);
        assertEquals(3, RowenaBalance.BOWLS);
        assertEquals(3, RowenaBalance.BOWL_HITS);
        assertTrue(RowenaBalance.WIPE_WARNING < RowenaBalance.RECITE_TICKS / 2, "the red warning is the end of it");
        assertTrue(RowenaBalance.WIPE_WARNING >= 40, "but long enough to brace");
        assertTrue(RowenaBalance.BOWL_DISTANCE <= RowenaHouseLayout.ARENA_RADIUS - 2.5, "every bowl stands inside her house's arena");
        assertTrue(RowenaBalance.RECITE_AGAIN > RowenaBalance.RECITE_TICKS, "not back to back");
        for (int whole = 0; whole <= RowenaBalance.BOWLS; whole++) {
            assertEquals(12f * whole, RowenaBalance.wipeDamage(whole), 1e-6, whole + " bowls whole");
        }
        assertEquals(0f, RowenaBalance.wipeDamage(-1), 1e-6);
        assertEquals(1.5f, RowenaBalance.share(true), 1e-6, "stunned, ×1.5");
        assertEquals(1f, RowenaBalance.share(false), 1e-6);
        assertEquals(100, RowenaBalance.STUN_TICKS);
        assertEquals(0, RowenaBalance.progress(0, RowenaBalance.RECITE_TICKS));
        assertEquals(500, RowenaBalance.progress(150, RowenaBalance.RECITE_TICKS));
        assertEquals(1000, RowenaBalance.progress(400, RowenaBalance.RECITE_TICKS), "clamped");
        assertEquals(0, RowenaBalance.progress(-5, RowenaBalance.RECITE_TICKS), "clamped");
        assertEquals(1000, RowenaBalance.progress(3, 0), "a recitation of no length is over");
        assertEquals(0, RowenaBalance.RECITE_TICKS % RowenaBalance.PROGRESS_EVERY);
    }

    @Test
    void herArenaHerCamerasAndHerGrimoire() {
        assertEquals(12, RowenaHouseLayout.ARENA_RADIUS, "her house is a ring of twelve");
        assertEquals(1200, RowenaBalance.GRIMOIRE_COOLDOWN, "the Grimoire's echo, once a minute");
        assertTrue(RowenaCinematics.INTRO_TICKS > 0 && RowenaCinematics.DEATH_TICKS > 0);
    }
}
