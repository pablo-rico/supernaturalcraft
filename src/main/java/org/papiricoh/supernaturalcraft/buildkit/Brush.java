package org.papiricoh.supernaturalcraft.buildkit;

/**
 * A block state chosen by position (pure): every surface of a build is painted with a brush rather than a single block, so
 * mixes, gradients and weathering are one argument away. A brush must be deterministic: the same cell always gets the same state.
 */
@FunctionalInterface
public interface Brush {

    String at(int x, int y, int z);

    /** The same block everywhere. */
    static Brush of(String state) {
        String s = St.full(state);
        return (x, y, z) -> s;
    }

    /** This brush, except where {@code chance} (per cell, salted by {@code seed}) picks {@code other}. */
    default Brush sprinkle(Brush other, double chance, int seed) {
        return (x, y, z) -> Noise.hash01(x, y, z, seed) < chance ? other.at(x, y, z) : at(x, y, z);
    }

    /** Applies {@code f} to what this brush picks (e.g. turn a block id into its stairs). */
    default Brush map(java.util.function.UnaryOperator<String> f) {
        return (x, y, z) -> f.apply(at(x, y, z));
    }
}
