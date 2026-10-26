package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.25): one moment of Dick Roman's fight, or of a Leviathan's, to show. The client plays it out on its own
 * ({@code client/leviathan/ClientDickFx}). Sent to players within 96 blocks. The telegraphs themselves are
 * {@code TelegraphMarker}s (red ring/cones/lanes/circles, violet contract lanes, yellow staff circles and surge warning); these
 * are the effects on top of them.
 *
 * <table>
 *   <caption>Kinds and their fields (unlisted: unused, 0)</caption>
 *   <tr><th>kind</th><th>entity</th><th>arg</th><th>arg2</th><th>point</th><th>duration</th></tr>
 *   <tr><td>{@link #TITLE}</td><td>Dick</td><td>phase (1-4)</td><td>0 opening / 1 his fall</td><td>Dick</td><td>ticks the card shows</td></tr>
 *   <tr><td>{@link #DOWNSIZE}</td><td>Dick</td><td>radius ×10</td><td>-</td><td>the ring's centre (floor)</td><td>ticks the shockwave runs</td></tr>
 *   <tr><td>{@link #CONTRACT}</td><td>Dick</td><td>the bound hunter's entity id</td><td>-</td><td>where the hunter stands</td><td>ticks bound (a glowing signature round them)</td></tr>
 *   <tr><td>{@link #BITE}</td><td>Dick</td><td>yaw (degrees)</td><td>reach ×10</td><td>the cone's apex</td><td>ticks (teeth snapping shut)</td></tr>
 *   <tr><td>{@link #SWALLOW}</td><td>Dick</td><td>the swallowed hunter's entity id</td><td>1 spat out / 0 swallowed</td><td>his maw</td><td>ticks held (0 at the spit)</td></tr>
 *   <tr><td>{@link #GOO_SPLASH}</td><td>Dick (or -1)</td><td>radius ×10</td><td>-</td><td>where goo lands (floor)</td><td>ticks the splash flies</td></tr>
 *   <tr><td>{@link #COPY}</td><td>Dick or a double</td><td>the copied hunter's entity id</td><td>1 Dick himself / 0 a double</td><td>where it stands</td><td>shimmer ticks</td></tr>
 *   <tr><td>{@link #EXPOSED}</td><td>Dick</td><td>doubles burst</td><td>-</td><td>Dick</td><td>ticks he staggers</td></tr>
 *   <tr><td>{@link #TENTACLE}</td><td>Dick</td><td>yaw (degrees)</td><td>reach ×10</td><td>where it rises (floor)</td><td>ticks it lashes</td></tr>
 *   <tr><td>{@link #SURGE}</td><td>Dick</td><td>arena radius</td><td>-</td><td>the arena's centre (floor)</td><td>windup ticks (the goo rolls back at the end)</td></tr>
 *   <tr><td>{@link #BORAX}</td><td>-1</td><td>radius ×10</td><td>things it burned</td><td>where borax lands</td><td>fizz ticks</td></tr>
 *   <tr><td>{@link #DECAPITATE}</td><td>the Leviathan's body</td><td>its head's entity id</td><td>-</td><td>the neck</td><td>spurt ticks</td></tr>
 *   <tr><td>{@link #REVEAL}</td><td>the Leviathan (or Dick)</td><td>-</td><td>-</td><td>its head</td><td>ticks the maw opens over</td></tr>
 * </table>
 *
 * @param entity   who, -1 if none
 * @param kind     one of the constants below (only append)
 * @param arg      kind-specific
 * @param arg2     kind-specific
 * @param point    where
 * @param duration ticks
 */
public record DickFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    public static final byte TITLE = 0;
    public static final byte DOWNSIZE = 1;
    public static final byte CONTRACT = 2;
    public static final byte BITE = 3;
    public static final byte SWALLOW = 4;
    public static final byte GOO_SPLASH = 5;
    public static final byte COPY = 6;
    public static final byte EXPOSED = 7;
    public static final byte TENTACLE = 8;
    public static final byte SURGE = 9;
    public static final byte BORAX = 10;
    public static final byte DECAPITATE = 11;
    public static final byte REVEAL = 12;

    public static final Type<DickFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("dick_fx"));
    public static final StreamCodec<ByteBuf, DickFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new DickFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Sends {@code payload} to every player within 96 blocks of {@code at}. */
    public static void send(net.minecraft.server.level.ServerLevel level, Vec3 at, DickFxPayload payload) {
        for (net.minecraft.server.level.ServerPlayer p : level.players()) {
            if (p.distanceToSqr(at) < 96 * 96) Send.toPlayer(p, payload);
        }
    }
}
