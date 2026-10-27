package org.papiricoh.supernaturalcraft.chain.seat;

import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonAnchors;
import org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonCatalog;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

/** The one place that picks each faction's stronghold plan and its extent (pure). */
public final class SeatPlans {
    private SeatPlans() {}

    /**
     * How far a plan reaches round its origin: {@code radius} horizontally, {@code height} up, {@code depth} down. The seat
     * builder clears, founds and force-loads this much; a seat's origin keeps it inside the world's build height.
     */
    public record Bounds(int radius, int height, int depth) {}

    public static LayoutPlan plan(ChainFaction faction, long seed) {
        return switch (faction) {
            case DEMON, ANGEL -> DungeonCatalog.plan(faction, seed);
            case LEVIATHAN -> org.papiricoh.supernaturalcraft.chain.seat.tower.SucroTower.plan(seed);
        };
    }

    public static Bounds bounds(ChainFaction faction) {
        return switch (faction) {
            case DEMON, ANGEL -> new Bounds(DungeonAnchors.RADIUS, DungeonAnchors.HEIGHT, DungeonAnchors.DEPTH);
            case LEVIATHAN -> new Bounds(org.papiricoh.supernaturalcraft.chain.seat.tower.SucroTower.RADIUS,
                    org.papiricoh.supernaturalcraft.chain.seat.tower.SucroTower.HEIGHT, org.papiricoh.supernaturalcraft.chain.seat.tower.SucroTower.DEPTH);
        };
    }
}
