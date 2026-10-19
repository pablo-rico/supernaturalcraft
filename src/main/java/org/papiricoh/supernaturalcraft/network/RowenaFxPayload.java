package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.21, P6): one moment of Rowena's fight to show. The client plays it out on its own
 * ({@code client/witchboss/ClientRowenaFx}). Sent to players within 96 blocks. The telegraphs themselves are
 * {@code TelegraphMarker}s (the violet fling lane, the red Ignis and blue Glacies circles, the yellow circle where her smoke
 * lands, the red circle over the whole arena before the recitation's hex); her smoke blinks and the curses down a stolen
 * taglock are also {@code MagicFxPayload}s (TELEPORT with SMOKE_YELLOW, BOWL_SEND and HEX_PULSE). Her synced flags
 * ({@code RowenaEntity.reciting()/stunned()}) carry the states that last; the bowls she recites over are
 * {@code RecitationBowlEntity}s (their cracks: {@code RecitationBowlEntity.hits()}).
 *
 * <table>
 *   <caption>Kinds and their fields (unlisted: unused, 0)</caption>
 *   <tr><th>kind</th><th>entity</th><th>arg</th><th>arg2</th><th>point</th><th>duration</th></tr>
 *   <tr><td>{@link #TITLE}</td><td>Rowena</td><td>phase (1-3)</td><td>0 opening / 1 her fall</td><td>Rowena</td><td>ticks the card shows</td></tr>
 *   <tr><td>{@link #FLING}</td><td>Rowena</td><td>yaw (degrees)</td><td>length (blocks)</td><td>the lane's start</td><td>ticks of the gust</td></tr>
 *   <tr><td>{@link #IGNIS}</td><td>Rowena</td><td>radius ×10</td><td>-</td><td>the circle's centre</td><td>ticks the fire burns</td></tr>
 *   <tr><td>{@link #GLACIES}</td><td>Rowena</td><td>radius ×10</td><td>-</td><td>the circle's centre</td><td>ticks the frost stays</td></tr>
 *   <tr><td>{@link #FAMILIAR}</td><td>Rowena</td><td>the familiar's id</td><td>-</td><td>where it appears</td><td>ticks</td></tr>
 *   <tr><td>{@link #HOMING}</td><td>Rowena</td><td>the cursed hunter's id</td><td>the curse ({@code CurseEffect.Curse} ordinal)</td><td>her hands</td><td>travel ticks</td></tr>
 *   <tr><td>{@link #VICTIMS}</td><td>Rowena</td><td>the hexed victim's id</td><td>-</td><td>where it rises</td><td>ticks</td></tr>
 *   <tr><td>{@link #VICTIM_FATE}</td><td>Rowena</td><td>the victim's id</td><td>0 killed (it heals her) / 1 freed (it hurts her)</td><td>the victim</td><td>ticks of the stream to her</td></tr>
 *   <tr><td>{@link #RECITE}</td><td>Rowena</td><td>progress (thousandths)</td><td>bowls still whole</td><td>Rowena</td><td>ticks until the next update</td></tr>
 *   <tr><td>{@link #BOWL_BROKEN}</td><td>Rowena</td><td>the bowl's entity id</td><td>bowls still whole</td><td>the bowl</td><td>ticks</td></tr>
 *   <tr><td>{@link #RECITE_END}</td><td>Rowena</td><td>0 broken off (she falls, stunned) / 1 spoken (the hex sweeps the arena)</td><td>the hex's damage</td><td>the arena's centre</td><td>ticks</td></tr>
 *   <tr><td>{@link #SMOKE}</td><td>Rowena</td><td>-</td><td>-</td><td>where she will appear</td><td>ticks until she does</td></tr>
 * </table>
 */
public record RowenaFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    public static final byte TITLE = 0;
    public static final byte FLING = 1;
    public static final byte IGNIS = 2;
    public static final byte GLACIES = 3;
    public static final byte FAMILIAR = 4;
    public static final byte HOMING = 5;
    public static final byte VICTIMS = 6;
    public static final byte VICTIM_FATE = 7;
    public static final byte RECITE = 8;
    public static final byte BOWL_BROKEN = 9;
    public static final byte RECITE_END = 10;
    public static final byte SMOKE = 11;

    public static final Type<RowenaFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("rowena_fx"));
    public static final StreamCodec<ByteBuf, RowenaFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new RowenaFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
