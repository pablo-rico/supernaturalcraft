package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.18): one moment of Heaven to show, for the plot, its memories, Naomi or Zachariah. The client plays it
 * out on its own ({@code client/heaven/ClientHeaven}). Kinds are grouped by who sends them; a sender may add kinds in its own
 * range (world 0-9, Naomi 10-19, Zachariah 20-29) and must document their arguments here.
 *
 * @param entity   the entity it is about (a boss, a chair, a figure), -1 if none
 * @param kind     one of the constants below
 * @param arg      kind-specific
 * @param arg2     kind-specific
 * @param point    where, if anywhere
 * @param duration ticks (0 ends a lasting effect)
 * @param text     kind-specific text (a translation key, an id, a list); empty if none
 */
public record HeavenFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration, String text)
        implements CustomPacketPayload {

    // --- the world and the memories (0-9) -----------------------------------------------------------------------------
    /** Arrival through a gate: a white wash fading out over {@code duration} ticks; {@code text} = the plot owner's name. */
    public static final byte ARRIVE = 0;
    /** The plot is still being written: {@code arg} = percent done (100 = finished). */
    public static final byte PLOT_PROGRESS = 1;
    /** Stepped into a memory: {@code arg} = sky/fog tint ARGB, {@code text} = its title key, {@code duration} = fade in. */
    public static final byte MEMORY_ENTER = 2;
    /** Left a memory (the tint fades back over {@code duration}). */
    public static final byte MEMORY_LEAVE = 3;
    /** A memory gathered: {@code text} = the memory's id (the toast and the book page). */
    public static final byte MEMORY_COLLECTED = 4;
    /** A title card in Heaven: {@code text} = title key ({@code .subtitle} appended for the line under it). */
    public static final byte TITLE = 5;

    // --- Naomi (10-19) ---------------------------------------------------------------------------------------------------
    /** Strapped in: {@code entity} = the chair, {@code arg} = presses needed, {@code duration} = ticks before the drill. */
    public static final byte QTE_START = 10;
    /** Struggle so far: {@code arg} = presses counted, {@code arg2} = presses needed. */
    public static final byte QTE_PROGRESS = 11;
    /** Out of the chair: {@code arg} 1 = broke free (or freed), 0 = the drill came down. */
    public static final byte QTE_END = 12;
    /** Memory Wipe: the screen goes white for {@code duration} ticks. */
    public static final byte WHITEOUT = 13;
    /** A training test begins ({@code duration} ticks to pass it) or ends ({@code duration} 0; {@code arg} 1 passed). */
    public static final byte TRAINING_TEST = 14;
    /** Naomi's phase title: {@code arg} = phase (1-2), {@code arg2} 1 = her fall. */
    public static final byte NAOMI_TITLE = 15;

    // --- Zachariah (20-29) -----------------------------------------------------------------------------------------------
    /** A Heavenly Form held: {@code arg} = cabinet (1-4, 0 = none held), {@code duration} = ticks until overdue. */
    public static final byte FORM = 20;
    /** Approved: harder blows for {@code duration} ticks. */
    public static final byte APPROVED = 21;
    /** What is already written: {@code text} = the next attacks' ids, comma separated (empty clears the docket). */
    public static final byte FORETOLD = 22;
    /** A revision: entry {@code arg} of the docket is struck through and becomes {@code text} in {@code duration} ticks. */
    public static final byte REVISION = 23;
    /** The office wrapped round: the camera moves by {@code point} (an offset) with no jump. */
    public static final byte WRAP = 24;
    /** A Termination Notice on {@code entity}, due in {@code duration} ticks. */
    public static final byte TERMINATION = 25;
    /** Zachariah's phase title: {@code arg} = phase (1-4), {@code arg2} 1 = his fall. */
    public static final byte ZACHARIAH_TITLE = 26;

    public static final Type<HeavenFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("heaven_fx"));
    public static final StreamCodec<ByteBuf, HeavenFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
        ByteBufCodecs.STRING_UTF8.encode(buf, p.text);
    }, buf -> new HeavenFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf)));

    /** A payload with no point and no text. */
    public static HeavenFxPayload of(int entity, byte kind, int arg, int arg2, int duration) {
        return new HeavenFxPayload(entity, kind, arg, arg2, Vec3.ZERO, duration, "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
