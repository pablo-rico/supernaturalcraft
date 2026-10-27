package org.papiricoh.supernaturalcraft.hell.cage;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.St;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lucifer's Cage and the Pit's centre as one kit plan (pure, deterministic: the Pit is the same in every world). Drawn in world
 * coordinates round the origin by {@link CageIsland}, {@link CageSpans} and {@link CageCrown}; what lies off the plan is the
 * Pit's own rock ({@link CageLayout#rock}). Air is planned only where the plan cuts the rock (the gates' tunnels, the road
 * before them) and inside the Cage.
 *
 * <p>{@link #compiled()} is the plan built once and packed by chunk for {@link CageBuilder}, which writes it chunk by chunk
 * during worldgen (and all at once for commands and tests). Anchors: {@code altar}, {@code throne}, {@code gate_<dir>} (feet
 * cells at the inner end of each tunnel). Zone {@code cage}: the Cage's own body ({@link CageCrown#box()}).
 */
public final class CagePlan {

    public static final String NAME = "hell_lucifers_cage";
    /** The rock the plan assumes off the canvas (the Pit's wall). */
    static final String ROCK = "supernaturalcraft:hellstone";
    /** The lowest block the plan may hold (its longest chains under the island). */
    public static final int LOW_Y = 36;

    private CagePlan() {
    }

    /** The whole plan's box. */
    public static Box bounds() {
        return new Box(-CageLayout.REACH, LOW_Y, -CageLayout.REACH, CageLayout.REACH, CageLayout.TOP_Y, CageLayout.REACH);
    }

    /** The Cage's own body (floor, bars, crown), the part {@code cageOnly} writes. */
    public static Box cageBox() {
        return CageCrown.box();
    }

    /** The closed iris plate at (x, z) of the Cage floor. */
    public static String irisState(int x, int z) {
        return CageCrown.irisState(x, z);
    }

    /** The finished canvas (a fresh one each call: tests, the layout dump). */
    public static Canvas canvas() {
        CageKit.init();
        Canvas c = new Canvas(NAME).outside((x, y, z) -> CageLayout.rock(x, y, z) ? ROCK : St.AIR);
        c.zone("cage", CageCrown.box());
        CageIsland.draw(c);
        CageSpans.draw(c);
        CageCrown.draw(c);
        return c.finish();
    }

    // --- packed for the writer -------------------------------------------------------------------------------------------------

    private static volatile Compiled compiled;

    /** The plan built once (about a second) and packed by chunk. */
    public static Compiled compiled() {
        Compiled c = compiled;
        if (c == null) {
            synchronized (CagePlan.class) {
                c = compiled;
                if (c == null) compiled = c = Compiled.of(canvas());
            }
        }
        return c;
    }

    /**
     * The plan by chunk: a palette of states and, for every chunk holding cells, the cells packed in longs
     * ({@code (y - LOW_Y) << 24 | localX << 20 | localZ << 16 | paletteIndex}).
     */
    public record Compiled(List<String> palette, Map<Long, long[]> chunks, Map<String, int[]> anchors) {

        static Compiled of(Canvas canvas) {
            List<String> palette = new ArrayList<>();
            Map<String, Integer> index = new HashMap<>();
            Map<Long, List<Long>> byChunk = new LinkedHashMap<>();
            for (var cell : canvas.cells()) {
                int i = index.computeIfAbsent(cell.state(), s -> {
                    palette.add(s);
                    return palette.size() - 1;
                });
                long packed = (long) (cell.y() - LOW_Y) << 24 | (long) (cell.x() & 15) << 20 | (long) (cell.z() & 15) << 16 | i;
                byChunk.computeIfAbsent(chunk(cell.x() >> 4, cell.z() >> 4), k -> new ArrayList<>()).add(packed);
            }
            Map<Long, long[]> chunks = new LinkedHashMap<>();
            for (Map.Entry<Long, List<Long>> e : byChunk.entrySet()) {
                chunks.put(e.getKey(), e.getValue().stream().mapToLong(Long::longValue).toArray());
            }
            return new Compiled(List.copyOf(palette), Map.copyOf(chunks), Map.copyOf(canvas.anchors()));
        }

        public static long chunk(int cx, int cz) {
            return (long) cx << 32 | cz & 0xFFFFFFFFL;
        }

        public static int chunkX(long key) {
            return (int) (key >> 32);
        }

        public static int chunkZ(long key) {
            return (int) key;
        }

        /** The cells of chunk (cx, cz), packed (empty when the plan has none there). */
        public long[] cells(int cx, int cz) {
            long[] c = chunks.get(chunk(cx, cz));
            return c == null ? new long[0] : c;
        }

        public static int x(int cx, long packed) {
            return (cx << 4) + (int) (packed >> 20 & 15);
        }

        public static int z(int cz, long packed) {
            return (cz << 4) + (int) (packed >> 16 & 15);
        }

        public static int y(long packed) {
            return (int) (packed >> 24) + LOW_Y;
        }

        public static int state(long packed) {
            return (int) (packed & 0xFFFF);
        }

        public int size() {
            int n = 0;
            for (long[] c : chunks.values()) n += c.length;
            return n;
        }
    }
}
