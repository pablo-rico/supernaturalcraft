package org.papiricoh.supernaturalcraft.util;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.UUID;

/**
 * What the server keeps in memory about a player for a moment (cooldowns, cinematic locks, counters) is forgotten when they
 * leave (v0.19), so a server with many comings and goings does not keep a line for everyone who ever joined.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class PlayerCleanup {

    private PlayerCleanup() {
    }

    public static void forget(UUID id) {
        org.papiricoh.supernaturalcraft.arena.ArenaRescue.forget(id);
        org.papiricoh.supernaturalcraft.network.RateLimiter.forget(id);
        org.papiricoh.supernaturalcraft.cinematic.CinematicLocks.forgetPlayer(id);
        org.papiricoh.supernaturalcraft.heaven.passage.HeavenPassage.forgetPlayer(id);
        org.papiricoh.supernaturalcraft.memory.MemoryStage.forgetPlayer(id);
        org.papiricoh.supernaturalcraft.weapon.RuneEvents.forgetPlayer(id);
        org.papiricoh.supernaturalcraft.entity.boss.amara.Consumption.forgetPlayer(id);
        org.papiricoh.supernaturalcraft.mark.Marks.forget(id);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        forget(event.getEntity().getUUID());
    }
}
