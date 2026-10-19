package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.21), to the owner only: whether they bear the Mark of Cain and how thirsty it is (0-1). Sent on login,
 * respawn, dimension change and when it changes enough to show ({@code client/mark/ClientMark}).
 */
public record MarkSyncPayload(boolean marked, float thirst) implements CustomPacketPayload {

    public static final Type<MarkSyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("mark_sync"));
    public static final StreamCodec<ByteBuf, MarkSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MarkSyncPayload::marked, ByteBufCodecs.FLOAT, MarkSyncPayload::thirst, MarkSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
