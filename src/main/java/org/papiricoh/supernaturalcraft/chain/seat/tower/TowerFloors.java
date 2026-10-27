package org.papiricoh.supernaturalcraft.chain.seat.tower;

import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Furniture;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonAnchors;
import org.papiricoh.supernaturalcraft.entity.boss.dick.BoardroomGround;

import static org.papiricoh.supernaturalcraft.chain.seat.tower.TowerDraw.LAMP;
import static org.papiricoh.supernaturalcraft.chain.seat.tower.TowerDraw.LETTER;
import static org.papiricoh.supernaturalcraft.chain.seat.tower.TowerDraw.TZ;
import static org.papiricoh.supernaturalcraft.chain.seat.tower.TowerDraw.ZN;
import static org.papiricoh.supernaturalcraft.chain.seat.tower.TowerDraw.ZS;

/**
 * The fit-out of the playable floors (pure; drawn by {@link TowerDraw} after the shell, into the air it carved). Every floor
 * has the same bones — a strip of rooms along the north glass, a west block, an east block, a strip along the south glass west of
 * the stair core — and keeps the cells before the stair door ({@code x 4..9, z 5..7}) clear. Furniture stands in islands with
 * aisles between, so every corner can be walked to.
 */
final class TowerFloors {

    static final String GOO = "supernaturalcraft:leviathan_goo[dried=true]";

    private final TowerDraw t;
    private final Canvas c;
    private TowerDraw.Sec sec;
    private int y;
    private String room;
    private int guards;

    TowerFloors(TowerDraw t) {
        this.t = t;
        this.c = t.c;
    }

    void draw(SucroTower.Floor f) {
        sec = TowerDraw.sec(f.floorY());
        y = f.y();
        room = f.room();
        guards = 0;
        switch (room) {
            case "lobby" -> lobby();
            case "office_a" -> office(false);
            case "office_b" -> office(true);
            case "archive" -> archive();
            case "lab" -> lab();
            case "factory" -> factory();
            case "canteen" -> canteen();
            case "executive" -> executive();
            case "boardroom" -> boardroom();
            default -> throw new IllegalStateException(room);
        }
    }

    // --- helpers -------------------------------------------------------------------------------------------------------------

    /** Open interior floor of this storey (not the curtain wall, the core or a column). */
    private boolean free(int x, int z) {
        return sec.interior(x, z) && !TowerDraw.core(x, z) && (y >= SucroTower.BOARDROOM_FLOOR || !TowerDraw.column(x, z));
    }

