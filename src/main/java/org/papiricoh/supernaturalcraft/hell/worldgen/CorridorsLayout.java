package org.papiricoh.supernaturalcraft.hell.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Crowley's Corridors: Hell as he ran it, an endless queue. A maze of three-wide stone corridors on a
 * 5×5 grid of junctions, lined with barred cells; some cells keep a chest. Planned as a grid of
 * characters (pure, seeded), built by {@link CorridorsStructure}.
 *
 * <pre>
 *  '#' wall   ' ' air   'B' iron bars   'C' chest   'L' soul lantern (hanging)   'F' floor   'R' roof
 * </pre>
 */
public final class CorridorsLayout {

    public static final int NODES = 5, PITCH = 10, SIZE = NODES * PITCH + 1, HEIGHT = 7;

    private CorridorsLayout() {
    }

    /** [x][y][z] characters for a seed. */
    public static char[][][] plan(long seed) {
        Random rng = new Random(seed);
        char[][][] g = new char[SIZE][HEIGHT][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) g[x][y][z] = y == 0 ? 'F' : y == HEIGHT - 1 ? 'R' : '#';
            }
        }
        // A spanning maze over the junctions (randomised depth-first), plus a few loops.
        boolean[][] seen = new boolean[NODES][NODES];
        List<int[]> edges = new ArrayList<>();
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{rng.nextInt(NODES), rng.nextInt(NODES)});
        seen[stack.peek()[0]][stack.peek()[1]] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!stack.isEmpty()) {
            int[] n = stack.peek();
            List<int[]> next = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = n[0] + d[0], nz = n[1] + d[1];
                if (nx >= 0 && nz >= 0 && nx < NODES && nz < NODES && !seen[nx][nz]) next.add(new int[]{nx, nz});
            }
            if (next.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] pick = next.get(rng.nextInt(next.size()));
            seen[pick[0]][pick[1]] = true;
            edges.add(new int[]{n[0], n[1], pick[0], pick[1]});
            stack.push(pick);
        }
        for (int i = 0; i < 4; i++) {
            int x = rng.nextInt(NODES - 1), z = rng.nextInt(NODES);
            if (rng.nextBoolean()) edges.add(new int[]{x, z, x + 1, z});
            else edges.add(new int[]{z, x, z, x + 1});
        }
        for (int nx = 0; nx < NODES; nx++) {
            for (int nz = 0; nz < NODES; nz++) {
                carve(g, centre(nx) - 1, centre(nz) - 1, centre(nx) + 1, centre(nz) + 1);
                g[centre(nx)][HEIGHT - 2][centre(nz)] = 'L';
            }
        }
        List<int[]> shuffled = new ArrayList<>(edges);
        Collections.shuffle(shuffled, rng);
        for (int[] e : shuffled) corridor(g, e, rng);
        // Two ways in, through the outer wall, from opposite sides.
        int a = centre(rng.nextInt(NODES)), b = centre(rng.nextInt(NODES));
        carve(g, 0, a - 1, centre(0), a + 1);
        carve(g, centre(NODES - 1), b - 1, SIZE - 1, b + 1);
        return g;
    }

    public static int centre(int node) {
        return node * PITCH + PITCH / 2;
    }

    private static void carve(char[][][] g, int x0, int z0, int x1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                for (int y = 1; y <= HEIGHT - 3; y++) {
                    if (g[x][y][z] == '#') g[x][y][z] = ' ';
                }
            }
        }
    }

    /** A corridor between two junctions, with barred cells set into its walls. */
    private static void corridor(char[][][] g, int[] e, Random rng) {
        int ax = centre(e[0]), az = centre(e[1]), bx = centre(e[2]), bz = centre(e[3]);
        boolean alongX = az == bz;
        carve(g, ax + (alongX ? 0 : -1), az + (alongX ? -1 : 0), bx + (alongX ? 0 : 1), bz + (alongX ? 1 : 0));
        int from = (alongX ? Math.min(ax, bx) : Math.min(az, bz)) + 3, to = (alongX ? Math.max(ax, bx) : Math.max(az, bz)) - 3;
        for (int t = from; t <= to; t += 4) {
            for (int side : new int[]{-1, 1}) {
                if (rng.nextFloat() > 0.75f) continue;
                cell(g, alongX, t, alongX ? az : ax, side, rng);
            }
        }
    }

    /** A 3×3 cell behind bars, beside the corridor at position {@code t} along it. */
    private static void cell(char[][][] g, boolean alongX, int t, int line, int side, Random rng) {
        for (int along = -1; along <= 1; along++) {
            for (int depth = 2; depth <= 4; depth++) {
                int x = alongX ? t + along : line + side * depth, z = alongX ? line + side * depth : t + along;
                if (x <= 0 || z <= 0 || x >= SIZE - 1 || z >= SIZE - 1) return;
                for (int y = 1; y <= 3; y++) {
                    char c = depth == 2 ? 'B' : ' ';
                    if (g[x][y][z] == '#') g[x][y][z] = c;
                }
            }
        }
        if (rng.nextFloat() < 0.3f) {
            int x = alongX ? t : line + side * 4, z = alongX ? line + side * 4 : t;
            if (g[x][1][z] == ' ') g[x][1][z] = 'C';
        }
    }
}
