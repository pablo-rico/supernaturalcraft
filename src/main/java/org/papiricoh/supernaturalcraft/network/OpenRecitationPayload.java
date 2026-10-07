package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Server → caster: the bowl at {@code pos} is lit, recite {@code incantation} within {@code allowedTicks}. */
public record OpenRecitationPayload(BlockPos pos, ResourceLocation spell, String incantation, int allowedTicks, int penaltyTicks,
                                    int smokeColor) implements CustomPacketPayload {

    public static final Type<OpenRecitationPayload> TYPE = new Type<>(SupernaturalCraft.asResource("open_recitation"));
    public static final StreamCodec<ByteBuf, OpenRecitationPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenRecitationPayload::pos,
            ResourceLocation.STREAM_CODEC, OpenRecitationPayload::spell,
            ByteBufCodecs.STRING_UTF8, OpenRecitationPayload::incantation,
            ByteBufCodecs.VAR_INT, OpenRecitationPayload::allowedTicks,
            ByteBufCodecs.VAR_INT, OpenRecitationPayload::penaltyTicks,
            ByteBufCodecs.INT, OpenRecitationPayload::smokeColor,
            OpenRecitationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
