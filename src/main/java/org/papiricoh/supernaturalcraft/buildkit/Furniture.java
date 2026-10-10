package org.papiricoh.supernaturalcraft.buildkit;

/**
 * Furniture from vanilla blocks (pure), the way builders make it: stairs for seats, slabs and trapdoors for tops, fences with a
 * carpet for small tables and stools, barrels and trapdoor fronts for cabinets, open trapdoors for monitors and armrests.
 * {@code y} is the floor-level cell the piece stands in (the first air above the floor). {@code looks} is where a seated person
 * faces; {@code front} where a cabinet's front faces. Pieces with block entities (barrels, beds, chests, lecterns, jukeboxes,
 * chiseled bookshelves) say so: layouts written through an arena must pass {@code entities = false} or avoid them.
 */
public final class Furniture {

    private Furniture() {
    }

    /** A chair: a stair seat with its back behind the sitter, open trapdoor arms either side where there is room (null: none). */
    public static void chair(Canvas c, int x, int y, int z, Dir looks, String stairs, String arms) {
        c.set(x, y, z, St.stairs(stairs, looks.opposite(), false));
        if (arms == null) return;
        for (Dir side : new Dir[]{looks.cw(), looks.ccw()}) {
            if (c.isAir(x + side.dx, y, z + side.dz)) c.set(x + side.dx, y, z + side.dz, St.trapdoorAgainst(arms, side.opposite()));
        }
    }

    /** A sofa {@code length} seats long from (x, z) toward {@code looks.cw()}: stair seats, trapdoor arms at both ends. */
    public static void sofa(Canvas c, int x, int y, int z, Dir looks, int length, String stairs, String arms) {
        Dir along = looks.cw();
        for (int i = 0; i < length; i++) c.set(x + along.dx * i, y, z + along.dz * i, St.stairs(stairs, looks.opposite(), false));
        c.setIfAir(x - along.dx, y, z - along.dz, St.trapdoorAgainst(arms, along));
        c.setIfAir(x + along.dx * length, y, z + along.dz * length, St.trapdoorAgainst(arms, along.opposite()));
    }

    /** A small round table: a fence leg with a carpet cloth on top (the carpet sits on the fence post). */
    public static void sideTable(Canvas c, int x, int y, int z, String fence, String cloth) {
        c.set(x, y, z, St.fence(fence));
        if (cloth != null) c.set(x, y + 1, z, St.carpet(cloth));
    }

