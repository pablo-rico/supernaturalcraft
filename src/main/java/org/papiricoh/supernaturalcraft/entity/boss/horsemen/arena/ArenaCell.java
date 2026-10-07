package org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena;

/**
 * One block of a Horseman's ground, relative to the arena's centre column: {@code dy} counts from the surface there
 * (0 replaces the surface block, 1 is the block above it, -1 the one below). {@code block} is a block state string
 * ({@code minecraft:wheat[age=7]}), so the plans stay pure and testable.
 */
public record ArenaCell(int dx, int dy, int dz, String block) {

    public int distanceSq() {
        return dx * dx + dz * dz;
    }
}
