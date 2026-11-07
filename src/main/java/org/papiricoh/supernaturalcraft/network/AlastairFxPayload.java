package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Alastair's fight: the trap, the hooks, the rack (v0.40). Kinds are append-only; {@code arg}/{@code arg2}/{@code duration} mean what each kind's comment says.
 */
public record AlastairFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    /** A title card ({@code arg}: the phase, 0 = none; {@code arg2} 1: his fall), {@code duration}: ticks. */
    public static final byte TITLE = 0;
    /** His chains whip the floor: a ring ({@code arg}: radius × 10; {@code arg2} 1: free, 0: from the trap). */
    public static final byte CHAINS = 1;
    /** A thrown scalpel's lane ({@code arg}: yaw, {@code arg2}: length). */
    public static final byte SCALPEL = 2;
    /** The hook cast along a lane ({@code arg}: yaw, {@code arg2}: the entity it bit, or -1). */
    public static final byte HOOK = 3;
    /** White smoke over a circle ({@code arg}: radius × 10). */
    public static final byte SMOKE = 4;
    /** The rack rising ({@code arg}: radius × 10), held {@code duration} ticks. */
    public static final byte RACK = 5;
    /** He breaks out of the trap. */
    public static final byte FREE = 6;

    public static final Type<AlastairFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("alastair_fx"));
    public static final StreamCodec<ByteBuf, AlastairFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new AlastairFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
