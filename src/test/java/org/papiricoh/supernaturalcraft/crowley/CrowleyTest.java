package org.papiricoh.supernaturalcraft.crowley;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors;
import org.papiricoh.supernaturalcraft.asylum.layout.AsylumPlan;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyAssets;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyBalance;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyCompanions;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySpoils;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyVoice;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Crowley's pure rules (v0.35): phases, damage share, the asylum's contract with the fight, and his text. */
class CrowleyTest {

    private static JsonObject lang(String path) throws Exception {
        try (Reader r = Files.newBufferedReader(Path.of(path))) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    @Test
    void phasesSplitHisHealthInThirdsWithoutGaps() {
        assertEquals(1f, CrowleyBalance.threshold(0), 1e-6);
        for (int p = 1; p < CrowleyBalance.PHASES; p++) {
            assertTrue(CrowleyBalance.threshold(p) < CrowleyBalance.threshold(p - 1));
            assertEquals(1f - p / (float) CrowleyBalance.PHASES, CrowleyBalance.threshold(p), 1e-6);
        }
        assertEquals(0f, CrowleyBalance.threshold(CrowleyBalance.PHASES), 1e-6);
    }

    @Test
    void hisPeopleCoverHimOnlyInNumbers() {
        assertEquals(1f, CrowleyBalance.share(CrowleyBalance.GUARDED_BY - 1));
        assertEquals(CrowleyBalance.RESIST, CrowleyBalance.share(CrowleyBalance.GUARDED_BY));
        assertTrue(CrowleyBalance.RESIST > 0 && CrowleyBalance.RESIST < 1);
    }

    @Test
    void theContractBookChargesBySize() {
        assertEquals(CrowleyBalance.BOOK_MIN_LEVELS, CrowleyBalance.bookCost(1));
        assertTrue(CrowleyBalance.bookCost(CrowleyBalance.BOOK_MAX_HEALTH) > CrowleyBalance.bookCost(40));
    }

    @Test
    void everyTriggeredClipHasItsTimings() {
        for (String clip : CrowleyAssets.TRIGGERED) {
            assertTrue(CrowleyAssets.CLIP_TICKS.containsKey(clip), clip);
            assertTrue(CrowleyAssets.HIT_TICKS.get(clip) < CrowleyAssets.CLIP_TICKS.get(clip), clip);
        }
    }

    @Test
    void theAsylumCarriesEveryAnchorInsideItsFootprint() {
        for (int q = 0; q < 4; q++) {
            LayoutPlan plan = AsylumPlan.plan(q);
            for (String name : AsylumAnchors.required()) {
                LayoutPoint p = plan.points().get(name);
                assertNotNull(p, "plan " + q + " lacks " + name);
                int r = Math.max(Math.max(-AsylumAnchors.MIN_X, AsylumAnchors.MAX_X), Math.max(-AsylumAnchors.MIN_Z, AsylumAnchors.MAX_Z));
                assertTrue(Math.abs(p.x()) <= r && Math.abs(p.z()) <= r, name + " outside the footprint");
                assertTrue(p.y() >= AsylumAnchors.BOTTOM_Y && p.y() <= AsylumAnchors.TOP_Y, name + " outside the height");
            }
            assertTrue(plan.points().get(AsylumAnchors.THRONE).y() < 0, "the throne room is in the basement");
        }
    }

    @Test
    void everyLineHeSaysIsWrittenInBothLanguages() throws Exception {
        JsonObject en = lang("src/generated/resources/assets/supernaturalcraft/lang/en_us.json");
        JsonObject es = lang("src/main/resources/assets/supernaturalcraft/lang/es_es.json");
        java.util.List<String> keys = new java.util.ArrayList<>();
        CrowleyVoice.LINES.forEach((s, n) -> {
            for (int i = 0; i < n; i++) keys.add(CrowleyVoice.key(s, i));
        });
        for (String node : CrowleySpoils.NODES) {
            String k = "dialogue.supernaturalcraft." + CrowleySpoils.DIALOGUE + "." + node;
            keys.add(k);
            for (String o : CrowleySpoils.OPTIONS) keys.add(k + "." + o);
        }
        String c = "dialogue.supernaturalcraft." + CrowleyCompanions.DIALOGUE + ".";
        keys.add(c + CrowleyCompanions.MENU);
        keys.add(c + CrowleyCompanions.DEALS);
        for (String o : CrowleyCompanions.MENU_OPTIONS) keys.add(c + CrowleyCompanions.MENU + "." + o);
        for (String o : CrowleyCompanions.DEAL_OPTIONS) keys.add(c + CrowleyCompanions.DEALS + "." + o);
        for (int p = 1; p <= CrowleyBalance.PHASES; p++) keys.add(CrowleyAssets.BAR_PREFIX + ".phase" + p);
        for (String k : keys) {
            assertTrue(en.has(k), "no English for " + k);
            assertTrue(es.has(k), "no Spanish for " + k);
        }
    }
}
