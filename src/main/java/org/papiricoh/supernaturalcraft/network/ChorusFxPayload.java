package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → nearby players, once when one of the Broken Chorus's effects begins. The client draws
 * it on its own from there, following the boss (its eyes, its core) as it moves.
 *
 * @param arg   an eye or wheel index, or a note, depending on the kind
 * @param target an entity the effect is aimed at (a gaze's victim), or -1
 */
public record ChorusFxPayload(int boss, byte kind, int arg, int target, Vec3 point, Vec3 aux, float radius, int duration, int color)
        implements CustomPacketPayload {

    /** An eye charging its judgment on {@code target}: a thread of light that tightens, then a flash. */
    public static final byte GAZE = 0;
    /** A beam from {@code point} to {@code aux}, {@code radius} wide, fading over its duration. */
    public static final byte BEAM = 1;
    /** A ring racing out from {@code point} to {@code radius}. */
    public static final byte RING_OUT = 2;
    /** Beams out from every living eye of wheel {@code arg}, {@code radius} long, turning with it. */
    public static final byte EYE_BEAMS = 3;
    /** A burning wheel rolling along the ground from {@code point} to {@code aux}. */
    public static final byte ROLLING = 4;
    /** The halo takes a note's colour and a thread of it runs down to the bell at {@code point}. */
    public static final byte NOTE = 5;
    /** The core's pulse: a sphere of light swelling to {@code radius}. */
    public static final byte PULSE = 6;

    public static final Type<ChorusFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("chorus_fx"));
    public static final StreamCodec<ByteBuf, ChorusFxPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.boss);
                buf.writeByte(p.kind);
                ByteBufCodecs.VAR_INT.encode(buf, p.arg);
                ByteBufCodecs.VAR_INT.encode(buf, p.target + 1);
                SNCodecs.VEC3.encode(buf, p.point);
                SNCodecs.VEC3.encode(buf, p.aux);
                buf.writeFloat(p.radius);
                ByteBufCodecs.VAR_INT.encode(buf, p.duration);
                buf.writeInt(p.color);
            },
            buf -> new ChorusFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf) - 1, SNCodecs.VEC3.decode(buf), SNCodecs.VEC3.decode(buf), buf.readFloat(),
                    ByteBufCodecs.VAR_INT.decode(buf), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
