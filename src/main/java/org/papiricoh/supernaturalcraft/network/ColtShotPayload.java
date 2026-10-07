package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → everyone near the line of fire: the Colt went off. {@code target} is the struck entity's
 * id plus one (0 for none), {@code outcome} a {@code ColtShot.Outcome} ordinal. The shooter's own
 * client has already played the shot it predicted, so it only draws what the server decided.
 */
public record ColtShotPayload(int shooter, long geoId, Vec3 end, int target, byte outcome, boolean lastRound)
        implements CustomPacketPayload {

    public static final Type<ColtShotPayload> TYPE = new Type<>(SupernaturalCraft.asResource("colt_shot"));
    public static final StreamCodec<ByteBuf, ColtShotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ColtShotPayload::shooter,
            ByteBufCodecs.VAR_LONG, ColtShotPayload::geoId,
            SNCodecs.VEC3, ColtShotPayload::end,
            ByteBufCodecs.VAR_INT, ColtShotPayload::target,
            ByteBufCodecs.BYTE, ColtShotPayload::outcome,
            ByteBufCodecs.BOOL, ColtShotPayload::lastRound,
            ColtShotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
