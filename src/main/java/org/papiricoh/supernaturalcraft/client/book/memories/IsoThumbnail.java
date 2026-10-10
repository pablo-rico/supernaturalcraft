package org.papiricoh.supernaturalcraft.client.book.memories;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A memory's scene seen from above at an angle (v0.18, pure): every block a small isometric cube in its map colour (top light,
 * the two sides shaded), drawn back to front into an ARGB image. What the book's Memories pages show of a scene.
 * <p>One block is {@link #TILE} pixels across its top's diagonal and {@link #STEP} pixels tall.
 */
public final class IsoThumbnail {

    public static final int TILE = 4, STEP = 2;

    /** A block of the scene: its position and its colour (RGB). */
    public record Voxel(int x, int y, int z, int rgb) {
    }

    /** The picture: {@code width}×{@code height} ARGB pixels, row by row. */
    public record Image(int width, int height, int[] pixels) {
        public int at(int x, int y) {
            return pixels[y * width + x];
        }
    }

    private IsoThumbnail() {
    }

    /** Draws the voxels; null if there are none. Hidden faces are skipped (a block under another, or behind a neighbour). */
    public static Image draw(List<Voxel> voxels) {
        if (voxels.isEmpty()) return null;
        int minSx = Integer.MAX_VALUE, maxSx = Integer.MIN_VALUE, minSy = Integer.MAX_VALUE, maxSy = Integer.MIN_VALUE;
        java.util.Set<Long> filled = new java.util.HashSet<>();
        for (Voxel v : voxels) {
            int sx = sx(v), sy = sy(v);
            minSx = Math.min(minSx, sx);
            maxSx = Math.max(maxSx, sx + TILE);
            minSy = Math.min(minSy, sy);
            maxSy = Math.max(maxSy, sy + TILE / 2 + STEP);
            filled.add(key(v.x(), v.y(), v.z()));
        }
        int w = maxSx - minSx + 1, h = maxSy - minSy + 1;
        int[] px = new int[w * h];
        List<Voxel> order = new ArrayList<>(voxels);
        // Back to front: far (small x+z) first, low before high.
        order.sort(Comparator.comparingInt((Voxel v) -> v.x() + v.z()).thenComparingInt(Voxel::y));
        for (Voxel v : order) {
            int ox = sx(v) - minSx, oy = sy(v) - minSy;
            boolean top = !filled.contains(key(v.x(), v.y() + 1, v.z()));
            boolean left = !filled.contains(key(v.x(), v.y(), v.z() + 1));
            boolean right = !filled.contains(key(v.x() + 1, v.y(), v.z()));
            if (!top && !left && !right) continue;
            cube(px, w, h, ox, oy, v.rgb(), top, left, right);
        }
        return new Image(w, h, px);
    }

    /** Screen x of a block's top diamond's left corner. */
    static int sx(Voxel v) {
        return (v.x() - v.z()) * (TILE / 2);
    }

    /** Screen y of its top diamond's top corner. */
    static int sy(Voxel v) {
        return (v.x() + v.z()) * (TILE / 4) - v.y() * STEP;
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    private static void cube(int[] px, int w, int h, int ox, int oy, int rgb, boolean top, boolean left, boolean right) {
        int half = TILE / 2;
        if (top) {
            // The top: TILE wide, TILE/2 tall (at this size a diamond is a flat lozenge of pixels).
            for (int dy = 0; dy < half; dy++) {
                for (int dx = 0; dx < TILE; dx++) put(px, w, h, ox + dx, oy + dy, shade(rgb, dx == 0 || dx == TILE - 1 ? 1.0f : 1.1f));
            }
        }
        // The two sides under the top, each half the tile wide and STEP tall.
        for (int dy = 0; dy < STEP; dy++) {
            for (int dx = 0; dx < half; dx++) {
                if (left) put(px, w, h, ox + dx, oy + half + dy, shade(rgb, 0.8f));
                if (right) put(px, w, h, ox + half + dx, oy + half + dy, shade(rgb, 0.62f));
            }
        }
    }

    private static void put(int[] px, int w, int h, int x, int y, int argb) {
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        px[y * w + x] = argb;
    }

    /** {@code rgb} lightened or darkened by {@code k}, opaque. */
    static int shade(int rgb, float k) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * k)), g = Math.min(255, Math.round(((rgb >> 8) & 255) * k)),
                b = Math.min(255, Math.round((rgb & 255) * k));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
