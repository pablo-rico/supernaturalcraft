package org.papiricoh.supernaturalcraft.chain.seat;

import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

/** The one place that picks each faction's stronghold plan (pure). */
public final class SeatPlans {
    private SeatPlans() {}

    public static LayoutPlan plan(ChainFaction faction, long seed) {
        return org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonCatalog.plan(faction, seed);
    }
}
