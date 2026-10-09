package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import static org.papiricoh.supernaturalcraft.legacy.bunker.plan.Kit.*;

/**
 * Level 2 (pure): the order's quarters. The stairwell's door opens south on a short lobby and that on the long corridor of
 * numbered doors, running east and west under the war room: Lebanon tile below cream, black iron pilasters every nine
 * blocks carrying black ribs over a cream barrel of a ceiling, a louvred lamp on every other pilaster. Eight dormitories
 * open north of it (7 to 10 west, 11 to 14 east, see {@link Level2Quarters}); south of it the wings (see {@link Level2Wings}):
 * the lab, the baths, the kitchen straight ahead of the stairs, the Dean cave and the armoury.
 *
 * <pre>
 *   z -53 .. -44   dormitories  7  8  9  10  [stairwell]  11  12  13  14
 *   z -43 .. -39   the corridor, x -41 .. 41 (inside z -42 .. -40)
 *   z -38 .. -24   lab · baths · kitchen · Dean cave · armoury
 * </pre>
 */
public final class Level2 {

    static final int F = Zones.LEVEL2;
    /** The corridor's inside. */
    static final int CX0 = -41, CX1 = 41, CZ0 = -42, CZ1 = -40;

    private Level2() {
    }

    public static void build(Plan p) {
        p.zone(Zones.LEVEL_2);
        corridor(p);
        Level2Quarters.build(p);
        Level2Wings.build(p);
        lobby(p);
        p.view("quarters", 40, F + 1, -41, 0, F + 2.5, -41);
        p.zone(null);
    }

    /** Whether a corridor x carries a pilaster (and its rib): every nine blocks from x ±7, between the doors. */
    static boolean pilaster(int x) {
        return x != 0 && Math.floorMod(Math.abs(x) - 7, 9) == 0 && Math.abs(x) <= 34;
    }

    private static void corridor(Plan p) {
        Room r = new Room(CX0, CX1, F, 5, CZ0, CZ1);
        Pattern ceiling = (x, y, z) -> pilaster(x) ? BLACK : CREAM;
        room(p, r, new Style(concreteFloor(), lebanonWall(F), ceiling, CREAM_STAIRS, null, 0, null, 0));
        // A black border either side of the checkered floor.
        for (int x = CX0; x <= CX1; x++) {
            p.set(x, F, CZ0 - 1, FLOOR_BORDER);
            p.set(x, F, CZ1 + 1, FLOOR_BORDER);
        }
        for (int x = CX0; x <= CX1; x++) {
            if (!pilaster(x)) continue;
            for (int z : new int[]{CZ0 - 1, CZ1 + 1}) {
                for (int y = F + 1; y <= F + 4; y++) p.set(x, y, z, St.axis(IRON_COLUMN, "y"));
                p.set(x, F + 5, z, BLACK_CHISELED);
            }
            // The rib: the cornice turns black over the pilasters and a black slab bridges the middle lane.
            p.set(x, F + 5, CZ0, St.stairs(BLACK_STAIRS, "north", true));
            p.set(x, F + 5, CZ1, St.stairs(BLACK_STAIRS, "south", true));
            p.set(x, F + 5, CZ0 + 1, St.slab(BLACK_SLAB, true));
            // A louvred lamp in every pilaster, alternating sides.
            boolean north = Math.floorMod((Math.abs(x) - 7) / 9, 2) == 0;
            wallLight(p, x, F + 3, north ? CZ0 - 1 : CZ1 + 1, north ? "south" : "north", HIDDEN_LIGHT);
        }
        // Pendants down the middle at both ends and over the stairwell's arch.
        for (int x : new int[]{-39, 0, 39}) p.set(x, F + 5, CZ0 + 1, St.lantern(true));
        // The ends: the order's banner over a bench and a potted palm, a fire bucket.
        for (int s : new int[]{-1, 1}) {
            int x = s * CX1;
            String in = s > 0 ? "west" : "east";
            banner(p, x, F + 4, CZ0 + 1, in, "black", ORDER_BANNER);
            chair(p, x, F + 1, CZ0 + 1, in, OAK_STAIRS, OAK_TRAPDOOR);
            p.set(x, F + 1, CZ0, "minecraft:potted_fern");
            p.set(x, F + 1, CZ1, "minecraft:cauldron");
            p.decor(Decor.frame(x, F + 3, CZ1, in, "minecraft:compass"));
        }
        // Little life along the way: a laundry basket, a stack of case files, a coat stand by the kitchen.
        p.set(-5, F + 1, CZ0, "minecraft:composter[level=3]");
        p.set(5, F + 1, CZ1, Level2Props.barrel("up"));
        p.set(5, F + 2, CZ1, St.candle("white", 1, false));
        p.set(-34 + 2, F + 1, CZ1, Level2Props.books("north", 0b100101));
        p.set(32, F + 1, CZ0, Level2Props.pot("south"));
    }

    /** The lobby from the stairwell's door (z -46) to the corridor, under an arch, the level's sign over it. */
    private static void lobby(Plan p) {
        enclose(p, -3, 3, F - 1, F + 6, -46, -44);
        Pattern wall = lebanonWall(F);
        for (int z = -46; z <= -44; z++) {
            for (int x = -2; x <= 2; x++) {
                boolean edge = Math.abs(x) == 2;
                p.set(x, F, z, edge ? FLOOR_BORDER : concreteFloor().at(x, F, z));
                for (int y = F + 1; y <= F + 4; y++) p.set(x, y, z, edge ? wall.at(x, y, z) : St.AIR);
                p.set(x, F + 5, z, CREAM);
            }
        }
        doorway(p, 0, F, -44, "south", 2, 3, 4, BLACK, BLACK_STAIRS, null);
        wallLight(p, -2, F + 3, -45, "east", HIDDEN_LIGHT);
        p.decor(Decor.frame(1, F + 2, -45, "west", "minecraft:clock"));
    }
}
