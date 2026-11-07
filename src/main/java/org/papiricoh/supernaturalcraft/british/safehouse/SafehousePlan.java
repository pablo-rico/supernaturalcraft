package org.papiricoh.supernaturalcraft.british.safehouse;

import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehousePlan;
import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Furniture;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.buildkit.Wood;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

/**
 * Kelly Kline's safe house (v0.41, season 12), a pure blueprint for the buildkit: a remote log cabin in a clearing in the pines,
 * where Dagon keeps the nephilim's mother until her time. A porch on the clearing, one room inside under the rafters: a stone
 * hearth, a little kitchen, a table, and in the far corner the nursery (a cot on the floor, a crib, a rocking chair). The clearing
 * in front is where Dagon stands guard and where the fight is: a woodpile, a split-rail fence, lamps. No block entities (the
 * whole place is her arena). Contract: {@link SafehouseAnchors}; {@code SafehousePlanQualityTest} holds it to the kit's bar.
 */
public final class SafehousePlan {

    static final int SEED = 41_300;
    /** The cabin's walls (inclusive) and their top; the porch's deck. */
    static final int WX0 = -7, WX1 = 7, WZ0 = 5, WZ1 = 15, TOP = 4, PORCH_Z0 = 2;

    static final Brush GROUND = Palette.patches2(SEED + 1, 3, "minecraft:grass_block", 8, "minecraft:podzol", 3,
            "minecraft:coarse_dirt", 2, "minecraft:moss_block", 1);
    static final Brush PATH = Palette.patches2(SEED + 2, 1.5, "minecraft:dirt_path", 5, "minecraft:coarse_dirt", 2, "minecraft:gravel", 1);
    static final Brush FLOOR = Palette.mix(SEED + 3, "minecraft:spruce_planks", 5, "minecraft:stripped_spruce_wood[axis=y]", 2,
            "minecraft:dark_oak_planks", 2, "minecraft:oak_planks", 1);
    static final Brush DECK = Palette.mix(SEED + 4, "minecraft:spruce_planks", 4, "minecraft:dark_oak_planks", 3,
            "minecraft:stripped_dark_oak_wood[axis=y]", 1);
    static final Brush FOOTING = Palette.mix(SEED + 5, "minecraft:cobblestone", 4, "minecraft:mossy_cobblestone", 2, "minecraft:stone", 1);
    static final Brush GABLE = Palette.mix(SEED + 6, "minecraft:spruce_planks", 4, "minecraft:stripped_spruce_wood[axis=y]", 2,
            "minecraft:dark_oak_planks", 2);
    static final Brush CHIMNEY = Palette.mix(SEED + 7, "minecraft:cobblestone", 4, "minecraft:mossy_cobblestone", 2,
            "minecraft:stone_bricks", 2, "minecraft:cracked_stone_bricks", 1);

    /** Log courses for a wall along {@code axis} ("x" or "z"). */
    static Brush logs(String axis, int seed) {
        return Palette.mix(seed, "minecraft:spruce_log[axis=" + axis + "]", 5, "minecraft:stripped_spruce_log[axis=" + axis + "]", 2,
                "minecraft:dark_oak_log[axis=" + axis + "]", 2);
    }

    private SafehousePlan() {
    }

    public static int[] rotate(int x, int z, int q) {
        return WarehousePlan.rotate(x, z, q);
    }

    public static LayoutPlan plan(int q) {
        return canvas(q).toPlan();
    }

