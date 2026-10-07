package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → nearby players, once when one of Amara's attacks begins: what to draw, where, and for
 * how long. The client animates it on its own from there, in step with the server's timings.
 */
public record AmaraFxPayload(int boss, byte kind, Vec3 point, float yaw, float radius, int duration) implements CustomPacketPayload {

    /** A tentacle rising over {@code point} and slamming down onto it. */
    public static final byte SLAM = 0;
    /** A tentacle sweeping one full turn at {@code radius} around her, starting at {@code yaw}. */
    public static final byte SWEEP = 1;
    /** A tentacle reaching out to hold {@code point}. */
    public static final byte GRASP = 2;
    /** A spike of void erupting from the ground at {@code point}. */
    public static final byte SPIKE = 3;
    /** A beam from her core turning one full circle from {@code yaw}, {@code radius} long. */
    public static final byte FLARE = 4;
    /** A ring of black light racing outward from her core to {@code radius}. */
    public static final byte RING_OUT = 5;
    /** A ring falling inward onto her core from {@code radius}. */
    public static final byte RING_IN = 6;

    public static final Type<AmaraFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("amara_fx"));
    public static final StreamCodec<ByteBuf, AmaraFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AmaraFxPayload::boss,
            ByteBufCodecs.BYTE, AmaraFxPayload::kind,
            SNCodecs.VEC3, AmaraFxPayload::point,
            ByteBufCodecs.FLOAT, AmaraFxPayload::yaw,
            ByteBufCodecs.FLOAT, AmaraFxPayload::radius,
            ByteBufCodecs.VAR_INT, AmaraFxPayload::duration,
            AmaraFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
