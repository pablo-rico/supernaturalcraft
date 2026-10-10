package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShapesTest {

    private static final String OAK = "minecraft:oak_stairs";

    @Test
    void theVanillaStairRule() {
        Canvas c = new Canvas("t");
        c.set(0, 0, 0, St.stairs(OAK, Dir.NORTH, false));
        c.set(0, 0, -1, St.stairs(OAK, Dir.EAST, false));
        assertEquals("outer_right", Shapes.stairShape(c, c.get(0, 0, 0), 0, 0, 0));
        c.set(0, 0, -1, St.stairs(OAK, Dir.WEST, false));
        assertEquals("outer_left", Shapes.stairShape(c, c.get(0, 0, 0), 0, 0, 0));
        c.remove(0, 0, -1);
        c.set(0, 0, 1, St.stairs(OAK, Dir.WEST, false));
        assertEquals("inner_left", Shapes.stairShape(c, c.get(0, 0, 0), 0, 0, 0));
        c.set(0, 0, 1, St.stairs(OAK, Dir.WEST, true));
        assertEquals("straight", Shapes.stairShape(c, c.get(0, 0, 0), 0, 0, 0), "a different half never joins");
    }

    @Test
    void aSkirtingTurnsItsCorners() {
        Canvas c = new Canvas("t");
        Box fp = new Box(0, 0, 0, 4, 0, 3);
        c.fill(fp.withY(0, 2), "minecraft:stone_bricks");
        for (Walls.Run r : Walls.around(fp)) Walls.plinth(c, r, 0, 1, Family.STONE_BRICKS, true);
        c.finish();
        for (int[] k : new int[][]{{-1, -1}, {5, -1}, {-1, 4}, {5, 4}}) {
            String s = c.get(k[0], 0, k[1]);
            assertTrue(St.get(s, "shape").startsWith("outer"), "skirting corner " + k[0] + "," + k[1] + ": " + s);
        }
        assertEquals("straight", St.get(c.get(2, 0, -1), "shape"));
    }

    @Test
    void fencesPanesAndWallsJoin() {
        Canvas c = new Canvas("t");
        for (int x = 0; x < 4; x++) c.set(x, 0, 0, St.fence("minecraft:oak_fence"));
        c.set(4, 0, 0, "minecraft:stone");
        c.set(0, 0, 2, St.pane("minecraft:glass_pane"));
        c.set(1, 0, 2, St.pane("minecraft:glass_pane"));
        for (int x = 0; x < 3; x++) c.set(x, 0, 4, St.wall("minecraft:cobblestone_wall"));
        c.set(1, 1, 4, "minecraft:stone");
        c.finish();
        String mid = c.get(1, 0, 0), end = c.get(3, 0, 0);
        assertEquals("true", St.get(mid, "east"));
        assertEquals("true", St.get(mid, "west"));
        assertEquals("false", St.get(mid, "north"));
        assertEquals("true", St.get(end, "east"), "a fence joins a full block");
        assertEquals("true", St.get(c.get(0, 0, 2), "east"));
        assertEquals("tall", St.get(c.get(1, 0, 4), "east"), "a wall under a block has tall sides");
        assertEquals("false", St.get(c.get(1, 0, 4), "up"), "a straight run under a block has no post");
        assertEquals("true", St.get(c.get(0, 0, 4), "up"), "a wall's end keeps its post");
        assertEquals("low", St.get(c.get(0, 0, 4), "east"));
    }

    @Test
    void finishingTwiceChangesNothing() {
        Canvas c = org.papiricoh.supernaturalcraft.buildkit.demo.DemoShop.build();
        java.util.Map<Long, String> once = new java.util.HashMap<>(c.map());
        c.finish();
        assertEquals(once, new java.util.HashMap<>(c.map()));
    }

    @Test
    void turningAStateTurnsItsFacingAxisAndSides() {
        assertEquals(St.stairs(OAK, Dir.EAST, true), St.rotate(St.stairs(OAK, Dir.NORTH, true), 1));
        assertEquals(St.stairs(OAK, Dir.WEST, true), St.rotate(St.stairs(OAK, Dir.NORTH, true), 3));
        assertEquals(St.axis("minecraft:oak_log", "z"), St.rotate(St.axis("minecraft:oak_log", "x"), 1));
        assertEquals(St.axis("minecraft:oak_log", "y"), St.rotate(St.axis("minecraft:oak_log", "y"), 1));
        String fence = St.with(St.fence("minecraft:oak_fence"), "north", "true");
        assertEquals("true", St.get(St.rotate(fence, 1), "east"));
        assertEquals("false", St.get(St.rotate(fence, 1), "north"));
        assertEquals("12", St.get(St.rotate(St.hangingSign("oak", 8), 1), "rotation"));
        Canvas prefab = new Canvas("p");
        prefab.set(1, 0, 0, St.stairs(OAK, Dir.EAST, false));
        Canvas c = new Canvas("t");
        c.stamp(prefab, 10, 0, 10, 1, Canvas.Mode.SET, null);
        assertEquals(St.stairs(OAK, Dir.SOUTH, false), c.get(10, 0, 11), "east of the origin turns to south of it");
    }
}
