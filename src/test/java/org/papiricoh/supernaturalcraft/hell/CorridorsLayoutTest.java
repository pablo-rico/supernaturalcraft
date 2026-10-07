package org.papiricoh.supernaturalcraft.hell;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.hell.worldgen.CorridorsLayout;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CorridorsLayoutTest {

    private static boolean walkable(char c) {
        return c == ' ' || c == 'C';
    }

    @Test
    void theSameSeedBuildsTheSameMaze() {
        assertTrue(Arrays.deepEquals(CorridorsLayout.plan(42), CorridorsLayout.plan(42)));
    }

    @Test
    void everyJunctionIsReachable() {
        for (long seed = 0; seed < 20; seed++) {
            char[][][] g = CorridorsLayout.plan(seed);
            int n = CorridorsLayout.SIZE;
            boolean[][] seen = new boolean[n][n];
            Deque<int[]> queue = new ArrayDeque<>();
            int s = CorridorsLayout.centre(0);
            queue.add(new int[]{s, s});
            seen[s][s] = true;
            while (!queue.isEmpty()) {
                int[] c = queue.poll();
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int x = c[0] + d[0], z = c[1] + d[1];
                    if (x < 0 || z < 0 || x >= n || z >= n || seen[x][z] || !walkable(g[x][1][z]) || !walkable(g[x][2][z])) continue;
                    seen[x][z] = true;
                    queue.add(new int[]{x, z});
                }
            }
            for (int i = 0; i < CorridorsLayout.NODES; i++) {
                for (int j = 0; j < CorridorsLayout.NODES; j++) {
                    assertTrue(seen[CorridorsLayout.centre(i)][CorridorsLayout.centre(j)], "seed " + seed + ": junction " + i + "," + j + " unreachable");
                }
            }
        }
    }
}
