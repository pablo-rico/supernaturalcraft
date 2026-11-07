package org.papiricoh.supernaturalcraft.british.compound;

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
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.buildkit.Wood;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

/**
 * The British Men of Letters' compound (v0.41, season 12), a pure blueprint for the buildkit: an English country manor turned
 * headquarters behind iron railings. A gravel drive runs from the gate to a pillared portico; inside, a panelled lobby, the war
 * room in the middle (a long table, a map wall, Hess's place at its head), a records library to the west and a wireless room to
 * the east, under a slate roof. No block entities: the whole manor is Hess's arena. Contract: {@link CompoundAnchors};
 * {@code BritishCompoundPlanQualityTest} holds it to the kit's bar.
 */
public final class CompoundPlan {

    static final int SEED = 41_200;
    /** The manor's outer walls (inclusive), the ceiling's level, the belt course. */
    static final int WX0 = -18, WX1 = 18, WZ0 = -10, WZ1 = 12, CEIL = 8, BELT = 4;
    /** The inner walls: the lobby's back (z), the war room's sides (x). */
    static final int LOBBY_Z = -6, WAR_X = 10;
    /** The railings' line and the gate. */
    static final int RAIL_Z = -26, RAIL_X = 23, RAIL_BACK = 19;

    static final Brush STONE = Palette.mix(SEED + 1, "minecraft:stone_bricks", 6, "minecraft:cracked_stone_bricks", 2,
            "minecraft:mossy_stone_bricks", 1, "minecraft:polished_andesite", 2, "minecraft:tuff_bricks", 1);
    static final Brush FOUNDATION = Palette.mix(SEED + 2, "minecraft:deepslate_bricks", 4, "minecraft:cracked_deepslate_bricks", 2,
            "minecraft:polished_deepslate", 2, "minecraft:deepslate_tiles", 1);
    static final Brush TRIM = Palette.mix(SEED + 3, "minecraft:calcite", 3, "minecraft:polished_diorite", 2, "minecraft:smooth_quartz", 1);
    static final Brush FLOOR = Palette.mix(SEED + 4, "minecraft:dark_oak_planks", 5, "minecraft:spruce_planks", 3,
            "minecraft:stripped_dark_oak_wood[axis=y]", 2, "minecraft:stripped_spruce_wood[axis=y]", 1);
    static final Brush WAR_FLOOR = Palette.mix(SEED + 5, "minecraft:polished_deepslate", 4, "minecraft:deepslate_tiles", 3,
            "minecraft:polished_blackstone", 2, "minecraft:deepslate_bricks", 1);
    static final Brush CEILING = Palette.mix(SEED + 6, "minecraft:dark_oak_planks", 5, "minecraft:spruce_planks", 3,
            "minecraft:stripped_dark_oak_wood[axis=y]", 2);
    static final Brush PANEL = Palette.mix(SEED + 7, "minecraft:dark_oak_planks", 4, "minecraft:stripped_dark_oak_wood[axis=y]", 3,
            "minecraft:spruce_planks", 2);
    static final Brush PLASTER = Palette.mix(SEED + 8, "minecraft:calcite", 4, "minecraft:white_terracotta", 3,
            "minecraft:smooth_quartz", 1, "minecraft:polished_diorite", 2);
    static final Brush MAP = Palette.patches(SEED + 9, 1.6, "minecraft:blue_terracotta", 4, "minecraft:light_blue_terracotta", 2,
            "minecraft:green_terracotta", 3, "minecraft:lime_terracotta", 1, "minecraft:white_terracotta", 2, "minecraft:brown_terracotta", 2);
    static final Brush LAWN = Palette.patches2(SEED + 10, 3, "minecraft:grass_block", 9, "minecraft:moss_block", 2,
            "minecraft:coarse_dirt", 1, "minecraft:podzol", 1);
    static final Brush DRIVE = Palette.patches2(SEED + 11, 2, "minecraft:gravel", 6, "minecraft:cobblestone", 2,
            "minecraft:coarse_dirt", 1, "minecraft:andesite", 1);
    static final Brush ATTIC = Palette.mix(SEED + 12, "minecraft:spruce_planks", 3, "minecraft:dark_oak_planks", 2, "minecraft:stone_bricks", 1);
    static final Brush GABLE = Palette.mix(SEED + 13, "minecraft:stone_bricks", 5, "minecraft:cracked_stone_bricks", 2,
            "minecraft:polished_andesite", 2, "minecraft:calcite", 1);

