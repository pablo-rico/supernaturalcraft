package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Server → one player: how far Hell's Torment has crept into them (0–1). Only visions; never harm. */
public record TormentPayload(float torment) implements CustomPacketPayload {

    public static final Type<TormentPayload> TYPE = new Type<>(SupernaturalCraft.asResource("torment"));
    public static final StreamCodec<ByteBuf, TormentPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, TormentPayload::torment, TormentPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
