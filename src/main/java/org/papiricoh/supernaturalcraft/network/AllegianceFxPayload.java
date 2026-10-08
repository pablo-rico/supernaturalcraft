package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.13): one moment of the allegiance to show. The client plays it out on its own.
 *
 * @param entity the player (or creature) it is about
 * @param kind   one of the constants below
 * @param arg    kind-specific
 * @param arg2   kind-specific
 * @param point  where, if anywhere
 * @param duration ticks
 */
public record AllegianceFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    /** A rank is reached: the ascension cinematic ({@code arg} = Faction ordinal, {@code arg2} = rank; rank 0 = cured) and its title card. */
    public static final byte ASCENSION = 0;
    /** A power was cast ({@code arg} = Power ordinal) at {@code point}: its sound and flash for whoever is near. */
    public static final byte CAST = 1;
    /** A cast was refused ({@code arg} = {@code PowerRules.Verdict} ordinal): a line on the HUD. */
    public static final byte DENIED = 2;
    /** True form: {@code entity} blazes for {@code duration} ticks (screen-wide light for those who look). */
    public static final byte TRUE_FORM = 3;
    /** Angel radio: something whispers from {@code point} ({@code arg} 0 a demon, 1 a boss) for {@code duration}. */
    public static final byte RADIO_PING = 4;
    /** {@code entity} was exorcised or banished: smoke or light torn out of them at {@code point}. */
    public static final byte EXPELLED = 5;
    /** Powers nullified ({@code arg} 1) or given back (0): Chuck's "I gave you that". */
    public static final byte SUPPRESSED = 6;
    /** A villager's pact sealed at {@code point}. */
    public static final byte PACT = 7;
    /** A message on the HUD: {@code arg} indexes {@code MESSAGES} (client), e.g. bloodlust starting. */
    public static final byte WHISPER = 8;

    public static final Type<AllegianceFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("allegiance_fx"));
    public static final StreamCodec<ByteBuf, AllegianceFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new AllegianceFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
