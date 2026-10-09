package org.papiricoh.supernaturalcraft.legacy.gen;

import java.util.UUID;

/**
 * Seeds for the Men of Letters' generators (v0.17), pure: everything generated for a hunter follows from the world seed, their
 * UUID, the generator and an index, so the same hunter in the same world always works out the same formulas, rites and cases.
 */
public final class GenSeed {

    private GenSeed() {
    }

    /** The seed of a hunter's {@code index}-th result of {@code generator}. */
    public static long of(long worldSeed, UUID player, String generator, int index) {
        long h = mix(worldSeed ^ 0x5DEECE66DL);
        h = mix(h ^ player.getMostSignificantBits());
        h = mix(h ^ player.getLeastSignificantBits());
        h = mix(h ^ generator.hashCode() * 0x9E3779B97F4A7C15L);
        return mix(h ^ (index + 1L) * 0xBF58476D1CE4E5B9L);
    }

    /** SplitMix64's finaliser: a good spread for neighbouring inputs. */
    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A small deterministic random stream (SplitMix64), so pure code needs no Minecraft {@code RandomSource}. */
    public static final class Rng {
        private long state;

        public Rng(long seed) {
            this.state = seed;
        }

        public long nextLong() {
            state += 0x9E3779B97F4A7C15L;
            return mix(state);
        }

        /** 0 (inclusive) … bound (exclusive). */
        public int nextInt(int bound) {
            if (bound <= 1) return 0;
            return (int) Math.floorMod(nextLong(), (long) bound);
        }

        /** min … max, both inclusive. */
        public int range(int min, int max) {
            return max <= min ? min : min + nextInt(max - min + 1);
        }

        /** 0 … 1. */
        public double nextDouble() {
            return (nextLong() >>> 11) * 0x1.0p-53;
        }

        public boolean chance(double p) {
            return nextDouble() < p;
        }

        public <T> T pick(java.util.List<T> list) {
            return list.get(nextInt(list.size()));
        }

        /** An index by weight. */
        public int weighted(int... weights) {
            int total = 0;
            for (int w : weights) total += Math.max(0, w);
            int r = nextInt(Math.max(1, total));
            for (int i = 0; i < weights.length; i++) {
                r -= Math.max(0, weights[i]);
                if (r < 0) return i;
            }
            return weights.length - 1;
        }
    }
}
