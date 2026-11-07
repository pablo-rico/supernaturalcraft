package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.39): one moment of Eve's fight, or of an Alpha's, to show. The client plays it out on its own
 * ({@code client/eve/ClientEveFx}). Sent to players within 96 blocks. The telegraphs themselves are {@code TelegraphMarker}s (red
 * cones, circles, lanes and rings, yellow birth circles, green safe circles); these are the effects on top of them.
 *
 * <table>
 *   <caption>Kinds and their fields (unlisted: unused, 0)</caption>
 *   <tr><th>kind</th><th>entity</th><th>arg</th><th>arg2</th><th>point</th><th>duration</th></tr>
 *   <tr><td>{@link #TITLE}</td><td>Eve</td><td>phase (1-4)</td><td>0 opening / 1 her fall</td><td>Eve</td><td>ticks the card shows</td></tr>
 *   <tr><td>{@link #LASH}</td><td>Eve</td><td>yaw (degrees)</td><td>reach ×10</td><td>the cone's apex</td><td>ticks the claws trail</td></tr>
 *   <tr><td>{@link #ROOTS}</td><td>Eve</td><td>radius ×10</td><td>1 holding someone / 0 not</td><td>where the roots rise (floor)</td><td>ticks they stand</td></tr>
 *   <tr><td>{@link #BIRTH}</td><td>the child</td><td>-</td><td>-</td><td>where it climbs out (floor)</td><td>ticks the earth heaves</td></tr>
 *   <tr><td>{@link #SWARM}</td><td>Eve</td><td>yaw (degrees)</td><td>reach ×10</td><td>the lane's start (her mouth's floor)</td><td>ticks the roaches run</td></tr>
 *   <tr><td>{@link #CALL}</td><td>Eve</td><td>range</td><td>souls that answered</td><td>Eve</td><td>ticks the cry carries</td></tr>
 *   <tr><td>{@link #THORNS}</td><td>Eve</td><td>inner ×10</td><td>outer ×10</td><td>the ring's centre (floor)</td><td>ticks the thorns stand</td></tr>
 *   <tr><td>{@link #FIELD}</td><td>Eve</td><td>arena radius</td><td>-</td><td>the glade's centre (floor)</td><td>ticks the field stands</td></tr>
 *   <tr><td>{@link #ANSWERED}</td><td>the soul</td><td>-</td><td>-</td><td>its head</td><td>ticks its eyes burn amber</td></tr>
 *   <tr><td>{@link #ALPHA}</td><td>the Alpha</td><td>0 lunge / 1 drain / 2 command / 3 leap / 4 howl / 5 slam / 6 steal / 7 slough</td><td>a victim's entity id or -1</td><td>where</td><td>ticks</td></tr>
 * </table>
 *
 * @param entity   who, -1 if none
 * @param kind     one of the constants below (only append)
 * @param arg      kind-specific
 * @param arg2     kind-specific
 * @param point    where
 * @param duration ticks
 */
public record EveFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    public static final byte TITLE = 0;
    public static final byte LASH = 1;
    public static final byte ROOTS = 2;
    public static final byte BIRTH = 3;
    public static final byte SWARM = 4;
    public static final byte CALL = 5;
    public static final byte THORNS = 6;
    public static final byte FIELD = 7;
    public static final byte ANSWERED = 8;
    public static final byte ALPHA = 9;

    public static final Type<EveFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("eve_fx"));
    public static final StreamCodec<ByteBuf, EveFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new EveFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Sends {@code payload} to every player within 96 blocks of {@code at}. */
    public static void send(net.minecraft.server.level.ServerLevel level, Vec3 at, EveFxPayload payload) {
        for (net.minecraft.server.level.ServerPlayer p : level.players()) {
            if (p.distanceToSqr(at) < 96 * 96) Send.toPlayer(p, payload);
        }
    }
}
