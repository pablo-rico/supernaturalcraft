package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoofsTest {

    static final Box FOOT = new Box(0, 0, 0, 8, 0, 6);
    static final int TOP = 4;

    private static Roofs.Style style(Roofs.Pitch p) {
        return new Roofs.Style(Wood.DARK_OAK.family(), Wood.SPRUCE.family(), Brush.of("minecraft:spruce_planks"), 1, p, 2, true,
                "minecraft:spruce_trapdoor", true);
    }

    /** A walled box with a roof of {@code shape} and {@code pitch}. */
    private static Canvas house(Roofs.Shape shape, Roofs.Pitch pitch) {
        Canvas c = new Canvas("roof");
        Walls.enclose(c, FOOT, 0, TOP, Brush.of("minecraft:stone_bricks"));
        Roofs.Plan plan = new Roofs.Plan();
        switch (shape) {
            case GABLE_X -> plan.gable(FOOT, TOP, true, style(pitch));
            case GABLE_Z -> plan.gable(FOOT, TOP, false, style(pitch));
            case HIP -> plan.hip(FOOT, TOP, style(pitch));
            case SHED -> plan.shed(FOOT, TOP, Dir.NORTH, style(pitch));
        }
        plan.draw(c);
        return c.finish();
    }

    /** The y of the highest non-air planned cell of a column above the walls' foot, or MIN_VALUE. */
    private static int top(Canvas c, int x, int z) {
        return c.topAt(x, z, 60, TOP - 4);
    }

    @Test
    void everyRoofIsClosedAndOverhangs() {
        for (Roofs.Shape shape : Roofs.Shape.values()) {
            for (Roofs.Pitch pitch : Roofs.Pitch.values()) {
                Canvas c = house(shape, pitch);
                String what = shape + "/" + pitch;
                List<String> bad = new ArrayList<>();
                Box over = FOOT.growXZ(1);
                for (int x = over.x0(); x <= over.x1(); x++) {
                    for (int z = over.z0(); z <= over.z1(); z++) {
                        int t = top(c, x, z);
                        if (t == Integer.MIN_VALUE) {
                            bad.add(x + "," + z + " no roof");
                            continue;
                        }
                        // No step bigger than a block between neighbouring columns: no holes, no cliffs.
                        for (Dir d : new Dir[]{Dir.EAST, Dir.SOUTH}) {
                            int nx = x + d.dx, nz = z + d.dz;
                            if (!over.containsColumn(nx, nz)) continue;
                            int n = top(c, nx, nz);
                            int limit = pitch == Roofs.Pitch.MANSARD ? 3 : 2;
                            if (n != Integer.MIN_VALUE && Math.abs(n - t) > limit) bad.add(x + "," + z + " step " + (n - t) + " to " + d.id());
                        }
                    }
                }
                // Inside the walls the roof covers every column above the wall top.
                for (int x = FOOT.x0(); x <= FOOT.x1(); x++) {
                    for (int z = FOOT.z0(); z <= FOOT.z1(); z++) {
                        if (top(c, x, z) <= TOP) bad.add(x + "," + z + " open to the sky");
                    }
                }
                assertTrue(bad.isEmpty(), what + ": " + bad);
            }
        }
    }

    @Test
    void stairsAreFacedHalvedAndShaped() {
        for (Roofs.Shape shape : Roofs.Shape.values()) {
            for (Roofs.Pitch pitch : Roofs.Pitch.values()) {
                Canvas c = house(shape, pitch);
                LayoutQuality.assertShapes(c);
                for (String s : c.map().values()) {
                    if (!Kinds.stairs(s)) continue;
                    assertTrue(St.get(s, "facing") != null && St.get(s, "half") != null && St.get(s, "shape") != null, s);
                }
            }
        }
    }

    @Test
    void gableEndsAreFilled() {
        Canvas c = house(Roofs.Shape.GABLE_X, Roofs.Pitch.STEEP);
        for (int x : new int[]{FOOT.x0(), FOOT.x1()}) {
            for (int z = FOOT.z0(); z <= FOOT.z1(); z++) {
                int t = top(c, x, z);
                for (int y = TOP + 1; y < t; y++) assertTrue(!c.isAir(x, y, z), "hole in the gable at " + x + "," + y + "," + z);
            }
        }
        // The ridge: one centre row (span 9 with the overhang) of full blocks under a cap of slabs.
        int mid = FOOT.centerZ();
        String ridge = c.get(4, top(c, 4, mid), mid);
        assertTrue(Kinds.slab(ridge) && "bottom".equals(St.get(ridge, "type")), "ridge cap " + ridge);
    }

    @Test
    void stairsClimbTowardTheRidge() {
        Canvas c = house(Roofs.Shape.GABLE_X, Roofs.Pitch.STEEP);
        // North eave row: stairs whose backs face south (up the slope).
        String eave = c.get(4, TOP, FOOT.z0() - 1);
        assertEquals("south", St.get(eave, "facing"), eave);
        assertEquals("bottom", St.get(eave, "half"));
        String south = c.get(4, TOP, FOOT.z1() + 1);
        assertEquals("north", St.get(south, "facing"));
        // A fascia board hangs under the eave.
        assertTrue(St.path(c.get(4, TOP - 1, FOOT.z0() - 1)).endsWith("_trapdoor"));
    }

    @Test
    void hipsTurnTheirCorners() {
        Canvas c = house(Roofs.Shape.HIP, Roofs.Pitch.STEEP);
        String corner = c.get(FOOT.x0() - 1, TOP, FOOT.z0() - 1);
        assertTrue(Kinds.stairs(corner) && St.get(corner, "shape").startsWith("outer"), "hip corner " + corner);
    }

    @Test
    void crossGablesMeetInValleys() {
        Canvas c = new Canvas("cross");
        Box wing = new Box(3, 0, 6, 5, 0, 11);
        Walls.enclose(c, FOOT, 0, TOP, Brush.of("minecraft:stone_bricks"));
        Walls.enclose(c, wing, 0, TOP, Brush.of("minecraft:stone_bricks"));
        new Roofs.Plan().gable(FOOT, TOP, true, style(Roofs.Pitch.STEEP)).gable(wing, TOP, false, style(Roofs.Pitch.STEEP)).draw(c);
        c.finish();
        LayoutQuality.assertShapes(c);
        long inner = c.map().values().stream().filter(s -> Kinds.stairs(s) && St.get(s, "shape").startsWith("inner")).count();
        assertTrue(inner >= 2, "valleys show as inner corners: " + inner);
        assertTrue(top(c, 4, 11) > TOP, "the wing is roofed to its end");
    }
}
