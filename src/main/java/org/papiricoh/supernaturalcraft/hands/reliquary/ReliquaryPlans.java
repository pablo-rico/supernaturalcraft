package org.papiricoh.supernaturalcraft.hands.reliquary;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Outside;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The five reliquaries of the Hands of God (v0.42) as pure blueprints for the buildkit, one per kind: the U-boat wreck
 * ({@link UboatPlan}), the Thule bunker ({@link BunkerPlan}), the church crypt ({@link CryptPlan}), the Ark's temple
 * ({@link TemplePlan}) and the snow monastery ({@link MonasteryPlan}). Each drawn facing north and turned by q quarter turns.
 * The altar and the chests are the builder's (they are block entities): the plans leave their cells open. Contract:
 * {@link ReliquaryAnchors}; {@code ReliquaryPlanQualityTest} holds them to the kit's bar.
 */
public final class ReliquaryPlans {

    private static final Map<S11Assets.Hand, LayoutPlan[]> CACHE = new EnumMap<>(S11Assets.Hand.class);

    private ReliquaryPlans() {
    }

    /** Which way a reliquary faces (0-3), from its seed. */
    public static int turnOf(long seed) {
        return (int) Math.floorMod(seed ^ (seed >>> 23), 4L);
    }

    public static int[] rotate(int x, int z, int q) {
        return Canvas.turn(x, z, q);
    }

    /** What lies round a kind's plan off the canvas: the sea round the wreck, the open air round the rest. */
    public static Outside outside(S11Assets.Hand kind) {
        return switch (kind) {
            case DELPHINES_HAND -> (x, y, z) -> y <= -3 ? "minecraft:sand" : St.water();
            // Dug into the ground: rock round the rooms.
            case NAIL_OF_THE_CROSS, APOSTLES_CHALICE, ROD_OF_AARON -> Outside.groundUpTo(-1);
            case TRUMPET_OF_JERICHO -> Outside.AIR;
        };
    }

    static Canvas north(S11Assets.Hand kind) {
        Canvas c = switch (kind) {
            case DELPHINES_HAND -> UboatPlan.drawn();
            case NAIL_OF_THE_CROSS -> BunkerPlan.drawn();
            case APOSTLES_CHALICE -> CryptPlan.drawn();
            case ROD_OF_AARON -> TemplePlan.drawn();
            case TRUMPET_OF_JERICHO -> MonasteryPlan.drawn();
        };
        // The churchyard and the monastery's court are cleared of whatever stood on their ground (hummocks, bushes, snow).
        if (kind == S11Assets.Hand.APOSTLES_CHALICE || kind == S11Assets.Hand.TRUMPET_OF_JERICHO) {
            ReliquaryAnchors.Bounds b = ReliquaryAnchors.bounds(kind);
            for (int x = b.minX(); x <= b.maxX(); x++) {
                for (int z = b.minZ(); z <= b.maxZ(); z++) {
                    if (!c.has(x, -1, z)) continue;
                    for (int y = 0; y <= CLEAR_TOP; y++) c.setIfEmpty(x, y, z, St.AIR);
                }
            }
        }
        return c;
    }

    /** How high the ground of the open reliquaries is cleared. */
    static final int CLEAR_TOP = 4;

    public static Canvas canvas(S11Assets.Hand kind, int q) {
        Canvas north = north(kind);
        Canvas c = new Canvas("reliquary_" + kind.reliquary.toLowerCase(Locale.ROOT));
        c.outside(outside(kind));
        ReliquaryAnchors.Bounds b = ReliquaryAnchors.bounds(kind);
        int r = b.reach() + 2;
        c.zone("reliquary", new Box(-r, b.bottomY(), -r, r, b.topY(), r));
        c.inZone("reliquary", () -> c.stamp(north, 0, 0, 0, Math.floorMod(q, 4), Canvas.Mode.SET, ""));
        return c.finish();
    }

    /** The plan of {@code kind} turned {@code q} (drawn once per kind and turn, then shared: plans are immutable). */
    public static LayoutPlan plan(S11Assets.Hand kind, int q) {
        int t = Math.floorMod(q, 4);
        synchronized (CACHE) {
            LayoutPlan[] turns = CACHE.computeIfAbsent(kind, k -> new LayoutPlan[4]);
            if (turns[t] == null) turns[t] = canvas(kind, t).toPlan();
            return turns[t];
        }
    }
}
