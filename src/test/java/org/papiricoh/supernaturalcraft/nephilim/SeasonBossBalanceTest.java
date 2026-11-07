package org.papiricoh.supernaturalcraft.nephilim;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.empty.EmptyShape;
import org.papiricoh.supernaturalcraft.entity.boss.asmodeus.AsmodeusAssets;
import org.papiricoh.supernaturalcraft.entity.boss.asmodeus.AsmodeusBalance;
import org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselAssets;
import org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselBalance;
import org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowAssets;
import org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowBalance;
import org.papiricoh.supernaturalcraft.entity.boss.soulless.SoullessJackBalance;
import org.papiricoh.supernaturalcraft.nephilim.jack.JackAssets;

import java.util.Map;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The core's four bosses (v0.41): their numbers against their clips. */
class SeasonBossBalanceTest {

    private static void quickens(String who, IntUnaryOperator gap, int phases) {
        for (int p = 1; p < phases; p++) assertTrue(gap.applyAsInt(p) > gap.applyAsInt(p + 1), who + " quickens into phase " + (p + 1));
    }

    /** Every windup is long enough to read the ground and to fit its clip's moment. */
    private static void windups(String who, Map<String, Integer> hits, Map<String, Integer> lengths, Object[][] pairs) {
        for (Object[] pair : pairs) {
            String clip = (String) pair[0];
            int windup = (Integer) pair[1];
            assertTrue(windup >= 12, who + ": " + clip + "'s windup of " + windup + " ticks is too short to step out of");
            Integer cue = hits.get(clip);
            assertTrue(cue != null, who + ": " + clip + " is a clip with a moment");
            assertTrue(cue <= windup, who + ": " + clip + "'s moment (" + cue + ") falls inside its windup (" + windup + ")");
        }
        for (var e : hits.entrySet()) {
            Integer len = lengths.get(e.getKey());
            assertTrue(len != null && e.getValue() < len, who + ": " + e.getKey() + "'s moment falls inside its clip");
        }
    }

    @Test
    void asmodeus() {
        assertEquals(3, AsmodeusBalance.PHASES);
        quickens("Asmodeus", AsmodeusBalance::attackGap, AsmodeusBalance.PHASES);
        windups("Asmodeus", AsmodeusAssets.HIT_TICKS, AsmodeusAssets.CLIP_TICKS, new Object[][]{
                {"cane_strike", AsmodeusBalance.CANE_WINDUP}, {"cane_slam", AsmodeusBalance.SLAM_WINDUP}, {"cane_slam", AsmodeusBalance.NOVA_WINDUP},
                {"summon", AsmodeusBalance.SUMMON_WINDUP}, {"shapeshift", AsmodeusBalance.BLINK_WINDUP},
                {"grace_blast", AsmodeusBalance.GRACE_WINDUP}, {"grace_blast", AsmodeusBalance.SMITE_WINDUP}});
        assertTrue(AsmodeusBalance.NOVA_RADIUS > AsmodeusBalance.SLAM_RADIUS);
        assertTrue(AsmodeusBalance.MAX_HOUNDS_LAST > AsmodeusBalance.MAX_HOUNDS);
    }

    @Test
    void michaelsVessel() {
        assertEquals(3, MichaelVesselBalance.PHASES);
        quickens("Michael", MichaelVesselBalance::attackGap, MichaelVesselBalance.PHASES);
        windups("Michael", MichaelVesselAssets.HIT_TICKS, MichaelVesselAssets.CLIP_TICKS, new Object[][]{
                {"slash", MichaelVesselBalance.SLASH_WINDUP}, {"smite", MichaelVesselBalance.SMITE_WINDUP},
                {"wing_flare", MichaelVesselBalance.FLARE_WINDUP}, {"choke", MichaelVesselBalance.CHOKE_WINDUP},
                {"snap", MichaelVesselBalance.SNAP_WINDUP}, {"teleport", MichaelVesselBalance.BLINK_WINDUP}});
        assertTrue(MichaelVesselBalance.MINDSCAPE_DAMAGE < 1f, "his projection hits a little softer in the small room");
    }

    @Test
    void soullessJack() {
        assertEquals(3, SoullessJackBalance.PHASES);
        quickens("Jack", SoullessJackBalance::attackGap, SoullessJackBalance.PHASES);
        windups("Jack", JackAssets.HIT_TICKS, JackAssets.CLIP_TICKS, new Object[][]{
                {"smite", SoullessJackBalance.BLAST_WINDUP}, {"smite", SoullessJackBalance.BLAST_WINDUP_LAST},
                {"wave_aside", SoullessJackBalance.WAVE_WINDUP}, {"snap", SoullessJackBalance.SNAP_WINDUP}, {"nova", SoullessJackBalance.NOVA_WINDUP}});
        assertTrue(SoullessJackBalance.NOVA_RADIUS_LAST > SoullessJackBalance.NOVA_RADIUS);
        assertTrue(JackAssets.LOOPS.contains("float_idle"), "his last phase is in the air");
    }

    @Test
    void theShadow() {
        assertEquals(3, ShadowBalance.PHASES);
        quickens("The Shadow", ShadowBalance::attackGap, ShadowBalance.PHASES);
        windups("The Shadow", ShadowAssets.HIT_TICKS, ShadowAssets.CLIP_TICKS, new Object[][]{
                {"lash", ShadowBalance.LASH_WINDUP}, {"grab", ShadowBalance.GRAB_WINDUP}, {"lull", ShadowBalance.LULL_WINDUP},
                {"engulf", ShadowBalance.SURGE_WINDUP}, {"engulf", ShadowBalance.ENGULF_WINDUP}, {"sink", ShadowBalance.SINK_WINDUP}});
        // One tendril more each phase: the rig has enough of them.
        assertTrue(ShadowAssets.OPTIONAL_BONES.stream().filter(b -> b.startsWith("tendril_")).count() >= ShadowBalance.PHASES);
    }

    @Test
    void theEmptysFloorHoldsItsSleepersAndItsLanding() {
        assertTrue(EmptyShape.floor(0, 0));
        assertTrue(EmptyShape.floor(0, EmptyShape.LANDING), "the hunter lands on ground");
        assertTrue(!EmptyShape.floor(EmptyShape.RADIUS + 1, 0), "nothing past the rim");
        assertTrue(EmptyShape.depth(0, 0) > EmptyShape.depth(EmptyShape.RADIUS - 2, 0), "a bowl, deeper at the middle");
        java.util.Set<Long> cells = new java.util.HashSet<>();
        for (int ring = 0; ring < EmptyShape.RINGS.length; ring++)
            for (int i = 0; i < EmptyShape.PER_RING; i++) {
                int[] s = EmptyShape.sleeper(ring, i);
                // Both halves of every sleeper lie on the floor, and no two sleepers share a cell.
                int[][] dirs = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};
                int hx = s[0] + dirs[s[2]][0], hz = s[1] + dirs[s[2]][1];
                assertTrue(EmptyShape.floor(s[0], s[1]) && EmptyShape.floor(hx, hz), "sleeper " + ring + "/" + i + " lies on the floor");
                assertTrue(cells.add(((long) s[0] << 32) ^ (s[1] & 0xffffffffL)), "sleeper " + ring + "/" + i + " has its own cell");
                assertTrue(Math.abs(s[1] - EmptyShape.LANDING) + Math.abs(s[0]) > 1 || ring != 1, "the landing is clear");
            }
    }
}
