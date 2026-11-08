package org.papiricoh.supernaturalcraft.hands.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Where Thule soldiers fell lately (v0.42), by dimension: a Thule officer raises the freshest corpse near him. In memory only
 * (a corpse is worth raising for a minute: {@link HandsMobRules#CORPSE_TICKS}), capped, and forgotten when the server stops.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ThuleCorpses {

    static final int CAP = 64;

    private record Corpse(BlockPos pos, long time) {
    }

    private static final Map<ResourceKey<Level>, Deque<Corpse>> FALLEN = new HashMap<>();

    private ThuleCorpses() {
    }

    public static synchronized void fell(Level level, BlockPos pos) {
        Deque<Corpse> d = FALLEN.computeIfAbsent(level.dimension(), k -> new ArrayDeque<>());
        d.addFirst(new Corpse(pos.immutable(), level.getGameTime()));
        while (d.size() > CAP) d.removeLast();
    }

    /** Takes (and forgets) the freshest corpse within {@code reach} of {@code near}, or null. */
    public static synchronized @Nullable BlockPos take(Level level, BlockPos near, double reach) {
        Deque<Corpse> d = FALLEN.get(level.dimension());
        if (d == null) return null;
        long now = level.getGameTime();
        for (Iterator<Corpse> it = d.iterator(); it.hasNext(); ) {
            Corpse c = it.next();
            if (now - c.time > HandsMobRules.CORPSE_TICKS || now < c.time) {
                it.remove();
                continue;
            }
            if (c.pos.distSqr(near) <= reach * reach) {
                it.remove();
                return c.pos;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        synchronized (ThuleCorpses.class) {
            FALLEN.clear();
        }
    }
}
