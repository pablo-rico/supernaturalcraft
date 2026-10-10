package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Client → server (v0.18): a strapped hunter's struggle against Naomi's chair, sent every few ticks while the overlay is up.
 * {@code chair} = the chair entity, {@code presses} = jump presses since the last packet. Validated by
 * {@code entity.boss.naomi.NaomiServerHandlers.struggle} (the sender is the one strapped in, presses capped per second).
 */
public record ChairStrugglePayload(int chair, int presses) implements CustomPacketPayload {

    public static final Type<ChairStrugglePayload> TYPE = new Type<>(SupernaturalCraft.asResource("chair_struggle"));

    public static final StreamCodec<ByteBuf, ChairStrugglePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChairStrugglePayload::chair,
            ByteBufCodecs.VAR_INT, ChairStrugglePayload::presses,
            ChairStrugglePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
