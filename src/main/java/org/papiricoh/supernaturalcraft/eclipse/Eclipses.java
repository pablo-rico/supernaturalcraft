package org.papiricoh.supernaturalcraft.eclipse;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.network.EclipseStatePayload;

/** The ritual eclipse: begin it, end it, ask about it, tell the clients. */
public final class Eclipses {

    private Eclipses() {
    }

    /** Server-side truth; always false on the client and in other dimensions without one. */
    public static boolean active(Level level) {
        return level instanceof ServerLevel server && EclipseSavedData.get(server).active();
    }

    /** @return false if one already hangs over this dimension */
    public static boolean begin(ServerLevel level, BlockPos altar, int ticks) {
        EclipseSavedData data = EclipseSavedData.get(level);
        if (data.active()) return false;
        data.begin(level.getGameTime(), ticks, altar);
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
        broadcast(level);
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.eclipse.ends").withStyle(ChatFormatting.GOLD), false);
        }
    }

    /** Held open while the Darkness is fought; when released it lingers a minute more. */
    public static void lock(ServerLevel level, boolean locked) {
        EclipseSavedData.get(level).setLocked(locked, level.getGameTime(), 1200);
        broadcast(level);
    }

    public static void tick(ServerLevel level) {
        EclipseSavedData data = EclipseSavedData.get(level);
        if (data.expired(level.getGameTime())) end(level);
    }

    public static EclipseStatePayload state(ServerLevel level) {
        EclipseSavedData d = EclipseSavedData.get(level);
        long end = d.locked() ? Long.MAX_VALUE / 2 : d.endTick();
        return new EclipseStatePayload(d.active(), d.startTick(), end);
    }

    public static void broadcast(ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level, state(level));
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, state(player.serverLevel()));
    }

    public static int defaultTicks() {
        return SNConfig.ECLIPSE_TICKS.get();
    }
}
