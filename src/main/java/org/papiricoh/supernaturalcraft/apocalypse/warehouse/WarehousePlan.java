package org.papiricoh.supernaturalcraft.apocalypse.warehouse;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

/**
 * The warehouse of "On the Head of a Pin" (v0.40), a pure blueprint for the buildkit: an abandoned brick-and-steel freight shed
 * on a gravel yard, where the angels have chained Alastair in a devil's trap. One tall hall: steel trusses with chains hanging
 * from them, pallets and crates along the walls, a forklift's dock by the loading doors, a clear floor in the middle for the
 * trap (painted by {@code WarehouseWorld}: the plan carries vanilla blocks only). No block entities: the whole shed is the
 * fight's arena. Contract: {@link WarehouseAnchors}; {@code WarehousePlanQualityTest} holds it to the kit's bar.
 */
public final class WarehousePlan {

    static final int SEED = 40_100;
    /** The shed's walls (inclusive), the roof's level, the belt course. */
    static final int WX0 = -16, WX1 = 16, WZ0 = -12, WZ1 = 12, ROOF = 9, BELT = 4;

    static final Brush BRICK = Palette.mix(SEED + 1, "minecraft:bricks", 8, "minecraft:mud_bricks", 2, "minecraft:granite", 1,
            "minecraft:red_terracotta", 1);
    static final Brush PANEL = Palette.mix(SEED + 2, "minecraft:light_gray_terracotta", 5, "minecraft:gray_concrete", 3,
            "minecraft:andesite", 2, "minecraft:cyan_terracotta", 1);
    static final Brush STONE = Palette.mix(SEED + 3, "minecraft:stone_bricks", 6, "minecraft:cracked_stone_bricks", 3,
            "minecraft:andesite", 2);
    static final Brush FLOOR = Palette.mix(SEED + 4, "minecraft:smooth_stone", 5, "minecraft:polished_andesite", 3,
            "minecraft:stone", 2, "minecraft:cracked_stone_bricks", 1, "minecraft:gravel", 1);
    static final Brush ROOFING = Palette.mix(SEED + 5, "minecraft:gray_concrete", 4, "minecraft:polished_deepslate", 2,
            "minecraft:cobbled_deepslate", 2, "minecraft:tuff", 2);
    static final Brush CEILING = Palette.mix(SEED + 6, "minecraft:spruce_planks", 5, "minecraft:dark_oak_planks", 3,
            "minecraft:stripped_spruce_wood[axis=y]", 2);
    static final Brush YARD = Palette.patches2(SEED + 7, 3, "minecraft:gravel", 8, "minecraft:coarse_dirt", 3, "minecraft:dirt_path", 2,
            "minecraft:grass_block", 3, "minecraft:cobblestone", 1);
    static final Brush FOOTING = Palette.mix(SEED + 8, "minecraft:cobblestone", 3, "minecraft:cobbled_deepslate", 2, "minecraft:stone", 2);
    static final Brush CRATE = Palette.mix(SEED + 9, "minecraft:stripped_spruce_wood[axis=y]", 4, "minecraft:spruce_planks", 3,
            "minecraft:stripped_oak_wood[axis=y]", 2, "minecraft:hay_block", 1);

    private WarehousePlan() {
    }

