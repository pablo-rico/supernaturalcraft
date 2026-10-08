package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Player → server: the answer to Michael's "I need your yes" ({@code entity} = Michael's id). Validated by the server. */
public record VesselAnswerPayload(int entity, boolean yes) implements CustomPacketPayload {

    public static final Type<VesselAnswerPayload> TYPE = new Type<>(SupernaturalCraft.asResource("vessel_answer"));
    public static final StreamCodec<ByteBuf, VesselAnswerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VesselAnswerPayload::entity,
            ByteBufCodecs.BOOL, VesselAnswerPayload::yes,
            VesselAnswerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
