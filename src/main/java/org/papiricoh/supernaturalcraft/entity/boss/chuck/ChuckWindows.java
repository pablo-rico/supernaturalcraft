package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import java.util.ArrayList;
import java.util.List;

/**
 * The bookkeeping of the Author's damage windows (pure): when the current window closes, and which of his four rings'
 * weak points are broken in chapter 4.
 */
public final class ChuckWindows {

    private long until = Long.MIN_VALUE;

    /** Opens (or lengthens) the window to last at least {@code ticks} from {@code now}. */
    public void open(long now, int ticks) {
        until = Math.max(until, now + ticks);
    }

    public boolean isOpen(long now) {
        return now < until;
    }

    public void close() {
        until = Long.MIN_VALUE;
    }

    public long until() {
        return until;
    }

    public void restore(long until) {
        this.until = until;
    }

    /** What breaking a ring's weak point did. */
    public enum Break { NOTHING, RING, ALL }

    /** Chapter 4: three weak points on each of four rings; a ring with all three broken is broken, and repairs later. */
    public static final class Rings {
        private final boolean[][] broken = new boolean[ChuckBones.RINGS][ChuckBones.NODES];
        private final long[] repairAt = new long[ChuckBones.RINGS];

        public boolean broken(int ring, int node) {
            return broken[ring][node];
        }

        public boolean ringBroken(int ring) {
            for (boolean b : broken[ring]) if (!b) return false;
            return true;
        }

        public int ringsBroken() {
            int n = 0;
            for (int r = 0; r < ChuckBones.RINGS; r++) if (ringBroken(r)) n++;
            return n;
        }

        /** Breaks a weak point; a ring it completes repairs at {@code now + repair}, unless it was the last ring. */
        public Break breakNode(int ring, int node, long now, int repair) {
            if (broken[ring][node]) return Break.NOTHING;
            broken[ring][node] = true;
            if (!ringBroken(ring)) return Break.NOTHING;
            if (ringsBroken() == ChuckBones.RINGS) return Break.ALL;
            repairAt[ring] = now + repair;
            return Break.RING;
        }

        /** Rings whose repair is due at {@code now}: they are whole again when this returns. */
        public List<Integer> repairDue(long now) {
            List<Integer> out = new ArrayList<>();
            for (int r = 0; r < ChuckBones.RINGS; r++) {
                if (ringBroken(r) && ringsBroken() < ChuckBones.RINGS && now >= repairAt[r]) {
                    java.util.Arrays.fill(broken[r], false);
                    out.add(r);
                }
            }
            return out;
        }

        /** Everything whole again (after the core's window, or a new chapter 4). */
        public void reset() {
            for (boolean[] ring : broken) java.util.Arrays.fill(ring, false);
            java.util.Arrays.fill(repairAt, 0);
        }

        /** How broken, 0-1, for the renderer's cracks. */
        public float brokenShare() {
            int n = 0;
            for (boolean[] ring : broken) for (boolean b : ring) if (b) n++;
            return n / (float) (ChuckBones.RINGS * ChuckBones.NODES);
        }
    }
}