    public static int[] rotate(int x, int z, int q) {
        return switch (Math.floorMod(q, 4)) {
            case 1 -> new int[]{-z, x};
            case 2 -> new int[]{-x, -z};
            case 3 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    public static LayoutPlan plan(int q) {
        return canvas(q).toPlan();
    }

    public static Canvas canvas(int q) {
        Canvas north = drawn();
        Canvas c = new Canvas("alastairs_warehouse");
        int r = Math.max(Math.max(-WarehouseAnchors.MIN_X, WarehouseAnchors.MAX_X), Math.max(-WarehouseAnchors.MIN_Z, WarehouseAnchors.MAX_Z)) + 4;
        c.zone("warehouse", new Box(-r, WarehouseAnchors.BOTTOM_Y, -r, r, WarehouseAnchors.TOP_Y, r));
        c.inZone("warehouse", () -> c.stamp(north, 0, 0, 0, Math.floorMod(q, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    static Canvas drawn() {
        Canvas c = new Canvas("alastairs_warehouse_north");
        yard(c);
        shed(c);
        hall(c);
        points(c);
        return c;
    }

    // --- the yard ----------------------------------------------------------------------------------------------------------------

    private static void yard(Canvas c) {
        int g = WarehouseAnchors.GROUND_Y;
        for (int x = WarehouseAnchors.MIN_X; x <= WarehouseAnchors.MAX_X; x++) {
            for (int z = WarehouseAnchors.MIN_Z; z <= WarehouseAnchors.MAX_Z; z++) {
                c.set(x, g, z, YARD.at(x, g, z));
                c.set(x, g - 1, z, "minecraft:dirt");
            }
        }
        // A chain-link fence round the yard (bars), gapped at the gate, with lamp posts at its corners.
        for (int x = WarehouseAnchors.MIN_X + 1; x <= WarehouseAnchors.MAX_X - 1; x++) {
            if (Math.abs(x) > 3) c.set(x, 0, WarehouseAnchors.MIN_Z + 1, St.bars());
        }
        for (int z = WarehouseAnchors.MIN_Z + 1; z < WZ0 - 1; z++) {
            c.set(WarehouseAnchors.MIN_X + 1, 0, z, St.bars());
            c.set(WarehouseAnchors.MAX_X - 1, 0, z, St.bars());
        }
        Lighting.lampPost(c, WarehouseAnchors.MIN_X + 2, 0, WarehouseAnchors.MIN_Z + 2, 3, "minecraft:stone_bricks", "minecraft:spruce_fence", Dir.EAST, false);
        Lighting.lampPost(c, WarehouseAnchors.MAX_X - 2, 0, WarehouseAnchors.MIN_Z + 2, 3, "minecraft:stone_bricks", "minecraft:spruce_fence", Dir.WEST, false);
        Lighting.lampPost(c, -6, 0, WZ0 - 3, 3, "minecraft:stone_bricks", "minecraft:spruce_fence", Dir.EAST, false);
        Lighting.lampPost(c, 6, 0, WZ0 - 3, 3, "minecraft:stone_bricks", "minecraft:spruce_fence", Dir.WEST, false);
        // Pallets left out in the yard.
        for (int[] p : new int[][]{{-12, -18}, {-11, -18}, {12, -17}, {13, -17}, {12, -16}}) {
            c.set(p[0], 0, p[1], St.slabBottom("minecraft:spruce_slab"));
        }
        c.set(-12, 1, -18, CRATE.at(-12, 1, -18));
    }

    // --- the shed ----------------------------------------------------------------------------------------------------------------

    static final Box FOOT = new Box(WX0, 0, WZ0, WX1, 0, WZ1);

    private static void shed(Canvas c) {
        int g = WarehouseAnchors.GROUND_Y;
        for (int x = WX0; x <= WX1; x++) {
            for (int z = WZ0; z <= WZ1; z++) {
                c.set(x, g - 1, z, FOOTING.at(x, g - 1, z));
                boolean edge = x == WX0 || x == WX1 || z == WZ0 || z == WZ1;
                c.set(x, g, z, edge ? STONE.at(x, g, z) : FLOOR.at(x, g, z));
                if (edge) continue;
                for (int y = 0; y < ROOF; y++) c.carve(x, y, z);
                c.set(x, ROOF, z, CEILING.at(x, ROOF, z));
                c.set(x, ROOF + 1, z, ROOFING.at(x, ROOF + 1, z));
            }
        }
        for (Walls.Run r : Walls.around(FOOT)) {
            Walls.body(c, r, 0, 1, STONE);
            Walls.body(c, r, 2, BELT - 1, BRICK);
            Walls.body(c, r, BELT, ROOF + 1, PANEL);
            Walls.plinth(c, r, 0, 1, Family.STONE_BRICKS, true);
            Walls.belt(c, r, BELT, Family.STONE_BRICKS);
            Walls.cornice(c, r, ROOF + 1, Family.STONE_BRICKS, null);
            windows(c, r);
            for (int i = 0; i < r.length(); i++) {
                int x = r.x(i), z = r.z(i);
                c.set(x, ROOF + 2, z, Math.floorMod(i, 5) == 0 ? "minecraft:stone_bricks" : St.wall("minecraft:stone_brick_wall"));
            }
        }
        // The loading doors (north): a wide opening, a lintel of steel; the side door (south).
        for (int x = -3; x <= 3; x++) for (int y = 0; y <= 4; y++) c.carve(x, y, WZ0);
        for (int x = -4; x <= 4; x++) c.set(x, 5, WZ0, x == -4 || x == 4 ? "minecraft:chiseled_stone_bricks" : "minecraft:polished_andesite");
        for (int y = 0; y <= 2; y++) c.carve(0, y, WZ1);
        c.set(0, 3, WZ1, "minecraft:polished_andesite");
        // A faded freight company's name over the doors.
        for (int x = -3; x <= 3; x++) c.set(x, 7, WZ0, Noise.chance(x, 7, WZ0, SEED + 30, 0.4) ? "minecraft:light_gray_terracotta" : "minecraft:white_terracotta");
        // Roof vents.
        for (int[] v : new int[][]{{-8, -6}, {8, -6}, {-8, 6}, {8, 6}}) {
            c.set(v[0], ROOF + 2, v[1], "minecraft:stone_bricks");
            c.set(v[0], ROOF + 3, v[1], St.slabBottom("minecraft:stone_brick_slab"));
        }
    }

    /** High strip windows above the belt: grimy panes, some broken out. */
    private static void windows(Canvas c, Walls.Run r) {
        for (int i = 2; i < r.length() - 2; i++) {
            if (Math.floorMod(i, 4) == 0) continue;
            int x = r.x(i), z = r.z(i);
            if (r.out() == Dir.NORTH && Math.abs(x) <= 5) continue;
            for (int y = BELT + 2; y <= BELT + 3; y++) {
                c.set(x, y, z, Noise.chance(x, y, z, SEED + 40, 0.2) ? St.AIR : St.pane("minecraft:gray_stained_glass_pane"));
            }
        }
    }

    // --- the hall ----------------------------------------------------------------------------------------------------------------

    private static void hall(Canvas c) {
        // The trusses: dark beams across the hall, chains from the middle one down over the trap.
        for (int z = WZ0 + 3; z < WZ1; z += 6) {
            for (int x = WX0 + 1; x < WX1; x++) c.set(x, ROOF - 1, z, St.axis("minecraft:stripped_dark_oak_log", "x"));
        }
        for (int[] ch : new int[][]{{-2, -3}, {2, -3}, {-2, 3}, {2, 3}}) {
            for (int y = 4; y < ROOF - 1; y++) c.set(ch[0], y, ch[1], St.chain("y"));
        }
        // Lights: lanterns hanging in a grid (the middle left dark over the trap), sconces on the walls.
        for (int x = -10; x <= 10; x += 5) {
            for (int z = -8; z <= 8; z += 8) {
                if (Math.abs(x) <= 2) continue;
                Lighting.hanging(c, x, ROOF, z, 2, false);
            }
        }
        for (int z = -9; z <= 9; z += 6) {
            for (int x : new int[]{WX0 + 1, WX1 - 1}) {
                for (int y = 0; y <= 2; y++) c.set(x, y, z, CRATE.at(x, y, z));   // a stack of crates under each lamp
                c.set(x, 3, z, St.lantern(false));
            }
        }
        // Crates and pallets along the long walls, gaps between stacks.
        for (int z = WZ0 + 2; z <= WZ1 - 2; z++) {
            if (Math.floorMod(z, 5) == 0) continue;
            for (int[] side : new int[][]{{WX0 + 1, WX0 + 2}, {WX1 - 1, WX1 - 2}}) {
                int h = 1 + (int) Math.floor(Noise.value3(side[0], 0, z, 1, SEED + 50) * 2.99);
                for (int y = 0; y < h; y++) c.set(side[0], y, z, CRATE.at(side[0], y, z));
                if (Noise.chance(side[1], 0, z, SEED + 51, 0.4)) c.set(side[1], 0, z, St.slabBottom("minecraft:spruce_slab"));
            }
        }
        // The dock by the doors: a raised platform with a rail, and a workbench corner (a cart, tools).
        for (int x = -11; x <= -7; x++) for (int z = WZ0 + 1; z <= WZ0 + 3; z++) c.set(x, 0, z, St.slabBottom("minecraft:smooth_stone_slab"));
        for (int x = -11; x <= -7; x++) c.set(x, 1, WZ0 + 4, St.bars());
        c.set(9, 0, WZ0 + 2, "minecraft:smithing_table");
        c.set(10, 0, WZ0 + 2, "minecraft:crafting_table");
        c.set(11, 0, WZ0 + 2, St.facing("minecraft:anvil", Dir.NORTH));
        c.set(9, 0, WZ1 - 2, "minecraft:cauldron");
        c.set(10, 0, WZ1 - 2, St.facing("minecraft:grindstone", Dir.NORTH).replace("]", ",face=floor]"));
        // Salt lines the angels poured in a square round the trap (white carpet, low and walkable), candles at its corners.
        for (int d = -5; d <= 5; d++) {
            for (int[] s : new int[][]{{d, -5}, {d, 5}, {-5, d}, {5, d}}) c.set(s[0], 0, s[1], "minecraft:white_carpet");
        }
        for (int[] k : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) c.set(k[0], 0, k[1], St.candle("white", 3, true));
    }

    // --- the points --------------------------------------------------------------------------------------------------------------

    private static void points(Canvas c) {
        c.anchor(WarehouseAnchors.ENTRY, 0, 0, WZ0 - 5);
        c.anchor(WarehouseAnchors.TRAP, 0, 0, 0);
        c.anchor(WarehouseAnchors.ARENA_CENTRE, 0, 0, 0);
        c.anchor(WarehouseAnchors.gateMin(WarehouseAnchors.GATE_DOORS), -3, 0, WZ0);
        c.anchor(WarehouseAnchors.gateMax(WarehouseAnchors.GATE_DOORS), 3, 4, WZ0);
        int[][] angels = {{-4, 0, -3}, {4, 0, -3}, {-4, 0, 3}, {4, 0, 3}};
        int[][] spawns = {{-10, 0, -6}, {10, 0, -6}, {-10, 0, 6}, {10, 0, 6}};
        for (int n = 0; n < WarehouseAnchors.ANGELS; n++) c.anchor(WarehouseAnchors.angel(n), angels[n][0], angels[n][1], angels[n][2]);
        for (int n = 0; n < WarehouseAnchors.MIN_SPAWNS; n++) c.anchor(WarehouseAnchors.spawn(n), spawns[n][0], spawns[n][1], spawns[n][2]);
    }
}
