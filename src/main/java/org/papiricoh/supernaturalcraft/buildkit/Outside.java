package org.papiricoh.supernaturalcraft.buildkit;

/**
 * What a layout assumes lies off the canvas (pure): what {@link Shapes} joins fences to, what {@link Light} treats as rock and what
 * the quality checks stand on. A floating island has only air around it; a house set on the world's ground has rock under
 * {@code y = -1}.
 */
@FunctionalInterface
public interface Outside {

    String at(int x, int y, int z);

    /** Nothing but air (sky islands, anything built in the void). */
    Outside AIR = (x, y, z) -> St.AIR;

    /** Solid ground up to {@code topY} (inclusive), air above. */
    static Outside groundUpTo(int topY) {
        return (x, y, z) -> y <= topY ? "minecraft:stone" : St.AIR;
    }
}
