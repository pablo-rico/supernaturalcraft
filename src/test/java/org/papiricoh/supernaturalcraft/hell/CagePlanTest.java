package org.papiricoh.supernaturalcraft.hell;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.buildkit.BlockIds;
import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.LayoutQuality;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.entity.boss.uncaged.UncagedTerrain;
import org.papiricoh.supernaturalcraft.hell.cage.CageLayout;
import org.papiricoh.supernaturalcraft.hell.cage.CagePlan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lucifer's Cage as a kit plan ({@link CagePlan}): the construction standard ({@link LayoutQuality}) and what the fight, the
 * iris and the ritual need of it. Look at the structview renders too ({@code LayoutDumpTest}, {@code hell_lucifers_cage}).
 */
class CagePlanTest {

    /** What nothing in the arena can move ({@code #arena_immune}, from {@code SNTagsProviders}): the Cage's own blocks. */
    static final Set<String> IMMUNE = Set.of("cage_bars", "cage_frame", "cage_seal", "cage_chain", "abyssal_bedrock", "enochian_pillar",
            "hellfire_brazier", "cage_ritual_stone", "hell_rift", "cage_post", "enochian_capital", "abyssal_bricks", "seal_lantern",
            "chain_anchor", "fallen_watcher");

    static Canvas c;
    static long millis;

    @BeforeAll
    static void draw() {
        Runtime rt = Runtime.getRuntime();
        long cold = System.nanoTime();
        CagePlan.canvas();  // the first draw loads the classes
        cold = (System.nanoTime() - cold) / 1_000_000;
        rt.gc();
        long before = rt.totalMemory() - rt.freeMemory();
        long t0 = System.nanoTime();
        c = CagePlan.canvas();
        millis = (System.nanoTime() - t0) / 1_000_000;
        long after = rt.totalMemory() - rt.freeMemory();
        System.out.println("Cage plan: " + c.size() + " cells in " + millis + " ms (first draw " + cold + " ms), canvas ~"
                + (after - before) / (1 << 20) + " MB; packed " + CagePlan.compiled().size() + " cells");
    }

    static boolean immune(String s) {
        return St.id(s).startsWith("supernaturalcraft:") && IMMUNE.contains(St.path(s));
    }

    @Test
    void meetsTheStandard() {
        List<String> mod = new ArrayList<>();
        for (String s : new HashSet<>(c.map().values())) if (St.id(s).startsWith("supernaturalcraft:")) mod.add(St.id(s));
        LayoutQuality.assertIdsKnown(c, BlockIds.vanilla().allow(mod.toArray(String[]::new)));
        for (String id : mod) assertTrue(Set.of("cage_bars", "cage_post", "cage_chain", "cage_frame", "cage_seal", "abyssal_bedrock", "abyssal_flagstone",
                "abyssal_bricks", "cage_ritual_stone", "hellfire_brazier", "abyssal_flagstone_stairs", "abyssal_flagstone_slab", "enochian_pillar",
                "enochian_capital", "seal_lantern", "chain_anchor", "fallen_watcher").contains(St.path(id)), "unexpected mod block " + id);
        LayoutQuality.assertBounds(c, CagePlan.bounds());
        LayoutQuality.assertBudget(c, 200_000);
        LayoutQuality.assertShapes(c);
        LayoutQuality.assertSupported(c);
        LayoutQuality.assertNoBlockEntities(c);
        LayoutQuality.assertPalette(c, 25);
        Set<String> ignore = new HashSet<>(LayoutQuality.NATURAL);
        ignore.add("supernaturalcraft:abyssal_flagstone");  // the floor's texture tiles in three sizes by itself
        LayoutQuality.assertSurfaceVariety(c, ignore);
        LayoutQuality.assertNoDarkInteriors(c, 8);
        assertTrue(millis < 4000, "the plan took " + millis + " ms to draw");
    }

    @Test
    void theCompiledPlanIsTheCanvas() {
        assertEquals(c.size(), CagePlan.compiled().size());
        assertEquals(c.map(), CagePlan.canvas().map(), "the plan is deterministic");
    }

