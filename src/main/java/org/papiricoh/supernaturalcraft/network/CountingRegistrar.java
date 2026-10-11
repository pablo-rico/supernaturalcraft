package org.papiricoh.supernaturalcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.papiricoh.supernaturalcraft.debug.AuditStats;

/**
 * The payload registrar the mod registers through: it hands every codec to {@link Send} (so sent packets can be weighed) and
 * counts what clients send in when the audit counters are on.
 */
final class CountingRegistrar {

    private final PayloadRegistrar registrar;

    CountingRegistrar(PayloadRegistrar registrar) {
        this.registrar = registrar;
    }

    <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                      IPayloadHandler<T> handler) {
        Send.codec(type, codec);
        registrar.playToClient(type, codec, handler);
    }

    <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                      IPayloadHandler<T> handler) {
        Send.codec(type, codec);
        registrar.playToServer(type, codec, (payload, context) -> {
            if (AuditStats.enabled() && context.player() instanceof ServerPlayer sp) {
                AuditStats.received(type.id().getPath(), Send.bytes(sp.serverLevel(), payload));
            }
            handler.handle(payload, context);
        });
    }
}
