package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → participants: play camera sequence {@code sequence} around entity {@code anchorEntity}
 * (or, once it is gone, where it was: {@code anchor}), framed by its facing at the start
 * ({@code yaw}), for at most {@code ticks}. Clients with cinematics turned off ignore it.
 */
public record CameraSequencePayload(ResourceLocation sequence, int anchorEntity, Vec3 anchor, float yaw, int ticks)
        implements CustomPacketPayload {

    public static final Type<CameraSequencePayload> TYPE = new Type<>(SupernaturalCraft.asResource("camera_sequence"));
    public static final StreamCodec<ByteBuf, CameraSequencePayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, CameraSequencePayload::sequence,
            ByteBufCodecs.VAR_INT, CameraSequencePayload::anchorEntity,
            SNCodecs.VEC3, CameraSequencePayload::anchor,
            ByteBufCodecs.FLOAT, CameraSequencePayload::yaw,
            ByteBufCodecs.VAR_INT, CameraSequencePayload::ticks,
            CameraSequencePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
