package org.papiricoh.supernaturalcraft.arena;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.papiricoh.supernaturalcraft.network.ArenaStatePayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.debug.AuditStats;

/** Ticks every arena, holds challengers inside the dome, and cleans up on shutdown. */
public final class ArenaEvents {

    /** How far outside its wall a fight warns the passer-by. */
    public static final int WARN_RANGE = 16;
    /** Ticks an active arena may go without its boss loaded before it is considered abandoned. */
    private static final int MISSING_BOSS_GRACE = 200;
    private static final Map<UUID, Integer> missingBoss = new HashMap<>();

    private ArenaEvents() {
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        long sn$t = AuditStats.start();
        try {
            if (!(event.getLevel() instanceof ServerLevel level)) return;
            ArenaSavedData data = ArenaSavedData.get(level);
            if (data.all().isEmpty()) return;
            boolean sync = level.getGameTime() % 20 == 0;
            for (ArenaController arena : data.all()) {
                if (arena.isActive()) {
                    tickActive(level, arena, sync);
                } else if (arena.status() == ArenaController.Status.RESTORING && sync) {
                    broadcast(level, arena, false);
                }
                arena.tick(level);
            }
            data.removeClosed();
            data.setDirty();
        } finally {
            AuditStats.stop("arena.levelTick", sn$t);
        }
    }

    private static void tickActive(ServerLevel level, ArenaController arena, boolean sync) {
        if (sync) {
            // Anyone who walks in joins the fight; nobody walks out. Those at the edge are told so first (v0.19).
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator()) continue;
                if (arena.contains(p.position())) {
                    arena.join(p);
                } else if (!arena.isParticipant(p) && arena.horizontalDistance(p.position()) < arena.radius() + WARN_RANGE) {
                    p.displayClientMessage(Component.translatable("message.supernaturalcraft.arena.warning").withStyle(ChatFormatting.GOLD), true);
                }
            }
            broadcast(level, arena, true);
        }
        Entity boss = arena.bossId() == null ? null : level.getEntity(arena.bossId());
        if (boss == null) {
            int missing = missingBoss.merge(arena.id(), 1, Integer::sum);
            if (missing > MISSING_BOSS_GRACE) {
                missingBoss.remove(arena.id());
                arena.beginRestore(false);
            }
        } else {
            missingBoss.remove(arena.id());
        }
        for (ServerPlayer p : level.players()) {
            if (arena.isParticipant(p)) {
                holdInside(level, arena, p);
                ArenaRescue.check(level, arena, p);
            }
        }
        org.papiricoh.supernaturalcraft.weather.StormLock.tick(level, arena);
    }

    /**
     * A fight is over, however it ended (v0.19): what it held shut or open in the world is let go, so nothing stays stuck if
     * its boss was unloaded, killed by command or cut short by a restart. The Darkness's eclipse unlocks; the Cage closes again.
     */
    public static void released(ServerLevel level, ArenaController arena, boolean stopping) {
        if (arena.theme() == ArenaTheme.DARKNESS) org.papiricoh.supernaturalcraft.eclipse.Eclipses.lock(level, false);
        if (arena.theme() == ArenaTheme.ABYSS && level.dimension() == org.papiricoh.supernaturalcraft.hell.HellDimension.LEVEL) {
            var cage = org.papiricoh.supernaturalcraft.hell.cage.CageController.get(level);
            if (!cage.isClosed()) {
                if (stopping) cage.set(level, false);
                else cage.close(level);
            }
        }
    }

    /** Pushes a challenger back from the dome wall and denies flight over it. */
    public static void holdInside(ServerLevel level, ArenaController arena, ServerPlayer p) {
        if (p.isSpectator() || p.isCreative()) return;
        double d = arena.horizontalDistance(p.position());
        int r = arena.radius();
        if (d > r - 0.5 && d < r + ArenaController.EDGE_MARGIN) {
            Vec3 in = arena.centerVec().subtract(p.position()).multiply(1, 0, 1).normalize();
            p.setDeltaMovement(in.x * 0.7, 0.25, in.z * 0.7);
            p.hurtMarked = true;
            if (p.isFallFlying()) p.stopFallFlying();
            if (p.tickCount % 10 == 0) {
                p.hurt(AllDamageTypes.source(level, AllDamageTypes.ARENA_BARRIER, null), 1.0f);
                level.playSound(null, p.blockPosition(), AllSounds.ARENA_BARRIER.get(), SoundSource.HOSTILE, 0.8f, 1.0f);
                p.displayClientMessage(Component.translatable(ArenaTheme.barrierKey(arena.theme()))
                        .withStyle(ChatFormatting.DARK_RED), true);
            }
        }
        if (p.position().y > arena.center().getY() + arena.height() - 1 && p.getDeltaMovement().y > 0) {
            p.setDeltaMovement(p.getDeltaMovement().multiply(1, -0.5, 1));
            p.hurtMarked = true;
        }
    }

    public static void broadcast(ServerLevel level, ArenaController arena, boolean active) {
        ArenaStatePayload payload = new ArenaStatePayload(arena.id(), arena.center(), arena.radius(), arena.phase(), active, arena.theme());
        for (ServerPlayer p : level.players()) {
            if (arena.horizontalDistance(p.position()) < arena.radius() + 64) {
                Send.toPlayer(p, payload);
            }
        }
    }

    static void denyTeleport(EntityTeleportEvent event) {
        Entity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level)) return;
        for (ArenaController arena : ArenaSavedData.get(level).all()) {
            if (arena.isActive() && arena.isParticipant(e) && !arena.contains(event.getTarget())) {
                event.setCanceled(true);
                if (e instanceof ServerPlayer p) {
                    p.displayClientMessage(Component.translatable(ArenaTheme.noEscapeKey(arena.theme()))
                            .withStyle(ChatFormatting.DARK_RED), true);
                }
                return;
            }
        }
    }

    public static void onEnderPearl(EntityTeleportEvent.EnderPearl event) {
        denyTeleport(event);
    }

    public static void onChorusFruit(EntityTeleportEvent.ChorusFruit event) {
        denyTeleport(event);
    }

    /** A fight interrupted by shutdown leaves no trace: every arena is restored on the spot. */
    public static void onServerStopping(ServerStoppingEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            ArenaSavedData data = ArenaSavedData.get(level);
            for (ArenaController arena : data.all()) {
                if (arena.status() != ArenaController.Status.CLOSED) arena.restoreNow(level, true);
            }
            data.removeClosed();
            data.setDirty();
        }
        missingBoss.clear();
    }
}