    @Test
    void theFightFloorIsFlatAndClear() {
        int r = CageLayout.ISLAND_RADIUS, y = CageLayout.ISLAND_Y;
        List<String> bad = new ArrayList<>();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > (r + 0.5) * (r + 0.5) || Math.max(Math.abs(x), Math.abs(z)) <= CageLayout.DAIS_HALF + 2 || nearPillar(x, z)) continue;
                String floor = c.world(x, y, z);
                if (!Kinds.fullCube(floor) || !Kinds.floor(floor)) bad.add(x + "," + z + " floor " + floor);
                for (int h = 1; h <= 4; h++) if (!c.isAir(x, y + h, z)) bad.add(x + "," + (y + h) + "," + z + " " + c.world(x, y + h, z));
            }
        }
        assertTrue(bad.isEmpty(), bad.size() + " cells, e.g. " + bad.subList(0, Math.min(12, bad.size())));
    }

    static boolean nearPillar(int x, int z) {
        for (BlockPos p : CageLayout.pillars()) if (Math.abs(p.getX() - x) <= 2 && Math.abs(p.getZ() - z) <= 2) return true;
        return false;
    }

    /** The last phase drops the ring past the last stand: all of it can fall (nothing immune) and nothing is left under it. */
    @Test
    void theOuterRingCanFallAway() {
        int keep = UncagedTerrain.LAST_STAND_RADIUS, r = CageLayout.ISLAND_RADIUS, top = CageLayout.ALTAR.getY() - 1;
        List<String> bad = new ArrayList<>();
        for (var cell : c.cells()) {
            double d = Math.sqrt(cell.x() * cell.x() + cell.z() * cell.z());
            if (d <= keep || d > r + 0.5 || Kinds.air(cell.state())) continue;
            boolean pillar = nearPillar(cell.x(), cell.z()) && cell.y() > CageLayout.ISLAND_Y;
            if (cell.y() > top) continue;
            if (cell.y() < top - CageLayout.SLAB) bad.add(cell.x() + "," + cell.y() + "," + cell.z() + " under the ring: " + cell.state());
            else if (immune(cell.state()) && !pillar) bad.add(cell.x() + "," + cell.y() + "," + cell.z() + " immune: " + cell.state());
        }
        assertTrue(bad.isEmpty(), bad.size() + " cells, e.g. " + bad.subList(0, Math.min(12, bad.size())));
        for (int y = top - CageLayout.SLAB; y <= CageLayout.ISLAND_Y; y++) assertTrue(!c.isAir(r, y, 0), "the slab is " + CageLayout.SLAB + " thick");
    }

    @Test
    void theDaisHoldsTheCircleUnderTheIris() {
        BlockPos a = CageLayout.ALTAR;
        assertTrue(LayoutQuality.standable(c, a.getX(), a.getY(), a.getZ()), "the altar's spot");
        assertEquals("supernaturalcraft:cage_ritual_stone", c.get(a.getX(), a.getY() - 1, a.getZ()));
        for (BlockPos p : CageLayout.plinths()) assertEquals("supernaturalcraft:cage_ritual_stone", c.get(p.getX(), p.getY(), p.getZ()));
        // The circle (radius 4 round the altar) is drawn on full blocks.
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) assertTrue(Kinds.fullCube(c.world(x, CageLayout.DAIS_Y, z)), "dais " + x + "," + z);
        // A clear shaft from the dais up to the iris.
        for (int y = a.getY(); y < CageLayout.CAGE_FLOOR; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if (CageLayout.plinths().contains(new BlockPos(x, y, z))) continue;
                    assertTrue(c.isAir(x, y, z), "the shaft is blocked at " + x + "," + y + "," + z + ": " + c.world(x, y, z));
                }
            }
        }
        for (BlockPos p : CageLayout.irisCells()) assertEquals(CagePlan.irisState(p.getX(), p.getZ()), c.get(p.getX(), p.getY(), p.getZ()));
        for (BlockPos p : CageLayout.irisCells()) {
            for (int y = CageLayout.CAGE_FLOOR - 4; y < CageLayout.CAGE_FLOOR; y++) assertTrue(c.isAir(p.getX(), y, p.getZ()), "under the iris at " + p);
        }
        assertEquals("supernaturalcraft:cage_frame", c.get(CageLayout.CAGE_HALF, CageLayout.CAGE_FLOOR, 0), "UncagedSummoning.underCage looks here");
        BlockPos t = CageLayout.THRONE;
        assertTrue(LayoutQuality.standable(c, t.getX(), t.getY(), t.getZ()), "Lucifer's place in the Cage");
    }

    @Test
    void theWaysOutAreWalkable() {
        List<int[]> gates = new ArrayList<>();
        for (String d : new String[]{"east", "south", "west", "north"}) gates.add(c.anchor("gate_" + d));
        Set<Long> reach = LayoutQuality.walk(c, gates.getFirst());
        BlockPos a = CageLayout.ALTAR;
        assertTrue(reach.contains(Canvas.key(a.getX(), a.getY(), a.getZ())), "the altar out of reach from the east gate");
        for (int[] g : gates) assertTrue(reach.contains(Canvas.key(g[0], g[1], g[2])), "gate at " + g[0] + "," + g[2] + " out of reach");
        // Every bridge two high and five wide, end to end.
        for (int q = 0; q < 4; q++) {
            for (int t = CageLayout.RIM_RADIUS; t <= CageLayout.GATE_END; t++) {
                for (int w = -1; w <= 1; w++) {
                    int[] p = CageLayout.spoke(q, t, w);
                    assertTrue(LayoutQuality.standable(c, p[0], CageLayout.ISLAND_Y + 1, p[1]), "spoke " + q + " blocked at t=" + t + " w=" + w);
                }
            }
        }
    }

    @Test
    void theGreatChainsAreMadeFast() {
        List<BlockPos> anchors = CageLayout.anchors();
        assertEquals(8, anchors.size());
        for (BlockPos p : anchors) {
            assertEquals("supernaturalcraft:chain_anchor", St.id(c.world(p.getX(), p.getY(), p.getZ())), "anchor at " + p);
            assertTrue(Math.max(Math.abs(p.getX()), Math.abs(p.getZ())) + 4 <= CageLayout.REACH);
            int chains = 0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++)
                if (St.path(c.world(p.getX() + dx, p.getY() + dy, p.getZ() + dz)).equals("cage_chain")) chains++;
            assertTrue(chains > 0, "no chain reaches the anchor at " + p);
        }
        for (BlockPos p : CageLayout.pillars()) {
            int chains = 0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                if (St.path(c.world(p.getX() + dx, CageLayout.PILLAR_TOP + 1, p.getZ() + dz)).equals("cage_chain")) chains++;
            assertTrue(chains > 0, "no chain from the pillar at " + p);
        }
    }

    @Test
    void theCageStaysInItsBox() {
        Box b = CagePlan.cageBox();
        for (var z : c.zones()) if (z.name().equals("cage")) assertEquals(b, z.bounds());
        assertTrue(CagePlan.bounds().contains(CageLayout.THRONE.getX(), CageLayout.THRONE.getY(), CageLayout.THRONE.getZ()));
    }
}
