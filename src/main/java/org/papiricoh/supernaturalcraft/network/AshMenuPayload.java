package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.18): open Ash's menu at the Roadhouse. {@code ash} = his entity id; {@code data} = the hint lines' keys,
 * the Heavens the hunter may visit (names and UUIDs) and whether their own welcomes visitors (format owned by the world work,
 * read by {@code client/heaven/AshMenuScreen}).
 */
public record AshMenuPayload(int ash, CompoundTag data) implements CustomPacketPayload {

    public static final Type<AshMenuPayload> TYPE = new Type<>(SupernaturalCraft.asResource("ash_menu"));

    public static final StreamCodec<ByteBuf, AshMenuPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AshMenuPayload::ash,
            ByteBufCodecs.COMPOUND_TAG, AshMenuPayload::data,
            AshMenuPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
