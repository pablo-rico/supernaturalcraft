package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.39): one moment of Godstiel's fight, or of season 6's story, to show. The client plays it out on its own
 * ({@code client/godstiel/ClientGodstielFx}). Sent to players within 96 blocks. Telegraphs are {@code TelegraphMarker}s (gold
 * for the light, violet for the Leviathans); these are the effects on top of them.
 *
 * <table>
 *   <caption>Kinds and their fields (unlisted: unused, 0)</caption>
 *   <tr><th>kind</th><th>entity</th><th>arg</th><th>arg2</th><th>point</th><th>duration</th></tr>
 *   <tr><td>{@link #TITLE}</td><td>Godstiel</td><td>phase (1-5)</td><td>0 opening / 1 kneels / 2 forgiven / 3 condemned</td><td>him</td><td>ticks the card shows</td></tr>
 *   <tr><td>{@link #SMITE}</td><td>Godstiel</td><td>radius ×10</td><td>-</td><td>where the light falls (floor)</td><td>ticks</td></tr>
 *   <tr><td>{@link #PALM}</td><td>Godstiel</td><td>yaw (degrees)</td><td>reach</td><td>the lane's start</td><td>ticks</td></tr>
 *   <tr><td>{@link #RADIANCE}</td><td>Godstiel</td><td>radius ×10</td><td>-</td><td>him</td><td>ticks of the flash</td></tr>
 *   <tr><td>{@link #JUDGED}</td><td>Godstiel</td><td>the heretic's id</td><td>-</td><td>the heretic</td><td>ticks of the burst</td></tr>
 *   <tr><td>{@link #BOLT}</td><td>Godstiel</td><td>end x − start x, ×10</td><td>end z − start z, ×10</td><td>the lane's start</td><td>ticks</td></tr>
 *   <tr><td>{@link #CRACK}</td><td>Godstiel</td><td>how many sets (1-3)</td><td>-</td><td>him</td><td>ticks of the shudder</td></tr>
 *   <tr><td>{@link #GOO}</td><td>Godstiel</td><td>yaw (degrees)</td><td>reach</td><td>the cone's apex</td><td>ticks it flies</td></tr>
 *   <tr><td>{@link #SWEEP}</td><td>Godstiel</td><td>radius ×10</td><td>-</td><td>him</td><td>ticks</td></tr>
 *   <tr><td>{@link #SURGE}</td><td>Godstiel</td><td>radius ×10</td><td>-</td><td>a geyser</td><td>ticks</td></tr>
 *   <tr><td>{@link #STRUGGLE}</td><td>Godstiel</td><td>1 begins / 0 ends</td><td>times spoken to</td><td>him</td><td>ticks it lasts</td></tr>
 *   <tr><td>{@link #VOICES}</td><td>Godstiel</td><td>the line (0..)</td><td>-</td><td>him</td><td>ticks the line shows</td></tr>
 *   <tr><td>{@link #DOOR}</td><td>-1</td><td>1 opens / 0 closes</td><td>-</td><td>the door, in the air</td><td>ticks it lasts</td></tr>
 *   <tr><td>{@link #SOULS}</td><td>the one who takes them (Castiel)</td><td>-</td><td>-</td><td>the door</td><td>ticks they pour</td></tr>
 *   <tr><td>{@link #BLACK_WATER}</td><td>Godstiel</td><td>-</td><td>-</td><td>the deck</td><td>ticks</td></tr>
 * </table>
 *
 * @param entity   see the table, -1 if none
 * @param kind     one of the constants below (only append)
 * @param arg      kind-specific
 * @param arg2     kind-specific
 * @param point    where
 * @param duration ticks
 */
public record GodstielFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    public static final byte TITLE = 0;
    public static final byte SMITE = 1;
    public static final byte PALM = 2;
    public static final byte RADIANCE = 3;
    public static final byte JUDGED = 4;
    public static final byte BOLT = 5;
    public static final byte CRACK = 6;
    public static final byte GOO = 7;
    public static final byte SWEEP = 8;
    public static final byte SURGE = 9;
    public static final byte STRUGGLE = 10;
    public static final byte VOICES = 11;
    public static final byte DOOR = 12;
    public static final byte SOULS = 13;
    public static final byte BLACK_WATER = 14;

    public static final Type<GodstielFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("godstiel_fx"));
    public static final StreamCodec<ByteBuf, GodstielFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new GodstielFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
