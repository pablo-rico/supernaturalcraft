package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Player → server: the wish chosen from the crossroads demon's offer (empty = walked away). */
public record DealChoicePayload(int entityId, String wish, int arg) implements CustomPacketPayload {

    public static final Type<DealChoicePayload> TYPE = new Type<>(SupernaturalCraft.asResource("deal_choice"));
    public static final StreamCodec<ByteBuf, DealChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DealChoicePayload::entityId,
            ByteBufCodecs.STRING_UTF8, DealChoicePayload::wish,
            ByteBufCodecs.VAR_INT, DealChoicePayload::arg,
            DealChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
