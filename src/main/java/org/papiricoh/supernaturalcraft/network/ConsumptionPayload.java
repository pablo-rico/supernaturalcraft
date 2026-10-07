package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Server → one player: how far the Darkness has consumed them (0-100). */
public record ConsumptionPayload(float value) implements CustomPacketPayload {

    public static final Type<ConsumptionPayload> TYPE = new Type<>(SupernaturalCraft.asResource("consumption"));
    public static final StreamCodec<ByteBuf, ConsumptionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ConsumptionPayload::value, ConsumptionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
