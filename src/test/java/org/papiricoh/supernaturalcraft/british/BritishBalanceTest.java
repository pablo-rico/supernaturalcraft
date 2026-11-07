package org.papiricoh.supernaturalcraft.british;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.british.arsenal.ArsenalRules;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonAssets;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonBalance;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessAssets;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessBalance;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchAssets;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchBalance;

import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Season 12's three fights and the British arsenal (v0.41): phases without gaps, readable windups, clips with their moments. */
class BritishBalanceTest {

    private static void phases(String who, int count, IntFunction<Float> threshold, IntUnaryOperator gap) {
        float last = 1f;
        for (int p = 1; p <= count; p++) {
            float t = threshold.apply(p);
            assertTrue(t < last, who + ": phase " + p + " ends below where it began");
            last = t;
        }
        assertEquals(0f, threshold.apply(count), who + "'s last phase runs to the end");
        assertTrue(gap.applyAsInt(1) > gap.applyAsInt(count), who + " quickens");
    }

    private static void windups(String who, int... w) {
        for (int x : w) assertTrue(x >= 12, who + ": a windup of " + x + " ticks is too short to step out of");
    }

    private static void clips(String who, Map<String, Integer> hits, Map<String, Integer> lengths, List<String> clips, List<String> used) {
        for (var e : hits.entrySet()) {
            Integer len = lengths.get(e.getKey());
            assertTrue(len != null && e.getValue() < len, who + ": " + e.getKey() + "'s moment falls inside its clip");
            assertTrue(clips.contains(e.getKey()), who + ": " + e.getKey() + " is one of the clips");
        }
        for (String c : used) assertTrue(clips.contains(c), who + " plays " + c + ", which the art must have");
    }

    @Test
    void ketch() {
        assertEquals(3, KetchBalance.PHASES);
        phases("Ketch", KetchBalance.PHASES, KetchBalance::threshold, KetchBalance::attackGap);
        windups("Ketch", KetchBalance.SHOT_WINDUP, KetchBalance.VOLLEY_WINDUP, KetchBalance.SLASH_WINDUP, KetchBalance.LUNGE_WINDUP,
                KetchBalance.GRENADE_WINDUP, KetchBalance.SMOKE_WINDUP);
        clips("Ketch", KetchAssets.HIT_TICKS, KetchAssets.CLIP_TICKS, KetchAssets.CLIPS,
                List.of("shoot", "volley", "knife_slash", "knife_lunge", "grenade_throw", "smoke_vanish", "taunt", "death", "idle", "walk", "run", "aim"));
        assertTrue(KetchBalance.DEATH >= KetchAssets.CLIP_TICKS.get("death"), "his death lasts its clip");
    }

    @Test
    void hess() {
        assertEquals(3, HessBalance.PHASES);
        phases("Hess", HessBalance.PHASES, HessBalance::threshold, HessBalance::attackGap);
        windups("Hess", HessBalance.BOLT_WINDUP, HessBalance.HEX_WINDUP, HessBalance.SUMMON_WINDUP, HessBalance.WARD_WINDUP, HessBalance.CURSE_WINDUP);
        clips("Hess", HessAssets.HIT_TICKS, HessAssets.CLIP_TICKS, HessAssets.CLIPS,
                List.of("wand_cast", "hex_throw", "summon_agents", "ward_raise", "folio_read", "appear", "leave", "talk", "death", "idle", "walk"));
        assertTrue(HessBalance.taken(true) < 1f && HessBalance.taken(false) == 1f, "her ward turns part of a blow aside, and only it");
        assertTrue(HessBalance.SAFE_RADIUS * 2 < HessBalance.CURSE_RADIUS, "the folio spares only a small circle");
        assertTrue(HessBalance.SAFE_MIN_DISTANCE > HessBalance.WARD_RADIUS, "and never at her side");
        assertTrue(HessBalance.DEATH >= HessAssets.CLIP_TICKS.get("death"));
    }

    @Test
    void dagon() {
        assertEquals(3, DagonBalance.PHASES);
        phases("Dagon", DagonBalance.PHASES, DagonBalance::threshold, DagonBalance::attackGap);
        windups("Dagon", DagonBalance.FLING_WINDUP, DagonBalance.CRUSH_WINDUP, DagonBalance.PALM_WINDUP, DagonBalance.RING_WINDUP,
                DagonBalance.PIN_WINDUP, DagonBalance.SCREAM_WINDUP);
        clips("Dagon", DagonAssets.HIT_TICKS, DagonAssets.CLIP_TICKS, DagonAssets.CLIPS,
                List.of("fling", "crush", "fire_palm", "fire_ring", "pin", "scream", "guard", "hover_idle", "death", "idle", "walk"));
        assertTrue(DagonBalance.RING_INNER < DagonBalance.RING_OUTER, "her ring of fire has a safe heart");
        assertTrue(DagonBalance.CRUSH_WINDUP > DagonBalance.FLING_WINDUP, "the fist closes slowly: the hardest blow is the slowest");
        assertEquals(DagonAssets.CLIP_TICKS.get("death"), DagonBalance.deathTicks());
    }

    @Test
    void theArsenalIsSane() {
        assertTrue(ArsenalRules.hexTicks(60, true) == 0, "a boss never takes a hex");
        assertEquals(60, ArsenalRules.hexTicks(60, false));
        assertEquals(1f, ArsenalRules.falloff(0, 3), 1e-6);
        assertEquals(0.5f, ArsenalRules.falloff(3, 3), 1e-6);
        assertEquals(0f, ArsenalRules.falloff(3.01, 3), 1e-6);
        // The pistol is no better than a blade: per second it deals about what the Angel Blade does in one blow.
        float perSecond = ArsenalRules.PISTOL_DAMAGE * 20f / ArsenalRules.PISTOL_COOLDOWN;
        assertTrue(perSecond <= org.papiricoh.supernaturalcraft.balance.WeaponDamage.angelBladeBaseModifier(), "the pistol stays modest: " + perSecond);
        assertTrue(ArsenalRules.BRAND_COOLDOWN >= 200 && ArsenalRules.GRENADE_COOLDOWN >= 20, "the charms and grenades rest");
    }
}
