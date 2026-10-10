package org.papiricoh.supernaturalcraft.memory.scenes;

/**
 * Someone standing in a staged memory (v0.18, pure contract from the foundations): spawned as a {@code MemoryFigureEntity}.
 *
 * @param dx     position relative to the stage centre, in blocks ({@code dy = 0} is the first air layer above the stage floor)
 * @param dy     see dx
 * @param dz     see dx
 * @param yaw    facing, in degrees (0 = south, like an entity's yaw)
 * @param figure who: an entity type id ({@code supernaturalcraft:lucifer}, {@code minecraft:wolf}), {@code @owner} (the hunter,
 *               with their skin) or {@code @ally:dean|sam|castiel|bobby}
 * @param pose   a pose name the figure renderer knows ({@code stand}, {@code kneel}, {@code strike}, {@code fallen}, {@code sit},
 *               {@code offer}...; unknown poses stand)
 * @param scale  size multiplier (1 = natural)
 * @param focus  the one figure to touch to gather the memory (at most one per scene)
 */
public record Figure(double dx, double dy, double dz, float yaw, String figure, String pose, float scale, boolean focus) {
}
