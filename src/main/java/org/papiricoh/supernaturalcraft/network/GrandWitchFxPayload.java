package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.21, P6): one moment of the Grand Witch's fight to show. The client plays it out on its own
 * ({@code client/witchboss/ClientGrandWitchFx}). Sent to players within 96 blocks. The telegraphs themselves are
 * {@code TelegraphMarker}s (blue frost circles, her red inferno circle, the violet crush circle, the red circles that follow
 * every hunter in the mass taglock, Rowena's green safe circles, the yellow circle where her smoke lands); her blinks are also
 * {@code MagicFxPayload} TELEPORTs (SMOKE_YELLOW). Lasting states are synced: {@code GrandWitchEntity.shielded()} (her shield,
 * the {@code shield} loop) and {@code CovenWitchEntity.channeling()} (the beam from each channeler to her staff's
 * {@code crystal}; {@link #CHANNEL} marks its start and end and is re-sent every {@code GrandWitchBalance.BEAM_REFRESH}).
 *
 * <table>
 *   <caption>Kinds and their fields (unlisted: unused, 0)</caption>
 *   <tr><th>kind</th><th>entity</th><th>arg</th><th>arg2</th><th>point</th><th>duration</th></tr>
 *   <tr><td>{@link #TITLE}</td><td>the Grand Witch</td><td>phase (1-3)</td><td>0 opening / 1 her fall</td><td>her</td><td>ticks the card shows</td></tr>
 *   <tr><td>{@link #CHANNEL}</td><td>the channeling Coven Witch</td><td>the Grand Witch's id</td><td>1 channels / 0 stopped (interrupted or dead)</td><td>the witch</td><td>ticks the beam holds unless refreshed</td></tr>
 *   <tr><td>{@link #SHIELD}</td><td>the Grand Witch</td><td>1 raised / 0 broken (no channeler left)</td><td>-</td><td>her</td><td>ticks</td></tr>
 *   <tr><td>{@link #FROST_HEX}</td><td>the Grand Witch</td><td>radius ×10</td><td>-</td><td>one circle's centre (one payload each)</td><td>ticks the frost stays</td></tr>
 *   <tr><td>{@link #INFERNO}</td><td>the Grand Witch</td><td>radius ×10</td><td>-</td><td>the circle's centre (her feet)</td><td>ticks the fire burns</td></tr>
 *   <tr><td>{@link #CRUSH}</td><td>the Grand Witch</td><td>the crushed entity's id (-1 none)</td><td>-</td><td>the circle's centre</td><td>ticks held</td></tr>
 *   <tr><td>{@link #MASS_TAGLOCK}</td><td>the Grand Witch</td><td>the marked hunter's id</td><td>radius ×10</td><td>the hunter</td><td>ticks until the curse lands</td></tr>
 *   <tr><td>{@link #SAFE_CIRCLE}</td><td>Rowena (her ally), or -1</td><td>radius ×10</td><td>-</td><td>the green circle's centre</td><td>ticks it holds</td></tr>
 *   <tr><td>{@link #MASS_CURSE}</td><td>the Grand Witch</td><td>the hunter's id</td><td>1 cursed / 0 spared (in a green circle)</td><td>the hunter</td><td>ticks</td></tr>
 *   <tr><td>{@link #VEIL}</td><td>the Grand Witch</td><td>-</td><td>-</td><td>where she will appear</td><td>ticks until she does</td></tr>
 *   <tr><td>{@link #ALLY}</td><td>Rowena (her ally)</td><td>1 arrives / 0 leaves</td><td>-</td><td>where</td><td>ticks of the smoke</td></tr>
 *   <tr><td>{@link #STAFF}</td><td>the Coven Staff's wielder</td><td>the hexed entity's id</td><td>-</td><td>where it struck</td><td>ticks</td></tr>
 * </table>
 */
public record GrandWitchFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    public static final byte TITLE = 0;
    public static final byte CHANNEL = 1;
    public static final byte SHIELD = 2;
    public static final byte FROST_HEX = 3;
    public static final byte INFERNO = 4;
    public static final byte CRUSH = 5;
    public static final byte MASS_TAGLOCK = 6;
    public static final byte SAFE_CIRCLE = 7;
    public static final byte MASS_CURSE = 8;
    public static final byte VEIL = 9;
    public static final byte ALLY = 10;
    public static final byte STAFF = 11;

    public static final Type<GrandWitchFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("grand_witch_fx"));
    public static final StreamCodec<ByteBuf, GrandWitchFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new GrandWitchFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
