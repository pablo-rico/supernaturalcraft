package org.papiricoh.supernaturalcraft.structure;

/** The y of the topmost solid block of a column: from the chunk generator at worldgen, or a live level. */
@FunctionalInterface
public interface HeightSampler {

    int top(int x, int z);
}
