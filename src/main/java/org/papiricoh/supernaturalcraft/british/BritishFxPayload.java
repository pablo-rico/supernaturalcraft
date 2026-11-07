package org.papiricoh.supernaturalcraft.british;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.SNCodecs;
import org.papiricoh.supernaturalcraft.network.Send;

/**
 * Season 12's moments on the client (v0.41): Ketch's, Hess's and Dagon's fights and Jack's birth. Kinds and palettes are
 * append-only; {@code arg}/{@code arg2}/{@code duration} mean what each kind's comment says.
 */
public record BritishFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    /** A title card: {@code arg} the {@code WHO_*} it is about, {@code arg2} the phase (0: the fall), {@code duration} ticks. */
    public static final byte TITLE = 0;
    /** A ring on the ground ({@code arg}: radius × 10, {@code arg2}: the palette). */
    public static final byte RING = 1;
    /** A lane along the ground from {@code point} ({@code arg}: yaw, {@code arg2}: length × 10 + palette × 10000). */
    public static final byte LANE = 2;
    /** A cloud filling a circle ({@code arg}: radius × 10, {@code arg2}: the palette), for {@code duration} ticks. */
    public static final byte CLOUD = 3;
    /** A flash of light at {@code point} ({@code arg}: its size × 10, {@code arg2}: the palette). */
    public static final byte FLASH = 4;
    /** A dome of a ward round {@code point} ({@code arg}: radius × 10), held {@code duration} ticks. */
    public static final byte WARD = 5;

    /** Who a title card is about. */
    public static final int WHO_KETCH = 0, WHO_HESS = 1, WHO_DAGON = 2, WHO_BIRTH = 3;
    /** Palettes: grey sparks (lead), hellfire, a hex (witch), telekinesis (violet), light (gold-white), smoke. */
    public static final int GREY = 0, FIRE = 1, HEX = 2, FORCE = 3, LIGHT = 4, SMOKE = 5;

    public static final Type<BritishFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("british_fx"));
    public static final StreamCodec<ByteBuf, BritishFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new BritishFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Sends {@code p} to everyone within 96 blocks of its point. */
    public static void send(ServerLevel level, BritishFxPayload p) {
        for (ServerPlayer pl : level.players()) if (pl.position().distanceToSqr(p.point()) < 96 * 96) Send.toPlayer(pl, p);
    }

    public static void send(ServerLevel level, int entity, byte kind, int arg, int arg2, Vec3 at, int duration) {
        send(level, new BritishFxPayload(entity, kind, arg, arg2, at, duration));
    }
}
