package org.papiricoh.supernaturalcraft.buildkit;

import java.util.ArrayDeque;

/**
 * Light over a canvas (pure), as the game's night-time block light: every emitter floods outward losing 1 per step through air
 * and thin blocks and 2 through partial ones (slabs, stairs, leaves…); opaque cubes stop it. Off the canvas the
 * {@link Canvas#outside} decides (air passes light, anything else stops it). Sky light is left out on purpose: an interior must be
 * lit at night.
 *
 * <p>{@link #of} floods the canvas's bounds grown by {@code margin} cells on every side.
 */
public final class Light {

    private final int x0, y0, z0, nx, ny, nz;
    private final byte[] light;

    private Light(int x0, int y0, int z0, int nx, int ny, int nz) {
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        this.light = new byte[nx * ny * nz];
    }

    public static Light of(Canvas canvas) {
        return of(canvas, 2);
    }

    public static Light of(Canvas canvas, int margin) {
        Box b = canvas.bounds().grow(margin);
        Light out = new Light(b.x0(), b.y0(), b.z0(), b.sizeX(), b.sizeY(), b.sizeZ());
        out.flood(canvas);
        return out;
    }

    private int index(int x, int y, int z) {
        int i = x - x0, j = y - y0, k = z - z0;
        if (i < 0 || j < 0 || k < 0 || i >= nx || j >= ny || k >= nz) return -1;
        return (i * ny + j) * nz + k;
    }

    /** The block light at a cell (0 outside the flooded box). */
    public int at(int x, int y, int z) {
        int i = index(x, y, z);
        return i < 0 ? 0 : light[i];
    }

    private void flood(Canvas canvas) {
        int[] cost = new int[light.length];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                for (int k = 0; k < nz; k++) {
                    int x = x0 + i, y = y0 + j, z = z0 + k;
                    String s = canvas.world(x, y, z);
                    int idx = (i * ny + j) * nz + k;
                    cost[idx] = Kinds.lightCost(s);
                    int em = Kinds.emission(s);
                    if (em > 0) {
                        light[idx] = (byte) em;
                        queue.add(new int[]{x, y, z});
                    }
                }
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
                int next = l - Math.max(1, cost[j]);
                if (next > light[j]) {
                    light[j] = (byte) next;
                    queue.add(new int[]{qx, qy, qz});
                }
            }
        }
    }

    /** Whether something opaque roofs the cell within {@code reach} blocks above it (an interior, a porch, under a tree). */
    public static boolean covered(Canvas canvas, int x, int y, int z, int reach) {
        for (int yy = y + 1; yy <= y + reach; yy++) {
            String s = canvas.world(x, yy, z);
            if (Kinds.opaqueCube(s) && !Kinds.air(s)) return true;
            if (Kinds.slab(s) || Kinds.stairs(s)) return true;
        }
        return false;
    }
}
