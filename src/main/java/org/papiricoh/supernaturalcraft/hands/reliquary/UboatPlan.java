package org.papiricoh.supernaturalcraft.hands.reliquary;

import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;

/**
 * The wreck of a 1943 U-boat on the ocean floor (v0.42), keeper of Delphine's Hand: a long steel hull broken in two and half sunk
 * in the silt, its plates rusted through, the conning tower still standing with its rail and periscope, the deck gun forward.
 * A hunter swims in through the breach torn in the bow's flank; inside, everything is flooded: the crew's quarters (bunks,
 * a chart table), the bulkheads with their round doors, and forward the torpedo room where the Hand lies on its altar between
 * the racks. The stern lies a little off across the gap, its engines dead. Lit by the sea itself: sea lanterns set in the hull
 * and sea pickles on the deck plates. Plan space: the hull lies along x, its breach facing -z.
 */
final class UboatPlan {

    static final int SEED = 42_100;
    static final int BOW = -20, STERN = 20, GAP0 = 3, GAP1 = 5, AXIS_Y = 2;
    static final double R = 3.4, SQUASH = 0.92;

    static final String HULL = "supernaturalcraft:rusted_hull", PLATE = "supernaturalcraft:rusted_hull_plate";
    static final Brush SHELL = Palette.patches2(SEED + 1, 2.5, HULL, 6, PLATE, 4, "minecraft:waxed_oxidized_copper", 1,
            "minecraft:waxed_weathered_cut_copper", 1);
    static final Brush SILT = Palette.patches2(SEED + 2, 3, "minecraft:sand", 5, "minecraft:gravel", 3, "minecraft:clay", 2,
            "minecraft:mud", 1);
    static final Brush BULKHEAD = Palette.mix(SEED + 3, PLATE, 4, "minecraft:waxed_weathered_copper", 1, "minecraft:iron_block", 1);

    private UboatPlan() {
    }

    /** The hull's radius at {@code x}: full amidships, tapering to the bow and the stern. */
    static double radius(int x) {
        double t = Math.min(1.0, (21 - Math.abs(x)) / 4.0);
        return Math.max(1.3, R * t);
    }

    static boolean section(int x) {
        return x >= BOW && x <= STERN && (x < GAP0 || x > GAP1);
    }

    static double ellipse(int y, int z, double r) {
        double dz = z / r, dy = (y - AXIS_Y) / (r * SQUASH);
        return dz * dz + dy * dy;
    }

