package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Server → players of one dimension: whether its sun is eclipsed, since and until when (game time). */
public record EclipseStatePayload(boolean active, long startTick, long endTick) implements CustomPacketPayload {

    public static final Type<EclipseStatePayload> TYPE = new Type<>(SupernaturalCraft.asResource("eclipse_state"));
    public static final StreamCodec<ByteBuf, EclipseStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, EclipseStatePayload::active,
            ByteBufCodecs.VAR_LONG, EclipseStatePayload::startTick,
            ByteBufCodecs.VAR_LONG, EclipseStatePayload::endTick,
            EclipseStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
