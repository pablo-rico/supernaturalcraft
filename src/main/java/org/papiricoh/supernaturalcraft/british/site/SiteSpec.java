package org.papiricoh.supernaturalcraft.british.site;

import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.function.IntFunction;

/**
 * One of season 12's unique places (v0.41): the British compound or Kelly Kline's safe house. What {@link SiteBuilder} needs
 * to find it a spot (a ring of candidates round the world's centre, dry and level enough), clear it and write its plan. The
 * footprint is in plan space before turning (inclusive), as the anchor classes give it.
 *
 * @param id       the name it is saved under ({@link BritishSitesData})
 * @param salt     mixed into the world seed for its candidates and turn
 * @param plan     its blueprint turned {@code q} quarter turns
 */
public record SiteSpec(String id, long salt, int minX, int maxX, int minZ, int maxZ, int bottomY, int groundY, int topY,
                       int ringMin, int ringStep, int candidates, int maxRelief, IntFunction<LayoutPlan> plan) {

    /** The widest reach of the footprint from its origin, whatever the turn. */
    public int reach() {
        return Math.max(Math.max(-minX, maxX), Math.max(-minZ, maxZ));
    }
}