    public static Canvas canvas(int q) {
        Canvas north = drawn();
        Canvas c = new Canvas("kelly_safehouse");
        int r = Math.max(Math.max(-SafehouseAnchors.MIN_X, SafehouseAnchors.MAX_X), Math.max(-SafehouseAnchors.MIN_Z, SafehouseAnchors.MAX_Z)) + 4;
        c.zone("safehouse", new Box(-r, SafehouseAnchors.BOTTOM_Y, -r, r, SafehouseAnchors.TOP_Y, r));
        c.inZone("safehouse", () -> c.stamp(north, 0, 0, 0, Math.floorMod(q, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    static Canvas drawn() {
        Canvas c = new Canvas("kelly_safehouse_north");
        clearing(c);
        cabin(c);
        porch(c);
        room(c);
        roof(c);
        woods(c);
        points(c);
        return c;
    }

    // --- the clearing ------------------------------------------------------------------------------------------------------------

    private static void clearing(Canvas c) {
        int g = SafehouseAnchors.GROUND_Y;
        for (int x = SafehouseAnchors.MIN_X; x <= SafehouseAnchors.MAX_X; x++) {
            for (int z = SafehouseAnchors.MIN_Z; z <= SafehouseAnchors.MAX_Z; z++) {
                boolean path = Math.abs(x + Noise.jitter(x, z, SEED + 10, 1)) <= 1 && z < PORCH_Z0;
                c.set(x, g, z, path ? PATH.at(x, g, z) : GROUND.at(x, g, z));
                c.set(x, g - 1, z, "minecraft:dirt");
            }
        }
        // A split-rail fence round the clearing's front, open where the track comes in.
        for (int x = -16; x <= 16; x++) {
            if (Math.abs(x) <= 2) continue;
            c.set(x, 0, -16, St.fence("minecraft:spruce_fence"));
        }
        for (int z = -15; z <= -8; z++) {
            c.set(-16, 0, z, St.fence("minecraft:spruce_fence"));
            c.set(16, 0, z, St.fence("minecraft:spruce_fence"));
        }
        // The woodpile under a lean-to, a chopping block.
        for (int x = -13; x <= -10; x++) {
            for (int z = -4; z <= -2; z++) {
                c.set(x, 0, z, St.axis(Noise.chance(x, 0, z, SEED + 11, 0.3) ? "minecraft:stripped_spruce_log" : "minecraft:spruce_log", "x"));
                if (z != -2 || x == -13) c.set(x, 1, z, St.axis(Noise.chance(x, 1, z, SEED + 12, 0.4) ? "minecraft:stripped_spruce_log" : "minecraft:spruce_log", "x"));
            }
            c.set(x, 3, -4, St.slabBottom("minecraft:spruce_slab"));
        }
        for (int y = 0; y <= 2; y++) {
            c.set(-14, y, -4, St.fence("minecraft:spruce_fence"));
            c.set(-9, y, -4, St.fence("minecraft:spruce_fence"));
        }
        c.set(-14, 3, -4, St.slabBottom("minecraft:spruce_slab"));
        c.set(-9, 3, -4, St.slabBottom("minecraft:spruce_slab"));
        c.set(-8, 0, -1, St.log("minecraft:oak_log"));
        // Lamps on posts either side of the clearing, and one where the track comes in.
        Lighting.lampPost(c, -7, 0, -5, 2, "minecraft:cobblestone", "minecraft:spruce_fence", null, false);
        Lighting.lampPost(c, 8, 0, -6, 2, "minecraft:cobblestone", "minecraft:spruce_fence", null, false);
        Lighting.lampPost(c, -3, 0, -16, 2, "minecraft:cobblestone", "minecraft:spruce_fence", null, false);
        // A water butt and a bench by the cabin.
        c.set(9, 0, 6, St.of("water_cauldron", "level", "3"));
        Furniture.sofa(c, 10, 0, 3, Dir.NORTH, 2, "minecraft:spruce_stairs", "minecraft:spruce_trapdoor");
    }

    // --- the cabin ---------------------------------------------------------------------------------------------------------------

    static final Box FOOT = new Box(WX0, 0, WZ0, WX1, 0, WZ1);

    private static void cabin(Canvas c) {
        int g = SafehouseAnchors.GROUND_Y;
        for (int x = WX0; x <= WX1; x++) {
            for (int z = WZ0; z <= WZ1; z++) {
                boolean edge = x == WX0 || x == WX1 || z == WZ0 || z == WZ1;
                c.set(x, g - 1, z, FOOTING.at(x, g - 1, z));
                c.set(x, g, z, edge ? FOOTING.at(x, g, z) : FLOOR.at(x, g, z));
                if (!edge) for (int y = 0; y <= TOP + 1; y++) c.carve(x, y, z);
            }
        }
        int k = 0;
        for (Walls.Run r : Walls.around(FOOT)) {
            String axis = r.out() == Dir.NORTH || r.out() == Dir.SOUTH ? "x" : "z";
            Walls.body(c, r, 0, TOP, logs(axis, SEED + 20 + k++));
            if (r.out() != Dir.NORTH) Walls.plinth(c, r, 0, 1, Family.COBBLESTONE, true);
            for (int i = 2; i < r.length() - 2; i += 3) {
                int x = r.x(i), z = r.z(i);
                if (r.out() == Dir.NORTH && Math.abs(x) <= 1) continue;
                if (r.out() == Dir.EAST && Math.abs(z - 10) <= 1) continue;
                for (int y = 1; y <= 2; y++) c.set(x, y, z, St.pane("minecraft:glass_pane"));
            }
        }
        Walls.cornerPosts(c, FOOT, 0, TOP, "minecraft:dark_oak_log[axis=y]");
        // The front door's opening.
        for (int y = 0; y <= 2; y++) c.carve(0, y, WZ0);
        c.set(0, 3, WZ0, "minecraft:stripped_dark_oak_log[axis=x]");
    }

    /** The porch: a plank deck, a rail with a gap for the steps, posts and a slab roof, a lantern. */
    private static void porch(Canvas c) {
        int g = SafehouseAnchors.GROUND_Y;
        for (int x = WX0; x <= WX1; x++) {
            for (int z = PORCH_Z0; z < WZ0; z++) c.set(x, g, z, DECK.at(x, g, z));
            if (Math.abs(x) >= 2) c.set(x, 0, PORCH_Z0, St.fence("minecraft:spruce_fence"));
        }
        for (int x : new int[]{WX0, WX1}) for (int y = 1; y <= 2; y++) c.set(x, y, PORCH_Z0, St.fence("minecraft:spruce_fence"));
        for (int x = WX0 - 1; x <= WX1 + 1; x++) {
            for (int z = PORCH_Z0; z < WZ0; z++) c.set(x, 3, z, St.slabBottom("minecraft:spruce_slab"));
        }
        Lighting.hanging(c, 0, 3, 3, 1, false);
        Furniture.chair(c, -4, 0, 4, Dir.NORTH, "minecraft:spruce_stairs", null);
        Furniture.chair(c, 4, 0, 4, Dir.NORTH, "minecraft:spruce_stairs", null);
    }

    /** The one room: hearth, kitchen, table, and the nursery corner. */
    private static void room(Canvas c) {
        // The hearth on the east wall, its stack outside.
        Furniture.fireplace(c, WX1 - 1, 0, 10, Dir.WEST, Family.COBBLESTONE, false);
        // The kitchen along the back wall.
        Furniture.kitchen(c, 2, 0, WZ1 - 1, Dir.EAST, 4, Dir.NORTH, "minecraft:spruce_planks", "minecraft:spruce_trapdoor", false);
        // The table and its lamp.
        Furniture.diningSet(c, 1, 9, 2, 9, 0, "minecraft:spruce_slab", "minecraft:spruce_stairs");
        c.set(1, 1, 9, St.lantern(false));
        c.set(2, 1, 9, St.candle("white", 2, true));
        // The nursery: Kelly's cot on the floor (blankets), the crib, a rocking chair, a lamp and a rug.
        c.set(WX0 + 1, 0, 11, St.carpet("light_gray"));
        c.set(WX0 + 1, 0, 12, St.carpet("white"));
        c.set(WX0 + 1, 0, 13, St.trapdoor("minecraft:spruce_trapdoor", Dir.NORTH, false, false));
        for (int x = -5; x <= -4; x++) {
            c.set(x, 0, WZ1 - 1, St.fence("minecraft:birch_fence"));
            c.set(x, 1, WZ1 - 1, St.carpet("light_blue"));
        }
        Furniture.chair(c, -3, 0, 13, Dir.WEST, "minecraft:birch_stairs", null);
        c.set(WX0 + 1, 0, 8, St.slabTop("minecraft:spruce_slab"));
        c.set(WX0 + 1, 1, 8, St.lantern(false));
        for (int x = -5; x <= -4; x++) for (int z = 10; z <= 12; z++) c.set(x, 0, z, St.carpet(Math.floorMod(x + z, 2) == 0 ? "yellow" : "white"));
        // A rug by the door and a coat peg.
        for (int x = -1; x <= 1; x++) c.set(x, 0, 6, St.carpet("brown"));
        c.set(-3, 0, 6, St.fence("minecraft:spruce_fence"));
        c.set(-3, 1, 6, St.trapdoor("minecraft:spruce_trapdoor", Dir.NORTH, true, false));
    }

    /** A steep gable of dark shingles, the rafters open inside, the chimney up the east end. */
    private static void roof(Canvas c) {
        Roofs.Style shingles = new Roofs.Style(Wood.DARK_OAK.family(), Wood.SPRUCE.family(), GABLE, 1, Roofs.Pitch.STEEP, 0, true,
                "minecraft:spruce_trapdoor", true);
        Roofs.gable(c, FOOT, TOP, true, shingles);
        Roofs.chimney(c, WX1 + 1, 10, 0, CHIMNEY, Family.COBBLESTONE, false);
    }

    /** Pines round the clearing's back and sides. */
    private static void woods(Canvas c) {
        int[][] pines = {{-16, -12}, {15, -13}, {-16, 8}, {16, 13}, {-13, 16}, {13, 16}, {-16, 2}};
        for (int i = 0; i < pines.length; i++) Trees.spruce(c, pines[i][0], 0, pines[i][1], 0, SEED + 40 + i);
    }

    // --- the points --------------------------------------------------------------------------------------------------------------

    private static void points(Canvas c) {
        c.anchor(SafehouseAnchors.ENTRY, 0, 0, -18);
        c.anchor(SafehouseAnchors.GUARD, 0, 0, -1);
        c.anchor(SafehouseAnchors.ARENA_CENTRE, 0, 0, 0);
        c.anchor(SafehouseAnchors.RITE, 0, 0, -10);
        c.anchor(SafehouseAnchors.COT, WX0 + 1, 0, 12);
        c.anchor(SafehouseAnchors.ROURKE, -2, 0, 11);
        c.anchor(SafehouseAnchors.RIFT, 5, 0, -2);
        c.anchor(SafehouseAnchors.gateMin(SafehouseAnchors.GATE_DOOR), 0, 0, WZ0);
        c.anchor(SafehouseAnchors.gateMax(SafehouseAnchors.GATE_DOOR), 0, 2, WZ0);
    }
}
