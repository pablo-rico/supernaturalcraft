package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.18): a hunter's own memory log ({@code MemoryLog}, encoded with its codec into {@code log}), for the book's
 * Memories tab and the toasts. Sent on login, respawn, dimension change and whenever a memory is added or gathered.
 */
public record MemorySyncPayload(CompoundTag log) implements CustomPacketPayload {

    public static final Type<MemorySyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("memory_sync"));

    public static final StreamCodec<ByteBuf, MemorySyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, MemorySyncPayload::log,
            MemorySyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
