package org.papiricoh.supernaturalcraft.entity.boss.aumichael;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.SNCodecs;

/**
 * AU Michael's fight on the client (v0.41, registered by {@code ApocWorldNet}). Kinds are append-only; {@code arg}/{@code arg2}/
 * {@code duration} mean what each kind's comment says.
 */
public record AuMichaelFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    /** A title card ({@code arg}: the phase, 0 = none; {@code arg2} 1: his flight), {@code duration}: ticks. */
    public static final byte TITLE = 0;
    /** Holy fire over a circle ({@code arg}: radius × 10). */
    public static final byte SMITE = 1;
    /** His wings beaten forward ({@code arg}: yaw, {@code arg2}: reach). */
    public static final byte GALE = 2;
    /** A bolt of the burned sky at {@code point}. */
    public static final byte BOLT = 3;
    /** Jack's grace flaring gold at a hunter's side ({@code arg}: the hunter's entity id), {@code duration} ticks. */
    public static final byte JACK = 4;
    /** He steps into the rift at {@code point} and is gone. */
    public static final byte FLEE = 5;
    /** A cut of his blade ({@code arg}: yaw, {@code arg2}: reach × 10). */
    public static final byte SLASH = 6;

    public static final Type<AuMichaelFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("au_michael_fx"));
    public static final StreamCodec<ByteBuf, AuMichaelFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new AuMichaelFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
