package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * A seal event's moments: the warning, the rite's progress, a seal breaking or held (v0.40); handled by {@code ClientSealFx}. Kinds are append-only; {@code arg}/{@code arg2}/{@code duration} mean what each kind's comment says.
 */
public record SealFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    /** A title card ({@code arg}: which one, {@code duration}: ticks). */
    public static final byte TITLE = 0;
    /** A hunter is called to a seal: the card, the sound and the compass ({@code arg}: the kind, {@code point}: the site). */
    public static final byte WARNING = 1;
    /** A seal broke, heard by every hunter: far thunder, the sky reddens ({@code arg}: its number 1-12, {@code arg2}: the new stage). */
    public static final byte BROKEN = 2;
    /** A seal held ({@code arg}: the kind, {@code point}: the site): the card and the light breaking through. */
    public static final byte HELD = 3;
    /** The hex at a site pulses ({@code point}, {@code arg}: the rite's progress 0-100): motes and a low chant. */
    public static final byte PULSE = 4;
    /** A plague takes hold round {@code point} ({@code arg}: radius): a drift of hex motes and blood drops. */
    public static final byte PLAGUE = 5;

    public static final Type<SealFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("seal_fx"));
    public static final StreamCodec<ByteBuf, SealFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new SealFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
