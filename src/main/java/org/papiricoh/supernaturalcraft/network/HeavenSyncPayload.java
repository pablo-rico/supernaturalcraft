package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.18): a hunter's own {@code HeavenStanding} (encoded with its codec into {@code standing}) plus whatever
 * the client needs about the plot they stand in ({@code plot}: owner name, home unlocked, wing and office open...). Sent on
 * login, dimension change and when either changes.
 */
public record HeavenSyncPayload(CompoundTag standing, CompoundTag plot) implements CustomPacketPayload {

    public static final Type<HeavenSyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("heaven_sync"));

    public static final StreamCodec<ByteBuf, HeavenSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, HeavenSyncPayload::standing,
            ByteBufCodecs.COMPOUND_TAG, HeavenSyncPayload::plot,
            HeavenSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
