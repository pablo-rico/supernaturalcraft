package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → a hunter in Michael's fight: one moment for the client to show (a phase's celestial title card, his question,
 * a possession, the Host's mark, the lance pinning someone, Heaven changing round them, the transform, a burst of steel
 * feathers, the grace's favour). The client animates it on its own from there.
 *
 * @param entity   Michael's entity id (or the hunter's, where noted)
 * @param kind     one of the constants below
 * @param arg      kind-specific number
 * @param arg2     kind-specific number
 * @param point    where, if anywhere
 * @param duration how long, in ticks
 */
public record MichaelFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    /** A title card: {@code arg} = the phase (1-6, {@code title.supernaturalcraft.michael.phase<n>}), or {@link #TITLE_DEATH} /
     *  {@link #TITLE_VICTORY}; shown for {@code duration}. */
    public static final byte TITLE = 0;
    /** He asks the receiver for their yes: answer within {@code duration} ticks with a {@code VesselAnswerPayload}. */
    public static final byte ASK_YES = 1;
    /** The receiver is worn ({@code arg} = 1) for {@code duration}, or let go (0): third-person camera, no control. */
    public static final byte POSSESSED = 2;
    /** Heaven's mark on hunter {@code arg} (entity id) for {@code duration}: the Host hunts them. */
    public static final byte MARK = 3;
    /** The lance pins hunter {@code arg} (entity id) at {@code point} for {@code duration}. */
    public static final byte LANCE_PIN = 4;
    /** The arena turns into Heaven's {@code arg} (0 Garden, 1 War in Heaven, 2 Throne Room): sky, fog and light. */
    public static final byte ARENA_SHIFT = 5;
    /** His vessel bursts into light and his true form stands up ({@code duration} = the whole transform). */
    public static final byte TRANSFORM = 6;
    /** A fan of {@code arg} steel feathers leaves {@code point}: a flash and a shower on the receiver's screen if near. */
    public static final byte FEATHER_BURST = 7;
    /** The receiver carries his grace's favour for {@code duration}: double damage against him. */
    public static final byte FAVOR = 8;

    /** {@link #TITLE} args beyond the six phases. */
    public static final int TITLE_DEATH = 7, TITLE_VICTORY = 8;

    public static final Type<MichaelFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("michael_fx"));
    public static final StreamCodec<ByteBuf, MichaelFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new MichaelFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
