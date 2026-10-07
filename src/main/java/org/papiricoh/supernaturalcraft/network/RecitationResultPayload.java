package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Caster → server: how the recitation at the bowl at {@code pos} went. Re-validated on the server. */
public record RecitationResultPayload(BlockPos pos, boolean success, int typos) implements CustomPacketPayload {

    public static final Type<RecitationResultPayload> TYPE = new Type<>(SupernaturalCraft.asResource("recitation_result"));
    public static final StreamCodec<ByteBuf, RecitationResultPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RecitationResultPayload::pos,
            ByteBufCodecs.BOOL, RecitationResultPayload::success,
            ByteBufCodecs.VAR_INT, RecitationResultPayload::typos,
            RecitationResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
