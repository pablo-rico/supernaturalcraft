package org.papiricoh.supernaturalcraft.buildkit;

import java.util.ArrayList;
import java.util.List;

/**
 * The small things that make a room lived in (pure): props on table tops and counters, pots and baskets along the walls, things
 * hung on the walls ({@link Decor}). {@link #room} looks for free surfaces inside a room's box and dresses a share of them from
 * the {@link Kind}'s set. Props are chosen so each stands on what it is placed on (candles, pots and lanterns need a sturdy top).
 * {@code entities = false} keeps block entities out (decorated pots, barrels, lecterns, chests) for arena-written layouts.
 */
public final class Clutter {

    private Clutter() {
    }

    /** What sort of room it is. */
    public enum Kind {LIVING, KITCHEN, BEDROOM, STUDY, BAR, CLINIC, OFFICE, WORKSHOP, STORAGE, PORCH}

    /** Props that stand on a surface (a table top, a counter, a shelf). */
    static Object[] onTop(Kind k, boolean entities) {
        return switch (k) {
            case KITCHEN -> new Object[]{"minecraft:flower_pot", 2, St.candle("white", 1, true), 2, "minecraft:potted_fern", 1,
                    entities ? St.decoratedPot(Dir.SOUTH) : "minecraft:potted_red_mushroom", 2, "minecraft:potted_brown_mushroom", 1};
            case BAR -> new Object[]{St.candle("orange", 2, true), 3, "minecraft:potted_dead_bush", 1, "minecraft:brewing_stand", 1,
                    St.lantern(false), 1};
            case CLINIC -> new Object[]{"minecraft:brewing_stand", 2, St.candle("white", 1, false), 1, "minecraft:potted_white_tulip", 1};
            case OFFICE -> new Object[]{St.candle("light_gray", 1, false), 1, "minecraft:potted_bamboo", 1, "minecraft:potted_cactus", 1,
                    St.trapdoor("minecraft:iron_trapdoor", Dir.NORTH, false, true), 2};
            case STUDY -> new Object[]{St.candle("white", 3, true), 3, St.lantern(false), 1, "minecraft:potted_fern", 1,
                    "minecraft:potted_azure_bluet", 1};
            case WORKSHOP -> new Object[]{St.lantern(false), 1, "minecraft:flower_pot", 1, St.candle("brown", 2, true), 1};
            default -> new Object[]{St.candle("white", 2, true), 2, "minecraft:potted_red_tulip", 1, "minecraft:potted_azalea_bush", 1,
                    "minecraft:potted_lily_of_the_valley", 1, St.lantern(false), 1};
        };
    }

    /** Props that stand on the floor against a wall. */
    static Object[] onFloor(Kind k, boolean entities) {
        List<Object> out = new ArrayList<>(List.of("minecraft:potted_fern", 1, "minecraft:flower_pot", 1));
        switch (k) {
            case KITCHEN -> out.addAll(List.of("minecraft:composter", 1, St.axis("minecraft:hay_block", "y"), 1, "minecraft:cauldron", 1));
            case BAR -> out.addAll(List.of("minecraft:cauldron", 1, St.axis("minecraft:hay_block", "y"), 1));
            case WORKSHOP -> out.addAll(List.of("minecraft:smithing_table", 1, "minecraft:grindstone[face=floor,facing=north]", 1,
                    "minecraft:fletching_table", 1, "minecraft:anvil[facing=north]", 1));
            case STORAGE -> out.addAll(List.of(St.axis("minecraft:hay_block", "y"), 2, "minecraft:composter", 1));
            case PORCH -> out.addAll(List.of("minecraft:potted_azalea_bush", 2, "minecraft:potted_flowering_azalea_bush", 2));
            case CLINIC -> out.addAll(List.of("minecraft:cauldron", 1, "minecraft:potted_white_tulip", 1));
            default -> out.addAll(List.of("minecraft:potted_azalea_bush", 1, "minecraft:potted_flowering_azalea_bush", 1));
        }
        if (entities) {
            out.addAll(List.of(St.decoratedPot(Dir.SOUTH), 1));
            if (k == Kind.STORAGE || k == Kind.KITCHEN || k == Kind.BAR || k == Kind.WORKSHOP) out.addAll(List.of(St.barrel(Dir.UP, false), 2));
        }
        return out.toArray();
    }

    /**
     * Dresses a room: inside {@code room} (its air, inclusive), a share {@code topShare} of the free cells right above table tops,
     * counters and shelves gets a prop, and a share {@code floorShare} of the floor cells along walls (not in front of doors or
     * in the walkway {@code keepClear}, which may be null).
     */
    public static int room(Canvas c, Box room, Kind kind, double topShare, double floorShare, boolean entities, Box keepClear, int seed) {
        int n = 0;
        Object[] tops = onTop(kind, entities), floors = onFloor(kind, entities);
        for (int x = room.x0(); x <= room.x1(); x++) {
            for (int z = room.z0(); z <= room.z1(); z++) {
                for (int y = room.y0(); y <= room.y1(); y++) {
                    if (!c.isAir(x, y, z) || keepClear != null && keepClear.contains(x, y, z)) continue;
                    String below = c.world(x, y - 1, z);
                    if (Kinds.air(below)) continue;
                    boolean surface = y > room.y0() && furnitureTop(below);
                    if (surface && Noise.chance(x, y, z, seed, topShare)) {
                        c.set(x, y, z, Scatter.pick(x, z * 31 + y, seed + 1, tops));
                        n++;
                    } else if (y == room.y0() && againstWall(c, x, y, z) && Noise.chance(x, y, z, seed + 2, floorShare) && notByDoor(c, x, y, z)) {
                        c.set(x, y, z, Scatter.pick(x, z * 31 + y, seed + 3, floors));
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /** Things one puts props on: top slabs, closed top trapdoors, counters, barrels, cabinets, bookshelves at waist height. */
    static boolean furnitureTop(String s) {
        String p = St.path(s);
        if (p.endsWith("_slab")) return "top".equals(St.get(s, "type")) || "double".equals(St.get(s, "type"));
        if (p.endsWith("_trapdoor")) return "top".equals(St.get(s, "half")) && !"true".equals(St.get(s, "open"));
        return p.equals("barrel") || p.equals("bookshelf") || p.equals("crafting_table") || p.equals("smoker")
                || p.equals("cartography_table") || p.equals("fletching_table") || p.equals("note_block");
    }

    private static boolean againstWall(Canvas c, int x, int y, int z) {
        for (Dir d : Dir.HORIZONTAL) if (Kinds.opaqueCube(c.world(x + d.dx, y, z + d.dz)) && !Kinds.air(c.world(x + d.dx, y, z + d.dz))) return true;
        return false;
    }

    private static boolean notByDoor(Canvas c, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                String s = c.world(x + dx, y, z + dz);
                if (St.path(s).endsWith("_door") || St.path(s).endsWith("_stairs") && dx * dz == 0) return false;
            }
        }
        return true;
    }

    /** A painting centred on a wall face above the floor (the wall behind must be solid there). */
    public static void painting(Canvas c, int x, int y, int z, Dir facing, String variant) {
        c.decor(Decor.painting(x, y, z, facing, variant));
    }
}
