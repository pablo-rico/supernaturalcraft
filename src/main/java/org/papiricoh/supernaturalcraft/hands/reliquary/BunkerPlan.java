package org.papiricoh.supernaturalcraft.hands.reliquary;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Furniture;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;

/**
 * The Thule Society's bunker cut into a mountain (v0.42), keeper of the Nail of the Cross. Outside, a cut in the slope with
 * sandbags and wire leads to a concrete portal: buttresses, a canopy, the Society's black sun cast over the door. Behind it a
 * hall and a long stair down into the rock, then a corridor nine blocks under the mountain with rooms either side (the
 * barracks with its bunks, the map room, the armoury, the dead generator) and at its end, behind a frame of sigil plates, the
 * officer's vault: a red runner up to the altar, the black sun on the wall behind it, candles. Copper lamps along the
 * concrete. Plan space: the portal faces -z, the bunker runs +z into the mountain.
 */
final class BunkerPlan {

    static final int SEED = 42_200;
    /** The underground floor's feet row and its ceiling. */
    static final int DEEP = -9;

    static final String CONCRETE = "supernaturalcraft:thule_concrete", PLATE = "supernaturalcraft:thule_sigil_plate";
    static final Brush WALL = Palette.mix(SEED + 1, CONCRETE, 5, "minecraft:gray_concrete", 2, "minecraft:polished_andesite", 2,
            "minecraft:smooth_stone", 1, "minecraft:cracked_stone_bricks", 1);
    static final Brush FLOOR = Palette.mix(SEED + 2, "minecraft:polished_andesite", 4, CONCRETE, 3, "minecraft:gray_concrete", 2,
            "minecraft:smooth_stone", 2);
    static final Brush CEILING = Palette.mix(SEED + 3, CONCRETE, 4, "minecraft:smooth_stone", 3, "minecraft:gray_concrete", 2,
            "minecraft:andesite", 1);
    static final Brush CUT = Palette.mix(SEED + 4, "minecraft:gravel", 4, "minecraft:coarse_dirt", 3, "minecraft:cobblestone", 2,
            "minecraft:stone", 2);

    private BunkerPlan() {
    }

    static Canvas drawn() {
        Canvas c = new Canvas("reliquary_thule_bunker_north");
        approach(c);
        portal(c);
        underground(c);
        ReliquaryKit.wrap(c, new Box(-12, -12, 1, 12, 7, 47), WALL, FLOOR, CEILING);
        decorate(c);
        c.anchor(ReliquaryAnchors.ENTRY, 0, 0, -11);
        c.anchor(ReliquaryAnchors.guard(0), 0, DEEP, 30);
        c.anchor(ReliquaryAnchors.guard(1), -6, DEEP, 22);
        c.anchor(ReliquaryAnchors.guard(2), 2, DEEP, 40);
        return c;
    }

