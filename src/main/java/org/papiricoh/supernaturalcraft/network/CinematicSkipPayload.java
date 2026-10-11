package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Client → server (v0.19): the player skipped the camera sequence, so they stop being untouchable for the rest of it. */
public record CinematicSkipPayload() implements CustomPacketPayload {

    public static final CinematicSkipPayload INSTANCE = new CinematicSkipPayload();
    public static final Type<CinematicSkipPayload> TYPE = new Type<>(SupernaturalCraft.asResource("cinematic_skip"));
    public static final StreamCodec<ByteBuf, CinematicSkipPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
