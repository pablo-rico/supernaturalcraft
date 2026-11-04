package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Server → client (v0.36): {@code entity} wears (or stops wearing) a borrowed villager's face ({@code client/prophecy/ClientMimicry}). */
public record MimicryPayload(int entity, boolean active) implements CustomPacketPayload {

    public static final Type<MimicryPayload> TYPE = new Type<>(SupernaturalCraft.asResource("mimicry"));
    public static final StreamCodec<ByteBuf, MimicryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MimicryPayload::entity, ByteBufCodecs.BOOL, MimicryPayload::active, MimicryPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
