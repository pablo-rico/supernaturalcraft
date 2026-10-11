package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.debug.AuditStats;

import java.util.HashMap;
import java.util.Map;

/**
 * Every packet the mod sends to clients goes out through here: the same reach as {@link PacketDistributor}'s methods, with the
 * audit counters ({@link AuditStats}) measuring each payload when they are on.
 */
public final class Send {

    private static final Map<CustomPacketPayload.Type<?>, StreamCodec<? super RegistryFriendlyByteBuf, ?>> CODECS = new HashMap<>();

    private Send() {
    }

    /** Remembers a payload's codec so the counters can weigh it (registration). */
    static void codec(CustomPacketPayload.Type<?> type, StreamCodec<? super RegistryFriendlyByteBuf, ?> codec) {
        CODECS.put(type, codec);
    }

    public static void toPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
        count(player.serverLevel(), payload, 1);
    }

    public static void inDimension(ServerLevel level, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersInDimension(level, payload);
        count(level, payload, level.players().size());
    }

    public static void near(ServerLevel level, @Nullable ServerPlayer excluded, double x, double y, double z, double radius,
                            CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersNear(level, excluded, x, y, z, radius, payload);
        if (!AuditStats.enabled()) return;
        int n = 0;
        for (ServerPlayer p : level.players()) if (p != excluded && p.distanceToSqr(x, y, z) < radius * radius) n++;
        count(level, payload, n);
    }

    public static void trackingEntity(Entity entity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
        if (AuditStats.enabled() && entity.level() instanceof ServerLevel level) count(level, payload, watchers(level, entity, false));
    }

    public static void trackingEntityAndSelf(Entity entity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
        if (AuditStats.enabled() && entity.level() instanceof ServerLevel level) count(level, payload, watchers(level, entity, true));
    }

    /** Players tracking an entity, and the entity itself if it is a player and {@code self}. */
    private static int watchers(ServerLevel level, Entity entity, boolean self) {
        int n = level.getChunkSource().chunkMap.getPlayersWatching(entity).size();
        return self && entity instanceof ServerPlayer ? n + 1 : n;
    }

    private static void count(ServerLevel level, CustomPacketPayload payload, int recipients) {
        if (!AuditStats.enabled()) return;
        AuditStats.sent(payload.type().id().getPath(), bytes(level, payload), recipients);
    }

    /** The encoded size of a payload, or -1 if its codec is unknown. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static int bytes(ServerLevel level, CustomPacketPayload payload) {
        StreamCodec codec = CODECS.get(payload.type());
        if (codec == null) return -1;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try {
            codec.encode(buf, payload);
            return buf.writerIndex();
        } catch (RuntimeException e) {
            return -1;
        } finally {
            buf.release();
        }
    }
}
