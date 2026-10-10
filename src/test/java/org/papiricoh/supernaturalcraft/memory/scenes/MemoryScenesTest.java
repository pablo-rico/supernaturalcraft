package org.papiricoh.supernaturalcraft.memory.scenes;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.papiricoh.supernaturalcraft.buildkit.BlockIds;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.LayoutQuality;
import org.papiricoh.supernaturalcraft.buildkit.Outside;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.memory.Memory;
import org.papiricoh.supernaturalcraft.memory.MemoryKind;
import org.papiricoh.supernaturalcraft.memory.MemoryText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every memory scene fits the stage and its arena, holds together, and has exactly one figure to touch. */
class MemoryScenesTest {

    static final BlockIds IDS = BlockIds.vanilla().allow("supernaturalcraft:cloud_stone", "supernaturalcraft:map_table",
            "supernaturalcraft:filing_cabinet");

    /** Every kind with a few subjects and details, every boss. */
    static Map<String, Memory> all() {
        Map<String, Memory> out = new java.util.LinkedHashMap<>(MemorySceneCatalog.samples());
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            out.put("boss " + b.name(), new Memory("boss:" + b.name(), MemoryKind.BOSS_VICTORY, b.name().toLowerCase(java.util.Locale.ROOT), "", 0, 0, 0));
        }
        for (String scenario : List.of("barn", "village", "graveyard", "haunted_house", "night_woods", "mine", "")) {
            out.put("case " + scenario, new Memory("case:" + scenario, MemoryKind.CASE_SOLVED, "supernaturalcraft:shapeshifter", scenario, 0, 0, 0));
        }
        for (MemoryKind k : MemoryKind.values()) out.put("bare " + k, new Memory("bare:" + k, k, "", "", 0, 0, 0));
        return out;
    }

    /** A scene as a canvas over the stage (its marble floor at y = -1 and below). */
    static Canvas canvas(String name, Scene s) {
        Canvas c = new Canvas(name).outside(Outside.groundUpTo(-1));
        for (ArenaCell a : s.cells()) c.set(a.dx(), a.dy(), a.dz(), a.block());
        return c;
    }

    @TestFactory
    List<DynamicTest> scenes() {
        List<DynamicTest> out = new ArrayList<>();
        for (Map.Entry<String, Memory> e : all().entrySet()) {
            out.add(DynamicTest.dynamicTest(e.getKey(), () -> {
                Memory m = e.getValue();
                Scene s = MemoryScenes.scene(m, 99L);
                assertTrue(s.cells().size() <= MemoryScenes.MAX_CELLS, s.cells().size() + " cells");
                Set<Long> seen = new HashSet<>();
                for (ArenaCell a : s.cells()) {
                    assertTrue(a.dx() * a.dx() + a.dz() * a.dz() <= MemoryScenes.RADIUS * MemoryScenes.RADIUS, "outside the stage: " + a);
                    assertTrue(a.dy() >= -1 && a.dy() < MemoryScenes.HEIGHT, "too high or deep: " + a);
                    assertTrue(!Kinds.blockEntity(a.block()), "block entity: " + a);
                    assertTrue(seen.add(Canvas.key(a.dx(), a.dy(), a.dz())), "two cells at " + a);
                    String p = St.path(a.block());
                    assertTrue(!p.equals("bedrock") && !p.equals("barrier") && !p.contains("portal") && !p.equals("fire"), "unsafe block " + a);
                }
                Canvas c = canvas(e.getKey(), s);
                LayoutQuality.assertIdsKnown(c, IDS);
                LayoutQuality.assertSupported(c);
                LayoutQuality.assertWaterContained(c);
                assertEquals(1, s.figures().stream().filter(Figure::focus).count(), "one focus");
                for (Figure f : s.figures()) assertTrue(Math.hypot(f.dx(), f.dz()) < MemoryScenes.RADIUS - 2, "figure off the stage " + f);
                int[] en = s.entry();
                assertTrue(LayoutQuality.standable(c, en[0], en[1], en[2]), "the entry is free");
                assertEquals(MemoryText.titleKey(m), s.titleKey());
                Scene again = MemoryScenes.scene(m, 99L);
                assertEquals(s.cells(), again.cells(), "deterministic");
            }));
        }
        return out;
    }

    @Test
    void scenesDiffer() {
        Set<Integer> looks = new HashSet<>();
        for (Memory m : MemorySceneCatalog.samples().values()) looks.add(MemoryScenes.scene(m, 1L).cells().hashCode());
        assertTrue(looks.size() >= MemorySceneCatalog.samples().size() - 1, "each sample has its own scene");
    }
}
