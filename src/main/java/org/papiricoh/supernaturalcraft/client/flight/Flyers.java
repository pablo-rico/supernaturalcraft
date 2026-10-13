package org.papiricoh.supernaturalcraft.client.flight;

import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Who this client sees flying on wings (v0.20), as the flight pose decides it each tick: the wings spread and beat by the
 * same verdict the body lies down by, also for other players, whose abilities never reach us.
 */
public final class Flyers {

    private static final Map<Player, Boolean> FLYING = Collections.synchronizedMap(new WeakHashMap<>());

    private Flyers() {
    }

    public static void set(Player player, boolean flying) {
        if (flying) FLYING.put(player, true);
        else FLYING.remove(player);
    }

    public static boolean flying(Player player) {
        return FLYING.containsKey(player);
    }
}
