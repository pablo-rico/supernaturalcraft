package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client (v0.21): one moment of Abaddon's fight (or of her retinue) to show. The client plays it out on its own
 * ({@code client/abaddon/ClientAbaddonFx}). Sent to players within 96 blocks. The telegraphs themselves are
 * {@code TelegraphMarker}s (the herald's red mark circle that follows its target, the red sweep cone, the yellow smoke lane,
 * the violet pins, the Brand's red rings and green gaps); these are the effects on top of them. Her synced flags
 * ({@code AbaddonEntity.smoke()/trapped()/stunned()}) and the knight's ({@code AbaddonKnightEntity.possessed()}) carry the
 * states that last (the renderer hides her while she is smoke; a possessed knight glows).
 *
 * <table>
 *   <caption>Kinds and their fields (unlisted: unused, 0)</caption>
 *   <tr><th>kind</th><th>entity</th><th>arg</th><th>arg2</th><th>point</th><th>duration</th></tr>
 *   <tr><td>{@link #TITLE}</td><td>Abaddon</td><td>phase (1-3)</td><td>0 opening / 1 her fall</td><td>Abaddon</td><td>ticks the card shows</td></tr>
 *   <tr><td>{@link #HERALD_MARK}</td><td>who marks (Abaddon or a herald)</td><td>the marked entity's id</td><td>radius ×10</td><td>the mark's start (floor)</td><td>ticks it follows and holds before the strike</td></tr>
 *   <tr><td>{@link #MARK_STRIKE}</td><td>who marked</td><td>radius ×10</td><td>1 Abaddon's / 0 a herald's</td><td>where it lands</td><td>ticks of the burst</td></tr>
 *   <tr><td>{@link #SWEEP}</td><td>Abaddon</td><td>yaw (degrees)</td><td>reach (blocks)</td><td>the cone's apex</td><td>ticks the fire lingers</td></tr>
 *   <tr><td>{@link #SUMMON}</td><td>Abaddon</td><td>the summoned entity's id</td><td>0 knight / 1 herald</td><td>where it rises</td><td>ticks</td></tr>
 *   <tr><td>{@link #SMOKE_OUT}</td><td>Abaddon</td><td>the knight's id</td><td>-</td><td>where the smoke leaves her (chest)</td><td>flight ticks to the knight</td></tr>
 *   <tr><td>{@link #POSSESS}</td><td>Abaddon</td><td>the knight's id</td><td>1 begins / 0 she leaves of her own will</td><td>the knight</td><td>ticks the possession may last (begins)</td></tr>
 *   <tr><td>{@link #EXPEL}</td><td>Abaddon</td><td>the knight's id (-1 gone)</td><td>how ({@link #BY_EXORCISE}, {@link #BY_NAME}, {@link #BY_SALT}, {@link #BY_TRAP}, {@link #BY_VESSEL})</td><td>where her body comes back</td><td>stun ticks (×2 damage)</td></tr>
 *   <tr><td>{@link #PIN}</td><td>Abaddon</td><td>the pinned entity's id</td><td>-</td><td>where it is held</td><td>ticks held</td></tr>
 *   <tr><td>{@link #BRAND}</td><td>Abaddon</td><td>rings (radii {@code AbaddonBalance.BRAND_RINGS})</td><td>gaps per ring</td><td>the Brand's centre</td><td>windup ticks (the burst at the end)</td></tr>
 *   <tr><td>{@link #TRAPPED}</td><td>Abaddon</td><td>-</td><td>-</td><td>the devil's trap's centre</td><td>ticks it holds her</td></tr>
 *   <tr><td>{@link #TRAP_FLARE}</td><td>Abaddon</td><td>-</td><td>-</td><td>the broken trap's centre</td><td>ticks of the flare</td></tr>
 *   <tr><td>{@link #EXPELLED_BY}</td><td>Abaddon</td><td>who drove her out (entity id)</td><td>how (as {@link #EXPEL})</td><td>where they stand</td><td>ticks (sent with the {@link #EXPEL} it explains: the recoil points away from them)</td></tr>
 *   <tr><td>{@link #CLEAVE}</td><td>the wielder (the Knights' Cleaver)</td><td>yaw (degrees)</td><td>reach (blocks)</td><td>the wave's start</td><td>ticks the wave runs</td></tr>
 * </table>
 *
 * @param entity   see the table, -1 if none
 * @param kind     one of the constants below (only append)
 * @param arg      kind-specific
 * @param arg2     kind-specific
 * @param point    where
 * @param duration ticks
 */
public record AbaddonFxPayload(int entity, byte kind, int arg, int arg2, Vec3 point, int duration) implements CustomPacketPayload {

    public static final byte TITLE = 0;
    public static final byte HERALD_MARK = 1;
    public static final byte MARK_STRIKE = 2;
    public static final byte SWEEP = 3;
    public static final byte SUMMON = 4;
    public static final byte SMOKE_OUT = 5;
    public static final byte POSSESS = 6;
    public static final byte EXPEL = 7;
    public static final byte PIN = 8;
    public static final byte BRAND = 9;
    public static final byte TRAPPED = 10;
    public static final byte TRAP_FLARE = 11;
    public static final byte CLEAVE = 12;
    /** v0.21 (P6, for FX): who forced her out of a knight (salt, Exorcise, a name), sent right after {@link #EXPEL}. */
    public static final byte EXPELLED_BY = 13;

    /** How she was forced out of a knight ({@link #EXPEL}'s {@code arg2}). */
    public static final int BY_EXORCISE = 0, BY_NAME = 1, BY_SALT = 2, BY_TRAP = 3, BY_VESSEL = 4;

    public static final Type<AbaddonFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("abaddon_fx"));
    public static final StreamCodec<ByteBuf, AbaddonFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg2);
        SNCodecs.VEC3.encode(buf, p.point);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new AbaddonFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), SNCodecs.VEC3.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
