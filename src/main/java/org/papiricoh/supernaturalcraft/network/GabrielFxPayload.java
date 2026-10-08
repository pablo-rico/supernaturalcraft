package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.14): one moment of Gabriel's TV Land (or of his pranks) to show. The client plays it out on its own
 * ({@code client/gabriel/ClientGabriel}).
 *
 * @param entity   Gabriel (or the creature/double it is about), -1 if none
 * @param kind     one of the constants below
 * @param arg      kind-specific
 * @param arg2     kind-specific
 * @param point    where, if anywhere
 * @param duration ticks
 */
public record GabrielFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    /** The channel flips: static, rolling bars and the "CH n" display ({@code arg} = Channel ordinal) for {@code duration}. */
    public static final byte CHANNEL = 0;
    /** A sign on the HUD: {@code arg} 0 LAUGH, 1 APPLAUSE, 2 ON AIR; {@code arg2} 1 lit, 0 dark. */
    public static final byte SIGN = 1;
    /**
     * A quiz round ({@code arg} = index in {@code QuizBank.QUESTIONS}, {@code arg2} = the deal packed as
     * {@code QuizBank.pack}: the answer on each platform) for {@code duration} ticks; {@code arg} -1 closes the panel,
     * with {@code arg2} 1 right / 0 wrong for this hunter.
     */
    public static final byte QUIZ = 2;
    /** The heart monitor beeps ({@code duration} = ticks to the next beep, so the HUD can draw the trace). */
    public static final byte BEAT = 3;
    /** The real Gabriel's six-winged shadow, at {@code point}, for {@code duration} (the commercial). */
    public static final byte REVEAL = 4;
    /** A prank for this hunter ({@code arg} = {@code PrankRules.Prank} ordinal) at {@code point}: its sound, its hint. */
    public static final byte PRANK = 5;
    /** A party hat on {@code entity} for {@code duration} ticks (0 takes it off). */
    public static final byte HAT = 6;
    /** Free will sees through a double ({@code entity}): it flickers, translucent, for this hunter only, for {@code duration}. */
    public static final byte FREE_WILL_TELL = 7;
    /** A title card (the channel's name or the episode's end): {@code arg} = Channel ordinal, {@code arg2} 0 opening / 1 the end. */
    public static final byte TITLE = 8;

    public static final Type<GabrielFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("gabriel_fx"));
    public static final StreamCodec<ByteBuf, GabrielFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new GabrielFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
