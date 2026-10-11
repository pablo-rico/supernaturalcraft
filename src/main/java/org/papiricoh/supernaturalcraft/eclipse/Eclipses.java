package org.papiricoh.supernaturalcraft.eclipse;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.network.EclipseStatePayload;
import org.papiricoh.supernaturalcraft.network.Send;

/** The ritual eclipse: begin it, end it, ask about it, tell the clients. */
public final class Eclipses {

    private Eclipses() {
    }

    /** Server-side truth; always false on the client and in other dimensions without one. */
    public static boolean active(Level level) {
        if (!(level instanceof ServerLevel server)) return false;
        // v0.19: asked for every undead every tick: the answer is kept for the rest of the tick.
        long now = server.getGameTime();
        if (server == cachedLevel && now == cachedTick) return cachedActive;
        cachedLevel = server;
        cachedTick = now;
        cachedActive = EclipseSavedData.get(server).active();
        return cachedActive;
    }

    private static ServerLevel cachedLevel;
    private static long cachedTick = Long.MIN_VALUE;
    private static boolean cachedActive;

    /** Forgets the cached answer (an eclipse began, ended or was locked this tick). */
    static void touched() {
        cachedLevel = null;
    }

    /** @return false if one already hangs over this dimension */
    public static boolean begin(ServerLevel level, BlockPos altar, int ticks) {
        EclipseSavedData data = EclipseSavedData.get(level);
        if (data.active()) return false;
        data.begin(level.getGameTime(), ticks, altar);
        touched();
        broadcast(level);
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.eclipse.begins").withStyle(ChatFormatting.DARK_PURPLE), false);
            level.playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 0.6f, 0.4f);
        }
        return true;
    }

    public static void end(ServerLevel level) {
        EclipseSavedData data = EclipseSavedData.get(level);
        if (!data.active()) return;
        data.end();
        touched();
        broadcast(level);
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.eclipse.ends").withStyle(ChatFormatting.GOLD), false);
        }
    }

    /** Held open while the Darkness is fought; when released it lingers a minute more. */
    public static void lock(ServerLevel level, boolean locked) {
        EclipseSavedData.get(level).setLocked(locked, level.getGameTime(), 1200);
        touched();
        broadcast(level);
    }

    public static void tick(ServerLevel level) {
        EclipseSavedData data = EclipseSavedData.get(level);
        if (data.expired(level.getGameTime())) end(level);
        // v0.19: a lock with no Darkness being fought (an older world, a fight cut short) never holds the sky for good.
        if (level.getGameTime() % 200 == 0) releaseStaleLock(level);
    }

    /** Lets go of a lock no Darkness is being fought for. @return whether one was let go */
    public static boolean releaseStaleLock(ServerLevel level) {
        if (!EclipseSavedData.get(level).locked() || fought(level)) return false;
        lock(level, false);
        return true;
    }

    /** Whether the Darkness's arena is up in this level. */
    private static boolean fought(ServerLevel level) {
        for (var a : org.papiricoh.supernaturalcraft.arena.ArenaSavedData.get(level).all()) {
            if (a.isActive() && a.theme() == org.papiricoh.supernaturalcraft.arena.ArenaTheme.DARKNESS) return true;
        }
        return false;
    }

    public static EclipseStatePayload state(ServerLevel level) {
        EclipseSavedData d = EclipseSavedData.get(level);
        long end = d.locked() ? Long.MAX_VALUE / 2 : d.endTick();
        return new EclipseStatePayload(d.active(), d.startTick(), end);
    }

    public static void broadcast(ServerLevel level) {
        Send.inDimension(level, state(level));
    }

    public static void sync(ServerPlayer player) {
        Send.toPlayer(player, state(player.serverLevel()));
    }

    public static int defaultTicks() {
        return SNConfig.ECLIPSE_TICKS.get();
    }
}
