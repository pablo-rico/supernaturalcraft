package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.UUID;

/** Server → nearby players: where the Cage is, how big, which phase, and whether it still stands. */
public record ArenaStatePayload(UUID id, BlockPos center, int radius, int phase, boolean active, int theme)
        implements CustomPacketPayload {

    public static final Type<ArenaStatePayload> TYPE = new Type<>(SupernaturalCraft.asResource("arena_state"));
    public static final StreamCodec<ByteBuf, ArenaStatePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ArenaStatePayload::id,
            BlockPos.STREAM_CODEC, ArenaStatePayload::center,
            ByteBufCodecs.VAR_INT, ArenaStatePayload::radius,
            ByteBufCodecs.VAR_INT, ArenaStatePayload::phase,
            ByteBufCodecs.BOOL, ArenaStatePayload::active,
            ByteBufCodecs.VAR_INT, ArenaStatePayload::theme,
            ArenaStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
