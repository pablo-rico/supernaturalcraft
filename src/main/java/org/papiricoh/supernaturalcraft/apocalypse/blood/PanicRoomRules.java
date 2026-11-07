package org.papiricoh.supernaturalcraft.apocalypse.blood;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Whether a room is a Panic Room (v0.40, pure, tested by {@code PanicRoomRulesTest}): a flood from the cell inside the door
 * through everything that is not a wall must close within {@link #MAX_CELLS} cells and {@link #MAX_SPAN} on every axis (a
 * leak means an opening), and one of the cells it reaches must hold a devil's trap painted on the ceiling. Walls, floor and
 * ceiling are {@code #panic_room_walls} (iron, and the door itself). Cells are packed {@code x, y, z} (see {@link #pack}).
 */
public final class PanicRoomRules {

    /** The most cells the inside may have (7×5×7). */
    public static final int MAX_CELLS = 7 * 5 * 7;
    /** The widest the inside may be on any axis. */
    public static final int MAX_SPAN = 7;

    /** What the room's blocks are, asked one cell at a time. */
    public interface World {
        /** An iron wall (or the door): the flood stops there. */
        boolean wall(int x, int y, int z);

        /** A devil's trap painted on the ceiling. */
        boolean ceilingTrap(int x, int y, int z);
    }

    public enum Verdict {
        OK, LEAKS, NO_TRAP
    }

    /** The result: the verdict and the cells inside (empty unless {@code OK}). */
    public record Room(Verdict verdict, Set<Long> cells) {
        public boolean ok() {
            return verdict == Verdict.OK;
        }

        public boolean contains(int x, int y, int z) {
            return cells.contains(pack(x, y, z));
        }
    }

    private PanicRoomRules() {
    }

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public static Room check(World w, int sx, int sy, int sz) {
        if (w.wall(sx, sy, sz)) return new Room(Verdict.LEAKS, Set.of());
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{sx, sy, sz});
        seen.add(pack(sx, sy, sz));
        int minX = sx, maxX = sx, minY = sy, maxY = sy, minZ = sz, maxZ = sz;
        boolean trap = false;
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            trap |= w.ceilingTrap(c[0], c[1], c[2]);
            for (int[] d : dirs) {
                int x = c[0] + d[0], y = c[1] + d[1], z = c[2] + d[2];
                if (w.wall(x, y, z) || !seen.add(pack(x, y, z))) continue;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
                if (seen.size() > MAX_CELLS || maxX - minX >= MAX_SPAN || maxY - minY >= MAX_SPAN || maxZ - minZ >= MAX_SPAN) {
                    return new Room(Verdict.LEAKS, Set.of());
                }
                queue.add(new int[]{x, y, z});
            }
        }
        return trap ? new Room(Verdict.OK, seen) : new Room(Verdict.NO_TRAP, Set.of());
    }
}
