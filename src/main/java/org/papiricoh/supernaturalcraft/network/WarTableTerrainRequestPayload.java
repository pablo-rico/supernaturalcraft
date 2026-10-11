package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.List;

/** Client → server: the terrain tiles ({@code TerrainTile} keys, at most {@link #MAX}) a war table near the hunter wants drawn (v0.19). */
public record WarTableTerrainRequestPayload(BlockPos centre, List<Long> keys) implements CustomPacketPayload {

    public static final int MAX = 16;

    public static final Type<WarTableTerrainRequestPayload> TYPE = new Type<>(SupernaturalCraft.asResource("war_table_terrain_request"));
    public static final StreamCodec<ByteBuf, WarTableTerrainRequestPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WarTableTerrainRequestPayload::centre,
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(MAX)), WarTableTerrainRequestPayload::keys,
            WarTableTerrainRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
