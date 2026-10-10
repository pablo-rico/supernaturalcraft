package org.papiricoh.supernaturalcraft.memory.scenes;

import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.memory.Memory;
import org.papiricoh.supernaturalcraft.memory.MemoryKind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A representative sample of memory scenes for the layout dump and {@code tools/structview} (pure): every kind, several bosses.
 * Figures become points named {@code figure_<n>_<pose>} (the focus {@code focus_<pose>}).
 */
public final class MemorySceneCatalog implements LayoutCatalog {

    /** The sample: name → memory. */
    public static Map<String, Memory> samples() {
        Map<String, Memory> out = new LinkedHashMap<>();
        for (String boss : List.of("azazel", "lilith", "lucifer", "gabriel", "war", "pestilence", "death", "raphael", "metatron", "broken_chorus",
                "amara", "michael", "zachariah", "chuck")) {
            out.put("memory_boss_" + boss, new Memory("boss:" + boss, MemoryKind.BOSS_VICTORY, boss, "", 0, 0, 0));
        }
        out.put("memory_deal_wild", new Memory("deal:1", MemoryKind.CROSSROADS_DEAL, "wealth", "wild", 0, 0, 0));
        out.put("memory_deal_bowl", new Memory("deal:2", MemoryKind.CROSSROADS_DEAL, "health", "bowl", 0, 0, 0));
        out.put("memory_case_solved", new Memory("case:1", MemoryKind.CASE_SOLVED, "supernaturalcraft:vampire", "graveyard", 0, 0, 0));
        out.put("memory_case_lost", new Memory("case:2", MemoryKind.CASE_LOST, "supernaturalcraft:werewolf", "barn", 0, 0, 0));
        out.put("memory_rank_angel", new Memory("rank:angel_2", MemoryKind.ASCENSION, "angel", "", 0, 0, 2));
        out.put("memory_rank_demon", new Memory("rank:demon_3", MemoryKind.ASCENSION, "demon", "", 0, 0, 3));
        out.put("memory_rank_hunter", new Memory("rank:hunter_1", MemoryKind.ASCENSION, "hunter", "", 0, 0, 1));
        out.put("memory_call", new Memory("call", MemoryKind.HEEDED_CALL, "messenger", "", 0, 0, 0));
        out.put("memory_legacy", new Memory("legacy:3", MemoryKind.LEGACY_RANK, "", "", 0, 0, 3));
        out.put("memory_pet", new Memory("pet:1", MemoryKind.PET_LOST, "minecraft:wolf", "Rumsfeld", 0, 0, 0));
        out.put("memory_sighting", new Memory("seen:ghost", MemoryKind.FIRST_SIGHTING, "supernaturalcraft:ghost", "", 0, 0, 0));
        out.put("memory_prey", new Memory("prey", MemoryKind.FAVOURITE_PREY, "supernaturalcraft:vampire", "", 0, 0, 40));
        return out;
    }

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        for (Map.Entry<String, Memory> e : samples().entrySet()) out.put(e.getKey(), () -> plan(MemoryScenes.scene(e.getValue(), 12345L)));
        return out;
    }

    /** A scene as a plan: one zone of its cells, its figures and entry as points. */
    public static LayoutPlan plan(Scene scene) {
        Map<String, LayoutPoint> points = new LinkedHashMap<>();
        points.put("entry", new LayoutPoint(scene.entry()[0], scene.entry()[1], scene.entry()[2]));
        int n = 0;
        for (Figure f : scene.figures()) {
            String key = f.focus() ? "focus_" + f.pose() : "figure_" + n++ + "_" + f.pose();
            points.put(key, new LayoutPoint((int) Math.floor(f.dx()), (int) Math.floor(f.dy()), (int) Math.floor(f.dz())));
        }
        return new LayoutPlan(List.of(new LayoutPlan.Zone("scene", scene.cells())), List.of(), points);
    }
}
