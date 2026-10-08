package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.16): one moment of Raphael's storm to show. The client plays it out on its own
 * ({@code client/raphael/ClientRaphael}).
 *
 * @param entity   Raphael (or the angel it is about), -1 if none
 * @param kind     one of the constants below
 * @param arg      kind-specific
 * @param arg2     kind-specific
 * @param point    where, if anywhere
 * @param duration ticks
 */
public record RaphaelFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    /** A title card: {@code arg} = phase (1-3), {@code arg2} 0 opening / 1 his fall. */
    public static final byte TITLE = 0;
    /** A lightning flash at {@code point}: the screen whitens and, if {@code arg} is 1, his wings' shadow is cast from it. */
    public static final byte FLASH = 1;
    /** A thread of grace from {@code entity} (an angel of his garrison) to Raphael ({@code arg}); {@code duration} 0 cuts it. */
    public static final byte TETHER = 2;
    /** He is held in a lit ring of holy oil centred on {@code point} for {@code duration} ticks (0: freed). */
    public static final byte TRAP = 3;
    /** The snap: a burst of {@code arg} blocks' radius at {@code point}; the safe ring is {@code arg2} blocks wide. */
    public static final byte SNAP = 4;

    public static final Type<RaphaelFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("raphael_fx"));
    public static final StreamCodec<ByteBuf, RaphaelFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new RaphaelFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
