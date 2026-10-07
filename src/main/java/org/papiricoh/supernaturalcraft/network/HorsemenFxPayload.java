package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → a hunter in a Horseman's fight: one moment for the client to show (War's illusion, Death's clock and limbo,
 * the world flipping, a Horseman mounting up). The client animates it on its own from there.
 *
 * @param entity   the Horseman's entity id
 * @param kind     one of the constants below
 * @param arg      kind-specific number
 * @param arg2     kind-specific number
 * @param point    where, if anywhere
 * @param duration how long, in ticks
 */
public record HorsemenFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    /** War's illusion falls on the receiver: everyone else looks like a demon for {@code duration}. */
    public static final byte ILLUSION = 0;
    /** The receiver's death clock: {@code arg} ticks left of {@code arg2}; {@code duration} = limbo ticks left (0 = not in limbo);
     *  {@code point.x} = 1 while the world of the dead runs it double. A clock of -1 ticks hides it (the fight is over). */
    public static final byte CLOCK = 1;
    /** The receiver falls into limbo: the world goes grey; the light out is at {@code point}; {@code duration} to reach it. */
    public static final byte LIMBO_ENTER = 2;
    /** The receiver is out of limbo ({@code arg} = 1 reached the light, 0 died or the fight ended). */
    public static final byte LIMBO_EXIT = 3;
    /** The world of the dead takes the arena ({@code arg} = 1) or lets it go (0). */
    public static final byte WORLD_FLIP = 4;
    /** Horseman {@code entity} mounts his horse ({@code arg} = his kind's ordinal). */
    public static final byte MOUNT = 5;

    public static final Type<HorsemenFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("horsemen_fx"));
    public static final StreamCodec<ByteBuf, HorsemenFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new HorsemenFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
