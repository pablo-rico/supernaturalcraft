package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Kit.*;

/**
 * The stairwell north of the war room (pure): a switchback of steel stairs round an open well from level 1 down to levels 2
 * and 3. Each level's landing is on the south (where its doorway opens), the half-landings on the north; flights go down north
 * on the west side and down south on the east, two lanes and a balustrade lane each, smooth soffits, railed well, lamps and the
 * level's name at every landing.
 */
public final class Core {

    /** The inside: x -4..4, z -58..-49; the well x -1..1 between the flights' balustrades (x ±2). */
    private static final int X0 = -4, X1 = 4, Z0 = -58, Z1 = -49;
    private static final int[] LEVELS = {Zones.LEVEL1, Zones.LEVEL2, Zones.LEVEL3};

    private Core() {
    }

    public static void build(Plan p) {
        p.zone(Zones.CORE);
        Zones.Box b = Zones.CORE_BOX;
        int top = Zones.LEVEL1 + 5, bottom = Zones.LEVEL3 - 1;
        // The shaft: rock round it, a finished wall inside, air within.
        Pattern wall = (x, y, z) -> {
            int off = Math.floorMod(y - Zones.LEVEL3, 5);
            return off == 0 ? STEEL_TILES : off == 4 ? TILE_CAP : "minecraft:polished_andesite";
        };
        for (int x = b.x0(); x <= b.x1(); x++) {
            for (int z = b.z0(); z <= b.z1(); z++) {
                for (int y = bottom; y <= top; y++) {
                    boolean inside = x >= X0 && x <= X1 && z >= Z0 && z <= Z1;
                    boolean shell = x >= X0 - 1 && x <= X1 + 1 && z >= Z0 - 1 && z <= Z1 + 1;
                    if (y == bottom || y == top) p.set(x, y, z, inside || shell ? (y == top ? CREAM : FLOOR_BORDER) : ROCK.at(x, y, z));
                    else if (inside) p.set(x, y, z, St.AIR);
                    else if (shell) p.set(x, y, z, wall.at(x, y, z));
                    else p.set(x, y, z, ROCK.at(x, y, z));
                }
            }
        }
        for (int l = 0; l < LEVELS.length; l++) {
            int f = LEVELS[l];
            // The level's landing (south) and, below it, the half-landing (north) and the two flights.
            landing(p, f, Z1 - 1, Z1);
            if (l == LEVELS.length - 1) break;
            int half = f - 5;
            landing(p, half, Z0, Z0 + 2);
            flight(p, X0 + 1, Z1 - 2, f, "north", "west", 2, 5, STEEL_STAIRS, STEEL_STAIRS);
            flight(p, X0 + 2, Z1 - 2, f, "north", "east", 1, 5, STEEL_STAIRS, STEEL_STAIRS);
            balusters(p, X0 + 2, Z1 - 2, f, "north", 5);
            flight(p, X1 - 1, Z0 + 3, half, "south", "east", 2, 5, STEEL_STAIRS, STEEL_STAIRS);
            flight(p, X1 - 2, Z0 + 3, half, "south", "west", 1, 5, STEEL_STAIRS, STEEL_STAIRS);
            balusters(p, X1 - 2, Z0 + 3, half, "south", 5);
            // Railings on the landings' edges over the well.
            List<int[]> south = new ArrayList<>(), north = new ArrayList<>();
            for (int x = -2; x <= 2; x++) {
                south.add(new int[]{x, Z1 - 2});
                north.add(new int[]{x, Z0 + 3});
            }
            railing(p, f + 1, south, BLACK_WALL, 0);
            railing(p, half + 1, north, BLACK_WALL, 0);
            for (int x = -2; x <= 2; x++) p.set(x, f, Z1 - 2, STEEL_TILES);
            for (int x = -2; x <= 2; x++) p.set(x, half, Z0 + 3, STEEL_TILES);
            // Lamps at both landings: a bulb set in the wall behind a louvre.
            wallLight(p, X0 - 1, f + 3, Z1 - 1, "east", "minecraft:waxed_copper_bulb[lit=true,powered=false]");
            wallLight(p, X1 + 1, half + 3, Z0 + 1, "west", "minecraft:waxed_copper_bulb[lit=true,powered=false]");
            wallLight(p, X0 - 1, half + 3, Z0 + 1, "east", "minecraft:waxed_copper_bulb[lit=true,powered=false]");
        }
        wallLight(p, X0 - 1, Zones.LEVEL3 + 3, Z1 - 1, "east", "minecraft:waxed_copper_bulb[lit=true,powered=false]");
        wallLight(p, X1 + 1, Zones.LEVEL3 + 3, Z1 - 1, "west", "minecraft:waxed_copper_bulb[lit=true,powered=false]");
        // The doorways south out of each landing, and the level's name over each.
        String[] names = {"Level 1", "Level 2", "Level 3"};
        String[] rooms = {"War Room", "Quarters", "Garage - Vault"};
        for (int l = 0; l < LEVELS.length; l++) {
            int f = LEVELS[l];
            doorway(p, 0, f, Z1 + 1, "south", 2, 3, 4, BLACK, BLACK_STAIRS, null);
            sign(p, 2, f + 3, Z1, "north", "dark_oak", "yellow", "", names[l], rooms[l], "");
        }
        // A light at the bottom of the well.
        p.set(0, Zones.LEVEL3, -53, "minecraft:sea_lantern");
        p.view("stairwell", 0, Zones.LEVEL1 + 1, Z1, 0, Zones.LEVEL1 - 6, -60);
        p.zone(null);
    }

    /** A landing across the shaft's whole width over rows {@code z0..z1}, floor at {@code y}, headroom cleared. */
    private static void landing(Plan p, int y, int z0, int z1) {
        for (int x = X0; x <= X1; x++) {
            for (int z = z0; z <= z1; z++) {
                p.set(x, y, z, STEEL_TILES);
                for (int h = 1; h <= 3; h++) p.set(x, y + h, z, St.AIR);
            }
        }
    }
}
