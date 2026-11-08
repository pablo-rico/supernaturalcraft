package org.papiricoh.supernaturalcraft.hands.reliquary;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.St;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Drawing helpers the reliquaries' plans share (v0.42), pure. */
final class ReliquaryKit {

    private ReliquaryKit() {
    }

    /** Plans air over a box (rooms, tunnels). */
    static void hollow(Canvas c, int x0, int y0, int z0, int x1, int y1, int z1) {
        c.carve(new Box(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1)));
    }

    /**
     * Walls in every planned air cell inside {@code region}: each unplanned neighbour becomes floor (under it), ceiling (over
     * it) or wall (beside it). Carved rooms and tunnels dug into the ground get their lining this way.
     */
    static void wrap(Canvas c, Box region, Brush wall, Brush floor, Brush ceiling) {
        List<int[]> air = new ArrayList<>();
        for (Map.Entry<Long, String> e : c.map().entrySet()) {
            int x = Canvas.kx(e.getKey()), y = Canvas.ky(e.getKey()), z = Canvas.kz(e.getKey());
            if (region.contains(x, y, z) && St.isAir(e.getValue())) air.add(new int[]{x, y, z});
        }
        int[][] around = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, -1, 0}, {0, 1, 0}};
        for (int[] a : air) {
            for (int[] d : around) {
                int x = a[0] + d[0], y = a[1] + d[1], z = a[2] + d[2];
                if (c.has(x, y, z)) continue;
                Brush b = d[1] < 0 ? floor : d[1] > 0 ? ceiling : wall;
                c.set(x, y, z, b.at(x, y, z));
            }
        }
    }
}