    /** The cut in the slope before the portal: a gravel track, sandbags, wire. */
    private static void approach(Canvas c) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -11; z <= -1; z++) {
                c.set(x, -1, z, CUT.at(x, -1, z));
                for (int y = 0; y <= 4; y++) c.carve(x, y, z);
            }
        }
        for (int x : new int[]{-4, -3, 3, 4}) {
            c.set(x, 0, -6, "minecraft:packed_mud");
            if (Math.abs(x) == 4) c.set(x, 1, -6, "minecraft:mud_bricks");
        }
        c.set(-2, 0, -6, St.slabBottom("minecraft:mud_brick_slab"));
        c.set(2, 0, -6, St.slabBottom("minecraft:mud_brick_slab"));
        for (int x : new int[]{-4, -3, 3, 4}) c.set(x, 0, -9, "minecraft:cobweb");
        Lighting.lampPost(c, -4, 0, -3, 2, "minecraft:polished_andesite", "minecraft:andesite_wall", Dir.EAST, false);
    }

    /** The concrete portal set into the slope. */
    private static void portal(Canvas c) {
        for (int x = -5; x <= 5; x++) {
            for (int y = -1; y <= 6; y++) {
                for (int z = 0; z <= 1; z++) c.set(x, y, z, WALL.at(x, y, z));
            }
        }
        for (int y = 0; y <= 2; y++) for (int x = -1; x <= 1; x++) for (int z = 0; z <= 1; z++) c.carve(x, y, z);
        // Buttresses either side of the door and at the ends, stepped back as they rise.
        for (int x : new int[]{-5, -3, 3, 5}) {
            for (int y = 0; y <= 4; y++) c.set(x, y, -1, Math.abs(x) == 5 ? "minecraft:polished_andesite" : CONCRETE);
            c.set(x, 5, -1, St.stairs("minecraft:andesite_stairs", Dir.SOUTH, false));
            if (Math.abs(x) == 5) c.set(x, 0, -2, St.stairs("minecraft:andesite_stairs", Dir.SOUTH, false));
        }
        // The canopy over the door, the lintel, the black sun cast above.
        for (int x = -2; x <= 2; x++) c.set(x, 4, -1, St.slabTop("minecraft:smooth_stone_slab"));
        for (int x = -1; x <= 1; x++) c.set(x, 3, -1, St.slabTop("minecraft:polished_andesite_slab"));
        c.set(0, 4, 0, PLATE);
        c.set(-1, 5, 0, PLATE);
        c.set(1, 5, 0, PLATE);
        c.set(0, 6, 0, PLATE);
        // Gun slits.
        c.set(-4, 2, 0, St.bars());
        c.set(4, 2, 0, St.bars());
    }

    /** Everything dug into the rock: hall, stair, corridor, rooms, the vault. */
    private static void underground(Canvas c) {
        // The hall behind the door.
        ReliquaryKit.hollow(c, -3, 0, 2, 3, 3, 6);
        // The stair down: nine steps, three wide, from the hall's floor to the deep floor.
        Furniture.flight(c, -1, DEEP, 15, Dir.NORTH, -DEEP, 3, "minecraft:polished_andesite_stairs", false);
        for (int z = 6; z <= 15; z++) for (int x = -1; x <= 1; x++) c.setIfEmpty(x, -9 + (15 - z) - 1, z, FLOOR.at(x, 0, z));
        // The corridor.
        ReliquaryKit.hollow(c, -1, DEEP, 16, 1, DEEP + 2, 37);
        // The rooms either side, their doors.
        ReliquaryKit.hollow(c, -9, DEEP, 18, -3, DEEP + 3, 25);
        ReliquaryKit.hollow(c, -2, DEEP, 21, -2, DEEP + 1, 22);
        ReliquaryKit.hollow(c, 3, DEEP, 18, 9, DEEP + 3, 25);
        ReliquaryKit.hollow(c, 2, DEEP, 21, 2, DEEP + 1, 22);
        ReliquaryKit.hollow(c, -9, DEEP, 28, -3, DEEP + 3, 33);
        ReliquaryKit.hollow(c, -2, DEEP, 30, -2, DEEP + 1, 31);
        ReliquaryKit.hollow(c, 3, DEEP, 28, 9, DEEP + 3, 33);
        ReliquaryKit.hollow(c, 2, DEEP, 30, 2, DEEP + 1, 31);
        // The vault.
        ReliquaryKit.hollow(c, -4, DEEP, 38, 4, DEEP + 4, 45);
    }

    private static void decorate(Canvas c) {
        // The vault's frame of sigil plates round its door, the black sun on its back wall.
        for (int y = DEEP; y <= DEEP + 3; y++) {
            c.set(-2, y, 37, PLATE);
            c.set(2, y, 37, PLATE);
        }
        for (int x = -2; x <= 2; x++) c.set(x, DEEP + 3, 37, PLATE);
        for (int x = -1; x <= 1; x++) c.set(x, DEEP + 3, 37, CONCRETE);
        for (int x = -2; x <= 2; x++) for (int y = DEEP + 1; y <= DEEP + 3; y++) {
            if (Math.abs(x) + Math.abs(y - DEEP - 2) <= 2) c.set(x, y, 46, PLATE);
        }
        for (int z = 38; z <= 42; z++) c.set(0, DEEP, z, St.carpet("red"));
        c.set(0, DEEP, 43, St.carpet("black"));
        c.anchor(ReliquaryAnchors.ALTAR, 0, DEEP, 44);
        c.carve(0, DEEP, 44);
        c.anchor(ReliquaryAnchors.KNEEL, 0, DEEP, 42);
        for (int x : new int[]{-3, 3}) {
            c.set(x, DEEP, 44, "minecraft:polished_blackstone");
            c.set(x, DEEP + 1, 44, St.candle("black", 4, true));
            c.set(x, DEEP, 39, "minecraft:polished_blackstone_bricks");
            c.set(x, DEEP + 1, 39, St.candle("red", 3, true));
        }
        Lighting.hanging(c, -2, DEEP + 5, 41, 2, true);
        Lighting.hanging(c, 2, DEEP + 5, 41, 2, true);
        // Copper lamps along the corridor and the stair, set in the walls.
        for (int z = 17; z <= 36; z += 5) {
            c.set(-2, DEEP + 2, z, St.bulb("minecraft:waxed_copper_bulb"));
            c.set(2, DEEP + 2, z + 2, St.bulb("minecraft:waxed_copper_bulb"));
        }
        for (int z = 7; z <= 14; z += 3) {
            int top = DEEP + (15 - z);
            c.set(-2, top + 2, z, St.bulb("minecraft:waxed_copper_bulb"));
            c.set(2, top + 2, z, St.bulb("minecraft:waxed_copper_bulb"));
        }
        Lighting.hanging(c, 0, 4, 3, 1, false);
        // The barracks: bunks along the west wall, a stove.
        for (int z : new int[]{19, 20, 23, 24}) {
            c.set(-9, DEEP, z, St.slabTop("minecraft:spruce_slab"));
            c.set(-9, DEEP + 2, z, St.slabTop("minecraft:spruce_slab"));
        }
        for (int z : new int[]{18, 21, 22, 25}) {
            c.set(-9, DEEP, z, St.fence("minecraft:spruce_fence"));
            c.set(-9, DEEP + 1, z, St.fence("minecraft:spruce_fence"));
            c.set(-9, DEEP + 2, z, St.fence("minecraft:spruce_fence"));
        }
        c.set(-4, DEEP, 25, St.facing("minecraft:stonecutter", Dir.NORTH));
        Lighting.hanging(c, -6, DEEP + 4, 21, 1, false);
        // The map room: a long table, a cartographer's desk, a chest of orders.
        Furniture.table(c, 5, 20, 7, 23, DEEP, "minecraft:dark_oak_slab");
        Furniture.chair(c, 4, DEEP, 21, Dir.EAST, "minecraft:dark_oak_stairs", null);
        Furniture.chair(c, 8, DEEP, 22, Dir.WEST, "minecraft:dark_oak_stairs", null);
        c.set(9, DEEP, 18, "minecraft:cartography_table");
        c.set(9, DEEP, 19, "minecraft:fletching_table");
        c.carve(9, DEEP, 25);
        c.anchor(ReliquaryAnchors.chest(0), 9, DEEP, 25);
        Lighting.hanging(c, 6, DEEP + 4, 21, 1, false);
        // The armoury: racks of bars, a grindstone, the smith's table, chests.
        for (int z = 28; z <= 33; z++) if (z % 2 == 0) c.set(-9, DEEP + 1, z, St.bars());
        c.set(-9, DEEP, 29, "minecraft:smithing_table");
        c.set(-9, DEEP, 31, St.of("grindstone", "face", "floor", "facing", "east"));
        c.carve(-4, DEEP, 33);
        c.anchor(ReliquaryAnchors.chest(1), -4, DEEP, 33);
        c.carve(-8, DEEP, 33);
        c.anchor(ReliquaryAnchors.chest(2), -8, DEEP, 33);
        Lighting.hanging(c, -6, DEEP + 4, 30, 1, false);
        // The dead generator: iron and copper along the east wall, chains from the ceiling.
        for (int z = 28; z <= 33; z++) {
            c.set(9, DEEP, z, z % 2 == 0 ? "minecraft:iron_block" : "minecraft:waxed_copper_grate");
            c.set(9, DEEP + 1, z, z % 3 == 0 ? "minecraft:waxed_exposed_copper" : "minecraft:iron_block");
        }
        c.set(8, DEEP + 1, 30, St.lever("wall", Dir.WEST));
        for (int x = 5; x <= 7; x += 2) c.set(x, DEEP + 3, 31, St.chain("y"));
        Lighting.hanging(c, 6, DEEP + 4, 29, 1, false);
        // Rubble where the ceiling cracked.
        for (int x = -1; x <= 1; x++) for (int z = 16; z <= 36; z++) {
            if (Noise.chance(x, DEEP, z, SEED + 9, 0.05) && x != 0) c.set(x, DEEP, z, St.slabBottom("minecraft:cobblestone_slab"));
        }
    }
}
