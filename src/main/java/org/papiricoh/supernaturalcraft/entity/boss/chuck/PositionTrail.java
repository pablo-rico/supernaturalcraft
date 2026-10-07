package org.papiricoh.supernaturalcraft.entity.boss.chuck;

/**
 * Where a hunter has been over the last few seconds: a ring buffer of timed samples, for Backspace ("you were there five
 * seconds ago; you are there again"). Pure.
 */
public final class PositionTrail {

    /** One sample: when, and where (feet). */
    public record Sample(long tick, double x, double y, double z) {
    }

    private final Sample[] ring;
    private int head, size;

    /** Holds the last {@code capacity} samples. */
    public PositionTrail(int capacity) {
        ring = new Sample[Math.max(2, capacity)];
    }

    /** A trail long enough for {@code ticks} sampled every {@code every} ticks. */
    public static PositionTrail covering(int ticks, int every) {
        return new PositionTrail(ticks / Math.max(1, every) + 2);
    }

    public void record(long tick, double x, double y, double z) {
        ring[head] = new Sample(tick, x, y, z);
        head = (head + 1) % ring.length;
        size = Math.min(size + 1, ring.length);
    }

    public int size() {
        return size;
    }

    public void clear() {
        size = 0;
        head = 0;
    }

    /** The {@code i}-th newest sample (0 = newest). */
    public Sample newest(int i) {
        if (i < 0 || i >= size) throw new IndexOutOfBoundsException(i);
        return ring[Math.floorMod(head - 1 - i, ring.length)];
    }

    /**
     * Where they were {@code ticks} ago: the newest sample at least that old, or the oldest one there is if the trail is
     * younger than that; null if empty.
     */
    public Sample back(long now, int ticks) {
        if (size == 0) return null;
        for (int i = 0; i < size; i++) {
            Sample s = newest(i);
            if (s.tick <= now - ticks) return s;
        }
        return newest(size - 1);
    }

    /** The index (for {@link #newest}) of {@link #back}'s answer, so a caller can try older or newer ones if it is blocked. */
    public int backIndex(long now, int ticks) {
        for (int i = 0; i < size; i++) {
            if (newest(i).tick <= now - ticks) return i;
        }
        return size - 1;
    }
}
