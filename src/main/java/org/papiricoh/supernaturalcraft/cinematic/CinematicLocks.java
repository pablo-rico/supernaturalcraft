package org.papiricoh.supernaturalcraft.cinematic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.papiricoh.supernaturalcraft.network.CameraSequencePayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.papiricoh.supernaturalcraft.network.Send;

/**
 * Players watching a cinematic cannot be hurt: their camera is elsewhere and their hands are tied.
 * The lock is server-side and lasts the sequence's length whether or not a client skips it.
 */
public final class CinematicLocks {

    private static final Map<UUID, Long> LOCKED = new HashMap<>();

    /** Forgets a player who left (v0.19, {@code PlayerCleanup}). */
    public static void forgetPlayer(java.util.UUID player) {
        LOCKED.remove(player);
    }

    private CinematicLocks() {
    }

    /**
     * Plays {@code sequence} for the players within {@code range} of {@code anchor} and locks them meanwhile. When the anchor
     * fights in an arena only its challengers see it (v0.19): a bystander nearby keeps their camera and their hands.
     */
    public static void play(Entity anchor, ResourceLocation sequence, int ticks, double range) {
        if (!(anchor.level() instanceof ServerLevel level)) return;
        var payload = new CameraSequencePayload(sequence, anchor.getId(), anchor.position(), anchor.getYRot(), ticks);
        var arena = org.papiricoh.supernaturalcraft.arena.BossShares.arenaOf(level, anchor);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(anchor) > range * range) continue;
            if (arena != null && !arena.isParticipant(p)) continue;
            lock(p, ticks);
            Send.toPlayer(p, payload);
        }
    }

    public static void lock(ServerPlayer player, int ticks) {
        LOCKED.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    public static boolean locked(ServerPlayer player) {
        Long until = LOCKED.get(player.getUUID());
        if (until == null) return false;
        if (player.level().getGameTime() < until) return true;
        LOCKED.remove(player.getUUID());
        return false;
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && locked(p)) event.setCanceled(true);
    }

    public static void clear() {
        LOCKED.clear();
    }
}
