package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.21): one moment of Cain's fight to show. The client plays it out on its own
 * ({@code client/cain/ClientCainFx}). Sent to players within 96 blocks of him. The telegraphs themselves are
 * {@code TelegraphMarker}s (red lanes/cones/rings/circles, yellow swarm circles, violet tool lanes, green shelters); these are
 * the effects on top of them.
 *
 * <table>
 *   <caption>Kinds and their fields (unlisted: unused, 0)</caption>
 *   <tr><th>kind</th><th>entity</th><th>arg</th><th>arg2</th><th>point</th><th>duration</th></tr>
 *   <tr><td>{@link #TITLE}</td><td>Cain</td><td>phase (1-3)</td><td>0 opening / 1 his fall</td><td>Cain</td><td>ticks the card shows</td></tr>
 *   <tr><td>{@link #SWARM}</td><td>Cain</td><td>radius (blocks)</td><td>-</td><td>the swarm's centre (floor)</td><td>ticks the bees stay</td></tr>
 *   <tr><td>{@link #LUNGE}</td><td>Cain</td><td>dash number (0-2)</td><td>-</td><td>where the dash ends (he started at his last position)</td><td>trail ticks</td></tr>
 *   <tr><td>{@link #FURROW}</td><td>Cain</td><td>yaw (degrees)</td><td>reach (blocks)</td><td>the cone's apex</td><td>ticks the earth flies</td></tr>
 *   <tr><td>{@link #SPIKES}</td><td>Cain</td><td>outer radius ×10</td><td>inner radius ×10</td><td>the ring's centre</td><td>ticks the spikes stand</td></tr>
 *   <tr><td>{@link #TOOLS}</td><td>Cain (thrown from his hand)</td><td>tool (0 pitchfork, 1 sickle, 2 shovel, 3 axe)</td><td>-</td><td>where it lands</td><td>flight ticks</td></tr>
 *   <tr><td>{@link #EXECUTION}</td><td>Cain</td><td>the target's entity id (-1 none)</td><td>-</td><td>where he reappears</td><td>ticks</td></tr>
 *   <tr><td>{@link #MARK_FLARE}</td><td>Cain</td><td>radius ×10</td><td>shelters (each sent next as a {@link #FLARE_SHELTER})</td><td>the flare's centre (the arena's centre, on the floor)</td><td>windup ticks (the burst at the end)</td></tr>
 *   <tr><td>{@link #FLARE_SHELTER}</td><td>Cain</td><td>shelter number (0-2)</td><td>its radius ×10</td><td>the shelter's centre (floor)</td><td>windup ticks (as its MARK_FLARE)</td></tr>
 *   <tr><td>{@link #OFFER}</td><td>Cain (or -1 at his grave)</td><td>the hunter offered the Mark (entity id)</td><td>1 taken / 0 offered</td><td>where</td><td>ticks</td></tr>
 * </table>
 *
 * @param entity   Cain, -1 if none
 * @param kind     one of the constants below (only append)
 * @param arg      kind-specific
 * @param arg2     kind-specific
 * @param point    where
 * @param duration ticks
 */
public record CainFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    public static final byte TITLE = 0;
    public static final byte SWARM = 1;
    public static final byte LUNGE = 2;
    public static final byte FURROW = 3;
    public static final byte SPIKES = 4;
    public static final byte TOOLS = 5;
    public static final byte EXECUTION = 6;
    public static final byte MARK_FLARE = 7;
    public static final byte OFFER = 8;
    /** One green shelter of the Mark's flare (sent right after its {@link #MARK_FLARE}, one per shelter). */
    public static final byte FLARE_SHELTER = 9;

    public static final Type<CainFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("cain_fx"));
    public static final StreamCodec<ByteBuf, CainFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new CainFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
