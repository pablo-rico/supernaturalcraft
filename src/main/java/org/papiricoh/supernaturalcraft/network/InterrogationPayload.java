package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.36), to the interrogator: the resistance left in the creature ({@code entity}) they are breaking, 0–100,
 * and the state ({@link #ONGOING}, {@link #BROKEN}, {@link #LOST}). Drawn by {@code client/interrogation/InterrogationHud}.
 */
public record InterrogationPayload(int entity, int resistance, int state) implements CustomPacketPayload {

    public static final int ONGOING = 0, BROKEN = 1, LOST = 2;
    public static final Type<InterrogationPayload> TYPE = new Type<>(SupernaturalCraft.asResource("interrogation"));
    public static final StreamCodec<ByteBuf, InterrogationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, InterrogationPayload::entity, ByteBufCodecs.VAR_INT, InterrogationPayload::resistance,
            ByteBufCodecs.VAR_INT, InterrogationPayload::state, InterrogationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
