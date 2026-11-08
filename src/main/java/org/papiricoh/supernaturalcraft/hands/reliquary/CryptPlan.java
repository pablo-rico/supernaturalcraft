package org.papiricoh.supernaturalcraft.hands.reliquary;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.buildkit.Walls;
import org.papiricoh.supernaturalcraft.buildkit.Wood;

/**
 * A ruined church over a crypt (v0.42), keeper of the Apostles' Chalice. Above: a churchyard of mossy headstones inside a
 * broken iron fence, and the church itself, its walls fallen to half their height in places, buttressed, its windows empty, the
 * nave open to the sky but for a few rafters, the chancel still roofed. Pews in rows, the altar on its step. A stair down the
 * east aisle leads under the floor: an ossuary hall with bones and candles in its niches, a low gallery, and the reliquary
 * chamber with its coffins and the Chalice on its altar. Plan space: the door faces -z, the chancel is to the south.
 */
final class CryptPlan {

    static final int SEED = 42_300;
    static final int X0 = -5, X1 = 5, Z0 = -6, Z1 = 12, TOP = 7;
    /** The crypt's feet row. */
    static final int DEEP = -7;

    static final String BRICK = "supernaturalcraft:crypt_bricks";
    static final Brush WALL = Palette.mix(SEED + 1, BRICK, 4, "minecraft:stone_bricks", 3, "minecraft:mossy_stone_bricks", 2,
            "minecraft:cracked_stone_bricks", 2, "minecraft:cobblestone", 1);
    static final Brush FLOOR = Palette.patches2(SEED + 2, 2, "minecraft:stone_bricks", 3, "minecraft:cracked_stone_bricks", 2,
            "minecraft:polished_andesite", 2, "minecraft:mossy_stone_bricks", 2, "minecraft:cobblestone", 1);
    static final Brush YARD = Palette.patches2(SEED + 3, 3, "minecraft:grass_block", 7, "minecraft:podzol", 2, "minecraft:coarse_dirt", 2,
            "minecraft:moss_block", 1);
    static final Brush PATH = Palette.patches2(SEED + 4, 1.5, "minecraft:dirt_path", 4, "minecraft:gravel", 2, "minecraft:cobblestone", 1);
    static final Brush CRYPT = Palette.mix(SEED + 5, BRICK, 5, "minecraft:deepslate_bricks", 2, "minecraft:cracked_deepslate_bricks", 2,
            "minecraft:polished_deepslate", 1, "minecraft:tuff_bricks", 1);
    static final Brush CRYPT_FLOOR = Palette.mix(SEED + 6, "minecraft:deepslate_tiles", 3, BRICK, 3, "minecraft:cracked_deepslate_tiles", 2,
            "minecraft:polished_deepslate", 2);

    private CryptPlan() {
    }

    /** How high a wall column still stands (the north-east corner fell hardest; the chancel and the facade held). */
    static int wallTop(int x, int z) {
        if (z >= 8 || z == Z0) return TOP;
        double wear = Noise.fbm2(x, z, 6, 2, SEED + 7) * 0.5 + 0.5;
        double corner = Math.max(0, (x + z * 0.3) / 8.0);
        return Math.max(1, TOP - (int) Math.round(wear * 3 + corner * 2.5));
    }

    static Canvas drawn() {
        Canvas c = new Canvas("reliquary_church_crypt_north");
        yard(c);
        church(c);
        crypt(c);
        ReliquaryKit.wrap(c, new Box(-8, -9, -6, 8, -2, 18), CRYPT, CRYPT_FLOOR, CRYPT);
        cryptDecor(c);
        c.anchor(ReliquaryAnchors.ENTRY, 0, 0, -14);
        return c;
    }

    private static void yard(Canvas c) {
        for (int x = -13; x <= 13; x++) {
            for (int z = -15; z <= 17; z++) {
                boolean path = Math.abs(x + Noise.jitter(x, z, SEED + 8, 1)) <= 1 && z < Z0;
                c.set(x, -1, z, path ? PATH.at(x, -1, z) : YARD.at(x, -1, z));
                c.set(x, -2, z, "minecraft:dirt");
            }
        }
        // The fence, broken in places, open at the gate.
        for (int i = -13; i <= 13; i++) {
            for (int[] p : new int[][]{{i, -15}, {-13, i}, {13, i}}) {
                if (p[1] == -15 && Math.abs(p[0]) <= 1) continue;
                if (p[1] > 17) continue;
                if (Noise.chance(p[0], 0, p[1], SEED + 9, 0.3)) continue;
                c.set(p[0], 0, p[1], St.bars());
            }
        }
        // Headstones in rows either side, sunk and leaning.
        for (int s : new int[]{-1, 1}) {
            for (int z = -11; z <= 14; z += 3) {
                for (int x = 8; x <= 11; x += 3) {
                    int gx = s * x;
                    if (Noise.chance(gx, 0, z, SEED + 10, 0.2)) continue;
                    String stone = switch (Noise.pick(gx, 0, z, SEED + 11, 3)) {
                        case 0 -> St.wall("minecraft:mossy_stone_brick_wall");
                        case 1 -> St.wall("minecraft:cobblestone_wall");
                        default -> "minecraft:chiseled_stone_bricks";
                    };
                    c.set(gx, 0, z, stone);
                    c.set(gx, -1, z + 1, "minecraft:rooted_dirt");
                }
            }
        }
        // Dead bushes and long grass between the stones.
        for (int x = -12; x <= 12; x++) {
            for (int z = -14; z <= 16; z++) {
                if (Math.abs(x) <= 6 && z >= -8 && z <= 13 || c.has(x, 0, z)) continue;
                if (!St.path(c.world(x, -1, z)).equals("grass_block")) continue;
                if (Noise.chance(x, 0, z, SEED + 12, 0.05)) c.set(x, 0, z, "minecraft:dead_bush");
                else if (Noise.chance(x, 0, z, SEED + 13, 0.08)) c.set(x, 0, z, "minecraft:short_grass");
            }
        }
    }

