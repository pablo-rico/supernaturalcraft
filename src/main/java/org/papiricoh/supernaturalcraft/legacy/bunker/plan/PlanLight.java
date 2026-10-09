package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import java.util.ArrayDeque;
import java.util.Map;

/**
 * Block light over the plan (pure): every emitter floods outward losing 1 per step through air and thin blocks and 2 through
 * partial ones (slabs, stairs…); opaque cubes stop it. Off the plan is rock below the surface. Sky light is ignored: the bunker
 * is underground, and the test only asks about covered cells.
 */
public final class PlanLight {

    private final int x0, y0, z0, nx, ny, nz;
    private final byte[] light;

    private PlanLight(int x0, int y0, int z0, int nx, int ny, int nz) {
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        this.light = new byte[nx * ny * nz];
    }

    public static PlanLight of(Map<Long, String> cells) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (long k : cells.keySet()) {
            minX = Math.min(minX, Plan.kx(k));
            maxX = Math.max(maxX, Plan.kx(k));
            minY = Math.min(minY, Plan.ky(k));
            maxY = Math.max(maxY, Plan.ky(k));
            minZ = Math.min(minZ, Plan.kz(k));
            maxZ = Math.max(maxZ, Plan.kz(k));
        }
        PlanLight out = new PlanLight(minX, minY, minZ, maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
        out.flood(cells);
        return out;
    }

    private int index(int x, int y, int z) {
        int i = x - x0, j = y - y0, k = z - z0;
        if (i < 0 || j < 0 || k < 0 || i >= nx || j >= ny || k >= nz) return -1;
        return (i * ny + j) * nz + k;
    }

    /** The block light at a point (0 off the plan). */
    public int at(int x, int y, int z) {
        int i = index(x, y, z);
        return i < 0 ? 0 : light[i];
    }

    private void flood(Map<Long, String> cells) {
        int[] cost = new int[light.length];
        for (int i = 0; i < cost.length; i++) cost[i] = 16;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (Map.Entry<Long, String> e : cells.entrySet()) {
            long k = e.getKey();
            int x = Plan.kx(k), y = Plan.ky(k), z = Plan.kz(k);
            int i = index(x, y, z);
            cost[i] = Kinds.lightCost(e.getValue());
            int em = Kinds.emission(e.getValue());
            if (em > light[i]) {
                light[i] = (byte) em;
                queue.add(new int[]{x, y, z});
            }
        }
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int l = light[index(p[0], p[1], p[2])];
            for (int[] d : dirs) {
                int qx = p[0] + d[0], qy = p[1] + d[1], qz = p[2] + d[2];
                int j = index(qx, qy, qz);
                if (j < 0) continue;
                int next = l - cost[j];
                if (next > light[j]) {
                    light[j] = (byte) next;
                    queue.add(new int[]{qx, qy, qz});
                }
            }
        }
    }
}
