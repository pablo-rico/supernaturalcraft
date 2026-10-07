package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Server → nearby players: a locating spell's smoke, flying from the bowl toward its target. */
public record SmokeTrailPayload(Vec3 from, Vec3 to, int color, int linger) implements CustomPacketPayload {

    public static final Type<SmokeTrailPayload> TYPE = new Type<>(SupernaturalCraft.asResource("smoke_trail"));
    public static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);
    public static final StreamCodec<ByteBuf, SmokeTrailPayload> STREAM_CODEC = StreamCodec.composite(
            VEC3, SmokeTrailPayload::from,
            VEC3, SmokeTrailPayload::to,
            ByteBufCodecs.INT, SmokeTrailPayload::color,
            ByteBufCodecs.VAR_INT, SmokeTrailPayload::linger,
            SmokeTrailPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