    private static void church(Canvas c) {
        Box foot = new Box(X0, 0, Z0, X1, 0, Z1);
        // The floor, the walls as far as they still stand.
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                c.set(x, -1, z, FLOOR.at(x, -1, z));
                boolean edge = x == X0 || x == X1 || z == Z0 || z == Z1;
                if (!edge) {
                    for (int y = 0; y <= TOP + 4; y++) c.carve(x, y, z);
                    continue;
                }
                int top = wallTop(x, z);
                for (int y = 0; y <= top; y++) c.set(x, y, z, y == 0 ? "minecraft:mossy_cobblestone" : WALL.at(x, y, z));
            }
        }
        // The facade's gable and its rose window.
        for (int y = TOP + 1; y <= TOP + 4; y++) {
            for (int x = -(TOP + 4 - y); x <= TOP + 4 - y; x++) c.set(x, y, Z0, WALL.at(x, y, Z0));
        }
        c.set(0, TOP + 4, Z0, "minecraft:chiseled_stone_bricks");
        for (int[] p : new int[][]{{0, 5}, {-1, 5}, {1, 5}, {0, 4}, {0, 6}}) c.set(p[0], p[1], Z0, St.pane("minecraft:gray_stained_glass_pane"));
        // The door, its lintel.
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) c.carve(x, y, Z0);
        c.set(-1, 3, Z0, "minecraft:chiseled_stone_bricks");
        c.set(0, 3, Z0, BRICK);
        c.set(1, 3, Z0, "minecraft:chiseled_stone_bricks");
        // Windows: tall and empty, a few still barred.
        for (int z : new int[]{-2, 2, 6, 10}) {
            for (int x : new int[]{X0, X1}) {
                for (int y = 2; y <= 4; y++) {
                    if (y > wallTop(x, z)) break;
                    if (Noise.chance(x, y, z, SEED + 15, 0.25)) c.set(x, y, z, St.bars());
                    else c.carve(x, y, z);
                }
            }
        }
        // Buttresses down the long walls and at the facade.
        for (int z : new int[]{-4, 0, 4, 8, 12}) {
            for (int s : new int[]{-1, 1}) {
                int x = s * (X1 + 1), top = Math.min(4, wallTop(s * X1, z) - 1);
                for (int y = 0; y <= top; y++) c.set(x, y, z, y == 0 ? "minecraft:mossy_cobblestone" : "minecraft:stone_bricks");
                c.set(x, top + 1, z, St.stairs("minecraft:stone_brick_stairs", s < 0 ? Dir.EAST : Dir.WEST, false));
            }
        }
        for (int x : new int[]{-5, -3, 3, 5}) {
            for (int y = 0; y <= 3; y++) c.set(x, y, Z0 - 1, y == 0 ? "minecraft:mossy_cobblestone" : "minecraft:stone_bricks");
            c.set(x, 4, Z0 - 1, St.stairs("minecraft:stone_brick_stairs", Dir.SOUTH, false));
        }
        // The bell tower's stump at the north-west corner.
        for (int x = -8; x <= -6; x++) {
            for (int z = -8; z <= -6; z++) {
                boolean shaft = x == -7 && z == -7;
                int top = 10 - (int) Math.round(Noise.hash01(x, z, SEED + 16) * 3);
                for (int y = 0; y <= top; y++) {
                    if (shaft) c.carve(x, y, z);
                    else c.set(x, y, z, y == 0 ? "minecraft:mossy_cobblestone" : WALL.at(x, y, z));
                }
                c.set(x, -1, z, "minecraft:stone_bricks");
            }
        }
        for (int y = 0; y <= 2; y++) c.carve(-7, y, -8);
        c.set(-7, 0, -7, St.lantern(false));
        for (int y = 7; y <= 8; y++) {
            c.carve(-8, y, -7);
            c.carve(-6, y, -7);
        }
        // Pews either side of the aisle, a few gone.
        for (int z = -4; z <= 2; z += 2) {
            for (int x = -3; x <= 3; x++) {
                if (x == 0 || Noise.chance(x, 0, z, SEED + 17, 0.15)) continue;
                c.set(x, 0, z, St.stairs("minecraft:dark_oak_stairs", Dir.NORTH, false));
            }
        }
        // The chancel's step, its floor, the altar table and its candles.
        for (int x = -2; x <= 2; x++) {
            c.set(x, 0, 8, St.stairs("minecraft:stone_brick_stairs", Dir.SOUTH, false));
            for (int z = 9; z <= 11; z++) c.set(x, 0, z, z == 11 && Math.abs(x) <= 1 ? "minecraft:chiseled_stone_bricks" : "minecraft:polished_andesite");
        }
        for (int x = -1; x <= 1; x++) c.set(x, 1, 11, St.slabTop("minecraft:polished_andesite_slab"));
        c.set(-1, 2, 11, St.candle("white", 3, true));
        c.set(0, 2, 11, St.lantern(false));
        c.set(1, 2, 11, St.candle("white", 2, true));
        // Rafters still across the nave, rubble where the roof came down.
        for (int z : new int[]{-2, 3}) for (int x = X0 + 1; x <= X1 - 1; x++) {
            if (wallTop(X0, z) >= TOP && wallTop(X1, z) >= TOP) c.set(x, TOP, z, St.axis("minecraft:dark_oak_log", "x"));
        }
        for (int x = X0 + 1; x <= X1 - 1; x++) {
            for (int z = Z0 + 1; z <= 6; z++) {
                if (c.has(x, 0, z) && !St.isAir(c.get(x, 0, z))) continue;
                if (x >= 3 && z >= 4) continue;
                if (Noise.chance(x, 0, z, SEED + 18, 0.08)) c.set(x, 0, z, Noise.chance(x, 1, z, SEED + 19, 0.5) ? "minecraft:cobblestone" : St.mossCarpet());
            }
        }
        // The chancel's roof, the only one left.
        Roofs.Style tiles = new Roofs.Style(Wood.DARK_OAK.family(), Family.STONE_BRICKS, WALL, 1, Roofs.Pitch.STEEP, 0, true, null, true);
        Roofs.gable(c, new Box(X0, 0, 8, X1, 0, Z1), TOP, false, tiles);
        // A railing along the stairwell.
        for (int z = 6; z <= 7; z++) c.set(2, 0, z, St.bars());
    }

    /** Under the floor: the stair down the east aisle, the ossuary hall, the gallery, the reliquary chamber. */
    private static void crypt(Canvas c) {
        org.papiricoh.supernaturalcraft.buildkit.Furniture.flight(c, 4, DEEP, 4, Dir.SOUTH, -DEEP, 2, "minecraft:stone_brick_stairs", false);
        ReliquaryKit.hollow(c, -4, DEEP, -4, 4, DEEP + 3, 3);
        ReliquaryKit.hollow(c, -1, DEEP, 4, 1, DEEP + 2, 10);
        ReliquaryKit.hollow(c, -3, DEEP, 11, 3, DEEP + 3, 16);
        // Niches down the hall's sides.
        for (int z = -3; z <= 3; z += 2) for (int s : new int[]{-1, 1}) ReliquaryKit.hollow(c, s * 5, DEEP + 1, z, s * 5, DEEP + 2, z);
    }

    private static void cryptDecor(Canvas c) {
        for (int z = -3; z <= 3; z += 2) {
            for (int s : new int[]{-1, 1}) {
                c.set(s * 5, DEEP + 1, z, "minecraft:bone_block");
                c.set(s * 5, DEEP + 2, z, St.candle(z % 4 == 1 ? "white" : "light_gray", 2, true));
            }
        }
        Lighting.hanging(c, 0, DEEP + 4, -1, 1, true);
        Lighting.hanging(c, 0, DEEP + 4, 14, 1, true);
        c.set(0, DEEP + 2, 7, St.lantern(true));
        // Coffins either side of the chamber.
        for (int s : new int[]{-1, 1}) {
            for (int z = 12; z <= 13; z++) c.set(s * 3, DEEP, z, St.slabBottom("minecraft:deepslate_tile_slab"));
        }
        c.carve(0, DEEP, 15);
        c.anchor(ReliquaryAnchors.ALTAR, 0, DEEP, 15);
        c.anchor(ReliquaryAnchors.KNEEL, 0, DEEP, 13);
        for (int s : new int[]{-1, 1}) {
            c.set(s * 2, DEEP, 16, "minecraft:chiseled_deepslate");
            c.set(s * 2, DEEP + 1, 16, St.candle("white", 4, true));
        }
        c.carve(-4, DEEP, -4);
        c.anchor(ReliquaryAnchors.chest(0), -4, DEEP, -4);
        c.carve(3, DEEP, 16);
        c.anchor(ReliquaryAnchors.chest(1), 3, DEEP, 16);
    }
}
