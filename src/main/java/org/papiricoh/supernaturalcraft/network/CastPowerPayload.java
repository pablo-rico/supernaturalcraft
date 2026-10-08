package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Player → server (v0.13): cast a power from the wheel. {@code power} = {@code Power} ordinal; {@code target} = the entity
 * the client sees under the crosshair (-1 for none). The server checks everything again (side, rank, essence, cooldown,
 * suppression, range and line of sight) and picks its own target if this one doesn't hold.
 */
public record CastPowerPayload(int power, int target) implements CustomPacketPayload {

    public static final Type<CastPowerPayload> TYPE = new Type<>(SupernaturalCraft.asResource("cast_power"));
    public static final StreamCodec<ByteBuf, CastPowerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CastPowerPayload::power,
            ByteBufCodecs.VAR_INT, CastPowerPayload::target,
            CastPowerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