    /** A table top of top slabs over a rectangle at row {@code y} (the top is at {@code y + 1}, a seat's height above a stair). */
    public static void table(Canvas c, int x0, int z0, int x1, int z1, int y, String slab) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) c.set(x, y, z, St.slabTop(slab));
        }
    }

    /** A dining set: a table of top slabs and a chair on each long side of every table cell where there is floor. */
    public static void diningSet(Canvas c, int x0, int z0, int x1, int z1, int y, String slab, String chairStairs) {
        table(c, x0, z0, x1, z1, y, slab);
        boolean alongX = Math.abs(x1 - x0) >= Math.abs(z1 - z0);
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                Dir[] sides = alongX ? new Dir[]{Dir.NORTH, Dir.SOUTH} : new Dir[]{Dir.EAST, Dir.WEST};
                for (Dir s : sides) {
                    int cx = x + s.dx, cz = z + s.dz;
                    if (c.isAir(cx, y, cz) && !c.isAir(cx, y - 1, cz)) chair(c, cx, y, cz, s.opposite(), chairStairs, null);
                }
            }
        }
    }

    /** A bed with its foot at (x, y, z) pointing {@code facing}, a nightstand beside its head with a lantern or candle. */
    public static void bed(Canvas c, int x, int y, int z, Dir facing, String color, String nightstand, boolean candle) {
        c.setBed(x, y, z, St.bed(color, facing));
        Dir side = facing.cw();
        int hx = x + facing.dx + side.dx, hz = z + facing.dz + side.dz;
        if (nightstand != null && c.isAir(hx, y, hz)) {
            c.set(hx, y, hz, nightstand);
            c.set(hx, y + 1, hz, candle ? St.candle("white", 2, true) : St.lantern(false));
        }
    }

    /**
     * A wall of shelves along a run of cells from (x, z) toward {@code along}, rows {@code y0..y1}, facing {@code front}: plain
     * bookshelves mixed with chiseled ones (books in varying slots) when {@code entities}, else bookshelves with a few
     * {@code accent} blocks (e.g. a stripped log) as uprights every {@code every} cells.
     */
    public static void bookshelves(Canvas c, int x, int z, Dir along, int length, int y0, int y1, Dir front, boolean entities, String upright, int every) {
        for (int i = 0; i < length; i++) {
            int cx = x + along.dx * i, cz = z + along.dz * i;
            boolean post = upright != null && every > 0 && i % every == 0;
            for (int y = y0; y <= y1; y++) {
                String s;
                if (post) s = upright;
                else if (entities && Noise.chance(cx, y, cz, 61, 0.35)) s = St.chiseledShelf(front, (int) (Noise.hash(cx, y, cz, 62) & 63));
                else s = "minecraft:bookshelf";
                c.set(cx, y, cz, s);
            }
        }
    }

    /**
     * A cabinet: a block at (x, y, z) with a door front — an open trapdoor flat on its face in the cell in front. With
     * {@code entities} the block is a barrel turned to the front (storage), else {@code body}.
     */
    public static void cabinet(Canvas c, int x, int y, int z, Dir front, String body, String trapdoor, boolean entities) {
        c.set(x, y, z, entities ? St.barrel(front, false) : body);
        if (trapdoor != null) c.setIfAir(x + front.dx, y, z + front.dz, St.trapdoorAgainst(trapdoor, front.opposite()));
    }

    /**
     * A kitchen run along a wall: base units ({@code base} blocks with a smoker, a sink — a water cauldron — and, with
     * {@code entities}, barrels), and a shelf of top-half trapdoors on the wall two rows up with jars and pots on it.
     */
    public static void kitchen(Canvas c, int x, int y, int z, Dir along, int length, Dir front, String base, String shelfTrapdoor, boolean entities) {
        for (int i = 0; i < length; i++) {
            int cx = x + along.dx * i, cz = z + along.dz * i;
            String s;
            if (i == length / 2) s = St.of("water_cauldron", "level", "3");
            else if (i == 1 && entities) s = St.of("smoker", "facing", front.id(), "lit", "false");
            else if (entities && i % 3 == 0) s = St.barrel(front, false);
            else s = base;
            c.set(cx, y, cz, s);
            c.set(cx, y + 2, cz, St.trapdoor(shelfTrapdoor, front, true, false));
            double n = Noise.hash01(cx, y, cz, 71);
            String prop = n < 0.25 ? "minecraft:flower_pot" : n < 0.4 ? St.candle("white", 1, false) : n < 0.55 ? "minecraft:potted_fern"
                    : n < 0.65 ? (entities ? St.decoratedPot(front) : null) : null;
            if (prop != null) c.setIfAir(cx, y + 3, cz, prop);
        }
    }

    /**
     * A desk at (x, y, z) facing {@code looks} (where its user faces): a top slab, a chair behind it, and on it either a monitor
     * (an open iron trapdoor standing on the slab) or a lamp.
     */
    public static void desk(Canvas c, int x, int y, int z, Dir looks, String slab, String chairStairs, boolean monitor) {
        c.set(x, y, z, St.slabTop(slab));
        chair(c, x - looks.dx, y, z - looks.dz, looks, chairStairs, null);
        if (monitor) c.set(x, y + 1, z, St.trapdoor("minecraft:iron_trapdoor", looks.opposite(), false, true));
        else c.set(x, y + 1, z, St.lantern(false));
    }

    /**
     * A bar counter from (x, z) toward {@code along}, {@code length} long, patrons on the {@code patrons} side: the counter is
     * {@code body} blocks topped by nothing (one block is counter height), with a lip of upside-down stairs over the patrons' side
     * row in front, stools (fence + red carpet) beyond.
     */
    public static void bar(Canvas c, int x, int y, int z, Dir along, int length, Dir patrons, String body, String lipStairs, String stoolFence) {
        for (int i = 0; i < length; i++) {
            int cx = x + along.dx * i, cz = z + along.dz * i;
            c.set(cx, y, cz, body);
            int px = cx + patrons.dx, pz = cz + patrons.dz;
            if (i % 2 == 1 && stoolFence != null) {
                c.set(px, y, pz, St.fence(stoolFence));
                c.set(px, y + 1, pz, St.carpet("red"));
            }
        }
        if (lipStairs != null) {
            for (int i = 0; i < length; i++) {
                int cx = x + along.dx * i, cz = z + along.dz * i;
                c.setIfAir(cx, y + 1, cz, St.slabBottom(lipStairs.replace("_stairs", "_slab")));
            }
        }
    }

    /** A pool table 2 × 4 from (x, z) along X or Z: dark wood under green carpet, black pockets at the corners. */
    public static void poolTable(Canvas c, int x, int y, int z, boolean alongX, String wood) {
        int lx = alongX ? 4 : 2, lz = alongX ? 2 : 4;
        for (int i = 0; i < lx; i++) {
            for (int k = 0; k < lz; k++) {
                c.set(x + i, y, z + k, wood);
                boolean corner = (i == 0 || i == lx - 1) && (k == 0 || k == lz - 1);
                c.set(x + i, y + 1, z + k, St.carpet(corner ? "black" : "green"));
            }
        }
    }

    /** A filing cabinet two drawers high at (x, y, z): {@code body} blocks with iron trapdoor drawer fronts in front of them. */
    public static void filing(Canvas c, int x, int y, int z, Dir front, String body) {
        for (int h = 0; h < 2; h++) {
            c.set(x, y + h, z, body);
            c.setIfAir(x + front.dx, y + h, z + front.dz, St.trapdoorAgainst("minecraft:iron_trapdoor", front.opposite()));
        }
    }

    /**
     * A reclining medical chair at (x, y, z), the patient looking {@code looks}: a white seat (quartz stairs), a footrest slab in
     * front, iron-bar headrest frame behind, and a lightning-rod drip stand at its side.
     */
    public static void medicalChair(Canvas c, int x, int y, int z, Dir looks) {
        c.set(x, y, z, St.stairs("minecraft:smooth_quartz_stairs", looks.opposite(), false));
        c.set(x + looks.dx, y, z + looks.dz, St.slabBottom("minecraft:smooth_quartz_slab"));
        c.setIfAir(x - looks.dx, y + 1, z - looks.dz, St.bars());
        c.setIfAir(x - looks.dx, y, z - looks.dz, "minecraft:iron_block");
        Dir side = looks.cw();
        c.setIfAir(x + side.dx, y, z + side.dz, St.rod("minecraft:lightning_rod", Dir.UP));
        c.setIfAir(x + side.dx, y + 1, z + side.dz, St.rod("minecraft:lightning_rod", Dir.UP));
    }

    /** A jukebox nook against a wall: bookshelves either side, a jukebox (block entity) or a note block in the middle, a lantern on it. */
    public static void jukeboxNook(Canvas c, int x, int y, int z, Dir front, boolean entities) {
        Dir side = front.cw();
        c.set(x, y, z, entities ? "minecraft:jukebox[has_record=false]" : "minecraft:note_block");
        c.set(x, y + 1, z, St.lantern(false));
        for (int s : new int[]{-1, 1}) {
            c.set(x + side.dx * s, y, z + side.dz * s, "minecraft:bookshelf");
            c.set(x + side.dx * s, y + 1, z + side.dz * s, St.potted(s < 0 ? "fern" : "red_tulip"));
        }
    }

    /** A fireplace in a wall: a hearth of {@code hearth} blocks round a campfire (lit; block entity) or a soul fire on soul soil. */
    public static void fireplace(Canvas c, int x, int y, int z, Dir front, Family stone, boolean entities) {
        Dir side = front.cw();
        for (int s = -1; s <= 1; s++) {
            int cx = x + side.dx * s, cz = z + side.dz * s;
            c.set(cx, y + 2, cz, stone.block());
            c.setIfAir(cx + front.dx, y + 2, cz + front.dz, stone.stairs(front.opposite(), true));
            if (s != 0) {
                c.set(cx, y, cz, stone.block());
                c.set(cx, y + 1, cz, stone.block());
            }
        }
        if (entities) {
            c.set(x, y, z, St.campfire(true, false, front));
        } else {
            c.set(x, y - 1, z, "minecraft:soul_soil");
            c.set(x, y, z, "minecraft:soul_fire");
        }
        c.carve(x, y + 1, z);
    }

    /**
     * A straight flight of {@code steps} stairs climbing toward {@code up} from floor-level cell (x, y, z), {@code width} wide toward
     * {@code up.cw()}: step k at height {@code y + k}; three cells of headroom carved over every step; under each step but the first
     * an upside-down stair (a smooth soffit) when {@code soffit}. One arrives on the floor at {@code y + steps} one cell past the last.
     */
    public static void flight(Canvas c, int x, int y, int z, Dir up, int steps, int width, String stairs, boolean soffit) {
        Dir across = up.cw();
        for (int k = 0; k < steps; k++) {
            for (int w = 0; w < width; w++) {
                int cx = x + up.dx * k + across.dx * w, cz = z + up.dz * k + across.dz * w;
                c.set(cx, y + k, cz, St.stairs(stairs, up, false));
                for (int h = 1; h <= 3; h++) c.carve(cx, y + k + h, cz);
                if (soffit && k > 0) c.set(cx, y + k - 1, cz, St.stairs(stairs, up.opposite(), true));
            }
        }
    }
}
