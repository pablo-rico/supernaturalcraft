package org.papiricoh.supernaturalcraft.chain.seat.tower;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.entity.boss.dick.BoardroomGround;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.ArrayList;
import java.util.List;

/**
 * The Leviathans' seat in the Chain of Command: the <b>SucroCorp tower</b> (pure plan, deterministic per seed, no block entities;
 * loot chests go on the {@code vault_<n>} anchors after the plan is written).
 *
 * <p>Origin = the ground at the centre of the plaza ({@code y = 0} the first air above the paving, {@code y = -1} the paving);
 * +z is the approach (south), as in {@code DungeonAnchors}. The plan fits in {@link #RADIUS} round the origin, from
 * {@code -DEPTH} to {@code +HEIGHT}; the seat builder must clear that box first (rooms that only hold air are planned as air,
 * but nothing outside the plan is). A 71 × 71 plaza (a lettered gateway on the south edge, reflecting pools, planters, lamp
 * posts) round a 41 × 41 glass tower centred {@link #TOWER_Z} north of the origin, three sections stepping in as it rises, with
 * a lettered crown, a helipad and an antenna, about 150 blocks in all.
 *
 * <p>Floors ({@link #FLOORS}): a stair core in the south-east corner climbs every storey; only the playable floors open on it,
 * the rest are sealed, lit offices seen from outside. The top floor is Dick Roman's boardroom, built so it can serve in place
 * as the arena of {@code BoardroomGround} (same table, same centre, a clear disc of {@link #BOARDROOM_RADIUS}).
 *
 * <p>Anchors: the {@code DungeonAnchors} contract ({@code entrance}, {@code arrival}, {@code lord}, {@code commander_1/2},
 * {@code throne_room}, {@code guard_<room>_<n>}, {@code vault_<n>}) plus {@link #BOARDROOM}, {@link #BOARDROOM_DOOR} and
 * {@link #DICK_SEAT}.
 */
public final class SucroTower {

    private SucroTower() {
    }

    /** Horizontal half-extent of the whole plan (plaza included). */
    public static final int RADIUS = 36;
    /** Above the origin: the antenna's tip is at {@code HEIGHT - 1}. */
    public static final int HEIGHT = 150;
    /** Below the origin (pool basins, foundations). */
    public static final int DEPTH = 3;

    /** The tower's centre line: it stands this far north (negative z) of the plaza's centre, the forecourt to the south. */
    public static final int TOWER_Z = -6;

    /** The layout name (the review dump adds the seed). */
    public static final String NAME = "chain_sucro_tower";

    /** The centre of the boardroom floor (feet cell): the arena's centre for {@code BoardroomGround}. */
    public static final String BOARDROOM = "boardroom";
    /** The lower half of the boardroom's (iron) door, the one way in. */
    public static final String BOARDROOM_DOOR = "boardroom_door";
    /** Dick's place at the head of the table (north side, facing the room, south); {@code lord} is the same cell. */
    public static final String DICK_SEAT = "dick_seat";

    /** The boardroom's feet level ({@code BOARDROOM_FLOOR - 1} is its floor). */
    public static final int BOARDROOM_FLOOR = 110;
    /** The boardroom's centre (x, z). */
    public static final int BOARDROOM_X = 0, BOARDROOM_Z = TOWER_Z - 3;
    /** The clear disc round the boardroom's centre: flat floor, nothing standing in it but the table and its chairs. */
    public static final int BOARDROOM_RADIUS = 16;
    /** Air cells over the boardroom floor (the ceiling is at {@code BOARDROOM_FLOOR + BOARDROOM_HEADROOM}). */
    public static final int BOARDROOM_HEADROOM = 9;

    /** A storey: its room name, its feet level, the height from its floor block to the next one's, whether it opens on the stair. */
    public record Floor(String room, int y, int height, boolean playable) {

        /** The floor block's level. */
        public int floorY() {
            return y - 1;
        }

        /** The last air row. */
        public int topY() {
            return y + height - 2;
        }
    }

    /** Every storey bottom to top. */
    public static final List<Floor> FLOORS = floors();

    private static List<Floor> floors() {
        List<Floor> out = new ArrayList<>();
        out.add(new Floor("lobby", 0, 10, true));
        String[] rooms = new String[20];
        rooms[0] = "office_a";
        rooms[2] = "office_b";
        rooms[4] = "archive";
        rooms[7] = "lab";
        rooms[10] = "factory";
        rooms[13] = "canteen";
        rooms[16] = "executive";
        for (int k = 0; k < 20; k++) {
            int y = 10 + 5 * k;
            out.add(new Floor(rooms[k] != null ? rooms[k] : "sealed_" + y, y, 5, rooms[k] != null));
        }
        out.add(new Floor("boardroom", BOARDROOM_FLOOR, BOARDROOM_HEADROOM + 1, true));
        return List.copyOf(out);
    }

    /** The storey by room name. */
    public static Floor floor(String room) {
        for (Floor f : FLOORS) if (f.room().equals(room)) return f;
        throw new IllegalArgumentException(room);
    }

    /** How many vault spots the seed gives (4 to 6). */
    public static int vaults(long seed) {
        return 4 + Math.floorMod(TowerKit.salt(seed) * 0x2545F491 + 7, 3);
    }

    public static LayoutPlan plan(long seed) {
        return canvas(seed).toPlan();
    }

    /** The tower as a finished kit canvas (tests and the layout dump). */
    public static Canvas canvas(long seed) {
        return new TowerDraw(seed).draw();
    }

    /** The whole plan's box. */
    public static Box bounds() {
        return new Box(-RADIUS, -DEPTH, -RADIUS, RADIUS, HEIGHT - 1, RADIUS);
    }

    /** The boardroom's centre as a feet cell (x, y, z). */
    public static int[] boardroom() {
        return new int[]{BOARDROOM_X, BOARDROOM_FLOOR, BOARDROOM_Z};
    }

    /** Dick's place: behind the middle of the table's north side ({@code BoardroomGround}'s head chair). */
    public static int[] dickSeat() {
        return new int[]{BOARDROOM_X, BOARDROOM_FLOOR, BOARDROOM_Z + BoardroomGround.TABLE_Z - 2};
    }
}