    /** Whether a box of open floor (x0..x1, z0..z1) is still empty at feet level. */
    private boolean empty(int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!free(x, z)) return false;
                String s = c.world(x, y, z);
                if (!Kinds.air(s) && !St.path(s).endsWith("_carpet")) return false;
            }
        }
        return true;
    }

    /** Re-lays the storey's floor with {@code brush} (the recessed light grid kept). */
    private void floor(Brush brush) {
        for (int x = -sec.hx(); x <= sec.hx(); x++) {
            for (int z = ZN; z <= ZS; z++) {
                if (free(x, z)) c.set(x, y - 1, z, TowerDraw.lampCell(x, z) ? LAMP : brush.at(x, y - 1, z));
            }
        }
    }

    /** Carpet tiles (3 × 3) over every open cell: {@code a}, one tile in four {@code b}. */
    private void carpet(String a, String b) {
        for (int x = -sec.hx(); x <= sec.hx(); x++) {
            for (int z = ZN; z <= ZS; z++) {
                boolean accent = Noise.chance(Math.floorDiv(x, 3), y, Math.floorDiv(z - TZ, 3), t.s + 43, 0.25);
                if (free(x, z) && Kinds.air(c.world(x, y, z))) c.set(x, y, z, St.carpet(accent ? b : a));
            }
        }
    }

    private void guard(int x, int z) {
        c.anchor(DungeonAnchors.guard(room, ++guards), x, y, z);
    }

    /** A wall of glass panes from (x0, z0) to (x1, z1) (straight), floor to ceiling, framed every four cells, with doorways. */
    private void glassWall(int x0, int z0, int x1, int z1, String pane, int... doors) {
        int dx = Integer.signum(x1 - x0), dz = Integer.signum(z1 - z0), n = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        for (int i = 0; i <= n; i++) {
            int x = x0 + dx * i, z = z0 + dz * i;
            if (!free(x, z)) continue;
            boolean door = false;
            for (int d : doors) door |= (dx != 0 ? x : z) == d;
            for (int h = 0; h <= 3; h++) {
                if (door && h <= 1) c.carve(x, y + h, z);
                else c.set(x, y + h, z, h == 3 || i == 0 || i == n || i % 4 == 0 ? St.pane("minecraft:light_gray_stained_glass_pane") : St.pane(pane));
            }
        }
    }

    private void potted(int x, int z, String plant) {
        if (!free(x, z)) return;
        String here = c.world(x, y, z);
        if (Kinds.air(here) || St.path(here).endsWith("_carpet")) c.set(x, y, z, plant);
    }

    private static final String[] PLANTS = {"minecraft:potted_bamboo", "minecraft:potted_fern", "minecraft:potted_azalea_bush",
            "minecraft:potted_flowering_azalea_bush", "minecraft:potted_spruce_sapling"};

    private void plant(int x, int z) {
        potted(x, z, PLANTS[Noise.pick(x, y, z, t.s + 41, PLANTS.length)]);
    }

    /** Two sofas face to face across a pair of low tables, centred on (cx, cz), running east-west. */
    private void lounge(int cx, int cz, String sofa, String cloth) {
        if (!empty(cx - 3, cz - 2, cx + 3, cz + 2)) return;
        Furniture.sofa(c, cx + 1, y, cz - 1, Dir.SOUTH, 3, sofa, "minecraft:iron_trapdoor");
        Furniture.sofa(c, cx - 1, y, cz + 1, Dir.NORTH, 3, sofa, "minecraft:iron_trapdoor");
        Furniture.sideTable(c, cx - 1, y, cz, "minecraft:dark_oak_fence", cloth);
        Furniture.sideTable(c, cx + 1, y, cz, "minecraft:dark_oak_fence", cloth);
    }

    /** A 3 × 3 planter (polished rim, grass) with an azalea in it. */
    private void indoorPlanter(int cx, int cz) {
        if (!empty(cx - 1, cz - 1, cx + 1, cz + 1)) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                c.set(cx + dx, y, cz + dz, dx == 0 && dz == 0 ? "minecraft:grass_block" : "minecraft:polished_andesite");
            }
        }
        c.set(cx, y + 1, cz, Noise.chance(cx, y, cz, t.s + 51, 0.5) ? "minecraft:flowering_azalea" : "minecraft:azalea");
    }

    /** Rows of shelves three high along x (from x0 to x1, every {@code step} from z0 to z1), with a cross aisle at gx0..gx1. */
    private void stacks(int x0, int x1, int z0, int z1, int step, int gx0, int gx1) {
        for (int z = z0; z <= z1; z += step) {
            for (int x = x0; x <= x1; x++) {
                if (x >= gx0 && x <= gx1 || !free(x, z)) continue;
                for (int h = 0; h <= 2; h++) {
                    String s = Math.floorMod(x, 5) == 0 ? "minecraft:stripped_dark_oak_wood[axis=y]" : "minecraft:bookshelf";
                    c.set(x, y + h, z, s);
                }
            }
        }
    }

    /** A small SucroCorp mark (7 × 7) in a wall plane z, centred on x, its bottom row at y0. */
    private void smallMark(int cx, int y0, int z, String ring, String letter) {
        String[] g = {"XXX", "X..", "XXX", "..X", "XXX"};
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                double dx = i - 3, dy = j - 3, d = Math.sqrt(dx * dx + dy * dy);
                String st = null;
                if (d >= 2.6 && d < 3.6) st = ring;
                else if (i >= 2 && i <= 4 && j >= 1 && j <= 5 && g[j - 1].charAt(i - 2) == 'X') st = letter;
                if (st != null) c.set(cx - 3 + i, y0 + 6 - j, z, st);
            }
        }
    }

    // --- the lobby -----------------------------------------------------------------------------------------------------------

    /**
     * The double-height lobby: doors from the forecourt, a reception counter before a dark wall with the mark, a line of
     * turnstiles closing off the stair and the "lifts", a waiting lounge to the west, and the atrium to the north round a sculpture
     * of sugar cubes, among planters.
     */
    private void lobby() {
        floor((x, yy, z) -> Math.floorMod(x, 8) == 0 || Math.floorMod(z - TZ, 8) == 0 ? "minecraft:polished_andesite"
                : ((x + z) & 1) == 0 ? "minecraft:smooth_quartz" : "minecraft:polished_diorite");
        for (int x = -2; x <= 2; x++) for (int z = ZS - 3; z < ZS; z++) c.set(x, y, z, St.carpet("gray"));
        // Reception.
        for (int x = -5; x <= 5; x++) {
            c.set(x, y, 2, Math.abs(x) == 5 ? "minecraft:polished_blackstone" : "minecraft:yellow_terracotta");
            c.set(x, y + 1, 2, St.slabBottom("minecraft:smooth_quartz_slab"));
        }
        for (int x : new int[]{-3, 0, 3}) Furniture.desk(c, x, y, 1, Dir.SOUTH, "minecraft:polished_blackstone_slab", "minecraft:quartz_stairs", true);
        Brush wall = TowerKit.tri("minecraft:polished_blackstone", "minecraft:polished_deepslate");
        for (int x = -7; x <= 7; x++) for (int h = 0; h <= 8; h++) c.set(x, y + h, -3, wall.at(x, y + h, -3));
        smallMark(0, y + 1, -3, "minecraft:yellow_concrete", LETTER);
        // Security.
        for (int z = 5; z <= 13; z++) {
            if (!free(3, z)) continue;
            c.set(3, y, z, z % 2 == 1 ? St.wall("minecraft:polished_blackstone_wall") : St.gate("minecraft:dark_oak_fence_gate", Dir.EAST, true, false));
        }
        for (int x = 3; x <= 19; x++) if (free(x, 4)) c.set(x, y, 4, x == 3 || x == 19 ? St.wall("minecraft:polished_blackstone_wall") : St.pane("minecraft:glass_pane"));
        c.set(9, y, 6, "minecraft:polished_blackstone");
        c.set(9, y + 1, 6, St.trapdoor("minecraft:iron_trapdoor", Dir.EAST, false, true));
        Furniture.chair(c, 10, y, 6, Dir.WEST, "minecraft:quartz_stairs", null);
        // Waiting lounge, a bench along the glass.
        lounge(-14, 8, "minecraft:quartz_stairs", "yellow");
        lounge(-14, -6, "minecraft:quartz_stairs", "black");
        for (int x = -18; x <= -8; x++) if (free(x, ZS - 1) && x % 4 != 0) c.set(x, y, ZS - 1, St.slabBottom("minecraft:smooth_quartz_slab"));
        // The atrium.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int h = 0; h <= 2; h++) {
                    if (h == 2 && Math.abs(dx) + Math.abs(dz) == 2) continue;
                    c.set(dx, y + h, -15 + dz, ((dx + dz + h) & 1) == 0 ? "minecraft:white_concrete" : "minecraft:smooth_quartz");
                }
            }
        }
        c.set(0, y + 3, -15, "minecraft:yellow_concrete");
        for (Dir d : Dir.HORIZONTAL) {
            for (int k = -1; k <= 1; k++) {
                int bx = d.dx * 4 + (d.dz != 0 ? k : 0), bz = -15 + d.dz * 4 + (d.dx != 0 ? k : 0);
                c.set(bx, y, bz, St.slabBottom("minecraft:dark_oak_slab"));
            }
        }
        for (int[] p : new int[][]{{-14, -20}, {14, -20}, {-6, -22}, {6, -22}, {14, -10}, {14, -3}}) indoorPlanter(p[0], p[1]);
        for (int[] p : new int[][]{{-18, 4}, {-18, 12}, {-7, 12}, {-7, 4}, {-18, -12}, {18, -12}, {-18, -1}, {18, 1}}) plant(p[0], p[1]);
        guard(-3, 7);
        guard(8, 6);
        guard(-10, 1);
        guard(6, -12);
    }

    // --- offices -------------------------------------------------------------------------------------------------------------

    /**
     * Open-plan offices: managers' glass offices along the north, desk clusters filling the middle, a meeting room in the
     * north-east, a kitchenette on the east glass and a break-out lounge along the south.
     */
    private void office(boolean second) {
        carpet(second ? "light_gray" : "gray", second ? "cyan" : "light_gray");
        String pane = second ? "minecraft:light_blue_stained_glass_pane" : "minecraft:gray_stained_glass_pane";
        glassWall(-19, ZN + 6, 19, ZN + 6, pane, -11, 0, 11);
        glassWall(-6, ZN + 1, -6, ZN + 5, pane);
        glassWall(5, ZN + 1, 5, ZN + 5, pane);
        for (int cx : new int[]{-11, 0, 11}) {
            Furniture.desk(c, cx, y, ZN + 3, Dir.SOUTH, "minecraft:dark_oak_slab", "minecraft:dark_oak_stairs", true);
            Furniture.filing(c, cx + 3, y, ZN + 1, Dir.SOUTH, "minecraft:light_gray_concrete");
            plant(cx - 3, ZN + 1);
            Furniture.chair(c, cx - 2, y, ZN + 4, Dir.NORTH, "minecraft:spruce_stairs", null);
        }
        // Meeting room.
        glassWall(8, -18, 8, -8, pane, -13);
        glassWall(8, -8, 19, -8, pane);
        Furniture.diningSet(c, 12, -14, 16, -13, y, "minecraft:dark_oak_slab", "minecraft:spruce_stairs");
        for (int z = -15; z <= -12; z++) {
            c.set(19, y + 1, z, "minecraft:black_concrete");
            c.set(19, y + 2, z, "minecraft:black_concrete");
        }
        // Desk clusters.
        for (int cx = -17; cx <= 4; cx += 7) {
            for (int cz = -17; cz <= 1; cz += 6) cluster(cx, cz, second, pane);
        }
        cluster(10, -5, second, pane);
        // Kitchenette, copier, break-out lounge.
        Furniture.kitchen(c, 19, y, -6, Dir.SOUTH, 6, Dir.WEST, "minecraft:white_concrete", "minecraft:birch_trapdoor", false);
        c.set(17, y, 2, "minecraft:white_concrete");
        c.set(17, y + 1, 2, St.of("minecraft:heavy_weighted_pressure_plate", "power", "0"));
        lounge(-12, 9, second ? "minecraft:spruce_stairs" : "minecraft:quartz_stairs", "cyan");
        lounge(-3, 9, second ? "minecraft:spruce_stairs" : "minecraft:quartz_stairs", "orange");
        for (int[] p : new int[][]{{-18, 6}, {2, 12}, {-9, 12}, {18, 6}, {4, -19}, {-19, -19}}) plant(p[0], p[1]);
        guard(-5, -12);
        guard(1, -6);
        guard(-13, -1);
    }

    /** Six desks at (cx..cx+2, cz..cz+1), a row facing each way with monitors, glass panes at the ends. */
    private void cluster(int cx, int cz, boolean turned, String pane) {
        if (!empty(cx - 1, cz - 1, cx + 3, cz + 2)) return;
        String chair = turned ? "minecraft:dark_oak_stairs" : "minecraft:spruce_stairs";
        for (int i = 0; i < 3; i++) {
            Furniture.desk(c, cx + i, y, cz, Dir.SOUTH, "minecraft:birch_slab", chair, true);
            Furniture.desk(c, cx + i, y, cz + 1, Dir.NORTH, "minecraft:birch_slab", chair, i != 1);
        }
        for (int z = cz; z <= cz + 1; z++) {
            for (int x : new int[]{cx - 1, cx + 3}) {
                c.set(x, y, z, St.pane(pane));
                c.set(x, y + 1, z, St.pane(pane));
            }
        }
    }

    // --- the archive and the vault -------------------------------------------------------------------------------------------

    /** Records: stacks of shelves west and south of the vault, filing cabinets and reading desks along the south glass. */
    private void archive() {
        carpet("gray", "brown");
        stacks(-18, -2, ZN + 3, 4, 3, -11, -9);
        stacks(5, 18, -9, 0, 3, 11, 12);
        for (int x = -18; x <= -9; x++) if (free(x, ZS - 1)) Furniture.filing(c, x, y, ZS - 1, Dir.NORTH, "minecraft:light_gray_concrete");
        for (int x = -6; x <= 2; x += 4) Furniture.desk(c, x, y, 8, Dir.NORTH, "minecraft:dark_oak_slab", "minecraft:dark_oak_stairs", false);
        // The vault: steel walls, its door open on the west, bullion inside.
        int x0 = 4, x1 = 16, z0 = ZN + 2, z1 = -12;
        Brush steel = TowerKit.tri("minecraft:iron_block", "minecraft:polished_deepslate");
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (x != x0 && x != x1 && z != z0 && z != z1) {
                    c.set(x, y - 1, z, ((x + z) & 1) == 0 ? "minecraft:polished_deepslate" : "minecraft:deepslate_tiles");
                    if (St.path(c.world(x, y, z)).endsWith("_carpet")) c.carve(x, y, z);
                    continue;
                }
                for (int h = 0; h <= 3; h++) c.set(x, y + h, z, steel.at(x, y + h, z));
            }
        }
        int door = (z0 + z1) / 2;
        c.setPair(x0, y, door, St.door("minecraft:iron_door", Dir.WEST, false, true));
        c.setPair(x0, y, door + 1, St.door("minecraft:iron_door", Dir.WEST, true, true));
        for (int h = 0; h <= 2; h++) {
            c.set(x0 - 1, y + h, door - 1, "minecraft:iron_block");
            c.set(x0 - 1, y + h, door + 2, "minecraft:iron_block");
        }
        c.set(x0 - 1, y + 2, door, "minecraft:iron_block");
        c.set(x0 - 1, y + 2, door + 1, "minecraft:iron_block");
        for (int x = 9; x <= 12; x++) {
            c.set(x, y, door, x % 2 == 0 ? "minecraft:gold_block" : "minecraft:raw_gold_block");
            if (x == 10 || x == 11) c.set(x, y + 1, door, "minecraft:gold_block");
        }
        for (int z = z0 + 3; z <= z1 - 2; z++) for (int h = 0; h <= 2; h++) c.set(x1 - 1, y + h, z, h == 1 ? "minecraft:chiseled_stone_bricks" : "minecraft:iron_block");
        int n = SucroTower.vaults(t.seed);
        int[][] spots = {{x0 + 2, z0 + 1}, {x0 + 5, z0 + 1}, {x0 + 8, z0 + 1}, {x0 + 11, z0 + 1}, {x1 - 2, z0 + 4}, {x1 - 2, z1 - 1}};
        for (int i = 0; i < n; i++) c.anchor(DungeonAnchors.vault(i), spots[i][0], y, spots[i][1]);
        for (int[] p : new int[][]{{-1, 12}, {18, 4}, {-18, 6}}) plant(p[0], p[1]);
        guard(-10, -14);
        guard(0, 4);
        guard(11, -6);
    }

    // --- the laboratory ------------------------------------------------------------------------------------------------------

    /**
     * The laboratory where the black stuff is studied: three rows of vats, a containment tank watched from a bank of monitors,
     * holding cells with examination chairs, benches of instruments along the south.
     */
    private void lab() {
        floor(TowerKit.checker("minecraft:white_concrete", "minecraft:smooth_quartz"));
        for (int vz = -21; vz <= -7; vz += 7) {
            for (int vx = -16; vx <= -1; vx += 5) vat(vx, vz);
        }
        // Containment tank and its monitors.
        for (int x = 8; x <= 14; x++) {
            for (int z = -10; z <= -4; z++) {
                boolean rim = x == 8 || x == 14 || z == -10 || z == -4;
                for (int h = 0; h <= 3; h++) {
                    String st;
                    if (rim) st = (x == 8 || x == 14) && (z == -10 || z == -4) || h == 3 ? "minecraft:iron_block" : "minecraft:light_gray_stained_glass";
                    else st = h <= 1 ? "minecraft:black_concrete" : h == 2 ? GOO : "minecraft:waxed_copper_grate";
                    c.set(x, y + h, z, st);
                }
            }
        }
        for (int x = 9; x <= 13; x += 2) Furniture.desk(c, x, y, -1, Dir.NORTH, "minecraft:smooth_quartz_slab", "minecraft:quartz_stairs", true);
        // Holding cells.
        int z0 = ZN + 1, z1 = -14;
        for (int x = 6; x <= 18; x++) {
            if (!free(x, z1)) continue;
            boolean gap = x == 8 || x == 12 || x == 16;
            for (int h = 0; h <= 3; h++) {
                if (gap && h <= 1) c.carve(x, y + h, z1);
                else c.set(x, y + h, z1, St.bars());
            }
        }
        for (int x : new int[]{6, 10, 14}) {
            for (int z = z0; z < z1; z++) for (int h = 0; h <= 3; h++) if (free(x, z)) c.set(x, y + h, z, St.bars());
        }
        for (int cx : new int[]{8, 12, 16}) Furniture.medicalChair(c, cx, y, z0 + 3, Dir.SOUTH);
        c.set(15, y, z0 + 1, GOO);
        c.set(9, y, z0 + 1, GOO);
        // Benches with instruments.
        for (int z : new int[]{2, 8}) {
            for (int x = -17; x <= -2; x++) {
                if (x == -10 || !free(x, z)) continue;
                c.set(x, y, z, x % 4 == 0 ? "minecraft:light_gray_concrete" : "minecraft:white_concrete");
                double r = Noise.hash01(x, y, z, t.s + 61);
                String top = r < 0.2 ? St.of("minecraft:end_rod", "facing", "up") : r < 0.35 ? St.candle("white", 1 + Noise.pick(x, y, z, 62, 3), true)
                        : r < 0.5 ? "minecraft:potted_crimson_fungus" : r < 0.6 ? St.of("minecraft:lightning_rod", "facing", "up") : null;
                if (top != null) c.set(x, y + 1, z, top);
                if (z == 8 && x % 3 == 0 && free(x, z + 1)) Furniture.chair(c, x, y, z + 1, Dir.NORTH, "minecraft:quartz_stairs", null);
            }
        }
        for (int x = 16; x <= 18; x++) for (int z = 0; z <= 3; z++) if (free(x, z)) Furniture.cabinet(c, x, y, z, Dir.WEST, "minecraft:white_concrete", null, false);
        for (int[] p : new int[][]{{-18, 12}, {2, 12}, {18, -4}}) potted(p[0], p[1], "minecraft:potted_crimson_fungus");
        guard(-8, -10);
        guard(-12, 5);
        guard(12, -12);
    }

    /** A vat: glass round a column of the black stuff, its top dried white, an iron frame and a grate lid. */
    private void vat(int vx, int vz) {
        if (!empty(vx - 1, vz - 1, vx + 1, vz + 1)) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = vx + dx, z = vz + dz;
                boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1, mid = dx == 0 && dz == 0;
                for (int h = 0; h <= 3; h++) {
                    String st;
                    if (mid) st = h <= 1 ? "minecraft:black_concrete" : h == 2 ? GOO : "minecraft:waxed_copper_grate";
                    else if (corner || h == 3) st = "minecraft:iron_block";
                    else st = "minecraft:light_gray_stained_glass";
                    c.set(x, y + h, z, st);
                }
            }
        }
    }

    // --- the snack line ------------------------------------------------------------------------------------------------------

    /**
     * SucroCorp's snack line: cookers feeding a belt of cakes under three presses, a packing line of boxed snacks, sugar silos,
     * pallets ready to ship, and the control desks.
     */
    private void factory() {
        floor((x, yy, z) -> z == -14 || z == -7 || z == -3 || z == 3 ? "minecraft:yellow_concrete"
                : ((x + z) & 1) == 0 ? "minecraft:gray_concrete" : "minecraft:polished_andesite");
        belt(-15, 12, -11, true);
        for (int px : new int[]{-9, -1, 7}) press(px, -11);
        for (int[] p : new int[][]{{-16, -18}, {-16, -4}}) copperTank(p[0], p[1]);
        for (int x = -17; x <= -15; x++) for (int z = -12; z <= -9; z++) c.set(x, y + 3, z, St.chain("x"));
        vat(-12, -18);
        // The packing line.
        belt(-9, 12, -1, false);
        for (int px : new int[]{-3, 5}) press(px, -1);
        // Silos, sacks, pallets.
        for (int[] p : new int[][]{{-14, 6}, {-14, 10}}) silo(p[0], p[1]);
        for (int x = -11; x <= -8; x++) for (int h = 0; h <= (x % 2 == 0 ? 1 : 0); h++) c.set(x, y + h, -4, "minecraft:white_wool");
        for (int px = 12; px <= 16; px += 2) {
            for (int pz = -22; pz <= -16; pz += 3) pallet(px, pz);
        }
        for (int pz = -6; pz <= 0; pz += 3) pallet(15, pz);
        // The control desks.
        for (int x = -6; x <= 0; x += 2) Furniture.desk(c, x, y, 8, Dir.NORTH, "minecraft:polished_blackstone_slab", "minecraft:polished_blackstone_stairs", true);
        c.set(-9, y, 10, "minecraft:stonecutter[facing=north]");
        c.set(-8, y, 10, "minecraft:grindstone[face=floor,facing=north]");
        for (int[] p : new int[][]{{2, 12}, {-11, 12}}) plant(p[0], p[1]);
        guard(-13, -7);
        guard(2, -14);
        guard(9, -6);
        guard(-3, 5);
    }

    /** A belt two wide along x at z, z+1: black carpet on smooth stone, with cakes (or boxed snacks) riding on it. */
    private void belt(int x0, int x1, int z, boolean cakes) {
        for (int x = x0; x <= x1; x++) {
            for (int zz = z; zz <= z + 1; zz++) {
                if (!free(x, zz)) continue;
                c.set(x, y, zz, "minecraft:smooth_stone");
                boolean item = Math.floorMod(x, 4) == 1 && zz == z;
                c.set(x, y + 1, zz, item ? (cakes ? "minecraft:cake" : St.carpet("yellow")) : St.carpet("black"));
            }
        }
    }

    /** A press over a belt at (px, z..z+1): iron posts, pistons pushing down, an observer on each. */
    private void press(int px, int z) {
        for (int h = 0; h <= 2; h++) {
            c.set(px, y + h, z - 1, "minecraft:iron_block");
            c.set(px, y + h, z + 2, "minecraft:iron_block");
        }
        for (int zz = z; zz <= z + 1; zz++) {
            c.set(px, y + 2, zz, St.of("minecraft:piston", "facing", "down", "extended", "false"));
            c.set(px, y + 3, zz, St.of("minecraft:observer", "facing", "up", "powered", "false"));
        }
    }

    private void copperTank(int cx, int cz) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = cx + dx, z = cz + dz;
                for (int h = 0; h <= 2; h++) c.set(x, y + h, z, (dx == 0 || dz == 0) && h == 1 ? "minecraft:waxed_copper_grate" : "minecraft:waxed_cut_copper");
                c.set(x, y + 3, z, dx == 0 && dz == 0 ? "minecraft:cauldron" : St.slabBottom("minecraft:waxed_cut_copper_slab"));
            }
        }
    }

    /** A sugar silo: a round-cornered column of white with a yellow band, floor to ceiling. */
    private void silo(int cx, int cz) {
        if (!empty(cx - 1, cz - 1, cx + 1, cz + 1)) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int h = 0; h <= 3; h++) c.set(cx + dx, y + h, cz + dz, h == 2 ? "minecraft:yellow_concrete" : "minecraft:white_concrete");
            }
        }
    }

    /** A pallet of boxed snacks (2 × 2, one or two high) where the floor is free. */
    private void pallet(int px, int pz) {
        if (!empty(px, pz, px + 1, pz + 1)) return;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                int stack = 1 + Noise.pick(px + dx, y, pz + dz, t.s + 71, 2);
                for (int h = 0; h < stack; h++) c.set(px + dx, y + h, pz + dz, (px + pz + h) % 3 == 0 ? "minecraft:orange_wool" : "minecraft:yellow_wool");
            }
        }
    }

    // --- the canteen ---------------------------------------------------------------------------------------------------------

    /** The staff canteen: a kitchen and a serving counter along the north, rows of tables, snack machines, a lounge by the glass. */
    private void canteen() {
        floor(TowerKit.checker("minecraft:white_terracotta", "minecraft:light_gray_terracotta"));
        Furniture.kitchen(c, -12, y, ZN + 2, Dir.EAST, 17, Dir.SOUTH, "minecraft:smooth_stone", "minecraft:iron_trapdoor", false);
        Furniture.bar(c, -12, y, ZN + 5, Dir.EAST, 17, Dir.SOUTH, "minecraft:white_concrete", "minecraft:smooth_quartz_stairs", null);
        for (int x = -12; x <= 4; x += 4) c.set(x, y + 1, ZN + 5, "minecraft:cake");
        for (int x0 = -16; x0 <= 12; x0 += 7) {
            for (int z0 = -14; z0 <= 4; z0 += 6) {
                if (empty(x0, z0 - 1, x0 + 3, z0 + 1) && !(x0 + 3 >= 4 && x0 <= 9 && z0 + 1 >= 5)) {
                    Furniture.diningSet(c, x0, z0, x0 + 3, z0, y, "minecraft:birch_slab", "minecraft:birch_stairs");
                }
            }
        }
        String[] colors = {"minecraft:red_concrete", "minecraft:yellow_concrete", "minecraft:orange_concrete", "minecraft:red_concrete"};
        for (int i = 0; i < colors.length; i++) {
            int z = -6 + 2 * i, x = -sec.hx() + 1;
            if (!free(x, z)) continue;
            c.set(x, y, z, colors[i]);
            c.set(x, y + 1, z, "minecraft:light_blue_stained_glass");
            c.set(x, y + 2, z, colors[i]);
            c.set(x + 1, y + 1, z, St.button("minecraft:stone_button", "wall", Dir.EAST));
        }
        lounge(-12, 10, "minecraft:birch_stairs", "yellow");
        lounge(-3, 10, "minecraft:birch_stairs", "orange");
        for (int[] p : new int[][]{{-17, 12}, {2, 12}, {17, -12}, {17, 2}, {-17, -18}}) plant(p[0], p[1]);
        guard(-5, -17);
        guard(10, -4);
        guard(-8, 7);
    }

    // --- the executive floor -------------------------------------------------------------------------------------------------

    /**
     * The executives: two corner offices behind dark glass (the Commanders' posts), a lounge between them, a bar, a private
     * dining table and a library corner.
     */
    private void executive() {
        floor(Palette.boards(true, Brush.of("minecraft:dark_oak_planks"), Brush.of("minecraft:spruce_planks"), 4));
        for (int side : new int[]{-1, 1}) {
            int cx = side * 13, wallX = side * 7;
            glassWall(wallX, ZN + 1, wallX, -11, "minecraft:black_stained_glass_pane");
            glassWall(side * 18, -11, wallX, -11, "minecraft:black_stained_glass_pane", side * 11, side * 12);
            for (int x = cx - 5; x <= cx + 5; x++) {
                for (int z = ZN + 1; z <= -12; z++) {
                    boolean even = ((Math.floorDiv(x, 3) + Math.floorDiv(z, 3)) & 1) == 0;
                    if (free(x, z) && Kinds.air(c.world(x, y, z))) c.set(x, y, z, St.carpet(even ? "black" : "gray"));
                }
            }
            for (int x = cx - 1; x <= cx + 1; x++) c.set(x, y, -19, St.slabTop("minecraft:dark_oak_slab"));
            c.set(cx - 1, y + 1, -19, St.lantern(false));
            c.set(cx + 1, y + 1, -19, St.trapdoor("minecraft:iron_trapdoor", Dir.NORTH, false, true));
            Furniture.chair(c, cx, y, -20, Dir.SOUTH, "minecraft:polished_blackstone_stairs", "minecraft:dark_oak_trapdoor");
            for (int x = cx - 2; x <= cx + 2; x++) for (int h = 0; h <= 2; h++) if (free(x, ZN + 1)) c.set(x, y + h, ZN + 1, "minecraft:bookshelf");
            Furniture.sofa(c, side < 0 ? cx + 1 : cx - 1, y, -13, Dir.NORTH, 3, "minecraft:dark_oak_stairs", "minecraft:dark_oak_trapdoor");
            plant(cx + side * 4, -21);
            plant(cx - side * 4, -21);
            c.anchor(DungeonAnchors.COMMANDERS.get(side < 0 ? 0 : 1), cx, y, -17);
        }
        // Lounge, bar, dining, library.
        Furniture.sofa(c, 1, y, -21, Dir.SOUTH, 4, "minecraft:quartz_stairs", "minecraft:iron_trapdoor");
        Furniture.sofa(c, -2, y, -17, Dir.NORTH, 4, "minecraft:quartz_stairs", "minecraft:iron_trapdoor");
        Furniture.sideTable(c, -1, y, -19, "minecraft:dark_oak_fence", "black");
        Furniture.sideTable(c, 0, y, -19, "minecraft:dark_oak_fence", "black");
        Furniture.bar(c, -5, y, -4, Dir.EAST, 9, Dir.SOUTH, "minecraft:polished_blackstone", "minecraft:dark_oak_stairs", "minecraft:dark_oak_fence");
        for (int x = -5; x <= 3; x++) c.set(x, y + 2, -6, St.trapdoor("minecraft:dark_oak_trapdoor", Dir.SOUTH, true, false));
        Furniture.diningSet(c, -16, 2, -9, 3, y, "minecraft:dark_oak_slab", "minecraft:dark_oak_stairs");
        for (int x = -15; x <= -10; x += 5) c.set(x, y + 1, 2, St.candle("white", 3, true));
        stacks(12, 18, -7, -1, 3, 99, 99);
        lounge(14, 4, "minecraft:dark_oak_stairs", "red");
        lounge(-4, 10, "minecraft:quartz_stairs", "black");
        for (int[] p : new int[][]{{-14, -5}, {6, 1}, {-2, -12}}) indoorPlanter(p[0], p[1]);
        for (int[] p : new int[][]{{-6, -24}, {6, -24}, {-17, 8}, {17, -9}, {-17, -8}, {2, 12}}) plant(p[0], p[1]);
        guard(0, -8);
        guard(-12, -8);
        guard(10, -9);
        guard(-6, 6);
    }

    // --- the boardroom -------------------------------------------------------------------------------------------------------

    /**
     * Dick Roman's boardroom, the arena of his first fight: {@code BoardroomGround}'s floor (deepslate tiles, a quartz ring, the
     * red runner, a mark at the centre) over a clear disc round {@link SucroTower#BOARDROOM}, his table where that plan puts it,
     * his own high-backed place at its head, glass on three sides. Open floor cells are {@code BoardroomGround.FLOOR_AIR}.
     */
    private void boardroom() {
        int bx = SucroTower.BOARDROOM_X, bz = SucroTower.BOARDROOM_Z, r = SucroTower.BOARDROOM_RADIUS;
        for (int x = -sec.hx(); x <= sec.hx(); x++) {
            for (int z = ZN; z <= ZS; z++) {
                if (!free(x, z)) continue;
                int dx = x - bx, dz = z - bz, d2 = dx * dx + dz * dz;
                String st;
                if (d2 <= r * r && d2 > (r - 1) * (r - 1)) st = "minecraft:smooth_quartz";
                else if (Math.abs(dx) <= 1 && dz > BoardroomGround.TABLE_Z && dz < -2) st = "minecraft:red_wool";
                else if (TowerDraw.lampCell(x, z) && d2 > 9) st = LAMP;
                else st = (Math.abs(dx) + Math.abs(dz)) % 3 == 0 ? "minecraft:polished_deepslate" : "minecraft:deepslate_tiles";
                c.set(x, y - 1, z, st);
                c.set(x, y, z, BoardroomGround.FLOOR_AIR);
            }
        }
        for (int i = 0; i < TowerKit.LOGO; i++) {
            for (int j = 0; j < TowerKit.LOGO; j++) {
                int m = TowerKit.logo(i, j);
                if (m != 0) c.set(bx + i - 5, y - 1, bz + j - 5, m == 1 ? "minecraft:gold_block" : LETTER);
            }
        }
        // The table, exactly as BoardroomGround lays it (dark oak slabs on fences), chairs down both sides.
        int tz = bz + BoardroomGround.TABLE_Z, half = BoardroomGround.TABLE_HALF;
        for (int dx = -half; dx <= half; dx++) {
            for (int z = tz - 1; z <= tz; z++) {
                if (Math.abs(dx) == half || dx == 0) c.set(bx + dx, y, z, St.fence("minecraft:dark_oak_fence"));
                c.set(bx + dx, y + 1, z, St.slabBottom("minecraft:dark_oak_slab"));
            }
            if (dx % 2 == 0) {
                if (dx != 0) c.set(bx + dx, y, tz - 2, St.stairs("minecraft:dark_oak_stairs", Dir.NORTH, false));
                c.set(bx + dx, y, tz + 1, St.stairs("minecraft:dark_oak_stairs", Dir.SOUTH, false));
            }
        }
        // Dick's place at the head: a high black back with a gold crest, arms either side; he stands in it facing the room.
        int[] seat = SucroTower.dickSeat();
        c.set(seat[0], y, seat[2] - 1, "minecraft:polished_blackstone");
        c.set(seat[0], y + 1, seat[2] - 1, "minecraft:polished_blackstone");
        c.set(seat[0], y + 2, seat[2] - 1, "minecraft:gold_block");
        for (int side : new int[]{-1, 1}) c.set(seat[0] + side, y, seat[2], St.slabBottom("minecraft:polished_blackstone_slab"));
        c.anchor(SucroTower.DICK_SEAT, seat[0], seat[1], seat[2]);
        c.anchor(DungeonAnchors.LORD, seat[0], seat[1], seat[2]);
        c.anchor(SucroTower.BOARDROOM, bx, y, bz);
        c.anchor(DungeonAnchors.THRONE_ROOM, bx, y, bz);
        // Light bars over the table.
        int ceiling = y + SucroTower.BOARDROOM_HEADROOM - 1;
        for (int dx = -half; dx <= half; dx += 2) for (int z = tz - 1; z <= tz; z++) c.set(bx + dx, ceiling, z, St.of("minecraft:end_rod", "facing", "down"));
        // Outside the disc: a waiting corner by the door, credenzas and plants in the far corners.
        for (int x = -12; x <= 0; x += 6) {
            Furniture.sofa(c, x, y, ZS - 1, Dir.NORTH, 3, "minecraft:quartz_stairs", "minecraft:iron_trapdoor");
            Furniture.sideTable(c, x + 1, y, ZS - 3, "minecraft:dark_oak_fence", "black");
        }
        for (int side : new int[]{-1, 1}) {
            for (int k = 0; k < 3; k++) {
                int x = side * (12 + k), z = ZN + 3 + k;
                int dx = x - bx, dz = z - bz;
                if (dx * dx + dz * dz <= r * r || !free(x, z)) continue;
                c.set(x, y, z, "minecraft:polished_blackstone");
                c.set(x, y + 1, z, k == 1 ? St.lantern(false) : St.slabBottom("minecraft:smooth_quartz_slab"));
            }
        }
        for (int[] p : new int[][]{{-15, ZN + 6}, {15, ZN + 6}, {-16, ZS - 2}, {-16, 0}, {16, 0}}) {
            int dx = p[0] - bx, dz = p[1] - bz;
            if (dx * dx + dz * dz > r * r) plant(p[0], p[1]);
        }
    }
}
