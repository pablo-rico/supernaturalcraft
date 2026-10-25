package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.22): angels dying by an angel blade, and the Fall. The client plays it out on its own (light, sound, the
 * burnt wings, the sky). Kinds are append-only.
 *
 * @param kind     one of the constants below
 * @param entity   the angel, for {@link #SMITE}
 * @param arg      kind-specific
 * @param point    where, if anywhere
 * @param yaw      which way the angel faced (its body yaw, degrees), for {@link #SMITE}: the wings are burnt behind it
 * @param duration ticks
 */
public record SmiteFxPayload(byte kind, int entity, int arg, Vec3 point, float yaw, int duration) implements CustomPacketPayload {

    /**
     * {@code entity} was struck by an angel blade and burns for {@code duration} ticks before it falls: light from its eyes and
     * mouth, then the flash and its wings burnt into the ground at {@code point} (its feet). {@code arg} = the raw id
     * ({@code BuiltInRegistries.ITEM.getId}) of the blade left in its chest.
     */
    public static final byte SMITE = 0;
    /** The night of the Fall begins ({@code arg} 1) or ends (0): angels falling across the whole Overworld sky. */
    public static final byte FALL_NIGHT = 1;
    /** One fallen angel streaks down toward {@code point} (x, z; y unused) over {@code duration} ticks. */
    public static final byte FALL_STAR = 2;

    public static final Type<SmiteFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("smite_fx"));
    public static final StreamCodec<ByteBuf, SmiteFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        SNCodecs.VEC3.encode(buf, p.point);
        buf.writeFloat(p.yaw);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new SmiteFxPayload(buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            SNCodecs.VEC3.decode(buf), buf.readFloat(), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