    static Canvas drawn() {
        Canvas c = new Canvas("reliquary_uboat_wreck_north");
        String water = St.water();
        ReliquaryAnchors.Bounds b = ReliquaryAnchors.bounds(org.papiricoh.supernaturalcraft.darkness.S11Assets.Hand.DELPHINES_HAND);
        // The silt bed, the hull settled into it.
        for (int x = b.minX(); x <= b.maxX(); x++) {
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                c.set(x, -2, z, SILT.at(x, -2, z));
                c.set(x, -1, z, SILT.at(x, -1, z));
            }
        }
        // The hull: a shell round a flooded inside, the floor plates at y = -1.
        for (int x = BOW; x <= STERN; x++) {
            if (!section(x)) continue;
            double r = radius(x);
            for (int y = -1; y <= AXIS_Y + 4; y++) {
                for (int z = -5; z <= 5; z++) {
                    double e = ellipse(y, z, r), in = r > 1.6 ? ellipse(y, z, r - 1) : 99;
                    if (e > 1) continue;
                    if (in <= 1) c.set(x, y, z, water);
                    else c.set(x, y, z, SHELL.at(x, y, z));
                }
            }
        }
        // Close the shell where the ellipse's steps leave the inside open to the sea (across the hull, never along it: the
        // halves stay open where they broke).
        for (int x = BOW; x <= STERN; x++) {
            if (!section(x)) continue;
            for (int y = -1; y <= AXIS_Y + 4; y++) {
                for (int z = -5; z <= 5; z++) {
                    if (!Ws.water(c.get(x, y, z))) continue;
                    for (int[] d : new int[][]{{0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                        int ny = y + d[1], nz = z + d[2];
                        if (!c.has(x, ny, nz)) c.set(x, ny, nz, SHELL.at(x, ny, nz));
                    }
                }
            }
        }
        // The broken middle:the hull's torn ends, plates strewn over the silt between the halves.
        for (int x : new int[]{GAP0 - 1, GAP1 + 1}) {
            for (int y = 0; y <= AXIS_Y + 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    if (c.has(x, y, z) && !St.isAir(c.get(x, y, z)) && !Ws.water(c.get(x, y, z)) && y >= 1 && Noise.chance(x, y, z, SEED + 4, 0.45)) c.set(x, y, z, water);
                }
            }
        }
        for (int x = GAP0; x <= GAP1; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Noise.chance(x, 0, z, SEED + 5, 0.35)) c.set(x, 0, z, PLATE);
                else if (Noise.chance(x, 0, z, SEED + 6, 0.15)) c.set(x, 0, z, St.waterlogged(St.chain("x")));
                else c.set(x, 0, z, water);
            }
        }
        // Rust holes in the upper plates.
        for (int x = BOW + 2; x <= STERN - 2; x++) {
            for (int y = AXIS_Y + 1; y <= AXIS_Y + 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    if (Math.abs(x + 3) <= 3 && Math.abs(z) <= 1) continue;
                    String s = c.get(x, y, z);
                    if (s != null && St.id(s).startsWith("supernaturalcraft:rusted") && Noise.chance(x, y, z, SEED + 7, 0.06)) c.set(x, y, z, water);
                }
            }
        }
        // The breach in the bow's flank, down to the floor plates.
        for (int x = -10; x <= -7; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = -5; z <= -1; z++) {
                    if (c.has(x, y, z) && !Ws.water(c.get(x, y, z)) && (y < 3 || x > -10 && x < -7)) c.set(x, y, z, water);
                }
            }
        }
        // Bulkheads with their round doors, between the torpedo room, the quarters and the control room.
        for (int x : new int[]{-13, -4}) {
            double r = radius(x);
            for (int y = 0; y <= AXIS_Y + 3; y++) {
                for (int z = -4; z <= 4; z++) {
                    if (ellipse(y, z, r - 1) > 1) continue;
                    boolean door = Math.abs(z) <= 1 && y <= 2 && !(Math.abs(z) == 1 && y == 2);
                    c.set(x, y, z, door ? water : BULKHEAD.at(x, y, z));
                }
            }
        }
        tower(c);
        gun(c);
        torpedoRoom(c);
        quarters(c);
        stern(c);
        lights(c);
        c.anchor(ReliquaryAnchors.ENTRY, -8, 0, -9);
        c.anchor(ReliquaryAnchors.guard(0), -9, 0, 0);
        c.anchor(ReliquaryAnchors.guard(1), 13, 0, 0);
        return c;
    }

    /** Water helpers for the plan (planned water and waterlogged blocks). */
    static final class Ws {
        static boolean water(String s) {
            return s != null && (St.path(s).equals("water") || "true".equals(St.get(s, "waterlogged")));
        }
    }

    /** The conning tower: its shell, flooded inside, a rail round the open top, the periscope and the hatch. */
    private static void tower(Canvas c) {
        String water = St.water();
        int top = AXIS_Y + 7;
        for (int x = -6; x <= -1; x++) {
            for (int z = -2; z <= 2; z++) {
                boolean edge = x == -6 || x == -1 || Math.abs(z) == 2;
                boolean corner = (x == -6 || x == -1) && Math.abs(z) == 2;
                for (int y = AXIS_Y + 3; y <= top; y++) {
                    if (corner && y > AXIS_Y + 4) continue;
                    c.set(x, y, z, edge ? SHELL.at(x, y, z) : water);
                }
                if (edge && !corner) c.set(x, top + 1, z, St.waterlogged(St.bars()));
            }
        }
        // The opening down into the hull under the tower, the hatch thrown back.
        for (int x = -4; x <= -3; x++) c.set(x, AXIS_Y + 3, 0, water);
        c.set(-2, top + 1, 0, St.waterlogged(St.chain("y")));
        c.set(-2, top + 2, 0, St.waterlogged(St.chain("y")));
        c.set(-5, top, 1, St.waterlogged(St.trapdoor("minecraft:iron_trapdoor", org.papiricoh.supernaturalcraft.buildkit.Dir.SOUTH, true, true)));
    }

    /** The deck gun forward of the tower. */
    private static void gun(Canvas c) {
        int deck = AXIS_Y + (int) Math.floor(R * SQUASH) + 1;
        c.set(-11, deck, 0, "minecraft:iron_block");
        for (int x = -14; x <= -11; x++) c.set(x, deck + 1, 0, St.axis("minecraft:polished_basalt", "x"));
        c.set(-11, deck + 1, 1, St.waterlogged(St.bars()));
        c.set(-11, deck + 1, -1, St.waterlogged(St.bars()));
    }

    /** Forward: the torpedo racks either side, the altar between them where the Hand lies. */
    private static void torpedoRoom(Canvas c) {
        for (int x = -17; x <= -15; x++) {
            for (int z : new int[]{-2, 2}) {
                if (Ws.water(c.get(x, AXIS_Y, z))) c.set(x, AXIS_Y, z, St.axis("minecraft:polished_basalt", "x"));
                if (Ws.water(c.get(x, AXIS_Y + 1, z))) c.set(x, AXIS_Y + 1, z, St.axis("minecraft:polished_basalt", "x"));
            }
        }
        c.set(-16, 0, 0, St.water());
        c.anchor(ReliquaryAnchors.ALTAR, -16, 0, 0);
        c.anchor(ReliquaryAnchors.KNEEL, -14, 0, 0);
        c.set(-16, 0, -1, St.seaPickle(4, true));
        c.set(-16, 0, 1, St.seaPickle(4, true));
    }

    /** The crew's quarters: bunks on the ledges, a chart table, a chest under the bunks. */
    private static void quarters(Canvas c) {
        for (int x = -12; x <= -6; x++) {
            if (x == -9) continue;
            if (Ws.water(c.get(x, 1, 2))) c.set(x, 1, 2, St.waterlogged(St.slabTop("minecraft:dark_oak_slab")));
            if (Ws.water(c.get(x, 3, 2)) && x % 2 == 0) c.set(x, 3, 2, St.waterlogged(St.slabBottom("minecraft:dark_oak_slab")));
        }
        c.set(-11, 1, -2, St.waterlogged(St.slabTop("minecraft:spruce_slab")));
        c.set(-10, 1, -2, St.waterlogged(St.slabTop("minecraft:spruce_slab")));
        c.set(-8, 0, 1, St.water());
        c.anchor(ReliquaryAnchors.chest(0), -8, 0, 1);
        // Hanging chains where the lamps were.
        c.set(-2, AXIS_Y + 2, 1, St.waterlogged(St.chain("y")));
        c.set(-2, AXIS_Y + 1, 1, St.waterlogged(St.chain("y")));
    }

    /** The stern, across the gap: the dead engines along its sides, a chest by them. */
    private static void stern(Canvas c) {
        for (int x = 9; x <= 16; x++) {
            for (int z : new int[]{-2, 2}) {
                String eng = x % 3 == 0 ? "minecraft:waxed_copper_grate" : "minecraft:iron_block";
                if (Ws.water(c.get(x, 1, z))) c.set(x, 1, z, eng);
                if (Ws.water(c.get(x, 2, z))) c.set(x, 2, z, x % 2 == 0 ? "minecraft:waxed_exposed_copper" : PLATE);
            }
        }
        c.set(14, 0, 0, St.water());
        c.anchor(ReliquaryAnchors.chest(1), 14, 0, 0);
    }

    /** Sea lanterns set in the hull's crown and pickles on the plates: the sea's own light. */
    private static void lights(Canvas c) {
        int crown = AXIS_Y + 3;
        for (int x : new int[]{-16, -11, -7, 8, 12, 16}) {
            for (int y = crown + 2; y >= crown; y--) {
                String s = c.get(x, y, 0);
                if (s != null && St.id(s).startsWith("supernaturalcraft:rusted")) {
                    c.set(x, y, 0, "minecraft:sea_lantern");
                    break;
                }
            }
        }
        for (int[] p : new int[][]{{-11, 0, 0}, {-6, 0, -1}, {-2, 0, 0}, {7, 0, 1}, {11, 0, -1}, {17, 0, 0}}) {
            if (Ws.water(c.get(p[0], p[1], p[2]))) c.set(p[0], p[1], p[2], St.seaPickle(3, true));
        }
    }
}
