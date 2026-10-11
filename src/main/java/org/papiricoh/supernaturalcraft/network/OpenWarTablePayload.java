package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Server → hunter: open the war table whose centre is {@code centre} (v0.19). Its pins come with the block entity, the cases with the Legacy. */
public record OpenWarTablePayload(BlockPos centre) implements CustomPacketPayload {

    public static final Type<OpenWarTablePayload> TYPE = new Type<>(SupernaturalCraft.asResource("open_war_table"));
    public static final StreamCodec<ByteBuf, OpenWarTablePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenWarTablePayload::centre, OpenWarTablePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