    private CompoundPlan() {
    }

    public static int[] rotate(int x, int z, int q) {
        return WarehousePlan.rotate(x, z, q);
    }

    public static LayoutPlan plan(int q) {
        return canvas(q).toPlan();
    }

    public static Canvas canvas(int q) {
        Canvas north = drawn();
        Canvas c = new Canvas("british_compound");
        int r = Math.max(Math.max(-CompoundAnchors.MIN_X, CompoundAnchors.MAX_X), Math.max(-CompoundAnchors.MIN_Z, CompoundAnchors.MAX_Z)) + 4;
        c.zone("compound", new Box(-r, CompoundAnchors.BOTTOM_Y, -r, r, CompoundAnchors.TOP_Y, r));
        c.inZone("compound", () -> c.stamp(north, 0, 0, 0, Math.floorMod(q, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    static Canvas drawn() {
        Canvas c = new Canvas("british_compound_north");
        grounds(c);
        manor(c);
        portico(c);
        lobby(c);
        warRoom(c);
        wings(c);
        roof(c);
        points(c);
        return c;
    }

    // --- the grounds -------------------------------------------------------------------------------------------------------------

    private static boolean drive(int x, int z) {
        return Math.abs(x) <= 3 && z < WZ0 || Math.abs(x) <= 7 && z >= -24 && z < WZ0;
    }

    private static void grounds(Canvas c) {
        int g = CompoundAnchors.GROUND_Y;
        for (int x = CompoundAnchors.MIN_X; x <= CompoundAnchors.MAX_X; x++) {
            for (int z = CompoundAnchors.MIN_Z; z <= CompoundAnchors.MAX_Z; z++) {
                c.set(x, g, z, drive(x, z) ? DRIVE.at(x, g, z) : LAWN.at(x, g, z));
                c.set(x, g - 1, z, "minecraft:dirt");
            }
        }
        // The railings: iron bars on a low stone kerb, stone piers with lanterns every few cells, the gate open in the front.
        for (int x = -RAIL_X; x <= RAIL_X; x++) {
            if (Math.abs(x) <= 2) continue;
            rail(c, x, RAIL_Z, Math.abs(x) == 3 || Math.floorMod(x, 6) == 0);
            rail(c, x, RAIL_BACK, Math.floorMod(x, 6) == 0 || Math.abs(x) == RAIL_X);
        }
        for (int z = RAIL_Z + 1; z < RAIL_BACK; z++) {
            boolean pier = Math.floorMod(z, 6) == 0;
            rail(c, -RAIL_X, z, pier);
            rail(c, RAIL_X, z, pier);
        }
        // Box hedges along the drive and lamp posts at the forecourt's corners.
        for (int z = -24; z <= -13; z++) {
            if (z == -19 || z == -18) continue;
            c.set(-9, 0, z, St.leaves("minecraft:azalea_leaves"));
            c.set(9, 0, z, St.leaves("minecraft:azalea_leaves"));
        }
        Lighting.lampPost(c, -8, 0, -25, 3, "minecraft:polished_deepslate", "minecraft:dark_oak_fence", null, false);
        Lighting.lampPost(c, 8, 0, -25, 3, "minecraft:polished_deepslate", "minecraft:dark_oak_fence", null, false);
        Lighting.lampPost(c, -8, 0, -12, 3, "minecraft:polished_deepslate", "minecraft:dark_oak_fence", null, false);
        Lighting.lampPost(c, 8, 0, -12, 3, "minecraft:polished_deepslate", "minecraft:dark_oak_fence", null, false);
        // A few flower beds against the manor's front.
        for (int x = WX0 + 2; x <= WX1 - 2; x++) {
            if (Math.abs(x) <= 8) continue;
            if (Noise.chance(x, 0, WZ0 - 2, SEED + 20, 0.55)) {
                c.set(x, 0, WZ0 - 2, Noise.chance(x, 1, WZ0 - 2, SEED + 21, 0.5) ? "minecraft:rose_bush[half=lower]" : "minecraft:peony[half=lower]");
                c.set(x, 1, WZ0 - 2, Noise.chance(x, 1, WZ0 - 2, SEED + 21, 0.5) ? "minecraft:rose_bush[half=upper]" : "minecraft:peony[half=upper]");
            }
        }
    }

    private static void rail(Canvas c, int x, int z, boolean pier) {
        if (pier) {
            c.set(x, 0, z, FOUNDATION.at(x, 0, z));
            c.set(x, 1, z, "minecraft:stone_bricks");
            c.set(x, 2, z, St.lantern(false));
        } else {
            c.set(x, 0, z, St.slabBottom("minecraft:stone_brick_slab"));
            c.set(x, 1, z, St.bars());
        }
    }

    // --- the manor ---------------------------------------------------------------------------------------------------------------

    static final Box FOOT = new Box(WX0, 0, WZ0, WX1, 0, WZ1);

    private static void manor(Canvas c) {
        int g = CompoundAnchors.GROUND_Y;
        for (int x = WX0; x <= WX1; x++) {
            for (int z = WZ0; z <= WZ1; z++) {
                boolean edge = x == WX0 || x == WX1 || z == WZ0 || z == WZ1;
                boolean war = Math.abs(x) < WAR_X && z > LOBBY_Z;
                c.set(x, g - 1, z, FOUNDATION.at(x, g - 1, z));
                c.set(x, g, z, edge ? FOUNDATION.at(x, g, z) : war ? WAR_FLOOR.at(x, g, z) : FLOOR.at(x, g, z));
                if (edge) continue;
                for (int y = 0; y < CEIL; y++) c.carve(x, y, z);
                c.set(x, CEIL, z, CEILING.at(x, CEIL, z));
            }
        }
        for (Walls.Run r : Walls.around(FOOT)) {
            Walls.body(c, r, 0, CEIL, STONE);
            Walls.plinth(c, r, 0, 1, Family.DEEPSLATE_BRICKS, true);
            Walls.belt(c, r, BELT, Family.STONE_BRICKS);
            Walls.cornice(c, r, CEIL, Family.STONE_BRICKS, null);
            windows(c, r);
        }
        Walls.quoins(c, FOOT, 0, CEIL - 1, TRIM);
        // The front door: a wide doorway under a calcite arch.
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 3; y++) c.carve(x, y, WZ0);
        for (int x = -2; x <= 2; x++) c.set(x, 4, WZ0, x == 0 ? "minecraft:chiseled_stone_bricks" : TRIM.at(x, 4, WZ0));
        for (int y = 0; y <= 3; y++) {
            c.set(-2, y, WZ0, TRIM.at(-2, y, WZ0));
            c.set(2, y, WZ0, TRIM.at(2, y, WZ0));
        }
    }

    /** Tall sash windows in pairs of panes, two rows of them, between the plinth and the cornice. */
    private static void windows(Canvas c, Walls.Run r) {
        for (int i = 2; i < r.length() - 2; i++) {
            if (Math.floorMod(i, 4) != 2) continue;
            int x = r.x(i), z = r.z(i);
            if (r.out() == Dir.NORTH && Math.abs(x) <= 3) continue;
            if (Math.abs(x) == WAR_X || z == LOBBY_Z) continue;
            for (int y = 2; y <= 3; y++) c.set(x, y, z, St.pane("minecraft:glass_pane"));
            for (int y = 5; y <= 6; y++) c.set(x, y, z, St.pane("minecraft:glass_pane"));
        }
    }

    /** The portico over the front door: two stone columns, a calcite entablature, a slab canopy. */
    private static void portico(Canvas c) {
        for (int x : new int[]{-3, 3}) {
            for (int z : new int[]{WZ0 - 3, WZ0 - 1}) {
                c.set(x, 0, z, FOUNDATION.at(x, 0, z));
                for (int y = 1; y <= 4; y++) c.set(x, y, z, St.wall("minecraft:stone_brick_wall"));
                c.set(x, 5, z, TRIM.at(x, 5, z));
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = WZ0 - 4; z <= WZ0 - 1; z++) {
                if ((x == -3 || x == 3) && (z == WZ0 - 3 || z == WZ0 - 1)) continue;
                c.set(x, 5, z, St.slabBottom("minecraft:polished_deepslate_slab"));
            }
            c.set(x, 6, WZ0 - 4, St.slabBottom("minecraft:stone_brick_slab"));
        }
        Lighting.hanging(c, 0, 5, WZ0 - 2, 1, false);
        // A red runner from the drive in.
        for (int z = WZ0 - 3; z < WZ0; z++) c.set(0, 0, z, St.carpet("red"));
    }

    // --- inside ----------------------------------------------------------------------------------------------------------------

    /** An inner wall: dark oak panelling below the rail, plaster above. */
    private static void inner(Canvas c, int x, int z) {
        for (int y = 0; y < CEIL; y++) c.set(x, y, z, y <= 2 ? PANEL.at(x, y, z) : y == 3 ? "minecraft:stripped_dark_oak_wood[axis=y]" : PLASTER.at(x, y, z));
    }

    private static void lobby(Canvas c) {
        for (int x = WX0 + 1; x < WX1; x++) inner(c, x, LOBBY_Z);
        // The arch into the war room, and the doors into the wings.
        for (int x = -2; x <= 2; x++) for (int y = 0; y <= 3; y++) c.carve(x, y, LOBBY_Z);
        for (int x : new int[]{-14, 14}) for (int y = 0; y <= 2; y++) c.carve(x, y, LOBBY_Z);
        // A runner down the lobby and to the arch.
        for (int x = -12; x <= 12; x++) c.set(x, 0, -8, St.carpet("red"));
        for (int z = WZ0 + 1; z < -8; z++) c.set(0, 0, z, St.carpet("red"));
        // Benches and coat stands along the front wall.
        Furniture.sofa(c, -8, 0, WZ0 + 1, Dir.SOUTH, 3, "minecraft:dark_oak_stairs", "minecraft:dark_oak_trapdoor");
        Furniture.sofa(c, 6, 0, WZ0 + 1, Dir.SOUTH, 3, "minecraft:dark_oak_stairs", "minecraft:dark_oak_trapdoor");
        for (int x : new int[]{-16, 16}) {
            c.set(x, 0, WZ0 + 1, St.fence("minecraft:dark_oak_fence"));
            c.set(x, 1, WZ0 + 1, St.fence("minecraft:dark_oak_fence"));
            c.set(x, 2, WZ0 + 1, St.trapdoor("minecraft:spruce_trapdoor", Dir.NORTH, true, false));
        }
        for (int x : new int[]{-12, 0, 12}) Lighting.hanging(c, x, CEIL, -8, 2, false);
    }

    private static void warRoom(Canvas c) {
        for (int z = LOBBY_Z + 1; z < WZ1; z++) {
            inner(c, -WAR_X, z);
            inner(c, WAR_X, z);
        }
        // Doors from the war room into each wing.
        for (int x : new int[]{-WAR_X, WAR_X}) for (int y = 0; y <= 2; y++) c.carve(x, y, 3);
        // The long table, its chairs, papers and lamps on it.
        Furniture.diningSet(c, -1, -3, 1, 3, 0, "minecraft:dark_oak_slab", "minecraft:dark_oak_stairs");
        c.set(0, 1, -2, St.lantern(false));
        c.set(0, 1, 2, St.lantern(false));
        c.set(-1, 1, 0, St.carpet("white"));
        c.set(1, 1, 1, St.carpet("white"));
        c.set(1, 1, -1, St.candle("red", 2, true));
        // The map wall behind Hess's place: a framed board against the south wall, a map of the world in terracotta, pins in red.
        int board = WZ1 - 1;
        for (int x = -7; x <= 7; x++) {
            for (int y = 0; y <= 7; y++) {
                boolean frame = Math.abs(x) == 7 || y <= 1 || y == 7;
                c.set(x, y, board, frame ? PANEL.at(x, y, board) : MAP.at(x, y, board));
            }
            if (Math.abs(x) < 7) c.set(x, 1, board - 1, St.trapdoor("minecraft:dark_oak_trapdoor", Dir.NORTH, true, false));
        }
        for (int[] pin : new int[][]{{-4, 5}, {-1, 3}, {2, 4}, {5, 6}, {0, 5}}) c.set(pin[0], pin[1], board, "minecraft:red_terracotta");
        // Bookshelves and filing along the side walls, gaps for the doors.
        for (int z = LOBBY_Z + 2; z <= WZ1 - 2; z++) {
            if (Math.abs(z - 3) <= 1) continue;
            for (int x : new int[]{-WAR_X + 1, WAR_X - 1}) {
                c.set(x, 0, z, "minecraft:bookshelf");
                c.set(x, 1, z, Math.floorMod(z, 3) == 0 ? "minecraft:chiseled_stone_bricks" : "minecraft:bookshelf");
            }
        }
        // Lights over the table and in the corners.
        Lighting.hanging(c, 0, CEIL, 0, 3, false);
        for (int[] l : new int[][]{{-6, -3}, {6, -3}, {-6, 8}, {6, 8}}) Lighting.hanging(c, l[0], CEIL, l[1], 2, false);
    }

    private static void wings(Canvas c) {
        int[] sides = {-1, 1};
        for (int s : sides) {
            int x0 = s < 0 ? WX0 + 1 : WAR_X + 1, x1 = s < 0 ? -WAR_X - 1 : WX1 - 1;
            int mid = (x0 + x1) / 2;
            if (s < 0) {
                // Records: shelves in rows, reading desks between them.
                for (int z = LOBBY_Z + 3; z <= WZ1 - 2; z += 4) {
                    for (int x = x0 + 1; x <= x1 - 2; x++) {
                        c.set(x, 0, z, "minecraft:bookshelf");
                        c.set(x, 1, z, Math.floorMod(x, 2) == 0 ? "minecraft:bookshelf" : St.slabBottom("minecraft:dark_oak_slab"));
                    }
                }
                Furniture.desk(c, x0 + 1, 0, LOBBY_Z + 1, Dir.NORTH, "minecraft:spruce_slab", "minecraft:spruce_stairs", false);
                Furniture.desk(c, x0 + 5, 0, LOBBY_Z + 1, Dir.NORTH, "minecraft:spruce_slab", "minecraft:spruce_stairs", false);
            } else {
                // The wireless room: radio sets (iron trapdoors on desks), a workbench, the armoury rack.
                for (int z = LOBBY_Z + 2; z <= WZ1 - 2; z += 3) {
                    c.set(x1, 0, z, St.slabTop("minecraft:polished_andesite_slab"));
                    c.set(x1, 1, z, St.trapdoor("minecraft:iron_trapdoor", Dir.WEST, false, true));
                    c.set(x1 - 1, 0, z, St.stairs("minecraft:spruce_stairs", Dir.WEST, false));
                }
                c.set(x0 + 1, 0, WZ1 - 1, "minecraft:smithing_table");
                c.set(x0 + 2, 0, WZ1 - 1, "minecraft:crafting_table");
                c.set(x0 + 3, 0, WZ1 - 1, St.facing("minecraft:anvil", Dir.EAST));
                for (int z = LOBBY_Z + 2; z <= LOBBY_Z + 4; z++) {
                    c.set(x0, 0, z, St.slabTop("minecraft:dark_oak_slab"));
                    c.set(x0, 1, z, St.trapdoor("minecraft:dark_oak_trapdoor", Dir.EAST, true, false));
                }
            }
            for (int z : new int[]{-8, 2, 9}) {
                if (z == -8) continue;
                Lighting.hanging(c, mid, CEIL, z, 2, false);
            }
            Lighting.hanging(c, mid - 2, CEIL, -1, 2, false);
        }
    }

    /** A slate roof, gabled east to west, its attic packed (nobody walks up there), and two chimneys on the end walls. */
    private static void roof(Canvas c) {
        Roofs.Style slate = new Roofs.Style(Family.DEEPSLATE_TILES, Wood.DARK_OAK.family(), GABLE, 1, Roofs.Pitch.HALF, 0, true,
                "minecraft:dark_oak_trapdoor", false);
        Roofs.gable(c, FOOT, CEIL, true, slate);
        Box roofBox = new Box(WX0 - 2, CEIL, WZ0 - 2, WX1 + 2, CompoundAnchors.TOP_Y, WZ1 + 2);
        Palette.swapFamily(c, roofBox, Family.DEEPSLATE_TILES, Family.COBBLED_DEEPSLATE, 0.12, 2.5, SEED + 30);
        Palette.swapFamily(c, roofBox, Family.DEEPSLATE_TILES, Family.DEEPSLATE_BRICKS, 0.12, 2, SEED + 31);
        for (int x = WX0 + 1; x < WX1; x++) {
            for (int z = WZ0 + 1; z < WZ1; z++) {
                for (int y = CEIL + 1; y <= CompoundAnchors.TOP_Y && c.isAir(x, y, z); y++) c.set(x, y, z, ATTIC.at(x, y, z));
            }
        }
        Roofs.chimney(c, WX0, -6, CEIL, Palette.patches(SEED + 32, 2, "minecraft:bricks", 4, "minecraft:mud_bricks", 1), Family.STONE_BRICKS, false);
        Roofs.chimney(c, WX1, 8, CEIL, Palette.patches(SEED + 33, 2, "minecraft:bricks", 4, "minecraft:mud_bricks", 1), Family.STONE_BRICKS, false);
    }

    // --- the points --------------------------------------------------------------------------------------------------------------

    private static void points(Canvas c) {
        c.anchor(CompoundAnchors.ENTRY, 0, 0, RAIL_Z - 2);
        c.anchor(CompoundAnchors.HESS, 0, 0, 6);
        c.anchor(CompoundAnchors.ARENA_CENTRE, 0, 0, -4);
        c.anchor(CompoundAnchors.RITE, 0, 0, -18);
        c.anchor(CompoundAnchors.gateMin(CompoundAnchors.GATE_FRONT), -1, 0, WZ0);
        c.anchor(CompoundAnchors.gateMax(CompoundAnchors.GATE_FRONT), 1, 3, WZ0);
        int[][] agents = {{-6, 0, -7}, {6, 0, -7}, {-7, 0, 8}, {7, 0, 8}, {-5, 0, -13}, {5, 0, -13}};
        for (int n = 0; n < CompoundAnchors.AGENTS; n++) c.anchor(CompoundAnchors.agent(n), agents[n][0], agents[n][1], agents[n][2]);
    }
}
