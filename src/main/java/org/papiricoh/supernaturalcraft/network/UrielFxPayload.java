package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Uriel's fight at the garrison (v0.40); handled by {@code ClientSealFx}. Kinds are append-only; {@code arg}/{@code arg2}/{@code duration} mean what each kind's comment says.
 */
public record UrielFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    /** A title card ({@code arg}: 1-2 the phase's, 3 his fall, 4 Castiel comes; {@code duration}: ticks). */
    public static final byte TITLE = 0;
    /** A blade cut ({@code point}: where it lands, {@code arg}: yaw): an arc of gold. */
    public static final byte SLASH = 1;
    /** The palm: the ground cracks round {@code point} ({@code arg}: radius x 10): dust, a shake. */
    public static final byte PALM = 2;
    /** The smite down a cone from {@code point} ({@code arg}: yaw, {@code arg2}: reach x 10): a sheet of gold light. */
    public static final byte SMITE = 3;
    /** A blinding flash of grace at {@code point} ({@code arg}: radius x 10): the screen goes white for whoever is in it. */
    public static final byte FLASH = 4;
    /** His wings open (phase II): a burst of feathers and light. */
    public static final byte WINGS = 5;
    /** He dies an angel's death at {@code point}: light out of him, wings burnt on the ground. */
    public static final byte DEATH = 6;

    public static final Type<UrielFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("uriel_fx"));
    public static final StreamCodec<ByteBuf, UrielFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new UrielFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
