package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.35): one moment of Crowley's fight, or of his companion form, to show. The client plays it out on its
 * own ({@code client/crowley/ClientCrowleyFx}). Sent to players within 96 blocks. Telegraphs are {@code TelegraphMarker}s
 * (red hound rings, the violet snap line, the yellow smoke zone, the red paper cone, the red hellfire lines); these are the
 * effects on top of them.
 *
 * <table>
 *   <caption>Kinds and their fields (unlisted: unused, 0)</caption>
 *   <tr><th>kind</th><th>entity</th><th>arg</th><th>arg2</th><th>point</th><th>duration</th></tr>
 *   <tr><td>{@link #TITLE}</td><td>Crowley</td><td>phase (1-3)</td><td>0 opening / 1 his fall</td><td>Crowley</td><td>ticks the card shows</td></tr>
 *   <tr><td>{@link #RED_SMOKE}</td><td>Crowley</td><td>radius ×10</td><td>-</td><td>the cloud's centre (floor)</td><td>ticks it lingers</td></tr>
 *   <tr><td>{@link #VANISH}</td><td>Crowley</td><td>-</td><td>-</td><td>where he stood</td><td>ticks of the burst</td></tr>
 *   <tr><td>{@link #APPEAR}</td><td>Crowley</td><td>-</td><td>-</td><td>where he arrives</td><td>ticks of the burst</td></tr>
 *   <tr><td>{@link #PAPER_STORM}</td><td>Crowley</td><td>yaw (degrees)</td><td>reach (blocks)</td><td>the cone's apex</td><td>ticks the papers fly</td></tr>
 *   <tr><td>{@link #HELLFIRE_LINE}</td><td>Crowley</td><td>end x − start x, ×10</td><td>end z − start z, ×10</td><td>the line's start (floor)</td><td>ticks it burns</td></tr>
 *   <tr><td>{@link #SNAP}</td><td>Crowley (or the companion)</td><td>the shoved entity's id (-1 none)</td><td>-</td><td>his hand</td><td>ticks</td></tr>
 *   <tr><td>{@link #SEAL_BREAK}</td><td>Crowley</td><td>the seal broken (1-3: the phase left behind)</td><td>-</td><td>Crowley</td><td>ticks</td></tr>
 *   <tr><td>{@link #HOUNDS}</td><td>Crowley (or the companion)</td><td>how many</td><td>-</td><td>where they come through</td><td>ticks of the scorch</td></tr>
 *   <tr><td>{@link #COLLECT}</td><td>Crowley</td><td>the marked hunter's id</td><td>-</td><td>the hunter</td><td>ticks the mark lasts</td></tr>
 *   <tr><td>{@link #COMPANION_POOF}</td><td>the companion</td><td>1 arrives / 0 leaves</td><td>-</td><td>where</td><td>ticks of the burst</td></tr>
 * </table>
 *
 * @param entity   see the table, -1 if none
 * @param kind     one of the constants below (only append)
 * @param arg      kind-specific
 * @param arg2     kind-specific
 * @param point    where
 * @param duration ticks
 */
public record CrowleyFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    public static final byte TITLE = 0;
    public static final byte RED_SMOKE = 1;
    public static final byte VANISH = 2;
    public static final byte APPEAR = 3;
    public static final byte PAPER_STORM = 4;
    public static final byte HELLFIRE_LINE = 5;
    public static final byte SNAP = 6;
    public static final byte SEAL_BREAK = 7;
    public static final byte HOUNDS = 8;
    public static final byte COLLECT = 9;
    public static final byte COMPANION_POOF = 10;

    public static final Type<CrowleyFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("crowley_fx"));
    public static final StreamCodec<ByteBuf, CrowleyFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new CrowleyFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
