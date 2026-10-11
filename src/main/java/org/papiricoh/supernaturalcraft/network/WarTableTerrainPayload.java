package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Server → hunter: one terrain tile for the war tables' holograms (v0.19); {@code data} as {@code TerrainTile} lays it out. */
public record WarTableTerrainPayload(long key, byte[] data) implements CustomPacketPayload {

    public static final Type<WarTableTerrainPayload> TYPE = new Type<>(SupernaturalCraft.asResource("war_table_terrain"));
    public static final StreamCodec<ByteBuf, WarTableTerrainPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, WarTableTerrainPayload::key,
            ByteBufCodecs.byteArray(64 * 1024), WarTableTerrainPayload::data,
            WarTableTerrainPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
